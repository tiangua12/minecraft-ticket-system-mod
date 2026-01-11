package com.easttown.ticketsystem.block;

import com.easttown.ticketsystem.init.BlockEntityInit;
import com.easttown.ticketsystem.init.MenuInit;
import com.easttown.ticketsystem.init.ItemInit;
import com.easttown.ticketsystem.item.TicketItem;
import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.manager.CoinSystem;
import com.easttown.ticketsystem.util.LanguageHelper;
import com.easttown.ticketsystem.util.TicketSystemLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReissueMachineBlockEntity extends BlockEntity implements MenuProvider, Container {

    // 物品槽位定义
    // 槽位0：车票输入槽
    // 槽位1-9：硬币输出槽（共9个）
    // 槽位10-63：硬币存储槽（管理员界面，54个槽位，6x9布局）
    // 退票时车票会被销毁，不需要输出槽
    private final ItemStackHandler itemHandler = new ItemStackHandler(64) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                // 强制发送数据包更新到客户端
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);

                // 当车票放入输入槽时，确保客户端及时更新
                if (slot == 0) {
                    ItemStack ticketStack = getTicketInputSlot();
                    if (!ticketStack.isEmpty() && ticketStack.getItem() == ItemInit.TICKET.get()) {
                        // 车票详情现在在GUI中显示，确保客户端及时更新
                        com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: 车票放入槽位，发送数据包更新");

                        // 强制同步数据到所有正在查看GUI的玩家
                        if (level.getServer() != null) {
                            for (net.minecraft.server.level.ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                                if (player.containerMenu instanceof com.easttown.ticketsystem.screen.terminal.menu.ReissueMachineMainMenu) {
                                    player.connection.send(getUpdatePacket());
                                }
                            }
                        }
                    }
                }
            }
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            // 槽位0：车票输入槽（只能放入未使用的车票）
            if (slot == 0) {
                return stack.getItem() == ItemInit.TICKET.get() && TicketItem.isUnused(stack);
            }
            // 槽位1-9：硬币输出槽（只能由机器放入）
            if (slot >= 1 && slot <= 9) {
                return false;
            }
            // 槽位10-63：硬币存储槽（只能放入硬币）
            if (slot >= 10 && slot <= 63) {
                String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                return CoinSystem.isCoin(itemId);
            }
            return false;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 所有槽位都允许提取，包括车票输入槽
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

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    // 硬币存储系统
    private final Map<String, Integer> storedCoins = new HashMap<>();

    public ReissueMachineBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.REISSUE_MACHINE.get(), pos, state);
        // 调试信息：输出初始化时的槽位数量
        com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity created with " + itemHandler.getSlots() + " slots");
    }

    // MenuProvider接口实现
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ticketsystem.reissue_machine");
    }

    public Component getDisplayName(Player player) {

        if (isAdmin(player)) {
            return Component.translatable("block.ticketsystem.reissue_machine");
        } else {
            return Component.translatable("block.ticketsystem.reissue_machine.is_adminmode");
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        // 检查是否是管理员，如果是则打开管理员菜单，否则打开主菜单
        if (isAdmin(player)) {
            return new com.easttown.ticketsystem.screen.terminal.menu.ReissueMachineAdminMenu(containerId, inventory, this);
        } else {
            return new com.easttown.ticketsystem.screen.terminal.menu.ReissueMachineMainMenu(containerId, inventory, this);
        }
    }

    // 客户端打开屏幕的方法
    public void openScreen(Player player) {
        if (player.level().isClientSide()) {
            // 直接打开主菜单界面
            player.openMenu(this);
        }
    }

    // 检查是否是管理员（持有管理员钥匙）
    public boolean isAdmin(Player player) {
        if (player == null) return false;

        // 检查主手和副手是否持有管理员钥匙
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return (mainHand.getItem() == com.easttown.ticketsystem.init.ItemInit.ADMIN_KEY.get()) ||
                (offHand.getItem() == com.easttown.ticketsystem.init.ItemInit.ADMIN_KEY.get());
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

        // 简单的NBT修复：如果槽位数量不对，重新创建
        CompoundTag inventoryTag = tag.getCompound("inventory");
        if (inventoryTag.contains("Size") && inventoryTag.getInt("Size") != 64) {
            int oldSize = inventoryTag.getInt("Size");
            com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: NBT数据槽位数量不匹配，从 " + oldSize + " 修复到 64");

            // 创建新的64槽位handler
            ItemStackHandler newHandler = new ItemStackHandler(64);

            // 迁移旧数据
            ItemStackHandler oldHandler = new ItemStackHandler(oldSize);
            oldHandler.deserializeNBT(inventoryTag);

            int migratedCount = 0;
            for (int i = 0; i < Math.min(oldHandler.getSlots(), 64); i++) {
                ItemStack stack = oldHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    newHandler.setStackInSlot(i, stack);
                    migratedCount++;
                }
            }

            itemHandler.deserializeNBT(newHandler.serializeNBT());
            com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: NBT数据迁移完成，迁移了 " + migratedCount + " 个槽位");
        } else {
            itemHandler.deserializeNBT(inventoryTag);
            com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: NBT数据加载完成，当前槽位数量: " + itemHandler.getSlots());
        }

        // 加载硬币存储
        storedCoins.clear();
        CompoundTag coinsTag = tag.getCompound("StoredCoins");
        for (String key : coinsTag.getAllKeys()) {
            storedCoins.put(key, coinsTag.getInt(key));
        }
    }

    // 槽位访问方法
    public ItemStack getTicketInputSlot() {
        return itemHandler.getStackInSlot(0);
    }

    public ItemStack getCoinOutputSlot(int index) {
        if (index >= 1 && index <= 9) {
            return itemHandler.getStackInSlot(index);
        }
        return ItemStack.EMPTY;
    }

    // 获取所有硬币输出槽
    public ItemStack[] getAllCoinOutputSlots() {
        ItemStack[] slots = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            slots[i] = itemHandler.getStackInSlot(i + 1);
        }
        return slots;
    }

    // 检查车票NBT完整性
    private boolean isTicketNBTComplete(ItemStack ticket) {
        if (ticket.isEmpty() || ticket.getTag() == null) {
            return false;
        }

        CompoundTag tag = ticket.getTag();
        return tag.contains("TicketId") &&
                tag.contains("StartStation") &&
                tag.contains("Destination") &&
                tag.contains("IssueTime") &&
                tag.contains("Price") &&
                tag.contains("Status");
    }

    // 退票功能
    public boolean canRefundTicket() {
        // 检查输入槽有未使用的车票
        ItemStack inputTicket = getTicketInputSlot();

        if (inputTicket.isEmpty() || !TicketItem.isUnused(inputTicket)) {
            return false;
        }

        // 检查车票NBT完整性
        if (!isTicketNBTComplete(inputTicket)) {
            return false;
        }

        // 不再检查硬币输出槽是否为空，允许堆叠硬币
        return true;
    }

    public boolean refundTicket(Player player) {
        if (!canRefundTicket()) {
            return false;
        }

        ItemStack inputTicket = getTicketInputSlot();
        CompoundTag tag = inputTicket.getTag();
        if (tag == null) {
            return false;
        }

        // 获取车票价格
        int price = tag.getInt("Price");
        long issueTime = tag.getLong("IssueTime");

        // 检查车票是否过期（例如超过24小时不可退票）
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - issueTime;
        long maxRefundTime = 24 * 60 * 60 * 1000; // 24小时

        if (timeDiff > maxRefundTime) {
            return false; // 车票已过期，不可退票
        }

        // 计算退款金额（可设置退款手续费，这里全额退款）
        int refundAmount = price;

        // 检查是否有足够的硬币进行退款
        if (getTotalCoinsValue() < refundAmount) {
            return false;
        }

        // 从存储中扣除硬币并获取实际扣除的硬币组合
        Map<String, Integer> deductedCoins = deductCoinsAndGet(refundAmount);
        if (deductedCoins == null) {
            return false;
        }

        // 销毁车票（清空输入槽）
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);

        // 将硬币放入输出槽，多余的放入玩家物品栏
        // 按价值从高到低排序硬币类型
        List<Map.Entry<String, Integer>> sortedCoins = new ArrayList<>(deductedCoins.entrySet());
        sortedCoins.sort((a, b) -> {
            int valueA = CoinSystem.getCoinValue(a.getKey());
            int valueB = CoinSystem.getCoinValue(b.getKey());
            return Integer.compare(valueB, valueA); // 降序排序
        });

        // 首先尝试放入输出槽（支持堆叠）
        for (Map.Entry<String, Integer> entry : sortedCoins) {
            String coinId = entry.getKey();
            int count = entry.getValue();
            if (count > 0) {
                net.minecraft.world.item.Item coinItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(coinId));
                if (coinItem != null) {
                    // 尝试将硬币堆叠到现有的输出槽中
                    boolean coinsPlaced = false;

                    // 首先检查是否有相同类型的硬币可以堆叠
                    for (int slot = 1; slot <= 9 && count > 0; slot++) {
                        ItemStack existingStack = itemHandler.getStackInSlot(slot);
                        if (!existingStack.isEmpty() && existingStack.getItem() == coinItem &&
                                existingStack.getCount() < existingStack.getMaxStackSize()) {
                            // 可以堆叠到现有槽位
                            int spaceLeft = existingStack.getMaxStackSize() - existingStack.getCount();
                            int toAdd = Math.min(count, spaceLeft);
                            existingStack.grow(toAdd);
                            count -= toAdd;
                            coinsPlaced = true;
                        }
                    }

                    // 如果没有可以堆叠的槽位，尝试放入空槽位
                    for (int slot = 1; slot <= 9 && count > 0; slot++) {
                        ItemStack existingStack = itemHandler.getStackInSlot(slot);
                        if (existingStack.isEmpty()) {
                            // 空槽位，直接放入
                            int toAdd = Math.min(count, coinItem.getMaxStackSize());
                            itemHandler.setStackInSlot(slot, new ItemStack(coinItem, toAdd));
                            count -= toAdd;
                            coinsPlaced = true;
                        }
                    }

                    // 如果输出槽已满或无法堆叠，放入玩家物品栏
                    if (count > 0) {
                        ItemStack remainingCoins = new ItemStack(coinItem, count);
                        if (player != null) {
                            if (!player.getInventory().add(remainingCoins)) {
                                // 如果玩家物品栏也满了，掉落在地上
                                player.drop(remainingCoins, false);
                            }
                            com.easttown.ticketsystem.util.DebugLogger.info("硬币输出槽已满，硬币已放入玩家物品栏");
                        }
                    }
                }
            }
        }

        setChanged();

        // 成功时输出聊天消息
        if (player != null) {
            player.displayClientMessage(LanguageHelper.translate("reissue.refund_success", price), false);
        }

        return true;
    }

    // 硬币管理方法
    public void addCoins(Map<String, Integer> coins) {
        for (Map.Entry<String, Integer> entry : coins.entrySet()) {
            String coinId = entry.getKey();
            int amount = entry.getValue();
            storedCoins.put(coinId, storedCoins.getOrDefault(coinId, 0) + amount);
        }
        setChanged();
    }

    // 获取硬币存储槽中的硬币
    public Map<String, Integer> getCoinsFromStorageSlots() {
        Map<String, Integer> coins = new HashMap<>();
        for (int slot = 10; slot <= 63; slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                String coinId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                coins.put(coinId, coins.getOrDefault(coinId, 0) + stack.getCount());
            }
        }
        return coins;
    }

    // 获取总硬币价值（包括存储槽中的硬币）
    public int getTotalCoinsValue() {
        int total = 0;

        // 计算存储槽中的硬币价值
        for (int slot = 10; slot <= 63; slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                String coinId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                int valuePerCoin = CoinSystem.getCoinValue(coinId);
                total += valuePerCoin * stack.getCount();
            }
        }

        // 加上内部存储的硬币价值
        for (Map.Entry<String, Integer> entry : storedCoins.entrySet()) {
            String coinId = entry.getKey();
            int amount = entry.getValue();
            int valuePerCoin = CoinSystem.getCoinValue(coinId);
            total += valuePerCoin * amount;
        }

        return total;
    }

    private boolean deductCoins(int amount) {
        if (getTotalCoinsValue() < amount) {
            return false;
        }

        // 计算需要扣除的硬币组合
        Map<String, Integer> coinsToDeduct = CoinSystem.calculateOptimalCoins(amount);

        // 首先尝试从存储槽中扣除硬币
        for (Map.Entry<String, Integer> entry : coinsToDeduct.entrySet()) {
            String coinId = entry.getKey();
            int required = entry.getValue();

            // 从存储槽中扣除
            for (int slot = 10; slot <= 63 && required > 0; slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    String stackCoinId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                    if (stackCoinId.equals(coinId)) {
                        int available = stack.getCount();
                        int toDeduct = Math.min(available, required);
                        stack.shrink(toDeduct);
                        required -= toDeduct;
                        if (stack.isEmpty()) {
                            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                        }
                    }
                }
            }

            // 如果存储槽中不足，尝试自动换钱
            if (required > 0) {
                if (!exchangeCoinsForSmaller(coinId, required)) {
                    return false;
                }
            }
        }

        setChanged();
        return true;
    }

    // 自动将大面额硬币换成小面额硬币
    private boolean exchangeCoinsForSmaller(String targetCoinId, int required) {
        // 获取目标硬币的价值
        int targetValue = CoinSystem.getCoinValue(targetCoinId);
        int requiredValue = targetValue * required;

        // 尝试从存储槽中寻找更大面额的硬币
        List<String> coinTypes = new ArrayList<>(CoinSystem.getCoinValues().keySet());
        Collections.sort(coinTypes, (a, b) -> Integer.compare(CoinSystem.getCoinValue(b), CoinSystem.getCoinValue(a)));

        for (String largerCoinId : coinTypes) {
            int largerValue = CoinSystem.getCoinValue(largerCoinId);
            if (largerValue <= targetValue) continue; // 跳过相同或更小的面额

            // 计算需要多少个大面额硬币
            int neededLargerCoins = (int) Math.ceil((double) requiredValue / largerValue);

            // 检查存储槽中是否有足够的大面额硬币
            int availableLargerCoins = 0;
            for (int slot = 10; slot <= 63; slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    String stackCoinId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                    if (stackCoinId.equals(largerCoinId)) {
                        availableLargerCoins += stack.getCount();
                    }
                }
            }

            if (availableLargerCoins >= neededLargerCoins) {
                // 兑换大面额硬币
                int remainingToExchange = neededLargerCoins;
                for (int slot = 10; slot <= 63 && remainingToExchange > 0; slot++) {
                    ItemStack stack = itemHandler.getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        String stackCoinId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                        if (stackCoinId.equals(largerCoinId)) {
                            int toExchange = Math.min(stack.getCount(), remainingToExchange);
                            stack.shrink(toExchange);
                            remainingToExchange -= toExchange;
                            if (stack.isEmpty()) {
                                itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                            }
                        }
                    }
                }

                // 计算找零（大面额硬币价值 - 所需价值）
                int changeValue = (neededLargerCoins * largerValue) - requiredValue;
                if (changeValue > 0) {
                    // 将找零放入存储槽
                    Map<
                            String,
                            Integer> changeCoins = CoinSystem.calculateOptimalCoins(changeValue);
                    addCoinsToStorageSlots(changeCoins);
                }

                return true;
            }
        }

        return false;
    }

    // 将硬币添加到存储槽
    private void addCoinsToStorageSlots(Map<String, Integer> coins) {
        for (Map.Entry<String, Integer> entry : coins.entrySet()) {
            String coinId = entry.getKey();
            int count = entry.getValue();
            if (count > 0) {
                net.minecraft.world.item.Item coinItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(coinId));
                if (coinItem != null) {
                    // 尝试将硬币放入存储槽
                    for (int slot = 10; slot <= 63 && count > 0; slot++) {
                        ItemStack existingStack = itemHandler.getStackInSlot(slot);
                        if (existingStack.isEmpty()) {
                            // 空槽位，直接放入
                            int toAdd = Math.min(count, coinItem.getMaxStackSize());
                            itemHandler.setStackInSlot(slot, new ItemStack(coinItem, toAdd));
                            count -= toAdd;
                        } else if (existingStack.getItem() == coinItem && existingStack.getCount() < existingStack.getMaxStackSize()) {
                            // 相同物品，可以堆叠
                            int spaceLeft = existingStack.getMaxStackSize() - existingStack.getCount();
                            int toAdd = Math.min(count, spaceLeft);
                            existingStack.grow(toAdd);
                            count -= toAdd;
                        }
                    }
                }
            }
        }
    }

    // 扣除硬币并返回实际扣除的硬币组合
    private Map<String, Integer> deductCoinsAndGet(int amount) {
        if (getTotalCoinsValue() < amount) {
            return null;
        }

        // 计算需要扣除的硬币组合
        Map<String, Integer> coinsToDeduct = CoinSystem.calculateOptimalCoins(amount);
        Map<String, Integer> actualDeducted = new HashMap<>();

        // 首先尝试从存储槽中扣除硬币
        for (Map.Entry<String, Integer> entry : coinsToDeduct.entrySet()) {
            String coinId = entry.getKey();
            int required = entry.getValue();
            int actuallyDeducted = 0;

            // 从存储槽中扣除
            for (int slot = 10; slot <= 63 && required > 0; slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    String stackCoinId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                    if (stackCoinId.equals(coinId)) {
                        int available = stack.getCount();
                        int toDeduct = Math.min(available, required);
                        stack.shrink(toDeduct);
                        actuallyDeducted += toDeduct;
                        required -= toDeduct;
                        if (stack.isEmpty()) {
                            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                        }
                    }
                }
            }

            // 记录实际扣除的数量
            if (actuallyDeducted > 0) {
                actualDeducted.put(coinId, actuallyDeducted);
            }

            // 如果存储槽中不足，尝试自动换钱
            if (required > 0) {
                Map<
                        String,
                        Integer> exchangedCoins = exchangeCoinsForSmallerAndGet(coinId, required);
                if (exchangedCoins == null) {
                    return null;
                }
                // 合并兑换得到的硬币
                for (Map.Entry<String, Integer> exchangeEntry : exchangedCoins.entrySet()) {
                    actualDeducted.put(exchangeEntry.getKey(),
                            actualDeducted.getOrDefault(exchangeEntry.getKey(), 0) + exchangeEntry.getValue());
                }
            }
        }

        setChanged();
        return actualDeducted;
    }

    // 自动将大面额硬币换成小面额硬币并返回实际兑换的硬币
    private Map<String, Integer> exchangeCoinsForSmallerAndGet(String targetCoinId, int required) {
        // 获取目标硬币的价值
        int targetValue = CoinSystem.getCoinValue(targetCoinId);
        int requiredValue = targetValue * required;

        // 尝试从存储槽中寻找更大面额的硬币
        List<String> coinTypes = new ArrayList<>(CoinSystem.getCoinValues().keySet());
        Collections.sort(coinTypes, (a, b) -> Integer.compare(CoinSystem.getCoinValue(b), CoinSystem.getCoinValue(a)));

        for (String largerCoinId : coinTypes) {
            int largerValue = CoinSystem.getCoinValue(largerCoinId);
            if (largerValue <= targetValue) continue; // 跳过相同或更小的面额

            // 计算需要多少个大面额硬币
            int neededLargerCoins = (int) Math.ceil((double) requiredValue / largerValue);

            // 检查存储槽中是否有足够的大面额硬币
            int availableLargerCoins = 0;
            for (int slot = 10; slot <= 63; slot++) {
                ItemStack stack = itemHandler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    String stackCoinId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                    if (stackCoinId.equals(largerCoinId)) {
                        availableLargerCoins += stack.getCount();
                    }
                }
            }

            if (availableLargerCoins >= neededLargerCoins) {
                // 兑换大面额硬币
                int remainingToExchange = neededLargerCoins;
                for (int slot = 10; slot <= 63 && remainingToExchange > 0; slot++) {
                    ItemStack stack = itemHandler.getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        String stackCoinId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                        if (stackCoinId.equals(largerCoinId)) {
                            int toExchange = Math.min(stack.getCount(), remainingToExchange);
                            stack.shrink(toExchange);
                            remainingToExchange -= toExchange;
                            if (stack.isEmpty()) {
                                itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                            }
                        }
                    }
                }

                // 计算找零（大面额硬币价值 - 所需价值）
                int changeValue = (neededLargerCoins * largerValue) - requiredValue;
                Map<String, Integer> result = new HashMap<>();
                result.put(targetCoinId, required);

                if (changeValue > 0) {
                    // 将找零放入存储槽
                    Map<
                            String,
                            Integer> changeCoins = CoinSystem.calculateOptimalCoins(changeValue);
                    addCoinsToStorageSlots(changeCoins);
                }

                return result;
            }
        }

        return null;
    }

    public Map<String, Integer> withdrawCoins() {
        Map<String, Integer> coins = new HashMap<>(storedCoins);
        storedCoins.clear();
        setChanged();
        return coins;
    }

    // 退币功能 - 根据车票价格退款
    public RefundResult refundTicketByPrice(Player player) {
        com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: 开始处理退票请求，玩家: " + player.getName().getString());

        // 获取车票输入槽
        ItemStack inputTicket = getTicketInputSlot();

        // 检查是否有车票
        if (inputTicket.isEmpty()) {
            com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: 退票失败 - 没有车票");
            TicketSystemLogger.logWarning(TicketSystemLogger.LogType.REFUND, "退票失败 - 没有车票，玩家: " + player.getName().getString());
            return RefundResult.NO_TICKET;
        }

        if (!TicketItem.isUnused(inputTicket)) {
            com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: 退票失败 - 车票已使用");
            TicketSystemLogger.logWarning(TicketSystemLogger.LogType.REFUND, "退票失败 - 车票已使用，玩家: " + player.getName().getString());
            return RefundResult.INVALID_TICKET;
        }

        // 检查车票NBT完整性
        if (!isTicketNBTComplete(inputTicket)) {
            com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: 退票失败 - 车票NBT数据不完整");
            TicketSystemLogger.logWarning(TicketSystemLogger.LogType.REFUND, "退票失败 - 车票NBT数据不完整，玩家: " + player.getName().getString());
            return RefundResult.INVALID_TICKET;
        }

        CompoundTag tag = inputTicket.getTag();
        if (tag == null) {
            com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: 退票失败 - 车票没有NBT数据");
            TicketSystemLogger.logWarning(TicketSystemLogger.LogType.REFUND, "退票失败 - 车票没有NBT数据，玩家: " + player.getName().getString());
            return RefundResult.INVALID_TICKET;
        }

        // 获取车票价格和购买时间
        int price = tag.getInt("Price");
        long issueTime = tag.getLong("IssueTime");
        String ticketId = tag.contains("TicketId") ? tag.getString("TicketId") : "未知";
        String startStation = tag.contains("StartStation") ? tag.getString("StartStation") : "未知";
        String destination = tag.contains("Destination") ? tag.getString("Destination") : "未知";

        if (price <= 0) {
            TicketSystemLogger.logWarning(TicketSystemLogger.LogType.REFUND, "退票失败 - 车票价格无效，玩家: " + player.getName().getString());
            return RefundResult.INVALID_TICKET;
        }

        // 计算退款金额（阶梯式退款）
        int refundAmount = calculateRefundAmount(price, issueTime);
        com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: 车票价格: " + price + ", 退款金额: " + refundAmount);

        if (refundAmount <= 0) {
            com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: 退票失败 - 车票已过期");
            TicketSystemLogger.logWarning(TicketSystemLogger.LogType.REFUND,
                    "退票失败 - 车票已过期，玩家: " + player.getName().getString() +
                            ", 车票ID: " + ticketId +
                            ", 原价: " + price);
            return RefundResult.TICKET_EXPIRED;
        }

        // 检查存储中是否有足够的硬币
        int totalCoins = getTotalCoinsValue();
        com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: 存储硬币总价值: " + totalCoins + ", 需要: " + refundAmount);

        if (totalCoins < refundAmount) {
            com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: 退票失败 - 硬币不足");
            TicketSystemLogger.logWarning(TicketSystemLogger.LogType.REFUND,
                    "退票失败 - 硬币不足，玩家: " + player.getName().getString() +
                            ", 车票ID: " + ticketId +
                            ", 需要: " + refundAmount +
                            ", 实际: " + totalCoins);
            return RefundResult.INSUFFICIENT_COINS;
        }

        // 从存储中扣除硬币并获取实际扣除的硬币组合
        Map<String, Integer> deductedCoins = deductCoinsAndGet(refundAmount);
        if (deductedCoins == null) {
            TicketSystemLogger.logWarning(TicketSystemLogger.LogType.REFUND,
                    "退票失败 - 硬币扣除失败，玩家: " + player.getName().getString() +
                            ", 车票ID: " + ticketId);
            return RefundResult.INSUFFICIENT_COINS;
        }

        // 销毁车票
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);

        // 将硬币放入输出槽，多余的放入玩家物品栏
        // 按价值从高到低排序硬币类型
        List<Map.Entry<String, Integer>> sortedCoins = new ArrayList<>(deductedCoins.entrySet());
        sortedCoins.sort((a, b) -> {
            int valueA = CoinSystem.getCoinValue(a.getKey());
            int valueB = CoinSystem.getCoinValue(b.getKey());
            return Integer.compare(valueB, valueA); // 降序排序
        });

        // 首先尝试放入输出槽（支持堆叠）
        for (Map.Entry<String, Integer> entry : sortedCoins) {
            String coinId = entry.getKey();
            int count = entry.getValue();
            if (count > 0) {
                net.minecraft.world.item.Item coinItem = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(coinId));
                if (coinItem != null) {
                    // 尝试将硬币堆叠到现有的输出槽中
                    boolean coinsPlaced = false;

                    // 首先检查是否有相同类型的硬币可以堆叠
                    for (int slot = 1; slot <= 9 && count > 0; slot++) {
                        ItemStack existingStack = itemHandler.getStackInSlot(slot);
                        if (!existingStack.isEmpty() && existingStack.getItem() == coinItem &&
                                existingStack.getCount() < existingStack.getMaxStackSize()) {
                            // 可以堆叠到现有槽位
                            int spaceLeft = existingStack.getMaxStackSize() - existingStack.getCount();
                            int toAdd = Math.min(count, spaceLeft);
                            existingStack.grow(toAdd);
                            count -= toAdd;
                            coinsPlaced = true;
                        }
                    }

                    // 如果没有可以堆叠的槽位，尝试放入空槽位
                    for (int slot = 1; slot <= 9 && count > 0; slot++) {
                        ItemStack existingStack = itemHandler.getStackInSlot(slot);
                        if (existingStack.isEmpty()) {
                            // 空槽位，直接放入
                            int toAdd = Math.min(count, coinItem.getMaxStackSize());
                            itemHandler.setStackInSlot(slot, new ItemStack(coinItem, toAdd));
                            count -= toAdd;
                            coinsPlaced = true;
                        }
                    }

                    // 如果输出槽已满或无法堆叠，放入玩家物品栏
                    if (count > 0) {
                        ItemStack remainingCoins = new ItemStack(coinItem, count);
                        if (player != null) {
                            if (!player.getInventory().add(remainingCoins)) {
                                // 如果玩家物品栏也满了，掉落在地上
                                player.drop(remainingCoins, false);
                            }
                            com.easttown.ticketsystem.util.DebugLogger.info("硬币输出槽已满，硬币已放入玩家物品栏");
                        }
                    }
                }
            }
        }

        setChanged();

        // 成功时输出聊天消息
        if (player != null) {
            player.displayClientMessage(LanguageHelper.translate("reissue.refund_success", refundAmount), false);
        }

        com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineBlockEntity: 退票成功，退款金额: " + refundAmount);

        // 记录详细的退票成功日志
        String refundReason = calculateRefundReason(price, issueTime);
        TicketSystemLogger.logRefund(player, ticketId, price, refundAmount, refundReason);

        return RefundResult.SUCCESS;
    }

    // 计算退款原因
    private String calculateRefundReason(int originalPrice, long issueTime) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - issueTime;

        // 时间阈值（毫秒）
        long halfHour = 30 * 60 * 1000; // 30分钟
        long oneHour = 60 * 60 * 1000; // 1小时
        long twoHours = 2 * 60 * 60 * 1000; // 2小时
        long sixHours = 6 * 60 * 60 * 1000; // 6小时
        long oneDay = 24 * 60 * 60 * 1000; // 24小时

        if (timeDiff <= halfHour) {
            return "30分钟内全额退款";
        } else if (timeDiff <= oneHour) {
            return "1小时内75%退款";
        } else if (timeDiff <= twoHours) {
            return "2小时内50%退款";
        } else if (timeDiff <= sixHours) {
            return "6小时内25%退款";
        } else if (timeDiff <= oneDay) {
            return "24小时内10%退款";
        } else {
            return "超过24小时不可退款";
        }
    }

    public Map<String, Integer> getStoredCoins() {
        return Collections.unmodifiableMap(storedCoins);
    }

    // 计算阶梯式退款金额
    private int calculateRefundAmount(int originalPrice, long issueTime) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - issueTime;

        // 时间阈值（毫秒）
        long halfHour = 30 * 60 * 1000; // 30分钟
        long oneHour = 60 * 60 * 1000; // 1小时
        long twoHours = 2 * 60 * 60 * 1000; // 2小时
        long sixHours = 6 * 60 * 60 * 1000; // 6小时
        long oneDay = 24 * 60 * 60 * 1000; // 24小时

        // 阶梯式退款比例（从配置文件读取）
        if (timeDiff <= halfHour) {
            // 30分钟内：全额退款
            return (int) (originalPrice * com.easttown.ticketsystem.config.TicketSystemConfig.getRefundHalfHourRate());
        } else if (timeDiff <= oneHour) {
            // 1小时内：75%退款
            return (int) (originalPrice * com.easttown.ticketsystem.config.TicketSystemConfig.getRefundOneHourRate());
        } else if (timeDiff <= twoHours) {
            // 2小时内：50%退款
            return (int) (originalPrice * com.easttown.ticketsystem.config.TicketSystemConfig.getRefundTwoHoursRate());
        } else if (timeDiff <= sixHours) {
            // 6小时内：25%退款
            return (int) (originalPrice * com.easttown.ticketsystem.config.TicketSystemConfig.getRefundSixHoursRate());
        } else if (timeDiff <= oneDay) {
            // 24小时内：10%退款
            return (int) (originalPrice * com.easttown.ticketsystem.config.TicketSystemConfig.getRefundOneDayRate());
        } else {
            // 超过24小时：不可退款
            return 0;
        }
    }

    // 获取车票详情信息（用于GUI显示）
    public Map<String, String> getTicketDetails() {
        Map<String, String> details = new HashMap<>();
        ItemStack ticketStack = getTicketInputSlot();

        if (ticketStack.isEmpty() || ticketStack.getTag() == null) {
            details.put("hasTicket", "false");
            return details;
        }

        CompoundTag tag = ticketStack.getTag();

        // 获取车票信息
        details.put("hasTicket", "true");
        details.put("ticketId", tag.contains("TicketId") ? tag.getString("TicketId") : "未知");
        details.put("startStation", tag.contains("StartStation") ? tag.getString("StartStation") : "未知");
        details.put("destination", tag.contains("Destination") ? tag.getString("Destination") : "未知");
        details.put("price", tag.contains("Price") ? String.valueOf(tag.getInt("Price")) : "0");
        details.put("status", tag.contains("Status") ? tag.getString("Status") : "未知");

        // 格式化时间
        long issueTime = tag.contains("IssueTime") ? tag.getLong("IssueTime") : 0;
        details.put("issueTime", formatTime(issueTime));

        return details;
    }

    // 格式化时间
    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "未知";
        }

        java.util.Date date = new java.util.Date(timestamp);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

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

    // 掉落物品
    public void dropContents(Level level, BlockPos pos) {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(level, pos, inventory);

        // 同时掉落存储的硬币
        if (!storedCoins.isEmpty()) {
            for (Map.Entry<String, Integer> entry : storedCoins.entrySet()) {
                net.minecraft.world.item.Item coin = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(entry.getKey()));
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

    // 数据同步方法
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    // Container接口实现
    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        // 添加边界检查，防止访问无效槽位
        if (slot < 0 || slot >= itemHandler.getSlots()) {
            // 输出警告日志
            TicketSystemMod.LOGGER.warn("ReissueMachineBlockEntity: Attempting to access slot " + slot + ", but only " + itemHandler.getSlots() + " slots available");
            return ItemStack.EMPTY;
        }
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        // 添加边界检查，防止访问无效槽位
        if (slot < 0 || slot >= itemHandler.getSlots()) {
            // 输出警告日志
            TicketSystemMod.LOGGER.warn("ReissueMachineBlockEntity: Attempting to access slot " + slot + ", but only " + itemHandler.getSlots() + " slots available");
            return ItemStack.EMPTY;
        }
        return itemHandler.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        // 添加边界检查，防止访问无效槽位
        if (slot < 0 || slot >= itemHandler.getSlots()) {
            // 输出警告日志
            TicketSystemMod.LOGGER.warn("ReissueMachineBlockEntity: Attempting to access slot " + slot + ", but only " + itemHandler.getSlots() + " slots available");
            return ItemStack.EMPTY;
        }
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // 添加边界检查，防止访问无效槽位
        if (slot < 0 || slot >= itemHandler.getSlots()) {
            // 输出警告日志
            TicketSystemMod.LOGGER.warn("ReissueMachineBlockEntity: Attempting to access slot " + slot + ", but only " + itemHandler.getSlots() + " slots available");
            return;
        }
        itemHandler.setStackInSlot(slot, stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level == null || this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr((double) this.worldPosition.getX() + 0.5D,
                        (double) this.worldPosition.getY() + 0.5D,
                        (double) this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public void startOpen(Player player) {
        // 可选实现
    }

    @Override
    public void stopOpen(Player player) {
        // 可选实现
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // 添加边界检查，防止访问无效槽位
        if (slot < 0 || slot >= itemHandler.getSlots()) {
            // 输出警告日志
            TicketSystemMod.LOGGER.warn("ReissueMachineBlockEntity: Attempting to access slot " + slot + ", but only " + itemHandler.getSlots() + " slots available");
            return false;
        }
        return itemHandler.isItemValid(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    // Container接口的监听器方法 - 可选实现
    public void addSlotListener(ContainerListener listener) {
        // 可选实现
    }

    public void removeSlotListener(ContainerListener listener) {
        // 可选实现
    }

    // 退款结果枚举
    public enum RefundResult {
        SUCCESS,
        NO_TICKET,
        INVALID_TICKET,
        INSUFFICIENT_COINS,
        TICKET_EXPIRED
    }
}