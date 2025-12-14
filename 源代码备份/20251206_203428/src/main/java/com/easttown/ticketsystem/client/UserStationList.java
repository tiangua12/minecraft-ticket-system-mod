package com.easttown.ticketsystem.client;

import com.easttown.ticketsystem.screen.TicketMachineScreen;
import com.easttown.ticketsystem.util.LanguageHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;

public class UserStationList extends BaseStationList {

    public UserStationList(StationListProvider provider, 
                         int x, int y, int width, int height) {
        super(provider, x, y, width, height);
    }
    
    @Override
    protected UserStationEntry createEntry(String station) {
        return new UserStationEntry(station);
    }
    
    @Override
    protected boolean canSelectStation(String station) {
        // 用户不能选择起始站作为目的地
        if (provider != null && provider.getStartStation() != null) {
            return !station.equals(provider.getStartStation());
        }
        return true;
    }
    
    @Override
    public void updateNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, LanguageHelper.translate("narrator.ticketsystem.user_station_list"));
    }
    
    public class UserStationEntry extends StationEntry {
        public UserStationEntry(String station) {
            super(station);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, 
                          int width, int height, int mouseX, int mouseY, 
                          boolean isHovered, float partialTicks) {
            
            String startStation = provider != null ? provider.getStartStation() : "";
            boolean isStart = station.equals(startStation);
            boolean isDisabled = !canSelectStation(station);

            // 绘制背景
            if (isStart) {
                guiGraphics.fill(left, top, left + width, top + height, START_BG);
            } else if (isSelected) {
                guiGraphics.fill(left, top, left + width, top + height, SELECTED_BG);
            } else if (isHovered) {
                guiGraphics.fill(left, top, left + width, top + height, HOVER_BG);
            } else if (isDisabled) {
                guiGraphics.fill(left, top, left + width, top + height, DISABLED_BG);
            } else {
                guiGraphics.fill(left, top, left + width, top + height, NORMAL_BG);
            }

            // 绘制文本
            int textColor;
            if (isStart) {
                textColor = START_TEXT;
            } else if (isSelected) {
                textColor = SELECTED_TEXT;
            } else if (isDisabled) {
                textColor = DISABLED_TEXT;
            } else {
                textColor = NORMAL_TEXT;
            }
            
            renderScrollingText(guiGraphics, station, left, top, width, height, textColor, getStation());
        }
        
        @Override
        public void updateNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal(station));
        }
    }
}
