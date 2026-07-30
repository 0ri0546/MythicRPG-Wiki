package com.mythicrpg.building;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Server-authoritative state used to open or refresh Building structure screens. */
public record BuildingPlanUiStatePayload(
        int toolId,
        int handId,
        boolean openScreen,
        boolean locked,
        String dimensionId,
        long firstPacked,
        long secondPacked,
        boolean hasFirst,
        boolean hasSecond,
        int normalAxisId,
        int rotationX,
        int rotationY,
        int rotationZ,
        int maxSize,
        String messageKey,
        boolean error
) implements CustomPayload {
    public static final int TOOL_2D = 0;
    public static final int TOOL_3D = 1;
    public static final int TOOL_MINIATURE = 2;

    public static final Id<BuildingPlanUiStatePayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "building_plan_ui_state"));

    public static final PacketCodec<RegistryByteBuf, BuildingPlanUiStatePayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.toolId());
                        buffer.writeVarInt(payload.handId());
                        buffer.writeBoolean(payload.openScreen());
                        buffer.writeBoolean(payload.locked());
                        buffer.writeString(payload.dimensionId(), 128);
                        buffer.writeLong(payload.firstPacked());
                        buffer.writeLong(payload.secondPacked());
                        buffer.writeBoolean(payload.hasFirst());
                        buffer.writeBoolean(payload.hasSecond());
                        buffer.writeVarInt(payload.normalAxisId());
                        buffer.writeVarInt(payload.rotationX());
                        buffer.writeVarInt(payload.rotationY());
                        buffer.writeVarInt(payload.rotationZ());
                        buffer.writeVarInt(payload.maxSize());
                        buffer.writeString(payload.messageKey(), 256);
                        buffer.writeBoolean(payload.error());
                    },
                    buffer -> new BuildingPlanUiStatePayload(
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readString(128),
                            buffer.readLong(),
                            buffer.readLong(),
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readString(256),
                            buffer.readBoolean()
                    )
            );

    public BuildingPlanUiStatePayload {
        if (toolId != TOOL_2D && toolId != TOOL_3D && toolId != TOOL_MINIATURE) {
            throw new IllegalArgumentException("Unknown Building plan UI tool: " + toolId);
        }
        if (handId < 0 || handId > 1) {
            throw new IllegalArgumentException("Invalid hand id: " + handId);
        }
        normalAxisId = Math.floorMod(normalAxisId, 3);
        rotationX = Math.floorMod(rotationX, 4);
        rotationY = Math.floorMod(rotationY, 4);
        rotationZ = Math.floorMod(rotationZ, 4);
        maxSize = Math.max(0, Math.min(12, maxSize));
        dimensionId = dimensionId == null ? "" : dimensionId;
        messageKey = messageKey == null ? "" : messageKey;
    }

    public BlockPos first() {
        return BlockPos.fromLong(firstPacked);
    }

    public BlockPos second() {
        return BlockPos.fromLong(secondPacked);
    }

    public BuildingStructureRotation rotation() {
        return new BuildingStructureRotation(rotationX, rotationY, rotationZ);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
