package com.easttown.ticketsystem.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 线路数据类 - 参考web版本的lines.json结构
 * 文档要求：简化线路显示，不使用SVG，只显示车站归属关系
 */
public class Line {
    private final String id;          // 线路ID，如"1"（唯一）
    private String name;             // 线路中文名称
    private String enName;           // 线路英文名称（可选）
    private String color;            // 线路颜色，如"#FF0000"或颜色名称
    private List<String> stationCodes; // 车站编码列表（有序）

    public Line(String id, String name, String color) {
        this.id = id;
        this.name = name;
        this.enName = "";
        this.color = color;
        this.stationCodes = new ArrayList<>();
    }

    public Line(String id, String name, String enName, String color) {
        this.id = id;
        this.name = name;
        this.enName = enName;
        this.color = color;
        this.stationCodes = new ArrayList<>();
    }

    // Getter方法
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEnName() {
        return enName;
    }

    public String getColor() {
        return color;
    }

    public List<String> getStationCodes() {
        return stationCodes;
    }

    // Setter方法（除了id是final）
    public void setName(String name) {
        this.name = name;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setStationCodes(List<String> stationCodes) {
        this.stationCodes = stationCodes;
    }

    // 便捷方法
    /**
     * 添加车站到线路末尾
     * @param stationCode 车站编码
     */
    public void addStation(String stationCode) {
        if (!stationCodes.contains(stationCode)) {
            stationCodes.add(stationCode);
        }
    }

    /**
     * 在指定位置插入车站
     * @param index 插入位置
     * @param stationCode 车站编码
     */
    public void insertStation(int index, String stationCode) {
        if (!stationCodes.contains(stationCode)) {
            if (index >= 0 && index <= stationCodes.size()) {
                stationCodes.add(index, stationCode);
            } else {
                stationCodes.add(stationCode);
            }
        }
    }

    /**
     * 移除车站
     * @param stationCode 车站编码
     * @return 是否成功移除
     */
    public boolean removeStation(String stationCode) {
        return stationCodes.remove(stationCode);
    }

    /**
     * 获取车站数量
     */
    public int getStationCount() {
        return stationCodes.size();
    }

    /**
     * 检查是否包含指定车站
     */
    public boolean containsStation(String stationCode) {
        return stationCodes.contains(stationCode);
    }

    /**
     * 获取车站在线路中的顺序（0-based）
     * @return 位置索引，如果不存在返回-1
     */
    public int getStationOrder(String stationCode) {
        return stationCodes.indexOf(stationCode);
    }

    /**
     * 获取相邻车站（如果存在）
     * @param stationCode 当前车站
     * @return 前一个和后一个车站的数组[prev, next]，可能为null
     */
    public String[] getAdjacentStations(String stationCode) {
        int index = stationCodes.indexOf(stationCode);
        if (index == -1) {
            return null;
        }

        String prev = index > 0 ? stationCodes.get(index - 1) : null;
        String next = index < stationCodes.size() - 1 ? stationCodes.get(index + 1) : null;
        return new String[]{prev, next};
    }

    /**
     * 检查线路是否有效（至少有2个车站）
     */
    public boolean isValid() {
        return stationCodes.size() >= 2;
    }

    @Override
    public String toString() {
        return String.format("Line{id='%s', name='%s', enName='%s', color='%s', stations=%d}",
                id, name, enName, color, stationCodes.size());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Line line = (Line) obj;
        return id.equals(line.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}