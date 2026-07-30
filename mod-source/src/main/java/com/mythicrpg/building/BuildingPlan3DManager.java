package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModBlocks;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Capture and preview front-end for 3D plans; placement uses the shared 2D/3D scheduler. */
public final class BuildingPlan3DManager {
    public static final int MAX_SIZE = 8;
    private static final long PREVIEW_LIFETIME_TICKS = 20L * 30L;
    private static final Map<UUID, PreviewSession> PREVIEWS = new HashMap<>();

    private BuildingPlan3DManager() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BuildingPlan3DManager::cleanupPreviews);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearAll());
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) clearPreview(player, false);
        });
    }

    public static boolean canUse(ServerPlayerEntity player) {
        return SkillTreeManager.hasBonus(player, SkillType.BUILDING, BonusType.BUILD_PLAN_3D);
    }

    public static CaptureResult capture(
            ServerPlayerEntity player,
            ServerWorld world,
            BuildingPlan3DData.Selection first,
            BlockPos second
    ) {
        if (!canUse(player)) return CaptureResult.failure("message.mythicrpg.building_plan_3d.locked");
        if (!dimensionId(world).equals(first.dimensionId())) {
            return CaptureResult.failure("message.mythicrpg.building_plan_3d.wrong_dimension");
        }

        int deltaX = second.getX() - first.pos().getX();
        int deltaY = second.getY() - first.pos().getY();
        int deltaZ = second.getZ() - first.pos().getZ();
        int sizeX = Math.abs(deltaX) + 1;
        int sizeY = Math.abs(deltaY) + 1;
        int sizeZ = Math.abs(deltaZ) + 1;
        if (sizeX > MAX_SIZE || sizeY > MAX_SIZE || sizeZ > MAX_SIZE) {
            return CaptureResult.failure(
                    "message.mythicrpg.building_plan_3d.too_large",
                    sizeX, sizeY, sizeZ, MAX_SIZE, MAX_SIZE, MAX_SIZE
            );
        }

        int stepX = deltaX < 0 ? -1 : 1;
        int stepY = deltaY < 0 ? -1 : 1;
        int stepZ = deltaZ < 0 ? -1 : 1;
        List<BuildingPlan3DData.Entry> entries = new ArrayList<>(sizeX * sizeY * sizeZ);

        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockPos offset = new BlockPos(x * stepX, y * stepY, z * stepZ);
                    BlockPos sourcePos = first.pos().add(offset);
                    if (!world.isChunkLoaded(sourcePos)) {
                        return CaptureResult.failure("message.mythicrpg.building_plan_3d.unloaded");
                    }
                    if (!world.isInBuildLimit(sourcePos) || !world.getWorldBorder().contains(sourcePos)) {
                        return CaptureResult.failure("message.mythicrpg.building_plan_3d.outside_world");
                    }
                    BlockState state = world.getBlockState(sourcePos);
                    if (state.isAir()) continue;

                    BlankBlockAppearance appearance = BlankBlockAppearance.EMPTY;
                    net.minecraft.block.entity.BlockEntity sourceBlockEntity = world.getBlockEntity(sourcePos);
                    if (sourceBlockEntity != null) {
                        if (state.isOf(ModBlocks.BLANK_BLOCK)
                                && sourceBlockEntity instanceof BlankBlockEntity blank) {
                            appearance = blank.appearance();
                        } else {
                            return CaptureResult.failure(
                                    "message.mythicrpg.building_plan_3d.unsupported",
                                    state.getBlock().getName()
                            );
                        }
                    }

                    if (!state.getFluidState().isEmpty()
                            || !BuildingBlockCatalog.isEligible(state.getBlock())
                            || state.getBlock().asItem() == Items.AIR
                            || !BlankBlockMaterialRegistry.isValid(appearance)) {
                        return CaptureResult.failure(
                                "message.mythicrpg.building_plan_3d.unsupported",
                                state.getBlock().getName()
                        );
                    }
                    entries.add(new BuildingPlan3DData.Entry(offset, state, appearance));
                }
            }
        }

        if (entries.isEmpty()) return CaptureResult.failure("message.mythicrpg.building_plan_3d.empty");
        clearPreview(player, false);
        return CaptureResult.success(new BuildingPlan3DData.Plan(
                UUID.randomUUID(), sizeX, sizeY, sizeZ, List.copyOf(entries)
        ).withSource(dimensionId(world), first.pos(), second));
    }

    /** First click previews the destination; the same click a second time confirms it. */
    public static void previewOrConfirm(
            ServerPlayerEntity player,
            ServerWorld world,
            BuildingPlan3DData.Plan plan,
            BlockPos anchor
    ) {
        BuildingPlan2DManager.clearPreviewOnly(player);
        if (!canUse(player)) {
            sendError(player, Text.translatable("message.mythicrpg.building_plan_3d.locked"));
            clearPreview(player, false);
            return;
        }
        if (!isPlanValid(plan)) {
            sendError(player, Text.translatable("message.mythicrpg.building_plan_3d.corrupt"));
            clearPreview(player, false);
            return;
        }
        if (BuildingPlan2DManager.hasActiveJob(player)) {
            sendError(player, Text.translatable("message.mythicrpg.building_plan_3d.already_active"));
            return;
        }

        String worldId = dimensionId(world);
        List<BuildingPlan2DManager.Placement> allPlacements = placements(plan, anchor);
        BuildingPlan2DManager.Validation validation = BuildingPlan2DManager.validate(
                player, world, allPlacements, true
        );
        List<BlockPos> previewPositions = allPlacements.stream()
                .map(BuildingPlan2DManager.Placement::pos)
                .toList();

        PreviewSession previous = PREVIEWS.get(player.getUuid());
        boolean confirms = previous != null
                && previous.planId().equals(plan.id())
                && previous.dimensionId().equals(worldId)
                && previous.anchor().equals(anchor)
                && world.getTime() <= previous.expiresAtTick();

        if (!confirms) {
            PREVIEWS.put(player.getUuid(), new PreviewSession(
                    plan.id(), worldId, anchor, world.getTime() + PREVIEW_LIFETIME_TICKS
            ));
            ServerPlayNetworking.send(
                    player,
                    BuildingPlan3DPreviewPayload.show(worldId, validation.valid(), previewPositions)
            );
            player.sendMessage(
                    Text.translatable(validation.valid()
                                    ? "message.mythicrpg.building_plan_3d.preview_ready"
                                    : "message.mythicrpg.building_plan_3d.preview_invalid")
                            .formatted(validation.valid() ? Formatting.GREEN : Formatting.RED),
                    true
            );
            if (!validation.valid()) {
                BuildingSoundFeedback.error(player);
            }
            return;
        }

        if (!validation.valid()) {
            sendValidationFailure(player, validation);
            ServerPlayNetworking.send(
                    player,
                    BuildingPlan3DPreviewPayload.show(worldId, false, previewPositions)
            );
            return;
        }
        if (validation.toPlace().isEmpty()) {
            player.sendMessage(Text.translatable("message.mythicrpg.building_plan_3d.nothing_to_build")
                    .formatted(Formatting.YELLOW), true);
            clearPreview(player, false);
            return;
        }

        Map<Item, Integer> reserved = player.isCreative()
                ? new IdentityHashMap<>()
                : BuildingPlan2DManager.reserveMaterials(player, validation.required());
        if (!player.isCreative() && reserved == null) {
            sendValidationFailure(player, BuildingPlan2DManager.validate(player, world, allPlacements, true));
            return;
        }

        boolean started = BuildingPlan2DManager.startSharedJob(
                player,
                BuildingPlan2DManager.JobKind.PLAN_3D,
                worldId,
                validation.toPlace(),
                reserved
        );
        if (!started) {
            if (reserved != null && !reserved.isEmpty()) {
                BuildingPlan2DManager.refundItemMap(player, reserved, false);
            }
            sendError(player, Text.translatable("message.mythicrpg.building_plan_3d.already_active"));
            return;
        }

        clearPreview(player, false);
        BuildingSoundFeedback.buildStarted(player, anchor);
        player.sendMessage(Text.translatable(
                "message.mythicrpg.building_plan_3d.started",
                validation.toPlace().size()
        ).formatted(Formatting.GREEN), true);
    }

    public static boolean cancelInteractiveState(ServerPlayerEntity player) {
        if (BuildingPlan2DManager.cancelActiveJob(player, true)) return true;
        if (cancelPreviewOnly(player, true)) return true;
        return BuildingPlan2DManager.cancelPreviewOnly(player, true);
    }

    static boolean cancelPreviewOnly(ServerPlayerEntity player, boolean notify) {
        if (player == null || !PREVIEWS.containsKey(player.getUuid())) return false;
        clearPreview(player, false);
        if (notify) {
            player.sendMessage(Text.translatable("message.mythicrpg.building_plan_3d.preview_cancelled")
                    .formatted(Formatting.YELLOW), true);
        }
        return true;
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        if (player != null) PREVIEWS.remove(player.getUuid());
    }

    public static void clearAll() {
        PREVIEWS.clear();
    }

    static void clearPreview(ServerPlayerEntity player, boolean notify) {
        if (player == null) return;
        boolean removed = PREVIEWS.remove(player.getUuid()) != null;
        ServerPlayNetworking.send(player, BuildingPlan3DPreviewPayload.clear());
        if (notify && removed) {
            player.sendMessage(Text.translatable("message.mythicrpg.building_plan_3d.preview_cancelled")
                    .formatted(Formatting.YELLOW), true);
        }
    }

    private static void cleanupPreviews(MinecraftServer server) {
        Iterator<Map.Entry<UUID, PreviewSession>> iterator = PREVIEWS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PreviewSession> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            PreviewSession preview = entry.getValue();
            ServerWorld world = player.getServerWorld();
            if (!canUse(player)
                    || !dimensionId(world).equals(preview.dimensionId())
                    || world.getTime() > preview.expiresAtTick()) {
                iterator.remove();
                ServerPlayNetworking.send(player, BuildingPlan3DPreviewPayload.clear());
            }
        }
    }

    private static boolean isPlanValid(BuildingPlan3DData.Plan plan) {
        if (plan == null || plan.id() == null
                || plan.sizeX() <= 0 || plan.sizeY() <= 0 || plan.sizeZ() <= 0
                || plan.sizeX() > MAX_SIZE || plan.sizeY() > MAX_SIZE || plan.sizeZ() > MAX_SIZE
                || plan.entries() == null || plan.entries().isEmpty()
                || plan.entries().size() > plan.sizeX() * plan.sizeY() * plan.sizeZ()) {
            return false;
        }
        HashSet<Long> offsets = new HashSet<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BuildingPlan3DData.Entry entry : plan.entries()) {
            if (entry == null || entry.offset() == null || entry.state() == null
                    || entry.appearance() == null
                    || Math.abs(entry.offset().getX()) >= plan.sizeX()
                    || Math.abs(entry.offset().getY()) >= plan.sizeY()
                    || Math.abs(entry.offset().getZ()) >= plan.sizeZ()
                    || !offsets.add(entry.offset().asLong())
                    || !BuildingBlockCatalog.isEligible(entry.state().getBlock())
                    || !entry.state().getFluidState().isEmpty()
                    || entry.state().getBlock().asItem() == Items.AIR
                    || !BlankBlockMaterialRegistry.isValid(entry.appearance())
                    || (!entry.state().isOf(ModBlocks.BLANK_BLOCK) && !entry.appearance().isEmpty())) {
                return false;
            }
            minX = Math.min(minX, entry.offset().getX());
            minY = Math.min(minY, entry.offset().getY());
            minZ = Math.min(minZ, entry.offset().getZ());
            maxX = Math.max(maxX, entry.offset().getX());
            maxY = Math.max(maxY, entry.offset().getY());
            maxZ = Math.max(maxZ, entry.offset().getZ());
        }
        return !(minX < 0 && maxX > 0)
                && !(minY < 0 && maxY > 0)
                && !(minZ < 0 && maxZ > 0)
                && BuildingPlanTransforms.canRotate(plan.entries(), plan.rotation());
    }

    private static List<BuildingPlan2DManager.Placement> placements(
            BuildingPlan3DData.Plan plan,
            BlockPos anchor
    ) {
        List<BuildingPlan2DManager.Placement> placements = new ArrayList<>(plan.entries().size());
        BuildingStructureRotation rotation = plan.rotation();
        for (BuildingPlan3DData.Entry entry : plan.entries()) {
            BlockPos rotatedOffset = BuildingPlanTransforms.rotateVector(entry.offset(), rotation);
            BlockState rotatedState = BuildingPlanTransforms.rotateState(entry.state(), rotation)
                    .orElse(entry.state());
            BlankBlockAppearance rotatedAppearance = BuildingPlanTransforms.rotateAppearance(
                    entry.appearance(),
                    rotation
            );
            BlockPos pos = anchor.add(rotatedOffset);
            placements.add(new BuildingPlan2DManager.Placement(
                    pos,
                    rotatedState,
                    rotatedState.getBlock().asItem(),
                    Math.abs(rotatedOffset.getX())
                            + Math.abs(rotatedOffset.getY())
                            + Math.abs(rotatedOffset.getZ()),
                    rotatedAppearance
            ));
        }
        placements.sort(
                Comparator.comparingInt((BuildingPlan2DManager.Placement p) -> p.pos().getY())
                        .thenComparingInt(BuildingPlan2DManager.Placement::distanceFromAnchor)
                        .thenComparingLong(p -> p.pos().asLong())
        );
        return List.copyOf(placements);
    }

    private static void sendValidationFailure(
            ServerPlayerEntity player,
            BuildingPlan2DManager.Validation validation
    ) {
        Text message = switch (validation.failure()) {
            case MISSING_MATERIAL -> Text.translatable(
                    "message.mythicrpg.building_plan_3d.missing_material",
                    new net.minecraft.item.ItemStack(validation.missingItem()).getName(),
                    validation.requiredCount(),
                    validation.availableCount()
            );
            case UNLOADED -> Text.translatable("message.mythicrpg.building_plan_3d.unloaded");
            case PROTECTED -> Text.translatable("message.mythicrpg.building_plan_3d.protected");
            case BLOCKED -> Text.translatable("message.mythicrpg.building_plan_3d.blocked");
            case UNSUPPORTED -> Text.translatable("message.mythicrpg.building_plan_3d.unsupported_destination");
            case NONE -> Text.translatable("message.mythicrpg.building_plan_3d.preview_invalid");
        };
        sendError(player, message);
    }

    private static void sendError(ServerPlayerEntity player, Text message) {
        player.sendMessage(message.copy().formatted(Formatting.RED), true);
        BuildingSoundFeedback.error(player);
    }

    private static String dimensionId(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }

    public record CaptureResult(
            boolean success,
            BuildingPlan3DData.Plan plan,
            String messageKey,
            Object[] messageArgs
    ) {
        static CaptureResult success(BuildingPlan3DData.Plan plan) {
            return new CaptureResult(true, plan, "", new Object[0]);
        }
        static CaptureResult failure(String key, Object... args) {
            return new CaptureResult(false, null, key, args);
        }
    }

    private record PreviewSession(UUID planId, String dimensionId, BlockPos anchor, long expiresAtTick) {}
}
