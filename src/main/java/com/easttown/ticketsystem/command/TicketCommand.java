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
                // 添加重新加载配置命令
                .then(
                    Commands.literal("reload")
                        .executes(ReloadCommand::execute)
                )
                // 车站管理命令（统一结构）
                .then(
                    Commands.literal("station")
                        .then(
                            Commands.literal("add")
                                .then(
                                    Commands.argument("chineseName", StringArgumentType.string())
                                        .then(
                                            Commands.argument("englishName", StringArgumentType.string())
                                                .then(
                                                    Commands.argument("lineId", StringArgumentType.string())
                                                        // 基本版本：只有三个参数
                                                        .executes(
                                                            context -> AddStationCommand.executeFull(
                                                                context,
                                                                StringArgumentType.getString(context, "chineseName"),
                                                                StringArgumentType.getString(context, "englishName"),
                                                                StringArgumentType.getString(context, "lineId")
                                                            )
                                                        )
                                                        // 可选站序号
                                                        .then(
                                                            Commands.argument("stationNumber", IntegerArgumentType.integer(0, 99))
                                                                .executes(
                                                                    context -> AddStationCommand.executeFull(
                                                                        context,
                                                                        StringArgumentType.getString(context, "chineseName"),
                                                                        StringArgumentType.getString(context, "englishName"),
                                                                        StringArgumentType.getString(context, "lineId"),
                                                                        IntegerArgumentType.getInteger(context, "stationNumber")
                                                                    )
                                                                )
                                                                // 可选坐标
                                                                .then(
                                                                    Commands.argument("x", IntegerArgumentType.integer())
                                                                        .then(
                                                                            Commands.argument("y", IntegerArgumentType.integer())
                                                                                .then(
                                                                                    Commands.argument("z", IntegerArgumentType.integer())
                                                                                        .executes(
                                                                                            context -> AddStationCommand.executeFull(
                                                                                                context,
                                                                                                StringArgumentType.getString(context, "chineseName"),
                                                                                                StringArgumentType.getString(context, "englishName"),
                                                                                                StringArgumentType.getString(context, "lineId"),
                                                                                                IntegerArgumentType.getInteger(context, "stationNumber"),
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
                                        )
                                )
                        )
                        .then(
                            Commands.literal("delete")
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
                        .then(
                            Commands.literal("list")
                                .executes(StationListCommand::execute)
                        )
                        .then(
                            Commands.literal("info")
                                .then(
                                    Commands.argument("stationCode", StringArgumentType.string())
                                        .suggests((context, builder) -> DeleteStationCommand.getSuggestions(context, builder))
                                        .executes(
                                            context -> StationInfoCommand.execute(
                                                context,
                                                StringArgumentType.getString(context, "stationCode")
                                            )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("manage")
                                .executes(ManageStationsCommand::execute)
                        )
                        .then(
                            Commands.literal("edit")
                                .then(
                                    Commands.argument("stationCode", StringArgumentType.string())
                                        .suggests((context, builder) -> DeleteStationCommand.getSuggestions(context, builder))
                                        .then(
                                            Commands.argument("newName", StringArgumentType.string())
                                                .executes(
                                                    context -> StationEditCommand.executeNameOnly(
                                                        context,
                                                        StringArgumentType.getString(context, "stationCode"),
                                                        StringArgumentType.getString(context, "newName")
                                                    )
                                                )
                                        )
                                )
                        )
                )
                // 线路管理命令
                .then(
                    Commands.literal("line")
                        .then(
                            Commands.literal("create")
                                .then(
                                    Commands.argument("lineId", StringArgumentType.string())
                                        .then(
                                            Commands.argument("name", StringArgumentType.string())
                                                .then(
                                                    Commands.argument("color", StringArgumentType.string())
                                                        .executes(
                                                            context -> LineCreateCommand.execute(
                                                                context,
                                                                StringArgumentType.getString(context, "lineId"),
                                                                StringArgumentType.getString(context, "name"),
                                                                StringArgumentType.getString(context, "color")
                                                            )
                                                        )
                                                        // 可选英文名
                                                        .then(
                                                            Commands.argument("enName", StringArgumentType.string())
                                                                .executes(
                                                                    context -> LineCreateCommand.execute(
                                                                        context,
                                                                        StringArgumentType.getString(context, "lineId"),
                                                                        StringArgumentType.getString(context, "name"),
                                                                        StringArgumentType.getString(context, "enName"),
                                                                        StringArgumentType.getString(context, "color")
                                                                    )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("delete")
                                .then(
                                    Commands.argument("lineId", StringArgumentType.string())
                                        .executes(
                                            context -> LineDeleteCommand.execute(
                                                context,
                                                StringArgumentType.getString(context, "lineId")
                                            )
                                        )
                                )
                        )
                        .then(
                            Commands.literal("list")
                                .executes(LineListCommand::execute)
                        )
                        .then(
                            Commands.literal("info")
                                .then(
                                    Commands.argument("lineId", StringArgumentType.string())
                                        .executes(
                                            context -> LineInfoCommand.execute(
                                                context,
                                                StringArgumentType.getString(context, "lineId")
                                            )
                                        )
                                )
                        )
                )
                // Web服务器管理命令
                .then(
                    Commands.literal("startwebserver")
                        .executes(WebServerStartCommand::execute)
                )
                .then(
                    Commands.literal("stopwebserver")
                        .executes(WebServerStartCommand::executeStop)
                )
                .then(
                    Commands.literal("webserverstatus")
                        .executes(WebServerStartCommand::executeStatus)
                )
        );
    }
}
