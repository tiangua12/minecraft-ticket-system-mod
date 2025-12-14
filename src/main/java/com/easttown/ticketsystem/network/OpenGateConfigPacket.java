package com.easttown.ticketsystem.network;

import com.easttown.ticketsystem.block.GateBlockEntity;
import com.easttown.ticketsystem.screen.GateConfigMenu;
import com.easttown.ticketsystem.screen.GateConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenGateConfigPacket {
    private final BlockPos pos;

    public OpenGateConfigPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(OpenGateConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    public static OpenGateConfigPacket decode(FriendlyByteBuf buffer) {
        return new OpenGateConfigPacket(buffer.readBlockPos());
    }

    public static void handle(OpenGateConfigPacket packet, Supplier<NetworkEvent.Context> context) {
        // 标记为已处理
        context.get().setPacketHandled(true);
        
        // 只在客户端执行
        if (context.get().getDirection().getReceptionSide().isClient()) {
            context.get().enqueueWork(() -> {
                // 使用DistExecutor确保在客户端执行
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet));
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(OpenGateConfigPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null && minecraft.level != null) {
            // 获取方块实体
            if (minecraft.level.getBlockEntity(packet.pos) instanceof GateBlockEntity blockEntity) {
                // 创建菜单对象
                GateConfigMenu menu = new GateConfigMenu(
                        0,
                        player.getInventory(),
                        blockEntity
                );
                
                // 创建屏幕对象
                GateConfigScreen screen = new GateConfigScreen(
                        menu,
                        player.getInventory(),
                        Component.translatable("ticketsystem.gui.gate_config")
                );
                
                // 打开屏幕
                minecraft.setScreen(screen);
            }
        }
    }
}
