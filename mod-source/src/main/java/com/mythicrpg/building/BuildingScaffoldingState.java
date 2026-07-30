package com.mythicrpg.building;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistent data for scaffolding placed with the extended-support perk.
 *
 * <p>Format 3 separates two concerns:</p>
 * <ul>
 *     <li>a compact, bounded eligibility index used to preserve exact perk
 *     behavior when a structure changes between vanilla and extended support;</li>
 *     <li>an active distance index containing only positions whose real support
 *     distance is currently 7..32.</li>
 * </ul>
 *
 * <p>Both layers are grouped by dimension and chunk. The former dynamic NBT
 * string keys are migrated automatically.</p>
 */
public final class BuildingScaffoldingState extends PersistentState {
    public static final int MIN_EXTENDED_DISTANCE = 7;
    public static final int MAX_EXTENDED_DISTANCE = 32;

    /** Hard safety limits. Updates to existing entries are never rejected. */
    public static final int MAX_TOTAL_ENTRIES = 524_288;
    public static final int MAX_ENTRIES_PER_DIMENSION = 262_144;
    public static final int MAX_ENTRIES_PER_CHUNK = 32_768;

    private static final int FORMAT_VERSION = 3;
    private static final int MAX_DIMENSIONS_ON_LOAD = 128;
    private static final int MAX_CHUNK_TAGS_PER_DIMENSION_ON_LOAD = 262_144;
    private static final int MAX_DIMENSION_ID_LENGTH = 256;
    private static final String STATE_ID = "mythicrpg_building_extended_scaffolding";
    private static final Type<BuildingScaffoldingState> TYPE = new Type<>(
            BuildingScaffoldingState::new,
            BuildingScaffoldingState::fromNbt,
            null
    );

    private final Map<String, DimensionIndex> dimensions = new HashMap<>();
    private int totalEligibleEntries;
    private int totalExtendedEntries;

    public static BuildingScaffoldingState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public boolean isEligible(World world, BlockPos pos) {
        DimensionIndex index = dimensions.get(dimensionId(world));
        if (index == null) {
            return false;
        }
        Set<Long> positions = index.eligibleByChunk.get(chunkKey(pos));
        return positions != null && positions.contains(pos.asLong());
    }

    /** Returns the real 7..32 distance, or -1 when the position is not active. */
    public int getDistance(World world, BlockPos pos) {
        DimensionIndex index = dimensions.get(dimensionId(world));
        if (index == null) {
            return -1;
        }
        Byte distance = index.distanceByPosition.get(pos.asLong());
        return distance == null ? -1 : Byte.toUnsignedInt(distance);
    }

    /**
     * Marks a perk-placed position as eligible. Returns false only when a new
     * entry would exceed a safety limit.
     */
    public boolean addEligible(World world, BlockPos pos) {
        String dimension = dimensionId(world);
        long packedPos = pos.asLong();
        long chunkKey = chunkKey(pos);

        DimensionIndex existingIndex = dimensions.get(dimension);
        if (existingIndex != null) {
            Set<Long> existingChunk = existingIndex.eligibleByChunk.get(chunkKey);
            if (existingChunk != null && existingChunk.contains(packedPos)) {
                return true;
            }
        }

        int dimensionSize = existingIndex == null ? 0 : existingIndex.eligibleCount;
        int chunkSize = 0;
        if (existingIndex != null) {
            Set<Long> existingChunk = existingIndex.eligibleByChunk.get(chunkKey);
            chunkSize = existingChunk == null ? 0 : existingChunk.size();
        }
        if (totalEligibleEntries >= MAX_TOTAL_ENTRIES
                || dimensionSize >= MAX_ENTRIES_PER_DIMENSION
                || chunkSize >= MAX_ENTRIES_PER_CHUNK) {
            return false;
        }

        DimensionIndex index = dimensions.computeIfAbsent(dimension, ignored -> new DimensionIndex());
        Set<Long> positions = index.eligibleByChunk.computeIfAbsent(
                chunkKey,
                ignored -> new HashSet<>()
        );
        positions.add(packedPos);
        index.eligibleCount++;
        totalEligibleEntries++;
        markDirty();
        return true;
    }

