package com.easttown.ticketsystem;

import com.easttown.ticketsystem.command.TicketCommand;
import com.easttown.ticketsystem.config.CoinConfig;
import com.easttown.ticketsystem.config.DebugConfig;
import com.easttown.ticketsystem.init.*;
import com.easttown.ticketsystem.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
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
    
    // 添加全局调试模式开关
    public static boolean debugMode = false;
    
    public TicketSystemMod() {
        // 使用新的API获取上下文
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext modContext = ModLoadingContext.get();
        
        modContext.registerConfig(
            ModConfig.Type.COMMON,
            CoinConfig.SPEC,
            "ticketsystem-common.toml"
        );

        modContext.registerConfig(
            ModConfig.Type.CLIENT,
            DebugConfig.SPEC,
            "ticketsystem-client.toml"
        );
        
        BlockInit.BLOCKS.register(modEventBus);
        ItemInit.ITEMS.register(modEventBus);
        BlockEntityInit.BLOCK_ENTITIES.register(modEventBus);
        MenuInit.MENUS.register(modEventBus);
        TicketSystemTab.register(modEventBus);
        
        modEventBus.addListener(this::onCommonSetup);
        
        MinecraftForge.EVENT_BUS.register(this);
        
        new File("mods/" + MODID).mkdirs();
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
}
