package com.mythicrpg.traveling;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenTravelingCompassPayload() implements CustomPayload {

    public static final CustomPayload.Id<OpenTravelingCompassPayload> ID =
            new CustomPayload.Id<>(Identifier.of("mythicrpg", "open_traveling_compass"));

    public static final PacketCodec<RegistryByteBuf, OpenTravelingCompassPayload> CODEC =
            PacketCodec.unit(new OpenTravelingCompassPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
