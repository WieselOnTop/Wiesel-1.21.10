package com.wiesel.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.wiesel.client.WieselClient;
import com.wiesel.client.pathfinder.PathWalker;
import com.wiesel.client.pathfinder.PathfindResponse;
import com.wiesel.client.pathfinder.PathfinderManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class CommandManager {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(CommandManager::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                        CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("wiesel")
            .then(literal("goto")
                .then(argument("x", DoubleArgumentType.doubleArg())
                    .then(argument("y", DoubleArgumentType.doubleArg())
                        .then(argument("z", DoubleArgumentType.doubleArg())
                            .executes(context -> {
                                double x = DoubleArgumentType.getDouble(context, "x");
                                double y = DoubleArgumentType.getDouble(context, "y");
                                double z = DoubleArgumentType.getDouble(context, "z");

                                var player = context.getSource().getPlayer();
                                if (player == null) {
                                    context.getSource().sendError(Text.literal("Player not found"));
                                    return 0;
                                }

                                double startX = player.getX();
                                double startY = player.getY();
                                double startZ = player.getZ();

                                context.getSource().sendFeedback(Text.literal("§aCalculating path to §e" + x + ", " + y + ", " + z + "§a..."));

                                // Calculate path in separate thread
                                new Thread(() -> {
                                    try {
                                        PathfindResponse path = PathfinderManager.pathfind(startX, startY, startZ, x, y, z);

                                        if (path != null && path.path != null && !path.path.isEmpty()) {
                                            WieselClient.LOGGER.info("Path found with {} nodes", path.path.size());
                                            context.getSource().sendFeedback(Text.literal("§aPath found! §e" + path.path.size() + " nodes§a. Starting walk..."));

                                            // Start walking on the main thread
                                            context.getSource().getClient().execute(() -> {
                                                PathWalker.startWalking(path);
                                            });
                                        } else {
                                            context.getSource().sendError(Text.literal("§cFailed to find path"));
                                        }
                                    } catch (Exception e) {
                                        WieselClient.LOGGER.error("Error finding path", e);
                                        context.getSource().sendError(Text.literal("§cError: " + e.getMessage()));
                                    }
                                }, "WieselPathfinding").start();

                                return 1;
                            })
                        )
                    )
                )
            )
            .then(literal("stop")
                .executes(context -> {
                    if (PathWalker.isWalking()) {
                        PathWalker.stopWalking();
                        PathfinderManager.clearPath();
                        context.getSource().sendFeedback(Text.literal("§aStopped walking"));
                    } else {
                        context.getSource().sendFeedback(Text.literal("§eNot currently walking"));
                    }
                    return 1;
                })
            )
            .then(literal("clear")
                .executes(context -> {
                    PathfinderManager.clearPath();
                    context.getSource().sendFeedback(Text.literal("§aCleared path rendering"));
                    return 1;
                })
            )
            .then(literal("map")
                .then(argument("mapname", StringArgumentType.word())
                    .executes(context -> {
                        String mapName = StringArgumentType.getString(context, "mapname");
                        context.getSource().sendFeedback(Text.literal("§aLoading map: §e" + mapName + "§a..."));

                        new Thread(() -> {
                            boolean success = PathfinderManager.loadMap(mapName);
                            if (success) {
                                context.getSource().sendFeedback(Text.literal("§aMap loaded: §e" + mapName));
                            } else {
                                context.getSource().sendError(Text.literal("§cFailed to load map: " + mapName));
                            }
                        }, "WieselMapLoader").start();

                        return 1;
                    })
                )
            )
        );
    }
}
