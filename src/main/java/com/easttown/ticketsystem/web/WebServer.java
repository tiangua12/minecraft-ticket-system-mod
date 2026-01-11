package com.easttown.ticketsystem.web;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.config.TicketSystemConfig;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.easttown.ticketsystem.data.Station;
import com.easttown.ticketsystem.data.Line;
import com.easttown.ticketsystem.data.Fare;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Web服务器 - 提供类似web项目的API接口
 * 在配置的端口上运行HTTP服务器，提供车站/线路/票价数据管理
 */
public class WebServer {
    private static HttpServer server;
    private static int port;
    private static boolean running = false;
    private static String gameApiBase = "";

    // Gson实例（线程安全）
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 数据目录（与NetworkManager一致）
    private static final String DATA_BASE_PATH = "mods/" + TicketSystemMod.MODID + "/";

    /**
     * 启动Web服务器
     */
    public static void start() {
        if (running) {
            TicketSystemMod.LOGGER.warn("Web server is already running");
            return;
        }

        port = TicketSystemConfig.getWebServerPort();
        // 从游戏配置读取API基础地址
        gameApiBase = TicketSystemConfig.getWebApiBaseUrl();

        try {
            // 创建服务器，绑定端口，设置并发处理
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.setExecutor(Executors.newCachedThreadPool());

            // 注册API路由
            registerRoutes();

            server.start();
            running = true;

            TicketSystemMod.LOGGER.info("Web server started on port {}", port);
            TicketSystemMod.LOGGER.info("Access URL: http://127.0.0.1:{}/", port);

        } catch (IOException e) {
            TicketSystemMod.LOGGER.error("Failed to start web server on port {}", port, e);
        }
    }

    /**
     * 停止Web服务器
     */
    public static void stop() {
        if (server != null) {
            server.stop(0);
            running = false;
            TicketSystemMod.LOGGER.info("Web server stopped");
        }
    }

    /**
     * 检查服务器是否在运行
     */
    public static boolean isRunning() {
        return running;
    }

    /**
     * 获取服务器端口
     */
    public static int getPort() {
        return port;
    }

    /**
     * 获取游戏内配置的API基础地址
     */
    public static String getGameApiBase() {
        return gameApiBase;
    }

    /**
     * 注册API路由
     */
    private static void registerRoutes() {
        // 静态文件服务（HTML、CSS、JS）
        server.createContext("/", new StaticFileHandler());

        // API根路径
        server.createContext("/api", new ApiHandler());

        // 具体API端点
        server.createContext("/api/stations", new StationsHandler());
        server.createContext("/api/lines", new LinesHandler());
        server.createContext("/api/fares", new FaresHandler());
        server.createContext("/api/config", new ConfigHandler());
        server.createContext("/api/export", new ExportHandler());
        server.createContext("/api/health", new HealthHandler());

        // 更多端点可以按需添加
    }

    // ==================== 请求处理工具方法 ====================

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = GSON.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendTextResponse(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("ok", false);
        error.put("error", message);
        sendJsonResponse(exchange, statusCode, error);
    }

