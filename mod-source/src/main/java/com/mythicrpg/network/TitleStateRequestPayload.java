package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TitleStateRequestPayload() implements CustomPayload {
    public static final Id<TitleStateRequestPayload> ID =
            new Id<>(Identifier.of("mythicrpg", "title_state_request"));

    public static final PacketCodec<RegistryByteBuf, TitleStateRequestPayload> CODEC =
            PacketCodec.unit(new TitleStateRequestPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
