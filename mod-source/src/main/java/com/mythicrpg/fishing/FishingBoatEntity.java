
package com.mythicrpg.fishing;

import com.mythicrpg.core.ModEntities;
import com.mythicrpg.core.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class FishingBoatEntity extends BoatEntity implements NamedScreenHandlerFactory {
    private final SimpleInventory catches = new SimpleInventory(FishingBoatScreenHandler.CAPACITY) {
        @Override
        public boolean isValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return !FishingBoatEntity.this.isRemoved()
                    && player.squaredDistanceTo(FishingBoatEntity.this) <= 64.0D;
        }
    };

    private double traveled;
    private long nextCatch;
    private boolean scattered;

    public FishingBoatEntity(EntityType<? extends FishingBoatEntity> type, World world) {
        super(type, world);
        setVariant(Type.OAK);
    }

    public FishingBoatEntity(World world, double x, double y, double z) {
        this(ModEntities.FISHING_BOAT, world);
        setPosition(x, y, z);
        prevX = x;
        prevY = y;
        prevZ = z;
    }

    @Override
    public void tick() {
        double oldX = getX();
        double oldZ = getZ();
        super.tick();

        if (!(getWorld() instanceof ServerWorld serverWorld)
                || !serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
            return;
        }

        traveled += Math.hypot(getX() - oldX, getZ() - oldZ);
        if (nextCatch == 0L) {
            nextCatch = serverWorld.getTime()
                    + FishingBalance.passiveCaptureIntervalTicks(true);
        }

        if (serverWorld.getTime() >= nextCatch && traveled >= 64.0D) {
            nextCatch = serverWorld.getTime()
                    + FishingBalance.passiveCaptureIntervalTicks(true);
            traveled = 0.0D;
            tryCatch(serverWorld);
        }
    }

    private void tryCatch(ServerWorld world) {
        int freeSlot = firstFreeSlot();
        if (freeSlot < 0 || !(getControllingPassenger() instanceof ServerPlayerEntity player)) {
            return;
        }

        FishingFamily family = FishingFamily.select(world, getBlockPos(), player.getRandom());
        FishingRarity rarity = player.getRandom().nextInt(100) < 80
                ? FishingRarity.COMMON
                : FishingRarity.RARE;
        catches.setStack(freeSlot, FishingManager.createCatch(
                family,
                rarity,
                world.getBiome(getBlockPos())
                        .getKey()
                        .map(key -> key.getValue().toString())
                        .orElse(""),
                world.getRegistryKey().getValue().toString(),
                "boat"
        ));
        catches.markDirty();
    }

    private int firstFreeSlot() {
        for (int slot = 0; slot < FishingBoatScreenHandler.CAPACITY; slot++) {
            if (catches.getStack(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (player.isSneaking()) {
            if (!getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.openHandledScreen(this);
            }
            return ActionResult.success(getWorld().isClient());
        }
        return super.interact(player, hand);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("entity.mythicrpg.fishing_boat");
    }

    @Override
    public ScreenHandler createMenu(
            int syncId,
            PlayerInventory inventory,
            PlayerEntity player
    ) {
        return new FishingBoatScreenHandler(syncId, inventory, catches);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        for (int slot = 0; slot < FishingBoatScreenHandler.CAPACITY; slot++) {
            ItemStack stack = catches.getStack(slot);
            if (!stack.isEmpty()) {
                nbt.put("catch_" + slot, stack.encode(getWorld().getRegistryManager()));
            }
        }
        nbt.putDouble("traveled", traveled);
        nbt.putLong("next_catch", nextCatch);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        catches.clear();
        for (int slot = 0; slot < FishingBoatScreenHandler.CAPACITY; slot++) {
            int targetSlot = slot;
            if (nbt.contains("catch_" + slot)) {
                ItemStack.fromNbt(
                        getWorld().getRegistryManager(),
                        nbt.get("catch_" + slot)
                ).ifPresent(stack -> catches.setStack(targetSlot, stack));
            }
        }
        traveled = Math.max(0.0D, nbt.getDouble("traveled"));
        nextCatch = Math.max(0L, nbt.getLong("next_catch"));
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!scattered && !getWorld().isClient && reason.shouldDestroy()) {
            scattered = true;
            for (int slot = 0; slot < FishingBoatScreenHandler.CAPACITY; slot++) {
                ItemStack stack = catches.removeStack(slot);
                if (!stack.isEmpty()) {
                    getWorld().spawnEntity(new ItemEntity(
                            getWorld(),
                            getX(),
                            getY() + 0.5,
                            getZ(),
                            stack
                    ));
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public Item asItem() {
        return ModItems.FISHING_BOAT;
    }

    @Override
    public ItemStack getPickBlockStack() {
        return new ItemStack(ModItems.FISHING_BOAT);
    }
}
