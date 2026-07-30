package com.mythicrpg.eating;

import com.mythicrpg.core.ModBlockEntities;
import com.mythicrpg.titles.TitleManager;
import com.mythicrpg.titles.TitleRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FridgeBlockEntity extends LockableContainerBlockEntity implements SidedInventory {
    public static final int INVENTORY_SIZE = 54;
    private static final int[] ALL_SLOTS = createSlots();

    private DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final Set<UUID> activeViewers = new HashSet<>();
    private boolean lastPowered;
    private boolean preservationDirty = true;
    private boolean processingPreservation;
    private UUID ownerUuid;

    public FridgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRIDGE, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, FridgeBlockEntity fridge) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        boolean safetyTick = Math.floorMod(world.getTime() + pos.asLong(), 20L) == 0L;
        boolean powered = world.isReceivingRedstonePower(pos);
        if (fridge.preservationDirty || powered != fridge.lastPowered || safetyTick) {
            fridge.synchronizePreservation(serverWorld, powered);
        }
        if (safetyTick && !fridge.activeViewers.isEmpty()) {
            fridge.grantPsychopathTitle(serverWorld);
        }
    }

    public void claimOwner(ServerPlayerEntity player) {
        if (ownerUuid == null) {
            ownerUuid = player.getUuid();
            markSaved();
        }
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public int deliverTo(ServerPlayerEntity player, int maximum) {
        if (maximum <= 0 || ownerUuid == null || !ownerUuid.equals(player.getUuid()) || world == null) {
            return 0;
        }
        int delivered = 0;
        for (int slot = 0; slot < items.size() && delivered < maximum; slot++) {
            ItemStack stored = items.get(slot);
            if (PreparedDishData.read(stored).isEmpty()) {
                continue;
            }
            ItemStack delivery = stored.copyWithCount(1);
            EatingPreservationManager.updateStack(
                    delivery,
                    world.getTime(),
                    EatingPreservationManager.PreservationMode.NONE
            );
            PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(delivery, world.getTime());
            if (dish.dubious()) {
                continue;
            }
            if (!player.getInventory().insertStack(delivery) || !delivery.isEmpty()) {
                break;
            }
            stored.decrement(1);
            if (stored.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
            delivered++;
        }
        if (delivered > 0) {
            preservationDirty = true;
            markDirty();
            player.getInventory().markDirty();
        }
        return delivered;
    }

    public boolean isPowered() {
        return world != null && world.isReceivingRedstonePower(pos);
    }

    public void releasePreservation() {
        if (world == null) {
            return;
        }
        processingPreservation = true;
        boolean changed = false;
        long gameTime = world.getTime();
        for (ItemStack stack : items) {
            changed |= EatingPreservationManager.updateStack(
                    stack,
                    gameTime,
                    EatingPreservationManager.PreservationMode.NONE
            );
        }
        processingPreservation = false;
        lastPowered = false;
        preservationDirty = false;
        if (changed) {
            markSaved();
        }
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.mythicrpg.fridge");
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return items;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> stacks) {
        items = stacks;
        preservationDirty = true;
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, this);
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null
                && world.getBlockEntity(pos) == this
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return EatingFoodStorage.isFridgeAccepted(stack);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        if (isPowered()) {
            return false;
        }
        if (world != null && EatingPreservationManager.updateStack(
                stack,
                world.getTime(),
                EatingPreservationManager.PreservationMode.NONE
        )) {
            markSaved();
        }
        return true;
    }

    @Override
    public void onOpen(PlayerEntity player) {
        super.onOpen(player);
        if (player instanceof ServerPlayerEntity serverPlayer) {
            claimOwner(serverPlayer);
        }
        activeViewers.add(player.getUuid());
        if (world instanceof ServerWorld serverWorld) {
            grantPsychopathTitle(serverWorld);
        }
    }

    @Override
    public void onClose(PlayerEntity player) {
        super.onClose(player);
        activeViewers.remove(player.getUuid());
        if (player instanceof ServerPlayerEntity serverPlayer
                && EatingPreservationManager.refreshPlayerStorage(serverPlayer)) {
            serverPlayer.getInventory().markDirty();
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (processingPreservation) {
            return;
        }

        preservationDirty = true;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items, registryLookup);
        lastPowered = nbt.getBoolean("LastPowered");
        ownerUuid = null;
        if (!nbt.getString("OwnerUuid").isBlank()) {
            try {
                ownerUuid = UUID.fromString(nbt.getString("OwnerUuid"));
            } catch (IllegalArgumentException ignored) {
                ownerUuid = null;
            }
        }
        preservationDirty = true;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, items, registryLookup);
        nbt.putBoolean("LastPowered", lastPowered);
        nbt.putString("OwnerUuid", ownerUuid == null ? "" : ownerUuid.toString());
    }

    private void synchronizePreservation(ServerWorld world, boolean powered) {
        processingPreservation = true;
        boolean changed = false;
        long gameTime = world.getTime();
        for (ItemStack stack : items) {
            changed |= EatingPreservationManager.updateStack(
                    stack,
                    gameTime,
                    powered
                            ? EatingPreservationManager.PreservationMode.FRIDGE
                            : EatingPreservationManager.PreservationMode.NONE
            );
        }
        processingPreservation = false;
        lastPowered = powered;
        preservationDirty = false;
        if (changed) {
            markSaved();
        }
    }

    private void grantPsychopathTitle(ServerWorld world) {
        if (activeViewers.isEmpty() || items.stream().noneMatch(stack -> stack.isOf(Items.PLAYER_HEAD))) {
            return;
        }
        for (UUID viewer : Set.copyOf(activeViewers)) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(viewer);
            if (player == null
                    || player.getWorld() != world
                    || !canPlayerUse(player)
                    || !(player.currentScreenHandler instanceof GenericContainerScreenHandler handler)
                    || handler.getSlot(0).inventory != this) {
                activeViewers.remove(viewer);
                continue;
            }
            TitleManager.grantSpecialTitle(player, TitleRegistry.PSYCHOPATH_ID, true);
        }
    }

    private void markSaved() {
        super.markDirty();
    }

    private static int[] createSlots() {
        int[] slots = new int[INVENTORY_SIZE];
        for (int index = 0; index < slots.length; index++) {
            slots[index] = index;
        }
        return slots;
    }
}
