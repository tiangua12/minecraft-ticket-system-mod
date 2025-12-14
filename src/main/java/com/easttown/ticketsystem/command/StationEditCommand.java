package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.data.Station;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class StationEditCommand {
    public static int execute(CommandContext<CommandSourceStack> context, String stationCode,
                             String newName, String newEnName, Integer newStationNumber,
                             Integer x, Integer y, Integer z) {
        NetworkManager.initialize();
        Station station = NetworkManager.getStation(stationCode);

        if (station == null) {
            context.getSource().sendFailure(
                Component.literal("车站 " + stationCode + " 不存在")
            );
            return 0;
        }

        boolean modified = false;

        // 更新车站名称
        if (newName != null && !newName.isEmpty() && !newName.equals(station.getName())) {
            station.setName(newName);
            modified = true;
        }

        // 更新英文名称
        if (newEnName != null) {
            station.setEnName(newEnName);
            modified = true;
        }

        // 更新站序号
        if (newStationNumber != null && newStationNumber >= 0 && newStationNumber != station.getStationNumber()) {
            station.setStationNumber(newStationNumber);
            modified = true;
        }

        // 更新坐标
        if (x != null && y != null && z != null &&
            (x != station.getX() || y != station.getY() || z != station.getZ())) {
            station.setCoordinates(x, y, z);
            modified = true;
        }

        if (!modified) {
            context.getSource().sendFailure(
                Component.literal("没有需要更新的字段")
            );
            return 0;
        }

        // 更新车站
        boolean success = NetworkManager.updateStation(station);

        if (success) {
            context.getSource().sendSuccess(() ->
                Component.literal("已更新车站: " + stationCode),
                true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            context.getSource().sendFailure(
                Component.literal("更新车站失败")
            );
            return 0;
        }
    }

    // 简化版本：只更新名称
    public static int executeNameOnly(CommandContext<CommandSourceStack> context, String stationCode, String newName) {
        return execute(context, stationCode, newName, null, null, null, null, null);
    }

    // 简化版本：只更新坐标
    public static int executeCoordinatesOnly(CommandContext<CommandSourceStack> context, String stationCode,
                                            int x, int y, int z) {
        return execute(context, stationCode, null, null, null, x, y, z);
    }
}