package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ResetTreePayload(String skillId) implements CustomPayload {
    public static final CustomPayload.Id<ResetTreePayload> ID =
            new CustomPayload.Id<>(Identifier.of("mythicrpg", "reset_tree"));

    public static final PacketCodec<RegistryByteBuf, ResetTreePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ResetTreePayload::skillId,
            ResetTreePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}