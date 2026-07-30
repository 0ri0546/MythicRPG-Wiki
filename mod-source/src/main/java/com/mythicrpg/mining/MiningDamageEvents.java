package com.mythicrpg.mining;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;

public class MiningDamageEvents {
    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return true;
            }
            if (source.isOf(DamageTypes.FALL) && SkillTreeManager.hasBonus(player, SkillType.MINING, BonusType.NO_FALL_DAMAGE)) {
                return false;
            }
            return true;
        });
    }
}