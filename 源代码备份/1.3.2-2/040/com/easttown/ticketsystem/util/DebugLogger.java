package com.easttown.ticketsystem.util;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.config.DebugConfig;

public class DebugLogger {

    public static void info(String message, Object... params) {
        if (DebugConfig.SHOW_DEBUG_LOGS.get()) {
            TicketSystemMod.LOGGER.info(message, params);
        }
    }

    public static void debug(String message, Object... params) {
        if (DebugConfig.SHOW_DEBUG_LOGS.get()) {
            TicketSystemMod.LOGGER.debug(message, params);
        }
    }

    public static void warn(String message, Object... params) {
        if (DebugConfig.SHOW_DEBUG_LOGS.get()) {
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