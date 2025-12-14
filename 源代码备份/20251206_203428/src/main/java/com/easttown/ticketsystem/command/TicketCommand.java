package com.easttown.ticketsystem.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class TicketCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("ticketsystem")
                .requires(source -> source.hasPermission(4))
                .then(
                    Commands.literal("addstation")
                        .then(
                            Commands.argument("station", StringArgumentType.string())
                            .then(
                                Commands.argument("x", IntegerArgumentType.integer())
                                .then(
                                    Commands.argument("y", IntegerArgumentType.integer())
                                    .then(
                                        Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(
                                            context -> AddStationCommand.execute(
                                                context,
                                                StringArgumentType.getString(context, "station"),
                                                IntegerArgumentType.getInteger(context, "x"),
                                                IntegerArgumentType.getInteger(context, "y"),
                                                IntegerArgumentType.getInteger(context, "z")
                                            )
                                        )
                                    )
                                )
                            )
                        )
                )
                // 修复点运算符位置和缺失的括号
                .then(
                    Commands.literal("deletestation")
                        .then(
                            Commands.argument("station", StringArgumentType.string())
                                .suggests((context, builder) -> DeleteStationCommand.getSuggestions(context, builder))
                                .executes(
                                    context -> DeleteStationCommand.execute(
                                        context,
                                        StringArgumentType.getString(context, "station")
                                    )
                                )
                        )
                )
                // 添加重新加载配置命令
                .then(
                    Commands.literal("reload")
                        .executes(ReloadCommand::execute)
                )
        );
    }
}
