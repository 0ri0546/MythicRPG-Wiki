package com.mythicrpg.building;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Client requests issued by Building structure configuration screens. */
public record BuildingPlanUiActionPayload(
        int toolId,
        int handId,
        int action,
        long firstPacked,
        long secondPacked,
        int normalAxisId,
        int rotationX,
        int rotationY,
        int rotationZ
) implements CustomPayload {
    public static final int SAVE_DRAFT = 0;
    public static final int COPY = 1;
    public static final int MINIATURIZE = COPY;
    public static final int SET_LOCKED_ROTATION = 2;

    public static final Id<BuildingPlanUiActionPayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "building_plan_ui_action"));

    public static final PacketCodec<RegistryByteBuf, BuildingPlanUiActionPayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.toolId());
                        buffer.writeVarInt(payload.handId());
                        buffer.writeVarInt(payload.action());
                        buffer.writeLong(payload.firstPacked());
                        buffer.writeLong(payload.secondPacked());
                        buffer.writeVarInt(payload.normalAxisId());
                        buffer.writeVarInt(payload.rotationX());
                        buffer.writeVarInt(payload.rotationY());
                        buffer.writeVarInt(payload.rotationZ());
                    },
                    buffer -> new BuildingPlanUiActionPayload(
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readLong(),
                            buffer.readLong(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readVarInt()
                    )
            );

    public BuildingPlanUiActionPayload {
        if (toolId != BuildingPlanUiStatePayload.TOOL_2D
                && toolId != BuildingPlanUiStatePayload.TOOL_3D
                && toolId != BuildingPlanUiStatePayload.TOOL_MINIATURE) {
            throw new IllegalArgumentException("Unknown Building plan UI tool: " + toolId);
        }
        if (handId < 0 || handId > 1) {
            throw new IllegalArgumentException("Invalid hand id: " + handId);
        }
        if (action < SAVE_DRAFT || action > SET_LOCKED_ROTATION) {
            throw new IllegalArgumentException("Unknown Building plan UI action: " + action);
        }
        normalAxisId = Math.floorMod(normalAxisId, 3);
        rotationX = Math.floorMod(rotationX, 4);
        rotationY = Math.floorMod(rotationY, 4);
        rotationZ = Math.floorMod(rotationZ, 4);
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
