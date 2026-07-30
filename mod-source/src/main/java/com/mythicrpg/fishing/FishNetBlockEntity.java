
package com.mythicrpg.fishing;

import com.mythicrpg.core.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public final class FishNetBlockEntity extends LockableContainerBlockEntity {
    public static final int INVENTORY_SIZE = 5;

    private DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private UUID owner;
    private long nextCatch;

    public FishNetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FISH_NET, pos, state);
    }

    public boolean claim(ServerPlayerEntity player) {
        if (owner != null && !owner.equals(player.getUuid())) {
            return false;
        }
        if (!FishingNetManager.claim(player, pos)) {
            return false;
        }
        owner = player.getUuid();
        nextCatch = player.getServerWorld().getTime()
                + FishingBalance.passiveCaptureIntervalTicks(false);
        markDirty();
        return true;
    }

    public UUID owner() {
        return owner;
    }

    public boolean isOwner(PlayerEntity player) {
        return owner != null && owner.equals(player.getUuid());
    }

    public static void tick(
            World world,
            BlockPos pos,
            BlockState state,
            FishNetBlockEntity entity
    ) {
        if (!(world instanceof ServerWorld serverWorld)
                || !serverWorld.getRegistryKey().equals(World.OVERWORLD)
                || entity.owner == null) {
            return;
        }
        if (!FishingNetManager.ensureActive(entity.owner, serverWorld, pos)) {
            return;
        }
        if (serverWorld.getTime() < entity.nextCatch) {
            return;
        }

        entity.nextCatch = serverWorld.getTime()
                + FishingBalance.passiveCaptureIntervalTicks(false);
        entity.markDirty();

        ServerPlayerEntity player = serverWorld.getServer()
                .getPlayerManager()
                .getPlayer(entity.owner);
        if (player == null
                || player.getServerWorld() != serverWorld
                || player.squaredDistanceTo(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5
                ) > 128.0D * 128.0D) {
            return;
        }

        int capacity = FishingManager.netCapacity(player);
        if (capacity <= 0 || !state.get(FishNetBlock.WATERLOGGED)) {
            return;
        }

        for (int slot = 0; slot < Math.min(capacity, INVENTORY_SIZE); slot++) {
            if (!entity.items.get(slot).isEmpty()) {
                continue;
            }

            FishingFamily family = FishingFamily.select(serverWorld, pos, player.getRandom());
            FishingRarity rarity = player.getRandom().nextInt(100) < 75
                    ? FishingRarity.COMMON
                    : FishingRarity.RARE;
            entity.items.set(slot, FishingManager.createCatch(
                    family,
                    rarity,
                    serverWorld.getBiome(pos)
                            .getKey()
                            .map(key -> key.getValue().toString())
                            .orElse(""),
                    serverWorld.getRegistryKey().getValue().toString(),
                    "net"
            ));
            entity.markDirty();
            return;
        }
    }

    public int visibleCapacity(int unlockedCapacity) {
        int highestOccupied = 0;
        for (int slot = 0; slot < INVENTORY_SIZE; slot++) {
            if (!items.get(slot).isEmpty()) {
                highestOccupied = slot + 1;
            }
        }
        return Math.max(
                Math.max(0, Math.min(INVENTORY_SIZE, unlockedCapacity)),
                highestOccupied
        );
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.mythicrpg.fish_net");
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return items;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> stacks) {
        items = stacks;
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory inventory) {
        int unlockedCapacity = inventory.player instanceof ServerPlayerEntity serverPlayer
                ? FishingManager.netCapacity(serverPlayer)
                : 0;
        return new FishNetScreenHandler(
                syncId,
                inventory,
                this,
                visibleCapacity(unlockedCapacity)
        );
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null
                && world.getBlockEntity(pos) == this
                && isOwner(player)
                && player.squaredDistanceTo(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5
                ) <= 64.0D;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        Inventories.writeNbt(nbt, items, lookup);
        if (owner != null) {
            nbt.putUuid("owner", owner);
        }
        nbt.putLong("next_catch", nextCatch);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items, lookup);
        owner = nbt.containsUuid("owner") ? nbt.getUuid("owner") : null;
        nextCatch = Math.max(0L, nbt.getLong("next_catch"));
    }
}
