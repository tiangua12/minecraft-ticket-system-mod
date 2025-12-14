package com.easttown.ticketsystem.item;

import com.easttown.ticketsystem.util.LanguageHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TicketItem extends Item {
    // 车票状态常量
    public static final String UNUSED = "UNUSED";
    public static final String IN_USE = "IN_USE";
    public static final String COMPLETED = "COMPLETED";

    public TicketItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, 
                               List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            String start = tag.getString("StartStation");
            String dest = tag.getString("Destination");
            long issueTime = tag.getLong("IssueTime");
            int price = tag.getInt("Price");
            String status = tag.getString("Status");
            
            if (!start.isEmpty() && !dest.isEmpty()) {
                // 格式化时间
                String timeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(issueTime));
                
                // 添加基本信息 - 使用语言键
                tooltip.add(LanguageHelper.translate("item.ticket.tooltip.route", start, dest)
                    .copy().withStyle(ChatFormatting.GRAY));
                tooltip.add(LanguageHelper.translate("item.ticket.tooltip.price", price)
                    .copy().withStyle(ChatFormatting.GOLD));
                tooltip.add(LanguageHelper.translate("item.ticket.tooltip.time", timeStr)
                    .copy().withStyle(ChatFormatting.DARK_GRAY));
                
                // 添加状态信息 - 使用语言键
                Component statusText;
                switch (status) {
                    case IN_USE:
                        statusText = LanguageHelper.translate("item.ticket.status.in_use")
                            .copy().withStyle(ChatFormatting.YELLOW);
                        break;
                    case COMPLETED:
                        statusText = LanguageHelper.translate("item.ticket.status.completed")
                            .copy().withStyle(ChatFormatting.GREEN);
                        break;
                    default: // UNUSED
                        statusText = LanguageHelper.translate("item.ticket.status.unused")
                            .copy().withStyle(ChatFormatting.BLUE);
                }
                tooltip.add(statusText);
                
                // 添加使用说明 - 使用语言键
                tooltip.add(LanguageHelper.translate("item.ticket.usage")
                    .copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_PURPLE));
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = stack.getTag();
        
        if (tag != null) {
            String status = tag.getString("Status");
            String dest = tag.getString("Destination");
            
            if (!dest.isEmpty()) {
                if (!level.isClientSide) {
                    // 显示使用信息 - 使用语言键
                    switch (status) {
                        case UNUSED:
                            player.displayClientMessage(
                                LanguageHelper.translate("item.ticket.use.unused", dest), true);
                            break;
                        case IN_USE:
                            player.displayClientMessage(
                                LanguageHelper.translate("item.ticket.use.in_use", dest), true);
                            break;
                        case COMPLETED:
                            player.displayClientMessage(
                                LanguageHelper.translate("item.ticket.use.completed", dest), true);
                            break;
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        
        return InteractionResultHolder.pass(stack);
    }
    
    // 辅助方法：检查车票状态
    public static boolean isUnused(ItemStack ticket) {
        CompoundTag tag = ticket.getTag();
        return tag != null && tag.getString("Status").equals(UNUSED);
    }
    
    public static boolean isInUse(ItemStack ticket) {
        CompoundTag tag = ticket.getTag();
        return tag != null && tag.getString("Status").equals(IN_USE);
    }
    
    public static boolean isCompleted(ItemStack ticket) {
        CompoundTag tag = ticket.getTag();
        return tag != null && tag.getString("Status").equals(COMPLETED);
    }
}
