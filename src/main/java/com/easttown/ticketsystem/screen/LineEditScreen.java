package com.easttown.ticketsystem.screen;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.data.Line;
import com.easttown.ticketsystem.data.Station;
import com.easttown.ticketsystem.manager.LineManager;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.easttown.ticketsystem.util.IdGenerator;
import com.easttown.ticketsystem.util.LanguageHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 线路编辑界面
 * 用于创建或编辑线路信息
 */
public class LineEditScreen extends Screen {
    private final Line existingLine; // null表示创建新线路
    private final boolean isNewLine;

    // 表单字段
    private EditBox idField;
    private EditBox nameField;
    private EditBox enNameField;
    private EditBox colorField;
    private Button saveButton;
    private Button cancelButton;
    private Button addStationButton;
    private Button removeStationButton;
    private Button moveUpButton;
    private Button moveDownButton;

    // 车站选择组件
    private StationSelector stationSelector;
    private StationList stationList;

    // 车站列表数据
    private List<String> selectedStationCodes = new ArrayList<>();
    private List<Station> availableStations = new ArrayList<>();

    // 位置常量
    private static final int FIELD_WIDTH = 150;
    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_SPACING = 25;

    private static final int LEFT_COL_X = 20;
    private static final int RIGHT_COL_X = 200;
    private static final int START_Y = 40;

    private static final int STATION_LIST_WIDTH = 180;
    private static final int STATION_LIST_HEIGHT = 120;
    private static final int STATION_SELECTOR_WIDTH = 180;
    private static final int STATION_SELECTOR_HEIGHT = 120;

    public LineEditScreen(Line existingLine) {
        super(LanguageHelper.translate(existingLine == null ? "gui.create_line.title" : "gui.edit_line.title"));
        this.existingLine = existingLine;
        this.isNewLine = existingLine == null;
    }

    @Override
    protected void init() {
        super.init();

        // 确保数据管理器初始化
        NetworkManager.initialize();

        // 加载所有可用车站
        refreshAvailableStations();

        // 初始化表单字段
        int fieldY = START_Y;

        // 线路ID
        String initialId = isNewLine ? IdGenerator.generateLineId() : existingLine.getId();
        idField = new EditBox(font, LEFT_COL_X, fieldY, FIELD_WIDTH, FIELD_HEIGHT,
                Component.translatable("gui.line_id"));
        idField.setValue(initialId);
        idField.setEditable(isNewLine); // 新线路可编辑，已有线路ID不可更改
        addRenderableWidget(idField);
        fieldY += FIELD_SPACING;

        // 线路中文名称
        String initialName = isNewLine ? "" : existingLine.getName();
        nameField = new EditBox(font, LEFT_COL_X, fieldY, FIELD_WIDTH, FIELD_HEIGHT,
                Component.translatable("gui.line_name"));
        nameField.setValue(initialName);
        addRenderableWidget(nameField);
        fieldY += FIELD_SPACING;

        // 线路英文名称（可选）
        String initialEnName = isNewLine ? "" : existingLine.getEnName();
        enNameField = new EditBox(font, LEFT_COL_X, fieldY, FIELD_WIDTH, FIELD_HEIGHT,
                Component.translatable("gui.line_en_name"));
        enNameField.setValue(initialEnName);
        addRenderableWidget(enNameField);
        fieldY += FIELD_SPACING;

        // 线路颜色
        String initialColor = isNewLine ? "#3366CC" : existingLine.getColor();
        colorField = new EditBox(font, LEFT_COL_X, fieldY, FIELD_WIDTH, FIELD_HEIGHT,
                Component.translatable("gui.line_color"));
        colorField.setValue(initialColor);
        addRenderableWidget(colorField);
        fieldY += FIELD_SPACING;

        // 加载已有线路的车站列表
        if (!isNewLine) {
            selectedStationCodes = new ArrayList<>(existingLine.getStationCodes());
        }

        // 车站列表（已选择的车站）
        int listY = START_Y;
        stationList = new StationList(this, LEFT_COL_X, listY + 120,
                STATION_LIST_WIDTH, STATION_LIST_HEIGHT);
        addRenderableWidget(stationList);

        // 车站选择器（可用车站）
        stationSelector = new StationSelector(this, RIGHT_COL_X, listY,
                STATION_SELECTOR_WIDTH, STATION_SELECTOR_HEIGHT);
        addRenderableWidget(stationSelector);

        // 按钮位置
        int buttonY = listY + STATION_LIST_HEIGHT + 10;

        // 添加车站按钮
        addStationButton = Button.builder(
                LanguageHelper.translate("gui.add_station_to_line"),
                button -> onAddStation()
        ).bounds(LEFT_COL_X, buttonY, 120, 20).build();
        addRenderableWidget(addStationButton);

        // 移除车站按钮
        removeStationButton = Button.builder(
                LanguageHelper.translate("gui.remove_station_from_line"),
                button -> onRemoveStation()
        ).bounds(LEFT_COL_X + 125, buttonY, 120, 20).build();
        addRenderableWidget(removeStationButton);
        removeStationButton.active = false;

        // 上移按钮
        moveUpButton = Button.builder(
                Component.literal("▲"),
                button -> onMoveStationUp()
        ).bounds(RIGHT_COL_X, buttonY, 40, 20).build();
        addRenderableWidget(moveUpButton);
        moveUpButton.active = false;

        // 下移按钮
        moveDownButton = Button.builder(
                Component.literal("▼"),
                button -> onMoveStationDown()
        ).bounds(RIGHT_COL_X + 45, buttonY, 40, 20).build();
        addRenderableWidget(moveDownButton);
        moveDownButton.active = false;

        // 底部按钮
        int bottomY = height - 40;

        // 保存按钮
        saveButton = Button.builder(
                LanguageHelper.translate("gui.save"),
                button -> onSave()
        ).bounds(width / 2 - 105, bottomY, 100, 20).build();
        addRenderableWidget(saveButton);

        // 取消按钮
        cancelButton = Button.builder(
                LanguageHelper.translate("gui.cancel"),
                button -> onCancel()
        ).bounds(width / 2 + 5, bottomY, 100, 20).build();
        addRenderableWidget(cancelButton);

        // 刷新列表
        refreshStationList();
        refreshStationSelector();
    }

