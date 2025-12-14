package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.network.NetworkHandler;
import com.easttown.ticketsystem.network.OpenStationManagementPacket;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * 打开车站管理GUI命令
 * 发送网络包到客户端打开车站管理界面
 */
public class ManageStationsCommand {
    public static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        // 检查执行者是否为玩家
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            // 发送打开车站管理GUI的网络包
            NetworkHandler.sendToPlayer(new OpenStationManagementPacket(), player);
            source.sendSuccess(() ->
                Component.literal("已发送车站管理界面打开请求"),
                true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("此命令只能由玩家执行"));
            return 0;
        }
    }
}