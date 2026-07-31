package com.mythicrpg.core;

import com.mythicrpg.network.LevelUpPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class LevelUpEvents {
    public static void trigger(ServerPlayerEntity player, SkillType type, SkillProgress progress) {
        int level = progress.getLevel();
        int currentXp = progress.getXp();
        int xpForNext = level >= SkillProgress.MAX_LEVEL ? 0 : SkillProgress.xpRequiredForLevel(level);

        ServerPlayNetworking.send(player, new LevelUpPayload(type.name(), level, currentXp, xpForNext));

        player.getServerWorld().spawnParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getBodyY(0.5), player.getZ(),
                40, 0.4, 0.6, 0.4, 0.2
        );

        player.getServerWorld().playSound(
                null, player.getBlockPos(),
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,
                1.0f, 1.0f
        );
    }
}