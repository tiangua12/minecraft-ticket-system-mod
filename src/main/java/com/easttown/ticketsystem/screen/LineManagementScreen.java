package com.easttown.ticketsystem.screen;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.data.Line;
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
 * 线路管理界面
 * 显示所有线路，支持创建、编辑、删除线路
 * 文档要求：简化线路显示，只在网页终端中导出图片
 */
public class LineManagementScreen extends Screen {
    // GUI组件
    private EditBox searchBox;
    private Button createButton;
    private Button editButton;
    private Button deleteButton;
    private Button backButton;
    private LineList lineList;

    // 线路数据
    private List<Line> allLines = new ArrayList<>();
    private Line selectedLine = null;

    // 位置常量
    private static final int SEARCH_X = 20;
    private static final int SEARCH_Y = 20;
    private static final int SEARCH_WIDTH = 150;
    private static final int SEARCH_HEIGHT = 20;

    private static final int LIST_X = 20;
    private static final int LIST_Y = 50;
    private static final int LIST_WIDTH = 200;
    private static final int LIST_HEIGHT = 150;

    private static final int BUTTON_X = 230;
    private static final int BUTTON_Y_START = 50;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 25;

    public LineManagementScreen() {
        super(LanguageHelper.translate("gui.line_management.title"));
    }

