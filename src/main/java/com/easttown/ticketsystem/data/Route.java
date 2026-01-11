package com.easttown.ticketsystem.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 路径计算结果类
 * 用于存储票价计算的结果：经过的车站、线路、总价格等
 */
public class Route {
    private List<String> stationPath;   // 经过的车站编码列表
    private List<String> linePath;      // 经过的线路ID列表（用于换乘显示）
    private int totalPrice;             // 总价格（铜币）
    private int transferCount;          // 换乘次数
    private int stationCount;           // 经过的车站数（不包括起点）
    private String startStation;        // 起点车站
    private String endStation;          // 终点车站

    public Route(String startStation, String endStation) {
        this.startStation = startStation;
        this.endStation = endStation;
        this.stationPath = new ArrayList<>();
        this.linePath = new ArrayList<>();
        this.totalPrice = 0;
        this.transferCount = 0;
        this.stationCount = 0;
    }

    public Route(List<String> stationPath, List<String> linePath, int totalPrice,
                 int transferCount, String startStation, String endStation) {
        this.stationPath = new ArrayList<>(stationPath);
        this.linePath = new ArrayList<>(linePath);
        this.totalPrice = totalPrice;
        this.transferCount = transferCount;
        this.stationCount = stationPath.size() - 1; // 不包括起点
        this.startStation = startStation;
        this.endStation = endStation;
    }

    // Getter方法
    public List<String> getStationPath() {
        return new ArrayList<>(stationPath);
    }

    public List<String> getLinePath() {
        return new ArrayList<>(linePath);
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public int getTransferCount() {
        return transferCount;
    }

    public int getStationCount() {
        return stationCount;
    }

    public String getStartStation() {
        return startStation;
    }

    public String getEndStation() {
        return endStation;
    }

    // Setter方法
    public void setStationPath(List<String> stationPath) {
        this.stationPath = new ArrayList<>(stationPath);
        this.stationCount = stationPath.size() - 1; // 更新车站数
    }

    public void setLinePath(List<String> linePath) {
        this.linePath = new ArrayList<>(linePath);
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setTransferCount(int transferCount) {
        this.transferCount = transferCount;
    }

    // 便捷方法
    public void addStationToPath(String stationCode) {
        stationPath.add(stationCode);
        stationCount = stationPath.size() - 1;
    }

    public void addLineToPath(String lineId) {
        linePath.add(lineId);
    }

    public void addToTotalPrice(int price) {
        totalPrice += price;
    }

    public void incrementTransferCount() {
        transferCount++;
    }

    /**
     * 获取路径描述（简化显示）
     */
    public String getDescription() {
        if (stationPath.isEmpty()) {
            return String.format("从 %s 到 %s: 无路径", startStation, endStation);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("从 %s 到 %s: ", startStation, endStation));

        if (stationCount > 0) {
            sb.append(String.format("经过 %d 站", stationCount));
        } else {
            sb.append("直达");
        }

        if (transferCount > 0) {
            sb.append(String.format("，换乘 %d 次", transferCount));
        }

        sb.append(String.format("，票价 %d 铜币", totalPrice));
        return sb.toString();
    }

    /**
     * 检查路径是否有效（包含起点和终点）
     */
    public boolean isValid() {
        if (stationPath.isEmpty()) return false;
        String first = stationPath.get(0);
        String last = stationPath.get(stationPath.size() - 1);
        return startStation.equals(first) && endStation.equals(last);
    }

    /**
     * 获取换乘点列表
     */
    public List<String> getTransferPoints() {
        List<String> transfers = new ArrayList<>();
        if (linePath.size() <= 1) {
            return transfers; // 无需换乘
        }

        // 线路切换的位置就是换乘点
        String currentLine = linePath.get(0);
        for (int i = 1; i < linePath.size(); i++) {
            if (!linePath.get(i).equals(currentLine)) {
                // 线路切换，对应的车站是换乘点
                if (i - 1 < stationPath.size()) {
                    transfers.add(stationPath.get(i - 1));
                }
                currentLine = linePath.get(i);
            }
        }

        return transfers;
    }

    @Override
    public String toString() {
        return String.format("Route{start='%s', end='%s', stations=%d, lines=%d, price=%d, transfers=%d}",
                startStation, endStation, stationCount, linePath.size(), totalPrice, transferCount);
    }
}