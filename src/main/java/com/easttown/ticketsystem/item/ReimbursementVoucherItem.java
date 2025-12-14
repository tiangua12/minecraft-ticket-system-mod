package com.easttown.ticketsystem.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReimbursementVoucherItem extends Item {
    public ReimbursementVoucherItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, 
                               List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean("IsReimbursement")) {
            String start = tag.getString("StartStation");
            String dest = tag.getString("Destination");
            long issueTime = tag.getLong("IssueTime");
            int price = tag.getInt("Price");
            
            if (!start.isEmpty() && !dest.isEmpty()) {
                // 格式化时间
                String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(issueTime));
                
                // 添加基本信息
                tooltip.add(Component.translatable("item.voucher.tooltip.route", start, dest)
                    .copy().withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("item.voucher.tooltip.price", price)
                    .copy().withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable("item.voucher.tooltip.time", timeStr)
                    .copy().withStyle(ChatFormatting.DARK_GRAY));
                
                // 添加报销凭证标识
                tooltip.add(Component.translatable("item.voucher.status")
                    .copy().withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            }
        }
    }
}
