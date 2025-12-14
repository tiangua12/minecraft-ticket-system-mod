package com.easttown.ticketsystem;

import com.easttown.ticketsystem.command.TicketCommand;
import com.easttown.ticketsystem.config.TicketSystemConfig;
import com.easttown.ticketsystem.init.*;
import com.easttown.ticketsystem.network.NetworkHandler;
import com.easttown.ticketsystem.util.EasterEggHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(TicketSystemMod.MODID)
public class TicketSystemMod {
    public static final String MODID = "ticketsystem";
    public static final Logger LOGGER = LogManager.getLogger();
    
    public TicketSystemMod() {
        // 使用新的API获取上下文
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext modContext = ModLoadingContext.get();

        modContext.registerConfig(
            ModConfig.Type.COMMON,
            TicketSystemConfig.SPEC,
            "ticketsystem-common.toml"
        );

        BlockInit.BLOCKS.register(modEventBus);
        ItemInit.ITEMS.register(modEventBus);
        BlockEntityInit.BLOCK_ENTITIES.register(modEventBus);
        MenuInit.MENUS.register(modEventBus);
        TicketSystemTab.register(modEventBus);

        modEventBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        new File("mods/" + MODID).mkdirs();

        // 初始化日志系统
        com.easttown.ticketsystem.util.TicketSystemLogger.initialize();

        LOGGER.info("TicketSystem Mod initialized");
    }
    
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
    
    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        TicketCommand.register(event.getDispatcher());
        LOGGER.info("Registered commands");
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        String message = event.getRawText();
        LOGGER.info("收到聊天消息: " + message);
        if (EasterEggHandler.checkForEasterEgg(message, event.getPlayer())) {
            LOGGER.info("彩蛋已触发!");
        }
    }
}
