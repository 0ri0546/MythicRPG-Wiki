package com.mythicrpg.eating;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DeliveryPhoneActionPayload(int handId, int sourceId, int count) implements CustomPayload {
    public static final Id<DeliveryPhoneActionPayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "delivery_phone_action"));

    public static final PacketCodec<RegistryByteBuf, DeliveryPhoneActionPayload> CODEC = PacketCodec.ofStatic(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.handId());
                buffer.writeVarInt(payload.sourceId());
                buffer.writeVarInt(payload.count());
            },
            buffer -> new DeliveryPhoneActionPayload(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            )
    );

    public DeliveryPhoneActionPayload {
        handId = Math.floorMod(handId, 2);
        sourceId = Math.floorMod(sourceId, DeliverySource.values().length);
        count = Math.max(1, Math.min(9, count));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
