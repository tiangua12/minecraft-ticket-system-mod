package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.config.CoinConfig;
import net.minecraft.world.entity.player.Player;

public class PriceCalculator {
    public static int calculatePrice(String startStation, String destination) {
        if (startStation.equals(destination)) return 0;
        
        double distance = StationManager.calculateDistance(startStation, destination);
        
        // 确保距离是有效的
        if (distance <= 0) return 0;
        
        // 计算价格，确保不会溢出
        long price = (long) (distance * CoinConfig.getCostPerBlock());
        
        // 限制价格在合理范围内
        return (int) Math.min(price, Integer.MAX_VALUE);
    }
    
    public static boolean deductPayment(Player player, int price) {
        if (price <= 0) return true;
        return CoinSystem.deductWithChange(player, price)!=null;
    }
}