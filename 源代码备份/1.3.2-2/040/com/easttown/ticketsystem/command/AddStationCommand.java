package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.manager.StationManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class AddStationCommand {
    public static int execute(CommandContext<CommandSourceStack> context, String station, int x, int y, int z) {
        // 添加车站坐标信息
        StationManager.addStation(station, x, y, z);
        context.getSource().sendSuccess(() -> 
            Component.literal("已添加车站: " + station + " 坐标: (" + x + ", " + y + ", " + z + ")"), 
            true
        );
        return Command.SINGLE_SUCCESS;
    }
}