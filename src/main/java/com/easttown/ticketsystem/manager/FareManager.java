package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.data.Fare;

import java.util.Collection;

/**
 * 票价管理器
 * 提供票价管理的专门API，底层使用NetworkManager
 */
public class FareManager {
    /**
     * 初始化票价管理器
     */
    public static void initialize() {
        NetworkManager.initialize();
    }

    /**
     * 添加票价
     */
    public static boolean addFare(Fare fare) {
        return NetworkManager.addFare(fare);
    }

    /**
     * 添加票价（简化参数）
     */
    public static boolean addFare(String fromStation, String toStation, int price) {
        Fare fare = new Fare(fromStation, toStation, price);
        return addFare(fare);
    }

    /**
     * 添加双向票价
     */
    public static boolean addBidirectionalFare(Fare fare) {
        return NetworkManager.addBidirectionalFare(fare);
    }

    /**
     * 添加双向票价（简化参数）
     */
    public static boolean addBidirectionalFare(String station1, String station2, int price) {
        Fare fare = new Fare(station1, station2, price);
        return addBidirectionalFare(fare);
    }

    /**
     * 移除票价
     */
    public static boolean removeFare(String fromStation, String toStation) {
        return NetworkManager.removeFare(fromStation, toStation);
    }

    /**
     * 移除双向票价
     */
    public static boolean removeBidirectionalFare(String station1, String station2) {
        boolean removed1 = removeFare(station1, station2);
        boolean removed2 = removeFare(station2, station1);
        return removed1 || removed2;
    }

    /**
     * 获取票价
     */
    public static Fare getFare(String fromStation, String toStation) {
        return NetworkManager.getFare(fromStation, toStation);
    }

    /**
     * 获取票价价格，如果不存在返回0
     */
    public static int getFarePrice(String fromStation, String toStation) {
        Fare fare = getFare(fromStation, toStation);
        return fare != null ? fare.getPrice() : 0;
    }

    /**
     * 获取所有票价
     */
    public static Collection<Fare> getAllFares() {
        return NetworkManager.getAllFares();
    }

    /**
     * 检查票价是否存在
     */
    public static boolean hasFare(String fromStation, String toStation) {
        return NetworkManager.hasFare(fromStation, toStation);
    }

    /**
     * 检查双向票价是否存在
     */
    public static boolean hasBidirectionalFare(String station1, String station2) {
        return hasFare(station1, station2) || hasFare(station2, station1);
    }

    /**
     * 更新票价
     */
    public static boolean updateFare(Fare fare) {
        return NetworkManager.updateFare(fare);
    }

    /**
     * 更新票价（简化参数）
     */
    public static boolean updateFare(String fromStation, String toStation, int price) {
        Fare fare = getFare(fromStation, toStation);
        if (fare == null) {
            return false;
        }

        Fare newFare = new Fare(fromStation, toStation, price);
        return updateFare(newFare);
    }

    /**
     * 更新双向票价
     */
    public static boolean updateBidirectionalFare(String station1, String station2, int price) {
        boolean updated1 = updateFare(station1, station2, price);
        boolean updated2 = updateFare(station2, station1, price);
        return updated1 || updated2;
    }

    /**
     * 根据线路自动生成票价
     * 为线路中相邻车站生成基础票价
     */
    public static boolean generateFaresForLine(String lineId, int basePrice) {
        LineManager.initialize(); // 确保线路管理器初始化

        com.easttown.ticketsystem.data.Line line = LineManager.getLine(lineId);
        if (line == null || !line.isValid()) {
            return false;
        }

        boolean success = true;
        java.util.List<String> stations = line.getStationCodes();

        for (int i = 0; i < stations.size() - 1; i++) {
            String from = stations.get(i);
            String to = stations.get(i + 1);

            // 如果票价不存在，则添加
            if (!hasBidirectionalFare(from, to)) {
                boolean added = addBidirectionalFare(from, to, basePrice);
                if (!added) {
                    success = false;
                }
            }
        }

        return success;
    }

    /**
     * 计算路径总票价（简单累加）
     * 假设路径是有效的车站序列
     */
    public static int calculatePathFare(java.util.List<String> stationPath) {
        if (stationPath == null || stationPath.size() < 2) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < stationPath.size() - 1; i++) {
            String from = stationPath.get(i);
            String to = stationPath.get(i + 1);
            int price = getFarePrice(from, to);
            if (price <= 0) {
                // 没有票价定义，使用默认值或标记为无效
                return -1;
            }
            total += price;
        }

        return total;
    }

    /**
     * 验证票价表完整性
     * 检查所有相邻车站（在线路中）是否有票价定义
     */
    public static java.util.List<String> validateFareCompleteness() {
        java.util.List<String> issues = new java.util.ArrayList<>();

        for (com.easttown.ticketsystem.data.Line line : LineManager.getAllLines()) {
            java.util.List<String> stations = line.getStationCodes();
            for (int i = 0; i < stations.size() - 1; i++) {
                String from = stations.get(i);
                String to = stations.get(i + 1);

                if (!hasBidirectionalFare(from, to)) {
                    issues.add(String.format("线路 %s: 车站 %s -> %s 缺少票价定义",
                            line.getId(), from, to));
                }
            }
        }

        return issues;
    }

    /**
     * 重新加载票价数据
     */
    public static void reloadFares() {
        NetworkManager.reloadAll();
    }

    /**
     * 获取票价统计信息
     */
    public static String getFareStats() {
        Collection<Fare> fares = getAllFares();
        int uniqueSegments = fares.size();
        int totalPriceSum = 0;
        int minPrice = Integer.MAX_VALUE;
        int maxPrice = 0;

        for (Fare fare : fares) {
            int price = fare.getPrice();
            totalPriceSum += price;
            minPrice = Math.min(minPrice, price);
            maxPrice = Math.max(maxPrice, price);
        }

        if (uniqueSegments == 0) {
            return "票价表为空";
        }

        int avgPrice = totalPriceSum / uniqueSegments;
        return String.format("票价表: %d个区间, 价格范围: %d-%d铜币, 平均: %d铜币",
                uniqueSegments, minPrice, maxPrice, avgPrice);
    }
}