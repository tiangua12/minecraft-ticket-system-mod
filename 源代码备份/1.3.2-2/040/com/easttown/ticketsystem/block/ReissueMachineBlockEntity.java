package com.easttown.ticketsystem.block;

import com.easttown.ticketsystem.init.BlockEntityInit;
import com.easttown.ticketsystem.init.MenuInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ReissueMachineBlockEntity extends BlockEntity implements MenuProvider {

    public ReissueMachineBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityInit.REISSUE_MACHINE.get(), pos, state);
    }

    // MenuProvider接口实现
    @Override
    public Component getDisplayName() {
        return Component.literal("补票机器");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        // 返回主菜单容器
        return new com.easttown.ticketsystem.screen.terminal.menu.TerminalMainMenu(containerId, inventory, this);
    }

    // 客户端打开屏幕的方法
    public void openScreen(Player player) {
        if (player.level().isClientSide()) {
            // 直接打开主菜单界面
            player.openMenu(this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // 极简版本，不需要保存额外数据
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 极简版本，不需要加载额外数据
    }

    // 数据同步方法
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }
}