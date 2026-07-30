package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record LevelUpPayload(String skillId, int level, int currentXp, int xpForNext) implements CustomPayload {
    public static final CustomPayload.Id<LevelUpPayload> ID =
            new CustomPayload.Id<>(Identifier.of("mythicrpg", "level_up"));

    public static final PacketCodec<RegistryByteBuf, LevelUpPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, LevelUpPayload::skillId,
            PacketCodecs.INTEGER, LevelUpPayload::level,
            PacketCodecs.INTEGER, LevelUpPayload::currentXp,
            PacketCodecs.INTEGER, LevelUpPayload::xpForNext,
            LevelUpPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}