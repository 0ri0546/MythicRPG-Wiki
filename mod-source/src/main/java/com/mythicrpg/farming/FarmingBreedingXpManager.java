package com.mythicrpg.farming;

import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.SkillXpManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class FarmingBreedingXpManager {
    private static final int BREEDING_XP = 8;
    private static final int MEMORY_DURATION_TICKS = 20 * 20;
    private static final double BABY_MATCH_RADIUS_SQUARED = 8.0 * 8.0;

    private static final List<PendingBreedingAction> PENDING_BREEDING_ACTIONS = new ArrayList<>();

    private FarmingBreedingXpManager() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            if (!(entity instanceof AnimalEntity animal)) {
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);

            if (!isValidBreedingAttempt(animal, stack)) {
                return ActionResult.PASS;
            }

            rememberBreedingAction(serverPlayer, serverWorld, animal);

            return ActionResult.PASS;
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            if (!(entity instanceof AnimalEntity babyAnimal)) {
                return;
            }

            if (!babyAnimal.isBaby()) {
                return;
            }

            tryGrantBreedingXp(serverWorld, babyAnimal);
        });

        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            if (serverWorld.getTime() % 100 != 0) {
                return;
            }

            cleanupExpiredActions(serverWorld);
        });
    }

    private static boolean isValidBreedingAttempt(AnimalEntity animal, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (animal.isBaby()) {
            return false;
        }

        if (animal.isInLove()) {
            return false;
        }

        if (animal.getBreedingAge() != 0) {
            return false;
        }

        return animal.isBreedingItem(stack);
    }

    private static void rememberBreedingAction(
            ServerPlayerEntity player,
            ServerWorld world,
            AnimalEntity animal
    ) {
        PENDING_BREEDING_ACTIONS.add(new PendingBreedingAction(
                player.getUuid(),
                world,
                animal.getBlockPos(),
                animal.getType(),
                world.getTime() + MEMORY_DURATION_TICKS
        ));
    }

    private static void tryGrantBreedingXp(ServerWorld world, AnimalEntity babyAnimal) {
        PendingBreedingAction action = findBestMatchingAction(world, babyAnimal);

        if (action == null) {
            return;
        }

        ServerPlayerEntity player = world.getServer()
                .getPlayerManager()
                .getPlayer(action.playerUuid());

        if (player == null) {
            PENDING_BREEDING_ACTIONS.remove(action);
            return;
        }

        SkillXpManager.addXp(player, SkillType.FARMING, BREEDING_XP, false);

        world.spawnParticles(
                ParticleTypes.HEART,
                babyAnimal.getX(),
                babyAnimal.getBodyY(0.8),
                babyAnimal.getZ(),
                4,
                0.3,
                0.3,
                0.3,
                0.02
        );

        world.playSound(
                null,
                babyAnimal.getBlockPos(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                0.45f,
                1.5f
        );

        PENDING_BREEDING_ACTIONS.remove(action);
    }

    private static PendingBreedingAction findBestMatchingAction(ServerWorld world, AnimalEntity babyAnimal) {
        long now = world.getTime();

        PendingBreedingAction best = null;
        double bestDistance = Double.MAX_VALUE;

        Iterator<PendingBreedingAction> iterator = PENDING_BREEDING_ACTIONS.iterator();

        while (iterator.hasNext()) {
            PendingBreedingAction action = iterator.next();

            if (action.world() != world) {
                continue;
            }

            if (now > action.expireTick()) {
                iterator.remove();
                continue;
            }

            if (action.entityType() != babyAnimal.getType()) {
                continue;
            }

            double distanceSquared = action.pos().getSquaredDistance(babyAnimal.getBlockPos());

            if (distanceSquared > BABY_MATCH_RADIUS_SQUARED) {
                continue;
            }

            if (distanceSquared < bestDistance) {
                bestDistance = distanceSquared;
                best = action;
            }
        }

        return best;
    }

    private static void cleanupExpiredActions(ServerWorld world) {
        long now = world.getTime();

        PENDING_BREEDING_ACTIONS.removeIf(action ->
                action.world() == world && now > action.expireTick()
        );
    }

    private record PendingBreedingAction(
            UUID playerUuid,
            ServerWorld world,
            BlockPos pos,
            EntityType<?> entityType,
            long expireTick
    ) {
    }
}