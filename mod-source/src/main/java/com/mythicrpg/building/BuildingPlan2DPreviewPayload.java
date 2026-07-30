package com.mythicrpg.building;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Sends a bounded wireframe preview to the player holding the 2D plan. */
public record BuildingPlan2DPreviewPayload(
        int action,
        String dimensionId,
        boolean valid,
        List<Long> packedPositions
) implements CustomPayload {
    public static final int SHOW = 0;
    public static final int CLEAR = 1;
    private static final int MAX_POSITIONS = 144;

    public static final Id<BuildingPlan2DPreviewPayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "building_plan_2d_preview"));

    public static final PacketCodec<RegistryByteBuf, BuildingPlan2DPreviewPayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.action());
                        buffer.writeString(payload.dimensionId());
                        buffer.writeBoolean(payload.valid());
                        int size = Math.min(MAX_POSITIONS, payload.packedPositions().size());
                        buffer.writeVarInt(size);
                        for (int index = 0; index < size; index++) {
                            buffer.writeLong(payload.packedPositions().get(index));
                        }
                    },
                    buffer -> {
                        int action = buffer.readVarInt();
                        String dimensionId = buffer.readString();
                        boolean valid = buffer.readBoolean();
                        int size = buffer.readVarInt();
                        if (size < 0 || size > MAX_POSITIONS) {
                            throw new IllegalArgumentException("Invalid 2D plan preview size: " + size);
                        }
                        List<Long> positions = new ArrayList<>(size);
                        for (int index = 0; index < size; index++) {
                            positions.add(buffer.readLong());
                        }
                        return new BuildingPlan2DPreviewPayload(
                                action,
                                dimensionId,
                                valid,
                                List.copyOf(positions)
                        );
                    }
            );

    public BuildingPlan2DPreviewPayload {
        if (action != SHOW && action != CLEAR) {
            throw new IllegalArgumentException("Invalid 2D plan preview action: " + action);
        }
        if (packedPositions.size() > MAX_POSITIONS) {
            throw new IllegalArgumentException("Too many 2D plan preview positions");
        }
        dimensionId = dimensionId == null ? "" : dimensionId;
        packedPositions = List.copyOf(packedPositions);
    }

    public static BuildingPlan2DPreviewPayload show(
            String dimensionId,
            boolean valid,
            List<BlockPos> positions
    ) {
        return new BuildingPlan2DPreviewPayload(
                SHOW,
                dimensionId,
                valid,
                positions.stream().limit(MAX_POSITIONS).map(BlockPos::asLong).toList()
        );
    }

    public static BuildingPlan2DPreviewPayload clear() {
        return new BuildingPlan2DPreviewPayload(CLEAR, "", false, List.of());
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
