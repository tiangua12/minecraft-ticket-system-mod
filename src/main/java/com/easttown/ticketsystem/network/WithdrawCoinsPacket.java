package com.easttown.ticketsystem.network;

import com.easttown.ticketsystem.block.TicketMachineBlockEntity;
import com.easttown.ticketsystem.manager.CoinSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import com.easttown.ticketsystem.init.ItemInit; // 添加导入
import net.minecraft.network.chat.Component; // 添加导入
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.function.Supplier;

public class WithdrawCoinsPacket {
    private final BlockPos pos;
    
    public WithdrawCoinsPacket(BlockPos pos) {
        this.pos = pos;
    }
    
    public static void encode(WithdrawCoinsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }
    
    public static WithdrawCoinsPacket decode(FriendlyByteBuf buffer) {
        return new WithdrawCoinsPacket(buffer.readBlockPos());
    }
    
    public static void handle(WithdrawCoinsPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                if (player.level().getBlockEntity(packet.pos) instanceof TicketMachineBlockEntity blockEntity) {
                    // 检查是否是管理员
                    if (!player.getMainHandItem().getItem().equals(ItemInit.ADMIN_KEY.get())) {
                        player.displayClientMessage(Component.translatable("ticketsystem.command.not_admin"), true);
                        return;
                    }
                    
                    // 取出所有硬币
                    Map<String, Integer> coins = blockEntity.withdrawCoins();
                    int totalValue = blockEntity.getTotalCopperValue();
                    
                    if (coins.isEmpty()) {
                        player.displayClientMessage(Component.translatable("ticketsystem.command.no_coins"), true);
                        return;
                    }
                    
                    // 给予玩家硬币
                    for (Map.Entry<String, Integer> entry : coins.entrySet()) {
                        Item coinItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(entry.getKey()));
                        if (coinItem != null) {
                            int amount = entry.getValue();
                            while (amount > 0) {
                                int stackSize = Math.min(coinItem.getMaxStackSize(), amount);
                                ItemStack stack = new ItemStack(coinItem, stackSize);
                                if (!player.addItem(stack)) {
                                    player.drop(stack, false);
                                }
                                amount -= stackSize;
                            }
                        }
                    }
                    
                    player.displayClientMessage(Component.translatable("ticketsystem.command.coins_withdrawn", totalValue), false);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
