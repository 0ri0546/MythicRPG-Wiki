
package com.mythicrpg.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FishingMiniGameActionPayload(int action, int value) implements CustomPayload {
    public static final Id<FishingMiniGameActionPayload> ID =
            new Id<>(Identifier.of("mythicrpg", "fishing_minigame_action"));

    public static final PacketCodec<RegistryByteBuf, FishingMiniGameActionPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER,
                    FishingMiniGameActionPayload::action,
                    PacketCodecs.INTEGER,
                    FishingMiniGameActionPayload::value,
                    FishingMiniGameActionPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
