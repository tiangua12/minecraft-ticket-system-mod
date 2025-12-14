package com.easttown.ticketsystem.client;

import com.easttown.ticketsystem.screen.TicketMachineScreen;
import com.easttown.ticketsystem.util.LanguageHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;

public class AdminStationList extends BaseStationList {
    
    public AdminStationList(StationListProvider provider, 
                         int x, int y, int width, int height) {
        super(provider, x, y, width, height);
    }
    
    @Override
    protected AdminStationEntry createEntry(String station) {
        return new AdminStationEntry(station);
    }
    
    @Override
    protected boolean canSelectStation(String station) {
        // 管理员可以选择任何车站
        return true;
    }
    
    @Override
    public void updateNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, LanguageHelper.translate("narrator.ticketsystem.admin_station_list"));
    }
    
    public class AdminStationEntry extends StationEntry {
        public AdminStationEntry(String station) {
            super(station);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, 
                          int width, int height, int mouseX, int mouseY, 
                          boolean isHovered, float partialTicks) {
            
            String startStation = provider != null ? provider.getStartStation() : "";
            boolean isStart = station.equals(startStation);
            
            // 绘制条目背景
            if (isSelected) {
                guiGraphics.fill(left, top, left + width, top + height, SELECTED_BG);
            } else if (isHovered) {
                guiGraphics.fill(left, top, left + width, top + height, HOVER_BG);
            } else if (isStart) {
                guiGraphics.fill(left, top, left + width, top + height, START_BG);
            } else {
                guiGraphics.fill(left, top, left + width, top + height, NORMAL_BG);
            }
            
            // 绘制车站名称
            int color = isStart ? START_TEXT : (isSelected ? SELECTED_TEXT : NORMAL_TEXT);
            renderScrollingText(guiGraphics, station, left, top, width, height, color, getStation());
        }
        
        @Override
        public void updateNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal(station));
        }
    }
}
