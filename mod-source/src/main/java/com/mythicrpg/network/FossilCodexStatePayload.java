package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.List;

public record FossilCodexStatePayload(List<String> keys, List<Integer> reconstructedCounts,
        List<Long> firstReconstructedDays, List<Integer> analyzedCounts) implements CustomPayload {
    private static final int MAX_ENTRIES = 256;
    public FossilCodexStatePayload {
        int size = Math.min(MAX_ENTRIES, Math.min(Math.min(safeSize(keys), safeSize(reconstructedCounts)),
                Math.min(safeSize(firstReconstructedDays), safeSize(analyzedCounts))));
        keys = copy(keys, size); reconstructedCounts = copy(reconstructedCounts, size);
        firstReconstructedDays = copy(firstReconstructedDays, size); analyzedCounts = copy(analyzedCounts, size);
    }
    private static int safeSize(List<?> list) { return list == null ? 0 : list.size(); }
    private static <T> List<T> copy(List<T> list, int size) { return size == 0 ? List.of() : List.copyOf(list.subList(0, size)); }
    public static final Id<FossilCodexStatePayload> ID = new Id<>(Identifier.of("mythicrpg", "fossil_codex_state"));
    public static final PacketCodec<RegistryByteBuf, FossilCodexStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.collect(PacketCodecs.toList()), FossilCodexStatePayload::keys,
            PacketCodecs.INTEGER.collect(PacketCodecs.toList()), FossilCodexStatePayload::reconstructedCounts,
            PacketCodecs.VAR_LONG.collect(PacketCodecs.toList()), FossilCodexStatePayload::firstReconstructedDays,
            PacketCodecs.INTEGER.collect(PacketCodecs.toList()), FossilCodexStatePayload::analyzedCounts,
            FossilCodexStatePayload::new);
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
