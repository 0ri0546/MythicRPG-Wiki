package com.mythicrpg.mining.archaeology;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Persistent, world-global index for generated archaeology expeditions. */
public final class GrandFossilSiteState extends PersistentState {

    private static final String STATE_ID = "mythicrpg_grand_fossil_sites";
    private static final Type<GrandFossilSiteState> TYPE = new Type<>(
            GrandFossilSiteState::new,
            GrandFossilSiteState::fromNbt,
            null
    );

    private final Map<UUID, GrandSiteRecord> sites = new HashMap<>();
    private final Set<UUID> analyzedSpecimenIds = new HashSet<>();
    private final Set<UUID> reservedSpecimenIds = new HashSet<>();

    // Transient indexes rebuilt from the persisted records. They keep frequent
    // proximity/discovery/barrel lookups independent from the total site count.
    private final Map<Long, Set<UUID>> siteIdsByCenterChunk = new HashMap<>();
    private final Map<Long, UUID> siteIdByBarrelPosition = new HashMap<>();
    private final Map<UUID, UUID> siteIdBySpecimen = new HashMap<>();
    private final Map<UUID, Set<UUID>> siteIdsByReconstructor = new HashMap<>();

    public static GrandFossilSiteState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public synchronized boolean tryReserveSpecimen(UUID specimenId) {
        if (specimenId == null || analyzedSpecimenIds.contains(specimenId)) {
            return false;
        }
        return reservedSpecimenIds.add(specimenId);
    }

    public synchronized void releaseSpecimen(UUID specimenId) {
        reservedSpecimenIds.remove(specimenId);
    }

    public synchronized boolean registerCompletedSite(GrandSiteRecord record) {
        if (record == null
                || sites.containsKey(record.id())
                || analyzedSpecimenIds.contains(record.specimenId())) {
            if (record != null) {
                reservedSpecimenIds.remove(record.specimenId());
            }
            return false;
        }

        sites.put(record.id(), record);
        indexSite(record);
        analyzedSpecimenIds.add(record.specimenId());
        reservedSpecimenIds.remove(record.specimenId());
        markDirty();
        return true;
    }

    public synchronized boolean isSpecimenAnalyzed(UUID specimenId) {
        return analyzedSpecimenIds.contains(specimenId);
    }

    public synchronized int resetSpecimenLocks(Collection<UUID> specimenIds) {
        int removed = 0;
        for (UUID specimenId : specimenIds) {
            if (analyzedSpecimenIds.remove(specimenId)) {
                removed++;
            }
            reservedSpecimenIds.remove(specimenId);
        }
        if (removed > 0) {
            markDirty();
        }
        return removed;
    }

    public synchronized void markFossilRemoved(UUID siteId) {
        GrandSiteRecord current = sites.get(siteId);
        if (current == null || current.remainingFossils() <= 0) {
            return;
        }
        int remaining = current.remainingFossils() - 1;
        GrandSiteStatus status = remaining <= 0
                ? GrandSiteStatus.DEPLETED
                : GrandSiteStatus.PARTIALLY_EXCAVATED;
        sites.put(siteId, current.withRemainingAndStatus(remaining, status));
        markDirty();
    }

    public synchronized void setRemainingFossils(UUID siteId, int remaining) {
        GrandSiteRecord current = sites.get(siteId);
        if (current == null) {
            return;
        }
        int corrected = Math.max(0, Math.min(current.initialFossils(), remaining));
        GrandSiteStatus status;
        if (corrected <= 0) {
            status = GrandSiteStatus.DEPLETED;
        } else if (corrected < current.initialFossils()) {
            status = GrandSiteStatus.PARTIALLY_EXCAVATED;
        } else if (current.status() == GrandSiteStatus.DISCOVERED) {
            status = GrandSiteStatus.DISCOVERED;
        } else {
            status = GrandSiteStatus.GENERATED;
        }
        if (corrected != current.remainingFossils() || status != current.status()) {
            sites.put(siteId, current.withRemainingAndStatus(corrected, status));
            markDirty();
        }
    }

    public synchronized void markDiscovered(UUID siteId) {
        GrandSiteRecord current = sites.get(siteId);
        if (current == null || current.status() != GrandSiteStatus.GENERATED) {
            return;
        }
        sites.put(siteId, current.withStatus(GrandSiteStatus.DISCOVERED));
        markDirty();
    }

    public synchronized void markBarrelRemoved(UUID siteId) {
        GrandSiteRecord current = sites.get(siteId);
        if (current == null || !current.barrelPresent()) {
            return;
        }
        sites.put(siteId, current.withBarrelPresent(false));
        markDirty();
    }

    public synchronized void setBarrelPresent(UUID siteId, boolean present) {
        GrandSiteRecord current = sites.get(siteId);
        if (current == null || current.barrelPresent() == present) {
            return;
        }
        sites.put(siteId, current.withBarrelPresent(present));
        markDirty();
    }

