
package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FishingMiniGameOpenPayload(
        int gameType,
        int a,
        int b,
        boolean mastery,
        boolean guaranteed,
        int rarityRank
) implements CustomPayload {
    public static final Id<FishingMiniGameOpenPayload> ID =
            new Id<>(Identifier.of("mythicrpg", "fishing_minigame_open"));

    public static final PacketCodec<RegistryByteBuf, FishingMiniGameOpenPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, FishingMiniGameOpenPayload::gameType,
                    PacketCodecs.INTEGER, FishingMiniGameOpenPayload::a,
                    PacketCodecs.INTEGER, FishingMiniGameOpenPayload::b,
                    PacketCodecs.BOOL, FishingMiniGameOpenPayload::mastery,
                    PacketCodecs.BOOL, FishingMiniGameOpenPayload::guaranteed,
                    PacketCodecs.INTEGER, FishingMiniGameOpenPayload::rarityRank,
                    FishingMiniGameOpenPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
