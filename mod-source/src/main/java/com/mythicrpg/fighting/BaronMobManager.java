package com.mythicrpg.fighting;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.core.SkillProgress;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.fighting.barons.BaronEntityQuery;
import com.mythicrpg.fighting.barons.BaronScaling;
import com.mythicrpg.fighting.barons.DiamondBaronBehavior;
import com.mythicrpg.fighting.barons.DrownedKingBaronBehavior;
import com.mythicrpg.fighting.barons.GiantBaronBehavior;
import com.mythicrpg.fighting.barons.HeavyBaronBehavior;
import com.mythicrpg.fighting.barons.RunnerBaronBehavior;
import com.mythicrpg.fighting.barons.StalkerBaronBehavior;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Optional;

public final class BaronMobManager {

    private static final String BARON_TAG = "mythicrpg_baron";
    private static final String FORCED_BARON_PENDING_TAG = "mythicrpg_forced_baron_pending";
    private static final String PROMOTION_CHECKED_TAG = "mythicrpg_baron_promotion_checked";

    private static final Identifier BARON_SCALE_MODIFIER_ID =
            Identifier.of(MythicRPG.MOD_ID, "baron_scale");

    private static final double BARON_SCALE_BONUS = 0.10;

    private static final double PLAYER_SEARCH_RADIUS = 96.0;
    private static final double LOW_LEVEL_PROTECTION_RADIUS = 48.0;

    private static final int MIN_FIGHTING_LEVEL = 10;
    private static final List<BaronType> SKELETON_BARON_TYPES =
            List.of(BaronType.DRUID, BaronType.BARRAGE);

    private static final List<BaronType> WITHER_SKELETON_BARON_TYPES =
            List.of(BaronType.STALKER);

    private static final List<BaronType> DROWNED_BARON_TYPES =
            List.of(BaronType.DROWNED_KING, BaronType.NUKE, BaronType.SURVIVOR);

    private static final List<BaronType> ZOMBIE_BARON_TYPES =
            List.of(BaronType.NUKE, BaronType.SURVIVOR);

    private static final List<BaronType> CREEPER_BARON_TYPES =
            List.of(BaronType.HEAVY, BaronType.RUNNER);

    private static final List<BaronType> SPIDER_BARON_TYPES =
            List.of(BaronType.DARKNIGHT, BaronType.THROWER);

    private static final List<BaronType> SLIME_BARON_TYPES =
            List.of(BaronType.GIANT);

    private static final List<BaronType> WITCH_BARON_TYPES =
            List.of(BaronType.ALCHEMIST);

    private static final List<BaronType> BLAZE_BARON_TYPES =
            List.of(BaronType.HOTHEAD);

    private static final List<BaronType> ENDERMAN_BARON_TYPES =
            List.of(BaronType.SWIMMING);

    private static final List<BaronType> GHAST_BARON_TYPES =
            List.of(BaronType.BALLOON);

    private static final List<BaronType> GUARDIAN_BARON_TYPES =
            List.of(BaronType.INFERNO);

    private static final List<BaronType> CHARGING_BARON_TYPES =
            List.of(BaronType.CHARGING);

    private static final List<BaronType> VEX_BARON_TYPES =
            List.of(BaronType.DIAMOND);

    private static final List<BaronType> IRON_GOLEM_BARON_TYPES =
            List.of(BaronType.MOLTEN);

    private static final List<BaronType> SQUID_BARON_TYPES =
            List.of(BaronType.INK);

    private static final List<BaronType> WOLF_BARON_TYPES =
            List.of(BaronType.UNDYING_WOLF);

    private static final List<BaronType> PASSIVE_BARON_TYPES =
            List.of(BaronType.FUGITIVE, BaronType.GOLDEN, BaronType.PANIC);

    private static final List<EntityType<? extends LivingEntity>> LUCKY_BLOCK_BARON_ENTITY_TYPES = List.of(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.SLIME,
            EntityType.WITCH,
            EntityType.DROWNED,
            EntityType.ENDERMAN,
            EntityType.BLAZE,
            EntityType.HOGLIN,
            EntityType.CREEPER,
            EntityType.WITHER_SKELETON,
            EntityType.VEX
    );

