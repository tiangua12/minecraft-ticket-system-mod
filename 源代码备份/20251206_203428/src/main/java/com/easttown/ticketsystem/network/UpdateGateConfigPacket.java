package com.easttown.ticketsystem.network;

import com.easttown.ticketsystem.block.GateBlockEntity;
import com.easttown.ticketsystem.block.GateType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateGateConfigPacket {
    private final BlockPos pos;
    private final String gateId;
    private final String stationId;
    private final String gateType;
    private final boolean allowReentry;
    private final int maxTravelMinutes;
    private final boolean destroyTicket;
    private final boolean enabled;

    public UpdateGateConfigPacket(BlockPos pos, String gateId, String stationId, String gateType, boolean allowReentry, 
                                  int maxTravelMinutes, boolean destroyTicket, boolean enabled) {
        this.pos = pos;
        this.gateId = gateId;
        this.stationId = stationId;
        this.gateType = gateType;
        this.allowReentry = allowReentry;
        this.maxTravelMinutes = maxTravelMinutes;
        this.destroyTicket = destroyTicket;
        this.enabled = enabled;
    }

    public static void encode(UpdateGateConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeUtf(packet.gateId);
        buffer.writeUtf(packet.stationId);
        buffer.writeUtf(packet.gateType);
        buffer.writeBoolean(packet.allowReentry);
        buffer.writeInt(packet.maxTravelMinutes);
        buffer.writeBoolean(packet.destroyTicket);
        buffer.writeBoolean(packet.enabled);
    }

    public static UpdateGateConfigPacket decode(FriendlyByteBuf buffer) {
        return new UpdateGateConfigPacket(
            buffer.readBlockPos(),
            buffer.readUtf(),
            buffer.readUtf(),
            buffer.readUtf(),
            buffer.readBoolean(),
            buffer.readInt(),
            buffer.readBoolean(),
            buffer.readBoolean()
        );
    }

    public static void handle(UpdateGateConfigPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerLevel level = (ServerLevel) context.get().getSender().level();
            if (level.getBlockEntity(packet.pos) instanceof GateBlockEntity gate) {
                gate.setGateId(packet.gateId);
                gate.setStationId(packet.stationId);
                gate.setGateType(GateType.valueOf(packet.gateType));
                gate.setAllowReentry(packet.allowReentry);
                gate.setMaxTravelMinutes(packet.maxTravelMinutes);
                gate.setDestroyTicket(packet.destroyTicket);
                gate.setEnabled(packet.enabled);
                
                gate.setChanged();
                level.sendBlockUpdated(packet.pos, gate.getBlockState(), gate.getBlockState(), 3);
            }
        });
        context.get().setPacketHandled(true);
    }
}
