package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.data.Station;
import com.easttown.ticketsystem.data.StationData;

import java.util.HashSet;
import java.util.Set;

/**
 * 车站管理器兼容层
 * 提供与原有StationManager相同的API，但底层使用NetworkManager
 * 用于保持现有代码的兼容性，同时迁移到新系统
 */
public class StationManagerCompat {
    /**
     * 添加车站（兼容原有API）
     * 注意：这个方法应该只在服务器端调用
     * 客户端应该通过网络包发送添加请求
     */
    public static void addStation(String stationName, int x, int y, int z) {
        // 创建车站编码（使用名称作为编码，或生成唯一编码）
        String stationCode = generateStationCode(stationName);

        // 创建新的Station对象
        Station station = new Station(stationCode, stationName);
        station.setCoordinates(x, y, z);

        // 使用NetworkManager添加
        NetworkManager.addStation(station);

        TicketSystemMod.LOGGER.debug("Added station via compat layer: {} -> {}", stationName, stationCode);
    }

    /**
     * 移除车站（兼容原有API）
     */
    public static void removeStation(String stationName) {
        // 查找对应的车站编码
        String stationCode = findStationCodeByName(stationName);
        if (stationCode != null) {
            NetworkManager.removeStation(stationCode);
            TicketSystemMod.LOGGER.debug("Removed station via compat layer: {} -> {}", stationName, stationCode);
        } else {
            TicketSystemMod.LOGGER.warn("Station not found for removal: {}", stationName);
        }
    }

    /**
     * 获取所有车站名称（兼容原有API）
     * 注意：这返回车站名称，而不是编码
     */
    public static Set<String> getStations() {
        Set<String> stationNames = new HashSet<>();
        for (Station station : NetworkManager.getAllStations()) {
            stationNames.add(station.getName());
        }
        return stationNames;
    }

    /**
     * 获取车站数据（兼容原有API）
     * 返回旧的StationData对象以保持兼容性
     */
    public static StationData getStationData(String stationName) {
        String stationCode = findStationCodeByName(stationName);
        if (stationCode == null) {
            return null;
        }

        Station station = NetworkManager.getStation(stationCode);
        if (station == null) {
            return null;
        }

        // 创建旧的StationData对象
        StationData data = new StationData(station.getX(), station.getY(), station.getZ());
        // 注意：旧的price字段未使用，保持默认值
        return data;
    }

    /**
     * 检查车站是否存在（兼容原有API）
     */
    public static boolean containsStation(String stationName) {
        return findStationCodeByName(stationName) != null;
    }

    /**
     * 计算距离（兼容原有API）
     * 使用新系统的车站坐标计算距离
     */
    public static double calculateDistance(String station1Name, String station2Name) {
        String code1 = findStationCodeByName(station1Name);
        String code2 = findStationCodeByName(station2Name);

        if (code1 == null || code2 == null) {
            return 0;
        }

        Station s1 = NetworkManager.getStation(code1);
        Station s2 = NetworkManager.getStation(code2);

        if (s1 == null || s2 == null) {
            return 0;
        }

        long dx = (long) s1.getX() - s2.getX();
        long dy = (long) s1.getY() - s2.getY();
        long dz = (long) s1.getZ() - s2.getZ();

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double MAX_DISTANCE = 1000000; // 与原StationManager一致
        return Math.min(distance, MAX_DISTANCE);
    }

    /**
     * 检查坐标是否有效（兼容原有API）
     */
    public static boolean isValidCoordinate(int x, int y, int z) {
        return NetworkManager.getStation("dummy") != null ?
               new Station("dummy", "dummy").isValidCoordinate() : // 使用Station的检查逻辑
               (x >= -30000000 && x <= 30000000 &&
                y >= -2048 && y <= 2048 &&
                z >= -30000000 && z <= 30000000);
    }

    /**
     * 重新加载车站数据（兼容原有API）
     */
    public static void reloadStations() {
        NetworkManager.reloadAll();
    }

    // ==================== 辅助方法 ====================

    /**
     * 根据车站名称查找车站编码
     */
    private static String findStationCodeByName(String stationName) {
        for (Station station : NetworkManager.getAllStations()) {
            if (station.getName().equals(stationName)) {
                return station.getCode();
            }
        }
        return null;
    }

    /**
     * 生成车站编码
     * 简单实现：使用名称的hashCode，确保唯一性
     */
    private static String generateStationCode(String stationName) {
        // 简单实现：使用名称作为编码的基础
        // 实际应该使用更健壮的编码生成逻辑
        String baseCode = stationName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();

        // 确保唯一性
        int suffix = 1;
        String code = baseCode;
        while (NetworkManager.getStation(code) != null) {
            code = baseCode + "_" + suffix;
            suffix++;
        }

        return code;
    }

    /**
     * 直接添加车站（使用编码）
     * 新代码应该使用这个方法而不是旧的addStation
     */
    public static boolean addStationWithCode(String stationCode, String stationName, int x, int y, int z) {
        Station station = new Station(stationCode, stationName);
        station.setCoordinates(x, y, z);
        return NetworkManager.addStation(station);
    }

    /**
     * 获取车站编码（新代码使用）
     */
    public static String getStationCode(String stationName) {
        return findStationCodeByName(stationName);
    }

    /**
     * 获取车站名称（新代码使用）
     */
    public static String getStationName(String stationCode) {
        Station station = NetworkManager.getStation(stationCode);
        return station != null ? station.getName() : null;
    }
}