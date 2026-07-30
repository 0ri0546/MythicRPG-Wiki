package com.mythicrpg.traveling;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TravelingProgressState extends PersistentState {

    private static final String STATE_ID = "mythicrpg_traveling_progress";
    private static final int RECENT_BIOME_LIMIT = 3;

    private static final Type<TravelingProgressState> TYPE = new Type<>(
            TravelingProgressState::new,
            TravelingProgressState::fromNbt,
            null
    );

    private final Map<UUID, PlayerTravelData> dataByPlayer = new HashMap<>();
    private final Map<String, Set<Long>> verifiedTreasureChests = new HashMap<>();

    public static TravelingProgressState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);

        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }

        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public boolean markStructureDiscovered(
            UUID playerUuid,
            Identifier dimensionId,
            Identifier structureId,
            int startChunkX,
            int startChunkZ
    ) {
        PlayerTravelData data = getData(playerUuid);
        Set<Long> starts = data.discoveredStructures
                .computeIfAbsent(dimensionId.toString(), ignored -> new HashMap<>())
                .computeIfAbsent(structureId.toString(), ignored -> new HashSet<>());
        boolean added = starts.add(packCoordinates(startChunkX, startChunkZ));
        if (added) {
            markDirty();
        }
        return added;
    }

    public boolean markDimensionVisited(UUID playerUuid, Identifier dimensionId) {
        boolean added = getData(playerUuid).visitedDimensions.add(dimensionId.toString());
        if (added) {
            markDirty();
        }
        return added;
    }

    public boolean markMovementCellRewarded(
            UUID playerUuid,
            Identifier dimensionId,
            int cellX,
            int cellZ
    ) {
        PlayerTravelData data = getData(playerUuid);
        Set<Long> cells = data.rewardedMovementCells.computeIfAbsent(
                dimensionId.toString(),
                ignored -> new HashSet<>()
        );
        boolean added = cells.add(packCoordinates(cellX, cellZ));
        if (added) {
            markDirty();
        }
        return added;
    }

    public boolean markTreasureChestOpened(
            UUID playerUuid,
            Identifier dimensionId,
            BlockPos chestPos
    ) {
        PlayerTravelData data = getData(playerUuid);
        boolean added = addPackedPosition(
                data.openedTreasureChests,
                dimensionId.toString(),
                chestPos.asLong()
        );
        if (added) {
            markDirty();
        }
        return added;
    }

    public void setActiveDeathRecall(
            UUID playerUuid,
            UUID recallId,
            long expiresAtMillis
    ) {
        PlayerTravelData data = getData(playerUuid);
        String value = recallId.toString();

        if (!value.equals(data.activeDeathRecallId)
                || data.activeDeathRecallExpiresAtMillis != expiresAtMillis) {
            data.activeDeathRecallId = value;
            data.activeDeathRecallExpiresAtMillis = expiresAtMillis;
            markDirty();
        }
    }

    public boolean isActiveDeathRecall(UUID playerUuid, UUID recallId) {
        PlayerTravelData data = getValidActiveDeathRecallData(playerUuid);
        return data != null && recallId.toString().equals(data.activeDeathRecallId);
    }

    public boolean hasActiveDeathRecall(UUID playerUuid) {
        return getValidActiveDeathRecallData(playerUuid) != null;
    }

    public void clearActiveDeathRecall(UUID playerUuid) {
        PlayerTravelData data = dataByPlayer.get(playerUuid);
        if (data != null && (!data.activeDeathRecallId.isEmpty()
                || data.activeDeathRecallExpiresAtMillis > 0L)) {
            data.activeDeathRecallId = "";
            data.activeDeathRecallExpiresAtMillis = 0L;
            markDirty();
        }
    }

    private PlayerTravelData getValidActiveDeathRecallData(UUID playerUuid) {
        PlayerTravelData data = dataByPlayer.get(playerUuid);
        if (data == null || data.activeDeathRecallId.isEmpty()) {
            return null;
        }

        if (data.activeDeathRecallExpiresAtMillis <= System.currentTimeMillis()) {
            data.activeDeathRecallId = "";
            data.activeDeathRecallExpiresAtMillis = 0L;
            markDirty();
            return null;
        }

        return data;
    }

    public void setDeathRecallCooldownUntil(UUID playerUuid, long cooldownUntilMillis) {
        PlayerTravelData data = getData(playerUuid);

        if (data.deathRecallCooldownUntilMillis != cooldownUntilMillis) {
            data.deathRecallCooldownUntilMillis = cooldownUntilMillis;
            markDirty();
        }
    }

    public long getDeathRecallCooldownRemainingMillis(UUID playerUuid) {
        PlayerTravelData data = dataByPlayer.get(playerUuid);

        if (data == null || data.deathRecallCooldownUntilMillis <= 0L) {
            return 0L;
        }

        long remaining = data.deathRecallCooldownUntilMillis - System.currentTimeMillis();

        if (remaining <= 0L) {
            data.deathRecallCooldownUntilMillis = 0L;
            markDirty();
            return 0L;
        }

        return remaining;
    }

    public boolean markVerifiedTreasureChest(Identifier dimensionId, BlockPos chestPos) {
        boolean added = addPackedPosition(
                verifiedTreasureChests,
                dimensionId.toString(),
                chestPos.asLong()
        );
        if (added) {
            markDirty();
        }
        return added;
    }

    public boolean isVerifiedTreasureChest(Identifier dimensionId, BlockPos chestPos) {
        Set<Long> positions = verifiedTreasureChests.get(dimensionId.toString());
        return positions != null && positions.contains(chestPos.asLong());
    }

    /**
     * Records a biome only when it is not already present in the three-biome history.
     * Returns true when the biome is new to that history and the list was updated.
     */
    public boolean recordRecentBiome(UUID playerUuid, Identifier biomeId) {
        PlayerTravelData data = getData(playerUuid);
        String value = biomeId.toString();

        if (data.recentBiomes.contains(value)) {
            return false;
        }

        data.recentBiomes.add(value);
        while (data.recentBiomes.size() > RECENT_BIOME_LIMIT) {
            data.recentBiomes.remove(0);
        }

        markDirty();
        return true;
    }

    public int getStructureCount(UUID playerUuid) {
        PlayerTravelData data = dataByPlayer.get(playerUuid);
        if (data == null) {
            return 0;
        }
        int total = 0;
        for (Map<String, Set<Long>> structures : data.discoveredStructures.values()) {
            total += countPackedPositions(structures);
        }
        return total;
    }

    public int getDimensionCount(UUID playerUuid) {
        PlayerTravelData data = dataByPlayer.get(playerUuid);
        return data == null ? 0 : data.visitedDimensions.size();
    }

    public int getMovementCellCount(UUID playerUuid) {
        PlayerTravelData data = dataByPlayer.get(playerUuid);
        if (data == null) {
            return 0;
        }
        int total = 0;
        for (Set<Long> cells : data.rewardedMovementCells.values()) {
            total += cells.size();
        }
        return total;
    }

    public int getTreasureChestCount(UUID playerUuid) {
        PlayerTravelData data = dataByPlayer.get(playerUuid);
        return data == null ? 0 : countPackedPositions(data.openedTreasureChests);
    }

    public void clearPlayer(UUID playerUuid) {
        if (dataByPlayer.remove(playerUuid) != null) {
            markDirty();
        }
    }

    private PlayerTravelData getData(UUID playerUuid) {
        return dataByPlayer.computeIfAbsent(playerUuid, ignored -> new PlayerTravelData());
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList players = new NbtList();

        for (Map.Entry<UUID, PlayerTravelData> entry : dataByPlayer.entrySet()) {
            NbtCompound playerTag = new NbtCompound();
            playerTag.putString("uuid", entry.getKey().toString());

            PlayerTravelData data = entry.getValue();
            playerTag.put("structures_v2", writeStructures(data.discoveredStructures));
            playerTag.put("dimensions", writeStringSet(data.visitedDimensions));
            playerTag.put("movement_cells_v2", writeMovementCells(data.rewardedMovementCells));
            playerTag.put("treasure_chests_v2", writePackedPositions(data.openedTreasureChests));
            playerTag.put("recent_biomes", writeStringList(data.recentBiomes));
            if (!data.activeDeathRecallId.isEmpty()) {
                playerTag.putString("active_death_recall_id", data.activeDeathRecallId);
                playerTag.putLong(
                        "active_death_recall_expires_at",
                        data.activeDeathRecallExpiresAtMillis
                );
            }

            if (data.deathRecallCooldownUntilMillis > 0L) {
                playerTag.putLong(
                        "death_recall_cooldown_until",
                        data.deathRecallCooldownUntilMillis
                );
            }

            players.add(playerTag);
        }

        nbt.put("players", players);
        nbt.put("verified_treasure_chests_v2", writePackedPositions(verifiedTreasureChests));
        return nbt;
    }

    private static TravelingProgressState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        TravelingProgressState state = new TravelingProgressState();
        NbtList players = nbt.getList("players", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < players.size(); i++) {
            NbtCompound playerTag = players.getCompound(i);

            try {
                UUID playerUuid = UUID.fromString(playerTag.getString("uuid"));
                PlayerTravelData data = new PlayerTravelData();

                if (playerTag.contains("structures_v2", NbtElement.LIST_TYPE)) {
                    readStructures(playerTag, data.discoveredStructures);
                } else {
                    readLegacyStructures(playerTag, data.discoveredStructures);
                    if (playerTag.contains("structures", NbtElement.LIST_TYPE)) {
                        state.markDirty();
                    }
                }
                readStringSet(playerTag, "dimensions", data.visitedDimensions);
                if (playerTag.contains("movement_cells_v2", NbtElement.LIST_TYPE)) {
                    readMovementCells(playerTag, data.rewardedMovementCells);
                } else {
                    readLegacyMovementCells(playerTag, data.rewardedMovementCells);
                    if (playerTag.contains("movement_cells", NbtElement.LIST_TYPE)) {
                        state.markDirty();
                    }
                }
                if (playerTag.contains("treasure_chests_v2", NbtElement.LIST_TYPE)) {
                    readPackedPositions(playerTag, "treasure_chests_v2", data.openedTreasureChests);
                } else {
                    readLegacyBlockPositions(playerTag, "treasure_chests", data.openedTreasureChests);
                    if (playerTag.contains("treasure_chests", NbtElement.LIST_TYPE)) {
                        state.markDirty();
                    }
                }
                readStringList(playerTag, "recent_biomes", data.recentBiomes, RECENT_BIOME_LIMIT);
                data.activeDeathRecallId = playerTag.getString("active_death_recall_id");
                data.activeDeathRecallExpiresAtMillis =
                        playerTag.getLong("active_death_recall_expires_at");

                data.deathRecallCooldownUntilMillis =
                        playerTag.getLong("death_recall_cooldown_until");

                state.dataByPlayer.put(playerUuid, data);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed player data instead of preventing world load.
            }
        }

        if (nbt.contains("verified_treasure_chests_v2", NbtElement.LIST_TYPE)) {
            readPackedPositions(
                    nbt,
                    "verified_treasure_chests_v2",
                    state.verifiedTreasureChests
            );
        } else {
            readLegacyBlockPositions(
                    nbt,
                    "verified_treasure_chests",
                    state.verifiedTreasureChests
            );
            if (nbt.contains("verified_treasure_chests", NbtElement.LIST_TYPE)) {
                state.markDirty();
            }
        }
        return state;
    }

    private static NbtList writeStructures(
            Map<String, Map<String, Set<Long>>> structuresByDimension
    ) {
        NbtList dimensions = new NbtList();
        for (Map.Entry<String, Map<String, Set<Long>>> dimensionEntry
                : structuresByDimension.entrySet()) {
            NbtList structureTypes = new NbtList();
            for (Map.Entry<String, Set<Long>> structureEntry
                    : dimensionEntry.getValue().entrySet()) {
                if (structureEntry.getValue().isEmpty()) {
                    continue;
                }
                NbtCompound structureTag = new NbtCompound();
                structureTag.putString("structure", structureEntry.getKey());
                structureTag.putLongArray("starts", toLongArray(structureEntry.getValue()));
                structureTypes.add(structureTag);
            }
            if (structureTypes.isEmpty()) {
                continue;
            }
            NbtCompound dimensionTag = new NbtCompound();
            dimensionTag.putString("dimension", dimensionEntry.getKey());
            dimensionTag.put("structures", structureTypes);
            dimensions.add(dimensionTag);
        }
        return dimensions;
    }

    private static void readStructures(
            NbtCompound source,
            Map<String, Map<String, Set<Long>>> destination
    ) {
        NbtList dimensions = source.getList("structures_v2", NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < dimensions.size(); index++) {
            NbtCompound dimensionTag = dimensions.getCompound(index);
            String dimension = dimensionTag.getString("dimension");
            if (dimension.isBlank() || dimension.length() > 256
                    || Identifier.tryParse(dimension) == null
                    || !dimensionTag.contains("structures", NbtElement.LIST_TYPE)) {
                continue;
            }

            Map<String, Set<Long>> structures = destination.computeIfAbsent(
                    dimension,
                    ignored -> new HashMap<>()
            );
            NbtList structureTypes = dimensionTag.getList(
                    "structures",
                    NbtElement.COMPOUND_TYPE
            );
            for (int structureIndex = 0; structureIndex < structureTypes.size(); structureIndex++) {
                NbtCompound structureTag = structureTypes.getCompound(structureIndex);
                String structure = structureTag.getString("structure");
                if (structure.isBlank() || structure.length() > 256
                        || Identifier.tryParse(structure) == null) {
                    continue;
                }
                Set<Long> starts = structures.computeIfAbsent(
                        structure,
                        ignored -> new HashSet<>()
                );
                for (long packedStart : structureTag.getLongArray("starts")) {
                    starts.add(packedStart);
                }
            }
        }
    }

    private static void readLegacyStructures(
            NbtCompound source,
            Map<String, Map<String, Set<Long>>> destination
    ) {
        NbtList legacy = source.getList("structures", NbtElement.STRING_TYPE);
        for (int index = 0; index < legacy.size(); index++) {
            String[] parts = legacy.getString(index).split("\\|", -1);
            if (parts.length != 4
                    || Identifier.tryParse(parts[0]) == null
                    || Identifier.tryParse(parts[1]) == null) {
                continue;
            }
            try {
                destination
                        .computeIfAbsent(parts[0], ignored -> new HashMap<>())
                        .computeIfAbsent(parts[1], ignored -> new HashSet<>())
                        .add(packCoordinates(
                                Integer.parseInt(parts[2]),
                                Integer.parseInt(parts[3])
                        ));
            } catch (NumberFormatException ignored) {
                // Skip malformed legacy entries without blocking world load.
            }
        }
    }

    private static boolean addPackedPosition(
            Map<String, Set<Long>> positionsByDimension,
            String dimension,
            long packedPosition
    ) {
        return positionsByDimension
                .computeIfAbsent(dimension, ignored -> new HashSet<>())
                .add(packedPosition);
    }

    private static int countPackedPositions(Map<String, Set<Long>> positionsByDimension) {
        int total = 0;
        for (Set<Long> positions : positionsByDimension.values()) {
            total += positions.size();
        }
        return total;
    }

    private static NbtList writePackedPositions(Map<String, Set<Long>> positionsByDimension) {
        NbtList dimensions = new NbtList();
        for (Map.Entry<String, Set<Long>> entry : positionsByDimension.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            NbtCompound dimensionTag = new NbtCompound();
            dimensionTag.putString("dimension", entry.getKey());
            dimensionTag.putLongArray("positions", toLongArray(entry.getValue()));
            dimensions.add(dimensionTag);
        }
        return dimensions;
    }

    private static void readPackedPositions(
            NbtCompound source,
            String key,
            Map<String, Set<Long>> destination
    ) {
        NbtList dimensions = source.getList(key, NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < dimensions.size(); index++) {
            NbtCompound dimensionTag = dimensions.getCompound(index);
            String dimension = dimensionTag.getString("dimension");
            if (dimension.isBlank() || dimension.length() > 256
                    || Identifier.tryParse(dimension) == null) {
                continue;
            }
            Set<Long> positions = destination.computeIfAbsent(
                    dimension,
                    ignored -> new HashSet<>()
            );
            for (long position : dimensionTag.getLongArray("positions")) {
                positions.add(position);
            }
        }
    }

    private static void readLegacyBlockPositions(
            NbtCompound source,
            String key,
            Map<String, Set<Long>> destination
    ) {
        NbtList legacy = source.getList(key, NbtElement.STRING_TYPE);
        for (int index = 0; index < legacy.size(); index++) {
            String value = legacy.getString(index);
            String[] parts = value.split("\\|", -1);
            if (parts.length != 4 || Identifier.tryParse(parts[0]) == null) {
                continue;
            }
            try {
                BlockPos pos = new BlockPos(
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3])
                );
                addPackedPosition(destination, parts[0], pos.asLong());
            } catch (NumberFormatException ignored) {
                // Skip malformed legacy entries without blocking world load.
            }
        }
    }

    private static NbtList writeMovementCells(Map<String, Set<Long>> cellsByDimension) {
        NbtList dimensions = new NbtList();
        for (Map.Entry<String, Set<Long>> entry : cellsByDimension.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            NbtCompound dimensionTag = new NbtCompound();
            dimensionTag.putString("dimension", entry.getKey());
            dimensionTag.putLongArray("cells", toLongArray(entry.getValue()));
            dimensions.add(dimensionTag);
        }
        return dimensions;
    }

    private static void readMovementCells(
            NbtCompound source,
            Map<String, Set<Long>> destination
    ) {
        NbtList dimensions = source.getList("movement_cells_v2", NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < dimensions.size(); index++) {
            NbtCompound dimensionTag = dimensions.getCompound(index);
            String dimension = dimensionTag.getString("dimension");
            if (dimension.isBlank() || dimension.length() > 256
                    || Identifier.tryParse(dimension) == null) {
                continue;
            }
            Set<Long> cells = destination.computeIfAbsent(dimension, ignored -> new HashSet<>());
            for (long cell : dimensionTag.getLongArray("cells")) {
                cells.add(cell);
            }
        }
    }

    private static void readLegacyMovementCells(
            NbtCompound source,
            Map<String, Set<Long>> destination
    ) {
        NbtList legacy = source.getList("movement_cells", NbtElement.STRING_TYPE);
        for (int index = 0; index < legacy.size(); index++) {
            String value = legacy.getString(index);
            int lastSeparator = value.lastIndexOf('|');
            int secondLastSeparator = lastSeparator <= 0
                    ? -1
                    : value.lastIndexOf('|', lastSeparator - 1);
            if (secondLastSeparator <= 0 || lastSeparator <= secondLastSeparator + 1) {
                continue;
            }
            String dimension = value.substring(0, secondLastSeparator);
            if (Identifier.tryParse(dimension) == null) {
                continue;
            }
            try {
                int cellX = Integer.parseInt(value.substring(secondLastSeparator + 1, lastSeparator));
                int cellZ = Integer.parseInt(value.substring(lastSeparator + 1));
                destination.computeIfAbsent(dimension, ignored -> new HashSet<>())
                        .add(packCoordinates(cellX, cellZ));
            } catch (NumberFormatException ignored) {
                // Skip malformed legacy entries without blocking world load.
            }
        }
    }

    private static long[] toLongArray(Set<Long> values) {
        long[] packed = new long[values.size()];
        int index = 0;
        for (long value : values) {
            packed[index++] = value;
        }
        Arrays.sort(packed);
        return packed;
    }

    private static long packCoordinates(int x, int z) {
        return ((long) x & 0xffffffffL) << 32 | ((long) z & 0xffffffffL);
    }

    private static NbtList writeStringSet(Set<String> values) {
        NbtList list = new NbtList();

        for (String value : values) {
            list.add(NbtString.of(value));
        }

        return list;
    }

    private static NbtList writeStringList(List<String> values) {
        NbtList list = new NbtList();

        for (String value : values) {
            list.add(NbtString.of(value));
        }

        return list;
    }

    private static void readStringList(
            NbtCompound source,
            String key,
            List<String> destination,
            int limit
    ) {
        NbtList list = source.getList(key, NbtElement.STRING_TYPE);
        int start = Math.max(0, list.size() - limit);

        for (int i = start; i < list.size(); i++) {
            destination.add(list.getString(i));
        }
    }

    private static void readStringSet(NbtCompound source, String key, Set<String> destination) {
        NbtList list = source.getList(key, NbtElement.STRING_TYPE);

        for (int i = 0; i < list.size(); i++) {
            destination.add(list.getString(i));
        }
    }

    private static final class PlayerTravelData {
        private final Map<String, Map<String, Set<Long>>> discoveredStructures = new HashMap<>();
        private final Set<String> visitedDimensions = new HashSet<>();
        private final Map<String, Set<Long>> rewardedMovementCells = new HashMap<>();
        private final Map<String, Set<Long>> openedTreasureChests = new HashMap<>();
        private final List<String> recentBiomes = new ArrayList<>();
        private String activeDeathRecallId = "";
        private long activeDeathRecallExpiresAtMillis;
        private long deathRecallCooldownUntilMillis;
    }
}
