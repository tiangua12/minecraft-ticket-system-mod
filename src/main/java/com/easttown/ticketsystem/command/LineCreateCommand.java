package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.data.Line;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class LineCreateCommand {
    // 基本版本：线路ID、名称、颜色
    public static int execute(CommandContext<CommandSourceStack> context,
                             String lineId, String name, String color) {
        return execute(context, lineId, name, "", color);
    }

    // 完整版本：线路ID、名称、英文名、颜色
    public static int execute(CommandContext<CommandSourceStack> context,
                             String lineId, String name, String enName, String color) {
        // 检查线路是否已存在
        if (NetworkManager.hasLine(lineId)) {
            context.getSource().sendFailure(
                Component.literal("线路 " + lineId + " 已存在")
            );
            return 0;
        }

        // 创建线路对象
        Line line = new Line(lineId, name, enName, color);

        // 添加到管理器
        boolean success = NetworkManager.addLine(line);

        if (success) {
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("已创建线路: %s (%s) 颜色: %s",
                                               lineId, name, color)),
                true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            context.getSource().sendFailure(
                Component.literal("创建线路失败")
            );
            return 0;
        }
    }
}