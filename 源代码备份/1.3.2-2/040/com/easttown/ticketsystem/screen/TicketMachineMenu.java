package com.easttown.ticketsystem.screen;

import com.easttown.ticketsystem.block.TicketMachineBlockEntity;
import com.easttown.ticketsystem.init.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class TicketMachineMenu extends AbstractContainerMenu {
    // 槽位位置常量 - 标准间距 (18像素)
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 175; // 调整为更合适的位置
    private static final int HOTBAR_Y = PLAYER_INVENTORY_Y+16*3+6+4; // 标准箱子布局的快捷栏位置
    
    // 输出槽位置 (只用于用户模式)
    private static final int USER_OUTPUT_SLOT_X = 233;
    private static final int USER_OUTPUT_SLOT_Y = 60;
    
    public final TicketMachineBlockEntity blockEntity;
    private final ContainerData data;
    private final boolean adminMode;
    
    public TicketMachineMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, 
            (TicketMachineBlockEntity) inventory.player.level().getBlockEntity(extraData.readBlockPos()),
            extraData.readBoolean());
    }
    
    public TicketMachineMenu(int containerId, Inventory inventory, TicketMachineBlockEntity blockEntity, boolean adminMode) {
        super(MenuInit.TICKET_MACHINE_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.adminMode = adminMode;
        this.data = new ContainerData() {
            @Override
            public int get(int index) { return 0; }
            @Override
            public void set(int index, int value) {}
            @Override
            public int getCount() { return 0; }
        };
        
        // 添加玩家物品栏槽位 - 使用标准18像素间距
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                    inventory, 
                    col + row * 9 + 9, 
                    PLAYER_INVENTORY_X + col * 18, 
                    PLAYER_INVENTORY_Y + row * 18
                ));
            }
        }
        
        // 添加快捷栏槽位 - 使用标准18像素间距
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                inventory, 
                col, 
                PLAYER_INVENTORY_X + col * 18, 
                HOTBAR_Y
            ));
        }
        
        // 只在用户模式下添加输出槽
        if (!adminMode && blockEntity != null) {
            IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
            if (handler != null) {
                this.addSlot(new OutputSlot(handler, 0, USER_OUTPUT_SLOT_X, USER_OUTPUT_SLOT_Y));
            }
        }
        
        addDataSlots(data);
    }
    
    // 输出槽专用类
    private static class OutputSlot extends SlotItemHandler {
        public OutputSlot(IItemHandler itemHandler, int index, int x, int y) {
            super(itemHandler, index, x, y);
        }
        
        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && 
               !blockEntity.isRemoved() &&
               player.distanceToSqr(
                   blockEntity.getBlockPos().getX() + 0.5, 
                   blockEntity.getBlockPos().getY() + 0.5, 
                   blockEntity.getBlockPos().getZ() + 0.5) <= 64;
    }
    
    public boolean isAdminMode() {
        return adminMode;
    }
}
