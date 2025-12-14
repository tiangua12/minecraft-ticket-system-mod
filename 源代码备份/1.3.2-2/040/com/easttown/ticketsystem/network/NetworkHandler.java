package com.easttown.ticketsystem.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel INSTANCE;
    
    public static void register() {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("ticketsystem", "main");
        
        INSTANCE = NetworkRegistry.newSimpleChannel(
            location,
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );
        
        int id = 0;
        INSTANCE.registerMessage(id++, PrintTicketPacket.class, 
            PrintTicketPacket::encode, PrintTicketPacket::decode, PrintTicketPacket::handle);
        INSTANCE.registerMessage(id++, SetStartStationPacket.class, 
            SetStartStationPacket::encode, SetStartStationPacket::decode, SetStartStationPacket::handle);
        // 修复：只在客户端注册OpenGateConfigPacket
        if (FMLLoader.getDist() == Dist.CLIENT) {
            INSTANCE.registerMessage(id++, OpenGateConfigPacket.class, 
                OpenGateConfigPacket::encode, OpenGateConfigPacket::decode, OpenGateConfigPacket::handle);
        }
        INSTANCE.registerMessage(id++, UpdateGateConfigPacket.class, 
            UpdateGateConfigPacket::encode, UpdateGateConfigPacket::decode, UpdateGateConfigPacket::handle);
        INSTANCE.registerMessage(id++, WithdrawCoinsPacket.class,
            WithdrawCoinsPacket::encode, WithdrawCoinsPacket::decode, WithdrawCoinsPacket::handle);
    }
    
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        if (INSTANCE != null) {
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
    
    public static void sendToServer(Object packet) {
        if (INSTANCE != null) {
            INSTANCE.sendToServer(packet);
        }
    }
}
