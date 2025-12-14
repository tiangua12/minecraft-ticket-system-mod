package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.config.TicketSystemConfig;
import com.easttown.ticketsystem.manager.StationManager;
import com.easttown.ticketsystem.util.DebugLogger;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ReloadCommand {

    public static int execute(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();

            // 重新加载配置文件
            reloadConfigurations(source);

            source.sendSuccess(() ->
                Component.literal("票务系统配置已重新加载"), true);

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("重新加载配置时发生错误: " + e.getMessage())
            );
            DebugLogger.error("Reload command failed", e);
            return 0;
        }
    }

    private static void reloadConfigurations(CommandSourceStack source) {
        // 重新加载车站数据
        StationManager.reloadStations();

        // Forge配置会自动重新加载，不需要手动调用
        // 这里可以添加其他需要重新加载的配置

        DebugLogger.info("Configurations reloaded successfully");
    }
}