package com.mythicrpg.core;

import net.minecraft.server.network.ServerPlayerEntity;

public class GlobalLevelManager {

    public static int getGlobalLevel(ServerPlayerEntity player) {
        int total = 0;

        for (SkillType type : SkillType.values()) {
            SkillProgress progress = ModAttachments.getProgress(player, type);
            total += progress.getLevel();
        }

        return total;
    }

    public static int getMaxGlobalLevel() {
        return SkillType.values().length * SkillProgress.MAX_LEVEL;
    }
}