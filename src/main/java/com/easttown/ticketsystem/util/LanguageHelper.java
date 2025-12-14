package com.easttown.ticketsystem.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class LanguageHelper {
    public static MutableComponent translate(String key) {
        return Component.translatable("ticketsystem." + key);
    }
    
    public static MutableComponent translate(String key, Object... args) {
        return Component.translatable("ticketsystem." + key, args);
    }
}
