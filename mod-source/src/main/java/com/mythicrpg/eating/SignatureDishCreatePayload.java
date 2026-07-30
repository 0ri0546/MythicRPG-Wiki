package com.mythicrpg.eating;

import com.mythicrpg.MythicRPG;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SignatureDishCreatePayload(
        int handId,
        String name,
        int bonusId,
        String iconId,
        List<String> ingredientIds
) implements CustomPayload {
    public static final Id<SignatureDishCreatePayload> ID =
            new Id<>(Identifier.of(MythicRPG.MOD_ID, "signature_dish_create"));

    public static final PacketCodec<RegistryByteBuf, SignatureDishCreatePayload> CODEC = PacketCodec.ofStatic(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.handId());
                buffer.writeString(payload.name(), 32);
                buffer.writeVarInt(payload.bonusId());
                buffer.writeString(payload.iconId(), 128);
                buffer.writeVarInt(payload.ingredientIds().size());
                for (String ingredientId : payload.ingredientIds()) {
                    buffer.writeString(ingredientId, 128);
                }
            },
            buffer -> {
                int handId = buffer.readVarInt();
                String name = buffer.readString(32);
                int bonusId = buffer.readVarInt();
                String iconId = buffer.readString(128);
                int size = buffer.readVarInt();
                if (size < 0 || size > 5) {
                    throw new IllegalArgumentException("Invalid signature ingredient count: " + size);
                }
                ArrayList<String> ingredientIds = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    ingredientIds.add(buffer.readString(128));
                }
                return new SignatureDishCreatePayload(handId, name, bonusId, iconId, ingredientIds);
            }
    );

    public SignatureDishCreatePayload {
        handId = Math.floorMod(handId, 2);
        name = ChefNotebookData.sanitizeName(name);
        bonusId = Math.floorMod(bonusId, SignatureBonus.values().length);
        iconId = iconId == null ? "minecraft:bowl" : iconId;
        ingredientIds = List.copyOf(ingredientIds == null ? List.of() : ingredientIds);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
