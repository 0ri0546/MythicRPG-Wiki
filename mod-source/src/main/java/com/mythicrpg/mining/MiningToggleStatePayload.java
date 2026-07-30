package com.mythicrpg.mining;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record MiningToggleStatePayload(boolean enabled) implements CustomPayload {
    public static final Id<MiningToggleStatePayload> ID =
            new Id<>(Identifier.of("mythicrpg", "mining_toggle_3x3_state"));
    public static final PacketCodec<RegistryByteBuf, MiningToggleStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, MiningToggleStatePayload::enabled,
            MiningToggleStatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
