package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.data.Line;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 线路管理器
 * 提供线路管理的专门API，底层使用NetworkManager
 */
public class LineManager {
    /**
     * 初始化线路管理器
     */
    public static void initialize() {
        NetworkManager.initialize();
    }

    /**
     * 添加线路
     */
    public static boolean addLine(Line line) {
        return NetworkManager.addLine(line);
    }

    /**
     * 添加线路（简化参数）
     */
    public static boolean addLine(String id, String name, String color) {
        Line line = new Line(id, name, color);
        return addLine(line);
    }

    /**
     * 移除线路
     */
    public static boolean removeLine(String lineId) {
        return NetworkManager.removeLine(lineId);
    }

    /**
     * 获取线路
     */
    public static Line getLine(String lineId) {
        return NetworkManager.getLine(lineId);
    }

    /**
     * 获取所有线路ID
     */
    public static Set<String> getAllLineIds() {
        return NetworkManager.getLineIds();
    }

    /**
     * 获取所有线路
     */
    public static Collection<Line> getAllLines() {
        return NetworkManager.getAllLines();
    }

    /**
     * 检查线路是否存在
     */
    public static boolean hasLine(String lineId) {
        return NetworkManager.hasLine(lineId);
    }

    /**
     * 更新线路
     */
    public static boolean updateLine(Line line) {
        return NetworkManager.updateLine(line);
    }

    /**
     * 将车站添加到线路
     */
    public static boolean addStationToLine(String lineId, String stationCode) {
        Line line = getLine(lineId);
        if (line == null) {
            return false;
        }

        line.addStation(stationCode);
        return updateLine(line);
    }

    /**
     * 将车站插入到线路的指定位置
     */
    public static boolean insertStationToLine(String lineId, int index, String stationCode) {
        Line line = getLine(lineId);
        if (line == null) {
            return false;
        }

        line.insertStation(index, stationCode);
        return updateLine(line);
    }

    /**
     * 从线路移除车站
     */
    public static boolean removeStationFromLine(String lineId, String stationCode) {
        Line line = getLine(lineId);
        if (line == null) {
            return false;
        }

        boolean removed = line.removeStation(stationCode);
        if (removed) {
            return updateLine(line);
        }
        return false;
    }

    /**
     * 获取包含指定车站的所有线路
     */
    public static List<Line> getLinesContainingStation(String stationCode) {
        return NetworkManager.getLinesContainingStation(stationCode);
    }

    /**
     * 检查车站是否属于线路
     */
    public static boolean isStationInLine(String lineId, String stationCode) {
        Line line = getLine(lineId);
        return line != null && line.containsStation(stationCode);
    }

    /**
     * 获取线路中的车站顺序
     */
    public static int getStationOrderInLine(String lineId, String stationCode) {
        Line line = getLine(lineId);
        return line != null ? line.getStationOrder(stationCode) : -1;
    }

    /**
     * 获取线路中的相邻车站
     */
    public static String[] getAdjacentStationsInLine(String lineId, String stationCode) {
        Line line = getLine(lineId);
        return line != null ? line.getAdjacentStations(stationCode) : null;
    }

    /**
     * 验证线路是否有效（至少2个车站）
     */
    public static boolean isValidLine(String lineId) {
        Line line = getLine(lineId);
        return line != null && line.isValid();
    }

    /**
     * 获取线路统计信息
     */
    public static String getLineStats(String lineId) {
        Line line = getLine(lineId);
        if (line == null) {
            return "线路不存在";
        }

        return String.format("线路 %s: %s, %d个车站, 颜色: %s",
                lineId, line.getName(), line.getStationCount(), line.getColor());
    }

    /**
     * 重新加载线路数据
     */
    public static void reloadLines() {
        NetworkManager.reloadAll();
    }
}