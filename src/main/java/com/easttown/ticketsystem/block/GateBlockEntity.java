package com.easttown.ticketsystem.block;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.init.BlockEntityInit;
import com.easttown.ticketsystem.item.TicketItem;
import com.easttown.ticketsystem.screen.GateConfigMenu;
import com.easttown.ticketsystem.util.GateUtil;
import com.easttown.ticketsystem.util.TicketSystemLogger;
import net.minecraft.world.ticks.TickPriority;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

public class GateBlockEntity extends BlockEntity implements MenuProvider {
    // 闸机配置属性
    private String gateId = "G-" + UUID.randomUUID().toString().substring(0, 8);
    private String stationId = "";
    private GateType gateType = GateType.BIDIRECTIONAL;
    private boolean allowReentry = false;
    private int maxTravelMinutes = 1440; // 默认24小时
    private boolean destroyTicket = true;
    private boolean enabled = true; // 闸机是否启用
    private Direction lastFacing = null;

    // 状态变量
    private long lastPassTime = 0;
    private int cooldownTicks = 20; // 默认1秒冷却

    // 玩家通行管理
    private UUID currentPlayerId; // 当前正在通过的玩家
    private int timeoutTicks = 0; // 超时计时器（1分钟=1200 ticks）
    private static final String GATE_TAG_PREFIX = "ticketsystem_gate_"; // 玩家标签前缀

    // AABB检测区域
    private AABB entryDetectionArea; // 入站检测面（正面）
    private AABB exitDetectionArea; // 出站检测面（背面）
    private static final double DETECTION_THICKNESS = 0.1; // 检测面厚度