    /** Adds or updates the active real distance of an eligible position. */
    public boolean putDistance(World world, BlockPos pos, int distance) {
        if (distance < MIN_EXTENDED_DISTANCE || distance > MAX_EXTENDED_DISTANCE) {
            throw new IllegalArgumentException("Extended scaffolding distance must be 7..32");
        }
        if (!isEligible(world, pos)) {
            return false;
        }

        DimensionIndex index = dimensions.get(dimensionId(world));
        long packedPos = pos.asLong();
        byte encoded = (byte) distance;
        Byte previous = index.distanceByPosition.put(packedPos, encoded);
        if (previous == null) {
            totalExtendedEntries++;
            markDirty();
        } else if (previous != encoded) {
            markDirty();
        }
        return true;
    }

    /** Removes only the active 7..32 distance while retaining perk eligibility. */
    public boolean removeExtended(World world, BlockPos pos) {
        DimensionIndex index = dimensions.get(dimensionId(world));
        if (index == null || index.distanceByPosition.remove(pos.asLong()) == null) {
            return false;
        }
        totalExtendedEntries--;
        markDirty();
        return true;
    }

    /** Removes both eligibility and active distance for a block that no longer exists. */
    public boolean removeAll(World world, BlockPos pos) {
        String dimension = dimensionId(world);
        DimensionIndex index = dimensions.get(dimension);
        if (index == null) {
            return false;
        }

        long packedPos = pos.asLong();
        long chunkKey = chunkKey(pos);
        Set<Long> positions = index.eligibleByChunk.get(chunkKey);
        if (positions == null || !positions.remove(packedPos)) {
            return false;
        }

        if (index.distanceByPosition.remove(packedPos) != null) {
            totalExtendedEntries--;
        }
        if (positions.isEmpty()) {
            index.eligibleByChunk.remove(chunkKey);
        }
        index.eligibleCount--;
        totalEligibleEntries--;
        if (index.eligibleCount == 0) {
            dimensions.remove(dimension);
        }
        markDirty();
        return true;
    }

    /** Snapshot safe for validation while entries are being removed. */
    public long[] positionsInChunk(World world, int chunkX, int chunkZ) {
        DimensionIndex index = dimensions.get(dimensionId(world));
        if (index == null) {
            return new long[0];
        }
        Set<Long> positions = index.eligibleByChunk.get(chunkKey(chunkX, chunkZ));
        if (positions == null || positions.isEmpty()) {
            return new long[0];
        }

        long[] snapshot = new long[positions.size()];
        int cursor = 0;
        for (long packedPos : positions) {
            snapshot[cursor++] = packedPos;
        }
        return snapshot;
    }

