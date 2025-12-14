package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.web.WebServer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class WebServerStartCommand {

    public static int execute(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();

            // 检查Web服务器是否已经在运行
            if (WebServer.isRunning()) {
                source.sendSuccess(() ->
                    Component.literal("Web服务器已在运行，端口: " + WebServer.getPort()), true);
                return Command.SINGLE_SUCCESS;
            }

            // 尝试启动Web服务器
            TicketSystemMod.LOGGER.info("手动执行Web服务器启动命令");
            WebServer.start();

            // 检查启动是否成功
            if (WebServer.isRunning()) {
                source.sendSuccess(() ->
                    Component.literal("Web服务器启动成功，端口: " + WebServer.getPort()), true);
                TicketSystemMod.LOGGER.info("Web服务器通过命令手动启动成功，端口: {}", WebServer.getPort());
            } else {
                source.sendFailure(
                    Component.literal("Web服务器启动失败，请查看日志获取详细信息")
                );
                TicketSystemMod.LOGGER.error("Web服务器通过命令手动启动失败");
            }

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("启动Web服务器时发生错误: " + e.getMessage())
            );
            TicketSystemMod.LOGGER.error("Web服务器启动命令执行失败", e);
            return 0;
        }
    }

    // 可选：添加一个停止命令
    public static int executeStop(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();

            if (!WebServer.isRunning()) {
                source.sendSuccess(() ->
                    Component.literal("Web服务器未在运行"), true);
                return Command.SINGLE_SUCCESS;
            }

            int port = WebServer.getPort();
            WebServer.stop();

            source.sendSuccess(() ->
                Component.literal("Web服务器已停止，端口: " + port), true);
            TicketSystemMod.LOGGER.info("Web服务器通过命令手动停止，端口: {}", port);

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("停止Web服务器时发生错误: " + e.getMessage())
            );
            TicketSystemMod.LOGGER.error("Web服务器停止命令执行失败", e);
            return 0;
        }
    }

    // 可选：添加状态检查命令
    public static int executeStatus(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();

            boolean isRunning = WebServer.isRunning();
            String status = isRunning ? "运行中" : "已停止";
            String message = "Web服务器状态: " + status;

            if (isRunning) {
                message += "，端口: " + WebServer.getPort();
                message += "，访问地址: http://localhost:" + WebServer.getPort() + "/";
            }

            final String finalMessage = message;
            source.sendSuccess(() -> Component.literal(finalMessage), true);
            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("获取Web服务器状态时发生错误: " + e.getMessage())
            );
            return 0;
        }
    }
}