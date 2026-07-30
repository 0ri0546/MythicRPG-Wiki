package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Temporary playtest payload carrying only nearby grand-site block positions. */
public record GrandSiteHighlightPayload(List<Long> positions) implements CustomPayload {

    public static final int MAX_POSITIONS = 4096;

    public static final Id<GrandSiteHighlightPayload> ID = new Id<>(
            Identifier.of("mythicrpg", "grand_site_highlight")
    );

    public static final PacketCodec<RegistryByteBuf, GrandSiteHighlightPayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.positions().size());
                        for (long position : payload.positions()) {
                            buffer.writeLong(position);
                        }
                    },
                    buffer -> {
                        int size = buffer.readVarInt();
                        if (size < 0 || size > MAX_POSITIONS) {
                            throw new IllegalArgumentException("Invalid grand-site highlight payload size: " + size);
                        }
                        ArrayList<Long> positions = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            positions.add(buffer.readLong());
                        }
                        return new GrandSiteHighlightPayload(List.copyOf(positions));
                    }
            );

    public GrandSiteHighlightPayload {
        if (positions.size() > MAX_POSITIONS) {
            throw new IllegalArgumentException("Too many grand-site highlight positions");
        }
        positions = List.copyOf(positions);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
