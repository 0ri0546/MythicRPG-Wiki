package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UnlockRequestPayload(String skillId, int nodeId) implements CustomPayload {
    public static final CustomPayload.Id<UnlockRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("mythicrpg", "unlock_request"));

    public static final PacketCodec<RegistryByteBuf, UnlockRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, UnlockRequestPayload::skillId,
            PacketCodecs.INTEGER, UnlockRequestPayload::nodeId,
            UnlockRequestPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}