package com.mythicrpg.mining.archaeology;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.Optional;

public final class ArchaeologistInteractionManager {

    private ArchaeologistInteractionManager() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register(ArchaeologistInteractionManager::interact);
        ArchaeologistExpeditionManager.register();
    }

    private static ActionResult interact(
            net.minecraft.entity.player.PlayerEntity player,
            World world,
            Hand hand,
            Entity entity,
            net.minecraft.util.hit.EntityHitResult hitResult
    ) {
        if (!(entity instanceof VillagerEntity villager)
                || villager.getVillagerData().getProfession() != ModVillagers.ARCHAEOLOGIST) {
            return ActionResult.PASS;
        }
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)
                || !(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }

        if (!serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
            serverPlayer.sendMessage(Text.translatable(
                    "message.mythicrpg.archaeologist.overworld_only"
            ).formatted(Formatting.RED), true);
            return ActionResult.SUCCESS;
        }
        if (!SkillTreeManager.hasBonus(
                serverPlayer,
                SkillType.MINING,
                BonusType.FOSSIL_ARCHAEOLOGIST
        )) {
            serverPlayer.sendMessage(Text.translatable(
                    "message.mythicrpg.archaeologist.perk_required"
            ).formatted(Formatting.RED), true);
            return ActionResult.SUCCESS;
        }

        serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, owner) -> new ArchaeologistScreenHandler(
                        syncId,
                        inventory,
                        villager.getId()
                ),
                Text.empty()
        ));
        return ActionResult.SUCCESS;
    }

    public static AnalysisResult beginAnalysisForInterface(
            ServerPlayerEntity player,
            ServerWorld world,
            VillagerEntity villager,
            ItemStack skeleton,
            ArchaeologistScreenHandler handler
    ) {
        if (!world.getRegistryKey().equals(World.OVERWORLD)) {
            return AnalysisResult.empty(AnalysisStatus.OVERWORLD_ONLY);
        }
        if (!SkillTreeManager.hasBonus(
                player,
                SkillType.MINING,
                BonusType.FOSSIL_ARCHAEOLOGIST
        )) {
            return AnalysisResult.empty(AnalysisStatus.PERK_REQUIRED);
        }

        Optional<FossilSpecimenData.Specimen> parsed = FossilSpecimenData.read(skeleton);
        if (parsed.isEmpty()) {
            return AnalysisResult.empty(AnalysisStatus.INVALID_SPECIMEN);
        }

        FossilSpecimenData.Specimen specimen = parsed.get();
        if (!specimen.reconstructedBy().equals(player.getUuid())) {
            return AnalysisResult.empty(AnalysisStatus.NOT_OWNER);
        }

        GrandFossilSiteState state = GrandFossilSiteState.get(player.getServer());
        boolean worldAnalyzed = state.isSpecimenAnalyzed(specimen.specimenId());

        if (specimen.analyzed() && !worldAnalyzed) {
            FossilSpecimenData.markUnanalyzed(skeleton);
            specimen = new FossilSpecimenData.Specimen(
                    specimen.family(),
                    specimen.rarity(),
                    specimen.specimenId(),
                    specimen.reconstructedBy(),
                    specimen.reconstructedDay(),
                    false
            );
        }

        if (worldAnalyzed) {
            return reissueDossier(skeleton, specimen, state);
        }

        if (!state.tryReserveSpecimen(specimen.specimenId())) {
            return AnalysisResult.empty(AnalysisStatus.SPECIMEN_BUSY);
        }

        ArchaeologistExpeditionManager.StartResult startResult =
                ArchaeologistExpeditionManager.start(
                        player,
                        world,
                        villager,
                        specimen,
                        handler,
                        state
                );
        if (startResult != ArchaeologistExpeditionManager.StartResult.STARTED) {
            state.releaseSpecimen(specimen.specimenId());
            return AnalysisResult.empty(
                    startResult == ArchaeologistExpeditionManager.StartResult.THROTTLED
                            ? AnalysisStatus.RETRY_LATER
                            : AnalysisStatus.SPECIMEN_BUSY
            );
        }
        return AnalysisResult.empty(AnalysisStatus.BUSY);
    }

    private static AnalysisResult reissueDossier(
            ItemStack skeleton,
            FossilSpecimenData.Specimen specimen,
            GrandFossilSiteState state
    ) {
        Optional<GrandFossilSiteState.GrandSiteRecord> previousSite =
                FossilSpecimenData.analyzedSiteId(skeleton).flatMap(state::findById);
        if (previousSite.isEmpty()) {
            previousSite = state.findBySpecimenId(specimen.specimenId());
        }
        if (previousSite.isEmpty()) {
            return AnalysisResult.empty(AnalysisStatus.ALREADY_ANALYZED);
        }

        GrandFossilSiteState.GrandSiteRecord record = previousSite.get();
        FossilSpecimenData.markAnalyzed(skeleton, record.id());
        return new AnalysisResult(
                AnalysisStatus.DOSSIER_REISSUED,
                ExpeditionDossierData.initialize(
                        new ItemStack(ModItems.EXPEDITION_DOSSIER),
                        record
                )
        );
    }

    static AnalysisResult completeGeneratedAnalysis(
            ServerPlayerEntity player,
            ServerWorld world,
            VillagerEntity villager,
            ItemStack skeleton,
            FossilSpecimenData.Specimen specimen,
            GrandFossilSiteGenerator.GeneratedGrandSite generatedSite,
            GrandFossilSiteState state
    ) {
        if (!specimen.reconstructedBy().equals(player.getUuid())) {
            generatedSite.rollback(world);
            state.releaseSpecimen(specimen.specimenId());
            return AnalysisResult.empty(AnalysisStatus.NOT_OWNER);
        }

        Optional<FossilSpecimenData.Specimen> current = FossilSpecimenData.read(skeleton);
        if (current.isEmpty()
                || !current.get().specimenId().equals(specimen.specimenId())
                || !current.get().reconstructedBy().equals(player.getUuid())) {
            generatedSite.rollback(world);
            state.releaseSpecimen(specimen.specimenId());
            return AnalysisResult.empty(AnalysisStatus.INVALID_SPECIMEN);
        }

        GrandFossilSiteState.GrandSiteRecord record = generatedSite.record();
        if (!state.registerCompletedSite(record)) {
            generatedSite.rollback(world);
            state.releaseSpecimen(specimen.specimenId());
            return AnalysisResult.empty(AnalysisStatus.GENERATION_FAILED);
        }

        FossilSpecimenData.markAnalyzed(skeleton, record.id());
        ItemStack dossier = ExpeditionDossierData.initialize(
                new ItemStack(ModItems.EXPEDITION_DOSSIER),
                record
        );
        FossilCodexManager.recordAnalysis(
                player,
                specimen.family(),
                specimen.rarity(),
                specimen.specimenId().toString()
        );

        villager.playWorkSound();
        world.sendEntityStatus(villager, (byte) 14);
        world.playSound(
                null,
                villager.getBlockPos(),
                SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
                SoundCategory.NEUTRAL,
                1.0F,
                0.9F
        );
        return new AnalysisResult(AnalysisStatus.SUCCESS, dossier);
    }

    public enum AnalysisStatus {
        IDLE(0, "tooltip.mythicrpg.archaeologist.idle"),
        SUCCESS(1, "tooltip.mythicrpg.archaeologist.success"),
        DOSSIER_REISSUED(2, "tooltip.mythicrpg.archaeologist.reissued"),
        INVALID_SPECIMEN(3, "tooltip.mythicrpg.archaeologist.invalid"),
        ALREADY_ANALYZED(4, "tooltip.mythicrpg.archaeologist.already_analyzed"),
        BUSY(5, "tooltip.mythicrpg.archaeologist.searching"),
        NO_SAFE_SITE(6, "tooltip.mythicrpg.archaeologist.no_safe_site"),
        GENERATION_FAILED(7, "tooltip.mythicrpg.archaeologist.failed"),
        OVERWORLD_ONLY(8, "tooltip.mythicrpg.archaeologist.overworld_only"),
        PERK_REQUIRED(9, "tooltip.mythicrpg.archaeologist.perk_required"),
        NOT_OWNER(10, "tooltip.mythicrpg.archaeologist.not_owner"),
        SPECIMEN_BUSY(11, "tooltip.mythicrpg.archaeologist.busy"),
        RETRY_LATER(12, "tooltip.mythicrpg.archaeologist.retry_later");

        private final int id;
        private final String tooltipKey;

        AnalysisStatus(int id, String tooltipKey) {
            this.id = id;
            this.tooltipKey = tooltipKey;
        }

        public int id() {
            return id;
        }

        public String tooltipKey() {
            return tooltipKey;
        }

        public boolean isSuccess() {
            return this == SUCCESS || this == DOSSIER_REISSUED;
        }

        public boolean isBusy() {
            return this == BUSY;
        }

        public static AnalysisStatus byId(int id) {
            for (AnalysisStatus status : values()) {
                if (status.id == id) {
                    return status;
                }
            }
            return IDLE;
        }
    }

    public record AnalysisResult(AnalysisStatus status, ItemStack dossier) {
        public static AnalysisResult empty(AnalysisStatus status) {
            return new AnalysisResult(status, ItemStack.EMPTY);
        }
    }
}
