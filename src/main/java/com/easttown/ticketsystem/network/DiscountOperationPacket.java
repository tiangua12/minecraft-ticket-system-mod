package com.easttown.ticketsystem.network;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.manager.DiscountManager;
import com.easttown.ticketsystem.util.LanguageHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 折扣操作网络包
 * 客户端发送折扣设置/清除请求到服务器
 */
public class DiscountOperationPacket {
    // 操作类型
    public enum Operation {
        SET,        // 设置折扣
        CLEAR,      // 清除折扣
        ENABLE,     // 启用折扣
        DISABLE     // 禁用折扣
    }

    private final Operation operation;
    private final String discountName;
    private final double discountValue;

    // Gson实例（线程安全）
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * 设置折扣操作包
     */
    public DiscountOperationPacket(Operation operation, String discountName, double discountValue) {
        this.operation = operation;
        this.discountName = discountName;
        this.discountValue = discountValue;
    }

    /**
     * 清除/启用/禁用折扣操作包
     */
    public DiscountOperationPacket(Operation operation) {
        this.operation = operation;
        this.discountName = "";
        this.discountValue = 1.0;
    }

    // 编码
    public static void encode(DiscountOperationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.operation);
        buffer.writeUtf(packet.discountName);
        buffer.writeDouble(packet.discountValue);
    }

    // 解码
    public static DiscountOperationPacket decode(FriendlyByteBuf buffer) {
        Operation operation = buffer.readEnum(Operation.class);
        String discountName = buffer.readUtf();
        double discountValue = buffer.readDouble();

        if (operation == Operation.CLEAR || operation == Operation.ENABLE || operation == Operation.DISABLE) {
            return new DiscountOperationPacket(operation);
        } else {
            return new DiscountOperationPacket(operation, discountName, discountValue);
        }
    }

    // 处理
    public static void handle(DiscountOperationPacket packet, Supplier<NetworkEvent.Context> context) {
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
                    case SET:
                        success = handleSet(packet, player);
                        messageKey = success ? "gui.discount_set" : "gui.discount_set_failed";
                        messageArgs = new Object[]{packet.discountName, (int)(packet.discountValue * 100)};
                        break;

                    case CLEAR:
                        success = handleClear(player);
                        messageKey = success ? "gui.discount_cleared" : "gui.discount_clear_failed";
                        break;

                    case ENABLE:
                        success = handleEnable(player);
                        messageKey = success ? "gui.discount_enabled" : "gui.discount_enable_failed";
                        break;

                    case DISABLE:
                        success = handleDisable(player);
                        messageKey = success ? "gui.discount_disabled" : "gui.discount_disable_failed";
                        break;
                }
            } catch (Exception e) {
                TicketSystemMod.LOGGER.error("Error handling discount operation: {}", packet.operation, e);
                messageKey = "gui.discount_operation_error";
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
     * 处理设置折扣
     */
    private static boolean handleSet(DiscountOperationPacket packet, ServerPlayer player) {
        // 验证折扣值
        if (packet.discountValue < 0 || packet.discountValue > 1) {
            TicketSystemMod.LOGGER.error("Invalid discount value: {} (must be between 0 and 1)", packet.discountValue);
            return false;
        }

        // 验证折扣名称
        if (packet.discountName == null || packet.discountName.trim().isEmpty()) {
            TicketSystemMod.LOGGER.error("Discount name cannot be empty");
            return false;
        }

        // 执行设置
        return DiscountManager.setDiscount(packet.discountName.trim(), packet.discountValue);
    }

    /**
     * 处理清除折扣
     */
    private static boolean handleClear(ServerPlayer player) {
        DiscountManager.clearDiscount();
        return true;
    }

    /**
     * 处理启用折扣
     */
    private static boolean handleEnable(ServerPlayer player) {
        DiscountManager.enableDiscount();
        return true;
    }

    /**
     * 处理禁用折扣
     */
    private static boolean handleDisable(ServerPlayer player) {
        DiscountManager.disableDiscount();
        return true;
    }

    // Getter方法（用于测试）
    public Operation getOperation() {
        return operation;
    }

    public String getDiscountName() {
        return discountName;
    }

    public double getDiscountValue() {
        return discountValue;
    }
}