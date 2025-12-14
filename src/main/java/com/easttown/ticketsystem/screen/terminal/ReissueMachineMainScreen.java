package com.easttown.ticketsystem.screen.terminal;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.screen.terminal.menu.ReissueMachineMainMenu;
import com.easttown.ticketsystem.network.NetworkHandler;
import com.easttown.ticketsystem.network.WithdrawCoinsByAmountPacket;
import com.easttown.ticketsystem.util.LanguageHelper;
import com.easttown.ticketsystem.manager.CoinSystem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.Map;

public class ReissueMachineMainScreen extends AbstractContainerScreen<ReissueMachineMainMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID, "textures/gui/reissue_machine_gui.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID, "textures/gui/gui_slots/slot.png");
    private static final ResourceLocation SLOT_9X6_TEXTURE = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID, "textures/gui/gui_slots/create_inventory_9x6.png");
    private static final ResourceLocation SLOT_3X6_TEXTURE = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID, "textures/gui/gui_slots/create_inventory_9x3.png");
    private static final ResourceLocation SLOT_1X6_TEXTURE = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID, "textures/gui/gui_slots/create_inventory_9x1.png");

    // GUI尺寸
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 196; // 根据新的槽位布局调整高度

    // 退还硬币信息在主GUI中的位置
    public static final int REFUND_INFO_X = 8;
    public static final int REFUND_INFO_Y = 70;
    public static final int REFUND_INFO_LINE_SPACING = 10;

    // 数据刷新相关变量
    private long lastDataRefreshTime = 0;
    private static final long DATA_REFRESH_INTERVAL = 500; // 每500毫秒刷新一次数据
    private String lastTicketStatus = "false"; // 记录上次的车票状态

    // 按钮定义
    private Button withdrawCoinsButton;

    public ReissueMachineMainScreen(ReissueMachineMainMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        /*  this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;*/
    }

    @Override
    protected void init() {
        // 确保尺寸设置
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;

        super.init();

        // 双重保险：手动计算位置
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        // 设置物品栏标签位置
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 99; // 或者使用你原来的 114

        // 添加调试信息
        com.easttown.ticketsystem.util.DebugLogger.info(
                String.format("GUI初始化: 屏幕=%dx%d, GUI=%dx%d, 位置=(%d,%d)",
                        this.width, this.height, this.imageWidth, this.imageHeight,
                        this.leftPos, this.topPos)
        );

        // 初始化时强制刷新车票数据
        if (this.menu != null && this.menu.getBlockEntity() != null) {
            this.menu.getBlockEntity().setChanged();
            com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineMainScreen: 初始化完成，强制刷新车票数据");
        }

        // 添加退币按钮
        int buttonX = this.leftPos + 30;
        int buttonY = this.topPos + 21;
        int buttonWidth = 75;
        int buttonHeight = 18;

        this.withdrawCoinsButton = Button.builder(
                LanguageHelper.translate("reissue_machine.withdraw_coins"),
                button -> {
                    com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineMainScreen: 退票按钮被点击，位置: " + this.menu.getPos());
                    NetworkHandler.sendToServer(new WithdrawCoinsByAmountPacket(this.menu.getPos()));
                }
        )
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();

        this.addRenderableWidget(this.withdrawCoinsButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        /*    RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);*/

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 渲染玩家物品栏和工具提示
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(this.font, Component.translatable("ticketsystem.gui.reissue_machine_block.tips"), x + 8, y + 45, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, Component.translatable("ticketsystem.gui.reissue_machine_block.tips2"), x + 8, y + 45 + 8 + 3, 0xFFFFFF, true);
        // 在主GUI中直接渲染退还硬币信息
        renderRefundCoinsInfo(guiGraphics, x, y);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // 刷新车票详情数据
    private void refreshTicketData() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDataRefreshTime > DATA_REFRESH_INTERVAL) {
            lastDataRefreshTime = currentTime;

            // 强制刷新数据
            if (this.menu != null && this.menu.getBlockEntity() != null) {
                // 检查车票状态是否发生变化
                Map<String, String> ticketDetails = this.menu.getBlockEntity().getTicketDetails();
                if (ticketDetails != null) {
                    String currentTicketStatus = ticketDetails.get("hasTicket");
                    if (!currentTicketStatus.equals(lastTicketStatus)) {
                        // 车票状态发生变化，强制重新渲染
                        lastTicketStatus = currentTicketStatus;
                        com.easttown.ticketsystem.util.DebugLogger.info("ReissueMachineMainScreen: 车票状态变化，从 " + lastTicketStatus + " 变为 " + currentTicketStatus);
                    }
                }
            }
        }
    }

    // 渲染退还硬币信息 - 直接在主GUI中显示
    private void renderRefundCoinsInfo(GuiGraphics guiGraphics, int x, int y) {
        // 刷新车票数据
        refreshTicketData();

        // 获取车票详情 - 添加空值检查
        if (this.menu == null || this.menu.getBlockEntity() == null) {
            return;
        }

        Map<String, String> ticketDetails = this.menu.getBlockEntity().getTicketDetails();

        // 检查车票详情状态
        if (ticketDetails == null) {
            return;
        }

        if (ticketDetails.get("hasTicket").equals("false")) {
            // 没有车票时显示提示
            guiGraphics.drawString(this.font, LanguageHelper.translate("reissue_machine.insert_ticket").getString(),
                    x + REFUND_INFO_X, y + REFUND_INFO_Y, 0xFFFFFF, false);
            return;
        }

        // 计算预计退还硬币
        int ticketPrice = Integer.parseInt(ticketDetails.get("price"));
        long issueTime = 0;
        try {
            // 解析时间字符串，这里简化处理，实际需要从NBT获取
            issueTime = System.currentTimeMillis() - 3600000; // 假设1小时前
        } catch (Exception e) {
            issueTime = System.currentTimeMillis();
        }

        int refundAmount = calculateRefundAmount(ticketPrice, issueTime);

        // 直接在GUI中显示退还信息
        int textX = x + REFUND_INFO_X;
        int textY = y + REFUND_INFO_Y;

        // 显示原价
        guiGraphics.drawString(this.font,
                LanguageHelper.translate("reissue_machine.original_price").getString() + ": " + ticketPrice + " " + LanguageHelper.translate("currency.copper").getString(),
                textX, textY, 0xFFFFFF, false);

        // 显示退还金额
        guiGraphics.drawString(this.font,
                LanguageHelper.translate("reissue_machine.refund_amount").getString() + ": " + refundAmount + " " + LanguageHelper.translate("currency.copper").getString(),
                textX, textY + REFUND_INFO_LINE_SPACING, 0xFFFFFF, false);

        // 显示硬币组合
        Map<String, Integer> coinCombination = CoinSystem.calculateOptimalCoins(refundAmount);
        int line = 2;
        for (Map.Entry<String, Integer> entry : coinCombination.entrySet()) {
            if (entry.getValue() > 0) {
                String coinName = CoinSystem.getCoinName(entry.getKey());
                guiGraphics.drawString(this.font,
                        coinName + ": " + entry.getValue() + " 枚",
                        textX, textY + REFUND_INFO_LINE_SPACING * line, 0xFFFFFF, false);
                line++;
            }
        }
    }

    // 计算退款金额（简化版本）
    private int calculateRefundAmount(int originalPrice, long issueTime) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - issueTime;

        // 时间阈值（毫秒）
        long halfHour = 30 * 60 * 1000; // 30分钟
        long oneHour = 60 * 60 * 1000; // 1小时
        long twoHours = 2 * 60 * 60 * 1000; // 2小时
        long sixHours = 6 * 60 * 60 * 1000; // 6小时
        long oneDay = 24 * 60 * 60 * 1000; // 24小时

        // 阶梯式退款比例
        if (timeDiff <= halfHour) {
            // 30分钟内：全额退款
            return (int) (originalPrice * 1.0);
        } else if (timeDiff <= oneHour) {
            // 1小时内：75%退款
            return (int) (originalPrice * 0.75);
        } else if (timeDiff <= twoHours) {
            // 2小时内：50%退款
            return (int) (originalPrice * 0.5);
        } else if (timeDiff <= sixHours) {
            // 6小时内：25%退款
            return (int) (originalPrice * 0.25);
        } else if (timeDiff <= oneDay) {
            // 24小时内：10%退款
            return (int) (originalPrice * 0.1);
        } else {
            // 超过24小时：不可退款
            return 0;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        /*  RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);*/
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制背景
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

        // 绘制槽位背景 - 在背景层渲染，不会干扰原版交互
        renderSlotBackgrounds(guiGraphics, x, y);
    }

    // 绘制槽位背景
    private void renderSlotBackgrounds(GuiGraphics guiGraphics, int x, int y) {
        // 切换到槽位纹理
        //  RenderSystem.setShaderTexture(0, SLOT_TEXTURE);

        // 对于 18×18 纹理，明确指定纹理尺寸
        final int SLOT_SIZE = 18;
        final int TEXTURE_SIZE = 18; // 因为你的纹理就是 18×18

        // 车票输入槽
        guiGraphics.blit(SLOT_TEXTURE,
                x + ReissueMachineMainMenu.TICKET_INPUT_X - 1, y + ReissueMachineMainMenu.TICKET_INPUT_Y - 1,
                0, 0,
                SLOT_SIZE, SLOT_SIZE,
                TEXTURE_SIZE, TEXTURE_SIZE);

        // 硬币输出槽 (9个) - 使用9×6图片一次性渲染
        guiGraphics.blit(SLOT_1X6_TEXTURE,
                x + ReissueMachineMainMenu.COIN_OUTPUT_START_X - 1, y + ReissueMachineMainMenu.COIN_OUTPUT_START_Y - 1,
                0, 0,
                9 * SLOT_SIZE, SLOT_SIZE,
                9 * TEXTURE_SIZE, TEXTURE_SIZE);

        // 玩家物品栏 (3行9列) - 使用3×6图片一次性渲染
        guiGraphics.blit(SLOT_3X6_TEXTURE,
                x + ReissueMachineMainMenu.PLAYER_INVENTORY_START_X - 1, y + ReissueMachineMainMenu.PLAYER_INVENTORY_START_Y - 1,
                0, 0,
                9 * SLOT_SIZE, 3 * SLOT_SIZE,
                9 * TEXTURE_SIZE, 3 * TEXTURE_SIZE);

        // 快捷栏 (9个) - 使用1×6图片一次性渲染
        guiGraphics.blit(SLOT_1X6_TEXTURE,
                x + ReissueMachineMainMenu.HOTBAR_START_X - 1, y + ReissueMachineMainMenu.HOTBAR_START_Y - 1,
                0, 0,
                9 * SLOT_SIZE, SLOT_SIZE,
                9 * TEXTURE_SIZE, TEXTURE_SIZE);
    }

}