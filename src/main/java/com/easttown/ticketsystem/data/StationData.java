package com.easttown.ticketsystem.data;

public class StationData {
    private final int x;
    private final int y;
    private final int z;
    private int price; // 价格（以铜币为单位）
    
    public StationData(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.price = 100; // 默认价格
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
    
    public int getPrice() {
        return price;
    }
    
    public void setPrice(int price) {
        this.price = price;
    }
}