package com.mythicrpg.building;

import com.mythicrpg.core.ModEntities;
import com.mythicrpg.core.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/** One static entity for an entire miniature; its bounded structure travels in one tracked ItemStack. */
public final class BuildingMiniatureEntity extends Entity {
    private static final TrackedData<ItemStack> MINIATURE_STACK = DataTracker.registerData(
            BuildingMiniatureEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Optional<UUID>> OWNER = DataTracker.registerData(
            BuildingMiniatureEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Float> ROLL_Z = DataTracker.registerData(
            BuildingMiniatureEntity.class, TrackedDataHandlerRegistry.FLOAT);

    public BuildingMiniatureEntity(EntityType<? extends BuildingMiniatureEntity> type, World world) {
        super(type, world);
        noClip = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public BuildingMiniatureEntity(World world) {
        this(ModEntities.BUILDING_MINIATURE, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(MINIATURE_STACK, ItemStack.EMPTY);
        builder.add(OWNER, Optional.empty());
        builder.add(ROLL_Z, 0.0F);
    }

    public void configure(UUID owner, ItemStack stack) {
        ItemStack stored = stack.copyWithCount(1);
        dataTracker.set(OWNER, Optional.ofNullable(owner));
        dataTracker.set(MINIATURE_STACK, stored);
        dataTracker.set(ROLL_Z, BuildingMiniatureData.readRollZ(stored));
    }

    public Optional<UUID> owner() {
        return dataTracker.get(OWNER);
    }

    public boolean isOwner(UUID uuid) {
        return owner().map(uuid::equals).orElse(false);
    }

    public ItemStack miniatureStack() {
        return dataTracker.get(MINIATURE_STACK);
    }

    public float rollZDegrees() {
        return dataTracker.get(ROLL_Z);
    }

    /** Server-authoritative rotation; only the compact float is synchronized per click. */
    public float rotateRollZ(float deltaDegrees) {
        float next = BuildingMiniatureData.normalizeRollZ(rollZDegrees() + deltaDegrees);
        dataTracker.set(ROLL_Z, next);
        return next;
    }

    /** Copies the project and injects the current rotation only when an item is actually needed. */
    public ItemStack recoverableStack() {
        ItemStack stored = miniatureStack().copyWithCount(1);
        if (!stored.isEmpty()) BuildingMiniatureData.writeRollZ(stored, rollZDegrees());
        return stored;
    }

    @Override
    public void tick() {
        // Intentionally empty: the miniature is a persistent static display.
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    public float getTargetingMargin() {
        return 0.25F;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public ItemStack getPickBlockStack() {
        ItemStack stack = recoverableStack();
        return stack.isOf(ModItems.BUILDING_MINIATURE_PROJECT) ? stack : ItemStack.EMPTY;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.containsUuid("Owner")) dataTracker.set(OWNER, Optional.of(nbt.getUuid("Owner")));
        RegistryWrapper.WrapperLookup lookup = getWorld().getRegistryManager();
        if (nbt.contains("Miniature")) {
            ItemStack.fromNbt(lookup, nbt.get("Miniature")).ifPresent(stack -> {
                dataTracker.set(MINIATURE_STACK, stack);
                dataTracker.set(ROLL_Z, BuildingMiniatureData.readRollZ(stack));
            });
        }
        if (nbt.contains("RollZ", net.minecraft.nbt.NbtElement.NUMBER_TYPE)) {
            dataTracker.set(ROLL_Z, BuildingMiniatureData.normalizeRollZ(nbt.getFloat("RollZ")));
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        owner().ifPresent(value -> nbt.putUuid("Owner", value));
        ItemStack stack = recoverableStack();
        if (!stack.isEmpty()) nbt.put("Miniature", stack.encode(getWorld().getRegistryManager()));
        nbt.putFloat("RollZ", rollZDegrees());
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!getWorld().isClient && reason.shouldDestroy() && getWorld() instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            BuildingMiniatureState.get(serverWorld.getServer()).remove(getUuid());
        }
        super.remove(reason);
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
        return new EntitySpawnS2CPacket(this, entityTrackerEntry);
    }
}
