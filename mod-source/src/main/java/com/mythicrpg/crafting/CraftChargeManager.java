package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.SkillXpManager;
import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.core.SkillProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class CraftChargeManager {

    private static final double CHARGE_PER_CRAFT_XP = 0.35;
    private static final double MAX_CHARGE_GAIN_PER_CRAFT = 20.0;
    private static final double BONUS_NEXT_LEVEL_RATIO = 0.25;

    private CraftChargeManager() {
    }

    public static void handleCraftCharge(ServerPlayerEntity player, int craftXp) {
        if (craftXp <= 0) {
            return;
        }

        if (!SkillTreeManager.hasBonus(
                player,
                SkillType.CRAFTING,
                BonusType.CRAFT_CHARGE
        )) {
            return;
        }

        MinecraftServer server = player.getServer();

        if (server == null) {
            return;
        }

        CraftChargeState state = CraftChargeState.get(server);

        double currentCharge = state.getCharge(player.getUuid());
        double gainedCharge = calculateChargeGain(craftXp);

        if (gainedCharge <= 0.0) {
            return;
        }

        double newCharge = currentCharge + gainedCharge;

        if (newCharge >= 100.0) {
            state.setCharge(player.getUuid(), newCharge - 100.0);

            int bonusXp = calculateChargeBonusXp(player);

            if (bonusXp == 0) {
                return;
            }

            SkillXpManager.addXp(
                    player,
                    SkillType.CRAFTING,
                    bonusXp,
                    false
            );

            player.sendMessage(
                    Text.translatable("message.mythicrpg.craft_charge.complete", bonusXp)
                            .formatted(Formatting.GOLD),
                    true
            );

            return;
        }

        state.setCharge(player.getUuid(), newCharge);

        player.sendMessage(
                Text.translatable("message.mythicrpg.craft_charge.progress", (int) Math.floor(newCharge))
                        .formatted(Formatting.YELLOW),
                true
        );
    }

    private static double calculateChargeGain(int craftXp) {
        double charge = craftXp * CHARGE_PER_CRAFT_XP;
        return Math.min(MAX_CHARGE_GAIN_PER_CRAFT, charge);
    }

    private static int calculateChargeBonusXp(ServerPlayerEntity player) {
        SkillProgress progress = ModAttachments.getProgress(player, SkillType.CRAFTING);

        if (progress.getLevel() >= SkillProgress.MAX_LEVEL) {
            return 0;
        }

        int xpForNextLevel = SkillProgress.xpRequiredForLevel(progress.getLevel());

        return Math.max(1, (int) Math.floor(xpForNextLevel * BONUS_NEXT_LEVEL_RATIO));
    }
}