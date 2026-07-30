package com.mythicrpg.building;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Bounded client wireframe preview for the 8x8x8 Building Plan. */
public record BuildingPlan3DPreviewPayload(
        int action,
        String dimensionId,
        boolean valid,
        List<Long> packedPositions
) implements CustomPayload {
    public static final int SHOW = 0;
    public static final int CLEAR = 1;
    private static final int MAX_POSITIONS = 512;

    public static final Id<BuildingPlan3DPreviewPayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "building_plan_3d_preview"));

    public static final PacketCodec<RegistryByteBuf, BuildingPlan3DPreviewPayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.action());
                        buffer.writeString(payload.dimensionId());
                        buffer.writeBoolean(payload.valid());
                        int size = Math.min(MAX_POSITIONS, payload.packedPositions().size());
                        buffer.writeVarInt(size);
                        for (int i = 0; i < size; i++) buffer.writeLong(payload.packedPositions().get(i));
                    },
                    buffer -> {
                        int action = buffer.readVarInt();
                        String dimensionId = buffer.readString();
                        boolean valid = buffer.readBoolean();
                        int size = buffer.readVarInt();
                        if (size < 0 || size > MAX_POSITIONS) {
                            throw new IllegalArgumentException("Invalid 3D plan preview size: " + size);
                        }
                        List<Long> positions = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) positions.add(buffer.readLong());
                        return new BuildingPlan3DPreviewPayload(action, dimensionId, valid, List.copyOf(positions));
                    }
            );

    public BuildingPlan3DPreviewPayload {
        if (action != SHOW && action != CLEAR) throw new IllegalArgumentException("Invalid action: " + action);
        if (packedPositions.size() > MAX_POSITIONS) throw new IllegalArgumentException("Too many preview positions");
        dimensionId = dimensionId == null ? "" : dimensionId;
        packedPositions = List.copyOf(packedPositions);
    }

    public static BuildingPlan3DPreviewPayload show(String dimensionId, boolean valid, List<BlockPos> positions) {
        return new BuildingPlan3DPreviewPayload(
                SHOW, dimensionId, valid,
                positions.stream().limit(MAX_POSITIONS).map(BlockPos::asLong).toList()
        );
    }

    public static BuildingPlan3DPreviewPayload clear() {
        return new BuildingPlan3DPreviewPayload(CLEAR, "", false, List.of());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