    private static Map<String, Object> parseRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }

            if (body.length() == 0) {
                return new HashMap<>();
            }

            return GSON.fromJson(body.toString(), new TypeToken<Map<String, Object>>() {}.getType());
        }
    }

    // ==================== 静态文件处理器 ====================

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            // 默认返回index.html
            if (path.equals("/") || path.equals("")) {
                path = "/index.html";
            }

            // 从资源文件夹读取文件
            String resourcePath = "assets/ticketsystem/html" + path;
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);

            if (is == null) {
                // 文件未找到
                sendError(exchange, 404, "File not found: " + path);
                return;
            }

            // 根据文件扩展名设置Content-Type
            String contentType = "text/html";
            if (path.endsWith(".css")) {
                contentType = "text/css";
            } else if (path.endsWith(".js")) {
                contentType = "application/javascript";
            } else if (path.endsWith(".png")) {
                contentType = "image/png";
            } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            }

            // 读取文件内容
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] data = baos.toByteArray();

            // 发送响应
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, data.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }

    // ==================== API处理器 ====================

    private static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> info = new HashMap<>();
            info.put("ok", true);
            info.put("service", "FTC Ticketing System API");
            info.put("version", "1.0");
            info.put("endpoints", Arrays.asList(
                "/api/stations",
                "/api/lines",
                "/api/fares",
                "/api/config",
                "/api/export",
                "/api/health"
            ));

            sendJsonResponse(exchange, 200, info);
        }
    }

    private static class StationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                NetworkManager.initialize();

                switch (exchange.getRequestMethod().toUpperCase()) {
                    case "GET":
                        handleGetStations(exchange);
                        break;
                    case "POST":
                        handlePostStation(exchange);
                        break;
                    case "PUT":
                        handlePutStation(exchange);
                        break;
                    case "DELETE":
                        handleDeleteStation(exchange);
                        break;
                    default:
                        sendError(exchange, 405, "Method not allowed");
                }
            } catch (Exception e) {
                TicketSystemMod.LOGGER.error("Error in stations handler", e);
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }

        private void handleGetStations(HttpExchange exchange) throws IOException {
            Collection<Station> stations = NetworkManager.getAllStations();

            // 转换为web格式
            List<Map<String, Object>> stationList = new ArrayList<>();
            for (Station station : stations) {
                Map<String, Object> stationData = new HashMap<>();
                stationData.put("code", station.getCode());
                stationData.put("name", station.getName());
                stationData.put("en_name", station.getEnName());
                // 坐标可选返回
                if (station.getX() != 0 || station.getY() != 0 || station.getZ() != 0) {
                    stationData.put("x", station.getX());
                    stationData.put("y", station.getY());
                    stationData.put("z", station.getZ());
                }
                stationList.add(stationData);
            }

            sendJsonResponse(exchange, 200, stationList);
        }

        private void handlePostStation(HttpExchange exchange) throws IOException {
            Map<String, Object> body = parseRequestBody(exchange);

            String code = (String) body.get("code");
            String name = (String) body.get("name");
            String enName = (String) body.getOrDefault("en_name", "");

            if (code == null || code.isEmpty() || name == null || name.isEmpty()) {
                sendError(exchange, 400, "Missing required fields: code, name");
                return;
            }

            // 创建车站（坐标可选）
            int x = ((Number) body.getOrDefault("x", 0)).intValue();
            int y = ((Number) body.getOrDefault("y", 0)).intValue();
            int z = ((Number) body.getOrDefault("z", 0)).intValue();

            Station station = new Station(code, name, enName, x, y, z);
            boolean success = NetworkManager.addStation(station);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("ok", true);
                response.put("message", "Station added successfully");
                sendJsonResponse(exchange, 201, response);
            } else {
                sendError(exchange, 400, "Failed to add station (may already exist)");
            }
        }

        private void handlePutStation(HttpExchange exchange) throws IOException {
            // 从路径提取车站编码
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 4) {
                sendError(exchange, 400, "Missing station code");
                return;
            }

            String stationCode = parts[3];
            Map<String, Object> body = parseRequestBody(exchange);

            // 获取现有车站
            Station existingStation = NetworkManager.getStation(stationCode);
            if (existingStation == null) {
                sendError(exchange, 404, "Station not found");
                return;
            }

            // 更新字段
            String name = (String) body.get("name");
            String enName = (String) body.get("en_name");
            Integer x = body.containsKey("x") ? ((Number) body.get("x")).intValue() : null;
            Integer y = body.containsKey("y") ? ((Number) body.get("y")).intValue() : null;
            Integer z = body.containsKey("z") ? ((Number) body.get("z")).intValue() : null;

            if (name != null && !name.isEmpty()) {
                existingStation.setName(name);
            }
            if (enName != null) {
                existingStation.setEnName(enName);
            }
            if (x != null) {
                existingStation.setX(x);
            }
            if (y != null) {
                existingStation.setY(y);
            }
            if (z != null) {
                existingStation.setZ(z);
            }

            // 更新车站（先删除再添加，简化实现）
            NetworkManager.removeStation(stationCode);
            boolean success = NetworkManager.addStation(existingStation);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("ok", true);
                response.put("message", "Station updated successfully");
                sendJsonResponse(exchange, 200, response);
            } else {
                sendError(exchange, 500, "Failed to update station");
            }
        }

        private void handleDeleteStation(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length < 4) {
                sendError(exchange, 400, "Missing station code");
                return;
            }

            String stationCode = parts[3];
            boolean success = NetworkManager.removeStation(stationCode);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("ok", true);
                response.put("message", "Station deleted successfully");
                sendJsonResponse(exchange, 200, response);
            } else {
                sendError(exchange, 404, "Station not found");
            }
        }
    }

    private static class LinesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                NetworkManager.initialize();

                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");

                // 判断是操作集合还是单个线路
                // /api/lines 或 /api/lines/{id}
                boolean isSingleLine = parts.length >= 4 && !parts[3].isEmpty();
                String lineId = isSingleLine ? parts[3] : null;

                switch (exchange.getRequestMethod().toUpperCase()) {
                    case "GET":
                        if (isSingleLine) {
                            handleGetLine(exchange, lineId);
                        } else {
                            handleGetLines(exchange);
                        }
                        break;
                    case "POST":
                        if (isSingleLine) {
                            sendError(exchange, 405, "Method not allowed for single line");
                        } else {
                            handlePostLine(exchange);
                        }
                        break;
                    case "PUT":
                        if (isSingleLine) {
                            handlePutLine(exchange, lineId);
                        } else {
                            sendError(exchange, 405, "Method not allowed for line collection");
                        }
                        break;
                    case "DELETE":
                        if (isSingleLine) {
                            handleDeleteLine(exchange, lineId);
                        } else {
                            sendError(exchange, 405, "Method not allowed for line collection");
                        }
                        break;
                    default:
                        sendError(exchange, 405, "Method not allowed");
                }
            } catch (Exception e) {
                TicketSystemMod.LOGGER.error("Error in lines handler", e);
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }

        private void handleGetLines(HttpExchange exchange) throws IOException {
            Collection<Line> lines = NetworkManager.getAllLines();

            List<Map<String, Object>> lineList = new ArrayList<>();
            for (Line line : lines) {
                Map<String, Object> lineData = new HashMap<>();
                lineData.put("id", line.getId());
                lineData.put("name", line.getName());
                lineData.put("en_name", line.getEnName());
                lineData.put("color", line.getColor());
                lineData.put("stations", line.getStationCodes());
                lineList.add(lineData);
            }

            sendJsonResponse(exchange, 200, lineList);
        }

        private void handleGetLine(HttpExchange exchange, String lineId) throws IOException {
            Line line = NetworkManager.getLine(lineId);
            if (line == null) {
                sendError(exchange, 404, "Line not found");
                return;
            }

            Map<String, Object> lineData = new HashMap<>();
            lineData.put("id", line.getId());
            lineData.put("name", line.getName());
            lineData.put("en_name", line.getEnName());
            lineData.put("color", line.getColor());
            lineData.put("stations", line.getStationCodes());

            sendJsonResponse(exchange, 200, lineData);
        }

        private void handlePostLine(HttpExchange exchange) throws IOException {
            Map<String, Object> body = parseRequestBody(exchange);

            String id = (String) body.get("id");
            String name = (String) body.get("name");
            String color = (String) body.getOrDefault("color", "#3366CC");

            // stations字段可能是车站编码列表
            List<String> stationCodes = new ArrayList<>();
            Object stationsObj = body.get("stations");
            if (stationsObj instanceof List) {
                for (Object item : (List<?>) stationsObj) {
                    if (item instanceof String) {
                        stationCodes.add((String) item);
                    }
                }
            }

            if (id == null || id.isEmpty() || name == null || name.isEmpty()) {
                sendError(exchange, 400, "Missing required fields: id, name");
                return;
            }

            Line line = new Line(id, name, color);
            if (!stationCodes.isEmpty()) {
                line.setStationCodes(stationCodes);
            }

            boolean success = NetworkManager.addLine(line);
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("ok", true);
                response.put("message", "Line added successfully");
                sendJsonResponse(exchange, 201, response);
            } else {
                sendError(exchange, 400, "Failed to add line (may already exist)");
            }
        }

        private void handlePutLine(HttpExchange exchange, String lineId) throws IOException {
            Map<String, Object> body = parseRequestBody(exchange);

            Line existingLine = NetworkManager.getLine(lineId);
            if (existingLine == null) {
                sendError(exchange, 404, "Line not found");
                return;
            }

            // 更新字段
            String name = (String) body.get("name");
            String color = (String) body.get("color");

            // stations字段可能是车站编码列表
            List<String> stationCodes = new ArrayList<>();
            Object stationsObj = body.get("stations");
            if (stationsObj instanceof List) {
                for (Object item : (List<?>) stationsObj) {
                    if (item instanceof String) {
                        stationCodes.add((String) item);
                    }
                }
            }

            if (name != null && !name.isEmpty()) {
                existingLine.setName(name);
            }
            if (color != null && !color.isEmpty()) {
                existingLine.setColor(color);
            }
            if (!stationCodes.isEmpty()) {
                existingLine.setStationCodes(stationCodes);
            }

            // 更新线路（先删除再添加，简化实现）
            NetworkManager.removeLine(lineId);
            boolean success = NetworkManager.addLine(existingLine);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("ok", true);
                response.put("message", "Line updated successfully");
                sendJsonResponse(exchange, 200, response);
            } else {
                sendError(exchange, 500, "Failed to update line");
            }
        }

        private void handleDeleteLine(HttpExchange exchange, String lineId) throws IOException {
            boolean success = NetworkManager.removeLine(lineId);
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("ok", true);
                response.put("message", "Line deleted successfully");
                sendJsonResponse(exchange, 200, response);
            } else {
                sendError(exchange, 404, "Line not found");
            }
        }
    }

    private static class FaresHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                NetworkManager.initialize();

                // 检查是否为票价计算请求
                String path = exchange.getRequestURI().getPath();
                if (path.endsWith("/calculate") || path.contains("/calculate/")) {
                    handleCalculateFare(exchange);
                    return;
                }

                switch (exchange.getRequestMethod().toUpperCase()) {
                    case "GET":
                        handleGetFares(exchange);
                        break;
                    case "POST":
                        handlePostFare(exchange);
                        break;
                    case "DELETE":
                        handleDeleteFare(exchange);
                        break;
                    case "PUT":
                        handlePutFare(exchange);
                        break;
                    default:
                        sendError(exchange, 405, "Method not allowed");
                }
            } catch (Exception e) {
                TicketSystemMod.LOGGER.error("Error in fares handler", e);
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }

        private void handleGetFares(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            // 检查是否为获取特定票价: /api/fares/{from}/{to}
            if (parts.length >= 5) {
                String from = parts[3];
                String to = parts[4];

                // 检查票价是否存在
                boolean hasFare = NetworkManager.hasFare(from, to);
                if (!hasFare) {
                    // 也检查反向
                    hasFare = NetworkManager.hasFare(to, from);
                    if (!hasFare) {
                        sendError(exchange, 404, "Fare not found");
                        return;
                    }
                    // 如果反向存在，交换from和to以获取正确方向
                    String temp = from;
                    from = to;
                    to = temp;
                }

                // 获取票价
                Fare fare = NetworkManager.getFare(from, to);
                if (fare == null) {
                    sendError(exchange, 404, "Fare not found");
                    return;
                }

                Map<String, Object> fareData = new HashMap<>();
                fareData.put("from", fare.getFromStation());
                fareData.put("to", fare.getToStation());
                fareData.put("cost", fare.getPrice());
                fareData.put("cost_regular", fare.getPrice());
                fareData.put("cost_express", fare.getPrice());
                fareData.put("price", fare.getPrice()); // 添加price字段

                sendJsonResponse(exchange, 200, fareData);
                return;
            }

            // 否则返回所有票价列表
            Collection<Fare> fares = NetworkManager.getAllFares();

            List<Map<String, Object>> fareList = new ArrayList<>();
            for (Fare fare : fares) {
                Map<String, Object> fareData = new HashMap<>();
                fareData.put("from", fare.getFromStation());
                fareData.put("to", fare.getToStation());
                fareData.put("cost", fare.getPrice());
                // 注意：web版本有cost_regular和cost_express，这里简化
                fareData.put("cost_regular", fare.getPrice());
                fareData.put("cost_express", fare.getPrice());
                fareData.put("price", fare.getPrice()); // 添加price字段
                fareList.add(fareData);
            }

            sendJsonResponse(exchange, 200, fareList);
        }

        private void handlePostFare(HttpExchange exchange) throws IOException {
            Map<String, Object> body = parseRequestBody(exchange);

            // 检查是否为批量添加
            if (body.containsKey("segments") || body.containsKey("cost_regular") || body.containsKey("cost_express")) {
                handleBulkFares(exchange, body);
                return;
            }

            // 单个票价添加
            // 支持多种字段名: from/fromStation, to/toStation
            String from = (String) body.get("from");
            if (from == null || from.isEmpty()) {
                from = (String) body.get("fromStation");
            }
            String to = (String) body.get("to");
            if (to == null || to.isEmpty()) {
                to = (String) body.get("toStation");
            }

            // 优先使用cost_regular，其次使用cost，最后使用price
            Object costRegularObj = body.get("cost_regular");
            Object costObj = body.get("cost");
            Object priceObj = body.get("price");

            int price = 0;
            if (costRegularObj instanceof Number) {
                price = ((Number) costRegularObj).intValue();
            } else if (costObj instanceof Number) {
                price = ((Number) costObj).intValue();
            } else if (priceObj instanceof Number) {
                price = ((Number) priceObj).intValue();
            } else {
                sendError(exchange, 400, "Missing price field: cost_regular, cost, or price required");
                return;
            }

            if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
                sendError(exchange, 400, "Missing required fields: from, to");
                return;
            }

            if (price <= 0) {
                sendError(exchange, 400, "Price must be positive");
                return;
            }

            Fare fare = new Fare(from, to, price);
            boolean success = NetworkManager.addFare(fare);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("ok", true);
                response.put("message", "Fare added successfully");
                sendJsonResponse(exchange, 201, response);
            } else {
                sendError(exchange, 400, "Failed to add fare (stations may not exist or fare already exists)");
            }
        }

        private void handleBulkFares(HttpExchange exchange, Map<String, Object> body) throws IOException {
            // 批量添加票价（支持分段批量添加）
            // 格式1: {segments: [{from, to}], cost_regular: X, cost_express: Y}
            // 格式2: {fares: [{from, to, cost_regular, cost_express}]}

            List<Map<String, Object>> faresToAdd = new ArrayList<>();

            // 检查格式1
            Object segmentsObj = body.get("segments");
            if (segmentsObj instanceof List) {
                Object costRegularObj = body.get("cost_regular");
                Object costExpressObj = body.get("cost_express");

                int costRegular = costRegularObj instanceof Number ? ((Number) costRegularObj).intValue() : 0;
                int costExpress = costExpressObj instanceof Number ? ((Number) costExpressObj).intValue() : costRegular;

                for (Object segmentObj : (List<?>) segmentsObj) {
                    if (segmentObj instanceof Map) {
                        Map<?, ?> segment = (Map<?, ?>) segmentObj;
                        Object fromObj = segment.get("from");
                        Object toObj = segment.get("to");

                        if (fromObj instanceof String && toObj instanceof String) {
                            Map<String, Object> fare = new HashMap<>();
                            fare.put("from", fromObj);
                            fare.put("to", toObj);
                            fare.put("cost_regular", costRegular);
                            fare.put("cost_express", costExpress);
                            faresToAdd.add(fare);
                        }
                    }
                }
            }

            // 检查格式2
            Object faresObj = body.get("fares");
            if (faresObj instanceof List && faresToAdd.isEmpty()) {
                for (Object fareObj : (List<?>) faresObj) {
                    if (fareObj instanceof Map) {
                        Map<?, ?> fareMap = (Map<?, ?>) fareObj;
                        Map<String, Object> newFareMap = new HashMap<>();
                        for (Map.Entry<?, ?> entry : fareMap.entrySet()) {
                            newFareMap.put(entry.getKey().toString(), entry.getValue());
                        }
                        faresToAdd.add(newFareMap);
                    }
                }
            }

            if (faresToAdd.isEmpty()) {
                sendError(exchange, 400, "Invalid bulk fare format. Use {segments: [...], cost_regular: X} or {fares: [...]}");
                return;
            }

            int successCount = 0;
            int failCount = 0;
            List<String> errors = new ArrayList<>();

            for (Map<String, Object> fareData : faresToAdd) {
                String from = (String) fareData.get("from");
                String to = (String) fareData.get("to");
                Object costRegularObj = fareData.get("cost_regular");
                Object costObj = fareData.get("cost");
                Object priceObj = fareData.get("price");

                int price = 0;
                if (costRegularObj instanceof Number) {
                    price = ((Number) costRegularObj).intValue();
                } else if (costObj instanceof Number) {
                    price = ((Number) costObj).intValue();
                } else if (priceObj instanceof Number) {
                    price = ((Number) priceObj).intValue();
                }

                if (from == null || to == null || price <= 0) {
                    failCount++;
                    errors.add("Invalid fare data: " + fareData);
                    continue;
                }

                Fare fare = new Fare(from, to, price);
                if (NetworkManager.addFare(fare)) {
                    successCount++;
                } else {
                    failCount++;
                    errors.add("Failed to add fare: " + from + "-" + to);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("message", String.format("Bulk fares added: %d success, %d failed", successCount, failCount));
            response.put("success", successCount);
            response.put("failed", failCount);
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }

            sendJsonResponse(exchange, 201, response);
        }

        private void handlePutFare(HttpExchange exchange) throws IOException {
            // PUT用于更新票价
            Map<String, Object> body = parseRequestBody(exchange);

            // 支持多种字段名: from/fromStation, to/toStation
            String from = (String) body.get("from");
            if (from == null || from.isEmpty()) {
                from = (String) body.get("fromStation");
            }
            String to = (String) body.get("to");
            if (to == null || to.isEmpty()) {
                to = (String) body.get("toStation");
            }

            // 如果请求体中没有，尝试从路径参数获取
            if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");
                if (parts.length >= 5) {
                    from = parts[3];
                    to = parts[4];
                }
            }

            // 优先使用cost_regular，其次使用cost，最后使用price
            Object costRegularObj = body.get("cost_regular");
            Object costObj = body.get("cost");
            Object priceObj = body.get("price");

            int price = 0;
            if (costRegularObj instanceof Number) {
                price = ((Number) costRegularObj).intValue();
            } else if (costObj instanceof Number) {
                price = ((Number) costObj).intValue();
            } else if (priceObj instanceof Number) {
                price = ((Number) priceObj).intValue();
            } else {
                sendError(exchange, 400, "Missing price field: cost_regular, cost, or price required");
                return;
            }

            if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
                sendError(exchange, 400, "Missing required fields: from, to");
                return;
            }

            if (price <= 0) {
                sendError(exchange, 400, "Price must be positive");
                return;
            }

            // 检查票价是否存在（支持双向）
            boolean hasFare = NetworkManager.hasFare(from, to);
            if (!hasFare) {
                // 检查反向
                hasFare = NetworkManager.hasFare(to, from);
                if (!hasFare) {
                    sendError(exchange, 404, "Fare not found");
                    return;
                }
                // 如果反向存在，交换from和to以更新正确方向
                String temp = from;
                from = to;
                to = temp;
            }

            // 移除旧票价，添加新票价
            NetworkManager.removeFare(from, to);
            Fare fare = new Fare(from, to, price);
            boolean success = NetworkManager.addFare(fare);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("ok", true);
                response.put("message", "Fare updated successfully");
                sendJsonResponse(exchange, 200, response);
            } else {
                sendError(exchange, 500, "Failed to update fare");
            }
        }

        private void handleDeleteFare(HttpExchange exchange) throws IOException {
            // DELETE方法：从请求体中获取from和to
            Map<String, Object> body = parseRequestBody(exchange);

            // 支持多种字段名: from/fromStation, to/toStation
            String from = (String) body.get("from");
            if (from == null || from.isEmpty()) {
                from = (String) body.get("fromStation");
            }
            String to = (String) body.get("to");
            if (to == null || to.isEmpty()) {
                to = (String) body.get("toStation");
            }

            if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
                // 尝试从路径参数获取
                String path = exchange.getRequestURI().getPath();
                String[] parts = path.split("/");
                if (parts.length >= 5) {
                    from = parts[3];
                    to = parts[4];
                } else {
                    sendError(exchange, 400, "Missing required fields: from, to");
                    return;
                }
            }

            boolean success = NetworkManager.removeFare(from, to);

            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("ok", true);
                response.put("message", "Fare deleted successfully");
                sendJsonResponse(exchange, 200, response);
            } else {
                sendError(exchange, 404, "Fare not found");
            }
        }

        private void handleCalculateFare(HttpExchange exchange) throws IOException {
            // 只支持POST方法
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed. Use POST.");
                return;
            }

            Map<String, Object> body = parseRequestBody(exchange);
            String startCode = (String) body.get("start");
            String endCode = (String) body.get("end");

            if (startCode == null || startCode.isEmpty() || endCode == null || endCode.isEmpty()) {
                sendError(exchange, 400, "Missing required fields: start, end");
                return;
            }

            if (startCode.equals(endCode)) {
                sendError(exchange, 400, "Start and end stations cannot be the same");
                return;
            }

            try {
                // 计算票价
                int price = com.easttown.ticketsystem.manager.PriceCalculator.calculatePrice(startCode, endCode);

                if (price <= 0) {
                    sendError(exchange, 404, "Unable to calculate fare between the specified stations");
                    return;
                }

                // 获取车站信息
                com.easttown.ticketsystem.manager.NetworkManager.initialize();
                com.easttown.ticketsystem.data.Station startStation =
                    com.easttown.ticketsystem.manager.NetworkManager.getStation(startCode);
                com.easttown.ticketsystem.data.Station endStation =
                    com.easttown.ticketsystem.manager.NetworkManager.getStation(endCode);

                String startName = startStation != null ? startStation.getName() : startCode;
                String endName = endStation != null ? endStation.getName() : endCode;

                // 计算距离（简化实现，使用车站数作为距离估算）
                int distance = 0;
                try {
                    com.easttown.ticketsystem.data.Route route =
                        com.easttown.ticketsystem.manager.RouteCalculator.findCheapestRoute(startCode, endCode);
                    if (route != null) {
                        // Route类没有getTotalDistance()方法，使用车站数作为距离估算
                        // 假设每个车站间平均距离为1公里
                        distance = route.getStationCount();
                    }
                } catch (Exception e) {
                    // 距离计算失败，使用0
                }

                // 生成票号
                String ticketNumber = "TICKET-" + System.currentTimeMillis() % 1000000;

                // 构建响应
                Map<String, Object> response = new HashMap<>();
                response.put("ok", true);
                response.put("start_code", startCode);
                response.put("end_code", endCode);
                response.put("start_name", startName);
                response.put("end_name", endName);
                response.put("price", price);
                response.put("distance", distance);
                response.put("fare_type", "普通票"); // 当前系统只有普通票
                response.put("ticket_number", ticketNumber);

                sendJsonResponse(exchange, 200, response);

            } catch (Exception e) {
                TicketSystemMod.LOGGER.error("Error calculating fare", e);
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }
    }

    private static class ConfigHandler implements HttpHandler {
        // 内存中的配置存储
        private static final Map<String, Object> configStore = new HashMap<>();
        static {
            // 初始化默认配置（api_base会在GET时动态设置）
            configStore.put("api_base", ""); // 占位符
            configStore.put("current_station", Map.of("name", "Station1", "code", "01-01"));
            configStore.put("transfers", new ArrayList<>());
            configStore.put("promotion", Map.of("name", "", "discount", 1.0));
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                switch (exchange.getRequestMethod().toUpperCase()) {
                    case "GET":
                        handleGetConfig(exchange);
                        break;
                    case "PUT":
                        handlePutConfig(exchange);
                        break;
                    case "POST":
                        handlePutConfig(exchange); // POST也当作PUT处理
                        break;
                    default:
                        sendError(exchange, 405, "Method not allowed");
                }
            } catch (Exception e) {
                TicketSystemMod.LOGGER.error("Error in config handler", e);
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            }
        }

        private void handleGetConfig(HttpExchange exchange) throws IOException {
            // 更新api_base中的端口（确保端口是最新的）
            configStore.put("api_base", "http://127.0.0.1:" + WebServer.getPort() + "/api");

            Map<String, Object> config = new HashMap<>(configStore);
            // 添加游戏内配置的API基础地址
            config.put("game_api_base", WebServer.getGameApiBase());
            sendJsonResponse(exchange, 200, config);
        }

        private void handlePutConfig(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");

            // 支持 /api/config 和 /api/config/{key} 两种格式
            if (parts.length >= 4 && !parts[3].isEmpty()) {
                // 子路径格式：/api/config/api_base, /api/config/transfers, /api/config/promotion
                handleSubConfig(exchange, parts[3]);
                return;
            }

            // 整体更新配置
            Map<String, Object> body = parseRequestBody(exchange);
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                configStore.put(entry.getKey(), entry.getValue());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("message", "Configuration updated successfully");
            sendJsonResponse(exchange, 200, response);
        }

        private void handleSubConfig(HttpExchange exchange, String configKey) throws IOException {
            Map<String, Object> body = parseRequestBody(exchange);

            switch (configKey) {
                case "api_base":
                    Object apiBase = body.get("api_base");
                    if (apiBase instanceof String) {
                        configStore.put("api_base", apiBase);
                    } else {
                        sendError(exchange, 400, "Invalid api_base value");
                        return;
                    }
                    break;

                case "transfers":
                    Object transfers = body.get("transfers");
                    if (transfers instanceof List) {
                        configStore.put("transfers", transfers);
                    } else {
                        sendError(exchange, 400, "Invalid transfers value, must be array");
                        return;
                    }
                    break;

                case "promotion":
                    Object nameObj = body.get("name");
                    Object discountObj = body.get("discount");

                    String name = nameObj instanceof String ? (String) nameObj : "";
                    double discount = 1.0;
                    if (discountObj instanceof Number) {
                        discount = ((Number) discountObj).doubleValue();
                    }

                    configStore.put("promotion", Map.of("name", name, "discount", discount));
                    break;

                default:
                    sendError(exchange, 404, "Config key not found: " + configKey);
                    return;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("message", "Configuration updated successfully");
            sendJsonResponse(exchange, 200, response);
        }
    }

    private static class ExportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                NetworkManager.initialize();

                Map<String, Object> export = new HashMap<>();

                // 车站数据
                List<Map<String, Object>> stations = new ArrayList<>();
                for (Station station : NetworkManager.getAllStations()) {
                    stations.add(Map.of(
                        "code", station.getCode(),
                        "name", station.getName(),
                        "en_name", station.getEnName(),
                        "x", station.getX(),
                        "y", station.getY(),
                        "z", station.getZ(),
                        "station_number", station.getStationNumber(),
                        "uuid", station.getUuid(),
                        "custom_id", station.getCustomId()
                    ));
                }
                export.put("stations", stations);

                // 线路数据
                List<Map<String, Object>> lines = new ArrayList<>();
                for (Line line : NetworkManager.getAllLines()) {
                    lines.add(Map.of(
                        "id", line.getId(),
                        "name", line.getName(),
                        "en_name", line.getEnName(),
                        "color", line.getColor(),
                        "stations", line.getStationCodes(),
                        "uuid", line.getUuid(),
                        "custom_id", line.getCustomId()
                    ));
                }
                export.put("lines", lines);

                // 票价数据
                List<Map<String, Object>> fares = new ArrayList<>();
                for (Fare fare : NetworkManager.getAllFares()) {
                    fares.add(Map.of(
                        "from", fare.getFromStation(),
                        "to", fare.getToStation(),
                        "fromStation", fare.getFromStation(),  // 兼容前端字段名
                        "toStation", fare.getToStation(),      // 兼容前端字段名
                        "cost", fare.getPrice(),
                        "cost_regular", fare.getPrice(),
                        "cost_express", fare.getPrice(),
                        "price", fare.getPrice()               // 前端使用的字段名
                    ));
                }
                export.put("fares", fares);

                // 配置
                export.put("config", Map.of(
                    "api_base", "http://127.0.0.1:" + port + "/api",
                    "promotion", Map.of("name", "", "discount", 1.0)
                ));

                sendJsonResponse(exchange, 200, export);

            } catch (Exception e) {
                TicketSystemMod.LOGGER.error("Error in export handler", e);
                sendError(exchange, 500, "Internal server error");
            }
        }
    }

    private static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> health = new HashMap<>();
            health.put("ok", true);
            health.put("service", "ticketsystem");
            health.put("timestamp", System.currentTimeMillis());
            health.put("port", port);
            health.put("running", running);

            sendJsonResponse(exchange, 200, health);
        }
    }
}