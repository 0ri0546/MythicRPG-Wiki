package com.mythicrpg.mining.archaeology;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Player-centric fossil clue renderer.
 *
 * The old architecture ticked every loaded Fossil BlockEntity forever. This
 * manager instead scans only loaded chunks close to players who own the perk,
 * once per second.
 */
public final class FossilHintManager {

    private static final int INTERVAL_TICKS = 20;
    private static final double RANGE = 12.0D;
    private static final double RANGE_SQUARED = RANGE * RANGE;
    private static final DustParticleEffect[] HINT_PARTICLES = java.util.Arrays.stream(FossilRarity.values())
            .map(rarity -> new DustParticleEffect(rarity.particleColor(), 0.8F))
            .toArray(DustParticleEffect[]::new);

    private FossilHintManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld overworld = server.getOverworld();
            if (Math.floorMod(overworld.getTime(), INTERVAL_TICKS) != 0L) {
                return;
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!(player.getWorld() instanceof ServerWorld world)
                        || !SkillTreeManager.hasBonus(
                                player,
                                SkillType.MINING,
                                BonusType.FOSSIL_EXCAVATION
                        )) {
                    continue;
                }
                emitNearbyHints(world, player);
            }
        });
    }

    private static void emitNearbyHints(ServerWorld world, ServerPlayerEntity player) {
        int minChunkX = ((int) Math.floor(player.getX() - RANGE)) >> 4;
        int maxChunkX = ((int) Math.floor(player.getX() + RANGE)) >> 4;
        int minChunkZ = ((int) Math.floor(player.getZ() - RANGE)) >> 4;
        int maxChunkZ = ((int) Math.floor(player.getZ() + RANGE)) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }
                WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof FossilBlockEntity fossil)) {
                        continue;
                    }
                    BlockPos pos = fossil.getPos();
                    if (player.squaredDistanceTo(
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D
                    ) > RANGE_SQUARED) {
                        continue;
                    }

                    fossil.registerSiteIfNeeded(world);
                    Direction exposedFace = fossil.findExposedFace(world);
                    if (exposedFace == null) {
                        continue;
                    }
                    spawnHint(world, player, fossil, exposedFace);
                }
            }
        }
    }

    private static void spawnHint(
            ServerWorld world,
            ServerPlayerEntity player,
            FossilBlockEntity fossil,
            Direction exposedFace
    ) {
        BlockPos pos = fossil.getPos();
        double x = pos.getX() + 0.5D + exposedFace.getOffsetX() * 0.52D;
        double y = pos.getY() + 0.5D + exposedFace.getOffsetY() * 0.52D;
        double z = pos.getZ() + 0.5D + exposedFace.getOffsetZ() * 0.52D;
        double jitterA = (world.getRandom().nextDouble() - 0.5D) * 0.5D;
        double jitterB = (world.getRandom().nextDouble() - 0.5D) * 0.5D;
        if (exposedFace.getAxis() == Direction.Axis.X) {
            y += jitterA;
            z += jitterB;
        } else if (exposedFace.getAxis() == Direction.Axis.Y) {
            x += jitterA;
            z += jitterB;
        } else {
            x += jitterA;
            y += jitterB;
        }

        world.spawnParticles(
                player,
                HINT_PARTICLES[fossil.rarity().ordinal()],
                false,
                x, y, z,
                1,
                0.01D, 0.01D, 0.01D,
                0.0D
        );
    }
}
