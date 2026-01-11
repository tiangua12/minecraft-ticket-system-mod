package com.easttown.ticketsystem.screen;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.data.Station;
import com.easttown.ticketsystem.manager.NetworkManager;
import com.easttown.ticketsystem.util.LanguageHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 车站管理屏幕
 * 显示所有车站列表，允许添加、删除、编辑车站
 */
@OnlyIn(Dist.CLIENT)
public class StationManagementScreen extends Screen {
    // 组件
    private EditBox searchBox;
    private Button addButton;
    private Button backButton;
    private Button refreshButton;

    // 动态操作按钮（每行编辑和删除按钮）
    private final List<Button> editButtons = new ArrayList<>();
    private final List<Button> deleteButtons = new ArrayList<>();
    // 按钮索引到车站编码的映射
    private final String[] editButtonStationCodes = new String[ITEMS_PER_PAGE];
    private final String[] deleteButtonStationCodes = new String[ITEMS_PER_PAGE];

    // 数据
    private List<Station> allStations = new ArrayList<>();
    private List<Station> filteredStations = new ArrayList<>();

    // 显示区域
    private int scrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 10;
    private static final int ITEM_HEIGHT = 20;

    public StationManagementScreen() {
        super(Component.literal("车站管理"));
    }

    @Override
    protected void init() {
        super.init();

        // 初始化网络管理器
        NetworkManager.initialize();

        // 加载车站数据
        loadStations();

        // 搜索框
        searchBox = new EditBox(
            this.font,
            this.width / 2 - 150, 40, 250, 20,
            Component.literal("搜索车站...")
        );
        searchBox.setResponder(text -> filterStations());
        this.addRenderableWidget(searchBox);

        // 刷新按钮
        refreshButton = Button.builder(
            Component.literal("刷新"),
            button -> {
                loadStations();
                filterStations();
            }
        )
        .pos(this.width / 2 + 110, 40)
        .size(40, 20)
        .build();
        this.addRenderableWidget(refreshButton);

        // 添加车站按钮
        addButton = Button.builder(
            Component.literal("添加车站"),
            button -> {
                Minecraft minecraft = Minecraft.getInstance();
                net.minecraft.core.BlockPos playerPos = minecraft.player != null ?
                    minecraft.player.blockPosition() : net.minecraft.core.BlockPos.ZERO;
                minecraft.setScreen(new AddStationScreen(playerPos));
            }
        )
        .pos(this.width / 2 - 100, this.height - 40)
        .size(100, 20)
        .build();
        this.addRenderableWidget(addButton);

        // 返回按钮
        backButton = Button.builder(
            Component.literal("返回"),
            button -> {
                this.onClose();
            }
        )
        .pos(this.width / 2 + 10, this.height - 40)
        .size(100, 20)
        .build();
        this.addRenderableWidget(backButton);

        // 上下滚动按钮
        Button scrollUpButton = Button.builder(
            Component.literal("↑"),
            button -> {
                if (scrollOffset > 0) scrollOffset--;
                updateActionButtons();
            }
        )
        .pos(this.width - 30, 70)
        .size(20, 20)
        .build();
        this.addRenderableWidget(scrollUpButton);

        Button scrollDownButton = Button.builder(
            Component.literal("↓"),
            button -> {
                int maxScroll = Math.max(0, filteredStations.size() - ITEMS_PER_PAGE);
                if (scrollOffset < maxScroll) scrollOffset++;
                updateActionButtons();
            }
        )
        .pos(this.width - 30, this.height - 50)
        .size(20, 20)
        .build();
        this.addRenderableWidget(scrollDownButton);

        // 创建操作按钮（编辑和删除），最多ITEMS_PER_PAGE个
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            final int buttonIndex = i; // 用于lambda表达式的final变量

            // 编辑按钮
            Button editButton = Button.builder(
                Component.literal("编辑"),
                button -> {
                    // 统一的编辑回调，根据按钮索引查找车站
                    String stationCode = editButtonStationCodes[buttonIndex];
                    if (stationCode != null) {
                        TicketSystemMod.LOGGER.debug("Edit station: {}", stationCode);
                        // TODO: 打开编辑车站屏幕
                    }
                }
            )
            .pos(0, 0) // 初始位置，将在updateActionButtons中更新
            .size(40, 20)
            .build();
            editButton.visible = false;
            this.addRenderableWidget(editButton);
            editButtons.add(editButton);

            // 删除按钮
            Button deleteButton = Button.builder(
                Component.literal("删除"),
                button -> {
                    // 统一的删除回调，根据按钮索引查找车站
                    String stationCode = deleteButtonStationCodes[buttonIndex];
                    if (stationCode != null) {
                        // 确保NetworkManager已初始化
                        NetworkManager.initialize();
                        if (NetworkManager.removeStation(stationCode)) {
                            TicketSystemMod.LOGGER.info("Deleted station: {}", stationCode);
                            // 重新加载数据
                            loadStations();
                            filterStations();
                            updateActionButtons();
                        } else {
                            TicketSystemMod.LOGGER.error("Failed to delete station: {}", stationCode);
                        }
                    }
                }
            )
            .pos(0, 0)
            .size(40, 20)
            .build();
            deleteButton.visible = false;
            this.addRenderableWidget(deleteButton);
            deleteButtons.add(deleteButton);
        }

