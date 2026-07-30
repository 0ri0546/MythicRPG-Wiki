package com.mythicrpg.building;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** One compact, generic world-selection box shared by the Building tools. */
public record BuildingSelectionBoxPayload(
        int action,
        String toolId,
        String dimensionId,
        long firstPacked,
        long secondPacked,
        boolean hasSecond,
        boolean valid
) implements CustomPayload {
    public static final int SHOW = 0;
    public static final int CLEAR = 1;

    public static final Id<BuildingSelectionBoxPayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "building_selection_box"));

    public static final PacketCodec<RegistryByteBuf, BuildingSelectionBoxPayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.action());
                        buffer.writeString(payload.toolId(), 32);
                        buffer.writeString(payload.dimensionId(), 128);
                        buffer.writeLong(payload.firstPacked());
                        buffer.writeLong(payload.secondPacked());
                        buffer.writeBoolean(payload.hasSecond());
                        buffer.writeBoolean(payload.valid());
                    },
                    buffer -> new BuildingSelectionBoxPayload(
                            buffer.readVarInt(),
                            buffer.readString(32),
                            buffer.readString(128),
                            buffer.readLong(),
                            buffer.readLong(),
                            buffer.readBoolean(),
                            buffer.readBoolean()
                    )
            );

    public BuildingSelectionBoxPayload {
        if (action != SHOW && action != CLEAR) {
            throw new IllegalArgumentException("Invalid Building selection box action: " + action);
        }
        toolId = toolId == null ? "" : toolId;
        dimensionId = dimensionId == null ? "" : dimensionId;
        if (action == SHOW && BuildingUiTool.fromNetworkId(toolId).isEmpty()) {
            throw new IllegalArgumentException("Unknown Building UI tool: " + toolId);
        }
    }

    public static BuildingSelectionBoxPayload show(
            BuildingUiTool tool,
            String dimensionId,
            BlockPos first,
            BlockPos second,
            boolean valid
    ) {
        BlockPos safeFirst = first == null ? BlockPos.ORIGIN : first;
        BlockPos safeSecond = second == null ? safeFirst : second;
        return new BuildingSelectionBoxPayload(
                SHOW,
                tool.networkId(),
                dimensionId,
                safeFirst.asLong(),
                safeSecond.asLong(),
                second != null,
                valid
        );
    }

    public static BuildingSelectionBoxPayload clear() {
        return new BuildingSelectionBoxPayload(CLEAR, "", "", 0L, 0L, false, false);
    }

    public BlockPos first() {
        return BlockPos.fromLong(firstPacked);
    }

    public BlockPos second() {
        return BlockPos.fromLong(secondPacked);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
