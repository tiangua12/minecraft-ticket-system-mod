package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.config.TicketSystemConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 折扣管理器
 * 管理全局折扣配置，支持多个折扣活动
 * 折扣系数为0-1之间的小数，应用于总票价
 */
public class DiscountManager {
    // 折扣配置
    public static class DiscountConfig {
        private String name;          // 折扣活动名称
        private double discount;      // 折扣系数 (0-1)
        private boolean enabled;      // 是否启用
        private long startTime;       // 开始时间（毫秒时间戳）
        private long endTime;         // 结束时间（毫秒时间戳）

        public DiscountConfig() {
            this.name = "";
            this.discount = 1.0;
            this.enabled = false;
            this.startTime = 0;
            this.endTime = Long.MAX_VALUE;
        }

        public DiscountConfig(String name, double discount) {
            this.name = name;
            this.discount = discount;
            this.enabled = true;
            this.startTime = System.currentTimeMillis();
            this.endTime = Long.MAX_VALUE;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getDiscount() { return discount; }
        public void setDiscount(double discount) {
            // 确保折扣在合理范围内
            if (discount < 0) discount = 0;
            if (discount > 1) discount = 1;
            this.discount = discount;
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }

        /**
         * 检查折扣是否有效（启用且在有效期内）
         */
        public boolean isValid() {
            if (!enabled) return false;
            long now = System.currentTimeMillis();
            return now >= startTime && now <= endTime;
        }

        /**
         * 应用折扣到价格
         * @param originalPrice 原始价格
         * @return 折扣后价格（向下取整）
         */
        public int applyDiscount(int originalPrice) {
            if (!isValid() || discount >= 1.0) {
                return originalPrice;
            }
            double discounted = originalPrice * discount;
            return (int) Math.floor(discounted);
        }
    }

    // 配置文件路径
    private static final String CONFIG_FILE_NAME = "discounts.json";
    private static File configFile;

    // Gson实例
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 当前折扣配置
    private static DiscountConfig currentDiscount = new DiscountConfig();

    // 折扣历史记录（按名称索引）
    private static Map<String, DiscountConfig> discountHistory = new HashMap<>();

    // 初始化标志
    private static boolean initialized = false;

    /**
     * 初始化折扣管理器
     * @param world 服务器世界（用于确定配置文件路径）
     */
    public static void initialize(ServerLevel world) {
        if (initialized) {
            return;
        }

        try {
            // 确定配置文件路径
            File modDir = new File("mods/" + TicketSystemMod.MODID);
            if (!modDir.exists()) {
                modDir.mkdirs();
            }
            configFile = new File(modDir, CONFIG_FILE_NAME);

            // 加载现有配置
            loadConfig();

            initialized = true;
            TicketSystemMod.LOGGER.info("DiscountManager initialized");
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to initialize DiscountManager", e);
        }
    }

    /**
     * 初始化（简化版本，用于客户端或测试）
     */
    public static void initialize() {
        try {
            File modDir = new File("mods/" + TicketSystemMod.MODID);
            if (!modDir.exists()) {
                modDir.mkdirs();
            }
            configFile = new File(modDir, CONFIG_FILE_NAME);

            // 尝试加载配置，如果文件不存在则使用默认值
            if (configFile.exists()) {
                loadConfig();
            }

            initialized = true;
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to initialize DiscountManager", e);
        }
    }

    /**
     * 加载折扣配置
     */
    private static void loadConfig() {
        try (FileReader reader = new FileReader(configFile)) {
            DiscountConfig loaded = GSON.fromJson(reader, DiscountConfig.class);
            if (loaded != null) {
                currentDiscount = loaded;
                TicketSystemMod.LOGGER.debug("Loaded discount config: {} ({}%)",
                    currentDiscount.getName(), currentDiscount.getDiscount() * 100);
            }
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to load discount config", e);
            // 使用默认配置
            currentDiscount = new DiscountConfig();
        }
    }