    private BaronMobManager() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            tryPromoteToBaron(entity, serverWorld);
        });

        MythicRPG.LOGGER.info("Registering Baron mob manager");
    }

    private static void tryPromoteToBaron(Entity entity, ServerWorld world) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return;
        }

        if (entity instanceof ServerPlayerEntity) {
            return;
        }

        if (isBaron(livingEntity)
                || livingEntity.getCommandTags().contains(FORCED_BARON_PENDING_TAG)) {
            return;
        }

        if (livingEntity.getCommandTags().contains(PROMOTION_CHECKED_TAG)) {
            return;
        }

        // Command tags are persisted in entity NBT. Mark the entity before any early return
        // so an old mob cannot reroll its Baron promotion after a chunk unload/reload.
        livingEntity.addCommandTag(PROMOTION_CHECKED_TAG);

        if (livingEntity.age > 20) {
            return;
        }

        ServerPlayerEntity nearestPlayer = findNearestPlayer(world, livingEntity, PLAYER_SEARCH_RADIUS);
        if (nearestPlayer == null) {
            return;
        }

        int fightingLevel = getFightingLevel(nearestPlayer);
        double chance = getBaronChance(fightingLevel);

        if (chance <= 0.0) {
            return;
        }

        if (fightingLevel >= MIN_FIGHTING_LEVEL
                && hasProtectedLowLevelPlayerNearby(world, livingEntity, nearestPlayer)) {
            return;
        }

        if (world.random.nextDouble() >= chance) {
            return;
        }

        BaronType type = chooseBaronType(livingEntity, world);
        promote(livingEntity, world, fightingLevel, type);
    }

    private static BaronType chooseBaronType(LivingEntity entity, ServerWorld world) {
        List<BaronType> specialTypes = getAvailableSpecialTypes(entity);

        if (specialTypes.isEmpty()) {
            return BaronType.NORMAL;
        }

        if (world.random.nextDouble() < 0.5) {
            return BaronType.NORMAL;
        }

        return specialTypes.get(world.random.nextInt(specialTypes.size()));
    }

    private static List<BaronType> getAvailableSpecialTypes(LivingEntity entity) {
        if (entity instanceof WitherSkeletonEntity) {
            return WITHER_SKELETON_BARON_TYPES;
        }

        if (entity instanceof SkeletonEntity) {
            return SKELETON_BARON_TYPES;
        }

        if (entity instanceof DrownedEntity) {
            return DROWNED_BARON_TYPES;
        }

        if (entity instanceof ZombieEntity) {
            return ZOMBIE_BARON_TYPES;
        }

        if (entity instanceof CreeperEntity) {
            return CREEPER_BARON_TYPES;
        }

        if (entity instanceof SpiderEntity) {
            return SPIDER_BARON_TYPES;
        }

        if (entity instanceof SlimeEntity) {
            return SLIME_BARON_TYPES;
        }

        if (entity instanceof WitchEntity) {
            return WITCH_BARON_TYPES;
        }

        if (entity instanceof BlazeEntity) {
            return BLAZE_BARON_TYPES;
        }

        if (entity instanceof EndermanEntity) {
            return ENDERMAN_BARON_TYPES;
        }

        if (entity instanceof GhastEntity) {
            return GHAST_BARON_TYPES;
        }

        if (entity instanceof GuardianEntity) {
            return GUARDIAN_BARON_TYPES;
        }

        if (entity instanceof RavagerEntity || entity instanceof HoglinEntity) {
            return CHARGING_BARON_TYPES;
        }

        if (entity instanceof VexEntity) {
            return VEX_BARON_TYPES;
        }

        if (entity instanceof IronGolemEntity) {
            return IRON_GOLEM_BARON_TYPES;
        }

        if (entity instanceof SquidEntity) {
            return SQUID_BARON_TYPES;
        }

        if (entity instanceof WolfEntity) {
            return WOLF_BARON_TYPES;
        }

        if (entity instanceof PassiveEntity) {
            return PASSIVE_BARON_TYPES;
        }

        return List.of();
    }

    private static ServerPlayerEntity findNearestPlayer(ServerWorld world, LivingEntity entity, double radius) {
        ServerPlayerEntity nearest = null;
        double bestDistanceSq = radius * radius;

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }

            double distanceSq = player.squaredDistanceTo(entity);

            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                nearest = player;
            }
        }

        return nearest;
    }

    private static boolean hasProtectedLowLevelPlayerNearby(
            ServerWorld world,
            LivingEntity entity,
            ServerPlayerEntity sourcePlayer
    ) {
        double radiusSq = LOW_LEVEL_PROTECTION_RADIUS * LOW_LEVEL_PROTECTION_RADIUS;

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }

            if (player.getUuid().equals(sourcePlayer.getUuid())) {
                continue;
            }

            if (player.squaredDistanceTo(entity) > radiusSq) {
                continue;
            }

            if (getFightingLevel(player) < MIN_FIGHTING_LEVEL) {
                return true;
            }
        }

        return false;
    }

    private static int getFightingLevel(ServerPlayerEntity player) {
        SkillProgress progress = ModAttachments.getProgress(player, SkillType.FIGHTING);
        return progress.getLevel();
    }

    private static double getBaronChance(int fightingLevel) {
        if (fightingLevel < MIN_FIGHTING_LEVEL) {
            return 0.05;
        }

        if (fightingLevel < 25) {
            return 0.2;
        }

        if (fightingLevel < 50) {
            return 0.3;
        }

        if (fightingLevel < 75) {
            return 0.5;
        }

        return 0.7;
    }

    private static void promote(LivingEntity entity, ServerWorld world, int fightingLevel, BaronType type) {
        if (isBaron(entity)) {
            return;
        }

        entity.addCommandTag(BARON_TAG);
        entity.addCommandTag(PROMOTION_CHECKED_TAG);
        entity.addCommandTag(type.tag());
        BaronScaling.setSpawnFightingLevel(entity, fightingLevel);

        applyTypeSpecificPromotionChanges(entity, world, type);
        applyBaronScale(entity);

        double healthMultiplier = BaronScaling.getHealthMultiplier(fightingLevel);
        double speedMultiplier = getSpeedMultiplier(entity, type);

        EntityAttributeInstance maxHealth = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() * healthMultiplier);
            entity.setHealth(entity.getMaxHealth());
        }


        EntityAttributeInstance movementSpeed = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.setBaseValue(movementSpeed.getBaseValue() * speedMultiplier);
        }

        applyInitialTypeEffects(entity, type);

        entity.setCustomName(type.createName(entity.getType().getName()));
        entity.setCustomNameVisible(true);

        world.spawnParticles(
                getSpawnParticle(type),
                entity.getX(),
                entity.getBodyY(0.5),
                entity.getZ(),
                18,
                0.45,
                0.6,
                0.45,
                0.02
        );

        world.playSound(
                null,
                entity.getBlockPos(),
                SoundEvents.ENTITY_WITHER_SPAWN,
                SoundCategory.HOSTILE,
                0.25f,
                1.8f
        );

        BaronEntityQuery.track(entity);
    }

    private static void applyBaronScale(LivingEntity entity) {
        EntityAttributeInstance scale = entity.getAttributeInstance(EntityAttributes.GENERIC_SCALE);

        if (scale == null) {
            return;
        }

        if (scale.getModifier(BARON_SCALE_MODIFIER_ID) != null) {
            return;
        }

        scale.addPersistentModifier(new EntityAttributeModifier(
                BARON_SCALE_MODIFIER_ID,
                BARON_SCALE_BONUS,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));
    }

    private static void applyInitialTypeEffects(LivingEntity entity, BaronType type) {
        if (type == BaronType.FUGITIVE) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, -1, 1, true, true));
        }

        if (type == BaronType.PANIC) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, -1, 1, true, true));
        }

        if (type == BaronType.GOLDEN) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, -1, 0, true, true));
        }

        if (type == BaronType.SURVIVOR) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, -1, 0, true, true));
        }
    }

    private static double getSpeedMultiplier(LivingEntity entity, BaronType type) {
        if (type == BaronType.RUNNER) {
            return 1.55 * BaronScaling.getRunnerSpeedMultiplier(entity);
        }

        if (type == BaronType.FUGITIVE) {
            return 1.35 * BaronScaling.getFugitiveSpeedMultiplier(entity);
        }

        if (type == BaronType.PANIC) {
            return 1.15;
        }

        if (type == BaronType.GOLDEN) {
            return 1.20;
        }

        if (entity instanceof SpiderEntity) {
            return 1.08;
        }

        if (entity instanceof CreeperEntity) {
            return 1.03;
        }

        if (entity instanceof ZombieEntity) {
            return 1.00;
        }

        if (entity instanceof HostileEntity) {
            return 1.05;
        }

        return 1.03;
    }

    private static ParticleEffect getSpawnParticle(BaronType type) {
        return switch (type) {
            case GOLDEN -> ParticleTypes.HAPPY_VILLAGER;
            case PANIC -> ParticleTypes.CLOUD;
            case FUGITIVE -> ParticleTypes.SONIC_BOOM;
            case DRUID -> ParticleTypes.HEART;
            case BARRAGE -> ParticleTypes.CRIT;
            case NUKE -> ParticleTypes.WITCH;
            case SURVIVOR -> ParticleTypes.ANGRY_VILLAGER;
            case NORMAL -> ParticleTypes.SOUL_FIRE_FLAME;
            case DARKNIGHT -> ParticleTypes.SMOKE;
            case GIANT -> ParticleTypes.POOF;
            case ALCHEMIST -> ParticleTypes.WITCH;
            case HOTHEAD -> ParticleTypes.FLAME;
            case SWIMMING -> ParticleTypes.SPLASH;
            case DROWNED_KING -> ParticleTypes.SPLASH;
            case BALLOON -> ParticleTypes.DRAGON_BREATH;
            case CHARGING -> ParticleTypes.ANGRY_VILLAGER;
            case DIAMOND -> ParticleTypes.ENCHANT;
            case STALKER -> ParticleTypes.SMOKE;
            case HEAVY -> ParticleTypes.ASH;
            case MOLTEN -> ParticleTypes.FLAME;
            case RUNNER -> ParticleTypes.CLOUD;
            case INK -> ParticleTypes.SQUID_INK;
            case UNDYING_WOLF -> ParticleTypes.SOUL;
            case INFERNO -> ParticleTypes.FLAME;
            case THROWER -> ParticleTypes.WITCH;
        };
    }

    static void spawnBaronParticles(ServerWorld world, LivingEntity entity, BaronType type) {
        world.spawnParticles(
                getIdleParticle(type),
                entity.getX(),
                entity.getBodyY(0.7),
                entity.getZ(),
                2,
                0.25,
                0.35,
                0.25,
                0.01
        );
    }

    private static ParticleEffect getIdleParticle(BaronType type) {
        return switch (type) {
            case GOLDEN -> ParticleTypes.HAPPY_VILLAGER;
            case PANIC -> ParticleTypes.CLOUD;
            case FUGITIVE -> ParticleTypes.END_ROD;
            case DRUID -> ParticleTypes.HEART;
            case BARRAGE -> ParticleTypes.CRIT;
            case NUKE -> ParticleTypes.WITCH;
            case SURVIVOR -> ParticleTypes.ANGRY_VILLAGER;
            case NORMAL -> ParticleTypes.SOUL;
            case DARKNIGHT -> ParticleTypes.SMOKE;
            case GIANT -> ParticleTypes.POOF;
            case ALCHEMIST -> ParticleTypes.WITCH;
            case HOTHEAD -> ParticleTypes.FLAME;
            case SWIMMING -> ParticleTypes.SPLASH;
            case DROWNED_KING -> ParticleTypes.SPLASH;
            case BALLOON -> ParticleTypes.DRAGON_BREATH;
            case CHARGING -> ParticleTypes.ANGRY_VILLAGER;
            case DIAMOND -> ParticleTypes.ENCHANT;
            case STALKER -> ParticleTypes.SMOKE;
            case HEAVY -> ParticleTypes.ASH;
            case MOLTEN -> ParticleTypes.FLAME;
            case RUNNER -> ParticleTypes.CLOUD;
            case INK -> ParticleTypes.SQUID_INK;
            case UNDYING_WOLF -> ParticleTypes.SOUL;
            case INFERNO -> ParticleTypes.FLAME;
            case THROWER -> ParticleTypes.WITCH;
        };
    }

    public static boolean spawnLuckyBlockBaron(
            ServerWorld world,
            BlockPos ritualPos,
            ServerPlayerEntity sourcePlayer
    ) {
        if (sourcePlayer == null || sourcePlayer.isSpectator()) {
            return false;
        }

        Optional<BlockPos> spawnPos = findLuckyBlockBaronSpawnPosition(world, ritualPos);

        if (spawnPos.isEmpty()) {
            return false;
        }

        EntityType<? extends LivingEntity> entityType = chooseLuckyBlockBaronEntityType(world);
        LivingEntity entity = entityType.create(world);

        if (entity == null) {
            return false;
        }

        BlockPos targetPos = spawnPos.get();

        entity.refreshPositionAndAngles(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5,
                world.random.nextFloat() * 360.0f,
                0.0f
        );

        entity.addCommandTag(FORCED_BARON_PENDING_TAG);

        if (!world.spawnEntity(entity)) {
            entity.removeCommandTag(FORCED_BARON_PENDING_TAG);
            return false;
        }

        entity.removeCommandTag(FORCED_BARON_PENDING_TAG);

        int fightingLevel = getFightingLevel(sourcePlayer);
        BaronType baronType = chooseBaronType(entity, world);

        promote(entity, world, fightingLevel, baronType);

        return true;
    }

    private static EntityType<? extends LivingEntity> chooseLuckyBlockBaronEntityType(ServerWorld world) {
        return LUCKY_BLOCK_BARON_ENTITY_TYPES.get(world.random.nextInt(LUCKY_BLOCK_BARON_ENTITY_TYPES.size()));
    }

    private static Optional<BlockPos> findLuckyBlockBaronSpawnPosition(ServerWorld world, BlockPos center) {
        for (int attempt = 0; attempt < 80; attempt++) {
            int dx = world.random.nextBetween(-4, 4);
            int dz = world.random.nextBetween(-4, 4);
            int dy = world.random.nextBetween(-1, 2);

            BlockPos candidate = center.add(dx, dy, dz);

            if (isLuckyBlockBaronSpawnPositionSafe(world, candidate)) {
                return Optional.of(candidate.toImmutable());
            }
        }

        BlockPos fallback = center.up();

        if (isLuckyBlockBaronSpawnPositionSafe(world, fallback)) {
            return Optional.of(fallback.toImmutable());
        }

        return Optional.empty();
    }

    private static boolean isLuckyBlockBaronSpawnPositionSafe(ServerWorld world, BlockPos pos) {
        if (pos.getY() <= world.getBottomY() + 1 || pos.getY() >= world.getTopY() - 2) {
            return false;
        }

        if (!world.getBlockState(pos).isAir()) {
            return false;
        }

        if (!world.getBlockState(pos.up()).isAir()) {
            return false;
        }

        if (!world.getFluidState(pos).isEmpty()) {
            return false;
        }

        if (!world.getFluidState(pos.up()).isEmpty()) {
            return false;
        }

        BlockPos floorPos = pos.down();

        return world.getBlockState(floorPos).isSideSolidFullSquare(
                world,
                floorPos,
                Direction.UP
        );
    }

    public static boolean isBaron(Entity entity) {
        return entity.getCommandTags().contains(BARON_TAG);
    }

    public static BaronType getBaronType(Entity entity) {
        return BaronType.fromEntityTags(entity.getCommandTags());
    }

    private static void applyTypeSpecificPromotionChanges(
            LivingEntity entity,
            ServerWorld world,
            BaronType type
    ) {
        if (type == BaronType.GIANT && entity instanceof SlimeEntity slime) {
            GiantBaronBehavior.applyPromotion(slime, world);
        }

        if (type == BaronType.DROWNED_KING && entity instanceof DrownedEntity drowned) {
            DrownedKingBaronBehavior.applyPromotion(drowned, world);
        }

        if (type == BaronType.DIAMOND && entity instanceof VexEntity vex) {
            DiamondBaronBehavior.applyPromotion(vex, world);
        }

        if (type == BaronType.STALKER && entity instanceof WitherSkeletonEntity witherSkeleton) {
            StalkerBaronBehavior.applyPromotion(witherSkeleton, world, BARON_SCALE_BONUS);
        }

        if (type == BaronType.HEAVY && entity instanceof CreeperEntity creeper) {
            HeavyBaronBehavior.applyPromotion(creeper, world);
        }

        if (type == BaronType.RUNNER && entity instanceof CreeperEntity creeper) {
            RunnerBaronBehavior.applyPromotion(creeper, world);
        }
    }
}
