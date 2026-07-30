package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Bounded S2C snapshot for the three legendary Fishing Codex cards. */
public record SeaMonsterStatePayload(
        List<Integer> gauges,
        List<Integer> victories,
        List<Long> firstVictoryDays,
        List<String> firstVictoryDimensions
) implements CustomPayload {
    private static final int ENTRY_COUNT = 3;

    public SeaMonsterStatePayload {
        gauges = boundedInts(gauges, 0, 1000);
        victories = boundedInts(victories, 0, 1_000_000);
        firstVictoryDays = boundedLongs(firstVictoryDays);
        firstVictoryDimensions = boundedStrings(firstVictoryDimensions);
    }

    public static final Id<SeaMonsterStatePayload> ID =
            new Id<>(Identifier.of("mythicrpg", "sea_monster_state"));

    public static final PacketCodec<RegistryByteBuf, SeaMonsterStatePayload> CODEC = PacketCodec.ofStatic(
            (buffer, payload) -> {
                for (int index = 0; index < ENTRY_COUNT; index++) {
                    buffer.writeVarInt(payload.gauges().get(index));
                    buffer.writeVarInt(payload.victories().get(index));
                    buffer.writeVarLong(payload.firstVictoryDays().get(index));
                    buffer.writeString(payload.firstVictoryDimensions().get(index), 128);
                }
            },
            buffer -> {
                ArrayList<Integer> gauges = new ArrayList<>(ENTRY_COUNT);
                ArrayList<Integer> victories = new ArrayList<>(ENTRY_COUNT);
                ArrayList<Long> days = new ArrayList<>(ENTRY_COUNT);
                ArrayList<String> dimensions = new ArrayList<>(ENTRY_COUNT);
                for (int index = 0; index < ENTRY_COUNT; index++) {
                    gauges.add(buffer.readVarInt());
                    victories.add(buffer.readVarInt());
                    days.add(buffer.readVarLong());
                    dimensions.add(buffer.readString(128));
                }
                return new SeaMonsterStatePayload(gauges, victories, days, dimensions);
            }
    );

    private static List<Integer> boundedInts(List<Integer> source, int min, int max) {
        ArrayList<Integer> result = new ArrayList<>(ENTRY_COUNT);
        for (int index = 0; index < ENTRY_COUNT; index++) {
            int value = source != null && index < source.size() && source.get(index) != null ? source.get(index) : 0;
            result.add(Math.max(min, Math.min(max, value)));
        }
        return List.copyOf(result);
    }

    private static List<Long> boundedLongs(List<Long> source) {
        ArrayList<Long> result = new ArrayList<>(ENTRY_COUNT);
        for (int index = 0; index < ENTRY_COUNT; index++) {
            long value = source != null && index < source.size() && source.get(index) != null ? source.get(index) : 0L;
            result.add(Math.max(0L, value));
        }
        return List.copyOf(result);
    }

    private static List<String> boundedStrings(List<String> source) {
        ArrayList<String> result = new ArrayList<>(ENTRY_COUNT);
        for (int index = 0; index < ENTRY_COUNT; index++) {
            String value = source != null && index < source.size() && source.get(index) != null ? source.get(index) : "";
            result.add(value.length() <= 128 ? value : value.substring(0, 128));
        }
        return List.copyOf(result);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
