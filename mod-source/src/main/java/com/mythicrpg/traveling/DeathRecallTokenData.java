package com.mythicrpg.traveling;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public final class DeathRecallTokenData {

    private static final String OWNER_KEY = "DeathRecallOwner";
    private static final String RECALL_ID_KEY = "DeathRecallId";
    private static final String DIMENSION_KEY = "DeathRecallDimension";
    private static final String X_KEY = "DeathRecallX";
    private static final String Y_KEY = "DeathRecallY";
    private static final String Z_KEY = "DeathRecallZ";
    private static final String EXPIRES_AT_KEY = "DeathRecallExpiresAt";

    private DeathRecallTokenData() {
    }

    public static void write(
            ItemStack stack,
            UUID owner,
            UUID recallId,
            RegistryKey<World> dimension,
            BlockPos deathPos,
            long expiresAtMillis
    ) {
        NbtCompound nbt = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        ).copyNbt();

        nbt.putString(OWNER_KEY, owner.toString());
        nbt.putString(RECALL_ID_KEY, recallId.toString());
        nbt.putString(DIMENSION_KEY, dimension.getValue().toString());
        nbt.putInt(X_KEY, deathPos.getX());
        nbt.putInt(Y_KEY, deathPos.getY());
        nbt.putInt(Z_KEY, deathPos.getZ());
        nbt.putLong(EXPIRES_AT_KEY, expiresAtMillis);

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static Optional<Data> read(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        ).copyNbt();

        if (!nbt.contains(OWNER_KEY)
                || !nbt.contains(RECALL_ID_KEY)
                || !nbt.contains(DIMENSION_KEY)
                || !nbt.contains(EXPIRES_AT_KEY)) {
            return Optional.empty();
        }

        try {
            UUID owner = UUID.fromString(nbt.getString(OWNER_KEY));
            UUID recallId = UUID.fromString(nbt.getString(RECALL_ID_KEY));
            Identifier dimensionId = Identifier.of(nbt.getString(DIMENSION_KEY));
            RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
            BlockPos deathPos = new BlockPos(
                    nbt.getInt(X_KEY),
                    nbt.getInt(Y_KEY),
                    nbt.getInt(Z_KEY)
            );
            long expiresAt = nbt.getLong(EXPIRES_AT_KEY);

            return Optional.of(new Data(owner, recallId, dimension, deathPos, expiresAt));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static boolean isExpired(ItemStack stack) {
        return read(stack)
                .map(data -> data.expiresAtMillis() <= System.currentTimeMillis())
                .orElse(true);
    }

    public record Data(
            UUID owner,
            UUID recallId,
            RegistryKey<World> dimension,
            BlockPos deathPos,
            long expiresAtMillis
    ) {
        public long remainingSeconds() {
            return Math.max(0L, (expiresAtMillis - System.currentTimeMillis() + 999L) / 1000L);
        }
    }
}
