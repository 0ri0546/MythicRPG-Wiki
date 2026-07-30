package com.mythicrpg.mining.archaeology;

import com.mythicrpg.core.ModBlocks;
import com.mythicrpg.mining.archaeology.relic.ArchaeologyRelicRewards;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CandleBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Plans, validates and atomically places major archaeology sites. */
public final class GrandFossilSiteGenerator {

    public static final int HALF_WIDTH = 5;
    public static final int HALF_HEIGHT = 3;
    public static final int MIN_DISTANCE = 128;
    public static final int NORMAL_MAX_DISTANCE = 500;
    public static final int SECONDARY_MAX_DISTANCE = 750;
    public static final int ABSOLUTE_MAX_DISTANCE = 1000;
    public static final int MIN_CENTER_Y = -48;
    public static final int MAX_CENTER_Y = 24;
    public static final int MIN_SITE_SEPARATION = 96;

    private static final int ATTEMPTS_PER_RANGE = 32;
    private static final int MAX_UNLOADED_VALIDATIONS_PER_RANGE = 12;

    /**
     * Notify tracking clients and create BlockEntities, but avoid applying
     * hundreds of intermediate neighbour/physics updates during the transaction.
     */
    private static final int PLACEMENT_FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;

    private GrandFossilSiteGenerator() {
    }

    /**
     * Creates a progressive search. One call to {@link SearchJob#step()} validates
     * at most one complete 11x7x11 candidate, so gameplay code can budget the
     * operation across server ticks.
     */
    public static SearchJob createSearchJob(
            ServerWorld world,
            BlockPos archaeologistPos,
            FossilSpecimenData.Specimen specimen,
            UUID owner,
            UUID archaeologist,
            GrandFossilSiteState state
    ) {
        return new SearchJob(
                world,
                archaeologistPos,
                specimen,
                owner,
                archaeologist,
                state
        );
    }

    /**
     * Synchronous compatibility entry point used only by operator/test code.
     * The normal Archaeologist flow uses SearchJob through the expedition manager.
     */
    public static Optional<GeneratedGrandSite> findAndGenerate(
            ServerWorld world,
            BlockPos archaeologistPos,
            FossilSpecimenData.Specimen specimen,
            UUID owner,
            UUID archaeologist,
            GrandFossilSiteState state
    ) {
        SearchJob job = createSearchJob(
                world,
                archaeologistPos,
                specimen,
                owner,
                archaeologist,
                state
        );
        while (true) {
            SearchStep step = job.step();
            if (step.status() == SearchStatus.FOUND) {
                return Optional.of(step.generated());
            }
            if (step.status() == SearchStatus.EXHAUSTED) {
                return Optional.empty();
            }
        }
    }

    public static Optional<GeneratedGrandSite> generateAtForTesting(
            ServerWorld world,
            BlockPos center,
            FossilFamily family,
            FossilRarity rarity,
            UUID owner,
            UUID archaeologist,
            UUID specimenId,
            UUID reconstructedBy
    ) {
        Optional<LinkedHashMap<BlockPos, BlockState>> originalStates =
                validateAndCapture(world, center);
        if (originalStates.isEmpty()) {
            return Optional.empty();
        }
        return generateAt(
                world,
                center,
                new FossilSpecimenData.Specimen(
                        family,
                        rarity,
                        specimenId,
                        reconstructedBy,
                        world.getTimeOfDay() / 24000L,
                        false
                ),
                owner,
                archaeologist,
                originalStates.get(),
                world.getRandom()
        );
    }

