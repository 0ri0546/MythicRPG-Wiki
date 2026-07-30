package com.mythicrpg.traveling;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.core.PlayerCooldownManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Formatting;

import java.util.Optional;
import java.util.UUID;

public final class LandMountManager {
    public static final float MAX_ADOPTION_HEALTH_RATIO = 0.25F;
    public static final float ADOPTION_HEAL_RATIO = 0.50F;
    public static final int UNMOUNTED_WANDER_RADIUS = 10;
    private static final double RETURN_TO_ANCHOR_SPEED = 1.0D;
    private static final int FEEDBACK_COOLDOWN_TICKS = 20;
    private static final float HEAL_AMOUNT = 1.0F;

    private LandMountManager() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof MobEntity mob)
                    || !(mob instanceof LandMountDataAccess access)
                    || !access.mythicrpg$isAdoptedLandMount()) {
                return;
            }

            LandMountType.fromEntity(mob).ifPresent(type -> {
                ItemStack returnedSaddle = new ItemStack(getSaddleItem(type));
                MountSaddleData.setDistance(returnedSaddle, access.mythicrpg$getTravelDistance());
                mob.dropStack(returnedSaddle);
                sendMountDeathMessage(mob, access, type);
            });
        });

        MythicRPG.LOGGER.info("Registering Traveling land mount manager");
    }

    public static ActionResult tryAdopt(
            PlayerEntity player,
            MobEntity mob,
            ItemStack saddleStack,
            LandMountType expectedType
    ) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.FAIL;
        }

        if (!expectedType.matches(mob)) {
            return ActionResult.FAIL;
        }

        LandMountDataAccess access = (LandMountDataAccess) mob;

        if (access.mythicrpg$isAdoptedLandMount()) {
            if (isOwner(mob, player)) {
                sendFeedback(player, Text.translatable("message.mythicrpg.land_mount.already_owned")
                        .formatted(Formatting.YELLOW));
            } else {
                sendFeedback(player, Text.translatable(
                        "message.mythicrpg.land_mount.owned_by_other",
                        access.mythicrpg$getLandMountOwnerName()
                ).formatted(Formatting.RED));
            }

            return ActionResult.FAIL;
        }

        if (!TravelingBonusCache.hasBonus(
                serverPlayer,
                expectedType.requiredBonus()
        )) {
            sendFeedback(player, Text.translatable(
                    "message.mythicrpg.land_mount.locked",
                    expectedType.requiredPerkName()
            ).formatted(Formatting.RED));
            return ActionResult.FAIL;
        }

        if (BaronMobManager.isBaron(mob)) {
            sendFeedback(player, Text.translatable("message.mythicrpg.land_mount.baron")
                    .formatted(Formatting.RED));
            return ActionResult.FAIL;
        }

        if (mob.isBaby()) {
            sendFeedback(player, Text.translatable("message.mythicrpg.land_mount.adult_only")
                    .formatted(Formatting.RED));
            return ActionResult.FAIL;
        }

        float maximumAdoptionHealth = mob.getMaxHealth() * MAX_ADOPTION_HEALTH_RATIO;

        if (mob.getHealth() > maximumAdoptionHealth) {
            sendFeedback(player, Text.translatable(
                    "message.mythicrpg.land_mount.too_healthy",
                    Math.round(MAX_ADOPTION_HEALTH_RATIO * 100.0F)
            ).formatted(Formatting.RED));
            return ActionResult.FAIL;
        }

        String ownerName = player.getName().getString();
        access.mythicrpg$setLandMountOwner(player.getUuid(), ownerName);
        access.mythicrpg$setLandMountAnchor(mob.getBlockX(), mob.getBlockZ());
        access.mythicrpg$setTravelDistance(MountSaddleData.getDistance(saddleStack));

        mob.setPersistent();
        mob.setHealth(mob.getMaxHealth() * ADOPTION_HEAL_RATIO);
        mob.getNavigation().stop();
        prepareAdoptedMount(mob);
        refreshPresentation(mob);

        if (!player.getAbilities().creativeMode) {
            saddleStack.decrement(1);
        }

        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    ParticleTypes.HEART,
                    mob.getX(),
                    mob.getBodyY(0.65D),
                    mob.getZ(),
                    8,
                    0.45D,
                    0.35D,
                    0.45D,
                    0.05D
            );

            serverWorld.playSound(
                    null,
                    mob.getBlockPos(),
                    SoundEvents.ENTITY_HORSE_SADDLE,
                    SoundCategory.NEUTRAL,
                    1.0F,
                    1.0F
            );
        }

        player.sendMessage(
                Text.translatable(
                        "message.mythicrpg.land_mount.adopted",
                        expectedType.displayName()
                ).formatted(Formatting.GREEN),
                true
        );

        return ActionResult.SUCCESS;
    }

    /**
     * Routes entity classes that override Minecraft's normal right-click method
     * back through the shared MythicRPG adoption and mounting logic.
     *
     * @param blockVanillaWhenAdopted true for merchants: once adopted, every
     *                                interaction is consumed so trading cannot open.
     */
    public static ActionResult handleSpecialInteraction(
            MobEntity mob,
            PlayerEntity player,
            Hand hand,
            boolean blockVanillaWhenAdopted
    ) {
        ItemStack heldStack = player.getStackInHand(hand);

        if (heldStack.getItem() instanceof AdoptionSaddleItem saddleItem) {
            return saddleItem.useOnEntity(heldStack, player, mob, hand);
        }

        ActionResult result = handleAdoptedInteraction(mob, player, heldStack);
        if (result != ActionResult.PASS) {
            return result;
        }

        if (blockVanillaWhenAdopted && isAdoptedMount(mob)) {
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    public static ActionResult handleAdoptedInteraction(
            MobEntity mob,
            PlayerEntity player,
            ItemStack heldStack
    ) {
        if (!(mob instanceof LandMountDataAccess access)
                || !access.mythicrpg$isAdoptedLandMount()) {
            return ActionResult.PASS;
        }

        Optional<LandMountType> mountType = LandMountType.fromEntity(mob);
        boolean breedingItem = mob instanceof AnimalEntity animal
                && animal.isBreedingItem(heldStack);
        boolean healingItem = breedingItem || mountType
                .map(type -> type.isHealingItem(heldStack))
                .orElse(false);

        if (healingItem) {
            if (!isOwner(mob, player)) {
                if (!mob.getWorld().isClient) {
                    sendFeedback(player, Text.translatable(
                            "message.mythicrpg.land_mount.owned_by_other",
                            access.mythicrpg$getLandMountOwnerName()
                    ).formatted(Formatting.RED));
                }
                return ActionResult.FAIL;
            }

            if (mob.getHealth() >= mob.getMaxHealth()) {
                if (!mob.getWorld().isClient) {
                    sendFeedback(player, Text.translatable(
                            "message.mythicrpg.land_mount.full_health"
                    ).formatted(Formatting.YELLOW));
                }
                return ActionResult.FAIL;
            }

            if (!mob.getWorld().isClient) {
                mob.heal(HEAL_AMOUNT);

                if (!player.getAbilities().creativeMode) {
                    heldStack.decrement(1);
                }

                if (mob.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.playSound(
                            null,
                            mob.getBlockPos(),
                            SoundEvents.ENTITY_GENERIC_EAT,
                            SoundCategory.NEUTRAL,
                            0.7F,
                            1.1F + mob.getRandom().nextFloat() * 0.15F
                    );
                    serverWorld.spawnParticles(
                            ParticleTypes.HEART,
                            mob.getX(),
                            mob.getBodyY(0.65D),
                            mob.getZ(),
                            3,
                            0.25D,
                            0.20D,
                            0.25D,
                            0.02D
                    );
                }
            }

            return ActionResult.success(mob.getWorld().isClient);
        }

        if (!heldStack.isEmpty() || player.shouldCancelInteraction()) {
            return ActionResult.PASS;
        }

        if (!isOwner(mob, player)) {
            if (!mob.getWorld().isClient) {
                sendFeedback(player, Text.translatable(
                        "message.mythicrpg.land_mount.owned_by_other",
                        access.mythicrpg$getLandMountOwnerName()
                ).formatted(Formatting.RED));
            }

            return ActionResult.FAIL;
        }

        if (mob.hasPassengers()) {
            return ActionResult.FAIL;
        }

        if (!mob.getWorld().isClient) {
            player.startRiding(mob, true);
            mob.getNavigation().stop();
            mob.setTarget(null);
        }

        return ActionResult.success(mob.getWorld().isClient);
    }

    public static boolean isOwner(MobEntity mob, PlayerEntity player) {
        if (!(mob instanceof LandMountDataAccess access)) {
            return false;
        }

        return access.mythicrpg$getLandMountOwnerUuid()
                .map(player.getUuid()::equals)
                .orElse(false);
    }

    public static boolean isAdoptedMount(LivingEntity entity) {
        return entity instanceof MobEntity
                && entity instanceof LandMountDataAccess access
                && access.mythicrpg$isAdoptedLandMount();
    }

    public static boolean isFlyingMount(MobEntity mob) {
        return LandMountType.fromEntity(mob)
                .map(LandMountType::isFlying)
                .orElse(false);
    }

    public static void refreshPresentation(MobEntity mob) {
        if (!(mob instanceof LandMountDataAccess access)
                || !access.mythicrpg$isAdoptedLandMount()) {
            return;
        }

        String ownerName = access.mythicrpg$getLandMountOwnerName();
        mob.setCustomName(Text.translatable(
                "entity.mythicrpg.land_mount.owned_by",
                Text.literal(ownerName)
        ));
        mob.setCustomNameVisible(true);
        mob.setPersistent();
        prepareAdoptedMount(mob);
    }

    public static void prepareAdoptedMount(MobEntity mob) {
        if (!(mob instanceof LandMountDataAccess access)
                || !access.mythicrpg$isAdoptedLandMount()) {
            return;
        }

        mob.setPersistent();
        maintainAdoptedMountState(mob);

        if (mob instanceof PhantomEntity) {
            mob.setAiDisabled(true);
            mob.getNavigation().stop();
            mob.extinguish();
            mob.fallDistance = 0.0F;
        }
    }

    /**
     * Keeps species-specific vanilla behavior from reactivating after adoption.
     * This method is intentionally idempotent: calling it repeatedly does not
     * change passive wandering or the mount's normal movement.
     */
    public static void maintainAdoptedMountState(MobEntity mob) {
        if (!(mob instanceof LandMountDataAccess access)
                || !access.mythicrpg$isAdoptedLandMount()) {
            return;
        }

        if (mob.getTarget() != null) {
            mob.setTarget(null);
        }
        if (mob.isAttacking()) {
            mob.setAttacking(false);
        }

        if (mob instanceof AnimalEntity animal) {
            animal.resetLoveTicks();
        }

        // Brain-driven mobs can remember an attack even after setTarget(null).
        // Forget only hostile/breeding memories; passive wandering is preserved.
        mob.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
        mob.getBrain().forget(MemoryModuleType.ANGRY_AT);
        mob.getBrain().forget(MemoryModuleType.HURT_BY);
        mob.getBrain().forget(MemoryModuleType.HURT_BY_ENTITY);
        mob.getBrain().forget(MemoryModuleType.NEAREST_ATTACKABLE);
        mob.getBrain().forget(MemoryModuleType.ROAR_TARGET);
        mob.getBrain().forget(MemoryModuleType.BREED_TARGET);

        if (mob instanceof GoatEntity goat) {
            suppressGoatRam(goat);
        }

        if (mob instanceof HoglinEntity hoglin) {
            hoglin.setImmuneToZombification(true);
        }

        if (mob instanceof PolarBearEntity polarBear) {
            polarBear.setAngerTime(0);
            polarBear.setAngryAt(null);
            polarBear.setWarning(false);
        }

        if (mob instanceof EndermanEntity enderman) {
            enderman.setAngerTime(0);
            enderman.setAngryAt(null);
            enderman.setCarriedBlock(null);
        }

        if (mob instanceof BeeEntity bee) {
            bee.setAngerTime(0);
            bee.setAngryAt(null);
        }

        if (mob instanceof GhastEntity ghast) {
            ghast.setShooting(false);
        }

        if (mob instanceof MerchantEntity merchant) {
            merchant.setCustomer(null);
        }

        if (mob instanceof WanderingTraderEntity trader) {
            trader.setDespawnDelay(Integer.MAX_VALUE);
        }
    }

    public static void refreshOwnerNameIfOnline(MobEntity mob) {
        if (!(mob.getWorld() instanceof ServerWorld serverWorld)
                || !(mob instanceof LandMountDataAccess access)) {
            return;
        }

        Optional<UUID> ownerUuid = access.mythicrpg$getLandMountOwnerUuid();

        if (ownerUuid.isEmpty()) {
            return;
        }

        ServerPlayerEntity owner = serverWorld.getServer()
                .getPlayerManager()
                .getPlayer(ownerUuid.get());

        if (owner == null) {
            return;
        }

        String currentName = owner.getName().getString();

        if (!currentName.equals(access.mythicrpg$getLandMountOwnerName())) {
            access.mythicrpg$setLandMountOwner(owner.getUuid(), currentName);
            refreshPresentation(mob);
        }
    }

    public static void keepNearAnchor(MobEntity mob) {
        if (!(mob instanceof LandMountDataAccess access)
                || !access.mythicrpg$isAdoptedLandMount()) {
            return;
        }

        if (!access.mythicrpg$hasLandMountAnchor()) {
            access.mythicrpg$setLandMountAnchor(mob.getBlockX(), mob.getBlockZ());
        }

        int anchorX = access.mythicrpg$getLandMountAnchorX();
        int anchorZ = access.mythicrpg$getLandMountAnchorZ();

        // The Y coordinate follows the mount so only horizontal distance is restricted.
        // Reuse the existing target while it is still correct instead of allocating
        // a new BlockPos every tick for every unmounted adopted mount.
        int anchorY = mob.getBlockY();
        BlockPos currentTarget = mob.getPositionTarget();
        boolean targetChanged = !mob.hasPositionTarget()
                || currentTarget.getX() != anchorX
                || currentTarget.getY() != anchorY
                || currentTarget.getZ() != anchorZ
                || mob.getPositionTargetRange() != UNMOUNTED_WANDER_RADIUS;

        if (targetChanged) {
            mob.setPositionTarget(
                    new BlockPos(anchorX, anchorY, anchorZ),
                    UNMOUNTED_WANDER_RADIUS
            );
        }

        int deltaX = Math.abs(mob.getBlockX() - anchorX);
        int deltaZ = Math.abs(mob.getBlockZ() - anchorZ);

        if (deltaX <= UNMOUNTED_WANDER_RADIUS
                && deltaZ <= UNMOUNTED_WANDER_RADIUS) {
            return;
        }

        if (isFlyingMount(mob)) {
            FlyingMountController.returnToAnchor(mob, anchorX, anchorZ);
            return;
        }

        // A failed path leaves navigation idle. Retry at most twice per second
        // instead of asking Minecraft to recalculate the same impossible path every tick.
        if (mob.getNavigation().isIdle() && mob.age % 10 == 0) {
            mob.getNavigation().startMovingTo(
                    anchorX + 0.5D,
                    mob.getY(),
                    anchorZ + 0.5D,
                    RETURN_TO_ANCHOR_SPEED
            );
        }
    }

    public static void suppressGoatRam(GoatEntity goat) {
        goat.getBrain().forget(MemoryModuleType.RAM_TARGET);
        goat.getBrain().remember(MemoryModuleType.RAM_COOLDOWN_TICKS, 200);
    }

    public static void sendFeedback(PlayerEntity player, Text message) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        if (PlayerCooldownManager.tryUse(
                serverPlayer,
                "land_mount_feedback",
                FEEDBACK_COOLDOWN_TICKS
        )) {
            player.sendMessage(message, true);
        }
    }

    public static void playMountedSound(MobEntity mob) {
        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(
                    null,
                    mob.getBlockPos(),
                    SoundEvents.ENTITY_HORSE_SADDLE,
                    SoundCategory.NEUTRAL,
                    0.45F,
                    1.15F
            );
        }
    }

    public static void playDismountedSound(MobEntity mob) {
        if (mob.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.playSound(
                    null,
                    mob.getBlockPos(),
                    SoundEvents.ENTITY_HORSE_STEP,
                    SoundCategory.NEUTRAL,
                    0.35F,
                    1.25F
            );
        }
    }

    private static void sendMountDeathMessage(
            MobEntity mob,
            LandMountDataAccess access,
            LandMountType type
    ) {
        if (!(mob.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        access.mythicrpg$getLandMountOwnerUuid().ifPresent(ownerUuid -> {
            ServerPlayerEntity owner = serverWorld.getServer()
                    .getPlayerManager()
                    .getPlayer(ownerUuid);

            if (owner == null) {
                return;
            }

            int variant = mob.getRandom().nextInt(3) + 1;
            owner.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.land_mount.death." + variant,
                            type.displayName()
                    ).formatted(Formatting.LIGHT_PURPLE),
                    false
            );
        });
    }

    private static Item getSaddleItem(LandMountType type) {
        return net.minecraft.registry.Registries.ITEM.get(
                Identifier.of(MythicRPG.MOD_ID, type.id() + "_saddle")
        );
    }
}
