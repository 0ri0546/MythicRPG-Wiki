package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record XpGainPayload(String skillId, int level, int currentXp, int xpForNext) implements CustomPayload {
    public static final CustomPayload.Id<XpGainPayload> ID =
            new CustomPayload.Id<>(Identifier.of("mythicrpg", "xp_gain"));

    public static final PacketCodec<RegistryByteBuf, XpGainPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, XpGainPayload::skillId,
            PacketCodecs.INTEGER, XpGainPayload::level,
            PacketCodecs.INTEGER, XpGainPayload::currentXp,
            PacketCodecs.INTEGER, XpGainPayload::xpForNext,
            XpGainPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}