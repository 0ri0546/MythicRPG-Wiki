package com.mythicrpg.traveling;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TravelingDeathRecallManager {

    private static final long TOKEN_LIFETIME_MILLIS = 5L * 60L * 1000L;
    private static final long USE_COOLDOWN_MILLIS = 5L * 60L * 1000L;
    private static final int PLAYER_VALIDATION_INTERVAL_TICKS = 20;
    private static final int SAFE_HORIZONTAL_RADIUS = 4;
    private static final int SAFE_VERTICAL_RADIUS = 6;

    private static final Map<UUID, PendingDeath> PENDING_DEATHS = new HashMap<>();
    private static final Set<UUID> ACTIVE_RECALL_PLAYERS = new HashSet<>();
    private static int tickCounter;

    private TravelingDeathRecallManager() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return;
            }

            UUID playerUuid = player.getUuid();
            TravelingProgressState.get(player.getServer())
                    .clearActiveDeathRecall(playerUuid);
            ACTIVE_RECALL_PLAYERS.remove(playerUuid);

            if (TravelingBonusCache.hasBonus(
                    player,
                    BonusType.TRAVEL_DEATH_RECALL
            )) {
                PENDING_DEATHS.put(playerUuid, new PendingDeath(
                        player.getServerWorld().getRegistryKey(),
                        player.getBlockPos().toImmutable()
                ));
            } else {
                PENDING_DEATHS.remove(playerUuid);
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                return;
            }

            PendingDeath pending = PENDING_DEATHS.remove(newPlayer.getUuid());
            if (pending == null) {
                return;
            }

            giveRecallToken(newPlayer, pending);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (ACTIVE_RECALL_PLAYERS.isEmpty()
                    || tickCounter % PLAYER_VALIDATION_INTERVAL_TICKS != 0) {
                return;
            }

            TravelingProgressState state = TravelingProgressState.get(server);
            Iterator<UUID> iterator = ACTIVE_RECALL_PLAYERS.iterator();
            while (iterator.hasNext()) {
                UUID playerUuid = iterator.next();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);

                if (player == null) {
                    iterator.remove();
                    continue;
                }

                if (!state.hasActiveDeathRecall(playerUuid)
                        || !TravelingBonusCache.hasBonus(player, BonusType.TRAVEL_DEATH_RECALL)) {
                    state.clearActiveDeathRecall(playerUuid);
                    iterator.remove();
                }
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            UUID playerUuid = handler.player.getUuid();
            if (TravelingProgressState.get(server).hasActiveDeathRecall(playerUuid)) {
                ACTIVE_RECALL_PLAYERS.add(playerUuid);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerUuid = handler.player.getUuid();
            PENDING_DEATHS.remove(playerUuid);
            ACTIVE_RECALL_PLAYERS.remove(playerUuid);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PENDING_DEATHS.clear();
            ACTIVE_RECALL_PLAYERS.clear();
            tickCounter = 0;
        });
    }

    public static boolean tryUse(ServerPlayerEntity player, ItemStack stack) {
        Optional<DeathRecallTokenData.Data> optionalData = DeathRecallTokenData.read(stack);
        if (optionalData.isEmpty()) {
            consumeInvalid(stack);
            return false;
        }

        DeathRecallTokenData.Data data = optionalData.get();
        if (!isUsableBy(player, data)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.death_recall.invalid")
                            .formatted(Formatting.RED),
                    true
            );
            discardInvalidInventoryToken(player, stack);
            return false;
        }

        MinecraftServer server = player.getServer();

        TravelingProgressState progressState =
                TravelingProgressState.get(server);

        long cooldownSeconds =
                getRemainingCooldownSeconds(progressState, player.getUuid());

        if (cooldownSeconds > 0L) {
            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.death_recall.cooldown",
                            cooldownSeconds
                    ).formatted(Formatting.RED),
                    true
            );

            return false;
        }

        ServerWorld targetWorld = server.getWorld(data.dimension());
        if (targetWorld == null) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.death_recall.dimension_missing")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        Optional<BlockPos> safeDestination = findSafeDestination(targetWorld, data.deathPos());
        if (safeDestination.isEmpty()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.death_recall.no_safe_location")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        progressState.clearActiveDeathRecall(player.getUuid());
        ACTIVE_RECALL_PLAYERS.remove(player.getUuid());
        stack.decrement(1);

        ServerWorld originWorld = player.getServerWorld();
        originWorld.spawnParticles(
                ParticleTypes.PORTAL,
                player.getX(),
                player.getBodyY(0.5),
                player.getZ(),
                32,
                0.4,
                0.6,
                0.4,
                0.15
        );
        originWorld.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                0.8f,
                0.9f
        );

        BlockPos destination = safeDestination.get();
        player.teleport(
                targetWorld,
                destination.getX() + 0.5,
                destination.getY(),
                destination.getZ() + 0.5,
                player.getYaw(),
                player.getPitch()
        );
        player.fallDistance = 0.0f;

        progressState.setDeathRecallCooldownUntil(
                player.getUuid(),
                System.currentTimeMillis() + USE_COOLDOWN_MILLIS
        );

        targetWorld.spawnParticles(
                ParticleTypes.PORTAL,
                player.getX(),
                player.getBodyY(0.5),
                player.getZ(),
                48,
                0.45,
                0.7,
                0.45,
                0.2
        );
        targetWorld.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                0.9f,
                1.15f
        );
        player.sendMessage(
                Text.translatable("message.mythicrpg.death_recall.success")
                        .formatted(Formatting.LIGHT_PURPLE),
                true
        );
        return true;
    }

    public static boolean isUsableBy(ServerPlayerEntity player, ItemStack stack) {
        return DeathRecallTokenData.read(stack)
                .map(data -> isUsableBy(player, data))
                .orElse(false);
    }

    private static boolean isUsableBy(
            ServerPlayerEntity player,
            DeathRecallTokenData.Data data
    ) {
        if (!data.owner().equals(player.getUuid())
                || data.expiresAtMillis() <= System.currentTimeMillis()
                || !TravelingBonusCache.hasBonus(player, BonusType.TRAVEL_DEATH_RECALL)) {
            return false;
        }

        return TravelingProgressState.get(player.getServer())
                .isActiveDeathRecall(player.getUuid(), data.recallId());
    }

    public static void discardInvalidInventoryToken(
            ServerPlayerEntity player,
            ItemStack stack
    ) {
        Optional<DeathRecallTokenData.Data> optionalData = DeathRecallTokenData.read(stack);
        if (optionalData.isPresent() && isUsableBy(player, optionalData.get())) {
            return;
        }

        optionalData.ifPresent(data -> {
            if (!data.owner().equals(player.getUuid())) {
                return;
            }

            TravelingProgressState state = TravelingProgressState.get(player.getServer());
            if (state.isActiveDeathRecall(player.getUuid(), data.recallId())) {
                state.clearActiveDeathRecall(player.getUuid());
                ACTIVE_RECALL_PLAYERS.remove(player.getUuid());
            }
        });
        consumeInvalid(stack);
    }

    public static boolean shouldDiscardDroppedToken(ItemStack stack, MinecraftServer server) {
        Optional<DeathRecallTokenData.Data> optionalData = DeathRecallTokenData.read(stack);
        if (optionalData.isEmpty()) {
            return true;
        }

        DeathRecallTokenData.Data data = optionalData.get();
        TravelingProgressState state = TravelingProgressState.get(server);
        boolean expired = data.expiresAtMillis() <= System.currentTimeMillis();
        boolean active = state.isActiveDeathRecall(data.owner(), data.recallId());

        if (expired && active) {
            state.clearActiveDeathRecall(data.owner());
            ACTIVE_RECALL_PLAYERS.remove(data.owner());
        }

        return expired || !active;
    }

    private static long getRemainingCooldownSeconds(
            TravelingProgressState state,
            UUID playerUuid
    ) {
        long remainingMillis =
                state.getDeathRecallCooldownRemainingMillis(playerUuid);

        return Math.max(0L, (remainingMillis + 999L) / 1000L);
    }

    private static void giveRecallToken(ServerPlayerEntity player, PendingDeath pending) {
        removeRecallTokens(player);

        TravelingProgressState progressState =
                TravelingProgressState.get(player.getServer());

        long cooldownSeconds =
                getRemainingCooldownSeconds(progressState, player.getUuid());

        if (cooldownSeconds > 0L) {
            progressState.clearActiveDeathRecall(player.getUuid());
            ACTIVE_RECALL_PLAYERS.remove(player.getUuid());

            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.death_recall.cooldown",
                            cooldownSeconds
                    ).formatted(Formatting.RED),
                    false
            );

            return;
        }

        UUID recallId = UUID.randomUUID();
        long expiresAt = System.currentTimeMillis() + TOKEN_LIFETIME_MILLIS;
        progressState.setActiveDeathRecall(
                player.getUuid(),
                recallId,
                expiresAt
        );
        ACTIVE_RECALL_PLAYERS.add(player.getUuid());

        ItemStack token = new ItemStack(ModItems.DEATH_RECALL_TOKEN);
        DeathRecallTokenData.write(
                token,
                player.getUuid(),
                recallId,
                pending.dimension(),
                pending.position(),
                expiresAt
        );

        ItemStack remaining = token.copy();
        if (!player.getInventory().insertStack(remaining)) {
            player.dropItem(remaining, false);
        }

        player.currentScreenHandler.sendContentUpdates();
        player.sendMessage(
                Text.translatable("message.mythicrpg.death_recall.received")
                        .formatted(Formatting.LIGHT_PURPLE),
                false
        );
    }

    private static void removeRecallTokens(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(ModItems.DEATH_RECALL_TOKEN)) {
                player.getInventory().setStack(slot, ItemStack.EMPTY);
            }
        }
    }

    private static void consumeInvalid(ItemStack stack) {
        stack.decrement(stack.getCount());
    }

    private static Optional<BlockPos> findSafeDestination(ServerWorld world, BlockPos origin) {
        for (int radius = 0; radius <= SAFE_HORIZONTAL_RADIUS; radius++) {
            for (int verticalDistance = 0; verticalDistance <= SAFE_VERTICAL_RADIUS; verticalDistance++) {
                int attempts = verticalDistance == 0 ? 1 : 2;

                for (int verticalAttempt = 0; verticalAttempt < attempts; verticalAttempt++) {
                    int dy = verticalAttempt == 0 ? verticalDistance : -verticalDistance;

                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                                continue;
                            }

                            BlockPos candidate = origin.add(dx, dy, dz);
                            if (isSafeDestination(world, candidate)) {
                                return Optional.of(candidate.toImmutable());
                            }
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isSafeDestination(ServerWorld world, BlockPos feetPos) {
        if (!world.isInBuildLimit(feetPos)
                || !world.getWorldBorder().contains(feetPos)) {
            return false;
        }

        BlockPos headPos = feetPos.up();
        BlockPos floorPos = feetPos.down();
        BlockState feet = world.getBlockState(feetPos);
        BlockState head = world.getBlockState(headPos);
        BlockState floor = world.getBlockState(floorPos);

        if (!feet.getCollisionShape(world, feetPos).isEmpty()
                || !head.getCollisionShape(world, headPos).isEmpty()
                || floor.getCollisionShape(world, floorPos).isEmpty()) {
            return false;
        }

        if (!feet.getFluidState().isEmpty()
                || !head.getFluidState().isEmpty()
                || !floor.getFluidState().isEmpty()) {
            return false;
        }

        return !floor.isOf(Blocks.MAGMA_BLOCK)
                && !floor.isOf(Blocks.CAMPFIRE)
                && !floor.isOf(Blocks.SOUL_CAMPFIRE)
                && !floor.isOf(Blocks.CACTUS)
                && !feet.isOf(Blocks.FIRE)
                && !feet.isOf(Blocks.SOUL_FIRE)
                && !feet.isOf(Blocks.POWDER_SNOW);
    }

    private record PendingDeath(
            net.minecraft.registry.RegistryKey<World> dimension,
            BlockPos position
    ) {
    }
}
