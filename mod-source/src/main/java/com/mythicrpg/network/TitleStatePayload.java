package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record TitleStatePayload(
        List<String> unlockedTitleIds,
        String activeTitleId,
        String primaryColorId,
        String secondaryColorId,
        boolean gradient,
        String finishId
) implements CustomPayload {
    public static final Id<TitleStatePayload> ID =
            new Id<>(Identifier.of("mythicrpg", "title_state"));

    public static final PacketCodec<RegistryByteBuf, TitleStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.collect(PacketCodecs.toList()), TitleStatePayload::unlockedTitleIds,
            PacketCodecs.STRING, TitleStatePayload::activeTitleId,
            PacketCodecs.STRING, TitleStatePayload::primaryColorId,
            PacketCodecs.STRING, TitleStatePayload::secondaryColorId,
            PacketCodecs.BOOL, TitleStatePayload::gradient,
            PacketCodecs.STRING, TitleStatePayload::finishId,
            TitleStatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
