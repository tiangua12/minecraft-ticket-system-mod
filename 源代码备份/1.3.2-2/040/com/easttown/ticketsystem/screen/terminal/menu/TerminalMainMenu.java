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

public class TerminalMainMenu extends AbstractContainerMenu {

    // 玩家物品栏位置
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_X = 8;
    private static final int HOTBAR_Y = 142;

    private final ReissueMachineBlockEntity blockEntity;
    private final BlockPos pos;

    public TerminalMainMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public TerminalMainMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(MenuInit.TERMINAL_MAIN_MENU.get(), containerId);
        this.blockEntity = (ReissueMachineBlockEntity) blockEntity;
        this.pos = blockEntity.getBlockPos();

        // 只添加玩家物品栏
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                int slotIndex = j + i * 9 + 9;
                this.addSlot(new Slot(playerInventory, slotIndex,
                PLAYER_INVENTORY_X + j * 18, PLAYER_INVENTORY_Y + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, HOTBAR_X + i * 18, HOTBAR_Y));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
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