package com.easttown.ticketsystem.client;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.manager.StationManager;
import com.easttown.ticketsystem.util.LanguageHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Consumer;

public abstract class BaseStationList 
    extends AbstractSelectionList<BaseStationList.StationEntry> {
    
    // 颜色常量
    protected static final int BACKGROUND = 0xFFE0F7FA;
    protected static final int NORMAL_BG = 0xFFB3E5FC;
    protected static final int SELECTED_BG = 0xFF03A9F4;
    protected static final int HOVER_BG = 0xFF81D4FA;
    protected static final int START_BG = 0xFFB2EBF2;
    protected static final int DISABLED_BG = 0xFFEEEEEE;
    protected static final int NORMAL_TEXT = 0xFF01579B;
    protected static final int SELECTED_TEXT = 0xFFFFFFFF;
    protected static final int DISABLED_TEXT = 0xFF80DEEA;
    protected static final int START_TEXT = 0xFF81D4FA;
    
    protected final StationListProvider provider;
    protected final List<String> stations = new ArrayList<>();
    protected int listX, listY, listWidth, listHeight;
    
    // 分页状态
    protected int currentPage = 0;
    protected int itemsPerPage;
protected int totalPages = 1;
    
    protected boolean visible = true;
    
    // 选择状态
    protected String selectedStation = "";
    protected int selectedIndex = -1;
    
    // 滚动条状态 (已移除)
    protected int scrollbarWidth = 6;
    protected int scrollbarMargin = 2;
    
    // 点击监听器
    private Consumer<String> clickListener;

    public BaseStationList(StationListProvider provider, 
                         int x, int y, int width, int height) {
        super(
            Minecraft.getInstance(), 
            width,
            height,
            y,
            y + height,
            20
        );
        this.provider = provider;
        this.listX = x;
        this.listY = y;
        this.listWidth = width;
        this.listHeight = height;
        this.setLeftPos(x);
        
        // 计算每页显示的项目数量
        this.itemsPerPage = Math.max(1, height / this.itemHeight);
    }
    
    // 设置点击监听器
    public void setOnClick(Consumer<String> listener) {
        this.clickListener = listener;
    }
    
    // 设置选中的车站
    public void setSelectedStation(String station) {
        this.selectedStation = station;
        if (provider != null) {
            provider.setSelectedStation(station);
        }
        updateSelectionIndex();
    }
    
    // 获取选中的车站
    public String getSelectedStation() {
        return selectedStation;
    }
    
    // 更新选中索引
    private void updateSelectionIndex() {
        this.selectedIndex = stations.indexOf(selectedStation);
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public void setPosition(int x, int y, int width, int height) {
        this.listX = x;
        this.listY = y;
        this.listWidth = width;
        this.listHeight = height;
        this.setLeftPos(x);
        this.y0 = y;
        this.y1 = y + height;
        this.width = width;
        this.height = height;
        
        // 重新计算每页显示的项目数量
        this.itemsPerPage = Math.max(1, height / this.itemHeight);
        updateTotalPages();
    }
    
    public void refreshStations(String filter) {
        this.clearEntries();
        Set<String> allStations = StationManager.getStations();
        stations.clear();
        
        for (String station : allStations) {
            if (filter.isEmpty() || station.toLowerCase().contains(filter.toLowerCase())) {
                stations.add(station);
            }
        }
        Collections.sort(stations);
        
        // 更新总页数
        updateTotalPages();
        
        // 确保当前页在有效范围内
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));
        
        // 添加当前页的条目
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, stations.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            String station = stations.get(i);
            this.addEntry(createEntry(station));
        }
        
        // 更新选中索引
        updateSelectionIndex();
    }
    
    private void updateTotalPages() {
        totalPages = stations.isEmpty() ? 1 : (int) Math.ceil((double) stations.size() / itemsPerPage);
    }
    
    protected abstract StationEntry createEntry(String station);
    
    protected abstract boolean canSelectStation(String station);
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;
        
        // 绘制背景
        guiGraphics.fill(
            this.x0, this.y0, 
            this.x0 + this.width, this.y0 + this.height, 
            BACKGROUND
        );
        
        // 绘制边框
        guiGraphics.renderOutline(
            this.x0, this.y0, 
            this.getWidth(), this.getHeight(), 
            0xFF03A9F4
        );
        
        // 绘制当前页的条目
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, stations.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int displayIndex = i - startIndex;
            StationEntry entry = this.getEntry(displayIndex);
            int yPos = this.y0 + (displayIndex * getItemHeight());
            
            if (yPos >= this.y0 && yPos + getItemHeight() <= this.y1) {
                boolean isHovered = isMouseOver(mouseX, mouseY) && 
                                   mouseY >= yPos && mouseY < yPos+ getItemHeight();
                boolean isSelected = stations.get(i).equals(selectedStation);
                
                entry.render(
                    guiGraphics, 
                    i, 
                    yPos, 
                    this.x0, 
                    this.getWidth(),
                    getItemHeight(), 
                    mouseX, 
                    mouseY, 
                    isHovered,
                    partialTicks
                );
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        // 处理车站选择
        if (button == 0 && mouseX >= this.x0 && mouseX <= this.x0 + this.width && 
            mouseY >= this.y0 && mouseY <= this.y0 + this.height) {
            
            int relativeY = (int) (mouseY - this.y0);
            int index = relativeY / getItemHeight();
            int actualIndex = currentPage * itemsPerPage + index;
            
            if (actualIndex >= 0 && actualIndex < stations.size()) {
                String station = stations.get(actualIndex);
                if (canSelectStation(station)) {
                    setSelectedStation(station);
                    
                    // 触发点击监听器
                    if (clickListener != null) {
                        clickListener.accept(station);
                    }
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    // 翻页方法
    public void nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            refreshStations(provider != null ? provider.getSearchText() : "");
        }
    }
    
    public void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            refreshStations(provider != null ? provider.getSearchText() : "");
        }
    }
    
    public boolean hasNextPage() {
        return currentPage < totalPages - 1;
    }
    
    public boolean hasPrevPage() {
        return currentPage > 0;
    }
    
    @Override
    public void updateNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, LanguageHelper.translate("narrator.ticketsystem.station_list"));
        
        for (int i = 0; i < this.getItemCount(); i++) {
            StationEntry entry = this.getEntry(i);
            entry.updateNarration(output);
        }
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public int getItemHeight() {
        return this.itemHeight;
    }
    
    protected void renderScrollingText(GuiGraphics guiGraphics, String text, 
                                      int left, int top, int width, int height, 
                                      int color, String stationKey) {
        int maxWidth = width - 10;
        int textWidth = Minecraft.getInstance().font.width(text);
        
        if (textWidth <= maxWidth) {
            guiGraphics.drawString(
                Minecraft.getInstance().font, 
                text, 
                left + 5, 
                top + (height - 8) / 2, 
                color, 
                false
            );
            return;
        }
        
        // 实现滚动文本
        long currentTime = System.currentTimeMillis();
        int scrollOffset = (int) ((currentTime / 30) % (textWidth + maxWidth));
        
        if (scrollOffset <= textWidth) {
            // 正常滚动阶段
            guiGraphics.drawString(
                Minecraft.getInstance().font, 
                text, 
                left + 5 - scrollOffset, 
                top + (height - 8) / 2, 
                color, 
                false
            );
        } else {
            // 暂停阶段 - 显示完整文本
            guiGraphics.drawString(
                Minecraft.getInstance().font, 
                text, 
                left + 5, 
                top + (height - 8) / 2, 
                color, 
                false
            );
        }
    }

    public abstract static class StationEntry extends AbstractSelectionList.Entry<StationEntry> {
        protected final String station;
        protected boolean isSelected;

        public StationEntry(String station) {
            this.station = station;
        }
        
        public abstract void render(GuiGraphics guiGraphics, int index, int top, int left, 
                                  int width, int height, int mouseX, int mouseY, 
                                  boolean isHovered, float partialTicks);
        
        public void updateNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, 
                Component.translatable("narrator.ticketsystem.station_entry", station));
        }
        
        public String getStation() {
            return station;
        }
        
        public void setSelected(boolean selected) {
            this.isSelected = selected;
        }
    }
    
    public interface StationListProvider {
        void setSelectedStation(String station);
        String getSelectedStation();
        String getSearchText();
        String getStartStation();
    }
    
    public int getHoveredIndex() {
        return -1; // 简化实现
    }
    
    public List<String> getStations() {
        return new ArrayList<>(); // 简化实现
    }
}