    public GateBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.GATE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ticketsystem.gate");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new GateConfigMenu(containerId, inventory, this);
    }

    public void openConfigGui(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, this, buf -> {
                buf.writeBlockPos(worldPosition);
            });
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateDetectionAreas();
        TicketSystemMod.LOGGER.info("闸机方块实体加载完成，位置: {}", worldPosition);
    }

    // 更新检测区域
    private void updateDetectionAreas() {
        if (level == null)
            return;

        Direction facing = getBlockState().getValue(GateBlock.FACING);

        // 创建入站检测面（正面）
        entryDetectionArea = createFacingAABB(worldPosition, facing, DETECTION_THICKNESS);

        // 创建出站检测面（背面）
        exitDetectionArea = createFacingAABB(worldPosition, facing.getOpposite(), DETECTION_THICKNESS);

        // 调试日志
        TicketSystemMod.LOGGER.debug("更新闸机检测区域 - 位置: {}, 朝向: {}", worldPosition, facing);
        TicketSystemMod.LOGGER.debug("入站检测面: {}", entryDetectionArea);
        TicketSystemMod.LOGGER.debug("出站检测面: {}", exitDetectionArea);
    }

    // 创建面向指定方向的AABB
    private AABB createFacingAABB(BlockPos pos, Direction direction, double thickness) {
        double minX = pos.getX();
        double minY = pos.getY();
        double minZ = pos.getZ();
        double maxX = minX + 1;
        double maxY = minY + 1;
        double maxZ = minZ + 1;

        return switch (direction) {
            case NORTH -> new AABB(minX, minY, maxZ - thickness, maxX, maxY, maxZ);
            case SOUTH -> new AABB(minX, minY, minZ, maxX, maxY, minZ + thickness);
            case WEST -> new AABB(maxX - thickness, minY, minZ, maxX, maxY, maxZ);
            case EAST -> new AABB(minX, minY, minZ, minX + thickness, maxY, maxZ);
            case UP -> new AABB(minX, maxY - thickness, minZ, maxX, maxY, maxZ);
            case DOWN -> new AABB(minX, minY, minZ, maxX, minY + thickness, maxZ);
            default -> new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        };
    }

    public void tryPassThrough(Player player) {
        if (!enabled || !(level instanceof ServerLevel serverLevel))
            return;

        // 检查当前是否有玩家正在通过
        if (currentPlayerId != null) {
            player.displayClientMessage(Component.translatable("ticketsystem.gate.busy"), false);
            TicketSystemMod.LOGGER.debug("闸机繁忙，已有玩家通过: {}", currentPlayerId);
            return;
        }

        // 检查冷却时间
        if (serverLevel.getGameTime() - lastPassTime < cooldownTicks) {
            TicketSystemMod.LOGGER.debug("闸机冷却中，跳过处理");
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        GatePassResult result = checkTicket(heldItem, player);

        if (result.success) {
            TicketSystemMod.LOGGER.info("玩家 {} 使用有效车票，准备通过闸机", player.getName().getString());

            // 设置当前玩家和超时计时器
            currentPlayerId = player.getUUID();
            timeoutTicks = 1200; // 1分钟超时
            player.addTag(getPlayerTag());
            handleSuccess(player, heldItem);
        } else {
            TicketSystemMod.LOGGER.info("玩家 {} 车票无效: {}", player.getName().getString(), result.reason);
            handleFailure(player, result.reason);
        }

        lastPassTime = serverLevel.getGameTime();
    }

    private String getPlayerTag() {
        return GATE_TAG_PREFIX + gateId;
    }

    private void resetGate() {
        if (currentPlayerId != null) {
            Player player = level.getPlayerByUUID(currentPlayerId);
            if (player != null) {
                player.removeTag(getPlayerTag());
                TicketSystemMod.LOGGER.debug("移除玩家标签: {}", player.getName().getString());
            }
            currentPlayerId = null;
        }
        timeoutTicks = 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GateBlockEntity gate) {
        if (level.isClientSide || !gate.enabled)
            return;

        // 更新检测区域
        Direction currentFacing = state.getValue(GateBlock.FACING);
        if (gate.lastFacing != currentFacing) {
            gate.lastFacing = currentFacing;
            gate.updateDetectionAreas();

        }

        // 处理超时
        if (gate.timeoutTicks > 0) {
            gate.timeoutTicks--;
            if (gate.timeoutTicks <= 0) {
                // 超时，重置闸机并关闭门
                gate.resetGate();
                if (state.getValue(GateBlock.OPEN)) {
                    level.setBlock(pos, state.setValue(GateBlock.OPEN, false), 3);
                    TicketSystemMod.LOGGER.info("超时关闭闸机: {}", pos);
                }
            }
        }

        // 检测玩家位置
        if (gate.currentPlayerId != null) {
            Player player = level.getPlayerByUUID(gate.currentPlayerId);
            if (player == null) {
                // 玩家不在线，重置
                gate.resetGate();
                TicketSystemMod.LOGGER.debug("玩家离线，重置闸机");
                return;
            }

            // 使用AABB检测区域
            gate.checkDetectionAreas(player);
        }
    }

    private void checkDetectionAreas(Player player) {
        if (player == null || level == null)
            return;

        // 检测入站面（正面）
        if (entryDetectionArea != null && player.getBoundingBox().intersects(entryDetectionArea)) {
            TicketSystemMod.LOGGER.debug("玩家 {} 接触入站检测面", player.getName().getString());

            if (!player.getTags().contains(getPlayerTag())) {
                // 非法闯入，关闭闸机并重置
                level.setBlock(worldPosition, getBlockState().setValue(GateBlock.OPEN, false), 3);
                resetGate();
                handleFailure(player, "ticketsystem.gate.illegal_entry");
                TicketSystemMod.LOGGER.info("非法闯入! 玩家 {} 没有有效标签", player.getName().getString());
            }
        }

        // 检测出站面（背面）
        if (exitDetectionArea != null && player.getBoundingBox().intersects(exitDetectionArea)) {
            TicketSystemMod.LOGGER.debug("玩家 {} 接触出站检测面", player.getName().getString());

            // 完成通过
            player.removeTag(getPlayerTag());
            resetGate();

            // 延迟0.1秒关闭闸机
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 2, TickPriority.HIGH);
            TicketSystemMod.LOGGER.info("玩家 {} 通过闸机，闸机将在0.1秒后关闭", player.getName().getString());
        }
    }

    // 处理闸机关闭
    public void closeGate() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            if (state.getValue(GateBlock.OPEN)) {
                level.setBlock(worldPosition, state.setValue(GateBlock.OPEN, false), 3);
                TicketSystemMod.LOGGER.debug("闸机关闭 - 位置: {}", worldPosition);
            }
        }
    }

    private GatePassResult checkTicket(ItemStack ticketStack, Player player) {
        if (ticketStack.isEmpty() || !(ticketStack.getItem() instanceof TicketItem)) {
            return new GatePassResult(false, "ticketsystem.gate.no_ticket");
        }

        CompoundTag ticketTag = ticketStack.getTag();
        if (ticketTag == null) {
            return new GatePassResult(false, "ticketsystem.gate.invalid_ticket");
        }

        // 1. 检查车票ID
        UUID ticketId = GateUtil.getTicketId(ticketTag);
        if (ticketId == null) {
            return new GatePassResult(false, "ticketsystem.gate.invalid_ticket");
        }

        // 4. 获取车票状态
        String status = ticketTag.getString("Status");
        long issueTime = ticketTag.getLong("IssueTime");
        String startStation = ticketTag.getString("StartStation");
        String endStation = ticketTag.getString("Destination");

        long currentTime = System.currentTimeMillis();
        long travelTimeMinutes = (currentTime - issueTime) / 60000;

        // 5. 检查车票是否过期
        if (travelTimeMinutes > maxTravelMinutes) {
            return new GatePassResult(false, "ticketsystem.gate.expired");
        }

        // 根据闸机类型执行不同检查
        switch (gateType) {
            case IN:
                return checkInTicket(status, startStation, ticketTag);
            case OUT:
                return checkOutTicket(status, endStation, ticketTag);
            case BIDIRECTIONAL:
                return checkBidirectionalTicket(status, startStation, endStation, ticketTag);
            default:
                return new GatePassResult(false, "ticketsystem.gate.invalid_gate");
        }
    }

    private GatePassResult checkInTicket(String status, String startStation, CompoundTag ticketTag) {
        // 入站检查
        if (status.equals(TicketItem.COMPLETED)) {
            return new GatePassResult(false, "ticketsystem.gate.already_used");
        }

        if (status.equals(TicketItem.IN_USE)) {
            return new GatePassResult(false, "ticketsystem.gate.in_use");
        }

        if (!startStation.equals(stationId)) {
            return new GatePassResult(false, "ticketsystem.gate.wrong_start");
        }

        // 防止同站重复入站
        if (ticketTag.contains("gate_id") && ticketTag.getString("gate_id").equals(gateId)) {
            return new GatePassResult(false, "ticketsystem.gate.same_gate");
        }

        return new GatePassResult(true, "");
    }

    private GatePassResult checkOutTicket(String status, String endStation, CompoundTag ticketTag) {
        // 出站检查
        if (status.equals(TicketItem.UNUSED)) {
            return new GatePassResult(false, "ticketsystem.gate.not_used");
        }

        if (status.equals(TicketItem.COMPLETED)) {
            return new GatePassResult(false, "ticketsystem.gate.already_used");
        }

        if (!endStation.equals(stationId)) {
            return new GatePassResult(false, "ticketsystem.gate.wrong_end");
        }

        // 防止同站重复出站
        if (ticketTag.contains("exit_gate_id") && ticketTag.getString("exit_gate_id").equals(gateId)) {
            return new GatePassResult(false, "ticketsystem.gate.same_gate");
        }

        return new GatePassResult(true, "");
    }

    private GatePassResult checkBidirectionalTicket(String status, String startStation, String endStation,
            CompoundTag ticketTag) {
        if (status.equals(TicketItem.IN_USE)) {
            // 已使用票作为出站
            if (!endStation.equals(stationId)) {
                return new GatePassResult(false, "ticketsystem.gate.wrong_end");
            }
            return new GatePassResult(true, "");
        } else if (status.equals(TicketItem.UNUSED)) {
            // 未使用票作为入站
            if (!startStation.equals(stationId)) {
                return new GatePassResult(false, "ticketsystem.gate.wrong_start");
            }
            return new GatePassResult(true, "");
        }
        return new GatePassResult(false, "ticketsystem.gate.invalid_status");
    }

    private void handleSuccess(Player player, ItemStack ticketStack) {
        // 设置闸机为开启状态
        BlockState state = getBlockState();
        if (state.getValue(GateBlock.OPEN)) {
            // 如果已经是开启状态，先关闭再打开，确保动画播放
            level.setBlock(worldPosition, state.setValue(GateBlock.OPEN, false), 3);
        }
        level.setBlock(worldPosition, state.setValue(GateBlock.OPEN, true), 3);
        TicketSystemMod.LOGGER.info("闸机开启: {}", worldPosition);

        // 处理车票
        CompoundTag ticketTag = ticketStack.getOrCreateTag();
        boolean isInGate = gateType == GateType.IN ||
                (gateType == GateType.BIDIRECTIONAL && ticketTag.getString("Status").equals(TicketItem.UNUSED));

        String ticketId = ticketTag.getString("TicketId");
        String station = stationId;
        String gateTypeStr = gateType.name();

        if (isInGate) {
            // 入站处理 - 更新车票状态（注意：旅行计时在通过闸机后才开始）
            ticketTag.putString("Status", TicketItem.IN_USE);
            ticketTag.putString("EntryGate", gateId);

            // 记录入站成功日志
            TicketSystemLogger.logGatePassage(player, station, ticketId, true, "入站成功", gateTypeStr);
        } else {
            // 出站处理
            ticketTag.putString("Status", TicketItem.COMPLETED);
            ticketTag.putString("ExitGate", gateId);
            ticketTag.putLong("ExitTime", System.currentTimeMillis());

            // 是否销毁车票
            if (destroyTicket) {
                ticketStack.shrink(1);
            }

            // 记录出站成功日志
            TicketSystemLogger.logGatePassage(player, station, ticketId, true, "出站成功", gateTypeStr);
        }
    }

    private void handleFailure(Player player, String reasonKey) {
        // 发送失败消息
        player.displayClientMessage(Component.translatable(reasonKey), false);

        // 记录失败日志
        String reason = getFailureReason(reasonKey);
        TicketSystemLogger.logGatePassage(player, stationId, "未知", false, reason, gateType.name());
    }

    private String getFailureReason(String reasonKey) {
        switch (reasonKey) {
            case "ticketsystem.gate.no_ticket":
                return "没有车票";
            case "ticketsystem.gate.invalid_ticket":
                return "无效车票";
            case "ticketsystem.gate.expired":
                return "车票已过期";
            case "ticketsystem.gate.wrong_start":
                return "起点站不匹配";
            case "ticketsystem.gate.wrong_end":
                return "终点站不匹配";
            case "ticketsystem.gate.invalid_status":
                return "车票状态无效";
            case "ticketsystem.gate.illegal_entry":
                return "非法闯入";
            default:
                return "未知原因";
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("GateId", gateId);
        tag.putString("StationId", stationId);
        tag.putString("GateType", gateType.name());
        tag.putBoolean("AllowReentry", allowReentry);
        tag.putInt("MaxTravelMinutes", maxTravelMinutes);
        tag.putBoolean("DestroyTicket", destroyTicket);
        tag.putBoolean("Enabled", enabled);
        if (currentPlayerId != null) {
            tag.putUUID("CurrentPlayer", currentPlayerId);
        }
        tag.putInt("TimeoutTicks", timeoutTicks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        gateId = tag.getString("GateId");
        stationId = tag.getString("StationId");
        gateType = GateType.valueOf(tag.getString("GateType"));
        allowReentry = tag.getBoolean("AllowReentry");
        maxTravelMinutes = tag.getInt("MaxTravelMinutes");
        destroyTicket = tag.getBoolean("DestroyTicket");
        enabled = tag.getBoolean("Enabled");
        if (tag.hasUUID("CurrentPlayer")) {
            currentPlayerId = tag.getUUID("CurrentPlayer");
        }
        timeoutTicks = tag.getInt("TimeoutTicks");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        handleUpdateTag(pkt.getTag());
    }

    // 闸机配置方法
    public String getGateId() {
        return gateId;
    }

    public void setGateId(String gateId) {
        this.gateId = gateId;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public GateType getGateType() {
        return gateType;
    }

    public void setGateType(GateType gateType) {
        this.gateType = gateType;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isAllowReentry() {
        return allowReentry;
    }

    public void setAllowReentry(boolean allowReentry) {
        this.allowReentry = allowReentry;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getMaxTravelMinutes() {
        return maxTravelMinutes;
    }

    public void setMaxTravelMinutes(int maxTravelMinutes) {
        this.maxTravelMinutes = maxTravelMinutes;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isDestroyTicket() {
        return destroyTicket;
    }

    public void setDestroyTicket(boolean destroyTicket) {
        this.destroyTicket = destroyTicket;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}

class GatePassResult {
    public final boolean success;
    public final String reason;

    public GatePassResult(boolean success, String reason) {
        this.success = success;
        this.reason = reason;
    }
}
