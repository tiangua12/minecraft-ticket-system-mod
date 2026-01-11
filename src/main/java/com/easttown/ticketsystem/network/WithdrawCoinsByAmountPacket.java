package com.easttown.ticketsystem.network;

import com.easttown.ticketsystem.block.ReissueMachineBlockEntity;
import com.easttown.ticketsystem.util.LanguageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WithdrawCoinsByAmountPacket {
    private final BlockPos pos;

    public WithdrawCoinsByAmountPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(WithdrawCoinsByAmountPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    public static WithdrawCoinsByAmountPacket decode(FriendlyByteBuf buffer) {
        return new WithdrawCoinsByAmountPacket(buffer.readBlockPos());
    }

    public static void handle(WithdrawCoinsByAmountPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                com.easttown.ticketsystem.util.DebugLogger.info("WithdrawCoinsByAmountPacket: 收到退票请求，玩家: " + player.getName().getString() + ", 位置: " + packet.pos);

                if (player.level().getBlockEntity(packet.pos) instanceof ReissueMachineBlockEntity blockEntity) {
                    // 调用退币方法，根据车票价格退款
                    ReissueMachineBlockEntity.RefundResult result = blockEntity.refundTicketByPrice(player);

                    switch (result) {
                        case NO_TICKET:
                            player.displayClientMessage(LanguageHelper.translate("reissue_machine.no_ticket"), false);
                            break;
                        case INVALID_TICKET:
                            player.displayClientMessage(LanguageHelper.translate("reissue_machine.invalid_ticket"), false);
                            break;
                        case INSUFFICIENT_COINS:
                            player.displayClientMessage(LanguageHelper.translate("reissue_machine.insufficient_coins"), false);
                            break;
                        case TICKET_EXPIRED:
                            player.displayClientMessage(LanguageHelper.translate("reissue_machine.ticket_expired"), false);
                            break;
                        default:
                            // SUCCESS 不需要显示额外消息
                            break;
                    }
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}