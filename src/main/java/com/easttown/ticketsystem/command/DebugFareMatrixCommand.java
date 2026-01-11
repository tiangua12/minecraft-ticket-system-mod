package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.data.Fare;
import com.easttown.ticketsystem.data.Route;
import com.easttown.ticketsystem.manager.FareManager;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.easttown.ticketsystem.manager.PriceCalculator;
import com.easttown.ticketsystem.util.DebugLogger;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Collection;

/**
 * 调试命令：计算并输出所有车票票价组合的详细信息
 * 命令：/ticketsystem debugfarematrix
 * 输出详细票价信息到日志，同时在聊天栏显示进度和统计
 */
public class DebugFareMatrixCommand {

    public static int execute(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();

            // 确保NetworkManager已初始化
            NetworkManager.initialize();

            // 获取所有车站
            var stations = NetworkManager.getAllStations();
            int stationCount = stations.size();

            if (stationCount < 2) {
                source.sendFailure(Component.literal("需要至少2个车站才能计算票价矩阵"));
                return 0;
            }

            source.sendSuccess(() ->
                Component.literal("开始计算票价矩阵... 共有" + stationCount + "个车站"), false);

            // 记录开始时间
            long startTime = System.currentTimeMillis();

            // 计算所有可能的车站组合票价
            final int[] calculatedCount = {0};
            final int totalCombinations = stationCount * (stationCount - 1);
            int validFares = 0;
            int zeroFares = 0;
            int errorFares = 0;
            int maxPrice = 0;
            int minPrice = Integer.MAX_VALUE;
            long totalPriceSum = 0;

            // 输出车站列表到日志
            TicketSystemMod.LOGGER.info("=== 票价矩阵调试输出 ===");
            TicketSystemMod.LOGGER.info("车站列表（共{}个）：", stationCount);
            for (var station : stations) {
                TicketSystemMod.LOGGER.info("  - {}: {} ({})",
                    station.getCode(), station.getName(), station.getEnName());
            }
            TicketSystemMod.LOGGER.info("");

            // 遍历所有车站对（有序，因为票价可能有方向性）
            for (var fromStation : stations) {
                for (var toStation : stations) {
                    if (fromStation.getCode().equals(toStation.getCode())) {
                        continue; // 跳过同一车站
                    }

                    String fromCode = fromStation.getCode();
                    String toCode = toStation.getCode();
                    String fromName = fromStation.getName();
                    String toName = toStation.getName();

                    // 使用PriceCalculator计算票价
                    int price = PriceCalculator.calculatePrice(fromCode, toCode);

                    // 获取路径详情（如果可用）
                    Route route = PriceCalculator.getRouteDetails(fromCode, toCode);
                    String routeInfo = "";
                    if (route != null && !route.getSegments().isEmpty()) {
                        routeInfo = String.format(" (路径: %s)", route.toString());
                    }

                    // 统计
                    calculatedCount[0]++;
                    if (price > 0) {
                        validFares++;
                        totalPriceSum += price;
                        maxPrice = Math.max(maxPrice, price);
                        minPrice = Math.min(minPrice, price);
                    } else if (price == 0) {
                        zeroFares++;
                    } else {
                        errorFares++; // price < 0 表示错误
                    }

                    // 输出详细票价信息到日志
                    String logMessage = String.format("%04d/%04d %s [%s] -> %s [%s]: %d铜币%s",
                        calculatedCount[0], totalCombinations,
                        fromName, fromCode,
                        toName, toCode,
                        price, routeInfo);
                    TicketSystemMod.LOGGER.info(logMessage);

                    // 每计算50个票价输出一次进度到聊天栏
                    if (calculatedCount[0] % 50 == 0) {
                        source.sendSuccess(() ->
                            Component.literal("票价计算进度: " + calculatedCount[0] + "/" + totalCombinations), false);
                    }
                }
            }

            // 计算耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 输出统计信息到日志
            TicketSystemMod.LOGGER.info("");
            TicketSystemMod.LOGGER.info("=== 票价矩阵统计 ===");
            TicketSystemMod.LOGGER.info("总组合数: {}", totalCombinations);
            TicketSystemMod.LOGGER.info("有效票价数 (价格>0): {}", validFares);
            TicketSystemMod.LOGGER.info("零票价数 (价格=0): {}", zeroFares);
            TicketSystemMod.LOGGER.info("错误票价数 (价格<0): {}", errorFares);
            if (validFares > 0) {
                TicketSystemMod.LOGGER.info("价格范围: {} - {} 铜币", minPrice, maxPrice);
                TicketSystemMod.LOGGER.info("平均价格: {} 铜币", totalPriceSum / validFares);
            }
            TicketSystemMod.LOGGER.info("计算耗时: {} 毫秒", duration);
            TicketSystemMod.LOGGER.info("");

            // 输出已定义的票价表信息（从FareManager）
            Collection<Fare> definedFares = FareManager.getAllFares();
            TicketSystemMod.LOGGER.info("已定义的票价表条目数: {}", definedFares.size());
            if (!definedFares.isEmpty()) {
                int definedMin = Integer.MAX_VALUE;
                int definedMax = 0;
                long definedSum = 0;
                for (Fare fare : definedFares) {
                    int price = fare.getPrice();
                    definedMin = Math.min(definedMin, price);
                    definedMax = Math.max(definedMax, price);
                    definedSum += price;
                }
                TicketSystemMod.LOGGER.info("定义票价范围: {} - {} 铜币", definedMin, definedMax);
                TicketSystemMod.LOGGER.info("定义票价平均: {} 铜币", definedSum / definedFares.size());
            }
            TicketSystemMod.LOGGER.info("=== 票价矩阵调试输出结束 ===");

            // 在聊天栏输出摘要
            source.sendSuccess(() ->
                Component.literal("票价矩阵计算完成! " +
                    "共计算" + calculatedCount[0] + "个组合, " +
                    "有效票价: " + validFares + ", " +
                    "零票价: " + zeroFares + ", " +
                    "错误: " + errorFares + ", " +
                    "耗时: " + duration + "ms"), true);

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            context.getSource().sendFailure(
                Component.literal("计算票价矩阵时发生错误: " + e.getMessage())
            );
            DebugLogger.error("Debug fare matrix command failed", e);
            TicketSystemMod.LOGGER.error("Debug fare matrix command failed", e);
            return 0;
        }
    }
}