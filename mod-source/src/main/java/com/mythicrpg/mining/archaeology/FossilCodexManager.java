package com.mythicrpg.mining.archaeology;

import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.network.FossilCodexStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Map;

public final class FossilCodexManager {

    private FossilCodexManager() {
    }

    public static boolean recordReconstruction(
            ServerPlayerEntity player,
            FossilFamily family,
            FossilRarity rarity,
            String specimenId,
            long day
    ) {
        FossilCodexData current = ModAttachments.getFossilCodex(player);
        FossilCodexData updated = current.withReconstruction(family, rarity, specimenId, day);

        if (updated == current) {
            return false;
        }

        ModAttachments.setFossilCodex(player, updated);
        sendStateTo(player);
        return true;
    }


    public static boolean recordAnalysis(
            ServerPlayerEntity player,
            FossilFamily family,
            FossilRarity rarity,
            String specimenId
    ) {
        FossilCodexData current = ModAttachments.getFossilCodex(player);
        FossilCodexData updated = current.withAnalysis(family, rarity, specimenId);
        if (updated == current) {
            return false;
        }
        ModAttachments.setFossilCodex(player, updated);
        sendStateTo(player);
        return true;
    }

    public static void reconcileAnalyses(ServerPlayerEntity player) {
        GrandFossilSiteState state = GrandFossilSiteState.get(player.getServer());
        FossilCodexData data = ModAttachments.getFossilCodex(player);
        ArrayList<FossilCodexData.AnalysisInput> analyses = new ArrayList<>();
        for (GrandFossilSiteState.GrandSiteRecord site : state.sitesForReconstructor(player.getUuid())) {
            if (state.isSpecimenAnalyzed(site.specimenId())) {
                analyses.add(new FossilCodexData.AnalysisInput(
                        site.specimenFamily(),
                        site.specimenRarity(),
                        site.specimenId().toString()
                ));
            }
        }
        FossilCodexData updated = data.withAnalyses(analyses);
        if (updated != data) {
            ModAttachments.setFossilCodex(player, updated);
        }
    }

    public static void reset(ServerPlayerEntity player) {
        ModAttachments.setFossilCodex(player, new FossilCodexData());
        sendStateTo(player);
    }

    public static void sendStateTo(ServerPlayerEntity player) {
        FossilCodexData data = ModAttachments.getFossilCodex(player);
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<Integer> reconstructedCounts = new ArrayList<>();
        ArrayList<Long> firstDays = new ArrayList<>();
        ArrayList<Integer> analyzedCounts = new ArrayList<>();

        for (Map.Entry<String, FossilCodexEntry> entry : data.entries().entrySet()) {
            keys.add(entry.getKey());
            reconstructedCounts.add(entry.getValue().reconstructedCount());
            firstDays.add(entry.getValue().firstReconstructedDay());
            analyzedCounts.add(entry.getValue().analyzedCount());
        }

        ServerPlayNetworking.send(player, new FossilCodexStatePayload(
                keys,
                reconstructedCounts,
                firstDays,
                analyzedCounts
        ));
    }
}
