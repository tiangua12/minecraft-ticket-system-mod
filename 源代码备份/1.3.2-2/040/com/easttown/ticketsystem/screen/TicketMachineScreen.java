package com.easttown.ticketsystem.screen;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.client.AdminStationList;
import com.easttown.ticketsystem.client.BaseStationList;
import com.easttown.ticketsystem.client.UserStationList;
import net.minecraft.core.BlockPos;
import com.easttown.ticketsystem.manager.CoinSystem;
import com.easttown.ticketsystem.manager.PriceCalculator;
import com.easttown.ticketsystem.network.NetworkHandler;
import com.easttown.ticketsystem.network.PrintTicketPacket;
import com.easttown.ticketsystem.network.SetStartStationPacket;
import com.easttown.ticketsystem.network.WithdrawCoinsPacket;
import com.easttown.ticketsystem.util.LanguageHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Map;

public class TicketMachineScreen extends AbstractContainerScreen<TicketMachineMenu>
        implements BaseStationList.StationListProvider {
    // GUI 尺寸常量
    public static final int ADMIN_IMAGE_WIDTH = 300;
    public static final int ADMIN_IMAGE_HEIGHT = 290;
    public static final int USER_IMAGE_WIDTH = 290;
    public static final int USER_IMAGE_HEIGHT = 270;

    // 列表位置常量
    public static final int ADMIN_LIST_X = 20;
    public static final int ADMIN_LIST_Y = 50;
    public static final int ADMIN_LIST_WIDTH = 120;
    public static final int ADMIN_LIST_HEIGHT = 120;

    public static final int USER_LIST_X = 20;
    public static final int USER_LIST_Y = 50;
    public static final int USER_LIST_WIDTH = 120;
    public static final int USER_LIST_HEIGHT = 120;

    // 搜索框位置
    public static final int SEARCH_X = 75;
    public static final int SEARCH_Y = 5;
    public static final int SEARCH_WIDTH = 150;
    public static final int SEARCH_HEIGHT = 20;

    // 按钮位置
    public static final int ADMIN_ADD_BUTTON_X = 180;
    public static final int ADMIN_ADD_BUTTON_Y = 20;
    public static final int ADMIN_SET_BUTTON_X = 180;
    public static final int ADMIN_SET_BUTTON_Y = 50;
    public static final int ADMIN_DELETE_BUTTON_X = 180;
    public static final int ADMIN_DELETE_BUTTON_Y = 80;

    public static final int USER_PRINT_BUTTON_X = 147;
    public static final int USER_PRINT_BUTTON_Y = 80;

    public static final int BUTTON_WIDTH = 100;
    public static final int BUTTON_HEIGHT = 20;

    // 信息显示位置
    public static final int INFO_X = 20;
    public static final int INFO_Y = 30;
    public static final int INFO_WIDTH = 155;
    public static final int INFO_HEIGHT = 10;

    public static final int PRICE_X = 150;
    public static final int PRICE_Y = 50;
    public static final int PRICE_WIDTH = 110;
    public static final int PRICE_HEIGHT = 20;

    // 硬币信息显示位置
    public static final int COINS_X = 150;
    public static final int COINS_Y = 75;
    public static final int COINS_WIDTH = 110;
    public static final int COINS_HEIGHT = 40;

    // 取款按钮位置
    public static final int WITHDRAW_BUTTON_X = 147;
    public static final int WITHDRAW_BUTTON_Y = 170;

    // 标签位置
    public static final int ADMIN_LABEL_X = 175;
    public static final int ADMIN_LABEL_Y = 5;
    public static final int ADMIN_LABEL_WIDTH = 110;
    public static final int ADMIN_LABEL_HEIGHT = 10;

    public static final int ADMIN_LIST_TITLE_X = 20;
    public static final int ADMIN_LIST_TITLE_Y = 30;
    public static final int ADMIN_LIST_TITLE_WIDTH = 120;
    public static final int ADMIN_LIST_TITLE_HEIGHT = 10;

    public static final int USER_LIST_TITLE_X = 17;
    public static final int USER_LIST_TITLE_Y = 30;
    public static final int USER_LIST_TITLE_WIDTH = 120;
    public static final int USER_LIST_TITLE_HEIGHT = 10;

    // 翻页按钮位置
    public static final int ADMIN_PREV_PAGE_BUTTON_X = 20;
    public static final int ADMIN_NEXT_PAGE_BUTTON_X = 140;
    public static final int ADMIN_PAGE_BUTTON_Y = 175;

    public static final int USER_PREV_PAGE_BUTTON_X = 20;
    public static final int USER_NEXT_PAGE_BUTTON_X = 240;
    public static final int USER_PAGE_BUTTON_Y = 5;

    public static final int PAGE_BUTTON_WIDTH = 40;
    public static final int PAGE_BUTTON_HEIGHT = 20;

    // 纹理资源
    private final ResourceLocation ADMIN_TEXTURE = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID,
            "textures/gui/admin_ticket_machine.png");
    private final ResourceLocation USER_TEXTURE = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID,
            "textures/gui/user_ticket_machine.png");
    // UI 组件
    private EditBox searchBox;
    private Button printButton, addButton, setStartButton, deleteButton;
    private Button prevPageButton, nextPageButton;
    private final AdminStationList adminStationList;
    private final UserStationList userStationList;
    private Button withdrawButton; // 取款按钮

    // 状态变量
    private String selectedStation = "";
    private int estimatedPrice = 0;
    private final BlockPos machinePos;
    protected boolean debugMode = false;

    // 当前界面尺寸
    private int currentWidth;
    private int currentHeight;

    // 用于跟踪起始站变化
    private String lastStartStation = "";

    public TicketMachineScreen(TicketMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.machinePos = menu.blockEntity.getBlockPos();

        // 初始化车站列表
        this.adminStationList = new AdminStationList(
                this,
                leftPos + ADMIN_LIST_X,
                topPos + ADMIN_LIST_Y,
                ADMIN_LIST_WIDTH,
                ADMIN_LIST_HEIGHT);

        this.userStationList = new UserStationList(
                this,
                leftPos + USER_LIST_X,
                topPos + USER_LIST_Y,
                USER_LIST_WIDTH,
                USER_LIST_HEIGHT);

        // 根据模式设置不同尺寸
        if (menu.isAdminMode()) {
            this.imageWidth = ADMIN_IMAGE_WIDTH;
            this.imageHeight = ADMIN_IMAGE_HEIGHT;
        } else {
            this.imageWidth = USER_IMAGE_WIDTH;
            this.imageHeight = USER_IMAGE_HEIGHT;
        }

        currentWidth = imageWidth;
        currentHeight = imageHeight;
    }

    @Override
    protected void init() {
        super.init();

        // 计算GUI居中位置
        leftPos = (width - currentWidth) / 2;
        topPos = (height - currentHeight) / 2;

        // 搜索框
        searchBox = new EditBox(font,
                leftPos + SEARCH_X, topPos + SEARCH_Y,
                SEARCH_WIDTH, SEARCH_HEIGHT,
                Component.translatable("ticketsystem.gui.search_station"));
        searchBox.setResponder(text -> refreshStations());
        addRenderableWidget(searchBox);

        // 管理员按钮
        addButton = Button.builder(Component.translatable("ticketsystem.gui.add_station"), button -> {
            BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
            Minecraft.getInstance().setScreen(new AddStationScreen(playerPos));
        })
                .bounds(leftPos + ADMIN_ADD_BUTTON_X, topPos + ADMIN_ADD_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(addButton);

        setStartButton = Button.builder(Component.translatable("ticketsystem.gui.set_start"), button -> {
            if (!selectedStation.isEmpty()) {
                NetworkHandler.sendToServer(new SetStartStationPacket(machinePos, selectedStation));
                selectedStation = "";
                refreshStations();
            } else {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("ticketsystem.command.station_not_selected"), true);
            }
        })
                .bounds(leftPos + ADMIN_SET_BUTTON_X, topPos + ADMIN_SET_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(setStartButton);

        deleteButton = Button.builder(Component.translatable("ticketsystem.gui.delete_station"), button -> {
            if (!selectedStation.isEmpty()) {
                Minecraft.getInstance().player.connection.sendCommand(
                        "ticketsystem deletestation " + selectedStation);
                selectedStation = "";
                refreshStations();
            } else {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("ticketsystem.command.station_not_selected"), true);
            }
        })
                .bounds(leftPos + ADMIN_DELETE_BUTTON_X, topPos + ADMIN_DELETE_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(deleteButton);

        // 用户按钮 - 打印车票按钮
        printButton = Button.builder(Component.translatable("ticketsystem.gui.buy_ticket"), button -> {
            if (!selectedStation.isEmpty()) {
                NetworkHandler.sendToServer(new PrintTicketPacket(machinePos, selectedStation));
            } else {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("ticketsystem.command.destination_not_selected"), true);
            }
        })
                .bounds(leftPos + USER_PRINT_BUTTON_X, topPos + USER_PRINT_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(printButton);

        // 翻页按钮
        boolean isAdminMode = menu.isAdminMode();
        prevPageButton = Button.builder(Component.literal("◀"), button -> {
            if (isAdminMode) {
                adminStationList.prevPage();
            } else {
                userStationList.prevPage();
            }
        })
                .bounds(
                        leftPos + (isAdminMode ? ADMIN_PREV_PAGE_BUTTON_X : USER_PREV_PAGE_BUTTON_X),
                        topPos + (isAdminMode ? ADMIN_PAGE_BUTTON_Y : USER_PAGE_BUTTON_Y),
                        PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT)
                .build();
        addRenderableWidget(prevPageButton);

        nextPageButton = Button.builder(Component.literal("▶"), button -> {
            if (isAdminMode) {
                adminStationList.nextPage();
            } else {
                userStationList.nextPage();
            }
        })
                .bounds(
                        leftPos + (isAdminMode ? ADMIN_NEXT_PAGE_BUTTON_X : USER_NEXT_PAGE_BUTTON_X),
                        topPos + (isAdminMode ? ADMIN_PAGE_BUTTON_Y : USER_PAGE_BUTTON_Y),
                        PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT)
                .build();
        addRenderableWidget(nextPageButton);

        // 添加车站列表组件
        addRenderableWidget(adminStationList);
        addRenderableWidget(userStationList);

        // 添加取款按钮
        withdrawButton = Button.builder(Component.translatable("ticketsystem.gui.withdraw_coins"), button -> {
            NetworkHandler.sendToServer(new WithdrawCoinsPacket(machinePos));
        })
                .bounds(leftPos + WITHDRAW_BUTTON_X, topPos + WITHDRAW_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(withdrawButton);

        // 更新组件可见性
        updateComponentVisibility();
        // 刷新车站数据
        refreshStations();

        // 初始化起始站跟踪
        lastStartStation = getStartStation();
    }

    // 刷新车站数据
    private void refreshStations() {
        String filter = searchBox.getValue();
        adminStationList.refreshStations(filter);
        userStationList.refreshStations(filter);
        updatePriceEstimate();
        updatePageButtons();
    }

    // 更新翻页按钮状态
    private void updatePageButtons() {
        if (menu.isAdminMode()) {
            prevPageButton.active = adminStationList.hasPrevPage();
            nextPageButton.active = adminStationList.hasNextPage();
        } else {
            prevPageButton.active = userStationList.hasPrevPage();
            nextPageButton.active = userStationList.hasNextPage();
        }
    }

    // 更新价格估算
    private void updatePriceEstimate() {
        String startStation = menu.blockEntity.getStartStation();
        if (startStation == null || startStation.isEmpty() ||
                selectedStation.isEmpty() || startStation.equals(selectedStation)) {
            estimatedPrice = 0;
        } else {
            estimatedPrice = PriceCalculator.calculatePrice(startStation, selectedStation);
        }
    }

    // 更新组件可见性
    private void updateComponentVisibility() {
        boolean adminMode = menu.isAdminMode();
        addButton.visible = adminMode;
        setStartButton.visible = adminMode;
        deleteButton.visible = adminMode;
        printButton.visible = !adminMode;
        withdrawButton.visible = adminMode; // 只显示给管理员

        adminStationList.setVisible(adminMode);
        userStationList.setVisible(!adminMode);

        prevPageButton.visible = true;
        nextPageButton.visible = true;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        // 检查起始站是否变化
        String currentStartStation = getStartStation();
        if (!currentStartStation.equals(lastStartStation)) {
            lastStartStation = currentStartStation;
            updatePriceEstimate();
            refreshStations();
        }

        // 根据模式选择不同纹理
        ResourceLocation texture = menu.isAdminMode() ? ADMIN_TEXTURE : USER_TEXTURE;

        // 渲染背景纹理
        if (texture != null) {
            try {
                guiGraphics.blit(texture,
                        leftPos, topPos,
                        0, 0,
                        currentWidth, currentHeight,
                        currentWidth, currentHeight);
            } catch (Exception e) {
                guiGraphics.fill(leftPos, topPos, leftPos + currentWidth, topPos + currentHeight, 0xFFE0F7FA);
            }
        } else {
            guiGraphics.fill(leftPos, topPos, leftPos + currentWidth, topPos + currentHeight, 0xFFE0F7FA);
        }

        // 获取并显示起始站信息
        String startStation = menu.blockEntity.getStartStation();
        String displayStart = (startStation == null || startStation.isEmpty())
                ? Component.translatable("ticketsystem.gui.not_set").getString()
                : startStation;

        // 绘制起始站信息背景 - 使用更明显的背景
        guiGraphics.fill(
                leftPos + INFO_X, topPos + INFO_Y,
                leftPos + INFO_X + INFO_WIDTH, topPos + INFO_Y + INFO_HEIGHT,
                0xAA000000); // 改为半透明黑色背景

        // 绘制起始站文本 - 使用白色文字
        guiGraphics.drawString(
                font,
                LanguageHelper.translate("gui.start_station", displayStart).getString(),
                leftPos + INFO_X + 5, topPos + INFO_Y + 2,
                0xFFFFFFFF, // 白色
                false);

        // 管理员模式特定渲染
        if (menu.isAdminMode()) {
            // 绘制管理员标签背景
            guiGraphics.fill(
                    leftPos + ADMIN_LABEL_X, topPos + ADMIN_LABEL_Y,
                    leftPos + ADMIN_LABEL_X + ADMIN_LABEL_WIDTH, topPos + ADMIN_LABEL_Y + ADMIN_LABEL_HEIGHT,
                    0xAA000000);

            // 绘制管理员模式文本
            guiGraphics.drawString(
                    font,
                    LanguageHelper.translate("gui.admin_mode").getString(),
                    leftPos + ADMIN_LABEL_X + 5, topPos + ADMIN_LABEL_Y + 2,
                    0xFFFF0000, // 红色
                    false);

            // 绘制车站列表标题背景
            guiGraphics.fill(
                    leftPos + ADMIN_LIST_TITLE_X, topPos + ADMIN_LIST_TITLE_Y,
                    leftPos + ADMIN_LIST_TITLE_X + ADMIN_LIST_TITLE_WIDTH,
                    topPos + ADMIN_LIST_TITLE_Y + ADMIN_LIST_TITLE_HEIGHT,
                    0xAA000000);

            // 绘制车站列表标题
            guiGraphics.drawString(
                    font,
                    LanguageHelper.translate("gui.all_stations").getString(),
                    leftPos + ADMIN_LIST_TITLE_X, topPos + ADMIN_LIST_TITLE_Y + 18,
                    0xFFFFFFFF,
                    false);

            // 绘制页码信息 - 管理员模式
            String pageInfo = LanguageHelper.translate("gui.page_indicator",
                    adminStationList.getCurrentPage() + 1,
                    adminStationList.getTotalPages()).getString();
            guiGraphics.drawString(
                    font,
                    pageInfo,
                    leftPos + ADMIN_PREV_PAGE_BUTTON_X + 20,
                    topPos + ADMIN_PAGE_BUTTON_Y + 6,
                    0x000000,
                    false);

            // 显示硬币存储信息
            int totalValue = menu.blockEntity.getTotalCopperValue();
            Map<String, Integer> storedCoins = menu.blockEntity.getStoredCoins();

            // 硬币存储背景框
            guiGraphics.fill(
                    leftPos + COINS_X, topPos + COINS_Y,
                    leftPos + COINS_X + COINS_WIDTH, topPos + COINS_Y + COINS_HEIGHT,
                    0xAAFFFF00);

            // 显示总价值
            guiGraphics.drawString(
                    font,
                    LanguageHelper.translate("gui.total_coins", totalValue).getString(),
                    leftPos + COINS_X + 5, topPos + COINS_Y + 5,
                    0x000000,
                    false);

            // 显示各种硬币数量
            int yOffset = 20;
            for (Map.Entry<String, Integer> entry : storedCoins.entrySet()) {
                if (entry.getValue() > 0) {
                    String coinName = CoinSystem.getCoinName(entry.getKey());
                    guiGraphics.drawString(
                            font,
                            LanguageHelper.translate("gui.coin_amount", coinName, entry.getValue()).getString(),
                            leftPos + COINS_X + 5, topPos + COINS_Y + yOffset,
                            0x000000,
                            false);
                    yOffset += 10;
                }
            }
        }
        // 用户模式特定渲染
        else {
            // 绘制目的地列表标题背景
            guiGraphics.fill(
                    leftPos + USER_LIST_TITLE_X, topPos + USER_LIST_TITLE_Y,
                    leftPos + USER_LIST_TITLE_X + USER_LIST_TITLE_WIDTH,
                    topPos + USER_LIST_TITLE_Y + USER_LIST_TITLE_HEIGHT,
                    0xAA000000);

            // 绘制目的地列表标题
            guiGraphics.drawString(
                    font,
                    LanguageHelper.translate("gui.select_destination").getString(),
                    leftPos + USER_LIST_TITLE_X, topPos + USER_LIST_TITLE_Y + 20,
                    0xFFFFFFFF,
                    false);

            // 绘制页码信息 - 用户模式
            String pageInfo = LanguageHelper.translate("gui.page_indicator",
                    userStationList.getCurrentPage() + 1,
                    userStationList.getTotalPages()).getString();
            guiGraphics.drawString(
                    font,
                    pageInfo,
                    leftPos + USER_PRINT_BUTTON_X,
                    topPos + USER_PRINT_BUTTON_Y + 30,
                    0x000000,
                    false);

            // 只在用户模式下显示价格估算
            if (estimatedPrice > 0) {
                // 价格背景框
                guiGraphics.fill(
                        leftPos + PRICE_X, topPos + PRICE_Y,
                        leftPos + PRICE_X + PRICE_WIDTH, topPos + PRICE_Y + PRICE_HEIGHT,
                        0xAA00AA00);

                // 价格文本
                guiGraphics.drawString(
                        font,
                        LanguageHelper.translate("gui.estimated_price", estimatedPrice).getString(),
                        leftPos + PRICE_X + 5, topPos + PRICE_Y + 6,
                        0xFFFFFFFF,
                        false);
            } else if (!selectedStation.isEmpty()) {
                // 警告背景框
                guiGraphics.fill(
                        leftPos + PRICE_X, topPos + PRICE_Y,
                        leftPos + PRICE_X + PRICE_WIDTH, topPos + PRICE_Y + PRICE_HEIGHT,
                        0xAAFF0000);

                // 警告文本
                guiGraphics.drawString(
                        font,
                        LanguageHelper.translate("gui.same_station_warning").getString(),
                        leftPos + PRICE_X + 5, topPos + PRICE_Y + 6,
                        0xFFFFFFFF,
                        false);
            }
        }
    }

    // 设置选中的车站
    public void setSelectedStation(String station) {
        this.selectedStation = station;
        updatePriceEstimate();
    }

    // 获取选中的车站
    public String getSelectedStation() {
        return selectedStation;
    }

    // 获取起始站
    public String getStartStation() {
        return menu.blockEntity.getStartStation();
    }

    // 获取搜索文本
    public String getSearchText() {
        return searchBox.getValue();
    }

    // 获取列表位置和尺寸
    public int getAdminListX() {
        return ADMIN_LIST_X;
    }

    public int getAdminListY() {
        return ADMIN_LIST_Y;
    }

    public int getAdminListWidth() {
        return ADMIN_LIST_WIDTH;
    }

    public int getAdminListHeight() {
        return ADMIN_LIST_HEIGHT;
    }

    public int getUserListX() {
        return USER_LIST_X;
    }

    public int getUserListY() {
        return USER_LIST_Y;
    }

    public int getUserListWidth() {
        return USER_LIST_WIDTH;
    }

    public int getUserListHeight() {
        return USER_LIST_HEIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 更新列表位置和尺寸
        adminStationList.setPosition(
                leftPos + getAdminListX(),
                topPos + getAdminListY(),
                getAdminListWidth(),
                getAdminListHeight());

        userStationList.setPosition(
                leftPos + getUserListX(),
                topPos + getUserListY(),
                getUserListWidth(),
                getUserListHeight());

        // 更新翻页按钮状态
        updatePageButtons();

        // 渲染背景
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // 渲染工具提示
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
