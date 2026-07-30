package com.mythicrpg.traveling;

import com.mythicrpg.core.BonusType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityPose;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative two-stage grappling movement for Traveling node 20. */
public final class GrapplingHookManager {
    private static final double VISUAL_SYNC_RADIUS = GrapplingHookConfig.MAX_RANGE_BLOCKS + 64.0D;
    private static final double VISUAL_SYNC_RADIUS_SQUARED = VISUAL_SYNC_RADIUS * VISUAL_SYNC_RADIUS;

    private static final Map<UUID, ActiveGrapple> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> FALL_PROTECTION = new HashMap<>();

    private GrapplingHookManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GrapplingHookManager::tick);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.player.getUuid();
            cancel(server, handler.player, false, null);
            FALL_PROTECTION.remove(playerId);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Map.Entry<UUID, ActiveGrapple> entry : ACTIVE.entrySet()) {
                restorePlayer(server.getPlayerManager().getPlayer(entry.getKey()), entry.getValue());
            }
            ACTIVE.clear();
            FALL_PROTECTION.clear();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ACTIVE.clear();
            FALL_PROTECTION.clear();
        });
    }

    public static boolean tryFire(ServerPlayerEntity player) {
        if (!TravelingBonusCache.hasBonus(
                player,
                BonusType.GRAPPLING_HOOK_CRAFT
        )) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.grappling_hook.locked")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        if (!player.isAlive() || player.isSpectator() || player.hasVehicle()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.grappling_hook.invalid_state")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        if (ACTIVE.containsKey(player.getUuid())) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.grappling_hook.already_active")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        ServerWorld world = player.getServerWorld();
        Vec3d launchOrigin = player.getEyePos();
        Vec3d rayEnd = launchOrigin.add(player.getRotationVec(1.0F)
                .multiply(GrapplingHookConfig.MAX_RANGE_BLOCKS));

        BlockHitResult hit = world.raycast(new RaycastContext(
                launchOrigin,
                rayEnd,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.grappling_hook.no_target")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        BlockState hitState = world.getBlockState(hit.getBlockPos());
        if (hitState.getCollisionShape(world, hit.getBlockPos()).isEmpty()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.grappling_hook.no_target")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        Vec3d destination = findSafeDestination(player, hit);
        if (destination == null) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.grappling_hook.no_safe_destination")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        double pullDistance = player.getPos().distanceTo(destination);
        if (pullDistance <= GrapplingHookConfig.ARRIVAL_DISTANCE) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.grappling_hook.too_close")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        Vec3d anchor = hit.getPos();
        double ropeDistance = launchOrigin.distanceTo(anchor);
        int maximumTicks = Math.max(
                1,
                (int) Math.ceil(
                        (ropeDistance + pullDistance)
                                / GrapplingHookConfig.TRAVEL_SPEED_BLOCKS_PER_TICK
                ) + GrapplingHookConfig.MAX_DURATION_GRACE_TICKS
        );

        ActiveGrapple state = new ActiveGrapple(
                player.getId(),
                world.getRegistryKey(),
                hit.getBlockPos().toImmutable(),
                launchOrigin,
                anchor,
                destination,
                player.hasNoGravity(),
                world.getTime(),
                ropeDistance,
                maximumTicks
        );

        ACTIVE.put(player.getUuid(), state);
        FALL_PROTECTION.remove(player.getUuid());

        broadcastNearby(
                player.getServer(),
                player,
                state,
                GrapplingHookVisualPayload.extending(
                        player.getId(),
                        state.startedAtWorldTick,
                        launchOrigin,
                        anchor,
                        destination
                )
        );

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_FISHING_BOBBER_THROW,
                SoundCategory.PLAYERS,
                0.9F,
                0.8F
        );

        player.sendMessage(
                Text.translatable(
                        "message.mythicrpg.grappling_hook.fired",
                        Math.max(1, (int) Math.round(ropeDistance))
                ).formatted(Formatting.AQUA),
                true
        );

        return true;
    }

    private static void tick(MinecraftServer server) {
        tickFallProtection(server);

        Iterator<Map.Entry<UUID, ActiveGrapple>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveGrapple> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            ActiveGrapple grapple = entry.getValue();

            if (!isStillValid(player, grapple)) {
                restoreAndStop(server, player, grapple);
                iterator.remove();
                continue;
            }

            if (!isAnchorStillSolid(player, grapple)) {
                restoreAndStop(server, player, grapple);
                iterator.remove();
                player.sendMessage(
                        Text.translatable("message.mythicrpg.grappling_hook.anchor_lost")
                                .formatted(Formatting.RED),
                        true
                );
                continue;
            }

            grapple.elapsedTicks++;
            if (grapple.elapsedTicks > grapple.maximumTicks) {
                restoreAndStop(server, player, grapple);
                iterator.remove();
                player.sendMessage(
                        Text.translatable("message.mythicrpg.grappling_hook.cancelled")
                                .formatted(Formatting.RED),
                        true
                );
                continue;
            }

            if (grapple.phase == GrapplePhase.EXTENDING) {
                double travelled = grapple.elapsedTicks
                        * GrapplingHookConfig.TRAVEL_SPEED_BLOCKS_PER_TICK;

                if (travelled < grapple.ropeDistance) {
                    continue;
                }

                beginPull(server, player, grapple);
            }

            tickPull(server, player, grapple, iterator);
        }
    }

    private static void beginPull(
            MinecraftServer server,
            ServerPlayerEntity player,
            ActiveGrapple grapple
    ) {
        grapple.phase = GrapplePhase.PULLING;
        grapple.stalledTicks = 0;

        player.setNoGravity(true);
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.fallDistance = 0.0F;

        broadcastNearby(
                server,
                player,
                grapple,
                GrapplingHookVisualPayload.pulling(
                        player.getId(),
                        grapple.startedAtWorldTick,
                        grapple.launchOrigin,
                        grapple.anchor,
                        grapple.destination
                )
        );

        player.getServerWorld().playSound(
                null,
                grapple.anchorBlockPos,
                SoundEvents.BLOCK_CHAIN_PLACE,
                SoundCategory.PLAYERS,
                0.9F,
                1.1F
        );
        player.sendMessage(
                Text.translatable("message.mythicrpg.grappling_hook.attached")
                        .formatted(Formatting.AQUA),
                true
        );
    }

    private static void tickPull(
            MinecraftServer server,
            ServerPlayerEntity player,
            ActiveGrapple grapple,
            Iterator<Map.Entry<UUID, ActiveGrapple>> iterator
    ) {
        Vec3d remaining = grapple.destination.subtract(player.getPos());
        double remainingDistance = remaining.length();

        if (remainingDistance <= GrapplingHookConfig.ARRIVAL_DISTANCE) {
            finish(server, player, grapple, iterator);
            return;
        }

        double stepLength = Math.min(
                GrapplingHookConfig.TRAVEL_SPEED_BLOCKS_PER_TICK,
                remainingDistance
        );
        Vec3d requestedVelocity = remaining.normalize().multiply(stepLength);
        Box targetBox = player.getBoundingBox().offset(
                requestedVelocity.x,
                requestedVelocity.y,
                requestedVelocity.z
        );

        player.forwardSpeed = 0.0F;
        player.sidewaysSpeed = 0.0F;
        player.upwardSpeed = 0.0F;
        player.setNoGravity(true);
        player.fallDistance = 0.0F;

        if (!player.getServerWorld().isSpaceEmpty(player, targetBox)) {
            grapple.stalledTicks++;
            player.setVelocity(Vec3d.ZERO);
            player.velocityModified = true;
        } else {
            grapple.stalledTicks = 0;
            player.setVelocity(requestedVelocity);
            player.velocityModified = true;
        }

        if (grapple.stalledTicks >= GrapplingHookConfig.MAX_STALLED_TICKS) {
            restoreAndStop(server, player, grapple);
            iterator.remove();
            player.sendMessage(
                    Text.translatable("message.mythicrpg.grappling_hook.blocked")
                            .formatted(Formatting.RED),
                    true
            );
        }
    }

    private static boolean isStillValid(ServerPlayerEntity player, ActiveGrapple grapple) {
        return player != null
                && player.getId() == grapple.playerEntityId
                && player.isAlive()
                && !player.isSpectator()
                && !player.hasVehicle()
                && player.getServerWorld().getRegistryKey().equals(grapple.worldKey)
                && TravelingBonusCache.hasBonus(
                        player,
                        BonusType.GRAPPLING_HOOK_CRAFT
                );
    }

    private static boolean isAnchorStillSolid(ServerPlayerEntity player, ActiveGrapple grapple) {
        BlockState state = player.getServerWorld().getBlockState(grapple.anchorBlockPos);
        return !state.getCollisionShape(player.getServerWorld(), grapple.anchorBlockPos).isEmpty();
    }

    private static void finish(
            MinecraftServer server,
            ServerPlayerEntity player,
            ActiveGrapple grapple,
            Iterator<Map.Entry<UUID, ActiveGrapple>> iterator
    ) {
        restoreAndStop(server, player, grapple);
        iterator.remove();
        FALL_PROTECTION.put(
                player.getUuid(),
                GrapplingHookConfig.POST_ARRIVAL_FALL_PROTECTION_TICKS
        );

        player.getServerWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE,
                SoundCategory.PLAYERS,
                0.8F,
                1.1F
        );
        player.sendMessage(
                Text.translatable("message.mythicrpg.grappling_hook.arrived")
                        .formatted(Formatting.GREEN),
                true
        );
    }

    private static void cancel(
            MinecraftServer server,
            ServerPlayerEntity player,
            boolean showMessage,
            String translationKey
    ) {
        if (player == null) {
            return;
        }

        ActiveGrapple grapple = ACTIVE.remove(player.getUuid());
        if (grapple == null) {
            return;
        }

        restoreAndStop(server, player, grapple);
        if (showMessage && translationKey != null) {
            player.sendMessage(Text.translatable(translationKey).formatted(Formatting.RED), true);
        }
    }

    private static void restoreAndStop(
            MinecraftServer server,
            ServerPlayerEntity player,
            ActiveGrapple grapple
    ) {
        restorePlayer(player, grapple);
        broadcastNearby(server, player, grapple, GrapplingHookVisualPayload.stop(grapple.playerEntityId));
    }

    private static void restorePlayer(ServerPlayerEntity player, ActiveGrapple grapple) {
        if (player == null || player.getId() != grapple.playerEntityId) {
            return;
        }

        player.setNoGravity(grapple.originalNoGravity);
        player.setVelocity(Vec3d.ZERO);
        player.velocityModified = true;
        player.fallDistance = 0.0F;
    }

    private static void broadcastNearby(
            MinecraftServer server,
            ServerPlayerEntity owner,
            ActiveGrapple grapple,
            GrapplingHookVisualPayload payload
    ) {
        if (owner != null && ServerPlayNetworking.canSend(owner, GrapplingHookVisualPayload.ID)) {
            ServerPlayNetworking.send(owner, payload);
        }

        ServerWorld world = server.getWorld(grapple.worldKey);
        if (world == null) {
            return;
        }

        // The whole rope fits within MAX_RANGE_BLOCKS of the anchor. The extra
        // margin keeps the visual stable for spectators without broadcasting to
        // unrelated dimensions or players on the other side of the server.
        for (ServerPlayerEntity receiver : world.getPlayers()) {
            if (owner != null && receiver.getUuid().equals(owner.getUuid())) {
                continue;
            }
            if (receiver.getPos().squaredDistanceTo(grapple.anchor) > VISUAL_SYNC_RADIUS_SQUARED) {
                continue;
            }
            if (ServerPlayNetworking.canSend(receiver, GrapplingHookVisualPayload.ID)) {
                ServerPlayNetworking.send(receiver, payload);
            }
        }
    }

    private static void tickFallProtection(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> iterator = FALL_PROTECTION.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());

            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }

            player.fallDistance = 0.0F;
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }

    private static Vec3d findSafeDestination(ServerPlayerEntity player, BlockHitResult hit) {
        Vec3d candidate = destinationForSurface(player, hit.getPos(), hit.getSide());
        Vec3d towardPlayer = player.getPos().subtract(candidate);

        if (towardPlayer.lengthSquared() < 1.0E-8D) {
            return isSafe(player, candidate) ? candidate : null;
        }

        Vec3d backDirection = towardPlayer.normalize();
        int attempts = (int) Math.ceil(
                GrapplingHookConfig.SAFE_POSITION_SEARCH_DISTANCE
                        / GrapplingHookConfig.SAFE_POSITION_SEARCH_STEP
        );

        for (int index = 0; index <= attempts; index++) {
            Vec3d tested = candidate.add(backDirection.multiply(
                    index * GrapplingHookConfig.SAFE_POSITION_SEARCH_STEP
            ));

            if (isSafe(player, tested)) {
                return tested;
            }
        }

        return null;
    }

    private static Vec3d destinationForSurface(
            ServerPlayerEntity player,
            Vec3d hitPos,
            Direction side
    ) {
        double horizontalOffset = player.getDimensions(EntityPose.STANDING).width() * 0.5D + 0.08D;
        double height = player.getDimensions(EntityPose.STANDING).height();

        return switch (side) {
            case UP -> new Vec3d(hitPos.x, hitPos.y + 0.02D, hitPos.z);
            case DOWN -> new Vec3d(hitPos.x, hitPos.y - height - 0.02D, hitPos.z);
            default -> new Vec3d(
                    hitPos.x + side.getOffsetX() * horizontalOffset,
                    hitPos.y - player.getStandingEyeHeight(),
                    hitPos.z + side.getOffsetZ() * horizontalOffset
            );
        };
    }

    private static boolean isSafe(ServerPlayerEntity player, Vec3d position) {
        Box box = player.getDimensions(EntityPose.STANDING).getBoxAt(position);
        return player.getServerWorld().isSpaceEmpty(player, box);
    }

    private enum GrapplePhase {
        EXTENDING,
        PULLING
    }

    private static final class ActiveGrapple {
        private final int playerEntityId;
        private final RegistryKey<World> worldKey;
        private final BlockPos anchorBlockPos;
        private final Vec3d launchOrigin;
        private final Vec3d anchor;
        private final Vec3d destination;
        private final boolean originalNoGravity;
        private final long startedAtWorldTick;
        private final double ropeDistance;
        private final int maximumTicks;

        private GrapplePhase phase = GrapplePhase.EXTENDING;
        private int elapsedTicks;
        private int stalledTicks;

        private ActiveGrapple(
                int playerEntityId,
                RegistryKey<World> worldKey,
                BlockPos anchorBlockPos,
                Vec3d launchOrigin,
                Vec3d anchor,
                Vec3d destination,
                boolean originalNoGravity,
                long startedAtWorldTick,
                double ropeDistance,
                int maximumTicks
        ) {
            this.playerEntityId = playerEntityId;
            this.worldKey = worldKey;
            this.anchorBlockPos = anchorBlockPos;
            this.launchOrigin = launchOrigin;
            this.anchor = anchor;
            this.destination = destination;
            this.originalNoGravity = originalNoGravity;
            this.startedAtWorldTick = startedAtWorldTick;
            this.ropeDistance = ropeDistance;
            this.maximumTicks = maximumTicks;
        }
    }
}
