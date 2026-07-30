package com.mythicrpg.traveling;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TravelingDebugCommands {

    private TravelingDebugCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("mythicrpg")
                        .then(CommandManager.literal("traveling")
                                .then(CommandManager.literal("status")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                            TravelingProgressState state = TravelingProgressState.get(
                                                    context.getSource().getServer()
                                            );

                                            player.sendMessage(Text.translatable(
                                                    "command.mythicrpg.traveling.stats",
                                                    state.getStructureCount(player.getUuid()),
                                                    state.getDimensionCount(player.getUuid()),
                                                    state.getMovementCellCount(player.getUuid()),
                                                    state.getTreasureChestCount(player.getUuid())
                                            ).formatted(Formatting.AQUA), false);

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(CommandManager.literal("reset_discoveries")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                            TravelingProgressState.get(context.getSource().getServer())
                                                    .clearPlayer(player.getUuid());
                                            TravelingXpManager.clearRuntimePlayer(player.getUuid());
                                            TravelingPerkManager.clearRuntimePlayer(player.getUuid());

                                            player.sendMessage(
                                                    Text.translatable("command.mythicrpg.traveling.reset")
                                                            .formatted(Formatting.GREEN),
                                                    false
                                            );

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                )
        );
    }
}
