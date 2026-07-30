package com.mythicrpg.building;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Server-authoritative state used to open or refresh the Architect's Compass interface. */
public record ArchitectCompassUiStatePayload(
        int handId,
        boolean openScreen,
        boolean hasCenter,
        String dimensionId,
        long centerPacked,
        int radius,
        int axisId,
        String messageKey,
        boolean error
) implements CustomPayload {
    public static final Id<ArchitectCompassUiStatePayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "architect_compass_ui_state"));

    public static final PacketCodec<RegistryByteBuf, ArchitectCompassUiStatePayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.handId());
                        buffer.writeBoolean(payload.openScreen());
                        buffer.writeBoolean(payload.hasCenter());
                        buffer.writeString(payload.dimensionId(), 128);
                        buffer.writeLong(payload.centerPacked());
                        buffer.writeVarInt(payload.radius());
                        buffer.writeVarInt(payload.axisId());
                        buffer.writeString(payload.messageKey(), 256);
                        buffer.writeBoolean(payload.error());
                    },
                    buffer -> new ArchitectCompassUiStatePayload(
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readString(128),
                            buffer.readLong(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readString(256),
                            buffer.readBoolean()
                    )
            );

    public ArchitectCompassUiStatePayload {
        if (handId < 0 || handId > 1) {
            throw new IllegalArgumentException("Invalid Architect's Compass hand id: " + handId);
        }
        dimensionId = dimensionId == null ? "" : dimensionId;
        radius = Math.max(
                ArchitectCompassData.MIN_RADIUS,
                Math.min(ArchitectCompassData.MAX_RADIUS, radius)
        );
        axisId = Math.floorMod(axisId, 3);
        messageKey = messageKey == null ? "" : messageKey;
    }

    public BlockPos center() {
        return BlockPos.fromLong(centerPacked);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
