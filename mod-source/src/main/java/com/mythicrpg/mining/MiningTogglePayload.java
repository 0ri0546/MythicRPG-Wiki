package com.mythicrpg.mining;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MiningTogglePayload(boolean enabled) implements CustomPayload {
    public static final CustomPayload.Id<MiningTogglePayload> ID =
            new CustomPayload.Id<>(Identifier.of("mythicrpg", "mining_toggle_3x3"));

    public static final PacketCodec<RegistryByteBuf, MiningTogglePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, MiningTogglePayload::enabled,
            MiningTogglePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}