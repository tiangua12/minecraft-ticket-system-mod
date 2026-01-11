package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.data.Station;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Collection;

public class StationListCommand {
    public static int execute(CommandContext<CommandSourceStack> context) {
        NetworkManager.initialize();
        Collection<Station> stations = NetworkManager.getAllStations();

        if (stations.isEmpty()) {
            context.getSource().sendSuccess(() ->
                Component.literal("没有车站数据"),
                true
            );
            return Command.SINGLE_SUCCESS;
        }

        // 发送标题
        context.getSource().sendSuccess(() ->
            Component.literal("=== 车站列表 (" + stations.size() + "个) ==="),
            false
        );

        // 列出每个车站
        for (Station station : stations) {
            String enName = station.getEnName() != null && !station.getEnName().isEmpty() ?
                " (" + station.getEnName() + ")" : "";

            String coordStr = (station.getX() == 0 && station.getY() == 0 && station.getZ() == 0) ?
                "" : String.format(" 坐标: (%d, %d, %d)", station.getX(), station.getY(), station.getZ());

            String stationNumberStr = station.getStationNumber() > 0 ?
                String.format(" 站序号: %02d", station.getStationNumber()) : "";

            context.getSource().sendSuccess(() ->
                Component.literal(String.format("  %s: %s%s%s%s",
                    station.getCode(),
                    station.getName(),
                    enName,
                    coordStr,
                    stationNumberStr)),
                false
            );
        }

        return Command.SINGLE_SUCCESS;
    }
}