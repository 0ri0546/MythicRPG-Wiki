package com.mythicrpg.building;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Client request used to validate and save the Architect's Compass interface. */
public record ArchitectCompassUiActionPayload(
        int handId,
        int centerX,
        int centerY,
        int centerZ,
        int radius,
        int axisId
) implements CustomPayload {
    public static final Id<ArchitectCompassUiActionPayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "architect_compass_ui_action"));

    public static final PacketCodec<RegistryByteBuf, ArchitectCompassUiActionPayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.handId());
                        buffer.writeInt(payload.centerX());
                        buffer.writeInt(payload.centerY());
                        buffer.writeInt(payload.centerZ());
                        buffer.writeVarInt(payload.radius());
                        buffer.writeVarInt(payload.axisId());
                    },
                    buffer -> new ArchitectCompassUiActionPayload(
                            buffer.readVarInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt()
                    )
            );

    public ArchitectCompassUiActionPayload {
        if (handId < 0 || handId > 1) {
            throw new IllegalArgumentException("Invalid Architect's Compass hand id: " + handId);
        }
        radius = Math.max(
                ArchitectCompassData.MIN_RADIUS,
                Math.min(ArchitectCompassData.MAX_RADIUS, radius)
        );
        axisId = Math.floorMod(axisId, 3);
    }

    public BlockPos center() {
        return new BlockPos(centerX, centerY, centerZ);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
