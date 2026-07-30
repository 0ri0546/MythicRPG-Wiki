package com.mythicrpg.crafting;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenMythicCraftingPayload(boolean requested) implements CustomPayload {
    public static final CustomPayload.Id<OpenMythicCraftingPayload> ID =
            new CustomPayload.Id<>(Identifier.of("mythicrpg", "open_mythic_crafting"));

    public static final PacketCodec<RegistryByteBuf, OpenMythicCraftingPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL, OpenMythicCraftingPayload::requested,
            OpenMythicCraftingPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
