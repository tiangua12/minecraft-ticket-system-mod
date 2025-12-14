package com.easttown.ticketsystem.client;

import com.easttown.ticketsystem.init.MenuInit;
import com.easttown.ticketsystem.screen.*;
import com.easttown.ticketsystem.screen.terminal.ReissueMachineMainScreen;
import com.easttown.ticketsystem.screen.terminal.ReissueMachineAdminScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "ticketsystem", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    private static volatile boolean screensRegistered = false;   // 二次注册防护

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (!screensRegistered) {
                MenuScreens.register(MenuInit.TICKET_MACHINE_MENU.get(), TicketMachineScreen::new);
                MenuScreens.register(MenuInit.GATE_CONFIG_MENU.get(), GateConfigScreen::new);
                // 注册退票机器菜单
                MenuScreens.register(MenuInit.REISSUE_MACHINE_MAIN_MENU.get(), com.easttown.ticketsystem.screen.terminal.ReissueMachineMainScreen::new);
                // 注册退票机器管理员菜单
                MenuScreens.register(MenuInit.REISSUE_MACHINE_ADMIN_MENU.get(), com.easttown.ticketsystem.screen.terminal.ReissueMachineAdminScreen::new);
                screensRegistered = true;
            }

            // 其他仅客户端逻辑（已移除调试功能）
        });
    }
}
