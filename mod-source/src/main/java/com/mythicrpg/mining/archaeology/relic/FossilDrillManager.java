package com.mythicrpg.mining.archaeology.relic;

import com.mythicrpg.core.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistent global guard for Fossil Drills.
 *
 * One player may own one active drill at a time across all dimensions. The
 * active position and cooldown survive chunk unloads, reconnects and server
 * restarts.
 */
public final class FossilDrillManager {

    private FossilDrillManager() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                restoreVisibleCooldown(handler.player)
        );
    }

    public static boolean hasActive(MinecraftServer server, UUID owner) {
        FossilDrillState state = FossilDrillState.get(server);
        Optional<FossilDrillState.ActiveDrill> active = state.active(owner);
        if (active.isEmpty()) {
            return false;
        }

        FossilDrillState.ActiveDrill record = active.get();
        ServerWorld world = server.getWorld(record.dimension());
        if (world == null) {
            // Preserve the record while a dimension is unavailable. Silently
            // releasing it would allow a second persistent drill.
            return true;
        }

        int chunkX = record.pos().getX() >> 4;
        int chunkZ = record.pos().getZ() >> 4;
        if (!world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
            return true;
        }

        BlockEntity blockEntity = world.getBlockEntity(record.pos());
        if (blockEntity instanceof FossilDrillBlockEntity drill
                && drill.isProcessing()
                && owner.equals(drill.owner())) {
            return true;
        }

        // The recorded chunk is loaded and no matching active drill exists:
        // this is a confirmed orphan, not a simple chunk unload.
        state.forceRelease(owner);
        return false;
    }

    public static boolean claim(ServerWorld world, UUID owner, BlockPos pos) {
        if (owner == null) {
            return false;
        }
        if (hasActive(world.getServer(), owner)) {
            return FossilDrillState.get(world.getServer())
                    .active(owner)
                    .filter(active -> active.dimension().equals(world.getRegistryKey()))
                    .filter(active -> active.pos().equals(pos))
                    .isPresent();
        }
        return FossilDrillState.get(world.getServer())
                .claim(owner, world.getRegistryKey(), pos);
    }

    /**
     * Reconciles a loaded BlockEntity with the persistent registry. A false
     * result means another drill already owns the player's global slot.
     */
    public static boolean ensureRegistered(ServerWorld world, UUID owner, BlockPos pos) {
        if (owner == null) {
            return false;
        }
        return FossilDrillState.get(world.getServer())
                .claim(owner, world.getRegistryKey(), pos);
    }

    public static void remove(ServerWorld world, UUID owner, BlockPos pos) {
        if (owner == null) {
            return;
        }
        FossilDrillState.get(world.getServer())
                .release(owner, world.getRegistryKey(), pos);
    }

    public static long cooldownRemainingMillis(MinecraftServer server, UUID owner) {
        return FossilDrillState.get(server)
                .cooldownRemainingMillis(owner, System.currentTimeMillis());
    }

    public static void startCooldown(ServerPlayerEntity player, int ticks) {
        int safeTicks = Math.max(0, ticks);
        long until = System.currentTimeMillis() + safeTicks * 50L;
        FossilDrillState.get(player.getServer()).setCooldownUntil(player.getUuid(), until);
        player.getItemCooldownManager().set(ModItems.FOSSIL_DRILL, safeTicks);
    }

    private static void restoreVisibleCooldown(ServerPlayerEntity player) {
        long remainingMillis = cooldownRemainingMillis(player.getServer(), player.getUuid());
        if (remainingMillis <= 0L) {
            return;
        }
        int ticks = (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(1L, (remainingMillis + 49L) / 50L)
        );
        player.getItemCooldownManager().set(ModItems.FOSSIL_DRILL, ticks);
    }
}
