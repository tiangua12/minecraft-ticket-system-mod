package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.data.Line;
import com.easttown.ticketsystem.data.Station;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.List;

public class LineInfoCommand {
    public static int execute(CommandContext<CommandSourceStack> context, String lineId) {
        Line line = NetworkManager.getLine(lineId);

        if (line == null) {
            context.getSource().sendFailure(
                Component.literal("线路 " + lineId + " 不存在")
            );
            return 0;
        }

        // 线路基本信息
        context.getSource().sendSuccess(() ->
            Component.literal("=== 线路信息: " + lineId + " ==="),
            false
        );

        String enName = line.getEnName() != null && !line.getEnName().isEmpty() ?
            " (" + line.getEnName() + ")" : "";

        context.getSource().sendSuccess(() ->
            Component.literal("名称: " + line.getName() + enName),
            false
        );

        context.getSource().sendSuccess(() ->
            Component.literal("颜色: " + line.getColor()),
            false
        );

        // 车站列表
        List<String> stationCodes = line.getStationCodes();
        int stationCount = stationCodes != null ? stationCodes.size() : 0;

        context.getSource().sendSuccess(() ->
            Component.literal("车站数量: " + stationCount),
            false
        );

        if (stationCount > 0) {
            context.getSource().sendSuccess(() ->
                Component.literal("车站列表:"),
                false
            );

            for (int i = 0; i < stationCodes.size(); i++) {
                final int index = i + 1;
                final String stationCode = stationCodes.get(i);
                final Station station = NetworkManager.getStation(stationCode);

                if (station != null) {
                    context.getSource().sendSuccess(() ->
                        Component.literal(String.format("  %d. %s (%s) - %s",
                            index,
                            station.getName(),
                            stationCode,
                            station.getEnName() != null && !station.getEnName().isEmpty() ?
                                station.getEnName() : "无英文名")),
                        false
                    );
                } else {
                    context.getSource().sendSuccess(() ->
                        Component.literal(String.format("  %d. %s (车站数据缺失)", index, stationCode)),
                        false
                    );
                }
            }
        }

        return Command.SINGLE_SUCCESS;
    }
}