    @Override
    protected void init() {
        super.init();

        // 确保数据管理器初始化
        NetworkManager.initialize();

        // 搜索框
        searchBox = new EditBox(font,
                LIST_X, SEARCH_Y,
                SEARCH_WIDTH, SEARCH_HEIGHT,
                Component.translatable("gui.search_lines"));
        searchBox.setResponder(text -> refreshLines());
        addRenderableWidget(searchBox);

        // 线路列表
        lineList = new LineList(
                this,
                LIST_X, LIST_Y,
                LIST_WIDTH, LIST_HEIGHT);
        addRenderableWidget(lineList);

        // 创建按钮
        createButton = Button.builder(
                LanguageHelper.translate("gui.create_line"),
                button -> onCreateLine()
        ).bounds(BUTTON_X, BUTTON_Y_START, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(createButton);

        // 编辑按钮
        editButton = Button.builder(
                LanguageHelper.translate("gui.edit_line"),
                button -> onEditLine()
        ).bounds(BUTTON_X, BUTTON_Y_START + BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(editButton);
        editButton.active = false; // 初始不可用

        // 删除按钮
        deleteButton = Button.builder(
                LanguageHelper.translate("gui.delete_line"),
                button -> onDeleteLine()
        ).bounds(BUTTON_X, BUTTON_Y_START + BUTTON_SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(deleteButton);
        deleteButton.active = false; // 初始不可用

        // 返回按钮
        backButton = Button.builder(
                LanguageHelper.translate("gui.back"),
                button -> onBack()
        ).bounds(BUTTON_X, BUTTON_Y_START + BUTTON_SPACING * 4, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(backButton);

        // 刷新线路数据
        refreshLines();
    }

    /**
     * 刷新线路列表
     */
    private void refreshLines() {
        String filter = searchBox.getValue().toLowerCase();
        allLines.clear();

        // 从NetworkManager获取所有线路
        for (Line line : NetworkManager.getAllLines()) {
            if (filter.isEmpty() ||
                    line.getId().toLowerCase().contains(filter) ||
                    line.getName().toLowerCase().contains(filter)) {
                allLines.add(line);
            }
        }

        // 更新列表
        lineList.refreshLines(allLines);

        // 更新按钮状态
        updateButtonStates();
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = selectedLine != null;
        editButton.active = hasSelection;
        deleteButton.active = hasSelection;
    }

    /**
     * 创建新线路
     */
    private void onCreateLine() {
        // 打开创建线路界面
        Minecraft.getInstance().setScreen(new LineEditScreen(null));
    }

    /**
     * 编辑选中线路
     */
    private void onEditLine() {
        if (selectedLine != null) {
            Minecraft.getInstance().setScreen(new LineEditScreen(selectedLine));
        }
    }

    /**
     * 删除选中线路
     */
    private void onDeleteLine() {
        if (selectedLine != null) {
            // 确认对话框
            Minecraft.getInstance().setScreen(new ConfirmScreen(
                    this::onConfirmDelete,
                    LanguageHelper.translate("gui.confirm_delete_line"),
                    LanguageHelper.translate("gui.confirm_delete_line_message", selectedLine.getName())
            ));
        }
    }

    /**
     * 确认删除回调
     */
    private void onConfirmDelete(boolean confirmed) {
        if (confirmed && selectedLine != null) {
            // 删除线路
            boolean success = LineManager.removeLine(selectedLine.getId());
            if (success) {
                Minecraft.getInstance().player.displayClientMessage(
                        LanguageHelper.translate("gui.line_deleted", selectedLine.getName()),
                        true);
                selectedLine = null;
                refreshLines();
            } else {
                Minecraft.getInstance().player.displayClientMessage(
                        LanguageHelper.translate("gui.line_delete_failed"),
                        true);
            }
        }

        // 返回到线路管理界面
        Minecraft.getInstance().setScreen(this);
    }

    /**
     * 返回上一界面
     */
    private void onBack() {
        Minecraft.getInstance().setScreen(null);
    }

    /**
     * 设置选中线路
     */
    public void setSelectedLine(Line line) {
        this.selectedLine = line;
        updateButtonStates();
    }

    /**
     * 获取选中线路
     */
    public Line getSelectedLine() {
        return selectedLine;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 渲染背景
        renderBackground(guiGraphics);

        // 渲染标题
        guiGraphics.drawString(font, title, LIST_X, 10, 0xFFFFFF);

        // 渲染选中线路信息
        if (selectedLine != null) {
            int infoX = BUTTON_X;
            int infoY = BUTTON_Y_START + BUTTON_SPACING * 3 + 10;

            guiGraphics.drawString(font,
                    LanguageHelper.translate("gui.selected_line") + ": " + selectedLine.getName(),
                    infoX, infoY, 0xFFFFFF);

            guiGraphics.drawString(font,
                    LanguageHelper.translate("gui.station_count") + ": " + selectedLine.getStationCount(),
                    infoX, infoY + 12, 0xCCCCCC);

            guiGraphics.drawString(font,
                    LanguageHelper.translate("gui.line_color") + ": " + selectedLine.getColor(),
                    infoX, infoY + 24, 0xCCCCCC);
        }

        // 渲染其他组件
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    // ==================== 线路列表组件 ====================

    /**
     * 线路列表组件
     */
    private static class LineList extends AbstractSelectionList<LineList.LineEntry> {
        private final LineManagementScreen parent;
        private List<Line> lines = new ArrayList<>();

        public LineList(LineManagementScreen parent, int x, int y, int width, int height) {
            super(Minecraft.getInstance(), width, height, y, y + height, 20);
            this.parent = parent;
            this.setLeftPos(x);
        }

        /**
         * 刷新线路列表
         */
        public void refreshLines(List<Line> lines) {
            this.clearEntries();
            this.lines = new ArrayList<>(lines);

            for (Line line : lines) {
                this.addEntry(new LineEntry(line));
            }
        }

        @Override
        public void setSelected(@org.jetbrains.annotations.Nullable LineEntry entry) {
            super.setSelected(entry);
            if (entry != null) {
                parent.setSelectedLine(entry.line);
            }
        }

        @Override
        public int getRowWidth() {
            return width - 10; // 留出滚动条空间
        }

        @Override
        protected int getScrollbarPosition() {
            return this.x0 + this.width - 6;
        }


        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            // 绘制背景
            guiGraphics.fill(this.x0, this.y0, this.x0 + this.width, this.y0 + this.height, 0x88000000);

            // 绘制边框
            guiGraphics.renderOutline(this.x0, this.y0, this.width, this.height, 0xFF666666);

            // 渲染条目
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {
            // 无障碍功能支持
        }

        /**
         * 线路列表条目
         */
        private class LineEntry extends AbstractSelectionList.Entry<LineEntry> {
            private final Line line;

            public LineEntry(Line line) {
                this.line = line;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                              int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
                // 背景色
                int backgroundColor = isMouseOver ? 0x44666666 : (index % 2 == 0 ? 0x44333333 : 0x44222222);
                if (line.equals(parent.getSelectedLine())) {
                    backgroundColor = 0x440099FF; // 选中颜色
                }

                guiGraphics.fill(left, top, left + width, top + height, backgroundColor);

                // 线路信息
                int textY = top + 6;

                // 线路ID和名称
                String displayText = line.getId() + " - " + line.getName();
                guiGraphics.drawString(Minecraft.getInstance().font, displayText,
                        left + 5, textY, 0xFFFFFF);

                // 车站数量和颜色
                String details = line.getStationCount() + "站 | " + line.getColor();
                guiGraphics.drawString(Minecraft.getInstance().font, details,
                        left + 5, textY + 10, 0xCCCCCC);

                // 分隔线
                guiGraphics.hLine(left, left + width - 1, top + height - 1, 0x44FFFFFF);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) { // 左键点击
                    LineList.this.setSelected(this);
                    return true;
                }
                return false;
            }

            public Component getNarration() {
                return Component.literal("线路 " + line.getName());
            }
        }
    }

    // ==================== 确认对话框 ====================

    /**
     * 简单确认对话框
     */
    private static class ConfirmScreen extends Screen {
        private final java.util.function.Consumer<Boolean> callback;
        private final Component title;
        private final Component message;

        public ConfirmScreen(java.util.function.Consumer<Boolean> callback, Component title, Component message) {
            super(title);
            this.callback = callback;
            this.title = title;
            this.message = message;
        }

        @Override
        protected void init() {
            int centerX = width / 2 - 50;
            int centerY = height / 2;

            // 确认按钮
            addRenderableWidget(Button.builder(Component.literal("确认"), button -> {
                callback.accept(true);
            }).bounds(centerX - 60, centerY + 20, 50, 20).build());

            // 取消按钮
            addRenderableWidget(Button.builder(Component.literal("取消"), button -> {
                callback.accept(false);
            }).bounds(centerX + 10, centerY + 20, 50, 20).build());
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            renderBackground(guiGraphics);

            // 标题
            guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 30, 0xFFFFFF);

            // 消息
            guiGraphics.drawCenteredString(font, message, width / 2, height / 2 - 10, 0xCCCCCC);

            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        }

        @Override
        public void onClose() {
            // 如果直接关闭，视为取消
            callback.accept(false);
            super.onClose();
        }
    }
}