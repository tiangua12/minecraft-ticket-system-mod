package com.easttown.ticketsystem.block;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.init.BlockEntityInit;
import com.easttown.ticketsystem.init.ItemInit;
import com.easttown.ticketsystem.init.MenuInit;
import com.easttown.ticketsystem.manager.CoinSystem;
import com.easttown.ticketsystem.manager.PriceCalculator;
import com.easttown.ticketsystem.screen.TicketMachineMenu;
import com.easttown.ticketsystem.util.TicketSystemLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class TicketMachineBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
        
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return false;
        }
        
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = super.extractItem(slot, amount, simulate);
            if (!simulate) {
                setChanged();
                if (level != null) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
            return stack;
        }
    };
    
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) { return 0; }
        @Override
        public void set(int index, int value) {}
        @Override
        public int getCount() { return 0; }
    };
    
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private String startStation = "";
    
    // 硬币存储系统
    private final Map<String, Integer> storedCoins = new HashMap<>();

    public TicketMachineBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.TICKET_MACHINE.get(), pos, state);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ticketsystem.ticket_machine");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TicketMachineMenu(containerId, inventory, this, player.getItemInHand(player.getUsedItemHand()).getItem() == ItemInit.ADMIN_KEY.get());
    }
    
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putString("StartStation", startStation);
        
        // 保存硬币存储
        CompoundTag coinsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : storedCoins.entrySet()) {
            coinsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("StoredCoins", coinsTag);
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        startStation = tag.getString("StartStation");
        
        // 加载硬币存储
        storedCoins.clear();
        CompoundTag coinsTag = tag.getCompound("StoredCoins");
        for (String key : coinsTag.getAllKeys()) {
            storedCoins.put(key, coinsTag.getInt(key));
        }
    }
    
    public void setStartStation(String station) {
        if (station != null && !station.isEmpty() && !station.equals(startStation)) {
            startStation = station;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }
    
    public String getStartStation() {
        return startStation;
    }
    
    public boolean canPrintTicket() {
        return itemHandler.getStackInSlot(0).isEmpty();
    }
    
    public void printTicket(String destination, Player player) {
        if (startStation.isEmpty() || destination.isEmpty()) return;

        ItemStack ticket = new ItemStack(ItemInit.TICKET.get());
        CompoundTag tag = new CompoundTag();

        // 添加车票详细信息
        String ticketId = UUID.randomUUID().toString();
        int price = PriceCalculator.calculatePrice(startStation, destination);

        tag.putString("TicketId", ticketId);
        tag.putString("StartStation", startStation);
        tag.putString("Destination", destination);
        tag.putLong("IssueTime", System.currentTimeMillis());
        tag.putInt("Price", price);
        tag.putString("Status", "UNUSED"); // 初始状态为未使用

        ticket.setTag(tag);

        itemHandler.setStackInSlot(0, ticket);
        setChanged();

        // 记录购买车票日志
        if (player != null) {
            String paymentMethod = "硬币"; // 可以根据实际支付方式调整
            TicketSystemLogger.logTicketPurchase(player, startStation, destination, price, ticketId, paymentMethod);
        }

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    public void dropContents(Level level, BlockPos pos) {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, inventory);
        
        // 同时掉落存储的硬币
        if (!storedCoins.isEmpty()) {
            for (Map.Entry<String, Integer> entry : storedCoins.entrySet()) {
                Item coin = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(entry.getKey()));
                if (coin != null) {
                    int count = entry.getValue();
                    while (count > 0) {
                        int amount = Math.min(count, coin.getMaxStackSize());
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(coin, amount));
                        count -= amount;
                    }
                }
            }
        }
    }
    
    // 添加硬币到存储
    public void addCoins(Map<String, Integer> coins) {
        for (Map.Entry<String, Integer> entry : coins.entrySet()) {
            String coinId = entry.getKey();
            int amount = entry.getValue();
            storedCoins.put(coinId, storedCoins.getOrDefault(coinId, 0) + amount);
        }
        setChanged();
    }
    
    // 取出所有存储的硬币
    public Map<String, Integer> withdrawCoins() {
        Map<String, Integer> coins = new HashMap<>(storedCoins);
        storedCoins.clear();
        setChanged();
        return coins;
    }
    
    // 获取存储的硬币
    public Map<String, Integer> getStoredCoins() {
        return Collections.unmodifiableMap(storedCoins);
    }
    
    // 获取硬币总价值（以铜币为单位）
    public int getTotalCopperValue() {
        int total = 0;
        for (Map.Entry<String, Integer> entry : storedCoins.entrySet()) {
            String coinId = entry.getKey();
            int amount = entry.getValue();
            int valuePerCoin = CoinSystem.getCoinValue(coinId);
            total += valuePerCoin * amount;
        }
        return total;
    }
    
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
    
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        handleUpdateTag(pkt.getTag());
    }
}
