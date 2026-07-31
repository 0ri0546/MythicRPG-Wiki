package com.mythicrpg.crafting;

import com.mythicrpg.core.ModItems;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ExpCharmBonusManager {

    private static final double BONUS_RATIO = 0.20;
    private static final Map<UUID, Double> XP_REMAINDERS = new HashMap<>();

    private ExpCharmBonusManager() {
    }

    public static void applyBonus(ServerPlayerEntity player, int vanillaXpAmount) {
        if (vanillaXpAmount <= 0) {
            return;
        }

        if (!player.getOffHandStack().isOf(ModItems.EXP_CHARM)) {
            return;
        }

        UUID uuid = player.getUuid();

        double storedRemainder = XP_REMAINDERS.getOrDefault(uuid, 0.0);
        double rawBonus = vanillaXpAmount * BONUS_RATIO + storedRemainder;

        int bonusXp = (int) Math.floor(rawBonus);
        double newRemainder = rawBonus - bonusXp;

        XP_REMAINDERS.put(uuid, newRemainder);

        if (bonusXp <= 0) {
            return;
        }

        player.addExperience(bonusXp);
    }

    public static void clearPlayer(UUID playerUuid) {
        XP_REMAINDERS.remove(playerUuid);
    }
}