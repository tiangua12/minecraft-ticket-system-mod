package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.data.Line;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Collection;

public class LineListCommand {
    public static int execute(CommandContext<CommandSourceStack> context) {
        Collection<Line> lines = NetworkManager.getAllLines();

        if (lines.isEmpty()) {
            context.getSource().sendSuccess(() ->
                Component.literal("没有线路数据"),
                true
            );
            return Command.SINGLE_SUCCESS;
        }

        // 发送标题
        context.getSource().sendSuccess(() ->
            Component.literal("=== 线路列表 (" + lines.size() + "条) ==="),
            false
        );

        // 列出每条线路
        for (Line line : lines) {
            String stationCount = line.getStationCodes() != null ?
                String.valueOf(line.getStationCodes().size()) : "0";

            String enName = line.getEnName() != null && !line.getEnName().isEmpty() ?
                " (" + line.getEnName() + ")" : "";

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("  %s: %s%s - 颜色: %s - 车站: %s个",
                    line.getId(),
                    line.getName(),
                    enName,
                    line.getColor(),
                    stationCount)),
                false
            );
        }

        return Command.SINGLE_SUCCESS;
    }
}