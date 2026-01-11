package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.manager.NetworkManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class LineDeleteCommand {
    public static int execute(CommandContext<CommandSourceStack> context, String lineId) {
        // 检查线路是否存在
        if (!NetworkManager.hasLine(lineId)) {
            context.getSource().sendFailure(
                Component.literal("线路 " + lineId + " 不存在")
            );
            return 0;
        }

        // 删除线路
        boolean success = NetworkManager.removeLine(lineId);

        if (success) {
            context.getSource().sendSuccess(() ->
                Component.literal("已删除线路: " + lineId),
                true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            context.getSource().sendFailure(
                Component.literal("删除线路失败")
            );
            return 0;
        }
    }
}