package com.easttown.ticketsystem.data;

/**
 * 票价数据类 - 参考web版本的fares.json结构
 * 文档要求：删除双票价系统，只保留单票价
 */
public class Fare {
    private final String fromStation;  // 起点车站编码
    private final String toStation;    // 终点车站编码
    private final int price;           // 票价（铜币）

    public Fare(String fromStation, String toStation, int price) {
        this.fromStation = fromStation;
        this.toStation = toStation;
        this.price = price;
    }

    // Getter方法
    public String getFromStation() {
        return fromStation;
    }

    public String getToStation() {
        return toStation;
    }

    public int getPrice() {
        return price;
    }

    /**
     * 检查票价是否有效（价格为正数，车站编码不为空）
     */
    public boolean isValid() {
        return fromStation != null && !fromStation.isEmpty() &&
               toStation != null && !toStation.isEmpty() &&
               price > 0;
    }

    /**
     * 检查是否为指定区间的票价（方向敏感）
     */
    public boolean isForSegment(String from, String to) {
        return fromStation.equals(from) && toStation.equals(to);
    }

    /**
     * 检查是否为指定区间的票价（方向不敏感，双向匹配）
     */
    public boolean isForSegmentBidirectional(String station1, String station2) {
        return (fromStation.equals(station1) && toStation.equals(station2)) ||
               (fromStation.equals(station2) && toStation.equals(station1));
    }

    /**
     * 获取对称票价（交换起点终点）
     */
    public Fare getSymmetricFare() {
        return new Fare(toStation, fromStation, price);
    }

    @Override
    public String toString() {
        return String.format("Fare{from='%s', to='%s', price=%d}",
                fromStation, toStation, price);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Fare fare = (Fare) obj;
        // 方向敏感的相等性比较
        return price == fare.price &&
               fromStation.equals(fare.fromStation) &&
               toStation.equals(fare.toStation);
    }

    @Override
    public int hashCode() {
        int result = fromStation.hashCode();
        result = 31 * result + toStation.hashCode();
        result = 31 * result + price;
        return result;
    }
}