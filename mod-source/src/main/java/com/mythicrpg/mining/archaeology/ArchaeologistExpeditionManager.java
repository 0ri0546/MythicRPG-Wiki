package com.mythicrpg.mining.archaeology;

import com.mythicrpg.MythicRPG;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Runs Archaeologist searches with a bounded, fair server-wide work budget.
 *
 * Each SearchJob work unit either loads one required chunk or validates one
 * complete 11x7x11 candidate. At most one work unit runs per server tick,
 * regardless of the number of players analyzing specimens simultaneously.
 */
public final class ArchaeologistExpeditionManager {

    private static final int MAX_SEARCH_WORK_UNITS_PER_TICK = 1;
    private static final long START_COOLDOWN_TICKS = 40L;
    private static final Map<UUID, ActiveAnalysis> ACTIVE_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Long> NEXT_START_AT = new HashMap<>();
    private static final ArrayDeque<UUID> WORK_QUEUE = new ArrayDeque<>();

    private ArchaeologistExpeditionManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ACTIVE_BY_PLAYER.clear();
            NEXT_START_AT.clear();
            WORK_QUEUE.clear();
        });
    }

    public static StartResult start(
            ServerPlayerEntity player,
            ServerWorld world,
            VillagerEntity villager,
            FossilSpecimenData.Specimen specimen,
            ArchaeologistScreenHandler handler,
            GrandFossilSiteState state
    ) {
        UUID playerUuid = player.getUuid();
        if (ACTIVE_BY_PLAYER.containsKey(playerUuid)) {
            return StartResult.ALREADY_RUNNING;
        }
        long now = world.getTime();
        long nextStartAt = NEXT_START_AT.getOrDefault(playerUuid, 0L);
        if (now < nextStartAt) {
            return StartResult.THROTTLED;
        }
        NEXT_START_AT.put(playerUuid, now + START_COOLDOWN_TICKS);

        GrandFossilSiteGenerator.SearchJob search = GrandFossilSiteGenerator.createSearchJob(
                world,
                villager.getBlockPos(),
                specimen,
                playerUuid,
                villager.getUuid(),
                state
        );
        ACTIVE_BY_PLAYER.put(playerUuid, new ActiveAnalysis(
                player,
                world,
                villager.getUuid(),
                specimen,
                handler,
                state,
                search
        ));
        WORK_QUEUE.addLast(playerUuid);
        return StartResult.STARTED;
    }

    public static void cancel(ArchaeologistScreenHandler handler) {
        Iterator<Map.Entry<UUID, ActiveAnalysis>> iterator = ACTIVE_BY_PLAYER.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveAnalysis> entry = iterator.next();
            ActiveAnalysis analysis = entry.getValue();
            if (analysis.handler() != handler) {
                continue;
            }
            analysis.state().releaseSpecimen(analysis.specimen().specimenId());
            WORK_QUEUE.remove(entry.getKey());
            iterator.remove();
        }
    }

    private static void tick() {
        if (ACTIVE_BY_PLAYER.isEmpty()) {
            return;
        }

        removeInvalidAnalyses();
        if (ACTIVE_BY_PLAYER.isEmpty()) {
            return;
        }

        int workBudget = Math.min(MAX_SEARCH_WORK_UNITS_PER_TICK, ACTIVE_BY_PLAYER.size());
        int queueChecks = WORK_QUEUE.size();
        while (workBudget > 0 && queueChecks-- > 0 && !WORK_QUEUE.isEmpty()) {
            UUID playerUuid = WORK_QUEUE.removeFirst();
            ActiveAnalysis analysis = ACTIVE_BY_PLAYER.get(playerUuid);
            if (analysis == null) {
                continue;
            }

            boolean keepRunning = runOneWorkUnit(analysis);
            workBudget--;
            if (keepRunning && ACTIVE_BY_PLAYER.containsKey(playerUuid)) {
                WORK_QUEUE.addLast(playerUuid);
            } else {
                ACTIVE_BY_PLAYER.remove(playerUuid);
            }
        }
    }

    private static void removeInvalidAnalyses() {
        Iterator<Map.Entry<UUID, ActiveAnalysis>> iterator = ACTIVE_BY_PLAYER.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveAnalysis> entry = iterator.next();
            ActiveAnalysis analysis = entry.getValue();
            ServerPlayerEntity player = analysis.player();

            if (player.isDisconnected()
                    || player.currentScreenHandler != analysis.handler()
                    || !analysis.handler().isAnalysisRunningFor(analysis.specimen().specimenId())) {
                analysis.state().releaseSpecimen(analysis.specimen().specimenId());
                WORK_QUEUE.remove(entry.getKey());
                iterator.remove();
                continue;
            }

            Entity entity = analysis.world().getEntity(analysis.villagerUuid());
            if (!(entity instanceof VillagerEntity villager)
                    || !villager.isAlive()
                    || villager.getVillagerData().getProfession() != ModVillagers.ARCHAEOLOGIST
                    || player.squaredDistanceTo(villager) > 64.0D) {
                analysis.state().releaseSpecimen(analysis.specimen().specimenId());
                finish(
                        analysis,
                        ArchaeologistInteractionManager.AnalysisResult.empty(
                                ArchaeologistInteractionManager.AnalysisStatus.GENERATION_FAILED
                        )
                );
                WORK_QUEUE.remove(entry.getKey());
                iterator.remove();
            }
        }
    }

    private static boolean runOneWorkUnit(ActiveAnalysis analysis) {
        try {
            GrandFossilSiteGenerator.SearchStep step = analysis.search().step();
            if (step.status() == GrandFossilSiteGenerator.SearchStatus.SEARCHING) {
                return true;
            }
            if (step.status() == GrandFossilSiteGenerator.SearchStatus.EXHAUSTED) {
                analysis.state().releaseSpecimen(analysis.specimen().specimenId());
                finish(
                        analysis,
                        ArchaeologistInteractionManager.AnalysisResult.empty(
                                ArchaeologistInteractionManager.AnalysisStatus.NO_SAFE_SITE
                        )
                );
                return false;
            }

            Entity entity = analysis.world().getEntity(analysis.villagerUuid());
            if (!(entity instanceof VillagerEntity villager)) {
                step.generated().rollback(analysis.world());
                analysis.state().releaseSpecimen(analysis.specimen().specimenId());
                finish(
                        analysis,
                        ArchaeologistInteractionManager.AnalysisResult.empty(
                                ArchaeologistInteractionManager.AnalysisStatus.GENERATION_FAILED
                        )
                );
                return false;
            }

            ArchaeologistInteractionManager.AnalysisResult result =
                    ArchaeologistInteractionManager.completeGeneratedAnalysis(
                            analysis.player(),
                            analysis.world(),
                            villager,
                            analysis.handler().inputStack(),
                            analysis.specimen(),
                            step.generated(),
                            analysis.state()
                    );
            finish(analysis, result);
            return false;
        } catch (RuntimeException exception) {
            MythicRPG.LOGGER.error(
                    "Archaeologist expedition generation failed for specimen {} with {} candidates remaining",
                    analysis.specimen().specimenId(),
                    analysis.search().remainingCandidates(),
                    exception
            );
            analysis.state().releaseSpecimen(analysis.specimen().specimenId());
            finish(
                    analysis,
                    ArchaeologistInteractionManager.AnalysisResult.empty(
                            ArchaeologistInteractionManager.AnalysisStatus.GENERATION_FAILED
                    )
            );
            return false;
        }
    }

    private static void finish(
            ActiveAnalysis analysis,
            ArchaeologistInteractionManager.AnalysisResult result
    ) {
        analysis.handler().completeAnalysis(result);
    }


    public enum StartResult {
        STARTED,
        ALREADY_RUNNING,
        THROTTLED
    }

    private record ActiveAnalysis(
            ServerPlayerEntity player,
            ServerWorld world,
            UUID villagerUuid,
            FossilSpecimenData.Specimen specimen,
            ArchaeologistScreenHandler handler,
            GrandFossilSiteState state,
            GrandFossilSiteGenerator.SearchJob search
    ) {
    }
}
