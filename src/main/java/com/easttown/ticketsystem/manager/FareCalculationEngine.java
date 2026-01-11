package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.data.Fare;
import com.easttown.ticketsystem.data.Station;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 票价计算引擎 - 基于web项目的实时计算算法
 * 实现sumLineFare、sumWithTransfer等核心算法
 * 票价基于相邻车站票价累加，支持零成本换乘
 */
public class FareCalculationEngine {
    // 解析车站编码
    public static class ParsedCode {
        public final String prefix;  // 线路前缀，如"01"
        public final int num;        // 站序号，如1

        public ParsedCode(String prefix, int num) {
            this.prefix = prefix;
            this.num = num;
        }

        @Override
        public String toString() {
            return String.format("%s-%02d", prefix, num);
        }
    }

    /**
     * 解析车站编码，如"01-01" -> prefix="01", num=1
     */
    public static ParsedCode parseCode(String code) {
        if (code == null || code.isEmpty()) {
            return new ParsedCode("", 0);
        }

        String[] parts = code.split("-");
        if (parts.length != 2) {
            TicketSystemMod.LOGGER.warn("Invalid station code format: {}", code);
            return new ParsedCode("", 0);
        }

        try {
            int num = Integer.parseInt(parts[1]);
            return new ParsedCode(parts[0], num);
        } catch (NumberFormatException e) {
            TicketSystemMod.LOGGER.warn("Invalid station number in code: {}", code);
            return new ParsedCode(parts[0], 0);
        }
    }

    /**
     * 获取相邻两站之间的单段票价
     * @param from 起点站编码
     * @param to 终点站编码
     * @param isRegular 是否为普通票价（true=普通，false=特急）
     * @return 单段票价，未找到返回0
     */
    public static int segFare(String from, String to, boolean isRegular) {
        if (from == null || to == null || from.isEmpty() || to.isEmpty()) {
            return 0;
        }

        // 初始化管理器
        NetworkManager.initialize();

        // 优先查找 from→to
        Fare fare = FareManager.getFare(from, to);
        if (fare != null && fare.getPrice() > 0) {
            // 注意：当前Fare类只有单票价，没有区分普通/特急
            // 根据web项目，fares.json有cost_regular和cost_express字段
            // 暂时统一使用price字段
            return fare.getPrice();
        }

        // 查找反向 to→from
        fare = FareManager.getFare(to, from);
        if (fare != null && fare.getPrice() > 0) {
            return fare.getPrice();
        }

        // 未找到票价定义
        TicketSystemMod.LOGGER.debug("No fare defined for {} -> {}", from, to);
        return 0;
    }

    /**
     * 计算同一条线路上两站之间的累计票价
     * @param a 起点站编码
     * @param b 终点站编码
     * @param isRegular 是否为普通票价
     * @return 累计票价，如果不在同一条线路返回-1
     */
    public static int sumLineFare(String a, String b, boolean isRegular) {
        ParsedCode pa = parseCode(a);
        ParsedCode pb = parseCode(b);

        if (pa.prefix.isEmpty() || pb.prefix.isEmpty() || !pa.prefix.equals(pb.prefix)) {
            // 不同线路
            return -1;
        }

        int lo = Math.min(pa.num, pb.num);
        int hi = Math.max(pa.num, pb.num);

        int total = 0;
        boolean hasMissingFare = false;

        for (int i = lo; i < hi; i++) {
            String from = String.format("%s-%02d", pa.prefix, i);
            String to = String.format("%s-%02d", pa.prefix, i + 1);

            int segmentFare = segFare(from, to, isRegular);
            if (segmentFare == 0) {
                hasMissingFare = true;
                TicketSystemMod.LOGGER.warn("Missing fare for segment: {} -> {}", from, to);
            }
            total += segmentFare;
        }

        if (hasMissingFare) {
            TicketSystemMod.LOGGER.warn("Some fares missing for line {}: {} -> {}", pa.prefix, a, b);
        }

        return total;
    }

