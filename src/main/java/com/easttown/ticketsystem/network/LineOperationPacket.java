package com.easttown.ticketsystem.network;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.data.Line;
import com.easttown.ticketsystem.manager.LineManager;
import com.easttown.ticketsystem.util.LanguageHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 线路操作网络包
 * 客户端发送线路创建/更新/删除请求到服务器
 */
public class LineOperationPacket {
    // 操作类型
    public enum Operation {
        CREATE,     // 创建线路
        UPDATE,     // 更新线路
        DELETE      // 删除线路
    }

    private final Operation operation;
    private final String lineId;
    private final String lineDataJson; // 序列化的线路数据（CREATE/UPDATE时使用）

    // Gson实例（线程安全）
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    /**
     * 创建线路操作包
     */
    public LineOperationPacket(Operation operation, String lineId, Line line) {
        this.operation = operation;
        this.lineId = lineId;
        this.lineDataJson = line != null ? serializeLine(line) : "";
    }

    /**
     * 删除线路操作包
     */
    public LineOperationPacket(Operation operation, String lineId) {
        this.operation = operation;
        this.lineId = lineId;
        this.lineDataJson = "";
    }

    /**
     * 序列化线路
     */
    private String serializeLine(Line line) {
        try {
            // 创建简化数据传输对象
            SimpleLineData data = new SimpleLineData();
            data.id = line.getId();
            data.name = line.getName();
            data.enName = line.getEnName();
            data.color = line.getColor();
            data.stationCodes = new ArrayList<>(line.getStationCodes());
            return GSON.toJson(data);
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to serialize line: {}", lineId, e);
            return "{}";
        }
    }

    /**
     * 反序列化线路
     */
    private Line deserializeLine(String json) {
        try {
            SimpleLineData data = GSON.fromJson(json, SimpleLineData.class);
            if (data == null) {
                return null;
            }

            Line line = new Line(data.id, data.name, data.color);
            line.setEnName(data.enName);
            if (data.stationCodes != null) {
                line.setStationCodes(new ArrayList<>(data.stationCodes));
            }
            return line;
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to deserialize line data: {}", json, e);
            return null;
        }
    }

    // 编码
    public static void encode(LineOperationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.operation);
        buffer.writeUtf(packet.lineId);
        buffer.writeUtf(packet.lineDataJson);
    }

    // 解码
    public static LineOperationPacket decode(FriendlyByteBuf buffer) {
        Operation operation = buffer.readEnum(Operation.class);
        String lineId = buffer.readUtf();
        String lineDataJson = buffer.readUtf();

        if (operation == Operation.DELETE || lineDataJson.isEmpty()) {
            return new LineOperationPacket(operation, lineId);
        } else {
            // 反序列化线路数据
            Line line = null;
            try {
                SimpleLineData data = GSON.fromJson(lineDataJson, SimpleLineData.class);
                if (data != null) {
                    line = new Line(data.id, data.name, data.color);
                    line.setEnName(data.enName);
                    if (data.stationCodes != null) {
                        line.setStationCodes(new ArrayList<>(data.stationCodes));
                    }
                }
            } catch (Exception e) {
                TicketSystemMod.LOGGER.error("Failed to decode line data", e);
            }
            return new LineOperationPacket(operation, lineId, line);
        }
    }

    // 处理
    public static void handle(LineOperationPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return; // 必须在服务器端处理
            }

            boolean success = false;
            String messageKey = "";
            Object[] messageArgs = new Object[0];

            try {
                switch (packet.operation) {
                    case CREATE:
                        success = handleCreate(packet, player);
                        messageKey = success ? "gui.line_created" : "gui.line_create_failed";
                        messageArgs = new Object[]{packet.lineId};
                        break;

                    case UPDATE:
                        success = handleUpdate(packet, player);
                        messageKey = success ? "gui.line_updated" : "gui.line_update_failed";
                        messageArgs = new Object[]{packet.lineId};
                        break;

                    case DELETE:
                        success = handleDelete(packet, player);
                        messageKey = success ? "gui.line_deleted" : "gui.line_delete_failed";
                        messageArgs = new Object[]{packet.lineId};
                        break;
                }
            } catch (Exception e) {
                TicketSystemMod.LOGGER.error("Error handling line operation: {}", packet.operation, e);
                messageKey = "gui.line_operation_error";
                messageArgs = new Object[]{packet.operation.name()};
            }

            // 发送反馈消息给玩家
            if (player != null) {
                Component message = LanguageHelper.translate(messageKey, messageArgs);
                player.displayClientMessage(message, true);
            }
        });
        context.get().setPacketHandled(true);
    }

    /**
     * 处理创建线路
     */
    private static boolean handleCreate(LineOperationPacket packet, ServerPlayer player) {
        Line line = packet.deserializeLine(packet.lineDataJson);
        if (line == null) {
            TicketSystemMod.LOGGER.error("Failed to deserialize line for creation: {}", packet.lineId);
            return false;
        }

        // 验证线路ID
        if (!line.getId().equals(packet.lineId)) {
            TicketSystemMod.LOGGER.error("Line ID mismatch: {} != {}", line.getId(), packet.lineId);
            return false;
        }

        // 验证线路数据
        if (line.getStationCodes().size() < 2) {
            TicketSystemMod.LOGGER.warn("Line {} needs at least 2 stations, got {}",
                    line.getId(), line.getStationCodes().size());
            return false;
        }

        // 执行创建
        return LineManager.addLine(line);
    }

    /**
     * 处理更新线路
     */
    private static boolean handleUpdate(LineOperationPacket packet, ServerPlayer player) {
        Line line = packet.deserializeLine(packet.lineDataJson);
        if (line == null) {
            TicketSystemMod.LOGGER.error("Failed to deserialize line for update: {}", packet.lineId);
            return false;
        }

        // 验证线路ID
        if (!line.getId().equals(packet.lineId)) {
            TicketSystemMod.LOGGER.error("Line ID mismatch in update: {} != {}", line.getId(), packet.lineId);
            return false;
        }

        // 验证线路存在
        Line existingLine = LineManager.getLine(packet.lineId);
        if (existingLine == null) {
            TicketSystemMod.LOGGER.error("Line not found for update: {}", packet.lineId);
            return false;
        }

        // 验证线路数据
        if (line.getStationCodes().size() < 2) {
            TicketSystemMod.LOGGER.warn("Line {} needs at least 2 stations, got {}",
                    line.getId(), line.getStationCodes().size());
            return false;
        }

        // 执行更新
        return LineManager.updateLine(line);
    }

    /**
     * 处理删除线路
     */
    private static boolean handleDelete(LineOperationPacket packet, ServerPlayer player) {
        // 验证线路存在
        Line existingLine = LineManager.getLine(packet.lineId);
        if (existingLine == null) {
            TicketSystemMod.LOGGER.error("Line not found for deletion: {}", packet.lineId);
            return false;
        }

        // 执行删除
        return LineManager.removeLine(packet.lineId);
    }

    // ==================== 数据传输对象 ====================

    /**
     * 简化的线路数据传输对象
     * 避免Gson序列化复杂对象带来的问题
     */
    private static class SimpleLineData {
        public String id;
        public String name;
        public String enName;
        public String color;
        public List<String> stationCodes;
    }
}