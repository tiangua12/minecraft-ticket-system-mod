package com.easttown.ticketsystem.command;

import com.easttown.ticketsystem.manager.NetworkManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DeleteStationCommand {
    public static int execute(CommandContext<CommandSourceStack> context, String station) {
        NetworkManager.initialize();

        if (NetworkManager.hasStation(station)) {
            boolean success = NetworkManager.removeStation(station);
            if (success) {
                context.getSource().sendSuccess(() ->
                    Component.literal("已删除车站: " + station), true);
                return Command.SINGLE_SUCCESS;
            } else {
                context.getSource().sendFailure(
                    Component.literal("删除车站失败: " + station));
                return 0;
            }
        } else {
            context.getSource().sendFailure(
                Component.literal("车站不存在: " + station));
            return 0;
        }
    }

    public static CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        NetworkManager.initialize();
        List<String> stationCodes = new ArrayList<>(NetworkManager.getStationCodes());
        return SharedSuggestionProvider.suggest(stationCodes, builder);
    }
}