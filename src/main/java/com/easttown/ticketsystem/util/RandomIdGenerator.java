package com.easttown.ticketsystem.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 随机ID生成器
 * 生成16位小写字母和数字组成的随机ID
 */
public class RandomIdGenerator {
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ID_LENGTH = 16;
    private static final Random RANDOM = new SecureRandom();

    /**
     * 生成一个新的随机ID
     * @return 16位小写字母和数字组成的随机字符串
     */
    public static String generateId() {
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 生成一个新的随机ID，确保在给定集合中唯一
     * @param existingIds 已存在的ID集合
     * @return 唯一的随机ID
     */
    public static String generateUniqueId(java.util.Set<String> existingIds) {
        String id;
        int attempts = 0;
        do {
            id = generateId();
            attempts++;
            // 防止无限循环（理论上概率极低）
            if (attempts > 100) {
                throw new IllegalStateException("无法生成唯一ID，尝试次数过多");
            }
        } while (existingIds.contains(id));
        return id;
    }

    /**
     * 验证ID是否符合格式：16位小写字母和数字
     * @param id 要验证的ID
     * @return 是否符合格式
     */
    public static boolean isValidId(String id) {
        if (id == null || id.length() != ID_LENGTH) {
            return false;
        }
        for (char c : id.toCharArray()) {
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 生成较短的ID（用于显示或临时用途）
     * @param length ID长度
     * @return 指定长度的随机ID
     */
    public static String generateId(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("ID长度必须大于0");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
}