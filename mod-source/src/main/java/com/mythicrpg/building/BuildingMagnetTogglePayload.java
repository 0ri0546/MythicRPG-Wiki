package com.mythicrpg.building;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BuildingMagnetTogglePayload(boolean enabled) implements CustomPayload {
    public static final Id<BuildingMagnetTogglePayload> ID =
            new Id<>(Identifier.of("mythicrpg", "building_magnet_toggle"));

    public static final PacketCodec<RegistryByteBuf, BuildingMagnetTogglePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, BuildingMagnetTogglePayload::enabled,
            BuildingMagnetTogglePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
