package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TitleSelectionPayload(
        String titleId,
        String primaryColorId,
        String secondaryColorId,
        boolean gradient,
        String finishId
) implements CustomPayload {
    public static final Id<TitleSelectionPayload> ID =
            new Id<>(Identifier.of("mythicrpg", "title_selection"));

    public static final PacketCodec<RegistryByteBuf, TitleSelectionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, TitleSelectionPayload::titleId,
            PacketCodecs.STRING, TitleSelectionPayload::primaryColorId,
            PacketCodecs.STRING, TitleSelectionPayload::secondaryColorId,
            PacketCodecs.BOOL, TitleSelectionPayload::gradient,
            PacketCodecs.STRING, TitleSelectionPayload::finishId,
            TitleSelectionPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
