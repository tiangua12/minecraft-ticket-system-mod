package com.easttown.ticketsystem.util;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

/**
 * ID生成器 - 生成由26个小写字母和数字组合的唯一值
 * 用于生成线路ID、车站编码等唯一标识符
 */
public class IdGenerator {
    // 字符集：26个小写字母 + 10个数字 = 36个字符
    private static final char[] CHARSET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final int CHARSET_SIZE = CHARSET.length;

    // 随机数生成器
    private static final SecureRandom random = new SecureRandom();

    // 已生成的ID缓存（避免冲突）
    private static final Set<String> generatedIds = new HashSet<>();

    /**
     * 生成指定长度的随机ID
     * @param length ID长度
     * @return 唯一的随机ID
     */
    public static String generateRandomId(int length) {
        if (length <= 0) {
            length = 8; // 默认长度
        }

        String id;
        int attempts = 0;
        final int MAX_ATTEMPTS = 100;

        do {
            // 生成随机ID
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                int index = random.nextInt(CHARSET_SIZE);
                sb.append(CHARSET[index]);
            }
            id = sb.toString();

            attempts++;
            if (attempts >= MAX_ATTEMPTS) {
                // 增加时间戳以避免无限循环
                id = id + System.currentTimeMillis() % 10000;
                break;
            }

        } while (generatedIds.contains(id));

        generatedIds.add(id);
        return id;
    }

    /**
     * 生成线路ID（默认长度8）
     */
    public static String generateLineId() {
        return "l_" + generateRandomId(6); // 如 "l_a1b2c3"
    }

    /**
     * 生成车站编码（默认长度8）
     */
    public static String generateStationCode() {
        return "s_" + generateRandomId(6); // 如 "s_x9y8z7"
    }

    /**
     * 生成短ID（用于显示）
     */
    public static String generateShortId() {
        return generateRandomId(4); // 如 "a1b2"
    }

    /**
     * 生成带有前缀的ID
     */
    public static String generatePrefixedId(String prefix, int randomLength) {
        return prefix + "_" + generateRandomId(randomLength);
    }

    /**
     * 基于时间戳生成ID（确保唯一性）
     */
    public static String generateTimestampId() {
        long timestamp = System.currentTimeMillis();
        // 转换为36进制（使用小写字母和数字）
        String base36 = Long.toString(timestamp, 36);

        // 确保只包含小写字母和数字
        base36 = base36.toLowerCase();

        // 添加随机后缀避免冲突
        String randomSuffix = generateRandomId(2);
        return base36 + randomSuffix;
    }

    /**
     * 验证ID是否符合格式（只包含小写字母和数字）
     */
    public static boolean isValidId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }

        for (char c : id.toCharArray()) {
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) {
                return false;
            }
        }

        return true;
    }

    /**
     * 规范化ID（转换为小写，移除非法字符）
     */
    public static String normalizeId(String id) {
        if (id == null) {
            return generateRandomId(8);
        }

        // 转换为小写
        id = id.toLowerCase();

        // 移除非法字符，用下划线替换
        StringBuilder sb = new StringBuilder();
        for (char c : id.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }

        String normalized = sb.toString();

        // 如果结果为空，生成随机ID
        if (normalized.isEmpty() || normalized.equals("_")) {
            return generateRandomId(8);
        }

        return normalized;
    }

    /**
     * 生成人类可读的ID（字母开头，避免纯数字）
     */
    public static String generateHumanReadableId() {
        // 确保以字母开头
        char firstChar = CHARSET[random.nextInt(26)]; // 0-25是小写字母
        String rest = generateRandomId(5); // 剩余部分

        return firstChar + rest;
    }

    /**
     * 清除已生成的ID缓存
     */
    public static void clearCache() {
        generatedIds.clear();
    }

    /**
     * 注册已存在的ID（避免生成重复ID）
     */
    public static void registerExistingId(String id) {
        if (id != null && !id.isEmpty()) {
            generatedIds.add(id);
        }
    }

    /**
     * 批量注册已存在的ID
     */
    public static void registerExistingIds(Iterable<String> ids) {
        for (String id : ids) {
            registerExistingId(id);
        }
    }

    /**
     * 检查ID是否已存在
     */
    public static boolean idExists(String id) {
        return generatedIds.contains(id);
    }

    /**
     * 获取ID生成统计
     */
    public static String getStats() {
        return String.format("已生成ID数量: %d, 字符集大小: %d",
                generatedIds.size(), CHARSET_SIZE);
    }
}