        // 初始更新操作按钮
        updateActionButtons();
    }

    /**
     * 加载车站数据
     */
    private void loadStations() {
        allStations.clear();
        try {
            allStations.addAll(NetworkManager.getAllStations());
            // 按编码排序
            allStations.sort(Comparator.comparing(Station::getCode));
            TicketSystemMod.LOGGER.debug("Loaded {} stations", allStations.size());
        } catch (Exception e) {
            TicketSystemMod.LOGGER.error("Failed to load stations", e);
        }
        filteredStations = new ArrayList<>(allStations);
    }

    /**
     * 过滤车站列表
     */
    private void filterStations() {
        String query = searchBox.getValue().toLowerCase().trim();
        filteredStations.clear();

        if (query.isEmpty()) {
            filteredStations.addAll(allStations);
        } else {
            for (Station station : allStations) {
                if (station.getCode().toLowerCase().contains(query) ||
                    station.getName().toLowerCase().contains(query) ||
                    (station.getEnName() != null && station.getEnName().toLowerCase().contains(query))) {
                    filteredStations.add(station);
                }
            }
        }
        scrollOffset = 0; // 重置滚动
        updateActionButtons(); // 更新操作按钮
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景
        this.renderBackground(guiGraphics);

        // 渲染标题
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // 渲染搜索框标签
        guiGraphics.drawString(this.font, "搜索:", this.width / 2 - 170, 45, 0xFFFFFF, false);

        // 渲染车站列表标题
        int listTop = 70;
        guiGraphics.fill(this.width / 2 - 160, listTop - 5, this.width / 2 + 150, listTop + 15, 0x80000000);
        guiGraphics.drawString(this.font, "车站编码", this.width / 2 - 150, listTop, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "车站名称", this.width / 2 - 50, listTop, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "坐标 (X,Y,Z)", this.width / 2 + 50, listTop, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, "操作", this.width / 2 + 150, listTop, 0xFFFFFF, false);

        // 渲染车站列表
        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredStations.size());

        for (int i = startIndex; i < endIndex; i++) {
            Station station = filteredStations.get(i);
            int yPos = listTop + 20 + (i - startIndex) * ITEM_HEIGHT;

            // 交替行背景色
            if ((i - startIndex) % 2 == 0) {
                guiGraphics.fill(this.width / 2 - 160, yPos - 2, this.width / 2 + 150, yPos + ITEM_HEIGHT - 2, 0x40000000);
            }

            // 车站信息
            guiGraphics.drawString(this.font, station.getCode(), this.width / 2 - 150, yPos, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, station.getName(), this.width / 2 - 50, yPos, 0xFFFFFF, false);
            guiGraphics.drawString(this.font,
                String.format("%d, %d, %d", station.getX(), station.getY(), station.getZ()),
                this.width / 2 + 50, yPos, 0xFFFFFF, false);

            // 操作按钮现在使用真正的Button组件（在init中创建，在updateActionButtons中更新）
            // 这里不需要绘制文本按钮
        }

        // 显示车站计数
        String countText = String.format("车站: %d/%d", filteredStations.size(), allStations.size());
        guiGraphics.drawString(this.font, countText, this.width / 2 - 150, this.height - 60, 0xFFFFFF, false);

        // 渲染滚动位置
        if (filteredStations.size() > ITEMS_PER_PAGE) {
            String scrollText = String.format("第 %d-%d 项，共 %d 项",
                startIndex + 1, endIndex, filteredStations.size());
            guiGraphics.drawString(this.font, scrollText, this.width / 2 + 50, this.height - 60, 0xFFFFFF, false);
        }

        // 渲染父类组件（按钮等）
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        searchBox.tick();
    }

    /**
     * 更新操作按钮的位置和可见性
     */
    private void updateActionButtons() {
        int listTop = 70;
        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredStations.size());

        // 重置按钮映射
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            editButtonStationCodes[i] = null;
            deleteButtonStationCodes[i] = null;
        }

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            Button editButton = editButtons.get(i);
            Button deleteButton = deleteButtons.get(i);

            if (i < (endIndex - startIndex)) {
                // 显示按钮
                int stationIndex = startIndex + i;
                Station station = filteredStations.get(stationIndex);
                int yPos = listTop + 20 + i * ITEM_HEIGHT;
                int buttonWidth = 40;
                int editX = this.width / 2 + 150;
                int deleteX = editX + buttonWidth + 5;

                // 更新编辑按钮位置
                editButton.setX(editX);
                editButton.setY(yPos - 2);
                editButton.setWidth(buttonWidth);
                editButton.setHeight(ITEM_HEIGHT - 4);
                editButton.visible = true;
                editButton.active = true;
                editButtonStationCodes[i] = station.getCode();

                // 更新删除按钮位置
                deleteButton.setX(deleteX);
                deleteButton.setY(yPos - 2);
                deleteButton.setWidth(buttonWidth);
                deleteButton.setHeight(ITEM_HEIGHT - 4);
                deleteButton.visible = true;
                deleteButton.active = true;
                deleteButtonStationCodes[i] = station.getCode();
            } else {
                // 隐藏多余的按钮
                editButton.visible = false;
                deleteButton.visible = false;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        // 返回上级屏幕（如果有）或关闭
    }
}