package com.easttown.ticketsystem.manager;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.data.Station;
import com.easttown.ticketsystem.data.Line;
import com.easttown.ticketsystem.data.Fare;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 网络数据管理器 - 替换现有的StationManager
 * 文档要求：修复数据存储位置bug，所有数据操作在服务器端执行
 * 统一管理车站、线路、票价数据
 */
public class NetworkManager {
    private static final String BASE_PATH = "mods/" + TicketSystemMod.MODID + "/";
    private static final String STATIONS_DIR = BASE_PATH + "stations/";
    private static final String LINES_DIR = BASE_PATH + "lines/";
    private static final String FARES_DIR = BASE_PATH + "fares/";

    private static final String STATIONS_FILE = STATIONS_DIR + "stations.json";
    private static final String LINES_FILE = LINES_DIR + "lines.json";
    private static final String FARES_FILE = FARES_DIR + "fares.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 数据缓存
    private static Map<String, Station> stations = new HashMap<>();
    private static Map<String, Line> lines = new HashMap<>();
    private static Map<String, Fare> fares = new HashMap<>(); // key: "from-to"

    // 初始化标志
    private static boolean initialized = false;

    /**
     * 初始化管理器，加载所有数据
     * 应该在服务器启动时调用
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        try {
            // 确保目录存在
            ensureDirectories();

            // 加载数据
            loadStations();
            loadLines();
            loadFares();

            TicketSystemMod.LOGGER.info("NetworkManager initialized: {} stations, {} lines, {} fares loaded",
                    stations.size(), lines.size(), fares.size());

            initialized = true;
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to initialize NetworkManager", e);
            // 使用空数据继续运行
            stations = new HashMap<>();
            lines = new HashMap<>();
            fares = new HashMap<>();
        }
    }

    private static void ensureDirectories() {
        new File(STATIONS_DIR).mkdirs();
        new File(LINES_DIR).mkdirs();
        new File(FARES_DIR).mkdirs();
    }

    // ==================== 车站管理 ====================

    private static void loadStations() {
        File file = new File(STATIONS_FILE);
        if (!file.exists()) {
            saveStations(); // 创建空文件
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Station>>() {}.getType();
            Map<String, Station> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                stations = loaded;
            }
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to load stations", e);
        }
    }

    private static void saveStations() {
        try (FileWriter writer = new FileWriter(STATIONS_FILE)) {
            GSON.toJson(stations, writer);
        } catch (IOException e) {
            TicketSystemMod.LOGGER.error("Failed to save stations", e);
        }
    }

    /**
     * 添加车站
     * @param station 车站对象
     * @return 是否成功添加
     */
    public static boolean addStation(Station station) {
        if (station == null || station.getCode() == null || station.getCode().isEmpty()) {
            return false;
        }

        if (!station.isValidCoordinate()) {
            TicketSystemMod.LOGGER.warn("Invalid coordinate for station: {}", station);
            return false;
        }

        stations.put(station.getCode(), station);
        saveStations();
        return true;
    }

    /**
     * 移除车站
     * @param stationCode 车站编码
     * @return 是否成功移除
     */
    public static boolean removeStation(String stationCode) {
        if (!stations.containsKey(stationCode)) {
            return false;
        }

        // 从所有线路中移除该车站
        for (Line line : lines.values()) {
            line.removeStation(stationCode);
        }
        saveLines();

        // 移除所有涉及该车站的票价
        removeFaresInvolvingStation(stationCode);

        // 移除车站
        stations.remove(stationCode);
        saveStations();

        return true;
    }

    /**
     * 获取车站
     */
    public static Station getStation(String stationCode) {
        return stations.get(stationCode);
    }

    /**
     * 获取所有车站编码
     */
    public static Set<String> getStationCodes() {
        return stations.keySet();
    }

    /**
     * 获取所有车站
     */
    public static Collection<Station> getAllStations() {
        return stations.values();
    }

