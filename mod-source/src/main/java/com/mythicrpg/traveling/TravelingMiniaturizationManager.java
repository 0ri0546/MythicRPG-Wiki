package com.mythicrpg.traveling;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public final class TravelingMiniaturizationManager {

    private static final Identifier SCALE_MODIFIER_ID =
            Identifier.of(MythicRPG.MOD_ID, "traveling_miniaturization");
    private static final double SCALE_MODIFIER = -0.5;
    private static final int VALIDATION_INTERVAL_TICKS = 10;

    private static final Set<UUID> ACTIVE_PLAYERS = new HashSet<>();
    private static int tickCounter;

    private TravelingMiniaturizationManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (ACTIVE_PLAYERS.isEmpty()
                    || tickCounter % VALIDATION_INTERVAL_TICKS != 0) {
                return;
            }

            Iterator<UUID> iterator = ACTIVE_PLAYERS.iterator();
            while (iterator.hasNext()) {
                UUID playerUuid = iterator.next();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);

                if (player == null || !isMiniaturized(player)) {
                    iterator.remove();
                    continue;
                }

                if (!canRemainMiniaturized(player)) {
                    applyMiniaturizedState(player, false, false);
                    iterator.remove();
                }
            }
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                setMiniaturized(player, false, false);
            }
            return true;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                setMiniaturized(handler.player, false, false)
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                setMiniaturized(handler.player, false, false)
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ACTIVE_PLAYERS.clear();
            tickCounter = 0;
        });
    }

    public static boolean toggle(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }

        if (!TravelingBonusCache.hasBonus(
                serverPlayer,
                BonusType.TRAVEL_MINIATURIZATION
        )) {
            serverPlayer.sendMessage(
                    Text.translatable("message.mythicrpg.miniaturization.locked").formatted(Formatting.RED),
                    true
            );
            return false;
        }

        boolean activate = !isMiniaturized(serverPlayer);
        if (activate && !hasCharm(serverPlayer)) {
            return false;
        }

        setMiniaturized(serverPlayer, activate, true);
        return true;
    }

    public static boolean isMiniaturized(PlayerEntity player) {
        EntityAttributeInstance scale = player.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
        return scale != null && scale.hasModifier(SCALE_MODIFIER_ID);
    }

    private static boolean canRemainMiniaturized(ServerPlayerEntity player) {
        return player.isAlive()
                && !player.isSpectator()
                && TravelingBonusCache.hasBonus(
                        player,
                        BonusType.TRAVEL_MINIATURIZATION
                )
                && hasCharm(player);
    }

    private static boolean hasCharm(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (player.getInventory().getStack(slot).isOf(ModItems.MINIATURIZATION_CHARM)) {
                return true;
            }
        }
        return false;
    }

    private static void setMiniaturized(ServerPlayerEntity player, boolean active, boolean feedback) {
        applyMiniaturizedState(player, active, feedback);

        if (active && isMiniaturized(player)) {
            ACTIVE_PLAYERS.add(player.getUuid());
        } else {
            ACTIVE_PLAYERS.remove(player.getUuid());
        }
    }

    private static void applyMiniaturizedState(
            ServerPlayerEntity player,
            boolean active,
            boolean feedback
    ) {
        EntityAttributeInstance scale = player.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
        if (scale == null) {
            return;
        }

        boolean changed;
        if (active) {
            changed = !scale.hasModifier(SCALE_MODIFIER_ID);
            if (changed) {
                scale.addTemporaryModifier(new EntityAttributeModifier(
                        SCALE_MODIFIER_ID,
                        SCALE_MODIFIER,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            }
        } else {
            changed = scale.hasModifier(SCALE_MODIFIER_ID);
            if (changed) {
                scale.removeModifier(SCALE_MODIFIER_ID);
            }
        }

        if (!changed) {
            return;
        }

        player.calculateDimensions();

        if (!feedback) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        world.spawnParticles(
                active ? ParticleTypes.POOF : ParticleTypes.CLOUD,
                player.getX(),
                player.getBodyY(0.5),
                player.getZ(),
                16,
                0.3,
                0.45,
                0.3,
                0.02
        );
        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                0.45f,
                active ? 1.65f : 0.85f
        );
        player.sendMessage(
                Text.translatable(active
                        ? "message.mythicrpg.miniaturization.small"
                        : "message.mythicrpg.miniaturization.normal"),
                true
        );
    }
}