    /** Removes data belonging to dimensions that are no longer present on this server. */
    public int retainDimensions(Set<String> validDimensions) {
        int removed = 0;
        var iterator = dimensions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DimensionIndex> entry = iterator.next();
            if (validDimensions.contains(entry.getKey())) {
                continue;
            }
            DimensionIndex index = entry.getValue();
            removed += index.eligibleCount;
            totalEligibleEntries -= index.eligibleCount;
            totalExtendedEntries -= index.distanceByPosition.size();
            iterator.remove();
        }
        if (removed > 0) {
            markDirty();
        }
        return removed;
    }

    public int eligibleSize() {
        return totalEligibleEntries;
    }

    public int extendedSize() {
        return totalExtendedEntries;
    }

    private static String dimensionId(World world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static long chunkKey(BlockPos pos) {
        return chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("Format", FORMAT_VERSION);
        NbtList dimensionList = new NbtList();

        for (Map.Entry<String, DimensionIndex> dimensionEntry : dimensions.entrySet()) {
            DimensionIndex index = dimensionEntry.getValue();
            if (index.eligibleCount == 0) {
                continue;
            }

            NbtCompound dimensionTag = new NbtCompound();
            dimensionTag.putString("Id", dimensionEntry.getKey());
            NbtList chunks = new NbtList();

            for (Map.Entry<Long, Set<Long>> chunkEntry : index.eligibleByChunk.entrySet()) {
                Set<Long> eligible = chunkEntry.getValue();
                if (eligible.isEmpty()) {
                    continue;
                }

                long[] eligiblePositions = new long[eligible.size()];
                long[] extendedPositions = new long[eligible.size()];
                byte[] extendedDistances = new byte[eligible.size()];
                int eligibleCursor = 0;
                int extendedCursor = 0;
                for (long packedPos : eligible) {
                    eligiblePositions[eligibleCursor++] = packedPos;
                    Byte distance = index.distanceByPosition.get(packedPos);
                    if (distance != null) {
                        extendedPositions[extendedCursor] = packedPos;
                        extendedDistances[extendedCursor] = distance;
                        extendedCursor++;
                    }
                }

                if (extendedCursor != extendedPositions.length) {
                    long[] compactPositions = new long[extendedCursor];
                    byte[] compactDistances = new byte[extendedCursor];
                    System.arraycopy(extendedPositions, 0, compactPositions, 0, extendedCursor);
                    System.arraycopy(extendedDistances, 0, compactDistances, 0, extendedCursor);
                    extendedPositions = compactPositions;
                    extendedDistances = compactDistances;
                }

                NbtCompound chunkTag = new NbtCompound();
                chunkTag.putLong("Chunk", chunkEntry.getKey());
                chunkTag.putLongArray("Eligible", eligiblePositions);
                if (extendedCursor > 0) {
                    chunkTag.putLongArray("ExtendedPositions", extendedPositions);
                    chunkTag.putByteArray("ExtendedDistances", extendedDistances);
                }
                chunks.add(chunkTag);
            }

            if (!chunks.isEmpty()) {
                dimensionTag.put("Chunks", chunks);
                dimensionList.add(dimensionTag);
            }
        }

        nbt.put("Dimensions", dimensionList);
        return nbt;
    }

    private static BuildingScaffoldingState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        BuildingScaffoldingState state = new BuildingScaffoldingState();
        int format = nbt.getInt("Format");
        boolean needsRewrite = false;

        if (format >= FORMAT_VERSION && nbt.contains("Dimensions", NbtElement.LIST_TYPE)) {
            needsRewrite |= !loadFormat3(state, nbt.getList("Dimensions", NbtElement.COMPOUND_TYPE));
        } else if (format == 2 && nbt.contains("Dimensions", NbtElement.LIST_TYPE)) {
            // Compatibility with the short-lived audit development format.
            loadFormat2(state, nbt.getList("Dimensions", NbtElement.COMPOUND_TYPE));
            needsRewrite = true;
        } else {
            loadLegacyFormat(state, nbt.getCompound("positions"));
            needsRewrite = true;
        }

        if (needsRewrite) {
            state.markDirty();
        }
        return state;
    }

    private static boolean loadFormat3(BuildingScaffoldingState state, NbtList dimensions) {
        boolean clean = true;
        int dimensionCount = Math.min(dimensions.size(), MAX_DIMENSIONS_ON_LOAD);
        if (dimensions.size() > dimensionCount) {
            clean = false;
        }

        for (int dimensionIndex = 0; dimensionIndex < dimensionCount; dimensionIndex++) {
            NbtCompound dimensionTag = dimensions.getCompound(dimensionIndex);
            String dimension = dimensionTag.getString("Id");
            if (!validDimensionId(dimension)) {
                clean = false;
                continue;
            }

            NbtList chunks = dimensionTag.getList("Chunks", NbtElement.COMPOUND_TYPE);
            int chunkCount = Math.min(chunks.size(), MAX_CHUNK_TAGS_PER_DIMENSION_ON_LOAD);
            if (chunks.size() > chunkCount) {
                clean = false;
            }
            for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
                NbtCompound chunkTag = chunks.getCompound(chunkIndex);
                long[] eligible = chunkTag.getLongArray("Eligible");
                for (long packedPos : eligible) {
                    if (!state.putLoadedEligible(dimension, packedPos)) {
                        clean = false;
                    }
                }

                long[] extendedPositions = chunkTag.getLongArray("ExtendedPositions");
                byte[] extendedDistances = chunkTag.getByteArray("ExtendedDistances");
                int extendedCount = Math.min(extendedPositions.length, extendedDistances.length);
                if (extendedPositions.length != extendedDistances.length) {
                    clean = false;
                }
                for (int index = 0; index < extendedCount; index++) {
                    int distance = Byte.toUnsignedInt(extendedDistances[index]);
                    if (!state.putLoadedDistance(dimension, extendedPositions[index], distance)) {
                        clean = false;
                    }
                }
            }
        }
        return clean;
    }

    private static void loadFormat2(BuildingScaffoldingState state, NbtList dimensions) {
        int dimensionCount = Math.min(dimensions.size(), MAX_DIMENSIONS_ON_LOAD);
        for (int dimensionIndex = 0; dimensionIndex < dimensionCount; dimensionIndex++) {
            NbtCompound dimensionTag = dimensions.getCompound(dimensionIndex);
            String dimension = dimensionTag.getString("Id");
            if (!validDimensionId(dimension)) {
                continue;
            }

            NbtList chunks = dimensionTag.getList("Chunks", NbtElement.COMPOUND_TYPE);
            int chunkCount = Math.min(chunks.size(), MAX_CHUNK_TAGS_PER_DIMENSION_ON_LOAD);
            for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
                NbtCompound chunkTag = chunks.getCompound(chunkIndex);
                long[] positions = chunkTag.getLongArray("Positions");
                byte[] distances = chunkTag.getByteArray("Distances");
                int count = Math.min(positions.length, distances.length);
                for (int index = 0; index < count; index++) {
                    state.putLoadedEligible(dimension, positions[index]);
                    state.putLoadedDistance(
                            dimension,
                            positions[index],
                            Byte.toUnsignedInt(distances[index])
                    );
                }
            }
        }
    }

    private static void loadLegacyFormat(BuildingScaffoldingState state, NbtCompound legacyPositions) {
        for (String legacyKey : new ArrayList<>(legacyPositions.getKeys())) {
            int separator = legacyKey.lastIndexOf('|');
            if (separator <= 0 || separator >= legacyKey.length() - 1) {
                continue;
            }
            String dimension = legacyKey.substring(0, separator);
            if (!validDimensionId(dimension)) {
                continue;
            }
            try {
                long packedPos = Long.parseLong(legacyKey.substring(separator + 1));
                if (state.putLoadedEligible(dimension, packedPos)) {
                    // The old format did not persist a real distance. Seven is a
                    // temporary seed; chunk reconciliation converges it locally.
                    state.putLoadedDistance(dimension, packedPos, MIN_EXTENDED_DISTANCE);
                }
            } catch (NumberFormatException ignored) {
                // Corrupt legacy entries are intentionally discarded.
            }
        }
    }

    private boolean putLoadedEligible(String dimension, long packedPos) {
        if (!validDimensionId(dimension)) {
            return false;
        }

        DimensionIndex existingIndex = dimensions.get(dimension);
        long chunkKey = chunkKey(BlockPos.fromLong(packedPos));
        if (existingIndex != null) {
            Set<Long> existingChunk = existingIndex.eligibleByChunk.get(chunkKey);
            if (existingChunk != null && existingChunk.contains(packedPos)) {
                return true;
            }
        }

        int dimensionSize = existingIndex == null ? 0 : existingIndex.eligibleCount;
        int chunkSize = 0;
        if (existingIndex != null) {
            Set<Long> existingChunk = existingIndex.eligibleByChunk.get(chunkKey);
            chunkSize = existingChunk == null ? 0 : existingChunk.size();
        }
        if (totalEligibleEntries >= MAX_TOTAL_ENTRIES
                || dimensionSize >= MAX_ENTRIES_PER_DIMENSION
                || chunkSize >= MAX_ENTRIES_PER_CHUNK) {
            return false;
        }

        DimensionIndex index = dimensions.computeIfAbsent(dimension, ignored -> new DimensionIndex());
        index.eligibleByChunk.computeIfAbsent(chunkKey, ignored -> new HashSet<>()).add(packedPos);
        index.eligibleCount++;
        totalEligibleEntries++;
        return true;
    }

    private boolean putLoadedDistance(String dimension, long packedPos, int distance) {
        if (distance < MIN_EXTENDED_DISTANCE || distance > MAX_EXTENDED_DISTANCE) {
            return false;
        }
        if (!putLoadedEligible(dimension, packedPos)) {
            return false;
        }

        DimensionIndex index = dimensions.get(dimension);
        Byte previous = index.distanceByPosition.put(packedPos, (byte) distance);
        if (previous == null) {
            totalExtendedEntries++;
        }
        return true;
    }

    private static boolean validDimensionId(String dimension) {
        return !dimension.isBlank() && dimension.length() <= MAX_DIMENSION_ID_LENGTH;
    }

    private static final class DimensionIndex {
        private final Map<Long, Byte> distanceByPosition = new HashMap<>();
        private final Map<Long, Set<Long>> eligibleByChunk = new HashMap<>();
        private int eligibleCount;
    }
}