    /**
     * 自动检测同名车站组（中文名或英文名相同的车站视为同一物理车站）
     * 用于零成本换乘计算
     */
    public static Map<String, List<String>> buildStationGroups() {
        NetworkManager.initialize();
        Collection<Station> stations = NetworkManager.getAllStations();

        Map<String, List<String>> groups = new HashMap<>();

        for (Station station : stations) {
            // 使用中文名作为分组键
            String nameKey = station.getName().trim();
            if (nameKey.isEmpty()) {
                continue;
            }

            groups.computeIfAbsent(nameKey, k -> new ArrayList<>())
                  .add(station.getCode());

            // 如果英文名非空，也加入分组（可选项）
            if (station.getEnName() != null && !station.getEnName().trim().isEmpty()) {
                String enKey = station.getEnName().trim();
                groups.computeIfAbsent(enKey, k -> new ArrayList<>())
                      .add(station.getCode());
            }
        }

        // 只保留有多个车站的组
        return groups.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 计算包含换乘的票价（支持零成本换乘）
     * @param from 起点站编码
     * @param to 终点站编码
     * @param isRegular 是否为普通票价
     * @return 包含换乘的最优票价，如果无法到达返回-1
     */
    public static int sumWithTransfer(String from, String to, boolean isRegular) {
        // 1. 首先尝试直接同线票价
        int directFare = sumLineFare(from, to, isRegular);
        if (directFare >= 0) {
            return directFare;
        }

        // 2. 如果不同线，则通过同名车站组进行换乘计算
        Map<String, List<String>> stationGroups = buildStationGroups();
        if (stationGroups.isEmpty()) {
            return -1; // 无换乘组可用
        }

        // 查找起点站和终点站所属的组
        String fromGroup = findStationGroup(from, stationGroups);
        String toGroup = findStationGroup(to, stationGroups);

        if (fromGroup == null || toGroup == null) {
            return -1; // 车站不在任何组中
        }

        // 如果起点和终点在同一组（同名车站），返回0（零成本换乘）
        if (fromGroup.equals(toGroup)) {
            return 0;
        }

        // 3. 查找连接两个组的换乘路径
        // 简化实现：查找同时属于两个组的车站
        // 实际上需要更复杂的图搜索算法，这里简化
        List<String> fromGroupStations = stationGroups.get(fromGroup);
        List<String> toGroupStations = stationGroups.get(toGroup);

        int bestFare = Integer.MAX_VALUE;

        // 遍历所有可能的换乘点
        for (String transferFrom : fromGroupStations) {
            for (String transferTo : toGroupStations) {
                // 计算起点到换乘点的票价（同组内零成本）
                int fare1 = 0; // 同组内零成本

                // 计算换乘点到终点的票价
                int fare2 = sumLineFare(transferFrom, transferTo, isRegular);
                if (fare2 < 0) {
                    continue; // 无法到达
                }

                int totalFare = fare1 + fare2;
                if (totalFare < bestFare) {
                    bestFare = totalFare;
                }
            }
        }

        if (bestFare == Integer.MAX_VALUE) {
            return -1; // 无法找到换乘路径
        }

        return bestFare;
    }

    /**
     * 查找车站所属的组
     */
    private static String findStationGroup(String stationCode, Map<String, List<String>> stationGroups) {
        for (Map.Entry<String, List<String>> entry : stationGroups.entrySet()) {
            if (entry.getValue().contains(stationCode)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 计算票价（主入口）
     * @param from 起点站编码
     * @param to 终点站编码
     * @param isRegular 是否为普通票价
     * @return 票价，如果无法计算返回-1
     */
    public static int calculateFare(String from, String to, boolean isRegular) {
        if (from == null || to == null || from.isEmpty() || to.isEmpty()) {
            return -1;
        }

        if (from.equals(to)) {
            return 0;
        }

        // 尝试包含换乘的计算
        int fare = sumWithTransfer(from, to, isRegular);
        if (fare >= 0) {
            return fare;
        }

        // 尝试直接计算（即使不同线，也尝试累加相邻票价）
        // 这需要更复杂的图搜索算法，暂时返回-1
        return -1;
    }

    /**
     * 计算票价（简化接口，默认普通票价）
     */
    public static int calculateFare(String from, String to) {
        return calculateFare(from, to, true);
    }

    /**
     * 生成票价矩阵（用于调试或导出）
     * @param stationCodes 车站编码列表
     * @param isRegular 是否为普通票价
     * @return 票价矩阵，matrix[i][j]表示从i到j的票价
     */
    public static int[][] generateFareMatrix(List<String> stationCodes, boolean isRegular) {
        int n = stationCodes.size();
        int[][] matrix = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                } else {
                    int fare = calculateFare(stationCodes.get(i), stationCodes.get(j), isRegular);
                    matrix[i][j] = fare >= 0 ? fare : -1; // -1表示无法到达
                }
            }
        }

        return matrix;
    }

    /**
     * 验证票价数据完整性
     * @return 错误消息列表
     */
    public static List<String> validateFareData() {
        List<String> errors = new ArrayList<>();

        NetworkManager.initialize();
        Collection<Station> stations = NetworkManager.getAllStations();

        // 检查相邻车站票价定义
        for (Station station : stations) {
            String code = station.getCode();
            ParsedCode parsed = parseCode(code);
            if (parsed.prefix.isEmpty()) {
                continue;
            }

            // 检查与下一站的票价定义
            String nextCode = String.format("%s-%02d", parsed.prefix, parsed.num + 1);
            if (NetworkManager.hasStation(nextCode)) {
                int fare = segFare(code, nextCode, true);
                if (fare == 0) {
                    errors.add(String.format("Missing fare for adjacent stations: %s -> %s", code, nextCode));
                }
            }
        }

        // 检查同名车站组
        Map<String, List<String>> groups = buildStationGroups();
        if (groups.isEmpty()) {
            errors.add("No station groups found for transfer calculations");
        }

        return errors;
    }
}