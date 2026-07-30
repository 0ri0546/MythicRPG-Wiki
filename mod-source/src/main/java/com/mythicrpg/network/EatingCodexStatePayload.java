package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.List;

public record EatingCodexStatePayload(List<String> recipeIds, List<Integer> preparationCounts,
        List<Integer> bestRarityRanks, List<Long> firstDiscoveryDays, List<Integer> lastPortions,
        List<Integer> lastShelfLifeDays) implements CustomPayload {
    private static final int MAX_ENTRIES = 256;
    public EatingCodexStatePayload {
        int size = Math.min(MAX_ENTRIES, Math.min(Math.min(Math.min(safeSize(recipeIds), safeSize(preparationCounts)),
                Math.min(safeSize(bestRarityRanks), safeSize(firstDiscoveryDays))),
                Math.min(safeSize(lastPortions), safeSize(lastShelfLifeDays))));
        recipeIds = copy(recipeIds, size); preparationCounts = copy(preparationCounts, size);
        bestRarityRanks = copy(bestRarityRanks, size); firstDiscoveryDays = copy(firstDiscoveryDays, size);
        lastPortions = copy(lastPortions, size); lastShelfLifeDays = copy(lastShelfLifeDays, size);
    }
    private static int safeSize(List<?> list) { return list == null ? 0 : list.size(); }
    private static <T> List<T> copy(List<T> list, int size) { return size == 0 ? List.of() : List.copyOf(list.subList(0, size)); }
    public static final Id<EatingCodexStatePayload> ID = new Id<>(Identifier.of("mythicrpg", "eating_codex_state"));
    public static final PacketCodec<RegistryByteBuf, EatingCodexStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.collect(PacketCodecs.toList()), EatingCodexStatePayload::recipeIds,
            PacketCodecs.INTEGER.collect(PacketCodecs.toList()), EatingCodexStatePayload::preparationCounts,
            PacketCodecs.INTEGER.collect(PacketCodecs.toList()), EatingCodexStatePayload::bestRarityRanks,
            PacketCodecs.VAR_LONG.collect(PacketCodecs.toList()), EatingCodexStatePayload::firstDiscoveryDays,
            PacketCodecs.INTEGER.collect(PacketCodecs.toList()), EatingCodexStatePayload::lastPortions,
            PacketCodecs.INTEGER.collect(PacketCodecs.toList()), EatingCodexStatePayload::lastShelfLifeDays,
            EatingCodexStatePayload::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
