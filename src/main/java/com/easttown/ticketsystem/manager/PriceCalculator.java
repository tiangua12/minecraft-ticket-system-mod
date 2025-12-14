package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.config.TicketSystemConfig;
import com.easttown.ticketsystem.data.Fare;
import com.easttown.ticketsystem.data.Route;
import net.minecraft.world.entity.player.Player;

/**
 * 价格计算器 - 重构版本
 * 文档要求：基于票价表查询，删除双票价系统，支持线路网络
 * 优先使用票价表，如果没有定义则回退到距离计算
 */
public class PriceCalculator {
    // 缓存最近的计算结果（简单实现）
    private static final java.util.Map<String, Integer> priceCache = new java.util.HashMap<>();
    private static final int CACHE_SIZE = 100;

    /**
     * 计算票价（主要方法）
     * @param startStation 起点车站名称（兼容旧代码）或车站编码
     * @param destination 终点车站名称（兼容旧代码）或车站编码
     * @return 票价（铜币），如果无法计算返回0
     */
    public static int calculatePrice(String startStation, String destination) {
        if (startStation == null || destination == null ||
                startStation.isEmpty() || destination.isEmpty()) {
            return 0;
        }

        if (startStation.equals(destination)) {
            return 0;
        }

        // 初始化折扣管理器
        DiscountManager.initialize();
        // 检查缓存（使用规范化缓存键，使区间票价无方向性）
        String cacheKey = getNormalizedCacheKey(startStation, destination);
        Integer cachedPrice = priceCache.get(cacheKey);
        if (cachedPrice != null) {
            return cachedPrice;
        }

        // 转换车站名称到编码（如果需要）
        String startCode = convertToStationCode(startStation);
        String destCode = convertToStationCode(destination);

        if (startCode == null || destCode == null) {
            // 车站不存在，使用旧的距离计算作为回退
            return calculatePriceByDistance(startStation, destination);
        }

        // 使用票价表计算
        int price = calculatePriceByFareTable(startCode, destCode);

        // 更新缓存
        if (priceCache.size() >= CACHE_SIZE) {
            // 简单LRU：清除一半缓存
            java.util.Iterator<String> it = priceCache.keySet().iterator();
            for (int i = 0; i < CACHE_SIZE / 2 && it.hasNext(); i++) {
                it.next();
                it.remove();
            }
        }
        priceCache.put(cacheKey, price);

        // 应用折扣
        int finalPrice = DiscountManager.applyDiscount(price);
        return finalPrice;
    }

    /**
     * 使用票价表计算价格（新系统）
     */
    private static int calculatePriceByFareTable(String startCode, String destCode) {
        // 初始化管理器
        NetworkManager.initialize();

        // 方法1：直接查询票价表
        Fare fare = FareManager.getFare(startCode, destCode);
        if (fare != null && fare.getPrice() > 0) {
            return fare.getPrice();
        }

        // 方法2：使用路线计算器查找最优路径
        Route route = RouteCalculator.findCheapestRoute(startCode, destCode);
        if (route != null && route.getTotalPrice() > 0) {
            return route.getTotalPrice();
        }

        // 方法3：如果票价表不完整，使用距离计算作为回退
        TicketSystemMod.LOGGER.debug("No fare definition found for {} -> {}, falling back to distance calculation",
                startCode, destCode);
        return calculatePriceByDistance(startCode, destCode);
    }

    /**
     * 使用距离计算价格（旧系统，作为回退）
     */
    private static int calculatePriceByDistance(String startStation, String destination) {
        // 尝试使用StationManagerCompat保持兼容性
        try {
            double distance = StationManagerCompat.calculateDistance(startStation, destination);
            if (distance <= 0) {
                return 0;
            }

            // 使用新的配置系统计算价格（支持小数，向上取整）
            int price = TicketSystemConfig.calculateTotalCost((int) distance);

            // 限制价格在合理范围内
            return Math.min(price, Integer.MAX_VALUE);
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Error in distance-based price calculation", e);
            return 100; // 默认票价
        }
    }

