
package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Bounded S2C snapshot of the 25 Fishing Codex cards. */
public record FishingCodexStatePayload(
        List<String> familyIds,
        List<Integer> rarityRanks,
        List<Integer> captureCounts,
        List<Long> firstDiscoveryDays,
        List<String> firstBiomes,
        List<String> firstDimensions,
        List<String> lastSources
) implements CustomPayload {
    private static final int MAX_ENTRIES = 25;

    public FishingCodexStatePayload {
        int size = Math.min(
                MAX_ENTRIES,
                Math.min(
                        Math.min(safeSize(familyIds), safeSize(rarityRanks)),
                        Math.min(
                                Math.min(safeSize(captureCounts), safeSize(firstDiscoveryDays)),
                                Math.min(
                                        Math.min(safeSize(firstBiomes), safeSize(firstDimensions)),
                                        safeSize(lastSources)
                                )
                        )
                )
        );
        familyIds = copy(familyIds, size);
        rarityRanks = copy(rarityRanks, size);
        captureCounts = copy(captureCounts, size);
        firstDiscoveryDays = copy(firstDiscoveryDays, size);
        firstBiomes = copy(firstBiomes, size);
        firstDimensions = copy(firstDimensions, size);
        lastSources = copy(lastSources, size);
    }

    public static final Id<FishingCodexStatePayload> ID =
            new Id<>(Identifier.of("mythicrpg", "fishing_codex_state"));

    public static final PacketCodec<RegistryByteBuf, FishingCodexStatePayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        int size = payload.familyIds().size();
                        buffer.writeVarInt(size);
                        for (int index = 0; index < size; index++) {
                            buffer.writeString(payload.familyIds().get(index), 32);
                            buffer.writeVarInt(payload.rarityRanks().get(index));
                            buffer.writeVarInt(payload.captureCounts().get(index));
                            buffer.writeVarLong(payload.firstDiscoveryDays().get(index));
                            buffer.writeString(payload.firstBiomes().get(index), 128);
                            buffer.writeString(payload.firstDimensions().get(index), 128);
                            buffer.writeString(payload.lastSources().get(index), 16);
                        }
                    },
                    buffer -> {
                        int size = buffer.readVarInt();
                        if (size < 0 || size > MAX_ENTRIES) {
                            throw new IllegalArgumentException("Invalid Fishing Codex entry count: " + size);
                        }

                        ArrayList<String> familyIds = new ArrayList<>(size);
                        ArrayList<Integer> rarityRanks = new ArrayList<>(size);
                        ArrayList<Integer> captureCounts = new ArrayList<>(size);
                        ArrayList<Long> firstDays = new ArrayList<>(size);
                        ArrayList<String> biomes = new ArrayList<>(size);
                        ArrayList<String> dimensions = new ArrayList<>(size);
                        ArrayList<String> sources = new ArrayList<>(size);

                        for (int index = 0; index < size; index++) {
                            familyIds.add(buffer.readString(32));
                            rarityRanks.add(buffer.readVarInt());
                            captureCounts.add(buffer.readVarInt());
                            firstDays.add(buffer.readVarLong());
                            biomes.add(buffer.readString(128));
                            dimensions.add(buffer.readString(128));
                            sources.add(buffer.readString(16));
                        }

                        return new FishingCodexStatePayload(
                                familyIds,
                                rarityRanks,
                                captureCounts,
                                firstDays,
                                biomes,
                                dimensions,
                                sources
                        );
                    }
            );

    private static int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static <T> List<T> copy(List<T> list, int size) {
        return size <= 0 ? List.of() : List.copyOf(list.subList(0, size));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
