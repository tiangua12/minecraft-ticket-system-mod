package com.easttown.ticketsystem.screen;

import com.easttown.ticketsystem.block.GateBlockEntity;
import com.easttown.ticketsystem.init.MenuInit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class GateConfigMenu extends AbstractContainerMenu {

    private final GateBlockEntity blockEntity;

    public GateConfigMenu(int containerId, Inventory inventory, GateBlockEntity blockEntity) {
        super(MenuInit.GATE_CONFIG_MENU.get(), containerId);
        this.blockEntity = blockEntity;
    }

    public GateConfigMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, (GateBlockEntity) inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public GateBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && !blockEntity.isRemoved();
    }

    @Nullable
    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        return null;
    }
}
