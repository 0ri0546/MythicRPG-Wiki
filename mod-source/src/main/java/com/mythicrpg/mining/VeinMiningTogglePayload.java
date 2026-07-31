package com.mythicrpg.mining;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VeinMiningTogglePayload(boolean enabled)
        implements CustomPayload {

    public static final Id<VeinMiningTogglePayload> ID =
            new Id<>(Identifier.of(
                    "mythicrpg",
                    "vein_mining_toggle"
            ));

    public static final PacketCodec<
            RegistryByteBuf,
            VeinMiningTogglePayload
            > CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL,
            VeinMiningTogglePayload::enabled,
            VeinMiningTogglePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}