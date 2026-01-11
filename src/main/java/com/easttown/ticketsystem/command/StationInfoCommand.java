package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.data.Line;
import com.easttown.ticketsystem.data.Station;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.List;

public class StationInfoCommand {
    public static int execute(CommandContext<CommandSourceStack> context, String stationCode) {
        NetworkManager.initialize();
        Station station = NetworkManager.getStation(stationCode);

        if (station == null) {
            context.getSource().sendFailure(
                Component.literal("车站 " + stationCode + " 不存在")
            );
            return 0;
        }

        // 车站基本信息
        context.getSource().sendSuccess(() ->
            Component.literal("=== 车站信息: " + stationCode + " ==="),
            false
        );

        String enName = station.getEnName() != null && !station.getEnName().isEmpty() ?
            " (" + station.getEnName() + ")" : "";

        context.getSource().sendSuccess(() ->
            Component.literal("名称: " + station.getName() + enName),
            false
        );

        if (station.getStationNumber() > 0) {
            context.getSource().sendSuccess(() ->
                Component.literal("站序号: " + String.format("%02d", station.getStationNumber())),
                false
            );
        }

        // 坐标信息
        if (station.getX() != 0 || station.getY() != 0 || station.getZ() != 0) {
            context.getSource().sendSuccess(() ->
                Component.literal("坐标: (" + station.getX() + ", " + station.getY() + ", " + station.getZ() + ")"),
                false
            );
        }

        // 获取包含该车站的线路
        List<Line> linesContainingStation = NetworkManager.getLinesContainingStation(stationCode);
        int lineCount = linesContainingStation.size();

        context.getSource().sendSuccess(() ->
            Component.literal("所属线路数量: " + lineCount),
            false
        );

        if (lineCount > 0) {
            context.getSource().sendSuccess(() ->
                Component.literal("所属线路列表:"),
                false
            );

            for (Line line : linesContainingStation) {
                // 获取车站在该线路中的位置
                List<String> stationCodes = line.getStationCodes();
                int position = stationCodes.indexOf(stationCode) + 1;

                context.getSource().sendSuccess(() ->
                    Component.literal(String.format("  %s: %s (第%d站)",
                        line.getId(),
                        line.getName(),
                        position)),
                    false
                );
            }
        }

        return Command.SINGLE_SUCCESS;
    }
}