package com.easttown.ticketsystem.screen.terminal.menu;

import com.easttown.ticketsystem.block.ReissueMachineBlockEntity;
import com.easttown.ticketsystem.init.MenuInit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ReissueMachineMainMenu extends AbstractContainerMenu {

    // 槽位定义
    public static final int TICKET_INPUT_SLOT = 0;
    public static final int COIN_OUTPUT_SLOT_START = 1;
    public static final int COIN_OUTPUT_SLOT_END = 9;
    // 退票时车票会被销毁，不需要输出槽

    // 槽位位置配置 - 基于贴图自动对准
    // 这些坐标对应贴图中每个槽位的左上角位置

    // 车票输入槽位置 - 基于18像素间距调整
    public static final int TICKET_INPUT_X = 8;
    public static final int TICKET_INPUT_Y = 22; // 第一排槽口位置

    // 硬币输出槽位置 - 水平排列，基于18像素间距
    public static final int COIN_OUTPUT_START_X = 8;
    public static final int COIN_OUTPUT_START_Y = 74; // 第6排槽口位置
    public static final int COIN_OUTPUT_SPACING = 18;

    // 玩家物品栏位置 - 3行9列网格，基于18像素间距
    public static final int PLAYER_INVENTORY_START_X = 8;
    public static final int PLAYER_INVENTORY_START_Y = 115; // 第8排槽口位置
    private static final int PLAYER_INVENTORY_SPACING_X = 18;
    private static final int PLAYER_INVENTORY_SPACING_Y = 18;

    // 快捷栏位置 - 水平排列，基于18像素间距
    public static final int HOTBAR_START_X = 8;
    public static final int HOTBAR_START_Y = 172; // 第12排槽口位置
    private static final int HOTBAR_SPACING = 18;

    private final ReissueMachineBlockEntity blockEntity;
    private final BlockPos pos;

    public ReissueMachineMainMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public ReissueMachineMainMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(MenuInit.REISSUE_MACHINE_MAIN_MENU.get(), containerId);
        this.blockEntity = (ReissueMachineBlockEntity) blockEntity;
        this.pos = blockEntity.getBlockPos();

        // 添加退票机器槽位
        addMachineSlots();

        // 添加玩家物品栏
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addMachineSlots() {
        // 车票输入槽 - 只能放入未使用的车票，但可以取出
        this.addSlot(new Slot(blockEntity, TICKET_INPUT_SLOT, TICKET_INPUT_X, TICKET_INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return blockEntity.canPlaceItem(TICKET_INPUT_SLOT, stack);
            }

            @Override
            public boolean mayPickup(Player player) {
                return true; // 车票输入槽允许玩家提取
            }
        });

        // 9个硬币输出槽 - 只能由机器放入
        for (int i = COIN_OUTPUT_SLOT_START; i <= COIN_OUTPUT_SLOT_END; i++) {
            int x = COIN_OUTPUT_START_X + (i - COIN_OUTPUT_SLOT_START) * COIN_OUTPUT_SPACING;
            this.addSlot(new Slot(blockEntity, i, x, COIN_OUTPUT_START_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false; // 硬币输出槽不允许玩家放入
                }
            });
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                int slotIndex = j + i * 9 + 9;
                int x = PLAYER_INVENTORY_START_X + j * PLAYER_INVENTORY_SPACING_X;
                int y = PLAYER_INVENTORY_START_Y + i * PLAYER_INVENTORY_SPACING_Y;
                this.addSlot(new Slot(playerInventory, slotIndex, x, y));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            int x = HOTBAR_START_X + i * HOTBAR_SPACING;
            this.addSlot(new Slot(playerInventory, i, x, HOTBAR_START_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 禁用快速移动功能
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity != null &&
                !this.blockEntity.isRemoved() &&
                player.distanceToSqr(
                                this.pos.getX() + 0.5,
                                this.pos.getY() + 0.5,
                                this.pos.getZ() + 0.5) <= 64;
    }

    public ReissueMachineBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getPos() {
        return pos;
    }

}