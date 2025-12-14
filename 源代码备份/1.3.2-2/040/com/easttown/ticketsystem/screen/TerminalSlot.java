package com.easttown.ticketsystem.screen;

import com.easttown.ticketsystem.item.TicketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 终端专用槽位类
 * 为旅行服务终端提供自定义的物品槽位逻辑
 */
public class TerminalSlot extends SlotItemHandler {
    private final boolean isInputSlot;
    
    public TerminalSlot(IItemHandler itemHandler, int index, int x, int y, boolean isInputSlot) {
        super(itemHandler, index, x, y);
        this.isInputSlot = isInputSlot;
    }
    
    @Override
    public boolean mayPlace(ItemStack stack) {
        // 输入槽只允许放入车票，输出槽不允许放入物品
        if (!isInputSlot) {
            return false;
        }
        
        // 检查是否为车票物品
        return stack.getItem() instanceof TicketItem;
    }
    
    @Override
    public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
        // 输出槽可以随时拿起，输入槽只有在有物品时可以拿起
        if (!isInputSlot) {
            return true;
        }
        
        ItemStack stack = getItem();
        return !stack.isEmpty();
    }
}