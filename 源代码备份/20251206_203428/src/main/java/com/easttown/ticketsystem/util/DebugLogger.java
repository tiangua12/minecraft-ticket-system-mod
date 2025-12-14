package com.easttown.ticketsystem.util;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.config.TicketSystemConfig;

public class DebugLogger {

    public static void info(String message, Object... params) {
        if (TicketSystemConfig.showDebugLogs()) {
            TicketSystemMod.LOGGER.info(message, params);
        }
    }

    public static void debug(String message, Object... params) {
        if (TicketSystemConfig.showDebugLogs()) {
            TicketSystemMod.LOGGER.debug(message, params);
        }
    }

    public static void warn(String message, Object... params) {
        if (TicketSystemConfig.showDebugLogs()) {
            TicketSystemMod.LOGGER.warn(message, params);
        }
    }

    public static void error(String message, Object... params) {
        // 错误日志总是显示，不受配置影响
        TicketSystemMod.LOGGER.error(message, params);
    }

    public static void error(String message, Throwable throwable) {
        // 错误日志总是显示，不受配置影响
        TicketSystemMod.LOGGER.error(message, throwable);
    }
}