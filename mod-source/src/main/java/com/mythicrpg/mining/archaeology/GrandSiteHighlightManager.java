package com.mythicrpg.mining.archaeology;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Keeps grand-site discovery and barrel state tracking.
 * The old green whole-site synchronization was test-only and is deliberately removed.
 */
public final class GrandSiteHighlightManager {

    private static final int DISCOVERY_RADIUS = 24;
    private static final int DISCOVERY_CHECK_INTERVAL_TICKS = 20;

    private GrandSiteHighlightManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld overworld = server.getWorld(World.OVERWORLD);
            if (overworld == null
                    || Math.floorMod(overworld.getTime(), DISCOVERY_CHECK_INTERVAL_TICKS) != 0) {
                return;
            }

            GrandFossilSiteState state = GrandFossilSiteState.get(server);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!player.getWorld().getRegistryKey().equals(World.OVERWORLD)) {
                    continue;
                }

                state.findNearestWithin(
                        player.getBlockPos(),
                        DISCOVERY_RADIUS,
                        null,
                        null,
                        true
                ).filter(GrandFossilSiteState.GrandSiteRecord::barrelPresent)
                        .ifPresent(site -> refreshBarrelState(overworld, state, site));

                Optional<GrandFossilSiteState.GrandSiteRecord> nearest = state.findNearestWithin(
                        player.getBlockPos(),
                        DISCOVERY_RADIUS,
                        null,
                        GrandSiteStatus.GENERATED,
                        true
                );
                if (nearest.isEmpty()) {
                    continue;
                }

                GrandFossilSiteState.GrandSiteRecord site = nearest.get();
                if (site.center().getSquaredDistance(player.getBlockPos())
                        > (double) DISCOVERY_RADIUS * DISCOVERY_RADIUS) {
                    continue;
                }

                state.markDiscovered(site.id());
                player.sendMessage(
                        Text.translatable(
                                site.specialRollSucceeded()
                                        ? "message.mythicrpg.grand_site.discovered_special"
                                        : "message.mythicrpg.grand_site.discovered"
                        ).formatted(site.specialRollSucceeded() ? Formatting.GOLD : Formatting.AQUA),
                        true
                );
                overworld.spawnParticles(
                        ParticleTypes.DUST_PLUME,
                        player.getX(),
                        player.getBodyY(0.45),
                        player.getZ(),
                        site.specialRollSucceeded() ? 10 : 6,
                        0.35,
                        0.4,
                        0.35,
                        0.025
                );
                overworld.playSound(
                        null,
                        player.getBlockPos(),
                        SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                        SoundCategory.PLAYERS,
                        0.55F,
                        site.specialRollSucceeded() ? 1.35F : 1.05F
                );
            }
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, blockState, blockEntity) -> {
            if (world.isClient() || !world.getRegistryKey().equals(World.OVERWORLD)) {
                return;
            }
            GrandFossilSiteState state = GrandFossilSiteState.get(world.getServer());
            state.siteByBarrelPosition(pos).ifPresent(site -> {
                if (site.barrelPos().equals(pos)) {
                    state.markBarrelRemoved(site.id());
                }
            });
        });
    }

    private static void refreshBarrelState(
            ServerWorld world,
            GrandFossilSiteState state,
            GrandFossilSiteState.GrandSiteRecord site
    ) {
        BlockPos barrelPos = site.barrelPos();
        if (!world.getChunkManager().isChunkLoaded(
                barrelPos.getX() >> 4,
                barrelPos.getZ() >> 4
        )) {
            return;
        }
        if (!world.getBlockState(barrelPos).isOf(Blocks.BARREL)) {
            state.markBarrelRemoved(site.id());
        }
    }
}
