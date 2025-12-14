package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.data.StationData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

public class StationManager {
    private static final String FILE_PATH = "mods/" + TicketSystemMod.MODID + "/stations.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, StationData> stations = new HashMap<>();
    
    // 最大距离限制，防止坐标过大导致计算问题
    private static final double MAX_DISTANCE = 1000000; // 1,000,000格

    static {
        try {
            loadStations();
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to initialize StationManager", e);
            stations = new HashMap<>();
        }
    }

    private static void loadStations() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, StationData>>() {}.getType();
                Map<String, StationData> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    stations = loaded;
                } else {
                    stations = new HashMap<>();
                }
            } catch (Exception e) {
                TicketSystemMod.LOGGER.error("Failed to load stations", e);
                stations = new HashMap<>();
            }
        } else {
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            saveStations();
        }
    }

    private static void saveStations() {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            GSON.toJson(stations, writer);
        } catch (IOException e) {
            TicketSystemMod.LOGGER.error("Failed to save stations", e);
        }
    }

    public static void addStation(String station, int x, int y, int z) {
        stations.put(station, new StationData(x, y, z));
        saveStations();
    }

    public static void removeStation(String station) {
        stations.remove(station);
        saveStations();
    }

    public static Set<String> getStations() {
        return stations.keySet();
    }

    public static StationData getStationData(String station) {
        return stations.get(station);
    }

    public static boolean containsStation(String station) {
        return stations.containsKey(station);
    }
    
    public static double calculateDistance(String station1, String station2) {
        if (station1.equals(station2)) return 0;
        
        StationData data1 = getStationData(station1);
        StationData data2 = getStationData(station2);
        if (data1 == null || data2 == null) return 0;
        
        // 使用long类型防止整数溢出
        long dx = (long) data1.getX() - data2.getX();
        long dy = (long) data1.getY() - data2.getY();
        long dz = (long) data1.getZ() - data2.getZ();
        
        // 计算欧几里得距离
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        // 限制最大距离
        return Math.min(distance, MAX_DISTANCE);
    }
    
    // 添加方法检查坐标是否有效
    public static boolean isValidCoordinate(int x, int y, int z) {
        // 检查坐标是否在合理范围内（Minecraft世界坐标范围）
        return x >= -30000000 && x <= 30000000 &&
               y >= -2048 && y <= 2048 &&
               z >= -30000000 && z <= 30000000;
    }

    /**
     * 重新加载车站数据
     */
    public static void reloadStations() {
        loadStations();
    }
}