    private static List<BlockPos> buildCandidates(
            ServerWorld world,
            BlockPos origin,
            GrandFossilSiteState state,
            Random random
    ) {
        int[] maximumRanges = {
                NORMAL_MAX_DISTANCE,
                SECONDARY_MAX_DISTANCE,
                ABSOLUTE_MAX_DISTANCE
        };
        ArrayList<BlockPos> ordered = new ArrayList<>();
        HashSet<Long> unique = new HashSet<>();

        for (int maximumRange : maximumRanges) {
            ArrayList<BlockPos> loaded = new ArrayList<>();
            ArrayList<BlockPos> unloaded = new ArrayList<>();
            for (int attempt = 0; attempt < ATTEMPTS_PER_RANGE; attempt++) {
                BlockPos candidate = randomCandidate(origin, maximumRange, random);
                if (!unique.add(candidate.asLong())
                        || state.isAreaNearExistingSite(candidate, MIN_SITE_SEPARATION)) {
                    continue;
                }
                if (isWholeVolumeLoaded(world, candidate)) {
                    loaded.add(candidate);
                } else if (unloaded.size() < MAX_UNLOADED_VALIDATIONS_PER_RANGE) {
                    unloaded.add(candidate);
                }
            }

            // Prefer already loaded terrain. Distant chunk acquisition remains possible,
            // but is spread over later ticks and preserves the original 12-per-range cap.
            ordered.addAll(loaded);
            ordered.addAll(unloaded);
        }
        return List.copyOf(ordered);
    }

    private static boolean isWholeVolumeLoaded(ServerWorld world, BlockPos center) {
        for (ChunkPos chunkPos : requiredChunks(center)) {
            if (!world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                return false;
            }
        }
        return true;
    }