    /**
     * 刷新可用车站列表
     */
    private void refreshAvailableStations() {
        availableStations.clear();
        for (Station station : NetworkManager.getAllStations()) {
            availableStations.add(station);
        }
    }

    /**
     * 刷新车站列表（已选择的车站）
     */
    private void refreshStationList() {
        stationList.refreshStations(selectedStationCodes);
        updateButtonStates();
    }

    /**
     * 刷新车站选择器（可用车站）
     */
    private void refreshStationSelector() {
        stationSelector.refreshStations(availableStations, selectedStationCodes);
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasStationSelected = stationList.getSelectedIndex() >= 0;
        removeStationButton.active = hasStationSelected;
        moveUpButton.active = hasStationSelected && stationList.getSelectedIndex() > 0;
        moveDownButton.active = hasStationSelected && stationList.getSelectedIndex() < selectedStationCodes.size() - 1;
    }

    /**
     * 添加车站到线路
     */
    private void onAddStation() {
        String selectedStationCode = stationSelector.getSelectedStationCode();
        if (selectedStationCode != null && !selectedStationCodes.contains(selectedStationCode)) {
            selectedStationCodes.add(selectedStationCode);
            refreshStationList();
            refreshStationSelector();
        }
    }

    /**
     * 从线路移除车站
     */
    private void onRemoveStation() {
        int selectedIndex = stationList.getSelectedIndex();
        if (selectedIndex >= 0) {
            selectedStationCodes.remove(selectedIndex);
            refreshStationList();
            refreshStationSelector();
        }
    }

    /**
     * 上移车站
     */
    private void onMoveStationUp() {
        int selectedIndex = stationList.getSelectedIndex();
        if (selectedIndex > 0) {
            String station = selectedStationCodes.get(selectedIndex);
            selectedStationCodes.remove(selectedIndex);
            selectedStationCodes.add(selectedIndex - 1, station);
            refreshStationList();
            stationList.setSelectedIndex(selectedIndex - 1);
        }
    }

