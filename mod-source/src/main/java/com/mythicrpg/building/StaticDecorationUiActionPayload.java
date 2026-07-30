package com.mythicrpg.building;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Client request to apply one of the 32 vanilla particles. */
public record StaticDecorationUiActionPayload(
        int handId,
        boolean editingBlock,
        long targetPosPacked,
        int effectIndex
) implements CustomPayload {
    public static final Id<StaticDecorationUiActionPayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "static_decoration_ui_action"));

    public static final PacketCodec<RegistryByteBuf, StaticDecorationUiActionPayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.handId());
                        buffer.writeBoolean(payload.editingBlock());
                        buffer.writeLong(payload.targetPosPacked());
                        buffer.writeVarInt(payload.effectIndex());
                    },
                    buffer -> new StaticDecorationUiActionPayload(
                            buffer.readVarInt(),
                            buffer.readBoolean(),
                            buffer.readLong(),
                            buffer.readVarInt()
                    )
            );

    public StaticDecorationUiActionPayload {
        if (handId < 0 || handId > 1) {
            throw new IllegalArgumentException("Invalid static decoration hand id: " + handId);
        }
        effectIndex = Math.floorMod(effectIndex, StaticDecorationEffect.values().length);
    }

    public BlockPos targetPos() {
        return BlockPos.fromLong(targetPosPacked);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
