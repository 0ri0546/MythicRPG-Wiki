package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModBlocks;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.mixin.PlayerManagerBuildingPlanInvoker;
import com.mythicrpg.mixin.ServerChunkLoadingManagerBuildingPlanInvoker;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative capture, preview, material reservation and progressive placement for 2D plans. */
public final class BuildingPlan2DManager {
    public static final int BASE_MAX_SIZE = 8;
    public static final int UPGRADED_MAX_SIZE = 12;
    private static final int BLOCKS_PER_JOB_TICK = 6;
    private static final int GLOBAL_BLOCKS_PER_TICK = 24;
    private static final long PREVIEW_LIFETIME_TICKS = 20L * 30L;
    private static final int CHECKPOINT_INTERVAL_PLACEMENTS = 24;
    private static final long CHECKPOINT_INTERVAL_TICKS = 20L * 10L;

    private static final Map<UUID, PreviewSession> PREVIEWS = new HashMap<>();
    private static final LinkedHashMap<UUID, BuildJob> JOBS = new LinkedHashMap<>();
    private static final ArrayList<UUID> JOB_ORDER = new ArrayList<>();
    private static boolean jobOrderDirty = true;
    private static int roundRobinStart;

    private BuildingPlan2DManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BuildingPlan2DManager::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> restoreStoredJob(handler.player, true))
        );
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Map.Entry<UUID, BuildJob> entry : List.copyOf(JOBS.entrySet())) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null) {
                    storeCurrentReceipt(player, entry.getValue());
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            clearJobs();
            PREVIEWS.clear();
            roundRobinStart = 0;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                boolean dropReserved = !player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY);
                cancelJob(player, dropReserved, false);
                clearPreview(player);
            }
        });
    }

    public static boolean canUse(ServerPlayerEntity player) {
        return SkillTreeManager.hasBonus(
                player,
                SkillType.BUILDING,
                BonusType.BUILD_PLAN_2D_8
        );
    }

    public static int maxSize(ServerPlayerEntity player) {
        if (!canUse(player)) {
            return 0;
        }
        return SkillTreeManager.hasBonus(
                player,
                SkillType.BUILDING,
                BonusType.BUILD_PLAN_2D_12
        ) ? UPGRADED_MAX_SIZE : BASE_MAX_SIZE;
    }

    /** Shared activity gate used by 2D and 3D plans. */
    public static boolean hasActiveJob(ServerPlayerEntity player) {
        return player != null && activeJob(player) != null;
    }

    /** Cancels whichever Building plan job is active for the player. */
    public static boolean cancelActiveJob(ServerPlayerEntity player, boolean notify) {
        if (player == null || activeJob(player) == null) {
            return false;
        }
        cancelJob(player, false, notify);
        return true;
    }

    /** Clears only the 2D destination preview, without touching a running job. */
    static void clearPreviewOnly(ServerPlayerEntity player) {
        clearPreview(player);
    }

    static boolean cancelPreviewOnly(ServerPlayerEntity player, boolean notify) {
        if (player == null || !PREVIEWS.containsKey(player.getUuid())) {
            return false;
        }
        clearPreview(player);
        if (notify) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_plan_2d.preview_cancelled")
                            .formatted(Formatting.YELLOW),
                    true
            );
        }
        return true;
    }

    /** Starts a prevalidated job through the shared 2D/3D scheduler. */
    static boolean startSharedJob(
            ServerPlayerEntity player,
            JobKind kind,
            String worldId,
            List<Placement> placements,
            Map<Item, Integer> reserved
    ) {
        if (player == null || kind == null || placements == null || placements.isEmpty()
                || worldId == null || JOBS.containsKey(player.getUuid())) {
            return false;
        }
        return startPersistentJob(
                player,
                kind,
                worldId,
                placements,
                reserved == null ? new IdentityHashMap<>() : reserved
        );
    }

    private static boolean canUseJob(ServerPlayerEntity player, JobKind kind) {
        return switch (kind) {
            case PLAN_2D -> canUse(player);
            case PLAN_3D -> SkillTreeManager.hasBonus(
                    player,
                    SkillType.BUILDING,
                    BonusType.BUILD_PLAN_3D
            );
        };
    }

    public static CaptureResult capture(
            ServerPlayerEntity player,
            ServerWorld world,
            BuildingPlan2DData.Selection first,
            BlockPos second
    ) {
        int maxSize = maxSize(player);
        if (maxSize <= 0) {
            return CaptureResult.failure("message.mythicrpg.building_plan_2d.locked");
        }

        String worldId = dimensionId(world);
        if (!worldId.equals(first.dimensionId())) {
            return CaptureResult.failure("message.mythicrpg.building_plan_2d.wrong_dimension");
        }

        if (coordinate(first.pos(), first.normalAxis()) != coordinate(second, first.normalAxis())) {
            return CaptureResult.failure("message.mythicrpg.building_plan_2d.not_flat");
        }

        Direction.Axis[] tangents = tangentAxes(first.normalAxis());
        int deltaU = coordinate(second, tangents[0]) - coordinate(first.pos(), tangents[0]);
        int deltaV = coordinate(second, tangents[1]) - coordinate(first.pos(), tangents[1]);
        int sizeU = Math.abs(deltaU) + 1;
        int sizeV = Math.abs(deltaV) + 1;
        if (sizeU > maxSize || sizeV > maxSize) {
            return CaptureResult.failure(
                    "message.mythicrpg.building_plan_2d.too_large",
                    sizeU,
                    sizeV,
                    maxSize,
                    maxSize
            );
        }

        int stepU = deltaU < 0 ? -1 : 1;
        int stepV = deltaV < 0 ? -1 : 1;
        List<BuildingPlan2DData.Entry> entries = new ArrayList<>(sizeU * sizeV);

        for (int u = 0; u < sizeU; u++) {
            for (int v = 0; v < sizeV; v++) {
                BlockPos offset = offset(tangents[0], u * stepU)
                        .add(offset(tangents[1], v * stepV));
                BlockPos sourcePos = first.pos().add(offset);
                if (!world.isChunkLoaded(sourcePos)) {
                    return CaptureResult.failure("message.mythicrpg.building_plan_2d.unloaded");
                }
                if (!world.isInBuildLimit(sourcePos) || !world.getWorldBorder().contains(sourcePos)) {
                    return CaptureResult.failure("message.mythicrpg.building_plan_2d.outside_world");
                }

                BlockState state = world.getBlockState(sourcePos);
                if (state.isAir()) {
                    continue;
                }

                BlankBlockAppearance appearance = BlankBlockAppearance.EMPTY;
                var sourceBlockEntity = world.getBlockEntity(sourcePos);
                if (sourceBlockEntity != null) {
                    if (state.isOf(ModBlocks.BLANK_BLOCK)
                            && sourceBlockEntity instanceof BlankBlockEntity blank) {
                        appearance = blank.appearance();
                    } else {
                        return CaptureResult.failure(
                                "message.mythicrpg.building_plan_2d.unsupported",
                                state.getBlock().getName()
                        );
                    }
                }

                if (!state.getFluidState().isEmpty()
                        || !BuildingBlockCatalog.isEligible(state.getBlock())
                        || state.getBlock().asItem() == Items.AIR) {
                    return CaptureResult.failure(
                            "message.mythicrpg.building_plan_2d.unsupported",
                            state.getBlock().getName()
                    );
                }
                entries.add(new BuildingPlan2DData.Entry(offset, state, appearance));
            }
        }

        if (entries.isEmpty()) {
            return CaptureResult.failure("message.mythicrpg.building_plan_2d.empty");
        }

        BuildingPlan2DData.Plan plan = new BuildingPlan2DData.Plan(
                UUID.randomUUID(),
                first.normalAxis(),
                sizeU,
                sizeV,
                List.copyOf(entries)
        ).withSource(worldId, first.pos(), second);
        clearPreview(player);
        return CaptureResult.success(plan);
    }

    /** First click shows/moves the preview; a second click on the same anchor starts the job. */
    public static void previewOrConfirm(
            ServerPlayerEntity player,
            ServerWorld world,
            BuildingPlan2DData.Plan plan,
            BlockPos anchor
    ) {
        BuildingPlan3DManager.clearPreview(player, false);
        if (!canUse(player)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_plan_2d.locked").formatted(Formatting.RED),
                    true
            );
            clearPreview(player);
            return;
        }
        if (!isPlanValid(plan)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_plan_2d.corrupt")
                            .formatted(Formatting.RED),
                    true
            );
            clearPreview(player);
            return;
        }
        if (plan.sizeU() > maxSize(player) || plan.sizeV() > maxSize(player)) {
            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.building_plan_2d.upgrade_required",
                            plan.sizeU(),
                            plan.sizeV(),
                            maxSize(player),
                            maxSize(player)
                    ).formatted(Formatting.RED),
                    true
            );
            clearPreview(player);
            return;
        }
        if (activeJob(player) != null) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_plan_2d.already_active")
                            .formatted(Formatting.RED),
                    true
            );
            return;
        }

        String worldId = dimensionId(world);
        List<Placement> allPlacements = placements(plan, anchor);
        Validation validation = validate(player, world, allPlacements, true);
        List<BlockPos> previewPositions = allPlacements.stream().map(Placement::pos).toList();

        PreviewSession previous = PREVIEWS.get(player.getUuid());
        boolean confirms = previous != null
                && previous.planId().equals(plan.id())
                && previous.dimensionId().equals(worldId)
                && previous.anchor().equals(anchor)
                && world.getTime() <= previous.expiresAtTick();

        if (!confirms) {
            PREVIEWS.put(
                    player.getUuid(),
                    new PreviewSession(plan.id(), worldId, anchor, world.getTime() + PREVIEW_LIFETIME_TICKS)
            );
            ServerPlayNetworking.send(
                    player,
                    BuildingPlan2DPreviewPayload.show(worldId, validation.valid(), previewPositions)
            );
            player.sendMessage(
                    Text.translatable(
                            validation.valid()
                                    ? "message.mythicrpg.building_plan_2d.preview_ready"
                                    : "message.mythicrpg.building_plan_2d.preview_invalid"
                    ).formatted(validation.valid() ? Formatting.GREEN : Formatting.RED),
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
                    BuildingPlan2DPreviewPayload.show(worldId, false, previewPositions)
            );
            return;
        }

        if (validation.toPlace().isEmpty()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_plan_2d.nothing_to_build")
                            .formatted(Formatting.YELLOW),
                    true
            );
            clearPreview(player);
            return;
        }

        Map<Item, Integer> reserved = player.isCreative()
                ? new IdentityHashMap<>()
                : reserveMaterials(player, validation.required());
        if (!player.isCreative() && reserved == null) {
            Validation retry = validate(player, world, allPlacements, true);
            sendValidationFailure(player, retry);
            return;
        }

        if (!startPersistentJob(
                player,
                JobKind.PLAN_2D,
                worldId,
                validation.toPlace(),
                reserved == null ? new IdentityHashMap<>() : reserved
        )) {
            if (reserved != null && !reserved.isEmpty()) {
                refundItemMap(player, reserved, false);
            }
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_plan_2d.already_active")
                            .formatted(Formatting.RED),
                    true
            );
            return;
        }
        clearPreview(player);
        BuildingSoundFeedback.buildStarted(player, anchor);
        player.sendMessage(
                Text.translatable(
                        "message.mythicrpg.building_plan_2d.started",
                        validation.toPlace().size()
                ).formatted(Formatting.GREEN),
                true
        );
    }

    /** Cancels a running job first, otherwise only the current destination preview. */
    public static boolean cancelInteractiveState(ServerPlayerEntity player) {
        if (activeJob(player) != null) {
            cancelJob(player, false, true);
            return true;
        }
        if (cancelPreviewOnly(player, true)) {
            return true;
        }
        return BuildingPlan3DManager.cancelPreviewOnly(player, true);
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        BuildJob job = removeJob(player.getUuid());
        if (job != null) {
            storeCurrentReceipt(player, job);
            forceSavePlayer(player);
        }
        PREVIEWS.remove(player.getUuid());
    }

    private static void tick(MinecraftServer server) {
        cleanupPreviews(server);
        if (JOBS.isEmpty()) {
            roundRobinStart = 0;
            return;
        }

        List<UUID> ids = jobOrder();
        int size = ids.size();
        int start = Math.floorMod(roundRobinStart, size);
        int globalBudget = GLOBAL_BLOCKS_PER_TICK;

        for (int offset = 0; offset < size && globalBudget > 0; offset++) {
            UUID playerId = ids.get((start + offset) % size);
            BuildJob job = JOBS.get(playerId);
            if (job == null) {
                continue;
            }
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                continue;
            }
            int used = processJob(player, job, Math.min(BLOCKS_PER_JOB_TICK, globalBudget));
            globalBudget -= used;
        }

        roundRobinStart = size <= 1 ? 0 : (start + 1) % size;
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
                ServerPlayNetworking.send(player, BuildingPlan2DPreviewPayload.clear());
            }
        }
    }

    private static int processJob(ServerPlayerEntity player, BuildJob job, int budget) {
        if (job.terminalOutcome != TerminalOutcome.NONE) {
            persistSettlementAndFinish(player, job);
            return 0;
        }
        if (!canUseJob(player, job.kind) || (job.creative && !player.isCreative())) {
            cancelJob(player, false, true);
            return 0;
        }
        if (!(player.getWorld() instanceof ServerWorld world)
                || !dimensionId(world).equals(job.dimensionId)) {
            job.paused = true;
            return 0;
        }

        if (job.recoveryPending) {
            RecoveryStatus recovery = reconcileRecoveredJob(player, world, job);
            if (recovery == RecoveryStatus.PAUSED) {
                job.paused = true;
                return 0;
            }
            if (recovery == RecoveryStatus.INVALID) {
                cancelJob(player, false, true);
                return 0;
            }
            if (job.cursor >= job.placements.size()) {
                completeJob(player, job);
                return 0;
            }
        }
        if (!escrowCoversRemaining(job)) {
            cancelJob(player, false, true);
            return 0;
        }

        job.paused = false;
        int processed = 0;
        while (processed < budget && job.cursor < job.placements.size()) {
            Placement placement = job.placements.get(job.cursor);
            BlockPos pos = placement.pos();

            // Never load a chunk for a plan job. The job remains paused until the
            // destination is naturally loaded again.
            if (!world.isChunkLoaded(pos)) {
                job.paused = true;
                break;
            }

            BlockState current = world.getBlockState(pos);
            if (matchesPlacement(world, pos, current, placement)) {
                // Preserve the pre-transaction behavior for a destination completed
                // by another action while the live job is running: skip it and
                // refund the now-unused escrow when the job completes.
                job.cursor++;
                job.dirty = true;
                processed++;
                continue;
            }

            if (!world.isInBuildLimit(pos)
                    || !world.getWorldBorder().contains(pos)
                    || !world.canPlayerModifyAt(player, pos)
                    || world.getBlockEntity(pos) != null
                    || !current.isReplaceable()
                    || !current.getFluidState().isEmpty()
                    || !placement.state().canPlaceAt(world, pos)
                    || !world.canPlace(placement.state(), pos, ShapeContext.of(player))) {
                cancelJob(player, false, true);
                return processed;
            }

            if (!job.creative && !hasPlacementMaterials(job.reserved, placement)) {
                cancelJob(player, false, true);
                return processed;
            }

            if (!world.setBlockState(pos, placement.state(), Block.NOTIFY_ALL)) {
                cancelJob(player, false, true);
                return processed;
            }

            if (placement.isBlank()) {
                if (!(world.getBlockEntity(pos) instanceof BlankBlockEntity blank)) {
                    world.setBlockState(
                            pos,
                            Blocks.AIR.getDefaultState(),
                            Block.NOTIFY_ALL | Block.SKIP_DROPS
                    );
                    cancelJob(player, false, true);
                    return processed;
                }
                blank.setAppearance(placement.appearance());
            }

            if (!consumePlacementMaterials(job, placement)) {
                world.setBlockState(
                        pos,
                        Blocks.AIR.getDefaultState(),
                        Block.NOTIFY_ALL | Block.SKIP_DROPS
                );
                cancelJob(player, false, true);
                return processed;
            }
            job.cursor++;
            job.dirty = true;
            processed++;
        }

        if (job.cursor >= job.placements.size()) {
            completeJob(player, job);
            return processed;
        }

        if (job.dirty) {
            syncJobToPlayer(player, job);
        }
        long now = world.getTime();
        if (job.cursor - job.lastSavedCursor >= CHECKPOINT_INTERVAL_PLACEMENTS
                || now - job.lastSavedTick >= CHECKPOINT_INTERVAL_TICKS) {
            forceSavePlayer(player);
            job.lastSavedCursor = job.cursor;
            job.lastSavedTick = now;
        }
        return processed;
    }

    private static boolean isPlanValid(BuildingPlan2DData.Plan plan) {
        if (plan == null
                || plan.id() == null
                || plan.normalAxis() == null
                || plan.sizeU() <= 0
                || plan.sizeV() <= 0
                || plan.sizeU() > UPGRADED_MAX_SIZE
                || plan.sizeV() > UPGRADED_MAX_SIZE
                || plan.entries().isEmpty()
                || plan.entries().size() > plan.sizeU() * plan.sizeV()) {
            return false;
        }

        Direction.Axis[] tangents = tangentAxes(plan.normalAxis());
        HashSet<Long> offsets = new HashSet<>();
        for (BuildingPlan2DData.Entry entry : plan.entries()) {
            if (entry == null || entry.offset() == null || entry.state() == null
                    || entry.appearance() == null) {
                return false;
            }
            boolean blank = entry.state().isOf(ModBlocks.BLANK_BLOCK);
            if (coordinate(entry.offset(), plan.normalAxis()) != 0
                    || Math.abs(coordinate(entry.offset(), tangents[0])) >= plan.sizeU()
                    || Math.abs(coordinate(entry.offset(), tangents[1])) >= plan.sizeV()
                    || !offsets.add(entry.offset().asLong())
                    || !BuildingBlockCatalog.isEligible(entry.state().getBlock())
                    || !entry.state().getFluidState().isEmpty()
                    || entry.state().getBlock().asItem() == Items.AIR
                    || !BlankBlockMaterialRegistry.isValid(entry.appearance())
                    || (!blank && !entry.appearance().isEmpty())) {
                return false;
            }
        }
        return BuildingPlanTransforms.canRotate(plan.entries(), plan.rotation());
    }

    static List<Placement> placements(BuildingPlan2DData.Plan plan, BlockPos anchor) {
        List<Placement> placements = new ArrayList<>(plan.entries().size());
        BuildingStructureRotation rotation = plan.rotation();
        for (BuildingPlan2DData.Entry entry : plan.entries()) {
            BlockPos rotatedOffset = BuildingPlanTransforms.rotateVector(entry.offset(), rotation);
            BlockState rotatedState = BuildingPlanTransforms.rotateState(entry.state(), rotation)
                    .orElse(entry.state());
            BlankBlockAppearance rotatedAppearance = BuildingPlanTransforms.rotateAppearance(
                    entry.appearance(),
                    rotation
            );
            placements.add(new Placement(
                    anchor.add(rotatedOffset),
                    rotatedState,
                    rotatedState.getBlock().asItem(),
                    Math.abs(rotatedOffset.getX())
                            + Math.abs(rotatedOffset.getY())
                            + Math.abs(rotatedOffset.getZ()),
                    rotatedAppearance
            ));
        }
        placements.sort(
                Comparator.comparingInt((Placement placement) -> placement.pos().getY())
                        .thenComparingInt(Placement::distanceFromAnchor)
                        .thenComparingLong(placement -> placement.pos().asLong())
        );
        return placements;
    }

    static Validation validate(
            ServerPlayerEntity player,
            ServerWorld world,
            List<Placement> placements,
            boolean checkInventory
    ) {
        List<Placement> toPlace = new ArrayList<>(placements.size());
        Map<Item, Integer> required = new IdentityHashMap<>();

        for (Placement placement : placements) {
            BlockPos pos = placement.pos();
            if (!world.isChunkLoaded(pos)) {
                return Validation.failure(Failure.UNLOADED);
            }
            if (!world.isInBuildLimit(pos)
                    || !world.getWorldBorder().contains(pos)
                    || !world.canPlayerModifyAt(player, pos)) {
                return Validation.failure(Failure.PROTECTED);
            }

            BlockState current = world.getBlockState(pos);
            if (matchesPlacement(world, pos, current, placement)) {
                continue;
            }
            if (world.getBlockEntity(pos) != null
                    || !current.isReplaceable()
                    || !current.getFluidState().isEmpty()
                    || !placement.state().canPlaceAt(world, pos)
                    || !world.canPlace(placement.state(), pos, ShapeContext.of(player))) {
                return Validation.failure(Failure.BLOCKED);
            }
            if (placement.item() == Items.AIR) {
                return Validation.failure(Failure.UNSUPPORTED);
            }

            toPlace.add(placement);
            if (!addRequiredMaterials(required, placement)) {
                return Validation.failure(Failure.UNSUPPORTED);
            }
        }

        if (checkInventory && !player.isCreative()) {
            IdentityHashMap<Item, Integer> available = new IdentityHashMap<>();
            for (int slot = 0; slot < player.getInventory().size(); slot++) {
                ItemStack stack = player.getInventory().getStack(slot);
                Item item = stack.getItem();
                if (required.containsKey(item) && isReservableStack(stack, item)) {
                    available.merge(item, stack.getCount(), Integer::sum);
                }
            }
            for (Map.Entry<Item, Integer> entry : required.entrySet()) {
                int count = available.getOrDefault(entry.getKey(), 0);
                if (count < entry.getValue()) {
                    return Validation.missing(
                            entry.getKey(),
                            entry.getValue(),
                            count,
                            List.copyOf(toPlace),
                            required
                    );
                }
            }
        }

        return Validation.success(List.copyOf(toPlace), required);
    }

    static Map<Item, Integer> reserveMaterials(
            ServerPlayerEntity player,
            Map<Item, Integer> required
    ) {
        IdentityHashMap<Item, Integer> available = new IdentityHashMap<>();
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            Item item = stack.getItem();
            if (required.containsKey(item) && isReservableStack(stack, item)) {
                available.merge(item, stack.getCount(), Integer::sum);
            }
        }
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return null;
            }
        }

        IdentityHashMap<Item, Integer> remaining = new IdentityHashMap<>();
        remaining.putAll(required);
        IdentityHashMap<Item, Integer> reserved = new IdentityHashMap<>();
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            Item item = stack.getItem();
            int needed = remaining.getOrDefault(item, 0);
            if (needed <= 0 || !isReservableStack(stack, item)) {
                continue;
            }
            int removed = Math.min(needed, stack.getCount());
            stack.decrement(removed);
            remaining.put(item, needed - removed);
            reserved.merge(item, removed, Integer::sum);
        }

        for (Map.Entry<Item, Integer> entry : remaining.entrySet()) {
            if (entry.getValue() > 0) {
                refundItemMap(player, reserved, false);
                return null;
            }
        }
        player.getInventory().markDirty();
        return reserved;
    }

    private static boolean isReservableStack(ItemStack stack, Item item) {
        if (!stack.isOf(item)) {
            return false;
        }
        if (item == ModBlocks.BLANK_BLOCK.asItem()) {
            return BlankBlockItemData.readStrict(stack)
                    .map(BlankBlockAppearance::isEmpty)
                    .orElse(false);
        }
        return true;
    }

    private static boolean consumeReserved(Map<Item, Integer> escrow, Item item) {
        if (escrow.isEmpty()) {
            return false;
        }
        int remaining = escrow.getOrDefault(item, 0);
        if (remaining <= 0) {
            return false;
        }
        if (remaining == 1) {
            escrow.remove(item);
        } else {
            escrow.put(item, remaining - 1);
        }
        return true;
    }

    private static boolean consumePlacementMaterials(BuildJob job, Placement placement) {
        if (job.creative) {
            return true;
        }
        return consumePlacementMaterials(job.reserved, placement);
    }

    private static boolean consumePlacementMaterials(
            Map<Item, Integer> escrow,
            Placement placement
    ) {
        IdentityHashMap<Item, Integer> required = new IdentityHashMap<>();
        if (!addRequiredMaterials(required, placement)) {
            return false;
        }
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            if (escrow.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            int remaining = escrow.get(entry.getKey()) - entry.getValue();
            if (remaining == 0) {
                escrow.remove(entry.getKey());
            } else {
                escrow.put(entry.getKey(), remaining);
            }
        }
        return true;
    }

    private static boolean hasPlacementMaterials(
            Map<Item, Integer> escrow,
            Placement placement
    ) {
        IdentityHashMap<Item, Integer> required = new IdentityHashMap<>();
        if (!addRequiredMaterials(required, placement)) {
            return false;
        }
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            if (escrow.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static boolean addRequiredMaterials(Map<Item, Integer> required, Placement placement) {
        if (placement.item() == Items.AIR) {
            return false;
        }
        required.merge(placement.item(), 1, Integer::sum);
        if (!placement.isBlank()) {
            return true;
        }
        for (net.minecraft.util.Identifier id : placement.appearance().configuredMaterials()) {
            Item item = BlankBlockMaterialRegistry.item(id);
            if (item == Items.AIR) {
                return false;
            }
            required.merge(item, 1, Integer::sum);
        }
        return true;
    }

    private static boolean matchesPlacement(
            ServerWorld world,
            BlockPos pos,
            BlockState current,
            Placement placement
    ) {
        if (!current.equals(placement.state())) {
            return false;
        }
        if (!placement.isBlank()) {
            return true;
        }
        return world.getBlockEntity(pos) instanceof BlankBlockEntity blank
                && blank.appearance().equals(placement.appearance());
    }

    private static void cancelJob(
            ServerPlayerEntity player,
            boolean dropAtPlayer,
            boolean notify
    ) {
        BuildJob job = activeJob(player);
        if (job == null) {
            return;
        }
        if (job.terminalOutcome != TerminalOutcome.NONE) {
            persistSettlementAndFinish(player, job);
            return;
        }
        prepareRecoveryRefund(player, job);

        // Commit any blocks already placed before releasing the escrow.
        syncJobToPlayer(player, job);
        if (!forceSavePlayer(player)) {
            job.paused = true;
            return;
        }
        ServerWorld destination = findJobWorld(player.getServer(), job.dimensionId);
        if (destination != null && !flushDestinationChunks(destination, job)) {
            job.paused = true;
            return;
        }

        if (!refundReserved(player, job, dropAtPlayer)) {
            job.paused = true;
            syncJobToPlayer(player, job);
            forceSavePlayer(player);
            return;
        }
        job.terminalOutcome = TerminalOutcome.CANCELLED;
        job.terminalNotify = notify;
        persistSettlementAndFinish(player, job);
    }

    private static boolean refundReserved(
            ServerPlayerEntity player,
            BuildJob job,
            boolean dropAtPlayer
    ) {
        return deliverEscrow(player, job.reserved, dropAtPlayer);
    }

    static void refundItemMap(
            ServerPlayerEntity player,
            Map<Item, Integer> items,
            boolean dropAtPlayer
    ) {
        IdentityHashMap<Item, Integer> temporary = BuildingPlanJobData.copyIdentity(items);
        if (!deliverEscrow(player, temporary, dropAtPlayer) && !temporary.isEmpty()) {
            // This path is used only for pre-job rollback/corrupt-data salvage. Keep
            // the warning explicit; active jobs retain their persistent receipt.
            System.err.println("[MythicRPG] Could not fully deliver a Building plan refund to "
                    + player.getGameProfile().getName());
        }
    }

    private static boolean deliverEscrow(
            ServerPlayerEntity player,
            Map<Item, Integer> escrow,
            boolean dropAtPlayer
    ) {
        if (escrow.isEmpty()) {
            return true;
        }
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return false;
        }

        IdentityHashMap<Item, Integer> spawnedCounts = new IdentityHashMap<>();
        ArrayList<ItemEntity> spawnedEntities = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : List.copyOf(escrow.entrySet())) {
            Item item = entry.getKey();
            int remaining = entry.getValue();
            int maxStack = new ItemStack(item).getMaxCount();
            while (remaining > 0) {
                int offered = Math.min(remaining, maxStack);
                ItemStack stack = new ItemStack(item, offered);
                if (!dropAtPlayer) {
                    player.getInventory().insertStack(stack);
                    int inserted = offered - stack.getCount();
                    if (inserted > 0) {
                        decrementEscrow(escrow, item, inserted);
                        remaining -= inserted;
                    }
                    if (stack.isEmpty()) {
                        continue;
                    }
                }

                int toDrop = stack.getCount();
                ItemEntity entity = new ItemEntity(
                        world,
                        player.getX(),
                        player.getY() + 0.5,
                        player.getZ(),
                        stack.copy()
                );
                if (!world.spawnEntity(entity)) {
                    break;
                }
                spawnedEntities.add(entity);
                spawnedCounts.merge(item, toDrop, Integer::sum);
                decrementEscrow(escrow, item, toDrop);
                remaining -= toDrop;
            }
        }
        player.getInventory().markDirty();

        if (!spawnedEntities.isEmpty()
                && !flushChunkSet(
                        world,
                        java.util.Set.of(ChunkPos.toLong(
                                player.getBlockX() >> 4,
                                player.getBlockZ() >> 4
                        )),
                        "refund/" + player.getUuid()
                )) {
            for (ItemEntity entity : spawnedEntities) {
                if (!entity.isRemoved()) {
                    entity.discard();
                }
            }
            for (Map.Entry<Item, Integer> entry : spawnedCounts.entrySet()) {
                escrow.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            return false;
        }
        return escrow.isEmpty();
    }

    private static void decrementEscrow(Map<Item, Integer> escrow, Item item, int count) {
        int remaining = escrow.getOrDefault(item, 0) - count;
        if (remaining <= 0) {
            escrow.remove(item);
        } else {
            escrow.put(item, remaining);
        }
    }

    private static boolean escrowCoversRemaining(BuildJob job) {
        if (job.creative) {
            return job.reserved.isEmpty();
        }
        IdentityHashMap<Item, Integer> expected = new IdentityHashMap<>();
        for (int index = job.cursor; index < job.placements.size(); index++) {
            if (!addRequiredMaterials(expected, job.placements.get(index))) {
                return false;
            }
        }
        for (Map.Entry<Item, Integer> entry : expected.entrySet()) {
            if (job.reserved.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static RecoveryStatus reconcileRecoveredJob(
            ServerPlayerEntity player,
            ServerWorld world,
            BuildJob job
    ) {
        for (Placement placement : job.placements) {
            if (!world.isChunkLoaded(placement.pos())) {
                return RecoveryStatus.PAUSED;
            }
        }

        IdentityHashMap<Item, Integer> recovered = BuildingPlanJobData.copyIdentity(job.initialReserved);
        ArrayList<Placement> remaining = new ArrayList<>(job.placements.size());
        for (Placement placement : job.placements) {
            BlockState current = world.getBlockState(placement.pos());
            if (matchesPlacement(world, placement.pos(), current, placement)) {
                if (!job.creative && !consumePlacementMaterials(recovered, placement)) {
                    return RecoveryStatus.INVALID;
                }
            } else {
                remaining.add(placement);
            }
        }

        if (!remaining.isEmpty() && remaining.size() < job.placements.size()
                && !flushDestinationChunks(world, job)) {
            // Do not compact the persisted receipt until every already-matching
            // destination chunk has been durably committed.
            return RecoveryStatus.PAUSED;
        }

        job.reserved = recovered;
        job.recoveryPending = false;
        job.dirty = true;
        if (remaining.isEmpty()) {
            // Keep the original placement list as the durable completion receipt.
            // completeJob needs it to flush every destination chunk before clearing NBT.
            job.cursor = job.placements.size();
            return RecoveryStatus.READY;
        }

        job.placements = remaining;
        job.cursor = 0;
        job.initialReserved = BuildingPlanJobData.copyIdentity(recovered);
        job.receiptStructureDirty = true;
        syncJobToPlayer(player, job);
        forceSavePlayer(player);
        job.lastSavedCursor = 0;
        job.lastSavedTick = world.getTime();
        return RecoveryStatus.READY;
    }

    private static void completeJob(ServerPlayerEntity player, BuildJob job) {
        if (job.terminalOutcome != TerminalOutcome.NONE) {
            persistSettlementAndFinish(player, job);
            return;
        }
        if (!(player.getWorld() instanceof ServerWorld world)
                || !dimensionId(world).equals(job.dimensionId)) {
            job.paused = true;
            return;
        }

        // Two-phase commit, ordered for crash safety:
        // 1) persist the still-active receipt with its remaining escrow;
        // 2) serialize and flush only the destination chunks;
        // 3) refund unused escrow and atomically save inventory + a settled marker;
        // 4) remove the settled marker in a best-effort cleanup save.
        syncJobToPlayer(player, job);
        if (!forceSavePlayer(player)) {
            job.paused = true;
            return;
        }
        if (!flushDestinationChunks(world, job)) {
            job.paused = true;
            return;
        }

        if (!refundReserved(player, job, false)) {
            job.paused = true;
            syncJobToPlayer(player, job);
            forceSavePlayer(player);
            return;
        }
        job.terminalOutcome = TerminalOutcome.COMPLETED;
        job.terminalNotify = true;
        persistSettlementAndFinish(player, job);
    }

    private static boolean persistSettlementAndFinish(
            ServerPlayerEntity player,
            BuildJob job
    ) {
        if (job == null || job.terminalOutcome == TerminalOutcome.NONE) {
            return false;
        }

        // The settled marker and the refunded inventory are written in the same
        // player-data save. If cleanup is interrupted, login removes the marker
        // without replaying the old escrow receipt.
        setSettledReceipt(player, job);
        if (!forceSavePlayer(player)) {
            job.paused = true;
            return false;
        }

        removeJob(player.getUuid());
        clearStoredJob(player);
        forceSavePlayer(player); // Best effort: the durable settled marker is already safe.

        if (job.terminalOutcome == TerminalOutcome.COMPLETED) {
            BuildingSoundFeedback.buildCompleted(player);
            player.sendMessage(
                    Text.translatable(
                            job.kind.completedMessageKey(),
                            job.totalPlacements
                    ).formatted(Formatting.GREEN),
                    true
            );
        } else if (job.terminalNotify) {
            player.sendMessage(
                    Text.translatable(job.kind.cancelledMessageKey())
                            .formatted(Formatting.YELLOW),
                    true
            );
        }
        return true;
    }

    private static boolean flushDestinationChunks(ServerWorld world, BuildJob job) {
        HashSet<Long> chunks = new HashSet<>();
        for (Placement placement : job.placements) {
            chunks.add(ChunkPos.toLong(
                    placement.pos().getX() >> 4,
                    placement.pos().getZ() >> 4
            ));
        }
        return flushChunkSet(world, chunks, playerlessJobLabel(job));
    }

    private static boolean flushChunkSet(
            ServerWorld world,
            java.util.Set<Long> chunks,
            String operationLabel
    ) {
        ArrayList<Chunk> attemptedSaves = new ArrayList<>();
        try {
            ServerChunkLoadingManagerBuildingPlanInvoker storage =
                    (ServerChunkLoadingManagerBuildingPlanInvoker)
                            world.getChunkManager().chunkLoadingManager;
            for (long packed : chunks) {
                int chunkX = ChunkPos.getPackedX(packed);
                int chunkZ = ChunkPos.getPackedZ(packed);
                Chunk chunk = world.getChunkManager().getChunk(
                        chunkX,
                        chunkZ,
                        ChunkStatus.FULL,
                        false
                );
                if (chunk != null && chunk.needsSaving()) {
                    attemptedSaves.add(chunk);
                    if (!storage.mythicrpg$saveBuildingPlanChunk(chunk)) {
                        restoreDirtyFlags(attemptedSaves);
                        return false;
                    }
                }
                // A chunk which naturally unloaded was already serialized by the
                // vanilla unload path; importantly, this lookup never loads it again.
            }
            world.getChunkManager().chunkLoadingManager.completeAll();
            return true;
        } catch (RuntimeException exception) {
            restoreDirtyFlags(attemptedSaves);
            System.err.println("[MythicRPG] Could not commit Building data for "
                    + operationLabel + ": " + exception.getMessage());
            return false;
        }
    }

    private static void restoreDirtyFlags(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            chunk.setNeedsSaving(true);
        }
    }

    private static String playerlessJobLabel(BuildJob job) {
        return job.kind.name() + "/" + job.jobId;
    }

    private static void prepareRecoveryRefund(ServerPlayerEntity player, BuildJob job) {
        if (!job.recoveryPending) {
            return;
        }
        ServerWorld world = findJobWorld(player.getServer(), job.dimensionId);
        if (world != null) {
            RecoveryStatus status = reconcileRecoveredJob(player, world, job);
            if (status != RecoveryStatus.PAUSED && status != RecoveryStatus.INVALID) {
                return;
            }
        }
        // When the destination cannot be inspected, favor loss prevention. The
        // initial escrow is the maximum amount that legitimately belonged to the job.
        for (Map.Entry<Item, Integer> entry : job.initialReserved.entrySet()) {
            job.reserved.merge(entry.getKey(), entry.getValue(), Math::max);
        }
        job.recoveryPending = false;
    }

    private static ServerWorld findJobWorld(MinecraftServer server, String dimension) {
        if (server == null || dimension == null) {
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            if (dimensionId(world).equals(dimension)) {
                return world;
            }
        }
        return null;
    }

    private static List<UUID> jobOrder() {
        if (jobOrderDirty) {
            JOB_ORDER.clear();
            JOB_ORDER.addAll(JOBS.keySet());
            jobOrderDirty = false;
        }
        return JOB_ORDER;
    }

    private static void putJob(UUID playerId, BuildJob job) {
        JOBS.put(playerId, job);
        jobOrderDirty = true;
    }

    private static BuildJob removeJob(UUID playerId) {
        BuildJob removed = JOBS.remove(playerId);
        if (removed != null) {
            jobOrderDirty = true;
        }
        return removed;
    }

    private static void clearJobs() {
        JOBS.clear();
        JOB_ORDER.clear();
        jobOrderDirty = false;
    }

    private static BuildJob activeJob(ServerPlayerEntity player) {
        if (player == null) {
            return null;
        }
        BuildJob job = JOBS.get(player.getUuid());
        return job != null ? job : restoreStoredJob(player, false);
    }

    private static boolean startPersistentJob(
            ServerPlayerEntity player,
            JobKind kind,
            String dimensionId,
            List<Placement> placements,
            Map<Item, Integer> reserved
    ) {
        if (player == null || kind == null || dimensionId == null || dimensionId.isBlank()
                || placements == null || placements.isEmpty()
                || placements.size() > BuildingPlanJobData.MAX_PLACEMENTS
                || activeJob(player) != null) {
            return false;
        }
        IdentityHashMap<Item, Integer> escrow = new IdentityHashMap<>(reserved);
        BuildJob job = new BuildJob(
                UUID.randomUUID(),
                kind,
                dimensionId,
                new ArrayList<>(placements),
                BuildingPlanJobData.copyIdentity(escrow),
                escrow,
                0,
                placements.size(),
                player.isCreative(),
                false,
                true
        );
        putJob(player.getUuid(), job);
        syncJobToPlayer(player, job);
        if (!forceSavePlayer(player)) {
            removeJob(player.getUuid());
            clearStoredJob(player);
            return false;
        }
        job.lastSavedTick = player.getServerWorld().getTime();
        return true;
    }

    private static BuildJob restoreStoredJob(ServerPlayerEntity player, boolean notify) {
        if (player == null) {
            return null;
        }
        BuildJob cached = JOBS.get(player.getUuid());
        if (cached != null) {
            return cached;
        }
        if (!(player instanceof BuildingPlanJobHolder holder)) {
            return null;
        }
        NbtCompound data = holder.mythicrpg$getBuildingPlanJobData();
        if (data.isEmpty()) {
            return null;
        }
        if (BuildingPlanJobData.isSettled(data)) {
            clearStoredJob(player);
            forceSavePlayer(player);
            return null;
        }
        var decoded = BuildingPlanJobData.read(data);
        if (decoded.isEmpty()) {
            Map<Item, Integer> salvaged = BuildingPlanJobData.readEscrowLenient(data);
            if (!salvaged.isEmpty()) {
                refundItemMap(player, salvaged, false);
            }
            clearStoredJob(player);
            forceSavePlayer(player);
            if (notify) {
                player.sendMessage(
                        Text.translatable("message.mythicrpg.building_plan.job_corrupt_refunded")
                                .formatted(Formatting.YELLOW),
                        false
                );
            }
            return null;
        }

        BuildingPlanJobData.DecodedJob stored = decoded.get();
        BuildJob job = new BuildJob(
                stored.jobId(),
                stored.kind(),
                stored.dimensionId(),
                new ArrayList<>(stored.placements()),
                BuildingPlanJobData.copyIdentity(stored.initialEscrow()),
                BuildingPlanJobData.copyIdentity(stored.remainingEscrow()),
                stored.cursor(),
                stored.totalPlacements(),
                stored.creative(),
                true,
                false
        );
        putJob(player.getUuid(), job);
        job.lastSavedTick = player.getServerWorld().getTime();
        if (notify) {
            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.building_plan.job_resumed",
                            Math.max(0, job.totalPlacements - job.cursor)
                    ).formatted(Formatting.GREEN),
                    false
            );
        }
        return job;
    }

    private static void storeCurrentReceipt(ServerPlayerEntity player, BuildJob job) {
        if (job == null) {
            return;
        }
        if (job.terminalOutcome != TerminalOutcome.NONE) {
            setSettledReceipt(player, job);
        } else {
            syncJobToPlayer(player, job);
        }
    }

    private static void setSettledReceipt(ServerPlayerEntity player, BuildJob job) {
        if (player instanceof BuildingPlanJobHolder holder) {
            holder.mythicrpg$setBuildingPlanJobData(BuildingPlanJobData.writeSettled(job.jobId));
        }
    }

    private static void syncJobToPlayer(ServerPlayerEntity player, BuildJob job) {
        if (!(player instanceof BuildingPlanJobHolder holder) || job == null) {
            return;
        }

        if (!job.receiptStructureDirty) {
            boolean patched = holder.mythicrpg$updateBuildingPlanJobProgress(
                    BuildingPlanJobData.writeProgress(
                            job.reserved,
                            job.cursor,
                            job.placements.size()
                    )
            );
            if (patched) {
                job.dirty = false;
                return;
            }
        }

        // New/reconciled jobs require one complete receipt. This is also a
        // defensive fallback if an attachment disappeared unexpectedly.
        holder.mythicrpg$setBuildingPlanJobData(BuildingPlanJobData.write(
                job.jobId,
                job.kind,
                job.dimensionId,
                job.placements,
                job.initialReserved,
                job.reserved,
                job.cursor,
                job.totalPlacements,
                job.creative
        ));
        job.receiptStructureDirty = false;
        job.dirty = false;
    }

    private static void clearStoredJob(ServerPlayerEntity player) {
        if (player instanceof BuildingPlanJobHolder holder) {
            holder.mythicrpg$setBuildingPlanJobData(new NbtCompound());
        }
    }

    private static boolean forceSavePlayer(ServerPlayerEntity player) {
        if (player == null || player.getServer() == null) {
            return false;
        }
        try {
            ((PlayerManagerBuildingPlanInvoker) player.getServer().getPlayerManager())
                    .mythicrpg$saveBuildingPlanPlayerData(player);
            return true;
        } catch (RuntimeException exception) {
            // Vanilla will still attempt the same save during autosave/shutdown.
            System.err.println("[MythicRPG] Could not checkpoint Building plan job for "
                    + player.getGameProfile().getName() + ": " + exception.getMessage());
            return false;
        }
    }

    private static void clearPreview(ServerPlayerEntity player) {
        PREVIEWS.remove(player.getUuid());
        ServerPlayNetworking.send(player, BuildingPlan2DPreviewPayload.clear());
    }

    private static void sendValidationFailure(ServerPlayerEntity player, Validation validation) {
        Text message = switch (validation.failure()) {
            case MISSING_MATERIAL -> Text.translatable(
                    "message.mythicrpg.building_plan_2d.missing_material",
                    new ItemStack(validation.missingItem()).getName(),
                    validation.requiredCount(),
                    validation.availableCount()
            );
            case UNLOADED -> Text.translatable("message.mythicrpg.building_plan_2d.unloaded");
            case PROTECTED -> Text.translatable("message.mythicrpg.building_plan_2d.protected");
            case BLOCKED -> Text.translatable("message.mythicrpg.building_plan_2d.blocked");
            case UNSUPPORTED -> Text.translatable("message.mythicrpg.building_plan_2d.unsupported_destination");
            case NONE -> Text.translatable("message.mythicrpg.building_plan_2d.preview_invalid");
        };
        player.sendMessage(message.copy().formatted(Formatting.RED), true);
        BuildingSoundFeedback.error(player);
    }

    private static Direction.Axis[] tangentAxes(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[]{Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private static int coordinate(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private static BlockPos offset(Direction.Axis axis, int amount) {
        return switch (axis) {
            case X -> new BlockPos(amount, 0, 0);
            case Y -> new BlockPos(0, amount, 0);
            case Z -> new BlockPos(0, 0, amount);
        };
    }

    private static String dimensionId(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }

    public record CaptureResult(
            boolean success,
            BuildingPlan2DData.Plan plan,
            String messageKey,
            Object[] messageArgs
    ) {
        static CaptureResult success(BuildingPlan2DData.Plan plan) {
            return new CaptureResult(true, plan, "", new Object[0]);
        }

        static CaptureResult failure(String key, Object... args) {
            return new CaptureResult(false, null, key, args);
        }
    }

    private record PreviewSession(UUID planId, String dimensionId, BlockPos anchor, long expiresAtTick) {
    }

    static record Placement(
            BlockPos pos,
            BlockState state,
            Item item,
            int distanceFromAnchor,
            BlankBlockAppearance appearance
    ) {
        Placement {
            appearance = appearance == null ? BlankBlockAppearance.EMPTY : appearance;
        }

        boolean isBlank() {
            return state.isOf(ModBlocks.BLANK_BLOCK);
        }
    }

    enum Failure {
        NONE,
        MISSING_MATERIAL,
        UNLOADED,
        PROTECTED,
        BLOCKED,
        UNSUPPORTED
    }

    static record Validation(
            boolean valid,
            Failure failure,
            Item missingItem,
            int requiredCount,
            int availableCount,
            List<Placement> toPlace,
            Map<Item, Integer> required
    ) {
        static Validation success(List<Placement> toPlace, Map<Item, Integer> required) {
            return new Validation(true, Failure.NONE, Items.AIR, 0, 0, toPlace, required);
        }

        static Validation failure(Failure failure) {
            return new Validation(false, failure, Items.AIR, 0, 0, List.of(), Map.of());
        }

        static Validation missing(
                Item item,
                int required,
                int available,
                List<Placement> toPlace,
                Map<Item, Integer> requiredItems
        ) {
            return new Validation(
                    false,
                    Failure.MISSING_MATERIAL,
                    item,
                    required,
                    available,
                    toPlace,
                    requiredItems
            );
        }
    }

    enum JobKind {
        PLAN_2D(
                "message.mythicrpg.building_plan_2d.completed",
                "message.mythicrpg.building_plan_2d.cancelled"
        ),
        PLAN_3D(
                "message.mythicrpg.building_plan_3d.completed",
                "message.mythicrpg.building_plan_3d.cancelled"
        );

        private final String completedMessageKey;
        private final String cancelledMessageKey;

        JobKind(String completedMessageKey, String cancelledMessageKey) {
            this.completedMessageKey = completedMessageKey;
            this.cancelledMessageKey = cancelledMessageKey;
        }

        String completedMessageKey() {
            return completedMessageKey;
        }

        String cancelledMessageKey() {
            return cancelledMessageKey;
        }
    }

    private enum TerminalOutcome {
        NONE,
        COMPLETED,
        CANCELLED
    }

    private enum RecoveryStatus {
        READY,
        PAUSED,
        INVALID
    }

    private static final class BuildJob {
        private final UUID jobId;
        private final JobKind kind;
        private final String dimensionId;
        private List<Placement> placements;
        private Map<Item, Integer> initialReserved;
        private Map<Item, Integer> reserved;
        private int cursor;
        private final int totalPlacements;
        private int lastSavedCursor;
        private long lastSavedTick;
        private boolean dirty;
        private boolean paused;
        private final boolean creative;
        private boolean recoveryPending;
        private boolean receiptStructureDirty;
        private TerminalOutcome terminalOutcome = TerminalOutcome.NONE;
        private boolean terminalNotify;

        private BuildJob(
                UUID jobId,
                JobKind kind,
                String dimensionId,
                List<Placement> placements,
                Map<Item, Integer> initialReserved,
                Map<Item, Integer> reserved,
                int cursor,
                int totalPlacements,
                boolean creative,
                boolean recoveryPending,
                boolean receiptStructureDirty
        ) {
            this.jobId = jobId;
            this.kind = kind;
            this.dimensionId = dimensionId;
            this.placements = placements;
            this.initialReserved = initialReserved;
            this.reserved = reserved;
            this.cursor = cursor;
            this.totalPlacements = totalPlacements;
            this.lastSavedCursor = cursor;
            this.creative = creative;
            this.recoveryPending = recoveryPending;
            this.receiptStructureDirty = receiptStructureDirty;
        }
    }


}