    public synchronized Optional<GrandSiteRecord> findNearest(
            BlockPos origin,
            FossilFamily family,
            GrandSiteStatus status,
            boolean includeDepleted
    ) {
        return sites.values().stream()
                .filter(site -> includeDepleted || site.status() != GrandSiteStatus.DEPLETED)
                .filter(site -> family == null || site.family() == family)
                .filter(site -> status == null || site.status() == status)
                .min(Comparator.comparingDouble(site -> site.center().getSquaredDistance(origin)));
    }

    public synchronized Optional<GrandSiteRecord> findById(UUID siteId) {
        return Optional.ofNullable(sites.get(siteId));
    }

    public synchronized Optional<GrandSiteRecord> findBySpecimenId(UUID specimenId) {
        UUID siteId = siteIdBySpecimen.get(specimenId);
        return Optional.ofNullable(siteId).map(sites::get);
    }

    public synchronized List<GrandSiteRecord> sitesForReconstructor(UUID playerUuid) {
        Set<UUID> ids = siteIdsByReconstructor.get(playerUuid);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        ArrayList<GrandSiteRecord> result = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            GrandSiteRecord site = sites.get(id);
            if (site != null) {
                result.add(site);
            }
        }
        return List.copyOf(result);
    }

    public synchronized boolean isAreaNearExistingSite(BlockPos center, int minimumDistance) {
        double minimumSquared = (double) minimumDistance * minimumDistance;
        return sitesNear(center, minimumDistance).stream()
                .anyMatch(site -> site.center().getSquaredDistance(center) < minimumSquared);
    }

    public synchronized Optional<GrandSiteRecord> findNearestWithin(
            BlockPos origin,
            int radius,
            FossilFamily family,
            GrandSiteStatus status,
            boolean includeDepleted
    ) {
        double radiusSquared = (double) radius * radius;
        return sitesNear(origin, radius).stream()
                .filter(site -> site.center().getSquaredDistance(origin) <= radiusSquared)
                .filter(site -> includeDepleted || site.status() != GrandSiteStatus.DEPLETED)
                .filter(site -> family == null || site.family() == family)
                .filter(site -> status == null || site.status() == status)
                .min(Comparator.comparingDouble(site -> site.center().getSquaredDistance(origin)));
    }

    public synchronized Optional<GrandSiteRecord> siteByBarrelPosition(BlockPos pos) {
        UUID siteId = siteIdByBarrelPosition.get(pos.asLong());
        return Optional.ofNullable(siteId).map(sites::get);
    }

    private void indexSite(GrandSiteRecord record) {
        long centerChunk = ChunkPos.toLong(record.center().getX() >> 4, record.center().getZ() >> 4);
        siteIdsByCenterChunk.computeIfAbsent(centerChunk, ignored -> new HashSet<>()).add(record.id());
        siteIdByBarrelPosition.put(record.barrelPos().asLong(), record.id());
        siteIdBySpecimen.putIfAbsent(record.specimenId(), record.id());
        siteIdsByReconstructor
                .computeIfAbsent(record.reconstructedBy(), ignored -> new HashSet<>())
                .add(record.id());
    }

    private List<GrandSiteRecord> sitesNear(BlockPos origin, int radius) {
        int minChunkX = (origin.getX() - radius) >> 4;
        int maxChunkX = (origin.getX() + radius) >> 4;
        int minChunkZ = (origin.getZ() - radius) >> 4;
        int maxChunkZ = (origin.getZ() + radius) >> 4;
        HashSet<UUID> ids = new HashSet<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Set<UUID> bucket = siteIdsByCenterChunk.get(ChunkPos.toLong(chunkX, chunkZ));
                if (bucket != null) {
                    ids.addAll(bucket);
                }
            }
        }
        ArrayList<GrandSiteRecord> result = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            GrandSiteRecord site = sites.get(id);
            if (site != null) {
                result.add(site);
            }
        }
        return result;
    }

    @Override
    public synchronized NbtCompound writeNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        NbtList siteList = new NbtList();
        for (GrandSiteRecord site : sites.values()) {
            NbtCompound tag = new NbtCompound();
            tag.putString("id", site.id().toString());
            tag.putString("specimen_id", site.specimenId().toString());
            tag.putString("owner", site.owner().toString());
            tag.putString("reconstructed_by", site.reconstructedBy().toString());
            tag.putString("archaeologist", site.archaeologist().toString());
            tag.putLong("center", site.center().asLong());
            tag.putString("specimen_family", site.specimenFamily().id());
            tag.putString("specimen_rarity", site.specimenRarity().id());
            tag.putString("family", site.family().id());
            tag.putString("dominant_rarity", site.dominantRarity().id());
            tag.putString("biome", site.biomeId());
            tag.putInt("initial_fossils", site.initialFossils());
            tag.putInt("remaining_fossils", site.remainingFossils());
            tag.putInt("ore_blocks", site.oreBlocks());
            tag.putBoolean("special_roll", site.specialRollSucceeded());
            tag.putLong("barrel_pos", site.barrelPos().asLong());
            tag.putBoolean("barrel_present", site.barrelPresent());
            tag.putString("status", site.status().id());
            siteList.add(tag);
        }
        nbt.put("sites", siteList);

        NbtList analyzedList = new NbtList();
        for (UUID specimenId : analyzedSpecimenIds) {
            NbtCompound tag = new NbtCompound();
            tag.putString("id", specimenId.toString());
            analyzedList.add(tag);
        }
        nbt.put("analyzed_specimens", analyzedList);
        return nbt;
    }

    private static GrandFossilSiteState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        GrandFossilSiteState state = new GrandFossilSiteState();
        boolean hasExplicitAnalyzedList = nbt.contains("analyzed_specimens", NbtElement.LIST_TYPE);
        NbtList siteList = nbt.getList("sites", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < siteList.size(); i++) {
            NbtCompound tag = siteList.getCompound(i);
            try {
                Optional<FossilFamily> family = FossilFamily.byId(tag.getString("family"));
                Optional<FossilRarity> rarity = FossilRarity.byId(tag.getString("dominant_rarity"));
                Optional<FossilFamily> specimenFamily = tag.contains("specimen_family")
                        ? FossilFamily.byId(tag.getString("specimen_family"))
                        : family;
                Optional<FossilRarity> specimenRarity = tag.contains("specimen_rarity")
                        ? FossilRarity.byId(tag.getString("specimen_rarity"))
                        : rarity;
                if (family.isEmpty() || rarity.isEmpty()
                        || specimenFamily.isEmpty() || specimenRarity.isEmpty()) {
                    continue;
                }
                UUID id = UUID.fromString(tag.getString("id"));
                UUID specimenId = UUID.fromString(tag.getString("specimen_id"));
                UUID owner = UUID.fromString(tag.getString("owner"));
                UUID reconstructedBy = tag.contains("reconstructed_by")
                        ? UUID.fromString(tag.getString("reconstructed_by"))
                        : owner;
                UUID archaeologist = UUID.fromString(tag.getString("archaeologist"));
                int initial = Math.max(1, tag.getInt("initial_fossils"));
                int remaining = Math.max(0, Math.min(initial, tag.getInt("remaining_fossils")));
                GrandSiteStatus status = GrandSiteStatus.byId(tag.getString("status"))
                        .orElse(remaining <= 0
                                ? GrandSiteStatus.DEPLETED
                                : GrandSiteStatus.GENERATED);
                GrandSiteRecord record = new GrandSiteRecord(
                        id,
                        specimenId,
                        owner,
                        reconstructedBy,
                        archaeologist,
                        BlockPos.fromLong(tag.getLong("center")),
                        specimenFamily.get(),
                        specimenRarity.get(),
                        family.get(),
                        rarity.get(),
                        tag.getString("biome"),
                        initial,
                        remaining,
                        Math.max(0, tag.getInt("ore_blocks")),
                        tag.getBoolean("special_roll"),
                        BlockPos.fromLong(tag.getLong("barrel_pos")),
                        !tag.contains("barrel_present") || tag.getBoolean("barrel_present"),
                        status
                );
                state.sites.put(id, record);
                state.indexSite(record);
                if (!hasExplicitAnalyzedList) {
                    state.analyzedSpecimenIds.add(specimenId);
                }
            } catch (IllegalArgumentException ignored) {
                // Malformed expedition records are skipped instead of blocking world load.
            }
        }

        NbtList analyzedList = nbt.getList("analyzed_specimens", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < analyzedList.size(); i++) {
            try {
                state.analyzedSpecimenIds.add(UUID.fromString(analyzedList.getCompound(i).getString("id")));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed UUIDs.
            }
        }
        return state;
    }

    public record GrandSiteRecord(
            UUID id,
            UUID specimenId,
            UUID owner,
            UUID reconstructedBy,
            UUID archaeologist,
            BlockPos center,
            FossilFamily specimenFamily,
            FossilRarity specimenRarity,
            FossilFamily family,
            FossilRarity dominantRarity,
            String biomeId,
            int initialFossils,
            int remainingFossils,
            int oreBlocks,
            boolean specialRollSucceeded,
            BlockPos barrelPos,
            boolean barrelPresent,
            GrandSiteStatus status
    ) {
        public GrandSiteRecord {
            center = center.toImmutable();
            barrelPos = barrelPos.toImmutable();
        }

        GrandSiteRecord withRemainingAndStatus(int remaining, GrandSiteStatus newStatus) {
            return new GrandSiteRecord(
                    id, specimenId, owner, reconstructedBy, archaeologist, center,
                    specimenFamily, specimenRarity, family, dominantRarity,
                    biomeId, initialFossils, remaining, oreBlocks,
                    specialRollSucceeded, barrelPos, barrelPresent, newStatus
            );
        }

        GrandSiteRecord withStatus(GrandSiteStatus newStatus) {
            return withRemainingAndStatus(remainingFossils, newStatus);
        }

        GrandSiteRecord withBarrelPresent(boolean present) {
            return new GrandSiteRecord(
                    id, specimenId, owner, reconstructedBy, archaeologist, center,
                    specimenFamily, specimenRarity, family, dominantRarity,
                    biomeId, initialFossils, remainingFossils, oreBlocks,
                    specialRollSucceeded, barrelPos, present, status
            );
        }
    }
}
