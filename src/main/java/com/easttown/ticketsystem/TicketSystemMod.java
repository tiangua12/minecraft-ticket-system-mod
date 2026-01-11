package com.easttown.ticketsystem;

import com.easttown.ticketsystem.command.TicketCommand;
import com.easttown.ticketsystem.config.TicketSystemConfig;
import com.easttown.ticketsystem.init.*;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.easttown.ticketsystem.network.NetworkHandler;
import com.easttown.ticketsystem.util.EasterEggHandler;
import com.easttown.ticketsystem.web.WebServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.nio.file.Path;
import java.nio.file.Paths;
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
        event.enqueueWork(() -> {
            NetworkHandler.register();

            // Web服务器现在在ServerStartedEvent中启动，以便使用正确的存档路径
            LOGGER.info("NetworkHandler registered. Web server will start when world loads.");
        });
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

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();

        // 设置数据存储路径到世界文件夹/data/ticketsystem/
        // 使用Minecraft的世界数据路径API
        Path worldDataDir;
        try {
            // 获取主世界（overworld）的路径
            var overworld = server.overworld();
            if (overworld != null) {
                // 使用世界根目录下的data/ticketsystem/文件夹
                Path worldRoot = server.getWorldPath(LevelResource.ROOT);
                worldDataDir = worldRoot.resolve("data/ticketsystem/");
            } else {
                // 回退到旧逻辑
                Path serverDir = server.getServerDirectory().toPath();
                worldDataDir = serverDir.resolve("world/data/ticketsystem/");
            }
        } catch (Exception e) {
            // 出错时使用旧逻辑
            Path serverDir = server.getServerDirectory().toPath();
            worldDataDir = serverDir.resolve("world/data/ticketsystem/");
            LOGGER.warn("Failed to get world path, using fallback: {}", worldDataDir, e);
        }

        // 确保目录存在
        worldDataDir.toFile().mkdirs();

        // 设置NetworkManager的基础路径
        NetworkManager.setBasePath(worldDataDir.toString());

        // 初始化NetworkManager（使用新路径）
        NetworkManager.initialize();

        LOGGER.info("TicketSystem data path set to: {}", worldDataDir);

        // 启动Web服务器（如果配置启用）
        if (TicketSystemConfig.isWebServerEnabled()) {
            try {
                WebServer.start();
                LOGGER.info("Web server started on port {}", WebServer.getPort());
            } catch (Exception e) {
                LOGGER.error("Failed to start web server", e);
            }
        } else {
            LOGGER.info("Web server is disabled in configuration");
        }
    }
}
