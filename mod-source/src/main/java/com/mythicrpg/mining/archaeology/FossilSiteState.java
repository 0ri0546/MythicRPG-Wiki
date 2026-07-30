package com.mythicrpg.mining.archaeology;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class FossilSiteState extends PersistentState {

    private static final String STATE_ID = "mythicrpg_fossil_sites";
    private static final Type<FossilSiteState> TYPE = new Type<>(
            FossilSiteState::new,
            FossilSiteState::fromNbt,
            null
    );

    private final Map<UUID, SiteRecord> sites = new HashMap<>();

    public static FossilSiteState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public void registerSite(
            UUID siteId,
            BlockPos center,
            FossilFamily family,
            FossilRarity dominantRarity,
            int initialBlocks
    ) {
        if (sites.containsKey(siteId)) {
            return;
        }

        sites.put(siteId, new SiteRecord(
                siteId,
                center.toImmutable(),
                family,
                dominantRarity,
                initialBlocks,
                initialBlocks
        ));
        markDirty();
    }

    public void markExtracted(UUID siteId) {
        SiteRecord current = sites.get(siteId);
        if (current == null || current.remainingBlocks() <= 0) {
            return;
        }

        int remaining = current.remainingBlocks() - 1;
        if (remaining <= 0) {
            sites.remove(siteId);
        } else {
            sites.put(siteId, current.withRemaining(remaining));
        }
        markDirty();
    }

    public void setRemaining(UUID siteId, int remainingBlocks) {
        SiteRecord current = sites.get(siteId);
        if (current == null) {
            return;
        }

        int correctedRemaining = Math.max(
                0,
                Math.min(current.initialBlocks(), remainingBlocks)
        );

        if (correctedRemaining == current.remainingBlocks()) {
            return;
        }

        if (correctedRemaining <= 0) {
            sites.remove(siteId);
        } else {
            sites.put(siteId, current.withRemaining(correctedRemaining));
        }
        markDirty();
    }

    public Optional<SiteRecord> findNearest(
            BlockPos origin,
            FossilFamily family,
            FossilRarity rarity,
            boolean includeDepleted
    ) {
        return sites.values().stream()
                .filter(site -> includeDepleted || site.remainingBlocks() > 0)
                .filter(site -> family == null || site.family() == family)
                .filter(site -> rarity == null || site.dominantRarity() == rarity)
                .min(Comparator.comparingDouble(site -> site.center().getSquaredDistance(origin)));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList siteList = new NbtList();

        for (SiteRecord site : sites.values()) {
            NbtCompound siteTag = new NbtCompound();
            siteTag.putString("id", site.id().toString());
            siteTag.putLong("center", site.center().asLong());
            siteTag.putString("family", site.family().id());
            siteTag.putString("dominant_rarity", site.dominantRarity().id());
            siteTag.putInt("initial_blocks", site.initialBlocks());
            siteTag.putInt("remaining_blocks", site.remainingBlocks());
            siteList.add(siteTag);
        }

        nbt.put("sites", siteList);
        return nbt;
    }

    private static FossilSiteState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        FossilSiteState state = new FossilSiteState();
        NbtList siteList = nbt.getList("sites", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < siteList.size(); i++) {
            NbtCompound siteTag = siteList.getCompound(i);

            try {
                UUID id = UUID.fromString(siteTag.getString("id"));
                Optional<FossilFamily> family = FossilFamily.byId(siteTag.getString("family"));
                Optional<FossilRarity> rarity = FossilRarity.byId(siteTag.getString("dominant_rarity"));

                if (family.isEmpty() || rarity.isEmpty()) {
                    continue;
                }

                int initial = Math.max(1, siteTag.getInt("initial_blocks"));
                int remaining = Math.max(0, Math.min(initial, siteTag.getInt("remaining_blocks")));
                if (remaining > 0) {
                    state.sites.put(id, new SiteRecord(
                            id,
                            BlockPos.fromLong(siteTag.getLong("center")),
                            family.get(),
                            rarity.get(),
                            initial,
                            remaining
                    ));
                }
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entries rather than preventing world load.
            }
        }

        return state;
    }

    public record SiteRecord(
            UUID id,
            BlockPos center,
            FossilFamily family,
            FossilRarity dominantRarity,
            int initialBlocks,
            int remainingBlocks
    ) {
        SiteRecord withRemaining(int remaining) {
            return new SiteRecord(id, center, family, dominantRarity, initialBlocks, remaining);
        }

        public boolean depleted() {
            return remainingBlocks <= 0;
        }
    }
}
