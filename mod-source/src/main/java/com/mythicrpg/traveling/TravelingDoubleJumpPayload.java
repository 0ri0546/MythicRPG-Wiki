package com.mythicrpg.traveling;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TravelingDoubleJumpPayload() implements CustomPayload {
    public static final CustomPayload.Id<TravelingDoubleJumpPayload> ID =
            new CustomPayload.Id<>(Identifier.of("mythicrpg", "traveling_double_jump"));

    public static final PacketCodec<RegistryByteBuf, TravelingDoubleJumpPayload> CODEC =
            PacketCodec.unit(new TravelingDoubleJumpPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
