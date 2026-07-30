package com.mythicrpg.eating;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DeliveryPhoneOpenPayload(int handId, int sourceId, int count) implements CustomPayload {
    public static final Id<DeliveryPhoneOpenPayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "delivery_phone_open"));

    public static final PacketCodec<RegistryByteBuf, DeliveryPhoneOpenPayload> CODEC = PacketCodec.ofStatic(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.handId());
                buffer.writeVarInt(payload.sourceId());
                buffer.writeVarInt(payload.count());
            },
            buffer -> new DeliveryPhoneOpenPayload(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            )
    );

    public DeliveryPhoneOpenPayload {
        handId = Math.floorMod(handId, 2);
        sourceId = Math.floorMod(sourceId, DeliverySource.values().length);
        count = Math.max(1, Math.min(9, count));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
