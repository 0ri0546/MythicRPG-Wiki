package com.mythicrpg.mining.archaeology.relic;

import com.mythicrpg.mining.archaeology.polish.ArchaeologyPolishEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TemporalReturnManager {

    private static final long MILLIS_PER_SECOND = 1_000L;
    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final int SAFE_RADIUS = 10;
    private static final long BLOCKED_RETRY_NOTICE_MILLIS = 5_000L;
    private static final long BLOCKED_RETRY_BASE_MILLIS = 1_000L;
    private static final long BLOCKED_RETRY_MAX_MILLIS = 30_000L;
    private static final Vector3f TEMPORAL_COLOR = new Vector3f(0.55F, 0.28F, 1.0F);
    private static final List<BlockPos> SAFE_OFFSETS = buildSafeOffsets();

    private static final Map<UUID, Integer> LAST_COUNTDOWN_SECOND = new HashMap<>();
    private static final Map<UUID, Long> NEXT_BLOCKED_NOTICE = new HashMap<>();
    private static final Map<UUID, Long> NEXT_RETURN_ATTEMPT = new HashMap<>();
    private static final Map<UUID, Integer> BLOCKED_FAILURES = new HashMap<>();

    private TemporalReturnManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TemporalReturnManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                clearTransientState(handler.player.getUuid())
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LAST_COUNTDOWN_SECOND.clear();
            NEXT_BLOCKED_NOTICE.clear();
            NEXT_RETURN_ATTEMPT.clear();
            BLOCKED_FAILURES.clear();
        });
    }

    private static void clearTransientState(UUID playerUuid) {
        LAST_COUNTDOWN_SECOND.remove(playerUuid);
        NEXT_BLOCKED_NOTICE.remove(playerUuid);
        NEXT_RETURN_ATTEMPT.remove(playerUuid);
        BLOCKED_FAILURES.remove(playerUuid);
    }

    public static boolean activate(ServerPlayerEntity player, ItemStack stack) {
        TemporalReturnState state = TemporalReturnState.get(player.getServer());
        long now = System.currentTimeMillis();
        long cooldownRemaining = state.cooldownRemainingMillis(player.getUuid(), now);
        if (cooldownRemaining > 0L) {
            player.sendMessage(Text.translatable(
                    "message.mythicrpg.temporal_machine.cooldown",
                    Math.max(1L, (cooldownRemaining + 999L) / MILLIS_PER_SECOND)
            ).formatted(Formatting.RED), true);
            return false;
        }
        if (state.hasPending(player.getUuid())) {
            player.sendMessage(Text.translatable("message.mythicrpg.temporal_machine.already_active")
                    .formatted(Formatting.RED), true);
            return false;
        }

        RelicLevel level = RelicItemData.getLevel(stack);
        long returnAt = now + (long) level.value() * MILLIS_PER_MINUTE;
        int cooldownMinutes = 20 - level.value();
        state.setCooldownUntil(
                player.getUuid(),
                now + (long) cooldownMinutes * MILLIS_PER_MINUTE
        );
        state.setPending(player.getUuid(), new TemporalReturnState.PendingReturn(
                player.getServerWorld().getRegistryKey(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYaw(),
                player.getPitch(),
                returnAt
        ));
        clearTransientState(player.getUuid());

        player.sendMessage(Text.translatable(
                "message.mythicrpg.temporal_machine.armed",
                level.value()
        ).formatted(Formatting.LIGHT_PURPLE), true);
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE,
                SoundCategory.PLAYERS,
                0.8F,
                1.35F
        );
        spawnAnchorFeedback(player.getServerWorld(), player.getPos(), level.value());
        return true;
    }

    private static void tick(MinecraftServer server) {
        TemporalReturnState state = TemporalReturnState.get(server);
        List<Map.Entry<UUID, TemporalReturnState.PendingReturn>> entries = state.pendingEntries();
        if (entries.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, TemporalReturnState.PendingReturn> entry : entries) {
            UUID playerUuid = entry.getKey();
            TemporalReturnState.PendingReturn pending = entry.getValue();
            long remainingMillis = pending.returnAtMillis() - now;

            ServerWorld anchorWorld = server.getWorld(pending.dimension());
            if (anchorWorld != null
                    && remainingMillis > 0L
                    && Math.floorMod(anchorWorld.getTime(), 20L) == 0L) {
                BlockPos anchorPos = BlockPos.ofFloored(pending.x(), pending.y(), pending.z());
                if (anchorWorld.getChunkManager().isChunkLoaded(
                        anchorPos.getX() >> 4,
                        anchorPos.getZ() >> 4
                )) {
                    double angle = anchorWorld.getTime() * 0.18;
                    anchorWorld.spawnParticles(
                            ParticleTypes.PORTAL,
                            pending.x() + Math.cos(angle) * 0.22,
                            pending.y() + 0.18,
                            pending.z() + Math.sin(angle) * 0.22,
                            1,
                            0.02, 0.04, 0.02,
                            0.0
                    );
                }
            }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
            if (player != null && remainingMillis > 0L) {
                announceCountdown(player, remainingMillis);
            }

            if (remainingMillis > 0L || player == null) {
                continue;
            }

            long nextAttempt = NEXT_RETURN_ATTEMPT.getOrDefault(playerUuid, 0L);
            if (now < nextAttempt) {
                continue;
            }

            ReturnResult result = performReturn(server, player, pending);
            if (result == ReturnResult.SUCCESS) {
                state.removePending(playerUuid);
                clearTransientState(playerUuid);
            } else {
                int failures = Math.min(30, BLOCKED_FAILURES.getOrDefault(playerUuid, 0) + 1);
                BLOCKED_FAILURES.put(playerUuid, failures);
                long retryDelay = result == ReturnResult.DIMENSION_MISSING
                        ? BLOCKED_RETRY_MAX_MILLIS
                        : Math.min(
                                BLOCKED_RETRY_MAX_MILLIS,
                                BLOCKED_RETRY_BASE_MILLIS << Math.min(5, failures - 1)
                        );
                NEXT_RETURN_ATTEMPT.put(playerUuid, now + retryDelay);
                long nextNotice = NEXT_BLOCKED_NOTICE.getOrDefault(playerUuid, 0L);
                if (now >= nextNotice) {
                    String messageKey = result == ReturnResult.DIMENSION_MISSING
                            ? "message.mythicrpg.temporal_machine.dimension_missing"
                            : "message.mythicrpg.temporal_machine.return_blocked";
                    player.sendMessage(Text.translatable(messageKey).formatted(Formatting.RED), true);
                    NEXT_BLOCKED_NOTICE.put(playerUuid, now + BLOCKED_RETRY_NOTICE_MILLIS);
                }
            }
        }
    }

    private static void announceCountdown(ServerPlayerEntity player, long remainingMillis) {
        int seconds = (int) Math.max(1L, (remainingMillis + 999L) / 1_000L);
        if (seconds != 10 && seconds != 5 && seconds != 3 && seconds != 2 && seconds != 1) {
            return;
        }
        Integer previous = LAST_COUNTDOWN_SECOND.put(player.getUuid(), seconds);
        if (previous != null && previous == seconds) {
            return;
        }

        player.sendMessage(
                Text.translatable("message.mythicrpg.temporal_machine.countdown", seconds)
                        .formatted(seconds <= 3 ? Formatting.GOLD : Formatting.LIGHT_PURPLE),
                true
        );
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS,
                0.45F,
                0.9F + (10 - seconds) * 0.035F
        );
    }

    private static ReturnResult performReturn(
            MinecraftServer server,
            ServerPlayerEntity player,
            TemporalReturnState.PendingReturn pending
    ) {
        ServerWorld world = server.getWorld(pending.dimension());
        if (world == null) {
            return ReturnResult.DIMENSION_MISSING;
        }

        BlockPos exact = BlockPos.ofFloored(pending.x(), pending.y(), pending.z());
        Optional<BlockPos> safe = findSafe(world, exact);
        boolean relocated = safe.isPresent() && !safe.get().equals(exact);
        boolean forced = safe.isEmpty();
        Optional<BlockPos> destinationResult = safe.isPresent()
                ? safe
                : forceExact(world, exact);
        if (destinationResult.isEmpty()) {
            return ReturnResult.BLOCKED;
        }
        BlockPos destination = destinationResult.get();

        ServerWorld departureWorld = player.getServerWorld();
        departureWorld.spawnParticles(
                ParticleTypes.REVERSE_PORTAL,
                player.getX(),
                player.getBodyY(0.5),
                player.getZ(),
                24,
                0.35, 0.55, 0.35,
                0.08
        );
        departureWorld.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(),
                SoundCategory.PLAYERS,
                0.8F,
                1.35F
        );

        player.teleport(
                world,
                destination.getX() + 0.5,
                destination.getY(),
                destination.getZ() + 0.5,
                pending.yaw(),
                pending.pitch()
        );

        world.spawnParticles(
                ParticleTypes.PORTAL,
                destination.getX() + 0.5,
                destination.getY() + 0.8,
                destination.getZ() + 0.5,
                36,
                0.42, 0.65, 0.42,
                0.12
        );
        world.spawnParticles(
                new DustParticleEffect(TEMPORAL_COLOR, 0.9F),
                destination.getX() + 0.5,
                destination.getY() + 0.55,
                destination.getZ() + 0.5,
                12,
                0.3, 0.4, 0.3,
                0.02
        );
        world.playSound(
                null,
                destination,
                SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(),
                SoundCategory.PLAYERS,
                1.0F,
                1.15F
        );

        if (relocated) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.temporal_machine.returned_safe")
                            .formatted(Formatting.YELLOW),
                    true
            );
        } else if (forced) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.temporal_machine.returned_forced")
                            .formatted(Formatting.GOLD),
                    true
            );
        } else {
            player.sendMessage(Text.translatable("message.mythicrpg.temporal_machine.returned")
                    .formatted(Formatting.AQUA), true);
        }
        return ReturnResult.SUCCESS;
    }

    private enum ReturnResult {
        SUCCESS,
        BLOCKED,
        DIMENSION_MISSING
    }

    private static void spawnAnchorFeedback(ServerWorld world, Vec3d center, int level) {
        ArchaeologyPolishEffects.spawnHorizontalRing(
                world,
                new DustParticleEffect(TEMPORAL_COLOR, 0.72F),
                center,
                0.8 + level * 0.08,
                20 + level * 2,
                0.08
        );
        world.spawnParticles(
                ParticleTypes.PORTAL,
                center.x,
                center.y + 0.55,
                center.z,
                18,
                0.3, 0.45, 0.3,
                0.08
        );
    }

    private static Optional<BlockPos> findSafe(ServerWorld world, BlockPos origin) {
        for (BlockPos offset : SAFE_OFFSETS) {
            BlockPos pos = origin.add(offset);
            if (isSafe(world, pos)) {
                return Optional.of(pos.toImmutable());
            }
        }
        return Optional.empty();
    }

    private static boolean isSafe(ServerWorld world, BlockPos feet) {
        if (!world.getWorldBorder().contains(feet)
                || feet.getY() <= world.getBottomY()
                || feet.getY() + 1 >= world.getTopY()) {
            return false;
        }
        return isClear(world, feet)
                && isClear(world, feet.up())
                && world.getBlockState(feet.down()).isSideSolidFullSquare(
                        world,
                        feet.down(),
                        net.minecraft.util.math.Direction.UP
                );
    }

    private static boolean isClear(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                && world.getFluidState(pos).isEmpty();
    }

    /**
     * Opens exactly the two player blocks at the recorded coordinates without
     * creating drops. Protected BlockEntities and unbreakable blocks are never
     * destroyed; in that exceptional case the pending return is kept and retried.
     */
    private static Optional<BlockPos> forceExact(ServerWorld world, BlockPos exact) {
        if (!world.getWorldBorder().contains(exact)
                || exact.getY() < world.getBottomY()
                || exact.getY() + 1 >= world.getTopY()) {
            return Optional.empty();
        }
        BlockPos head = exact.up();
        BlockState feetState = world.getBlockState(exact);
        BlockState headState = world.getBlockState(head);
        if (!canClear(world, exact, feetState) || !canClear(world, head, headState)) {
            return Optional.empty();
        }

        int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS;
        boolean clearedFeet = feetState.isAir()
                || world.setBlockState(exact, Blocks.AIR.getDefaultState(), flags);
        if (!clearedFeet) {
            return Optional.empty();
        }
        boolean clearedHead = headState.isAir()
                || world.setBlockState(head, Blocks.AIR.getDefaultState(), flags);
        if (!clearedHead) {
            if (!feetState.isAir()) {
                world.setBlockState(exact, feetState, flags);
            }
            return Optional.empty();
        }

        return isClear(world, exact) && isClear(world, head)
                ? Optional.of(exact.toImmutable())
                : Optional.empty();
    }

    private static boolean canClear(ServerWorld world, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity == null && state.getHardness(world, pos) >= 0.0F;
    }

    private static List<BlockPos> buildSafeOffsets() {
        ArrayList<BlockPos> offsets = new ArrayList<>();
        int radiusSquared = SAFE_RADIUS * SAFE_RADIUS;
        for (int dx = -SAFE_RADIUS; dx <= SAFE_RADIUS; dx++) {
            for (int dy = -SAFE_RADIUS; dy <= SAFE_RADIUS; dy++) {
                for (int dz = -SAFE_RADIUS; dz <= SAFE_RADIUS; dz++) {
                    int distanceSquared = dx * dx + dy * dy + dz * dz;
                    if (distanceSquared <= radiusSquared) {
                        offsets.add(new BlockPos(dx, dy, dz));
                    }
                }
            }
        }
        offsets.sort(Comparator
                .comparingInt((BlockPos pos) -> pos.getX() * pos.getX()
                        + pos.getY() * pos.getY()
                        + pos.getZ() * pos.getZ())
                .thenComparingInt(pos -> Math.abs(pos.getY()))
                .thenComparingInt(BlockPos::getY));
        return List.copyOf(offsets);
    }
}