    /**
     * 转换车站标识符（名称或编码）
     */
    private static String convertToStationCode(String stationIdentifier) {
        // 如果已经是编码格式（如"01-01"），直接返回
        if (stationIdentifier.matches("\\d{2}-\\d{2}")) {
            // 检查编码是否存在
            if (NetworkManager.hasStation(stationIdentifier)) {
                return stationIdentifier;
            }
        }

        // 尝试通过名称查找编码
        NetworkManager.initialize();
        for (com.easttown.ticketsystem.data.Station station : NetworkManager.getAllStations()) {
            if (station.getName().equals(stationIdentifier) ||
                    station.getCode().equals(stationIdentifier)) {
                return station.getCode();
            }
        }

        // 未找到
        return null;
    }

    /**
     * 扣除支付（兼容原有API）
     */
    public static boolean deductPayment(Player player, int price) {
        if (price <= 0) {
            return true;
        }

        // 使用CoinSystem扣除支付
        return CoinSystem.deductWithChange(player, price) != null;
    }

    /**
     * 计算票价并验证支付能力
     */
    public static PriceResult calculateAndValidatePrice(String startStation, String destination, Player player) {
        int price = calculatePrice(startStation, destination);

        // 检查玩家是否有足够金钱
        boolean canAfford = CoinSystem.hasSufficientCoins(player, price);

        return new PriceResult(price, canAfford);
    }

    /**
     * 获取路径详情（而不仅仅是价格）
     */
    public static Route getRouteDetails(String startStation, String destination) {
        String startCode = convertToStationCode(startStation);
        String destCode = convertToStationCode(destination);

        if (startCode == null || destCode == null) {
            return null;
        }

        NetworkManager.initialize();
        return RouteCalculator.findCheapestRoute(startCode, destCode);
    }

    /**
     * 清除价格缓存
     */
    public static void clearCache() {
        priceCache.clear();
    }

    /**
     * 获取缓存统计
     */
    public static String getCacheStats() {
        return String.format("Price cache: %d entries", priceCache.size());
    }

    /**
     * 验证票价表完整性
     */
    public static java.util.List<String> validateFareTable() {
        NetworkManager.initialize();
        return NetworkManager.validateData();
    }

    /**
     * 生成基础票价表（基于现有车站距离）
     */
    public static boolean generateBasicFares() {
        NetworkManager.initialize();
        java.util.List<String> issues = new java.util.ArrayList<>();

        java.util.Collection<com.easttown.ticketsystem.data.Station> stations = NetworkManager.getAllStations();
        if (stations.size() < 2) {
            TicketSystemMod.LOGGER.warn("Not enough stations to generate fares");
            return false;
        }

        int generatedCount = 0;
        com.easttown.ticketsystem.data.Station[] stationArray = stations.toArray(new com.easttown.ticketsystem.data.Station[0]);

        // 为所有车站对生成基础票价
        for (int i = 0; i < stationArray.length; i++) {
            for (int j = i + 1; j < stationArray.length; j++) {
                com.easttown.ticketsystem.data.Station s1 = stationArray[i];
                com.easttown.ticketsystem.data.Station s2 = stationArray[j];

                // 检查是否已有票价
                if (!NetworkManager.hasFare(s1.getCode(), s2.getCode())) {
                    // 基于距离计算票价
                    int price = calculatePriceByDistance(s1.getName(), s2.getName());

                    // 添加双向票价
                    Fare fare = new Fare(s1.getCode(), s2.getCode(), price);
                    if (NetworkManager.addBidirectionalFare(fare)) {
                        generatedCount++;
                    } else {
                        issues.add(String.format("Failed to generate fare: %s -> %s", s1.getCode(), s2.getCode()));
                    }
                }
            }
        }

        TicketSystemMod.LOGGER.info("Generated {} basic fares, {} issues", generatedCount, issues.size());
        return generatedCount > 0;
    }

    /**
     * 获取规范化缓存键（使区间票价无方向性）
     * 对两个车站标识符按字母顺序排序，然后连接
     */
    private static String getNormalizedCacheKey(String station1, String station2) {
        if (station1.compareTo(station2) <= 0) {
            return station1 + "->" + station2;
        } else {
            return station2 + "->" + station1;
        }
    }

    // ==================== 辅助类 ====================

    /**
     * 价格计算结果
     */
    public static class PriceResult {
        private final int price;
        private final boolean canAfford;

        public PriceResult(int price, boolean canAfford) {
            this.price = price;
            this.canAfford = canAfford;
        }

        public int getPrice() {
            return price;
        }

        public boolean canAfford() {
            return canAfford;
        }

        @Override
        public String toString() {
            return String.format("Price: %d铜币, 可支付: %s", price, canAfford ? "是" : "否");
        }
    }
}