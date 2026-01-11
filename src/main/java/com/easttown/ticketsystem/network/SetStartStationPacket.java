package com.easttown.ticketsystem.network;

import com.easttown.ticketsystem.block.TicketMachineBlockEntity;
import com.easttown.ticketsystem.util.LanguageHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetStartStationPacket {
    private final BlockPos pos;
    private final String station;
    
    public SetStartStationPacket(BlockPos pos, String station) {
        this.pos = pos;
        this.station = station;
    }
    
    public static void encode(SetStartStationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeUtf(packet.station);
    }
    
    public static SetStartStationPacket decode(FriendlyByteBuf buffer) {
        return new SetStartStationPacket(buffer.readBlockPos(), buffer.readUtf());
    }
    
    public static void handle(SetStartStationPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                Level level = player.level();
                if (level.getBlockEntity(packet.pos) instanceof TicketMachineBlockEntity blockEntity) {
                    blockEntity.setStartStation(packet.station);
                    player.displayClientMessage(
                        LanguageHelper.translate("command.start_set", packet.station), false);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}