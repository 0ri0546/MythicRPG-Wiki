package com.mythicrpg.eating;

import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.PlayerCooldownManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

public final class EatingDeliveryManager {
    private static final int RANGE = 32;
    private static final double RANGE_SQUARED = RANGE * RANGE;
    private static final int DELIVERY_COOLDOWN_TICKS = 10;
    private static final Map<ServerWorld, Map<Long, Map<Long, CookingPotBlockEntity>>> POTS = new IdentityHashMap<>();
    private static final Map<ServerWorld, Map<Long, Map<Long, FridgeBlockEntity>>> FRIDGES = new IdentityHashMap<>();
    /** Server-thread-only reverse index for prepared signature pots. */
    private static final Map<UUID, Set<CookingPotBlockEntity>> PREPARED_SIGNATURE_POTS = new HashMap<>();

    private EatingDeliveryManager() {
    }

    public static void register() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
            if (blockEntity instanceof CookingPotBlockEntity pot) {
                track(pot);
            } else if (blockEntity instanceof FridgeBlockEntity fridge) {
                track(fridge);
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
            if (blockEntity instanceof CookingPotBlockEntity pot) {
                untrackPreparedSignature(pot);
                untrack(POTS, world, blockEntity);
            } else if (blockEntity instanceof FridgeBlockEntity) {
                untrack(FRIDGES, world, blockEntity);
            }
        });
    }

    public static void track(CookingPotBlockEntity pot) {
        if (pot.getWorld() instanceof ServerWorld world) {
            track(POTS, world, pot);
            refreshPreparedSignatureIndex(pot);
        }
    }

    public static void track(FridgeBlockEntity fridge) {
        if (fridge.getWorld() instanceof ServerWorld world) {
            track(FRIDGES, world, fridge);
        }
    }

    private static <T extends BlockEntity> void track(
            Map<ServerWorld, Map<Long, Map<Long, T>>> index,
            ServerWorld world,
            T entity
    ) {
        long chunkKey = new ChunkPos(entity.getPos()).toLong();
        index.computeIfAbsent(world, ignored -> new HashMap<>())
                .computeIfAbsent(chunkKey, ignored -> new HashMap<>())
                .put(entity.getPos().asLong(), entity);
    }

    private static <T extends BlockEntity> void untrack(
            Map<ServerWorld, Map<Long, Map<Long, T>>> index,
            ServerWorld world,
            BlockEntity entity
    ) {
        Map<Long, Map<Long, T>> byChunk = index.get(world);
        if (byChunk == null) {
            return;
        }
        long chunkKey = new ChunkPos(entity.getPos()).toLong();
        Map<Long, T> chunkEntries = byChunk.get(chunkKey);
        if (chunkEntries != null && chunkEntries.get(entity.getPos().asLong()) == entity) {
            chunkEntries.remove(entity.getPos().asLong());
            if (chunkEntries.isEmpty()) {
                byChunk.remove(chunkKey);
            }
        }
        if (byChunk.isEmpty()) {
            index.remove(world);
        }
    }

    public static void handle(ServerPlayerEntity player, DeliveryPhoneActionPayload payload) {
        Hand hand = payload.handId() == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        ItemStack phone = player.getStackInHand(hand);
        if (!phone.isOf(ModItems.DELIVERY_PHONE) || !EatingPerks.hasDelivery(player)) {
            return;
        }
        if (!PlayerCooldownManager.tryUse(player, "eating_delivery_action", DELIVERY_COOLDOWN_TICKS)) {
            return;
        }

        DeliverySource source = DeliverySource.byOrdinal(payload.sourceId());
        int requested = Math.max(1, Math.min(9, payload.count()));
        DeliveryPhoneData.Settings current = DeliveryPhoneData.read(phone);
        if (current.source() != source || current.count() != requested) {
            DeliveryPhoneData.write(phone, source, requested);
            player.getInventory().markDirty();
        }

        int delivered = source == DeliverySource.COOKING_POT
                ? deliverFromPots(player, requested)
                : deliverFromFridges(player, requested);

        if (delivered <= 0) {
            player.sendMessage(Text.translatable("message.mythicrpg.eating.delivery_none")
                    .formatted(Formatting.RED), true);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(),
                    SoundCategory.PLAYERS, 0.5F, 0.7F);
            return;
        }

        player.sendMessage(Text.translatable("message.mythicrpg.eating.delivery_success", delivered)
                .formatted(Formatting.GREEN), true);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.PLAYERS, 0.7F, 1.25F);
    }

    private static int deliverFromPots(ServerPlayerEntity player, int requested) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return 0;
        }
        int delivered = 0;
        for (CookingPotBlockEntity pot : nearby(POTS, world, player.getBlockPos())) {
            if (delivered >= requested) {
                break;
            }
            delivered += pot.deliverTo(player, requested - delivered);
        }
        return delivered;
    }

    private static int deliverFromFridges(ServerPlayerEntity player, int requested) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return 0;
        }
        int delivered = 0;
        for (FridgeBlockEntity fridge : nearby(FRIDGES, world, player.getBlockPos())) {
            if (delivered >= requested) {
                break;
            }
            delivered += fridge.deliverTo(player, requested - delivered);
        }
        return delivered;
    }

    private static <T extends BlockEntity> List<T> nearby(
            Map<ServerWorld, Map<Long, Map<Long, T>>> index,
            ServerWorld world,
            BlockPos playerPos
    ) {
        Map<Long, Map<Long, T>> byChunk = index.get(world);
        if (byChunk == null || byChunk.isEmpty()) {
            return List.of();
        }
        ChunkPos center = new ChunkPos(playerPos);
        int chunkRadius = (RANGE + 15) / 16;
        ArrayList<T> result = new ArrayList<>();
        for (int chunkX = center.x - chunkRadius; chunkX <= center.x + chunkRadius; chunkX++) {
            for (int chunkZ = center.z - chunkRadius; chunkZ <= center.z + chunkRadius; chunkZ++) {
                long chunkKey = ChunkPos.toLong(chunkX, chunkZ);
                Map<Long, T> entries = byChunk.get(chunkKey);
                if (entries == null) {
                    continue;
                }
                entries.entrySet().removeIf(entry -> !isStillLoaded(world, entry.getValue()));
                for (T entity : entries.values()) {
                    if (entity.getPos().getSquaredDistance(playerPos) <= RANGE_SQUARED) {
                        result.add(entity);
                    }
                }
                if (entries.isEmpty()) {
                    byChunk.remove(chunkKey);
                }
            }
        }
        if (byChunk.isEmpty()) {
            index.remove(world);
        }
        result.sort(Comparator.comparingDouble(entity -> entity.getPos().getSquaredDistance(playerPos)));
        return result;
    }

    private static boolean isStillLoaded(ServerWorld world, BlockEntity entity) {
        return !entity.isRemoved()
                && entity.getWorld() == world
                && world.isChunkLoaded(entity.getPos())
                && world.getBlockEntity(entity.getPos()) == entity;
    }

    public static void refreshPreparedSignatureIndex(CookingPotBlockEntity pot) {
        untrackPreparedSignature(pot);
        UUID owner = pot.getPreparedSignatureOwner();
        if (owner != null && pot.isSignaturePrepared()) {
            PREPARED_SIGNATURE_POTS
                    .computeIfAbsent(owner, ignored -> new HashSet<>())
                    .add(pot);
        }
    }

    public static void untrackPreparedSignature(CookingPotBlockEntity pot) {
        var iterator = PREPARED_SIGNATURE_POTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Set<CookingPotBlockEntity>> entry = iterator.next();
            entry.getValue().remove(pot);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    public static void invalidatePreparedSignatures(UUID playerUuid) {
        Set<CookingPotBlockEntity> indexed = PREPARED_SIGNATURE_POTS.remove(playerUuid);
        if (indexed == null || indexed.isEmpty()) {
            return;
        }
        for (CookingPotBlockEntity pot : Set.copyOf(indexed)) {
            if (pot.getWorld() instanceof ServerWorld world
                    && isStillLoaded(world, pot)) {
                pot.invalidatePreparedSignatureOwnedBy(playerUuid);
            }
        }
    }

    public static void clear() {
        POTS.clear();
        FRIDGES.clear();
        PREPARED_SIGNATURE_POTS.clear();
    }
}