    private static List<ChunkPos> requiredChunks(BlockPos center) {
        int minChunkX = (center.getX() - HALF_WIDTH) >> 4;
        int maxChunkX = (center.getX() + HALF_WIDTH) >> 4;
        int minChunkZ = (center.getZ() - HALF_WIDTH) >> 4;
        int maxChunkZ = (center.getZ() + HALF_WIDTH) >> 4;
        ArrayList<ChunkPos> chunks = new ArrayList<>(4);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }
        return List.copyOf(chunks);
    }

    private static BlockPos randomCandidate(BlockPos origin, int maximumRange, Random random) {
        int minimumSquared = MIN_DISTANCE * MIN_DISTANCE;
        int maximumSquared = maximumRange * maximumRange;
        int dx;
        int dz;
        do {
            dx = random.nextBetween(-maximumRange, maximumRange);
            dz = random.nextBetween(-maximumRange, maximumRange);
        } while (dx * dx + dz * dz < minimumSquared || dx * dx + dz * dz > maximumSquared);

        int y = random.nextBetween(MIN_CENTER_Y, MAX_CENTER_Y);
        return new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
    }

    /** Compatibility query used by operator/debug commands. */
    public static boolean isSafeVolume(ServerWorld world, BlockPos center) {
        return validateAndCapture(world, center).isPresent();
    }

    /** Single-pass validation and transactional snapshot. */
    private static Optional<LinkedHashMap<BlockPos, BlockState>> validateAndCapture(
            ServerWorld world,
            BlockPos center
    ) {
        if (center.getY() - HALF_HEIGHT <= world.getBottomY() + 4
                || center.getY() + HALF_HEIGHT >= world.getTopY() - 5) {
            return Optional.empty();
        }

        LinkedHashMap<BlockPos, BlockState> states = new LinkedHashMap<>(11 * 7 * 11);
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
            for (int y = -HALF_HEIGHT; y <= HALF_HEIGHT; y++) {
                for (int z = -HALF_WIDTH; z <= HALF_WIDTH; z++) {
                    BlockPos pos = center.add(x, y, z).toImmutable();
                    BlockState blockState = world.getBlockState(pos);
                    if (!isNaturalMatrix(blockState) || !blockState.getFluidState().isEmpty()) {
                        return Optional.empty();
                    }
                    // Every accepted matrix block is a fixed vanilla non-BE block, so a
                    // separate world.getBlockEntity lookup is redundant.
                    states.put(pos, blockState);
                }
            }
        }
        return Optional.of(states);
    }

    private static boolean isNaturalMatrix(BlockState state) {
        return state.isOf(Blocks.STONE)
                || state.isOf(Blocks.DEEPSLATE)
                || state.isOf(Blocks.TUFF)
                || state.isOf(Blocks.ANDESITE)
                || state.isOf(Blocks.DIORITE)
                || state.isOf(Blocks.GRANITE);
    }

    private static Optional<GeneratedGrandSite> generateAt(
            ServerWorld world,
            BlockPos center,
            FossilSpecimenData.Specimen specimen,
            UUID owner,
            UUID archaeologist,
            LinkedHashMap<BlockPos, BlockState> originalStates,
            Random random
    ) {
        UUID siteId = new UUID(random.nextLong(), random.nextLong());
        FossilFamily siteFamily = rollSiteFamily(specimen.family(), random);
        FossilRarity dominantRarity = specimen.rarity();
        int fossilCount = 3 * dominantRarity.rank();
        int resourceCount = 2 * dominantRarity.rank();
        boolean specialRoll = random.nextInt(100) < 20 * dominantRarity.rank();

        ArrayList<BlockPos> fossilCandidates = new ArrayList<>();
        ArrayList<BlockPos> resourceCandidates = new ArrayList<>();

        try {
            placeRoomShellAndInterior(
                    world,
                    center,
                    random,
                    fossilCandidates,
                    resourceCandidates
            );
            shuffle(fossilCandidates, random);
            shuffle(resourceCandidates, random);

            if (fossilCandidates.size() < fossilCount
                    || resourceCandidates.size() < resourceCount) {
                rollback(world, originalStates);
                return Optional.empty();
            }

            Set<BlockPos> occupied = new HashSet<>();
            ArrayList<BlockPos> fossilPositions = selectPositions(
                    fossilCandidates,
                    fossilCount,
                    occupied
            );
            ArrayList<BlockPos> resourcePositions = selectPositions(
                    resourceCandidates,
                    resourceCount,
                    occupied
            );
            if (fossilPositions.size() != fossilCount
                    || resourcePositions.size() != resourceCount) {
                rollback(world, originalStates);
                return Optional.empty();
            }

            ArrayList<FossilRarity> actualRarities = new ArrayList<>(fossilCount);
            for (BlockPos fossilPos : fossilPositions) {
                FossilRarity actual = random.nextInt(4) == 0
                        ? FossilRarity.rollGeneration(random)
                        : dominantRarity;
                actualRarities.add(actual);
                world.setBlockState(
                        fossilPos,
                        ModBlocks.FOSSIL_BLOCK.getDefaultState(),
                        PLACEMENT_FLAGS
                );
            }

            for (int index = 0; index < fossilPositions.size(); index++) {
                BlockPos fossilPos = fossilPositions.get(index);
                BlockEntity blockEntity = world.getBlockEntity(fossilPos);
                if (!(blockEntity instanceof FossilBlockEntity fossil)) {
                    rollback(world, originalStates);
                    return Optional.empty();
                }
                fossil.configureGrandSite(
                        siteId,
                        center,
                        siteFamily,
                        actualRarities.get(index),
                        dominantRarity,
                        fossilCount
                );
            }

            for (BlockPos resourcePos : resourcePositions) {
                world.setBlockState(resourcePos, rollResourceBlockState(random), PLACEMENT_FLAGS);
            }

            BlockPos barrelPos = center.add(0, -HALF_HEIGHT + 1, 0);
            world.setBlockState(barrelPos, Blocks.BARREL.getDefaultState(), PLACEMENT_FLAGS);
            if (!(world.getBlockEntity(barrelPos) instanceof BarrelBlockEntity barrel)) {
                rollback(world, originalStates);
                return Optional.empty();
            }
            fillBarrel(barrel, random, siteFamily, specialRoll);
            barrel.markDirty();

            GrandFossilSiteState.GrandSiteRecord record =
                    new GrandFossilSiteState.GrandSiteRecord(
                            siteId,
                            specimen.specimenId(),
                            owner,
                            specimen.reconstructedBy(),
                            archaeologist,
                            center,
                            specimen.family(),
                            specimen.rarity(),
                            siteFamily,
                            dominantRarity,
                            biomeId(world.getBiome(center)),
                            fossilCount,
                            fossilCount,
                            resourceCount,
                            specialRoll,
                            barrelPos,
                            true,
                            GrandSiteStatus.GENERATED
                    );
            return Optional.of(new GeneratedGrandSite(record, Map.copyOf(originalStates)));
        } catch (RuntimeException exception) {
            rollback(world, originalStates);
            throw exception;
        }
    }

    private static ArrayList<BlockPos> selectPositions(
            List<BlockPos> candidates,
            int count,
            Set<BlockPos> occupied
    ) {
        ArrayList<BlockPos> selected = new ArrayList<>(count);
        for (BlockPos candidate : candidates) {
            if (selected.size() >= count) {
                break;
            }
            if (occupied.add(candidate)) {
                selected.add(candidate);
            }
        }
        return selected;
    }

    private static void placeRoomShellAndInterior(
            ServerWorld world,
            BlockPos center,
            Random random,
            List<BlockPos> fossilCandidates,
            List<BlockPos> resourceCandidates
    ) {
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) {
            for (int y = -HALF_HEIGHT; y <= HALF_HEIGHT; y++) {
                for (int z = -HALF_WIDTH; z <= HALF_WIDTH; z++) {
                    BlockPos pos = center.add(x, y, z).toImmutable();
                    boolean boundary = Math.abs(x) == HALF_WIDTH
                            || Math.abs(z) == HALF_WIDTH
                            || Math.abs(y) == HALF_HEIGHT;
                    if (!boundary) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), PLACEMENT_FLAGS);
                        continue;
                    }

                    world.setBlockState(pos, shellState(pos.getY(), random), PLACEMENT_FLAGS);

                    boolean usefulWall = Math.abs(y) < HALF_HEIGHT
                            && (Math.abs(x) == HALF_WIDTH || Math.abs(z) == HALF_WIDTH);
                    boolean ceilingOrFloor = Math.abs(y) == HALF_HEIGHT
                            && Math.abs(x) <= HALF_WIDTH - 1
                            && Math.abs(z) <= HALF_WIDTH - 1;
                    if (usefulWall || ceilingOrFloor) {
                        fossilCandidates.add(pos);
                        resourceCandidates.add(pos);
                    }
                }
            }
        }

        BlockPos[] pillars = {
                center.add(-3, -HALF_HEIGHT + 1, -3),
                center.add(3, -HALF_HEIGHT + 1, -3),
                center.add(-3, -HALF_HEIGHT + 1, 3),
                center.add(3, -HALF_HEIGHT + 1, 3)
        };
        for (BlockPos pillarBase : pillars) {
            for (int y = 0; y < 4; y++) {
                world.setBlockState(
                        pillarBase.up(y),
                        Blocks.BONE_BLOCK.getDefaultState(),
                        PLACEMENT_FLAGS
                );
            }
        }

        BlockPos[] decoration = {
                center.add(-2, -HALF_HEIGHT + 1, 0),
                center.add(2, -HALF_HEIGHT + 1, 0),
                center.add(0, -HALF_HEIGHT + 1, -2),
                center.add(0, -HALF_HEIGHT + 1, 2)
        };
        for (int index = 0; index < decoration.length; index++) {
            BlockState decorationState = index % 2 == 0
                    ? Blocks.COBWEB.getDefaultState()
                    : Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT, true);
            world.setBlockState(decoration[index], decorationState, PLACEMENT_FLAGS);
        }
    }

    private static BlockState shellState(int y, Random random) {
        int roll = random.nextInt(100);
        if (y < 0) {
            if (roll < 55) return Blocks.COBBLED_DEEPSLATE.getDefaultState();
            if (roll < 80) return Blocks.TUFF.getDefaultState();
            if (roll < 92) return Blocks.DEEPSLATE_BRICKS.getDefaultState();
            return Blocks.GRAVEL.getDefaultState();
        }
        if (roll < 55) return Blocks.COBBLESTONE.getDefaultState();
        if (roll < 80) return Blocks.TUFF.getDefaultState();
        if (roll < 92) return Blocks.MOSSY_COBBLESTONE.getDefaultState();
        return Blocks.GRAVEL.getDefaultState();
    }

    private static BlockState rollResourceBlockState(Random random) {
        int roll = random.nextInt(100);
        if (roll < 18) return Blocks.COAL_BLOCK.getDefaultState();
        if (roll < 38) return Blocks.COPPER_BLOCK.getDefaultState();
        if (roll < 63) return Blocks.IRON_BLOCK.getDefaultState();
        if (roll < 75) return Blocks.REDSTONE_BLOCK.getDefaultState();
        if (roll < 83) return Blocks.LAPIS_BLOCK.getDefaultState();
        if (roll < 92) return Blocks.GOLD_BLOCK.getDefaultState();
        if (roll < 95) return Blocks.EMERALD_BLOCK.getDefaultState();
        return Blocks.DIAMOND_BLOCK.getDefaultState();
    }

    private static FossilFamily rollSiteFamily(FossilFamily specimenFamily, Random random) {
        if (random.nextInt(4) != 0) {
            return specimenFamily;
        }
        FossilFamily selected;
        do {
            selected = FossilFamily.random(random);
        } while (selected == specimenFamily);
        return selected;
    }

    private static void fillBarrel(
            BarrelBlockEntity barrel,
            Random random,
            FossilFamily siteFamily,
            boolean specialRoll
    ) {
        ArrayList<ItemStack> loot = new ArrayList<>();
        if (specialRoll) {
            loot.add(ArchaeologyRelicRewards.create(siteFamily, random));
        }
        loot.add(new ItemStack(Items.GLOW_BERRIES, random.nextBetween(2, 8)));
        loot.add(new ItemStack(Items.KELP, random.nextBetween(3, 12)));
        loot.add(new ItemStack(Items.BONE, random.nextBetween(2, 8)));
        loot.add(new ItemStack(Items.CLAY_BALL, random.nextBetween(2, 10)));

        ItemStack[] optional = {
                new ItemStack(Items.BONE_MEAL, random.nextBetween(2, 8)),
                new ItemStack(Items.BRICK, random.nextBetween(1, 5)),
                new ItemStack(Items.FLINT, random.nextBetween(1, 6)),
                new ItemStack(Items.COAL, random.nextBetween(1, 6)),
                new ItemStack(Items.STRING, random.nextBetween(2, 8)),
                new ItemStack(Items.CANDLE, random.nextBetween(1, 4)),
                new ItemStack(Items.GLASS_BOTTLE, random.nextBetween(1, 3))
        };
        int optionalCount = random.nextBetween(1, 3);
        for (int index = 0; index < optionalCount; index++) {
            loot.add(optional[random.nextInt(optional.length)].copy());
        }
        if (random.nextInt(4) == 0) {
            ItemStack brush = new ItemStack(Items.BRUSH);
            brush.setDamage(random.nextBetween(8, Math.max(8, brush.getMaxDamage() - 1)));
            loot.add(brush);
        }

        ArrayList<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < barrel.size(); slot++) {
            slots.add(slot);
        }
        shuffle(slots, random);
        for (int index = 0; index < loot.size() && index < slots.size(); index++) {
            barrel.setStack(slots.get(index), loot.get(index));
        }
    }

    private static String biomeId(RegistryEntry<Biome> biome) {
        return biome.getKey()
                .map(key -> key.getValue().toString())
                .orElse("minecraft:unknown");
    }

    private static <T> void shuffle(List<T> values, Random random) {
        for (int index = values.size() - 1; index > 0; index--) {
            int target = random.nextInt(index + 1);
            T value = values.get(index);
            values.set(index, values.get(target));
            values.set(target, value);
        }
    }

    private static void rollback(ServerWorld world, Map<BlockPos, BlockState> originalStates) {
        for (Map.Entry<BlockPos, BlockState> entry : originalStates.entrySet()) {
            world.setBlockState(entry.getKey(), entry.getValue(), PLACEMENT_FLAGS);
        }
    }

    public enum SearchStatus {
        SEARCHING,
        FOUND,
        EXHAUSTED
    }

    public record SearchStep(SearchStatus status, GeneratedGrandSite generated) {
        private static SearchStep searching() {
            return new SearchStep(SearchStatus.SEARCHING, null);
        }

        private static SearchStep found(GeneratedGrandSite generated) {
            return new SearchStep(SearchStatus.FOUND, generated);
        }

        private static SearchStep exhausted() {
            return new SearchStep(SearchStatus.EXHAUSTED, null);
        }
    }

    public static final class SearchJob {
        private final ServerWorld world;
        private final FossilSpecimenData.Specimen specimen;
        private final UUID owner;
        private final UUID archaeologist;
        private final GrandFossilSiteState state;
        private final Random random;
        private final List<BlockPos> candidates;
        private int cursor;
        private BlockPos currentCandidate;
        private List<ChunkPos> currentChunks = List.of();
        private int currentChunkCursor;

        private SearchJob(
                ServerWorld world,
                BlockPos origin,
                FossilSpecimenData.Specimen specimen,
                UUID owner,
                UUID archaeologist,
                GrandFossilSiteState state
        ) {
            this.world = world;
            this.specimen = specimen;
            this.owner = owner;
            this.archaeologist = archaeologist;
            this.state = state;
            this.random = Random.create(world.getRandom().nextLong());
            this.candidates = buildCandidates(world, origin, state, random);
        }

        public SearchStep step() {
            while (true) {
                if (currentCandidate == null) {
                    if (cursor >= candidates.size()) {
                        return SearchStep.exhausted();
                    }
                    currentCandidate = candidates.get(cursor++);
                    currentChunks = requiredChunks(currentCandidate);
                    currentChunkCursor = 0;
                    if (state.isAreaNearExistingSite(currentCandidate, MIN_SITE_SEPARATION)) {
                        clearCurrentCandidate();
                        continue;
                    }
                }

                // Acquire at most one missing chunk per server tick. A single
                // distant candidate can cross four chunks; loading all of them in
                // one interaction tick would reintroduce the original spike.
                while (currentChunkCursor < currentChunks.size()) {
                    ChunkPos chunkPos = currentChunks.get(currentChunkCursor++);
                    if (world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                        continue;
                    }
                    world.getChunk(chunkPos.x, chunkPos.z);
                    return SearchStep.searching();
                }

                BlockPos candidate = currentCandidate;
                if (state.isAreaNearExistingSite(candidate, MIN_SITE_SEPARATION)) {
                    clearCurrentCandidate();
                    continue;
                }

                // A chunk loaded during an earlier work unit may have been released
                // before the candidate reaches its validation turn. Reacquire one
                // missing chunk per later work unit instead of silently forcing it
                // from the full-volume scan below.
                if (!isWholeVolumeLoaded(world, candidate)) {
                    currentChunkCursor = 0;
                    return SearchStep.searching();
                }

                Optional<LinkedHashMap<BlockPos, BlockState>> originalStates =
                        validateAndCapture(world, candidate);
                clearCurrentCandidate();
                if (originalStates.isEmpty()) {
                    return cursor >= candidates.size()
                            ? SearchStep.exhausted()
                            : SearchStep.searching();
                }

                Optional<GeneratedGrandSite> generated = generateAt(
                        world,
                        candidate,
                        specimen,
                        owner,
                        archaeologist,
                        originalStates.get(),
                        random
                );
                if (generated.isPresent()) {
                    return SearchStep.found(generated.get());
                }
                return cursor >= candidates.size()
                        ? SearchStep.exhausted()
                        : SearchStep.searching();
            }
        }

        private void clearCurrentCandidate() {
            currentCandidate = null;
            currentChunks = List.of();
            currentChunkCursor = 0;
        }

        public int remainingCandidates() {
            return Math.max(0, candidates.size() - cursor + (currentCandidate == null ? 0 : 1));
        }
    }

    public record GeneratedGrandSite(
            GrandFossilSiteState.GrandSiteRecord record,
            Map<BlockPos, BlockState> originalStates
    ) {
        public GeneratedGrandSite {
            originalStates = Map.copyOf(originalStates);
        }

        public void rollback(ServerWorld world) {
            GrandFossilSiteGenerator.rollback(world, originalStates);
        }
    }
}