    /**
     * 检查车站是否存在
     */
    public static boolean hasStation(String stationCode) {
        return stations.containsKey(stationCode);
    }

    /**
     * 更新车站信息
     */
    public static boolean updateStation(Station station) {
        if (!hasStation(station.getCode())) {
            return false;
        }

        stations.put(station.getCode(), station);
        saveStations();
        return true;
    }

    // ==================== 线路管理 ====================

    private static void loadLines() {
        File file = new File(LINES_FILE);
        if (!file.exists()) {
            saveLines(); // 创建空文件
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Line>>() {}.getType();
            Map<String, Line> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                lines = loaded;
            }
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to load lines", e);
        }
    }

    private static void saveLines() {
        try (FileWriter writer = new FileWriter(LINES_FILE)) {
            GSON.toJson(lines, writer);
        } catch (IOException e) {
            TicketSystemMod.LOGGER.error("Failed to save lines", e);
        }
    }

    /**
     * 添加线路
     */
    public static boolean addLine(Line line) {
        if (line == null || line.getId() == null || line.getId().isEmpty()) {
            return false;
        }

        // 验证线路中的车站是否存在
        for (String stationCode : line.getStationCodes()) {
            if (!hasStation(stationCode)) {
                TicketSystemMod.LOGGER.warn("Station {} not found when adding line {}", stationCode, line.getId());
                // 可以继续，线路可以包含尚未添加的车站
            }
        }

        lines.put(line.getId(), line);
        saveLines();
        return true;
    }

    /**
     * 移除线路
     */
    public static boolean removeLine(String lineId) {
        if (!lines.containsKey(lineId)) {
            return false;
        }

        lines.remove(lineId);
        saveLines();
        return true;
    }

    /**
     * 获取线路
     */
    public static Line getLine(String lineId) {
        return lines.get(lineId);
    }

    /**
     * 获取所有线路ID
     */
    public static Set<String> getLineIds() {
        return lines.keySet();
    }

    /**
     * 获取所有线路
     */
    public static Collection<Line> getAllLines() {
        return lines.values();
    }

    /**
     * 检查线路是否存在
     */
    public static boolean hasLine(String lineId) {
        return lines.containsKey(lineId);
    }

    /**
     * 更新线路
     */
    public static boolean updateLine(Line line) {
        if (!hasLine(line.getId())) {
            return false;
        }

        lines.put(line.getId(), line);
        saveLines();
        return true;
    }

    /**
     * 获取包含指定车站的所有线路
     */
    public static List<Line> getLinesContainingStation(String stationCode) {
        List<Line> result = new ArrayList<>();
        for (Line line : lines.values()) {
            if (line.containsStation(stationCode)) {
                result.add(line);
            }
        }
        return result;
    }

    // ==================== 票价管理 ====================

    private static void loadFares() {
        File file = new File(FARES_FILE);
        if (!file.exists()) {
            saveFares(); // 创建空文件
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Fare>>() {}.getType();
            Map<String, Fare> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                // 规范化加载的票价数据
                Map<String, Fare> normalizedFares = new HashMap<>();
                for (Map.Entry<String, Fare> entry : loaded.entrySet()) {
                    Fare fare = entry.getValue();
                    if (fare != null && fare.isValid()) {
                        Fare normalizedFare = normalizeFare(fare);
                        String key = getFareKey(normalizedFare.getFromStation(), normalizedFare.getToStation());

                        // 检查重复键（可能由于旧数据有双向票价）
                        if (normalizedFares.containsKey(key)) {
                            Fare existing = normalizedFares.get(key);
                            if (existing.getPrice() != normalizedFare.getPrice()) {
                                TicketSystemMod.LOGGER.warn("Duplicate fare for segment {}: existing price {}, new price {}",
                                        key, existing.getPrice(), normalizedFare.getPrice());
                            }
                        }
                        normalizedFares.put(key, normalizedFare);
                    } else {
                        TicketSystemMod.LOGGER.warn("Invalid fare skipped during loading: {}", entry.getKey());
                    }
                }
                fares = normalizedFares;
                TicketSystemMod.LOGGER.info("Loaded and normalized {} fares (from {} raw entries)",
                        fares.size(), loaded.size());
            }
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to load fares", e);
        }
    }

    private static void saveFares() {
        try (FileWriter writer = new FileWriter(FARES_FILE)) {
            GSON.toJson(fares, writer);
        } catch (IOException e) {
            TicketSystemMod.LOGGER.error("Failed to save fares", e);
        }
    }

    private static String getFareKey(String from, String to) {
        // 规范化键：按字母顺序排序车站代码，使区间票价无方向性
        if (from == null || to == null) {
            return from + "-" + to; // 保持向后兼容
        }
        if (from.compareTo(to) <= 0) {
            return from + "-" + to;
        } else {
            return to + "-" + from;
        }
    }

    /**
     * 规范化票价对象：确保fromStation是字母顺序较小的车站
     * 这样存储的数据格式统一，便于阅读和调试
     */
    private static Fare normalizeFare(Fare fare) {
        if (fare == null || fare.getFromStation() == null || fare.getToStation() == null) {
            return fare;
        }
        String from = fare.getFromStation();
        String to = fare.getToStation();
        // 如果顺序正确，直接返回原对象
        if (from.compareTo(to) <= 0) {
            return fare;
        }
        // 否则创建新的票价对象，交换起点终点
        return new Fare(to, from, fare.getPrice());
    }

    /**
     * 添加票价
     */
    public static boolean addFare(Fare fare) {
        TicketSystemMod.LOGGER.info("开始添加票价: {}", fare);
        if (fare == null || !fare.isValid()) {
            TicketSystemMod.LOGGER.info("票价无效: {}", fare);
            return false;
        }

        // 验证车站存在
        String from = fare.getFromStation();
        String to = fare.getToStation();
        boolean hasFrom = hasStation(from);
        boolean hasTo = hasStation(to);
        if (!hasFrom || !hasTo) {
            TicketSystemMod.LOGGER.warn("车站不存在: from={} (exists={}), to={} (exists={})", from, hasFrom, to, hasTo);
            return false;
        }

        // 规范化票价对象和键
        Fare normalizedFare = normalizeFare(fare);
        String key = getFareKey(normalizedFare.getFromStation(), normalizedFare.getToStation());
        TicketSystemMod.LOGGER.info("规范化后票价: {}, 存储键: {}", normalizedFare, key);

        // 检查票价是否已存在（区间票价无方向性）
        if (fares.containsKey(key)) {
            Fare existing = fares.get(key);
            TicketSystemMod.LOGGER.warn("票价已存在: 区间 {} 已有票价 {} (尝试添加 {})", key, existing, normalizedFare);
            return false;
        }

        fares.put(key, normalizedFare);
        saveFares();
        TicketSystemMod.LOGGER.info("票价添加成功: {}", normalizedFare);
        return true;
    }

    /**
     * 添加双向票价（现在票价系统无方向性，只需添加一次）
     */
    public static boolean addBidirectionalFare(Fare fare) {
        // 票价现在是无方向性的，只需添加一次
        // 记录日志以便调试
        TicketSystemMod.LOGGER.debug("addBidirectionalFare called with {}, now using single fare system", fare);
        return addFare(fare);
    }

    /**
     * 移除票价
     */
    public static boolean removeFare(String fromStation, String toStation) {
        String key = getFareKey(fromStation, toStation);
        if (!fares.containsKey(key)) {
            return false;
        }

        fares.remove(key);
        saveFares();
        return true;
    }

    /**
     * 移除涉及指定车站的所有票价
     */
    private static void removeFaresInvolvingStation(String stationCode) {
        List<String> toRemove = new ArrayList<>();
        for (String key : fares.keySet()) {
            Fare fare = fares.get(key);
            if (fare.getFromStation().equals(stationCode) || fare.getToStation().equals(stationCode)) {
                toRemove.add(key);
            }
        }

        for (String key : toRemove) {
            fares.remove(key);
        }

        if (!toRemove.isEmpty()) {
            saveFares();
        }
    }

    /**
     * 获取票价
     */
    public static Fare getFare(String fromStation, String toStation) {
        TicketSystemMod.LOGGER.info("查询票价: {} -> {}", fromStation, toStation);

        // 先尝试正向查询
        String key = getFareKey(fromStation, toStation);
        Fare fare = fares.get(key);
        TicketSystemMod.LOGGER.info("第一次查询键: {}, 结果: {}", key, fare);

        if (fare == null) {
            // 尝试反向查询（双向票价）
            String reverseKey = getFareKey(toStation, fromStation);
            if (!reverseKey.equals(key)) {
                TicketSystemMod.LOGGER.info("第一次查询未找到，尝试反向键: {}", reverseKey);
                fare = fares.get(reverseKey);
                TicketSystemMod.LOGGER.info("反向查询结果: {}", fare);
            }
        }

        if (fare != null) {
            TicketSystemMod.LOGGER.info("找到票价: {}", fare);
        } else {
            TicketSystemMod.LOGGER.info("未找到票价: {} -> {}", fromStation, toStation);
        }

        return fare;
    }

    /**
     * 获取所有票价
     */
    public static Collection<Fare> getAllFares() {
        return fares.values();
    }

    /**
     * 检查票价是否存在
     */
    public static boolean hasFare(String fromStation, String toStation) {
        return getFare(fromStation, toStation) != null;
    }

    /**
     * 更新票价
     */
    public static boolean updateFare(Fare fare) {
        if (fare == null || !fare.isValid()) {
            return false;
        }

        // 规范化票价对象和键
        Fare normalizedFare = normalizeFare(fare);
        String key = getFareKey(normalizedFare.getFromStation(), normalizedFare.getToStation());
        if (!fares.containsKey(key)) {
            return false;
        }

        fares.put(key, normalizedFare);
        saveFares();
        return true;
    }

    // ==================== 工具方法 ====================

    /**
     * 重新加载所有数据
     */
    public static void reloadAll() {
        initialized = false;
        initialize();
    }

    /**
     * 获取管理器状态
     */
    public static String getStatus() {
        return String.format("Stations: %d, Lines: %d, Fares: %d",
                stations.size(), lines.size(), fares.size());
    }

    /**
     * 验证数据完整性
     */
    public static List<String> validateData() {
        List<String> issues = new ArrayList<>();

        // 检查线路中的车站是否存在
        for (Line line : lines.values()) {
            for (String stationCode : line.getStationCodes()) {
                if (!hasStation(stationCode)) {
                    issues.add(String.format("线路 %s 包含不存在的车站: %s", line.getId(), stationCode));
                }
            }
        }

        // 检查票价对应的车站是否存在
        for (Fare fare : fares.values()) {
            if (!hasStation(fare.getFromStation())) {
                issues.add(String.format("票价起点车站不存在: %s", fare.getFromStation()));
            }
            if (!hasStation(fare.getToStation())) {
                issues.add(String.format("票价终点车站不存在: %s", fare.getToStation()));
            }
        }

        return issues;
    }

    /**
     * 获取车站的线路归属信息
     */
    public static List<String> getStationLineInfo(String stationCode) {
        List<String> lineInfo = new ArrayList<>();
        for (Line line : getLinesContainingStation(stationCode)) {
            int order = line.getStationOrder(stationCode) + 1; // 1-based
            lineInfo.add(String.format("%s (第%d站)", line.getName(), order));
        }
        return lineInfo;
    }
}