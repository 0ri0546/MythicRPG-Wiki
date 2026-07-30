
package com.mythicrpg.fishing;

import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.network.FishingCodexStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

public final class FishingCodexManager {
    private FishingCodexManager() {
    }

    public static void record(ServerPlayerEntity player, FishingCatchData.Catch caught) {
        FishingCodexData current = ModAttachments.getFishingCodex(player);
        boolean first = current.get(caught.family(), caught.rarity()).captures() == 0;
        FishingCodexData updated = current.withCatch(
                caught.family(),
                caught.rarity(),
                player.getWorld().getTimeOfDay() / 24000L,
                caught.biome(),
                caught.dimension(),
                caught.source()
        );
        ModAttachments.setFishingCodex(player, updated);
        sendStateTo(player);

        if (first) {
            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.fishing.discovered_rarity",
                            caught.rarity().displayName(),
                            caught.family().displayName()
                    ).formatted(caught.rarity().formatting()),
                    false
            );
        }
    }

    public static void sendStateTo(ServerPlayerEntity player) {
        FishingCodexData data = ModAttachments.getFishingCodex(player);
        ArrayList<String> familyIds = new ArrayList<>();
        ArrayList<Integer> rarityRanks = new ArrayList<>();
        ArrayList<Integer> captureCounts = new ArrayList<>();
        ArrayList<Long> firstDays = new ArrayList<>();
        ArrayList<String> biomes = new ArrayList<>();
        ArrayList<String> dimensions = new ArrayList<>();
        ArrayList<String> sources = new ArrayList<>();

        for (FishingFamily family : FishingFamily.values()) {
            for (FishingRarity rarity : FishingRarity.values()) {
                FishingCodexEntry entry = data.get(family, rarity);
                familyIds.add(family.id());
                rarityRanks.add(rarity.rank());
                captureCounts.add(entry.captures());
                firstDays.add(entry.firstDiscoveryDay());
                biomes.add(entry.firstBiome());
                dimensions.add(entry.firstDimension());
                sources.add(entry.lastSource());
            }
        }

        ServerPlayNetworking.send(
                player,
                new FishingCodexStatePayload(
                        familyIds,
                        rarityRanks,
                        captureCounts,
                        firstDays,
                        biomes,
                        dimensions,
                        sources
                )
        );
    }
}
