package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record TreeStatePayload(String skillId, List<Integer> unlockedIds, int skillPoints,
                               int level, int currentXp, int xpForNext) implements CustomPayload {
    public static final CustomPayload.Id<TreeStatePayload> ID =
            new CustomPayload.Id<>(Identifier.of("mythicrpg", "tree_state"));

    public static final PacketCodec<RegistryByteBuf, TreeStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, TreeStatePayload::skillId,
            PacketCodecs.INTEGER.collect(PacketCodecs.toList()), TreeStatePayload::unlockedIds,
            PacketCodecs.INTEGER, TreeStatePayload::skillPoints,
            PacketCodecs.INTEGER, TreeStatePayload::level,
            PacketCodecs.INTEGER, TreeStatePayload::currentXp,
            PacketCodecs.INTEGER, TreeStatePayload::xpForNext,
            TreeStatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}