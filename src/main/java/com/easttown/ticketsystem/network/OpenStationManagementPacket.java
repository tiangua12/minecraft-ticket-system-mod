package com.easttown.ticketsystem.network;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.screen.StationManagementScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 打开车站管理GUI网络包
 * 服务器发送此包到客户端，打开车站管理界面
 */
public class OpenStationManagementPacket {
    // 可以包含一些初始数据，如当前车站列表
    // 目前为空，客户端从NetworkManager加载数据

    public OpenStationManagementPacket() {
        // 空构造器
    }

    // 编码
    public static void encode(OpenStationManagementPacket packet, FriendlyByteBuf buffer) {
        // 无数据需要编码
    }

    // 解码
    public static OpenStationManagementPacket decode(FriendlyByteBuf buffer) {
        return new OpenStationManagementPacket();
    }

    // 处理
    public static void handle(OpenStationManagementPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // 确保在客户端执行
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                try {
                    // 打开车站管理屏幕
                    Minecraft.getInstance().setScreen(new StationManagementScreen());
                    TicketSystemMod.LOGGER.debug("Opened StationManagementScreen via network packet");
                } catch (Exception e) {
                    TicketSystemMod.LOGGER.error("Failed to open StationManagementScreen", e);
                }
            });
        });
        context.get().setPacketHandled(true);
    }
}