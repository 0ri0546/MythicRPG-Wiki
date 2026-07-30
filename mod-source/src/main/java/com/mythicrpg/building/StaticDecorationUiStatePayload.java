package com.mythicrpg.building;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Server-authoritative state for the vanilla particle generator screen. */
public record StaticDecorationUiStatePayload(
        int handId,
        boolean openScreen,
        boolean editingBlock,
        String dimensionId,
        long targetPosPacked,
        int effectIndex,
        String messageKey,
        boolean error
) implements CustomPayload {
    public static final Id<StaticDecorationUiStatePayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "static_decoration_ui_state"));

    public static final PacketCodec<RegistryByteBuf, StaticDecorationUiStatePayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.handId());
                        buffer.writeBoolean(payload.openScreen());
                        buffer.writeBoolean(payload.editingBlock());
                        buffer.writeString(payload.dimensionId(), 128);
                        buffer.writeLong(payload.targetPosPacked());
                        buffer.writeVarInt(payload.effectIndex());
                        buffer.writeString(payload.messageKey(), 256);
                        buffer.writeBoolean(payload.error());
                    },
                    buffer -> new StaticDecorationUiStatePayload(
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            buffer.readBoolean(),
                            buffer.readString(128),
                            buffer.readLong(),
                            buffer.readVarInt(),
                            buffer.readString(256),
                            buffer.readBoolean()
                    )
            );

    public StaticDecorationUiStatePayload {
        if (handId < 0 || handId > 1) {
            throw new IllegalArgumentException("Invalid static decoration hand id: " + handId);
        }
        dimensionId = dimensionId == null ? "" : dimensionId;
        effectIndex = Math.floorMod(effectIndex, StaticDecorationEffect.values().length);
        messageKey = messageKey == null ? "" : messageKey;
    }

    public BlockPos targetPos() {
        return BlockPos.fromLong(targetPosPacked);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