    /**
     * 保存折扣配置
     */
    private static void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(currentDiscount, writer);
        } catch (IOException e) {
            TicketSystemMod.LOGGER.error("Failed to save discount config", e);
        }
    }

    /**
     * 设置当前折扣
     * @param name 折扣名称
     * @param discount 折扣系数 (0-1)
     * @return 是否成功
     */
    public static boolean setDiscount(String name, double discount) {
        // 验证折扣系数
        if (discount < 0 || discount > 1) {
            TicketSystemMod.LOGGER.error("Invalid discount value: {} (must be between 0 and 1)", discount);
            return false;
        }

        // 创建新的折扣配置
        DiscountConfig newDiscount = new DiscountConfig(name, discount);
        newDiscount.setEnabled(true);

        // 保存到历史记录
        discountHistory.put(name, newDiscount);

        // 更新当前折扣
        currentDiscount = newDiscount;

        // 保存到文件
        saveConfig();

        TicketSystemMod.LOGGER.info("Discount set: {} ({}%)", name, discount * 100);
        return true;
    }

    /**
     * 清除当前折扣（恢复原价）
     */
    public static void clearDiscount() {
        currentDiscount = new DiscountConfig();
        saveConfig();
        TicketSystemMod.LOGGER.info("Discount cleared");
    }

    /**
     * 禁用当前折扣（但保留配置）
     */
    public static void disableDiscount() {
        currentDiscount.setEnabled(false);
        saveConfig();
        TicketSystemMod.LOGGER.info("Discount disabled");
    }

    /**
     * 启用当前折扣
     */
    public static void enableDiscount() {
        currentDiscount.setEnabled(true);
        saveConfig();
        TicketSystemMod.LOGGER.info("Discount enabled");
    }

    /**
     * 检查是否有有效折扣
     */
    public static boolean hasActiveDiscount() {
        return currentDiscount.isValid();
    }

    /**
     * 获取当前折扣名称
     */
    public static String getCurrentDiscountName() {
        return currentDiscount.getName();
    }

    /**
     * 获取当前折扣系数
     */
    public static double getCurrentDiscountFactor() {
        return currentDiscount.getDiscount();
    }

    /**
     * 应用折扣到价格
     * @param originalPrice 原始价格
     * @return 折扣后价格
     */
    public static int applyDiscount(int originalPrice) {
        return currentDiscount.applyDiscount(originalPrice);
    }

    /**
     * 获取折扣信息（用于显示）
     */
    public static String getDiscountInfo() {
        if (!hasActiveDiscount()) {
            return "无折扣";
        }
        return String.format("%s (%.0f%%)",
            currentDiscount.getName(),
            currentDiscount.getDiscount() * 100);
    }

    /**
     * 获取折扣节省金额
     * @param originalPrice 原始价格
     * @return 节省的金额
     */
    public static int getDiscountSavings(int originalPrice) {
        if (!hasActiveDiscount()) {
            return 0;
        }
        int discounted = applyDiscount(originalPrice);
        return originalPrice - discounted;
    }

    /**
     * 验证折扣配置
     * @return 验证错误信息列表，空列表表示配置有效
     */
    public static java.util.List<String> validateConfig() {
        java.util.List<String> errors = new java.util.ArrayList<>();

        if (currentDiscount.getDiscount() < 0 || currentDiscount.getDiscount() > 1) {
            errors.add(String.format("折扣系数超出范围: %.2f (必须介于0-1之间)", currentDiscount.getDiscount()));
        }

        if (currentDiscount.isEnabled() && currentDiscount.getEndTime() < System.currentTimeMillis()) {
            errors.add("折扣已过期");
        }

        return errors;
    }

    /**
     * 重新加载配置（热重载）
     */
    public static void reload() {
        if (configFile != null && configFile.exists()) {
            loadConfig();
            TicketSystemMod.LOGGER.info("Discount config reloaded");
        }
    }
}