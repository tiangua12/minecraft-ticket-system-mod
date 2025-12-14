package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.data.Station;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class AddStationCommand {
    // 旧方法：不带坐标，默认坐标为0
    public static int execute(CommandContext<CommandSourceStack> context, String station) {
        return executeOld(context, station, 0, 0, 0);
    }

    // 旧方法：带坐标
    public static int execute(CommandContext<CommandSourceStack> context, String station, int x, int y, int z) {
        return executeOld(context, station, x, y, z);
    }

    // 旧方法实现
    private static int executeOld(CommandContext<CommandSourceStack> context, String station, int x, int y, int z) {
        // 初始化NetworkManager
        NetworkManager.initialize();

        // 创建车站对象（使用station作为编码和名称）
        Station stationObj = new Station(station, station, "", x, y, z);

        // 添加车站坐标信息
        boolean success = NetworkManager.addStation(stationObj);

        if (success) {
            context.getSource().sendSuccess(() ->
                Component.literal("已添加车站: " + station +
                    (x == 0 && y == 0 && z == 0 ? "" : " 坐标: (" + x + ", " + y + ", " + z + ")")),
                true
            );
            return Command.SINGLE_SUCCESS;
        } else {
            context.getSource().sendFailure(
                Component.literal("添加车站失败，可能车站已存在或坐标无效")
            );
            return 0;
        }
    }

    // 新方法：完整信息，不带坐标
    public static int executeFull(CommandContext<CommandSourceStack> context,
                                 String chineseName, String englishName, String lineId) {
        return executeFull(context, chineseName, englishName, lineId, 0, 0, 0, 0);
    }

    // 新方法：完整信息，带站序号，不带坐标
    public static int executeFull(CommandContext<CommandSourceStack> context,
                                 String chineseName, String englishName, String lineId, int stationNumber) {
        return executeFull(context, chineseName, englishName, lineId, stationNumber, 0, 0, 0);
    }

    // 新方法：完整信息，带站序号和坐标
    public static int executeFull(CommandContext<CommandSourceStack> context,
                                 String chineseName, String englishName, String lineId,
                                 int stationNumber, int x, int y, int z) {
        NetworkManager.initialize();

        // 生成车站编码：使用线路ID和站序号，如果站序号为0则使用名称哈希
        String stationCode;
        if (stationNumber > 0) {
            stationCode = lineId + "-" + String.format("%02d", stationNumber);
        } else {
            // 使用名称哈希生成唯一编码
            String hash = Integer.toHexString(Math.abs(chineseName.hashCode()));
            // 确保哈希长度至少为3，最多取6位
            int length = Math.min(6, Math.max(3, hash.length()));
            stationCode = lineId + "-" + hash.substring(0, length);
        }

        // 创建车站对象
        Station station = new Station(stationCode, chineseName, englishName != null ? englishName : "", x, y, z, stationNumber);

        // 添加车站
        boolean success = NetworkManager.addStation(station);

        if (success) {
            // 尝试将车站添加到线路（如果线路存在）
            // 注意：线路管理需要单独处理，这里只添加车站

            String coordStr = (x == 0 && y == 0 && z == 0) ? "" :
                String.format(" 坐标: (%d, %d, %d)", x, y, z);
            String numberStr = stationNumber > 0 ? String.format(" (站序号: %02d)", stationNumber) : "";
            context.getSource().sendSuccess(() ->
                Component.literal(String.format("已添加车站: %s (%s) 编码: %s 到线路: %s%s%s",
                                               chineseName,
                                               englishName != null && !englishName.isEmpty() ? englishName : "无英文名",
                                               stationCode,
                                               lineId, numberStr, coordStr)),
                true
            );
        } else {
            context.getSource().sendFailure(
                Component.literal("添加车站失败，可能车站编码已存在或坐标无效")
            );
        }
        return success ? Command.SINGLE_SUCCESS : 0;
    }
}