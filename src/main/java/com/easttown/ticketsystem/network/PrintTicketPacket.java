package com.easttown.ticketsystem.network;

import com.easttown.ticketsystem.block.TicketMachineBlockEntity;
import com.easttown.ticketsystem.manager.CoinSystem;
import com.easttown.ticketsystem.manager.PriceCalculator;
import com.easttown.ticketsystem.manager.StationManager;
import com.easttown.ticketsystem.util.LanguageHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

public class PrintTicketPacket {
    private final BlockPos pos;
    private final String destination;
    
    public PrintTicketPacket(BlockPos pos, String destination) {
        this.pos = pos;
        this.destination = destination;
    }
    
    public static void encode(PrintTicketPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeUtf(packet.destination);
    }
    
    public static PrintTicketPacket decode(FriendlyByteBuf buffer) {
        return new PrintTicketPacket(buffer.readBlockPos(), buffer.readUtf());
    }
    
    public static void handle(PrintTicketPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                Level level = player.level();
                if (level.getBlockEntity(packet.pos) instanceof TicketMachineBlockEntity blockEntity) {
                    String startStation = blockEntity.getStartStation();
                    
                    if (startStation == null || startStation.isEmpty()) {
                        player.displayClientMessage(
                                LanguageHelper.translate("command.start_not_set")
                                        .copy().withStyle(ChatFormatting.RED), 
                                false
                        );
                        return;
                    }
                    
                    if (startStation.equals(packet.destination)) {
                        player.displayClientMessage(
                                LanguageHelper.translate("command.same_station")
                                        .copy().withStyle(ChatFormatting.RED), 
                                false
                        );
                        return;
                    }
                    
                    if (!StationManager.containsStation(packet.destination)) {
                        player.displayClientMessage(
                                LanguageHelper.translate("command.station_not_found", packet.destination)
                                        .copy().withStyle(ChatFormatting.RED), 
                                false
                        );
                        return;
                    }
                    
                    int price = PriceCalculator.calculatePrice(startStation, packet.destination);
                    
                    // 检查输出槽是否空闲
                    if (!blockEntity.canPrintTicket()) {
                        player.displayClientMessage(
                                LanguageHelper.translate("command.output_occupied")
                                        .copy().withStyle(ChatFormatting.RED), 
                                false
                        );
                        return;
                    }
                    
                    if (CoinSystem.hasSufficientCoins(player, price)) {
                        // 支付并获取实际扣除的硬币
                        Map<String, Integer> paidCoins = CoinSystem.deductWithChange(player, price);
                        
                        // 将硬币存入售票机
                        blockEntity.addCoins(paidCoins);
                        
                        blockEntity.printTicket(packet.destination, player);
                        player.displayClientMessage(
                                LanguageHelper.translate("command.ticket_printed", price), 
                                false
                        );
                    } else {
                        int deficit = price - CoinSystem.getPlayerCopperValue(player);
                        String deficitText = CoinSystem.formatDeficit(deficit);
                        
                        player.displayClientMessage(
                                LanguageHelper.translate("command.insufficient_coins", price, deficitText)
                                        .copy().withStyle(ChatFormatting.RED), 
                                false
                        );
                    }
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
