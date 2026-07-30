package com.mythicrpg.eating;

import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.network.EatingCodexStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Map;

public final class EatingCodexManager {
    private EatingCodexManager() {
    }

    public static boolean recordPreparation(
            ServerPlayerEntity player,
            String recipeId,
            DishRarity rarity,
            int portions,
            int shelfLifeDays
    ) {
        EatingCodexData current = ModAttachments.getEatingCodex(player);
        boolean firstDiscovery = !current.isDiscovered(recipeId);
        long day = player.getWorld().getTimeOfDay() / 24_000L;
        EatingCodexData updated = current.withPreparation(
                recipeId,
                rarity,
                day,
                portions,
                shelfLifeDays
        );
        ModAttachments.setEatingCodex(player, updated);
        sendStateTo(player);

        if (firstDiscovery) {
            EatingXpManager.awardDiscovery(player);
            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.eating.recipe_discovered",
                            Text.translatable("dish.mythicrpg." + recipeId)
                    ).formatted(Formatting.GOLD),
                    false
            );
        }
        return firstDiscovery;
    }

    public static void reset(ServerPlayerEntity player) {
        ModAttachments.setEatingCodex(player, new EatingCodexData());
        sendStateTo(player);
    }

    public static void sendStateTo(ServerPlayerEntity player) {
        EatingCodexData data = ModAttachments.getEatingCodex(player);
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<Integer> counts = new ArrayList<>();
        ArrayList<Integer> rarityRanks = new ArrayList<>();
        ArrayList<Long> days = new ArrayList<>();
        ArrayList<Integer> portions = new ArrayList<>();
        ArrayList<Integer> shelfLives = new ArrayList<>();

        data.entries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    ids.add(entry.getKey());
                    counts.add(entry.getValue().preparations());
                    rarityRanks.add(entry.getValue().bestRarityRank());
                    days.add(entry.getValue().firstDiscoveryDay());
                    portions.add(entry.getValue().lastPortions());
                    shelfLives.add(entry.getValue().lastShelfLifeDays());
                });

        ServerPlayNetworking.send(player, new EatingCodexStatePayload(
                ids, counts, rarityRanks, days, portions, shelfLives
        ));
    }
}