    /**
     * 下移车站
     */
    private void onMoveStationDown() {
        int selectedIndex = stationList.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < selectedStationCodes.size() - 1) {
            String station = selectedStationCodes.get(selectedIndex);
            selectedStationCodes.remove(selectedIndex);
            selectedStationCodes.add(selectedIndex + 1, station);
            refreshStationList();
            stationList.setSelectedIndex(selectedIndex + 1);
        }
    }

    /**
     * 保存线路
     */
    private void onSave() {
        // 验证输入
        String lineId = idField.getValue().trim();
        String lineName = nameField.getValue().trim();
        String lineColor = colorField.getValue().trim();

        if (lineId.isEmpty() || lineName.isEmpty() || lineColor.isEmpty()) {
            Minecraft.getInstance().player.displayClientMessage(
                    LanguageHelper.translate("gui.missing_required_fields"),
                    true);
            return;
        }

        if (selectedStationCodes.size() < 2) {
            Minecraft.getInstance().player.displayClientMessage(
                    LanguageHelper.translate("gui.line_needs_at_least_2_stations"),
                    true);
            return;
        }

        // 创建或更新线路对象
        Line line;
        if (isNewLine) {
            line = new Line(lineId, lineName, lineColor);
        } else {
            line = new Line(lineId, lineName, existingLine.getEnName(), lineColor);
        }

        line.setEnName(enNameField.getValue().trim());
        line.setStationCodes(new ArrayList<>(selectedStationCodes));

        // TODO: 通过网络包发送到服务器处理
        // 临时直接调用LineManager（仅用于测试）
        boolean success;
        if (isNewLine) {
            success = LineManager.addLine(line);
        } else {
            success = LineManager.updateLine(line);
        }

        if (success) {
            Minecraft.getInstance().player.displayClientMessage(
                    LanguageHelper.translate("gui.line_saved", lineName),
                    true);
            Minecraft.getInstance().setScreen(new LineManagementScreen());
        } else {
            Minecraft.getInstance().player.displayClientMessage(
                    LanguageHelper.translate("gui.line_save_failed"),
                    true);
        }
    }

    /**
     * 取消编辑
     */
    private void onCancel() {
        Minecraft.getInstance().setScreen(new LineManagementScreen());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);

        // 渲染标题
        guiGraphics.drawString(font, title, LEFT_COL_X, 10, 0xFFFFFF);

        // 渲染字段标签
        int labelY = START_Y - 12;
        guiGraphics.drawString(font, LanguageHelper.translate("gui.line_id") + ":", LEFT_COL_X, labelY, 0xCCCCCC);
        labelY += FIELD_SPACING;
        guiGraphics.drawString(font, LanguageHelper.translate("gui.line_name") + ":", LEFT_COL_X, labelY, 0xCCCCCC);
        labelY += FIELD_SPACING;
        guiGraphics.drawString(font, LanguageHelper.translate("gui.line_en_name") + ":", LEFT_COL_X, labelY, 0xCCCCCC);
        labelY += FIELD_SPACING;
        guiGraphics.drawString(font, LanguageHelper.translate("gui.line_color") + ":", LEFT_COL_X, labelY, 0xCCCCCC);

        // 渲染车站列表标签
        guiGraphics.drawString(font, LanguageHelper.translate("gui.selected_stations") + ":",
                LEFT_COL_X, START_Y + 105, 0xCCCCCC);
        guiGraphics.drawString(font, LanguageHelper.translate("gui.available_stations") + ":",
                RIGHT_COL_X, START_Y - 12, 0xCCCCCC);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    // ==================== 车站列表组件 ====================

    /**
     * 已选择车站列表组件
     */
    private static class StationList extends AbstractSelectionList<StationList.StationEntry> {
        private final LineEditScreen parent;
        private List<String> stationCodes = new ArrayList<>();
        private int selectedIndex = -1;

        public StationList(LineEditScreen parent, int x, int y, int width, int height) {
            super(Minecraft.getInstance(), width, height, y, y + height, 20);
            this.parent = parent;
            this.setLeftPos(x);
        }

        public void refreshStations(List<String> stationCodes) {
            this.clearEntries();
            this.stationCodes = new ArrayList<>(stationCodes);

            for (int i = 0; i < stationCodes.size(); i++) {
                String stationCode = stationCodes.get(i);
                this.addEntry(new StationEntry(stationCode, i + 1));
            }

            // 保持选中状态
            if (selectedIndex >= 0 && selectedIndex < stationCodes.size()) {
                setSelectedIndex(selectedIndex);
            } else {
                selectedIndex = -1;
            }
        }

        public int getSelectedIndex() {
            return selectedIndex;
        }

        public void setSelectedIndex(int index) {
            selectedIndex = index;
            if (index >= 0 && index < getItemCount()) {
                setSelected(getEntry(index));
            } else {
                setSelected(null);
            }
        }

        @Override
        public void setSelected(@org.jetbrains.annotations.Nullable StationEntry entry) {
            super.setSelected(entry);
            if (entry != null) {
                selectedIndex = entry.index - 1;
            } else {
                selectedIndex = -1;
            }
            parent.updateButtonStates();
        }

        @Override
        public int getRowWidth() {
            return width - 10;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.x0 + this.width - 6;
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {
            // 无障碍功能支持
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            // 绘制背景
            guiGraphics.fill(this.x0, this.y0, this.x0 + this.width, this.y0 + this.height, 0x88000000);
            // 绘制边框
            guiGraphics.renderOutline(this.x0, this.y0, this.width, this.height, 0xFF666666);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        }

        /**
         * 车站列表条目
         */
        private class StationEntry extends AbstractSelectionList.Entry<StationEntry> {
            private final String stationCode;
            private final int index;

            public StationEntry(String stationCode, int index) {
                this.stationCode = stationCode;
                this.index = index;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int entryIndex, int top, int left, int width, int height,
                              int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
                // 背景色
                int backgroundColor = isMouseOver ? 0x44666666 : (entryIndex % 2 == 0 ? 0x44333333 : 0x44222222);
                if (entryIndex == selectedIndex) {
                    backgroundColor = 0x440099FF; // 选中颜色
                }

                guiGraphics.fill(left, top, left + width, top + height, backgroundColor);

                // 车站信息
                Station station = NetworkManager.getStation(stationCode);
                String displayText = index + ". ";
                if (station != null) {
                    displayText += station.getName() + " (" + stationCode + ")";
                } else {
                    displayText += stationCode + " (未找到)";
                }

                guiGraphics.drawString(Minecraft.getInstance().font, displayText,
                        left + 5, top + 6, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) { // 左键点击
                    StationList.this.setSelected(this);
                    return true;
                }
                return false;
            }

            public Component getNarration() {
                return Component.literal("车站 " + stationCode);
            }
        }
    }

    // ==================== 车站选择器组件 ====================

    /**
     * 可用车站选择器组件
     */
    private static class StationSelector extends AbstractSelectionList<StationSelector.StationEntry> {
        private final LineEditScreen parent;
        private List<Station> stations = new ArrayList<>();
        private List<String> selectedStationCodes = new ArrayList<>();
        private int selectedIndex = -1;

        public StationSelector(LineEditScreen parent, int x, int y, int width, int height) {
            super(Minecraft.getInstance(), width, height, y, y + height, 20);
            this.parent = parent;
            this.setLeftPos(x);
        }

        public void refreshStations(List<Station> stations, List<String> selectedStationCodes) {
            this.clearEntries();
            this.stations = new ArrayList<>(stations);
            this.selectedStationCodes = new ArrayList<>(selectedStationCodes);

            for (Station station : stations) {
                this.addEntry(new StationEntry(station));
            }

            // 重置选中状态
            selectedIndex = -1;
        }

        public String getSelectedStationCode() {
            if (selectedIndex >= 0 && selectedIndex < stations.size()) {
                return stations.get(selectedIndex).getCode();
            }
            return null;
        }

        @Override
        public void setSelected(@org.jetbrains.annotations.Nullable StationEntry entry) {
            super.setSelected(entry);
            if (entry != null) {
                selectedIndex = stations.indexOf(entry.station);
            } else {
                selectedIndex = -1;
            }
        }

        @Override
        public int getRowWidth() {
            return width - 10;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.x0 + this.width - 6;
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {
            // 无障碍功能支持
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            // 绘制背景
            guiGraphics.fill(this.x0, this.y0, this.x0 + this.width, this.y0 + this.height, 0x88000000);
            // 绘制边框
            guiGraphics.renderOutline(this.x0, this.y0, this.width, this.height, 0xFF666666);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        }

        /**
         * 车站选择器条目
         */
        private class StationEntry extends AbstractSelectionList.Entry<StationEntry> {
            private final Station station;

            public StationEntry(Station station) {
                this.station = station;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int entryIndex, int top, int left, int width, int height,
                              int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
                // 检查是否已选择
                boolean alreadySelected = selectedStationCodes.contains(station.getCode());

                // 背景色
                int backgroundColor;
                if (alreadySelected) {
                    backgroundColor = 0x44444444; // 已选择，灰色
                } else if (isMouseOver) {
                    backgroundColor = 0x44666666; // 鼠标悬停
                } else {
                    backgroundColor = entryIndex % 2 == 0 ? 0x44333333 : 0x44222222;
                }

                if (entryIndex == selectedIndex) {
                    backgroundColor = 0x440099FF; // 选中颜色
                }

                guiGraphics.fill(left, top, left + width, top + height, backgroundColor);

                // 车站信息
                String displayText = station.getName() + " (" + station.getCode() + ")";
                int textColor = alreadySelected ? 0x888888 : 0xFFFFFF;

                guiGraphics.drawString(Minecraft.getInstance().font, displayText,
                        left + 5, top + 6, textColor);

                // 如果已选择，显示提示
                if (alreadySelected) {
                    guiGraphics.drawString(Minecraft.getInstance().font, "已选择",
                            left + width - 45, top + 6, 0x888888);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) { // 左键点击
                    StationSelector.this.setSelected(this);
                    return true;
                }
                return false;
            }

            public Component getNarration() {
                return Component.literal("车站 " + station.getName());
            }
        }
    }
}