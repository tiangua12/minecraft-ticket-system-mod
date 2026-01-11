package com.easttown.ticketsystem.screen;

import com.easttown.ticketsystem.block.GateBlockEntity;
import com.easttown.ticketsystem.block.GateType;
import com.easttown.ticketsystem.network.NetworkHandler;
import com.easttown.ticketsystem.network.UpdateGateConfigPacket;
import com.easttown.ticketsystem.util.LanguageHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GateConfigScreen extends AbstractContainerScreen<GateConfigMenu> {
    private static final int WIDTH = 256;
    private static final int HEIGHT = 300;
    
    private final GateBlockEntity blockEntity;
    
    // 配置控件
    private EditBox gateIdField;
    private EditBox stationIdField;
    private Button gateTypeButton;
    private Button allowReentryButton;
    private EditBox maxTravelMinutesField;
    private Button destroyTicketButton;
    private Button enabledButton;
    
    public GateConfigScreen(GateConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }
    
    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - imageWidth) / 2;
        this.topPos = (this.height - imageHeight) / 2;
        
        int labelX = leftPos + 10;
        int fieldX = leftPos + 120;
        int y = topPos + 25;
        int spacing = 25;
        
        // 闸机ID
        gateIdField = new EditBox(this.font, fieldX, y, 120, 20, Component.literal(""));
        gateIdField.setValue(blockEntity.getGateId());
        addRenderableWidget(gateIdField);
        y += spacing;
        
        // 站点ID
        stationIdField = new EditBox(this.font, fieldX, y, 120, 20, Component.literal(""));
        stationIdField.setValue(blockEntity.getStationId());
        addRenderableWidget(stationIdField);
        y += spacing;
        
        // 闸机类型按钮
        gateTypeButton = new Button.Builder(
                getGateTypeText(blockEntity.getGateType()), 
                button -> cycleGateType())
            .bounds(fieldX, y, 120, 20)
            .build();
        addRenderableWidget(gateTypeButton);
        y += spacing;
        
        // 允许重新进站
        allowReentryButton = new Button.Builder(
                getBooleanText(blockEntity.isAllowReentry(), "ticketsystem.gui.allow_reentry"), 
                button -> toggleBooleanSetting(button, "ticketsystem.gui.allow_reentry"))
            .bounds(fieldX, y, 120, 20)
            .build();
        addRenderableWidget(allowReentryButton);
        y += spacing;
        
        // 最大旅行时间
        maxTravelMinutesField = new EditBox(this.font, fieldX, y, 120, 20, Component.literal(""));
        maxTravelMinutesField.setValue(String.valueOf(blockEntity.getMaxTravelMinutes()));
        addRenderableWidget(maxTravelMinutesField);
        y += spacing;
        
        // 销毁车票
        destroyTicketButton = new Button.Builder(
                getBooleanText(blockEntity.isDestroyTicket(), "ticketsystem.gui.destroy_ticket"), 
                button -> toggleBooleanSetting(button, "ticketsystem.gui.destroy_ticket"))
            .bounds(fieldX, y, 120, 20)
            .build();
        addRenderableWidget(destroyTicketButton);
        y += spacing + 15;
        
        // 启用/禁用闸机
        enabledButton = new Button.Builder(
                getBooleanText(blockEntity.isEnabled(), "ticketsystem.gui.enabled"), 
                button -> toggleEnabled())
            .bounds(fieldX, y, 120, 20)
            .build();
        addRenderableWidget(enabledButton);
        
        // 按钮区域
        int buttonY = topPos + HEIGHT - 35;
        // 保存按钮
        addRenderableWidget(new Button.Builder(
                Component.translatable("ticketsystem.gui.save_settings"), 
                button -> saveSettings())
            .bounds(leftPos + 50, buttonY, 70, 20)
            .build());
        
        // 取消按钮
        addRenderableWidget(new Button.Builder(
                Component.translatable("ticketsystem.gui.cancel"), 
                button -> onClose())
            .bounds(leftPos + 130, buttonY, 70, 20)
            .build());
    }
    
    private Component getBooleanText(boolean value, String key) {
        return Component.translatable(key + (value ? ".on" : ".off"));
    }
    
    private Component getGateTypeText(GateType gateType) {
        return Component.translatable("ticketsystem.gate_type." + gateType.name().toLowerCase());
    }
    
    private void cycleGateType() {
        GateType[] values = GateType.values();
        GateType current = blockEntity.getGateType();
        int nextIndex = (current.ordinal() + 1) % values.length;
        GateType nextType = values[nextIndex];
        blockEntity.setGateType(nextType);
        gateTypeButton.setMessage(getGateTypeText(nextType));
    }
    
    private void toggleBooleanSetting(Button button, String key) {
        boolean currentValue;
        switch (key) {
            case "ticketsystem.gui.allow_reentry":
                currentValue = blockEntity.isAllowReentry();
                blockEntity.setAllowReentry(!currentValue);
                button.setMessage(getBooleanText(!currentValue, key));
                break;
            case "ticketsystem.gui.destroy_ticket":
                currentValue = blockEntity.isDestroyTicket();
                blockEntity.setDestroyTicket(!currentValue);
                button.setMessage(getBooleanText(!currentValue, key));
                break;
        }
    }

    private void toggleEnabled() {
        boolean newValue = !blockEntity.isEnabled();
        blockEntity.setEnabled(newValue);
        enabledButton.setMessage(getBooleanText(newValue, "ticketsystem.gui.enabled"));
    }
    
    private void saveSettings() {
        // 获取控件中的值
        String gateId = gateIdField.getValue();
        String stationId = stationIdField.getValue();
        String gateType = blockEntity.getGateType().name();
        boolean allowReentry = blockEntity.isAllowReentry();
        int maxTravelMinutes;
        try {
            maxTravelMinutes = Integer.parseInt(maxTravelMinutesField.getValue());
        } catch (NumberFormatException e) {
            maxTravelMinutes = blockEntity.getMaxTravelMinutes();
        }
        boolean destroyTicket = blockEntity.isDestroyTicket();
        boolean enabled = blockEntity.isEnabled();

        // 发送更新包到服务器
        NetworkHandler.sendToServer(new UpdateGateConfigPacket(
            blockEntity.getBlockPos(),
            gateId,
            stationId,
            gateType,
            allowReentry,
            maxTravelMinutes,
            destroyTicket,
            enabled
        ));

        // 关闭界面
        onClose();
    }
    
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // 绘制背景
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        guiGraphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF000000);
        
        // 绘制标题
        guiGraphics.drawCenteredString(font, title, leftPos + imageWidth / 2, topPos + 5, 0x000000);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int labelX = 10;
        int y = 30;
        int spacing = 25;
        
        // 绘制标签 - 与新的布局对齐
        guiGraphics.drawString(font, LanguageHelper.translate("gui.gate_id"), labelX, y, 0x000000, false);
        y += spacing;
        guiGraphics.drawString(font, LanguageHelper.translate("gui.station_id"), labelX, y, 0x000000, false);
        y += spacing;
        guiGraphics.drawString(font, LanguageHelper.translate("gui.gate_type"), labelX, y, 0x000000, false);
        y += spacing;
        guiGraphics.drawString(font, LanguageHelper.translate("gui.allow_reentry"), labelX, y, 0x000000, false);
        y += spacing;
        guiGraphics.drawString(font, LanguageHelper.translate("gui.max_travel_minutes"), labelX, y, 0x000000, false);
        y += spacing;
        guiGraphics.drawString(font, LanguageHelper.translate("gui.destroy_ticket"), labelX, y, 0x000000, false);
        y += spacing + 15;
        guiGraphics.drawString(font, LanguageHelper.translate("gui.enabled"), labelX, y, 0x000000, false);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
