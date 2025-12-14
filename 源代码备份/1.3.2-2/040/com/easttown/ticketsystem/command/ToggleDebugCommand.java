// ToggleDebugCommand.java
package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.TicketSystemMod;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ToggleDebugCommand {
    public static int execute(CommandContext<CommandSourceStack> context) {
        TicketSystemMod.debugMode = !TicketSystemMod.debugMode;
        String status = TicketSystemMod.debugMode ? "开启" : "关闭";
        context.getSource().sendSuccess(() -> 
            Component.literal("调试模式已" + status), true);
        return Command.SINGLE_SUCCESS;
    }
}