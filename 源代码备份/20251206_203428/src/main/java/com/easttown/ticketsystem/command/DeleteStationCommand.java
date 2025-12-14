package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.manager.StationManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;

public class DeleteStationCommand {
    public static int execute(CommandContext<CommandSourceStack> context, String station) {
        if (StationManager.containsStation(station)) {
            StationManager.removeStation(station);
            context.getSource().sendSuccess(() -> 
                Component.literal("已删除车站: " + station), true);
            return Command.SINGLE_SUCCESS;
        } else {
            context.getSource().sendFailure(
                Component.literal("车站不存在: " + station));
            return 0;
        }
    }
    
    public static CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(StationManager.getStations(), builder);
    }
}