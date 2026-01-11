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

public class ReissueMachineAdminMenu extends AbstractContainerMenu {

    // 槽位定义 - 硬币存储槽
    public static final int COIN_STORAGE_START = 10; // 与BlockEntity中的硬币存储槽索引对齐
    public static final int COIN_STORAGE_END = 63; // 6x9 = 54个槽位，索引10-63

    // 槽位位置
    public static final int COIN_STORAGE_START_X = 8;
    public static final int COIN_STORAGE_START_Y = 18;
    public static final int COIN_STORAGE_SPACING_X = 18;
    public static final int COIN_STORAGE_SPACING_Y = 18;

    // 玩家物品栏位置 - 重新调整以避免与硬币存储槽重叠
    public static final int PLAYER_INVENTORY_X = 8;
    public static final int PLAYER_INVENTORY_Y = 180; // 从140调整到180
    public static final int HOTBAR_X = 8;
    public static final int HOTBAR_Y = 238; // 从198调整到238

    private final ReissueMachineBlockEntity blockEntity;
    private final BlockPos pos;

    public ReissueMachineAdminMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    public ReissueMachineAdminMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(MenuInit.REISSUE_MACHINE_ADMIN_MENU.get(), containerId);
        this.blockEntity = (ReissueMachineBlockEntity) blockEntity;
        this.pos = blockEntity.getBlockPos();

        // 添加硬币存储槽位
        addCoinStorageSlots(playerInventory);

        // 添加玩家物品栏
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void addCoinStorageSlots(Inventory playerInventory) {
        // 6x9的硬币存储槽位 - 这些槽位连接到退票机器的硬币存储系统
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = COIN_STORAGE_START + col + row * 9;
                int x = COIN_STORAGE_START_X + col * COIN_STORAGE_SPACING_X;
                int y = COIN_STORAGE_START_Y + row * COIN_STORAGE_SPACING_Y;

                this.addSlot(new Slot(blockEntity, slotIndex, x, y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        // 只允许放入硬币
                        return isCoin(stack);
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return true; // 允许玩家取出硬币
                    }

                    private boolean isCoin(ItemStack stack) {
                        String itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                        return itemId.contains("coin");
                    }
                });
            }
        }
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
        // 禁用快速移动功能
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.blockEntity != null &&
                !this.blockEntity.isRemoved() &&
                this.blockEntity.isAdmin(player) && // 检查是否是管理员
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