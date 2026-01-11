package com.easttown.ticketsystem.screen.terminal;

import com.easttown.ticketsystem.TicketSystemMod;
import com.easttown.ticketsystem.screen.terminal.menu.ReissueMachineAdminMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ReissueMachineAdminScreen extends AbstractContainerScreen<ReissueMachineAdminMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID, "textures/gui/admin_coin_storage.png");
    private static final ResourceLocation SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID, "textures/gui/gui_slots/slot.png");
    private static final ResourceLocation CREATE_INVENTORY_9X6 = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID, "textures/gui/gui_slots/create_inventory_9x6.png");
    private static final ResourceLocation CREATE_INVENTORY_9X3 = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID, "textures/gui/gui_slots/create_inventory_9x3.png");
    private static final ResourceLocation CREATE_INVENTORY_9X1 = ResourceLocation.fromNamespaceAndPath(TicketSystemMod.MODID, "textures/gui/gui_slots/create_inventory_9x1.png");

    // GUI尺寸
    public static final int IMAGE_WIDTH = 176;
    public static final int IMAGE_HEIGHT = 256; // 增加高度以适应新的物品栏位置

    public ReissueMachineAdminScreen(ReissueMachineAdminMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 管理员界面不需要额外按钮
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = this.leftPos;
        int y = this.topPos;

        // 绘制背景
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 渲染玩家物品栏和工具提示
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      /*  RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);*/

        int x = this.leftPos;
        int y = this.topPos;

        // 绘制背景
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 绘制槽位背景
        renderSlotBackgrounds(guiGraphics, x, y);
    }

    // 绘制槽位背景
    private void renderSlotBackgrounds(GuiGraphics guiGraphics, int x, int y) {
        // 对于 18×18 纹理，明确指定纹理尺寸
        final int SLOT_SIZE = 18;
        final int TEXTURE_SIZE = 18; // 因为你的纹理就是 18×18

        // 硬币存储槽 (6x9) - 使用9×6图片一次性渲染
        guiGraphics.blit(CREATE_INVENTORY_9X6,
                x + ReissueMachineAdminMenu.COIN_STORAGE_START_X - 1,
                y + ReissueMachineAdminMenu.COIN_STORAGE_START_Y - 1,
                0, 0,
                9 * SLOT_SIZE, 6 * SLOT_SIZE,
                9 * TEXTURE_SIZE, 6 * TEXTURE_SIZE);

        // 玩家物品栏 (3行9列) - 使用9×3图片一次性渲染
        guiGraphics.blit(CREATE_INVENTORY_9X3,
                x + ReissueMachineAdminMenu.PLAYER_INVENTORY_X - 1,
                y + ReissueMachineAdminMenu.PLAYER_INVENTORY_Y - 1,
                0, 0,
                9 * SLOT_SIZE, 3 * SLOT_SIZE,
                9 * TEXTURE_SIZE, 3 * TEXTURE_SIZE);

        // 快捷栏 (9个) - 使用9×1图片一次性渲染
        guiGraphics.blit(CREATE_INVENTORY_9X1,
                x + ReissueMachineAdminMenu.HOTBAR_X - 1,
                y + ReissueMachineAdminMenu.HOTBAR_Y - 1,
                0, 0,
                9 * SLOT_SIZE, SLOT_SIZE,
                9 * TEXTURE_SIZE, TEXTURE_SIZE);
    }
}