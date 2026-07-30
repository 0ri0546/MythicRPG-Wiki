package com.mythicrpg.crafting;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class CraftDebugCommands {

    private CraftDebugCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("mythicrpg")
                        .then(CommandManager.literal("crafting")
                                .then(CommandManager.literal("reset_first_crafts")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayer();

                                            CraftFirstCraftState state = CraftFirstCraftState.get(
                                                    context.getSource().getServer()
                                            );

                                            state.clearPlayer(player.getUuid());

                                            player.sendMessage(
                                                    Text.translatable("command.mythicrpg.craft.first_craft_reset")
                                                            .formatted(Formatting.GREEN),
                                                    false
                                            );

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(CommandManager.literal("reset_charge")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayer();

                                            CraftChargeState state = CraftChargeState.get(
                                                    context.getSource().getServer()
                                            );

                                            state.clearPlayer(player.getUuid());

                                            player.sendMessage(
                                                    Text.translatable("command.mythicrpg.craft.charge_reset")
                                                            .formatted(Formatting.GREEN),
                                                    false
                                            );

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(CommandManager.literal("open_portable")
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayer();

                                            if (!PortableCraftingManager.hasPortableCrafting(player)) {
                                                player.sendMessage(
                                                        Text.translatable("message.mythicrpg.perk_required", Text.translatable("skill_tree.mythicrpg.crafting.1.name"))
                                                                .formatted(Formatting.RED),
                                                        true
                                                );
                                                return 0;
                                            }

                                            MythicCraftingScreenHandler.openPortable(player);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(CommandManager.literal("reset_portable_durability")
                                        .requires(source -> source.hasPermissionLevel(2))
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource().getPlayer();

                                            PortableCraftingState.get(context.getSource().getServer())
                                                    .resetPlayer(player.getUuid());

                                            player.sendMessage(
                                                    Text.translatable("command.mythicrpg.craft.portable_reset")
                                                            .formatted(Formatting.GREEN),
                                                    false
                                            );

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                                .then(CommandManager.literal("transform")
                                        .executes(context -> TransformationSlotManager.transformMainHand(
                                                context.getSource().getPlayer()
                                        ))
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> TransformationSlotManager.transformMainHand(
                                                        context.getSource().getPlayer(),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                ))
                                        )
                                )
                        )
                )
        );
    }
}
