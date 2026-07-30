package com.mythicrpg.mixin;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import com.mythicrpg.fighting.barons.BaronDeathMessageRegistry;
import com.mythicrpg.fighting.barons.BaronScaling;
import com.mythicrpg.mining.archaeology.relic.ColossalAegisManager;
import com.mythicrpg.eating.EatingAdvancedManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float mythicrpg$applyFightingDamageBonus(float amount, DamageSource source) {
        Entity attacker = source.getAttacker();
        LivingEntity self = (LivingEntity) (Object) this;
        ColossalAegisManager.record(self, amount);

        if (self instanceof ServerPlayerEntity damagedPlayer) {
            amount = EatingAdvancedManager.modifyIncomingDamage(damagedPlayer, source, amount);
        }

        boolean attackerIsBaron = attacker != null && BaronMobManager.isBaron(attacker);

        if (attackerIsBaron) {
            BaronType attackerType = BaronMobManager.getBaronType(attacker);
            BaronDeathMessageRegistry.rememberBaronDanger(self, attackerType);
            amount *= (float) BaronScaling.getDamageMultiplier(attacker);
        }

        if (BaronMobManager.isBaron(self)
                && BaronMobManager.getBaronType(self) == BaronType.SURVIVOR
                && isDirectPlayerMeleeDamage(source)) {
            amount *= BaronScaling.getSurvivorDirectDamageTakenMultiplier(self);
        }

        if (!(attacker instanceof ServerPlayerEntity player)) {
            return amount;
        }

        float multiplier = 1.0f;

        if (self.getType().isIn(EntityTypeTags.UNDEAD)) {
            multiplier += (float) SkillTreeManager.getBonusTotal(player, SkillType.FIGHTING, BonusType.UNDEAD_DAMAGE);
        }
        if (self instanceof SpiderEntity || self instanceof CaveSpiderEntity) {
            multiplier += (float) SkillTreeManager.getBonusTotal(player, SkillType.FIGHTING, BonusType.SPIDER_DAMAGE);
        }

        return amount * multiplier;
    }
    private boolean isDirectPlayerMeleeDamage(DamageSource source) {
        Entity attacker = source.getAttacker();
        Entity sourceEntity = source.getSource();

        return attacker instanceof ServerPlayerEntity
                && sourceEntity instanceof ServerPlayerEntity;
    }
}
