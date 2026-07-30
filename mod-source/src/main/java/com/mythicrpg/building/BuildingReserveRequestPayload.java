package com.mythicrpg.building;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client request sent when the construction-reserve key is pressed. */
public record BuildingReserveRequestPayload() implements CustomPayload {
    public static final CustomPayload.Id<BuildingReserveRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of("mythicrpg", "building_reserve_request"));

    public static final PacketCodec<RegistryByteBuf, BuildingReserveRequestPayload> CODEC =
            PacketCodec.unit(new BuildingReserveRequestPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
