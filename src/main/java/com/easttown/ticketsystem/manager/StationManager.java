package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.data.Line;
import com.easttown.ticketsystem.data.Station;
import com.easttown.ticketsystem.data.StationData;
import com.easttown.ticketsystem.util.RandomIdGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * 车站管理器 - 适配器版本
 * 保持原有API兼容性，但底层使用新的NetworkManager系统
 * 文档要求：修复数据存储位置bug，所有数据操作在服务器端执行
 */
public class StationManager {
    // 最大距离限制，防止坐标过大导致计算问题
    private static final double MAX_DISTANCE = 1000000; // 1,000,000格

    // 初始化NetworkManager（懒加载）
    static {
        try {
            NetworkManager.initialize();
            TicketSystemMod.LOGGER.info("StationManager initialized with NetworkManager backend");
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to initialize StationManager with NetworkManager", e);
        }
    }

    /**
     * 添加车站（兼容原有API）
     * 注意：这个方法应该只在服务器端调用
     * 客户端应该通过网络包发送添加请求
     */
    public static void addStation(String stationName, int x, int y, int z) {
        // 生成车站编码（使用名称作为基础，确保唯一性）
        String stationCode = generateStationCode(stationName);

        // 创建新的Station对象
        Station station = new Station(stationCode, stationName);
        station.setCoordinates(x, y, z);

        // 使用NetworkManager添加
        NetworkManager.addStation(station);

        TicketSystemMod.LOGGER.debug("Added station via StationManager adapter: {} -> {}", stationName, stationCode);
    }

    /**
     * 移除车站（兼容原有API）
     */
    public static void removeStation(String stationName) {
        // 查找对应的车站编码
        String stationCode = findStationCodeByName(stationName);
        if (stationCode != null) {
            NetworkManager.removeStation(stationCode);
            TicketSystemMod.LOGGER.debug("Removed station via StationManager adapter: {} -> {}", stationName, stationCode);
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
        if (station1Name.equals(station2Name)) return 0;

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

        // 使用long类型防止整数溢出
        long dx = (long) s1.getX() - s2.getX();
        long dy = (long) s1.getY() - s2.getY();
        long dz = (long) s1.getZ() - s2.getZ();

        // 计算欧几里得距离
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 限制最大距离
        return Math.min(distance, MAX_DISTANCE);
    }

    /**
     * 检查坐标是否有效（兼容原有API）
     */
    public static boolean isValidCoordinate(int x, int y, int z) {
        // 使用Station类的验证逻辑
        Station dummy = new Station("dummy", "dummy");
        dummy.setCoordinates(x, y, z);
        return dummy.isValidCoordinate();
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
     * 生成16位小写字母和数字组成的随机ID，确保唯一性
     */
    private static String generateStationCode(String stationName) {
        // 收集现有ID确保唯一性
        Set<String> existingIds = new HashSet<>(NetworkManager.getStationCodes());

        // 生成唯一ID
        return RandomIdGenerator.generateUniqueId(existingIds);
    }

    /**
     * 直接添加车站（使用编码）- 新代码使用
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

    /**
     * 添加车站到线路（完整信息）
     * @param chineseName 车站中文名
     * @param englishName 车站英文名（可选，可为null或空）
     * @param lineId 线路ID
     * @param stationNumber 站序号（1-99），如为0则自动分配
     * @param x 坐标X（可选）
     * @param y 坐标Y（可选）
     * @param z 坐标Z（可选）
     * @return 是否成功
     */
    public static boolean addStationToLine(String chineseName, String englishName, String lineId,
                                           int stationNumber, int x, int y, int z) {
        // 检查线路是否存在
        Line line = NetworkManager.getLine(lineId);
        if (line == null) {
            TicketSystemMod.LOGGER.error("线路不存在: {}", lineId);
            return false;
        }

        // 生成车站编码（格式：线路ID-站序号，如 L1-01）
        String stationCode;
        if (stationNumber > 0) {
            // 使用指定的站序号，格式化为两位数
            stationCode = String.format("%s-%02d", lineId, stationNumber);
        } else {
            // 自动分配站序号：找到线路中最大的站序号+1
            int maxNumber = 0;
            for (String existingCode : line.getStationCodes()) {
                if (existingCode.startsWith(lineId + "-")) {
                    try {
                        // 提取站序号部分（如 L1-01 -> 01 -> 1）
                        String numPart = existingCode.substring(lineId.length() + 1);
                        int num = Integer.parseInt(numPart);
                        if (num > maxNumber) {
                            maxNumber = num;
                        }
                    } catch (NumberFormatException e) {
                        // 忽略格式不正确的编码
                    }
                }
            }
            stationNumber = maxNumber + 1;
            stationCode = String.format("%s-%02d", lineId, stationNumber);
        }

        // 检查编码是否已存在
        if (NetworkManager.getStation(stationCode) != null) {
            TicketSystemMod.LOGGER.error("车站编码已存在: {}", stationCode);
            return false;
        }

        // 创建车站
        Station station = new Station(stationCode, chineseName,
                                     englishName != null ? englishName : "",
                                     x, y, z, stationNumber);

        // 添加到车站管理器
        boolean stationAdded = NetworkManager.addStation(station);
        if (!stationAdded) {
            TicketSystemMod.LOGGER.error("添加车站失败: {}", stationCode);
            return false;
        }

        // 将车站编码添加到线路末尾
        line.getStationCodes().add(stationCode);
        // 更新线路
        NetworkManager.removeLine(lineId);
        NetworkManager.addLine(line);

        TicketSystemMod.LOGGER.info("已添加车站: {} (编码: {}) 到线路: {}",
                                   chineseName, stationCode, lineId);
        return true;
    }

}