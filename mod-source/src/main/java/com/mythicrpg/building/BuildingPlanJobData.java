package com.mythicrpg.building;

import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Strict, bounded NBT codec for player-attached Building plan jobs. */
final class BuildingPlanJobData {
    static final int FORMAT_VERSION = 1;
    static final int MAX_PLACEMENTS = 512;
    static final int MAX_ESCROW_ITEMS = MAX_PLACEMENTS * 7;
    static final int MAX_ESCROW_TYPES = MAX_ESCROW_ITEMS;

    private BuildingPlanJobData() {
    }

    /**
     * Durable terminal marker written in the same player save as the refunded
     * inventory. If the following cleanup save is interrupted, this marker
     * prevents the old job receipt from being replayed on the next login.
     */
    static NbtCompound writeSettled(UUID jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("Missing settled Building job id");
        }
        NbtCompound root = new NbtCompound();
        root.putInt("Version", FORMAT_VERSION);
        root.putUuid("JobId", jobId);
        root.putBoolean("Settled", true);
        return root;
    }

    static boolean isSettled(NbtCompound root) {
        return root != null
                && root.getInt("Version") == FORMAT_VERSION
                && root.containsUuid("JobId")
                && root.getBoolean("Settled");
    }

    static NbtCompound write(
            UUID jobId,
            BuildingPlan2DManager.JobKind kind,
            String dimensionId,
            List<BuildingPlan2DManager.Placement> placements,
            Map<Item, Integer> initialEscrow,
            Map<Item, Integer> remainingEscrow,
            int cursor,
            int totalPlacements,
            boolean creative
    ) {
        validateForWrite(
                jobId,
                kind,
                dimensionId,
                placements,
                initialEscrow,
                remainingEscrow,
                cursor,
                totalPlacements,
                creative
        );

        NbtCompound root = new NbtCompound();
        root.putInt("Version", FORMAT_VERSION);
        root.putUuid("JobId", jobId);
        root.putString("Kind", kind.name());
        root.putString("Dimension", dimensionId);
        root.putInt("Cursor", Math.max(0, Math.min(cursor, placements.size())));
        root.putInt("TotalPlacements", Math.max(placements.size(), totalPlacements));
        root.putBoolean("Creative", creative);

        NbtList placementList = new NbtList();
        for (BuildingPlan2DManager.Placement placement : placements) {
            NbtCompound tag = new NbtCompound();
            tag.putLong("Position", placement.pos().asLong());
            tag.put("State", BuildingPlan2DData.writeState(placement.state()));
            tag.putString("Item", Registries.ITEM.getId(placement.item()).toString());
            tag.putInt("Distance", placement.distanceFromAnchor());
            if (!placement.appearance().isEmpty()) {
                tag.put("Blank", BlankBlockItemData.writeAppearance(placement.appearance()));
            }
            placementList.add(tag);
        }
        root.put("Placements", placementList);
        root.put("InitialEscrow", writeEscrow(initialEscrow));
        root.put("Escrow", writeEscrow(remainingEscrow));
        return root;
    }

    /**
     * Builds the mutable patch written after ordinary placement progress. The
     * large immutable placement list remains attached to the player unchanged.
     */
    static NbtCompound writeProgress(
            Map<Item, Integer> remainingEscrow,
            int cursor,
            int placementCount
    ) {
        if (cursor < 0 || cursor > placementCount
                || placementCount <= 0 || placementCount > MAX_PLACEMENTS) {
            throw new IllegalArgumentException("Invalid Building plan progress");
        }
        validateEscrow(remainingEscrow);
        NbtCompound progress = new NbtCompound();
        progress.putInt("Cursor", cursor);
        progress.put("Escrow", writeEscrow(remainingEscrow));
        return progress;
    }

    private static void validateForWrite(
            UUID jobId,
            BuildingPlan2DManager.JobKind kind,
            String dimensionId,
            List<BuildingPlan2DManager.Placement> placements,
            Map<Item, Integer> initialEscrow,
            Map<Item, Integer> remainingEscrow,
            int cursor,
            int totalPlacements,
            boolean creative
    ) {
        if (jobId == null || kind == null || dimensionId == null
                || Identifier.tryParse(dimensionId) == null
                || placements == null || placements.isEmpty()
                || placements.size() > MAX_PLACEMENTS
                || cursor < 0 || cursor > placements.size()
                || totalPlacements < placements.size() || totalPlacements > MAX_PLACEMENTS) {
            throw new IllegalArgumentException("Invalid persistent Building plan job");
        }

        HashSet<Long> positions = new HashSet<>();
        for (BuildingPlan2DManager.Placement placement : placements) {
            if (placement == null || placement.pos() == null || placement.state() == null
                    || placement.item() == null || placement.item() == Items.AIR
                    || placement.state().getBlock().asItem() != placement.item()
                    || placement.distanceFromAnchor() < 0
                    || !BlankBlockMaterialRegistry.isValid(placement.appearance())
                    || (!placement.isBlank() && !placement.appearance().isEmpty())
                    || !positions.add(placement.pos().asLong())) {
                throw new IllegalArgumentException("Invalid Building plan placement");
            }
        }

        validateEscrow(initialEscrow);
        validateEscrow(remainingEscrow);
        for (Map.Entry<Item, Integer> entry : remainingEscrow.entrySet()) {
            if (initialEscrow.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                throw new IllegalArgumentException("Building plan escrow exceeds initial reserve");
            }
        }
        if (creative) {
            if (!initialEscrow.isEmpty() || !remainingEscrow.isEmpty()) {
                throw new IllegalArgumentException("Creative Building job cannot contain escrow");
            }
        } else if (!escrowCoversPlacements(initialEscrow, placements)) {
            throw new IllegalArgumentException("Initial Building plan escrow is incomplete");
        }
    }

    private static void validateEscrow(Map<Item, Integer> escrow) {
        if (escrow == null || escrow.size() > MAX_ESCROW_TYPES) {
            throw new IllegalArgumentException("Building plan escrow exceeds persistent limits");
        }
        int total = 0;
        for (Map.Entry<Item, Integer> entry : escrow.entrySet()) {
            Integer count = entry.getValue();
            if (entry.getKey() == null || entry.getKey() == Items.AIR
                    || count == null || count <= 0 || count > MAX_ESCROW_ITEMS - total) {
                throw new IllegalArgumentException("Invalid Building plan escrow count");
            }
            total += count;
        }
    }

    private static NbtList writeEscrow(Map<Item, Integer> escrow) {
        NbtList list = new NbtList();
        for (Map.Entry<Item, Integer> entry : escrow.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            NbtCompound tag = new NbtCompound();
            tag.putString("Item", Registries.ITEM.getId(entry.getKey()).toString());
            tag.putInt("Count", entry.getValue());
            list.add(tag);
        }
        return list;
    }

    static Optional<DecodedJob> read(NbtCompound root) {
        if (root == null || root.isEmpty()
                || root.getInt("Version") != FORMAT_VERSION
                || !root.containsUuid("JobId")
                || !root.contains("Placements", NbtElement.LIST_TYPE)
                || !root.contains("Escrow", NbtElement.LIST_TYPE)) {
            return Optional.empty();
        }

        String dimensionId = root.getString("Dimension");
        if (dimensionId.isBlank() || dimensionId.length() > 256
                || Identifier.tryParse(dimensionId) == null) {
            return Optional.empty();
        }

        final BuildingPlan2DManager.JobKind kind;
        try {
            kind = BuildingPlan2DManager.JobKind.valueOf(root.getString("Kind"));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }

        NbtList placementList = root.getList("Placements", NbtElement.COMPOUND_TYPE);
        if (placementList.isEmpty() || placementList.size() > MAX_PLACEMENTS) {
            return Optional.empty();
        }

        ArrayList<BuildingPlan2DManager.Placement> placements = new ArrayList<>(placementList.size());
        HashSet<Long> positions = new HashSet<>();
        for (int index = 0; index < placementList.size(); index++) {
            NbtCompound tag = placementList.getCompound(index);
            if (!tag.contains("State", NbtElement.COMPOUND_TYPE)) {
                return Optional.empty();
            }
            Optional<BlockState> parsedState = BuildingPlan2DData.readState(tag.getCompound("State"));
            Identifier itemId = Identifier.tryParse(tag.getString("Item"));
            if (parsedState.isEmpty() || itemId == null || !Registries.ITEM.containsId(itemId)) {
                return Optional.empty();
            }
            Item item = Registries.ITEM.get(itemId);
            if (item == Items.AIR || parsedState.get().getBlock().asItem() != item) {
                return Optional.empty();
            }

            BlankBlockAppearance appearance = BlankBlockAppearance.EMPTY;
            if (tag.contains("Blank", NbtElement.COMPOUND_TYPE)) {
                Optional<BlankBlockAppearance> parsed = BlankBlockItemData.readAppearance(tag.getCompound("Blank"));
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                appearance = parsed.get();
            }
            if (!BlankBlockMaterialRegistry.isValid(appearance)
                    || (!parsedState.get().isOf(com.mythicrpg.core.ModBlocks.BLANK_BLOCK)
                    && !appearance.isEmpty())) {
                return Optional.empty();
            }

            BlockPos position = BlockPos.fromLong(tag.getLong("Position"));
            int distance = tag.getInt("Distance");
            if (distance < 0 || !positions.add(position.asLong())) {
                return Optional.empty();
            }
            placements.add(new BuildingPlan2DManager.Placement(
                    position,
                    parsedState.get(),
                    item,
                    distance,
                    appearance
            ));
        }

        Optional<Map<Item, Integer>> remainingEscrow = readEscrowStrict(
                root.getList("Escrow", NbtElement.COMPOUND_TYPE)
        );
        if (remainingEscrow.isEmpty()) {
            return Optional.empty();
        }
        Map<Item, Integer> initialEscrow;
        if (root.contains("InitialEscrow", NbtElement.LIST_TYPE)) {
            Optional<Map<Item, Integer>> parsedInitial = readEscrowStrict(
                    root.getList("InitialEscrow", NbtElement.COMPOUND_TYPE)
            );
            if (parsedInitial.isEmpty()) {
                return Optional.empty();
            }
            initialEscrow = parsedInitial.get();
        } else {
            initialEscrow = copyIdentity(remainingEscrow.get());
        }
        for (Map.Entry<Item, Integer> entry : remainingEscrow.get().entrySet()) {
            if (initialEscrow.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return Optional.empty();
            }
        }
        boolean creative = root.getBoolean("Creative");
        if (creative && (!initialEscrow.isEmpty() || !remainingEscrow.get().isEmpty())) {
            return Optional.empty();
        }
        if (!creative && !escrowCoversPlacements(initialEscrow, placements)) {
            return Optional.empty();
        }

        int cursor = root.getInt("Cursor");
        int totalPlacements = root.contains("TotalPlacements", NbtElement.INT_TYPE)
                ? root.getInt("TotalPlacements")
                : placements.size();
        if (cursor < 0 || cursor > placements.size()
                || totalPlacements < placements.size()
                || totalPlacements > MAX_PLACEMENTS) {
            return Optional.empty();
        }
        return Optional.of(new DecodedJob(
                root.getUuid("JobId"),
                kind,
                dimensionId,
                List.copyOf(placements),
                initialEscrow,
                remainingEscrow.get(),
                cursor,
                totalPlacements,
                creative
        ));
    }

    private static boolean escrowCoversPlacements(
            Map<Item, Integer> escrow,
            List<BuildingPlan2DManager.Placement> placements
    ) {
        IdentityHashMap<Item, Integer> required = new IdentityHashMap<>();
        for (BuildingPlan2DManager.Placement placement : placements) {
            if (!addPlacementMaterials(required, placement)) {
                return false;
            }
        }
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            if (escrow.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static boolean addPlacementMaterials(
            Map<Item, Integer> required,
            BuildingPlan2DManager.Placement placement
    ) {
        if (placement == null || placement.item() == null || placement.item() == Items.AIR) {
            return false;
        }
        required.merge(placement.item(), 1, Integer::sum);
        if (!placement.isBlank()) {
            return true;
        }
        for (Identifier id : placement.appearance().configuredMaterials()) {
            Item item = BlankBlockMaterialRegistry.item(id);
            if (item == Items.AIR) {
                return false;
            }
            required.merge(item, 1, Integer::sum);
        }
        return true;
    }

    private static Optional<Map<Item, Integer>> readEscrowStrict(NbtList list) {
        if (list.size() > MAX_ESCROW_TYPES) {
            return Optional.empty();
        }
        IdentityHashMap<Item, Integer> escrow = new IdentityHashMap<>();
        int total = 0;
        for (int index = 0; index < list.size(); index++) {
            NbtCompound tag = list.getCompound(index);
            Identifier itemId = Identifier.tryParse(tag.getString("Item"));
            int count = tag.getInt("Count");
            if (itemId == null || !Registries.ITEM.containsId(itemId) || count <= 0) {
                return Optional.empty();
            }
            Item item = Registries.ITEM.get(itemId);
            if (item == Items.AIR) {
                return Optional.empty();
            }
            if (count > MAX_ESCROW_ITEMS - total) {
                return Optional.empty();
            }
            total += count;
            escrow.merge(item, count, Integer::sum);
        }
        return Optional.of(escrow);
    }

    /** Salvages the largest bounded escrow entry per item when the job is malformed. */
    static Map<Item, Integer> readEscrowLenient(NbtCompound root) {
        IdentityHashMap<Item, Integer> salvaged = new IdentityHashMap<>();
        readEscrowLenient(root, "Escrow", salvaged);
        readEscrowLenient(root, "InitialEscrow", salvaged);
        return salvaged;
    }

    private static void readEscrowLenient(
            NbtCompound root,
            String key,
            IdentityHashMap<Item, Integer> salvaged
    ) {
        if (root == null || !root.contains(key, NbtElement.LIST_TYPE)) {
            return;
        }
        NbtList list = root.getList(key, NbtElement.COMPOUND_TYPE);
        int total = salvaged.values().stream().mapToInt(Integer::intValue).sum();
        for (int index = 0; index < Math.min(list.size(), MAX_ESCROW_TYPES); index++) {
            NbtCompound tag = list.getCompound(index);
            Identifier id = Identifier.tryParse(tag.getString("Item"));
            int count = tag.getInt("Count");
            if (id == null || !Registries.ITEM.containsId(id) || count <= 0) {
                continue;
            }
            Item item = Registries.ITEM.get(id);
            if (item == Items.AIR) {
                continue;
            }
            int existing = salvaged.getOrDefault(item, 0);
            int additional = Math.max(0, count - existing);
            int accepted = Math.min(additional, MAX_ESCROW_ITEMS - total);
            if (accepted <= 0) {
                continue;
            }
            salvaged.put(item, existing + accepted);
            total += accepted;
        }
    }

    static IdentityHashMap<Item, Integer> copyIdentity(Map<Item, Integer> source) {
        IdentityHashMap<Item, Integer> copy = new IdentityHashMap<>();
        copy.putAll(source);
        return copy;
    }

    record DecodedJob(
            UUID jobId,
            BuildingPlan2DManager.JobKind kind,
            String dimensionId,
            List<BuildingPlan2DManager.Placement> placements,
            Map<Item, Integer> initialEscrow,
            Map<Item, Integer> remainingEscrow,
            int cursor,
            int totalPlacements,
            boolean creative
    ) {
    }
}
