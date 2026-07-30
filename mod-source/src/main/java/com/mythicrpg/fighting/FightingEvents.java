package com.mythicrpg.fighting;

import com.mythicrpg.core.*;
import com.mythicrpg.fighting.barons.BaronScaling;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.ItemScatterer;

import java.util.List;
import net.minecraft.sound.SoundEvents;

public class FightingEvents {

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (blocked) {
                return;
            }
            if (!(source.getAttacker() instanceof ServerPlayerEntity player)) {
                return;
            }
            if (entity == player) {
                return;
            }

            applyGlowingOnHit(player, entity);
            applyPoisonOnHit(player, entity);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(damageSource.getAttacker() instanceof ServerPlayerEntity player)) {
                return;
            }
            if (!(entity.getWorld() instanceof ServerWorld world)) {
                return;
            }

            applyDoubleLootOnKill(player, entity, world, damageSource);
            applyMobXpBonus(player, entity, world);
            grantSkillXp(player, entity);
        });
    }

    private static void applyGlowingOnHit(ServerPlayerEntity player, LivingEntity target) {
        if (!SkillTreeManager.hasBonus(player, SkillType.FIGHTING, BonusType.HIT_GLOWING)) {
            return;
        }
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 0));
    }

    private static void applyPoisonOnHit(ServerPlayerEntity player, LivingEntity target) {
        PoisonOnHit poison = SkillTreeManager.getBestPoisonOnHit(player, SkillType.FIGHTING);
        if (poison == null) {
            return;
        }
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, poison.durationTicks(), poison.amplifier()));
        PassiveProcSoundManager.playForPlayer(
                player,
                "fighting_poison",
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                0.25f,
                1.8f,
                20
        );
    }

    private static void applyDoubleLootOnKill(ServerPlayerEntity player, LivingEntity entity,
                                              ServerWorld world, DamageSource damageSource) {
        double chance = SkillTreeManager.getBonusTotal(player, SkillType.FIGHTING, BonusType.DOUBLE_LOOT_CHANCE);
        if (chance <= 0 || world.random.nextDouble() >= chance) {
            return;
        }

        RegistryKey<LootTable> lootTableKey = entity.getLootTable();
        LootTable lootTable = world.getServer().getReloadableRegistries().getLootTable(lootTableKey);

        LootContextParameterSet params = new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.THIS_ENTITY, entity)
                .add(LootContextParameters.ORIGIN, entity.getPos())
                .add(LootContextParameters.DAMAGE_SOURCE, damageSource)
                .build(LootContextTypes.ENTITY);

        List<ItemStack> bonusLoot = lootTable.generateLoot(params);

        for (ItemStack stack : bonusLoot) {
            ItemScatterer.spawn(world, entity.getX(), entity.getY(), entity.getZ(), stack);
        }

        PassiveProcSoundManager.playForPlayer(
                player,
                "fighting_double_loot",
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                0.45f,
                1.2f,
                5
        );
    }

    private static void applyMobXpBonus(ServerPlayerEntity player, LivingEntity entity, ServerWorld world) {
        double multiplier = SkillTreeManager.getBonusTotal(player, SkillType.FIGHTING, BonusType.MOB_XP_MULTIPLIER);
        if (multiplier <= 0) {
            return;
        }

        int baseXp = baselineXpForMob(entity);
        if (baseXp <= 0) {
            return;
        }

        int bonusXp = (int) Math.round(baseXp * (multiplier - 1.0));
        if (bonusXp > 0) {
            ExperienceOrbEntity.spawn(world, entity.getPos(), bonusXp);
        }
    }

    // Valeurs approximatives (la plupart des mobs hostiles courants donnent 5xp en vanilla).
    // Pas un mixin sur le vrai calcul, donc pas garanti pile identique au montant reel qui vient de tomber.
    private static int baselineXpForMob(LivingEntity entity) {
        if (entity instanceof net.minecraft.entity.mob.BlazeEntity
                || entity instanceof net.minecraft.entity.mob.GuardianEntity) {
            return 10;
        }
        if (entity instanceof net.minecraft.entity.mob.HostileEntity) {
            return 5;
        }
        return 0;
    }

    private static void grantSkillXp(ServerPlayerEntity player, LivingEntity entity) {
        double xpHealthBasis = entity.getMaxHealth();

        if (BaronMobManager.isBaron(entity)) {
            xpHealthBasis = BaronScaling.getUnscaledMaxHealthForXp(entity);
        }

        int xpGained = (int) Math.max(1, Math.min(20, Math.round(xpHealthBasis / 5.0f)));

        if (BaronMobManager.isBaron(entity)) {
            xpGained = (int) Math.round(xpGained * 1.5) + 5;
            xpGained = (int) Math.max(1, Math.round(xpGained * BaronScaling.getXpRewardMultiplier(entity)));
        }

        SkillXpManager.addXp(player, SkillType.FIGHTING, xpGained, false);
    }
}