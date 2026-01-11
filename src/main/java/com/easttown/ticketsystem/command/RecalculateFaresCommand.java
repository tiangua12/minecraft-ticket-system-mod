package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.manager.NetworkManager;
import com.easttown.ticketsystem.manager.PriceCalculator;
import com.easttown.ticketsystem.util.DebugLogger;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class RecalculateFaresCommand {

    public static int execute(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();

            // 确保NetworkManager已初始化
            NetworkManager.initialize();

            // 获取所有车站
            var stations = NetworkManager.getAllStations();
            int stationCount = stations.size();

            if (stationCount < 2) {
                source.sendFailure(Component.literal("需要至少2个车站才能计算票价"));
                return 0;
            }

            source.sendSuccess(() ->
                Component.literal("开始重新计算所有票价... 共有" + stationCount + "个车站"), false);

            // 计算所有可能的车站组合票价
            final int[] calculatedCount = {0};
            final int totalCombinations = stationCount * (stationCount - 1) / 2;

            for (var station1 : stations) {
                for (var station2 : stations) {
                    if (station1.getCode().equals(station2.getCode())) {
                        continue; // 跳过同一车站
                    }

                    // 使用PriceCalculator计算票价
                    int price = PriceCalculator.calculatePrice(station1.getCode(), station2.getCode());

                    // 票价已自动缓存，无需手动存储
                    calculatedCount[0]++;

                    // 每计算100个票价输出一次进度
                    if (calculatedCount[0] % 100 == 0) {
                        source.sendSuccess(() ->
                            Component.literal("票价计算进度: " + calculatedCount[0] + "/" + totalCombinations), false);
                    }
                }
            }

            source.sendSuccess(() ->
                Component.literal("票价重新计算完成! 共计算了" + calculatedCount[0] + "个票价组合"), true);

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("重新计算票价时发生错误: " + e.getMessage())
            );
            DebugLogger.error("Recalculate fares command failed", e);
            return 0;
        }
    }
}