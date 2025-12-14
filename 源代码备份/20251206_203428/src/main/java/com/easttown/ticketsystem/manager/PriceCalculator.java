package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.config.TicketSystemConfig;
import net.minecraft.world.entity.player.Player;

public class PriceCalculator {
    public static int calculatePrice(String startStation, String destination) {
        if (startStation.equals(destination)) return 0;
        
        double distance = StationManager.calculateDistance(startStation, destination);
        
        // 确保距离是有效的
        if (distance <= 0) return 0;
        
        // 使用新的配置系统计算价格（支持小数，向上取整）
        int price = TicketSystemConfig.calculateTotalCost((int) distance);

        // 限制价格在合理范围内
        return Math.min(price, Integer.MAX_VALUE);
    }
    
    public static boolean deductPayment(Player player, int price) {
        if (price <= 0) return true;
        return CoinSystem.deductWithChange(player, price)!=null;
    }
}