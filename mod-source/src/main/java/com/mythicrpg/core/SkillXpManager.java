package com.mythicrpg.core;

import com.mythicrpg.network.XpGainPayload;
import com.mythicrpg.titles.TitleManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SkillXpManager {

    public static void addXp(ServerPlayerEntity player, SkillType skillType, int amount) {
        addXp(player, skillType, amount, false);
    }

    public static void addXp(ServerPlayerEntity player, SkillType skillType, int amount, boolean sendDebugMessage) {
        if (amount <= 0) {
            return;
        }

        SkillProgress progress = ModAttachments.getProgress(player, skillType);
        int oldLevel = progress.getLevel();

        progress.addXp(amount);

        ModAttachments.setProgress(player, skillType, progress);

        int xpForNext = progress.getLevel() >= SkillProgress.MAX_LEVEL
                ? 0
                : SkillProgress.xpRequiredForLevel(progress.getLevel());

        ServerPlayNetworking.send(
                player,
                new XpGainPayload(
                        skillType.name(),
                        progress.getLevel(),
                        progress.getXp(),
                        xpForNext
                )
        );

        if (progress.getLevel() > oldLevel) {
            SkillTreeManager.sendStateTo(player, skillType);
            TitleManager.onSkillProgressChanged(player);
            LevelUpEvents.trigger(player, skillType, progress);
        }

        if (sendDebugMessage) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.skill_xp.debug", amount, skillType.displayName(), progress.getXp()),
                    false
            );
        }
    }
}