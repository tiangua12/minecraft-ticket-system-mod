package com.easttown.ticketsystem.util;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.config.TicketSystemConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class TicketSystemLogger {

    private static final String LOG_FILE_PATH = "logs/ticketsystem.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // 日志级别
    public enum LogLevel {
        INFO,
        WARN,
        ERROR
    }

    // 日志类型
    public enum LogType {
        TICKET_PURCHASE("Ticket Purchase", "购买车票"),
        GATE_PASSAGE("Gate Passage", "通过闸机"),
        REFUND("Refund", "退票"),
        SYSTEM("System", "系统"),
        COIN("Coin", "硬币"),
        ADMIN("Admin", "管理");

        private final String englishName;
        private final String chineseName;

        LogType(String englishName, String chineseName) {
            this.englishName = englishName;
            this.chineseName = chineseName;
        }

        public String getEnglishName() {
            return englishName;
        }

        public String getChineseName() {
            return chineseName;
        }
    }

    /**
     * 记录购买车票日志
     */
    public static void logTicketPurchase(Player player, String startStation, String destination, int price,
                                        String ticketId, String paymentMethod) {
        String message = String.format(
            "[Ticket Purchase] Player: %s, Start: %s, Destination: %s, Price: %d, Ticket ID: %s, Payment: %s | " +
            "[购买车票] 玩家: %s, 起点: %s, 终点: %s, 价格: %d, 车票ID: %s, 支付方式: %s",
            player.getName().getString(), startStation, destination, price, ticketId, paymentMethod,
            player.getName().getString(), startStation, destination, price, ticketId, paymentMethod
        );
        logToFile(LogType.TICKET_PURCHASE, LogLevel.INFO, message);

        // 同时输出到控制台（如果启用了调试日志）
        if (TicketSystemConfig.showDebugLogs()) {
            TicketSystemMod.LOGGER.info("[Ticket Purchase] Player: {}, Start: {}, Destination: {}, Price: {}, Ticket ID: {}, Payment: {}",
                player.getName().getString(), startStation, destination, price, ticketId, paymentMethod);
        }
    }

    /**
     * 记录通过闸机日志
     */
    public static void logGatePassage(Player player, String station, String ticketId, boolean success,
                                     String reason, String gateType) {
        String status = success ? "SUCCESS" : "FAILED";
        String statusCn = success ? "成功" : "失败";

        String message = String.format(
            "[Gate Passage] Player: %s, Station: %s, Ticket ID: %s, Status: %s, Reason: %s, Gate Type: %s | " +
            "[通过闸机] 玩家: %s, 站点: %s, 车票ID: %s, 状态: %s, 原因: %s, 闸机类型: %s",
            player.getName().getString(), station, ticketId, status, reason, gateType,
            player.getName().getString(), station, ticketId, statusCn, reason, gateType
        );
        logToFile(LogType.GATE_PASSAGE, LogLevel.INFO, message);

        // 同时输出到控制台（如果启用了调试日志）
        if (TicketSystemConfig.showDebugLogs()) {
            TicketSystemMod.LOGGER.info("[Gate Passage] Player: {}, Station: {}, Ticket ID: {}, Status: {}, Reason: {}, Gate Type: {}",
                player.getName().getString(), station, ticketId, status, reason, gateType);
        }
    }

    /**
     * 记录退票日志
     */
    public static void logRefund(Player player, String ticketId, int originalPrice, int refundAmount,
                                String refundReason) {
        String message = String.format(
            "[Refund] Player: %s, Ticket ID: %s, Original Price: %d, Refund Amount: %d, Reason: %s | " +
            "[退票] 玩家: %s, 车票ID: %s, 原价: %d, 退款金额: %d, 原因: %s",
            player.getName().getString(), ticketId, originalPrice, refundAmount, refundReason,
            player.getName().getString(), ticketId, originalPrice, refundAmount, refundReason
        );
        logToFile(LogType.REFUND, LogLevel.INFO, message);

        if (TicketSystemConfig.showDebugLogs()) {
            TicketSystemMod.LOGGER.info("[Refund] Player: {}, Ticket ID: {}, Original Price: {}, Refund Amount: {}, Reason: {}",
                player.getName().getString(), ticketId, originalPrice, refundAmount, refundReason);
        }
    }

    /**
     * 记录硬币操作日志
     */
    public static void logCoinOperation(Player player, String operation, int amount, String coinType,
                                      String details) {
        String message = String.format(
            "[Coin Operation] Player: %s, Operation: %s, Amount: %d, Coin Type: %s, Details: %s | " +
            "[硬币操作] 玩家: %s, 操作: %s, 金额: %d, 硬币类型: %s, 详情: %s",
            player.getName().getString(), operation, amount, coinType, details,
            player.getName().getString(), operation, amount, coinType, details
        );
        logToFile(LogType.COIN, LogLevel.INFO, message);

        if (TicketSystemConfig.showDebugLogs()) {
            TicketSystemMod.LOGGER.info("[Coin Operation] Player: {}, Operation: {}, Amount: {}, Coin Type: {}, Details: {}",
                player.getName().getString(), operation, amount, coinType, details);
        }
    }

    /**
     * 记录管理员操作日志
     */
    public static void logAdminOperation(Player admin, String operation, String target, String details) {
        String message = String.format(
            "[Admin Operation] Admin: %s, Operation: %s, Target: %s, Details: %s | " +
            "[管理员操作] 管理员: %s, 操作: %s, 目标: %s, 详情: %s",
            admin.getName().getString(), operation, target, details,
            admin.getName().getString(), operation, target, details
        );
        logToFile(LogType.ADMIN, LogLevel.INFO, message);

        if (TicketSystemConfig.showDebugLogs()) {
            TicketSystemMod.LOGGER.info("[Admin Operation] Admin: {}, Operation: {}, Target: {}, Details: {}",
                admin.getName().getString(), operation, target, details);
        }
    }

    /**
     * 记录系统日志
     */
    public static void logSystem(String component, String action, String details) {
        String message = String.format(
            "[System] Component: %s, Action: %s, Details: %s | " +
            "[系统] 组件: %s, 操作: %s, 详情: %s",
            component, action, details,
            component, action, details
        );
        logToFile(LogType.SYSTEM, LogLevel.INFO, message);

        if (TicketSystemConfig.showDebugLogs()) {
            TicketSystemMod.LOGGER.info("[System] Component: {}, Action: {}, Details: {}",
                component, action, details);
        }
    }

    /**
     * 记录警告日志
     */
    public static void logWarning(LogType type, String message) {
        String fullMessage = String.format(
            "[%s] WARNING: %s | " +
            "[%s] 警告: %s",
            type.getEnglishName(), message,
            type.getChineseName(), message
        );
        logToFile(type, LogLevel.WARN, fullMessage);

        TicketSystemMod.LOGGER.warn("[{}] {}", type.getEnglishName(), message);
    }

    /**
     * 记录错误日志
     */
    public static void logError(LogType type, String message, Throwable throwable) {
        String fullMessage = String.format(
            "[%s] ERROR: %s | " +
            "[%s] 错误: %s",
            type.getEnglishName(), message,
            type.getChineseName(), message
        );
        logToFile(type, LogLevel.ERROR, fullMessage);

        TicketSystemMod.LOGGER.error("[{}] {}", type.getEnglishName(), message, throwable);
    }

    /**
     * 记录错误日志（无异常）
     */
    public static void logError(LogType type, String message) {
        String fullMessage = String.format(
            "[%s] ERROR: %s | " +
            "[%s] 错误: %s",
            type.getEnglishName(), message,
            type.getChineseName(), message
        );
        logToFile(type, LogLevel.ERROR, fullMessage);

        TicketSystemMod.LOGGER.error("[{}] {}", type.getEnglishName(), message);
    }

    /**
     * 将日志写入文件
     */
    private static synchronized void logToFile(LogType type, LogLevel level, String message) {
        try {
            File logFile = new File(LOG_FILE_PATH);
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                String timestamp = DATE_FORMAT.format(new Date());
                String logEntry = String.format("[%s] [%s] [%s] %s",
                    timestamp, level.name(), type.getEnglishName(), message);
                writer.println(logEntry);
            }
        } catch (IOException e) {
            // 如果文件写入失败，只输出到控制台
            TicketSystemMod.LOGGER.error("Failed to write to ticket system log file: {}", e.getMessage());
        }
    }

    /**
     * 初始化日志系统
     */
    public static void initialize() {
        try {
            File logFile = new File(LOG_FILE_PATH);
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 写入初始化信息
            String timestamp = DATE_FORMAT.format(new Date());
            String initMessage = String.format(
                "[%s] [INFO] [System] Ticket System Logger initialized | " +
                "[%s] [INFO] [系统] 车票系统日志记录器已初始化",
                timestamp, timestamp
            );

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println("=" .repeat(80));
                writer.println(initMessage);
                writer.println("=" .repeat(80));
            }

            TicketSystemMod.LOGGER.info("Ticket System Logger initialized successfully");
        } catch (IOException e) {
            TicketSystemMod.LOGGER.error("Failed to initialize ticket system log file: {}", e.getMessage());
        }
    }
}