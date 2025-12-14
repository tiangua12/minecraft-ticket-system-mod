package com.easttown.ticketsystem.screen;

import com.easttown.ticketsystem.manager.StationManager;
import com.easttown.ticketsystem.util.LanguageHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class AddStationScreen extends Screen {
    private EditBox stationNameField;
    private EditBox xField, yField, zField;
    private final BlockPos playerPos;
    
    public AddStationScreen(BlockPos playerPos) {
        super(LanguageHelper.translate("gui.add_station.title"));
        this.playerPos = playerPos;
    }
    
    @Override
    protected void init() {
        super.init();
        int centerX = width / 2 - 100;
        int centerY = height / 2 - 30;
        
        // 车站名称输入框
        stationNameField = new EditBox(font, centerX, centerY - 40, 200, 20, 
            LanguageHelper.translate("gui.station_name"));
        stationNameField.setMaxLength(32);
        addRenderableWidget(stationNameField);
        setInitialFocus(stationNameField);
        
        // 坐标输入框
        xField = new EditBox(font, centerX, centerY, 60, 20, Component.literal("X"));
        xField.setValue(String.valueOf(playerPos.getX()));
        addRenderableWidget(xField);
        
        yField = new EditBox(font, centerX + 80, centerY, 60, 20, Component.literal("Y"));
        yField.setValue(String.valueOf(playerPos.getY()));
        addRenderableWidget(yField);
        
        zField = new EditBox(font, centerX + 160, centerY, 60, 20, Component.literal("Z"));
        zField.setValue(String.valueOf(playerPos.getZ()));
        addRenderableWidget(zField);
        
        // 添加按钮
        addRenderableWidget(Button.builder(LanguageHelper.translate("gui.add"), button -> {
            addStation();
        }).bounds(centerX, centerY + 40, 95, 20).build());
        
        // 取消按钮
        addRenderableWidget(Button.builder(LanguageHelper.translate("gui.cancel"), button -> {
            Minecraft.getInstance().setScreen(null);
        }).bounds(centerX + 105, centerY + 40, 95, 20).build());
    }
    
    // 添加车站逻辑
    private void addStation() {
        String stationName = stationNameField.getValue().trim();
        String xText = xField.getValue().trim().replace("~", "");
        String yText = yField.getValue().trim().replace("~", "");
        String zText = zField.getValue().trim().replace("~", "");
        
        if (stationName.isEmpty() || xText.isEmpty() || yText.isEmpty() || zText.isEmpty()) {
            // 显示缺失信息错误
            Minecraft.getInstance().player.displayClientMessage(
                LanguageHelper.translate("gui.missing_info"), false);
            return;
        }
        
        try {
            int x = Integer.parseInt(xText);
            int y = Integer.parseInt(yText);
            int z = Integer.parseInt(zText);
            
            // 添加车站到管理器
            StationManager.addStation(stationName, x, y, z);
            Minecraft.getInstance().setScreen(null);
            
            // 显示成功消息
            Minecraft.getInstance().player.displayClientMessage(
                LanguageHelper.translate("command.station_added", stationName), false);
        } catch (NumberFormatException e) {
            // 显示无效坐标错误
            Minecraft.getInstance().player.displayClientMessage(
                LanguageHelper.translate("gui.invalid_coord"), false);
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染背景
        renderBackground(guiGraphics);
        // 居中绘制标题
        guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 80, 0xFFFFFF);
        
        // 绘制标签
        guiGraphics.drawString(font, LanguageHelper.translate("gui.station_name"), 
            width / 2 - 100, height / 2 - 55, 0xFFFFFF, false);
        
        guiGraphics.drawString(font, "X:", width / 2 - 115, height / 2 - 25, 0xFFFFFF);
        guiGraphics.drawString(font, "Y:", width / 2 - 45, height / 2 - 25, 0xFFFFFF);
        guiGraphics.drawString(font, "Z:", width / 2 + 25, height / 2 - 25, 0xFFFFFF);
        
        // 绘制波浪线提示
        guiGraphics.drawString(font, LanguageHelper.translate("gui.tilde_hint"), 
            width / 2 - 100, height / 2 - 5, 0xAAAAAA, false);
        
        // 渲染所有组件
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}