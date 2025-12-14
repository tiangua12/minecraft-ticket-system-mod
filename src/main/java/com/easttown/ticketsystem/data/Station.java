package com.easttown.ticketsystem.data;

/**
 * 车站数据类 - 参考web版本的stations.json结构
 * 文档要求：删除双票价系统，简化实现，参考web版本数据结构
 */
public class Station {
    private final String code;        // 车站编码，如"01-01"（唯一标识）
    private String name;             // 车站中文名称
    private String enName;           // 车站英文名称（可选）
    private int x, y, z;             // 坐标（用于Minecraft地图显示）
    private int stationNumber;       // 站序号（在线路中的顺序）

    public Station(String code, String name) {
        this.code = code;
        this.name = name;
        this.enName = "";
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.stationNumber = 0;
    }

    public Station(String code, String name, String enName, int x, int y, int z) {
        this.code = code;
        this.name = name;
        this.enName = enName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.stationNumber = 0;
    }

    public Station(String code, String name, String enName, int x, int y, int z, int stationNumber) {
        this.code = code;
        this.name = name;
        this.enName = enName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.stationNumber = stationNumber;
    }

    // Getter方法
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getEnName() {
        return enName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    // Setter方法（除了code是final）
    public void setName(String name) {
        this.name = name;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int getStationNumber() {
        return stationNumber;
    }

    public void setStationNumber(int stationNumber) {
        this.stationNumber = stationNumber;
    }

    /**
     * 设置坐标
     */
    public void setCoordinates(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * 检查坐标是否有效
     * @return 坐标是否在合理范围内
     */
    public boolean isValidCoordinate() {
        // Minecraft世界坐标范围
        return x >= -30000000 && x <= 30000000 &&
               y >= -2048 && y <= 2048 &&
               z >= -30000000 && z <= 30000000;
    }

    @Override
    public String toString() {
        return String.format("Station{code='%s', name='%s', enName='%s', x=%d, y=%d, z=%d, stationNumber=%d}",
                code, name, enName, x, y, z, stationNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Station station = (Station) obj;
        return code.equals(station.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}