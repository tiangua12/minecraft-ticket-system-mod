package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.data.Fare;
import com.easttown.ticketsystem.data.Line;
import com.easttown.ticketsystem.data.Route;
import com.easttown.ticketsystem.data.Station;

import java.util.*;

/**
 * 路线计算器 - 实现最短路径算法（Dijkstra）
 * 用于计算最低价格的路径，支持换乘
 */
public class RouteCalculator {
    /**
     * 计算从起点到终点的最低价格路径
     * @param startStationCode 起点车站编码
     * @param endStationCode 终点车站编码
     * @return 最优路径，如果没有路径则返回null
     */
    public static Route findCheapestRoute(String startStationCode, String endStationCode) {
        // 输入验证
        if (startStationCode == null || endStationCode == null ||
                startStationCode.isEmpty() || endStationCode.isEmpty()) {
            return null;
        }

        if (startStationCode.equals(endStationCode)) {
            return createDirectRoute(startStationCode, endStationCode);
        }

        // 初始化数据结构
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousStations = new HashMap<>();
        Map<String, String> previousLines = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.distance));

        // 初始化所有车站
        for (Station station : NetworkManager.getAllStations()) {
            String code = station.getCode();
            distances.put(code, Integer.MAX_VALUE);
        }

        // 设置起点
        distances.put(startStationCode, 0);
        queue.add(new Node(startStationCode, 0, null));

        // Dijkstra算法主循环
        while (!queue.isEmpty()) {
            Node current = queue.poll();

            // 如果找到终点，提前结束
            if (current.stationCode.equals(endStationCode)) {
                break;
            }

            // 如果当前距离大于已知最短距离，跳过
            if (current.distance > distances.get(current.stationCode)) {
                continue;
            }

            // 探索邻居车站
            exploreNeighbors(current, distances, previousStations, previousLines, queue);
        }

        // 构建路径
        return buildRoute(startStationCode, endStationCode, distances, previousStations, previousLines);
    }

    /**
     * 探索当前车站的邻居
     */
    private static void exploreNeighbors(Node current,
                                        Map<String, Integer> distances,
                                        Map<String, String> previousStations,
                                        Map<String, String> previousLines,
                                        PriorityQueue<Node> queue) {
        String currentStation = current.stationCode;

        // 获取所有相邻车站（通过票价定义）
        List<Neighbor> neighbors = getNeighbors(currentStation, current.lineId);

        for (Neighbor neighbor : neighbors) {
            String neighborStation = neighbor.stationCode;
            int farePrice = neighbor.farePrice;
            String lineId = neighbor.lineId;

            // 计算新距离
            int newDistance = distances.get(currentStation) + farePrice;

            // 如果找到更短路径
            if (newDistance < distances.get(neighborStation)) {
                distances.put(neighborStation, newDistance);
                previousStations.put(neighborStation, currentStation);
                if (lineId != null) {
                    previousLines.put(neighborStation, lineId);
                }

                queue.add(new Node(neighborStation, newDistance, lineId));
            }
        }
    }

    /**
     * 获取车站的所有邻居（相邻车站）
     */
    private static List<Neighbor> getNeighbors(String stationCode, String currentLineId) {
        List<Neighbor> neighbors = new ArrayList<>();

        // 方法1：通过票价定义查找邻居（双向检查）
        for (Fare fare : NetworkManager.getAllFares()) {
            String neighborStation = null;
            if (fare.getFromStation().equals(stationCode)) {
                neighborStation = fare.getToStation();
            } else if (fare.getToStation().equals(stationCode)) {
                neighborStation = fare.getFromStation();
            }

            if (neighborStation != null) {
                int price = fare.getPrice();
                if (price <= 0) continue; // 跳过无效票价

                // 尝试确定线路
                String lineId = findLineForSegment(stationCode, neighborStation, currentLineId);
                neighbors.add(new Neighbor(neighborStation, price, lineId));
            }
        }

        // 方法2：通过线路查找相邻车站（如果没有票价定义）
        if (neighbors.isEmpty()) {
            for (Line line : NetworkManager.getAllLines()) {
                if (line.containsStation(stationCode)) {
                    String[] adjacent = line.getAdjacentStations(stationCode);
                    if (adjacent != null) {
                        // 前一个车站
                        if (adjacent[0] != null) {
                            int price = estimateFare(stationCode, adjacent[0]);
                            neighbors.add(new Neighbor(adjacent[0], price, line.getId()));
                        }
                        // 后一个车站
                        if (adjacent[1] != null) {
                            int price = estimateFare(stationCode, adjacent[1]);
                            neighbors.add(new Neighbor(adjacent[1], price, line.getId()));
                        }
                    }
                }
            }
        }

        return neighbors;
    }

    /**
     * 为路径段查找线路
     */
    private static String findLineForSegment(String fromStation, String toStation, String currentLineId) {
        // 如果当前线路包含这两个连续车站，优先使用当前线路
        if (currentLineId != null) {
            Line currentLine = NetworkManager.getLine(currentLineId);
            if (currentLine != null) {
                String[] adjacent = currentLine.getAdjacentStations(fromStation);
                if (adjacent != null && (toStation.equals(adjacent[0]) || toStation.equals(adjacent[1]))) {
                    return currentLineId;
                }
            }
        }

        // 查找包含这两个车站的线路
        for (Line line : NetworkManager.getAllLines()) {
            if (line.containsStation(fromStation) && line.containsStation(toStation)) {
                // 检查是否相邻
                String[] adjacent = line.getAdjacentStations(fromStation);
                if (adjacent != null && (toStation.equals(adjacent[0]) || toStation.equals(adjacent[1]))) {
                    return line.getId();
                }
            }
        }

        return null; // 无法确定线路
    }

    /**
     * 估算票价（当没有明确定义时）
     */
    private static int estimateFare(String fromStation, String toStation) {
        // 先尝试获取已定义的票价
        Fare fare = NetworkManager.getFare(fromStation, toStation);
        if (fare != null) {
            return fare.getPrice();
        }

        // 使用基于距离的估算（回退到旧系统）
        Station s1 = NetworkManager.getStation(fromStation);
        Station s2 = NetworkManager.getStation(toStation);
        if (s1 == null || s2 == null) {
            return 100; // 默认票价
        }

        // 简单距离估算（简化版）
        long dx = (long) s1.getX() - s2.getX();
        long dy = (long) s1.getY() - s2.getY();
        long dz = (long) s1.getZ() - s2.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 简单价格公式：每100格1铜币，最低10铜币
        return Math.max(10, (int) (distance / 100));
    }

    /**
     * 构建路径结果
     */
    private static Route buildRoute(String startStation, String endStation,
                                   Map<String, Integer> distances,
                                   Map<String, String> previousStations,
                                   Map<String, String> previousLines) {
        // 检查是否找到路径
        if (distances.get(endStation) == Integer.MAX_VALUE) {
            return null; // 没有路径
        }

        // 回溯构建车站路径
        List<String> stationPath = new ArrayList<>();
        List<String> linePath = new ArrayList<>();
        String current = endStation;

        while (current != null) {
            stationPath.add(0, current);
            String lineId = previousLines.get(current);
            if (lineId != null) {
                linePath.add(0, lineId);
            }
            current = previousStations.get(current);
        }

        // 确保起点正确
        if (!stationPath.get(0).equals(startStation)) {
            return null; // 路径不完整
        }

        // 计算换乘次数
        int transferCount = calculateTransferCount(linePath);

        // 创建路径对象
        Route route = new Route(startStation, endStation);
        route.setStationPath(stationPath);
        route.setLinePath(linePath);
        route.setTotalPrice(distances.get(endStation));
        route.setTransferCount(transferCount);

        return route;
    }

    /**
     * 计算换乘次数
     */
    private static int calculateTransferCount(List<String> linePath) {
        if (linePath.size() <= 1) {
            return 0;
        }

        int transfers = 0;
        String currentLine = linePath.get(0);
        for (int i = 1; i < linePath.size(); i++) {
            if (!linePath.get(i).equals(currentLine)) {
                transfers++;
                currentLine = linePath.get(i);
            }
        }

        return transfers;
    }

    /**
     * 创建直达路径（起点=终点）
     */
    private static Route createDirectRoute(String startStation, String endStation) {
        List<String> stationPath = new ArrayList<>();
        stationPath.add(startStation);
        stationPath.add(endStation);

        Route route = new Route(startStation, endStation);
        route.setStationPath(stationPath);
        route.setTotalPrice(0);
        route.setTransferCount(0);

        return route;
    }

    /**
     * 查找最少换乘路径（备选算法）
     */
    public static Route findMinTransferRoute(String startStationCode, String endStationCode) {
        // 简化实现：先找最低价格路径，然后优化换乘
        Route cheapest = findCheapestRoute(startStationCode, endStationCode);
        if (cheapest == null) {
            return null;
        }

        // 这里可以添加换乘优化逻辑
        return cheapest;
    }

    /**
     * 查找所有可能路径（限制数量）
     */
    public static List<Route> findAllRoutes(String startStationCode, String endStationCode, int maxPaths) {
        // 简化实现：返回最优路径
        List<Route> routes = new ArrayList<>();
        Route route = findCheapestRoute(startStationCode, endStationCode);
        if (route != null) {
            routes.add(route);
        }
        return routes;
    }

    // ==================== 内部辅助类 ====================

    /**
     * Dijkstra算法节点
     */
    private static class Node {
        String stationCode;
        int distance;
        String lineId;

        Node(String stationCode, int distance, String lineId) {
            this.stationCode = stationCode;
            this.distance = distance;
            this.lineId = lineId;
        }
    }

    /**
     * 邻居车站信息
     */
    private static class Neighbor {
        String stationCode;
        int farePrice;
        String lineId;

        Neighbor(String stationCode, int farePrice, String lineId) {
            this.stationCode = stationCode;
            this.farePrice = farePrice;
            this.lineId = lineId;
        }
    }

    /**
     * 获取路径描述
     */
    public static String getRouteDescription(Route route) {
        if (route == null) {
            return "无法找到路径";
        }
        return route.getDescription();
    }

    /**
     * 验证路径有效性
     */
    public static boolean validateRoute(Route route) {
        if (route == null) {
            return false;
        }

        List<String> stationPath = route.getStationPath();
        if (stationPath.size() < 2) {
            return false;
        }

        // 检查每个相邻车站是否有票价定义
        for (int i = 0; i < stationPath.size() - 1; i++) {
            String from = stationPath.get(i);
            String to = stationPath.get(i + 1);
            if (!NetworkManager.hasFare(from, to)) {
                return false;
            }
        }

        return true;
    }
}