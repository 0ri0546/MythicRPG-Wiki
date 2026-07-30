package com.mythicrpg.mining.archaeology;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.core.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Optional;

public final class FossilIncubatorBlockEntity extends LockableContainerBlockEntity implements SidedInventory {

    public static final int FOSSIL_SLOT_START = 0;
    public static final int FOSSIL_SLOT_END = 9;
    public static final int WATER_SLOT = 9;
    public static final int KELP_SLOT = 10;
    public static final int RESULT_SLOT = 11;
    public static final int INVENTORY_SIZE = 12;

    public static final int PROPERTY_REMAINING_TICKS = 0;
    public static final int PROPERTY_TOTAL_TICKS = 1;
    public static final int PROPERTY_STATUS = 2;
    public static final int PROPERTY_OUTPUT_RARITY = 3;
    public static final int PROPERTY_OUTPUT_FAMILY = 4;
    public static final int PROPERTY_COUNT = 5;

    private static final int[] AUTOMATION_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, WATER_SLOT, KELP_SLOT
    };

    private DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private boolean processing;
    private long completionTime;
    private int totalDurationTicks;
    private FossilFamily outputFamily = FossilFamily.SMALL_LAND;
    private FossilRarity outputRarity = FossilRarity.COMMON;
    private transient long nextCompletionRetryAt;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_REMAINING_TICKS -> getRemainingTicksForSync();
                case PROPERTY_TOTAL_TICKS -> totalDurationTicks;
                case PROPERTY_STATUS -> processing ? 1 : (items.get(RESULT_SLOT).isEmpty() ? 0 : 2);
                case PROPERTY_OUTPUT_RARITY -> outputRarity.rank();
                case PROPERTY_OUTPUT_FAMILY -> outputFamily.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-authoritative properties. Client values are populated by ScreenHandler syncing.
        }

        @Override
        public int size() {
            return PROPERTY_COUNT;
        }
    };

    public FossilIncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOSSIL_INCUBATOR, pos, state);
    }

    public PropertyDelegate properties() {
        return properties;
    }

    public boolean isProcessing() {
        return processing;
    }

    public boolean isLockedForBreaking() {
        return processing || !items.get(RESULT_SLOT).isEmpty();
    }

    public boolean canStartProcess() {
        return !processing
                && items.get(RESULT_SLOT).isEmpty()
                && FossilIncubationRecipe.resolveReadyRecipe(this).isPresent();
    }

    public boolean startProcess() {
        if (processing || !items.get(RESULT_SLOT).isEmpty() || world == null || world.isClient()) {
            return false;
        }

        Optional<FossilIncubationRecipe.Output> resolved = FossilIncubationRecipe.resolveReadyRecipe(this);
        if (resolved.isEmpty()) {
            return false;
        }

        FossilIncubationRecipe.Output output = resolved.get();
        outputFamily = output.family();
        outputRarity = output.rarity();

        for (int slot = FOSSIL_SLOT_START; slot < FOSSIL_SLOT_END; slot++) {
            items.get(slot).decrement(1);
        }
        items.set(WATER_SLOT, new ItemStack(Items.BUCKET));
        items.get(KELP_SLOT).decrement(FossilIncubationRecipe.REQUIRED_KELP);

        totalDurationTicks = durationFor(outputRarity);
        completionTime = world.getTime() + totalDurationTicks;
        processing = true;
        markDirtyAndSync();

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                    new DustParticleEffect(outputRarity.particleColor(), 0.75F),
                    pos.getX() + 0.5,
                    pos.getY() + 1.05,
                    pos.getZ() + 0.5,
                    10,
                    0.22,
                    0.18,
                    0.22,
                    0.01
            );
            serverWorld.playSound(
                    null,
                    pos,
                    SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    SoundCategory.BLOCKS,
                    0.55F,
                    0.85F + outputRarity.rank() * 0.06F
            );
        }
        return true;
    }

    public boolean prepareSkeletonClaim(PlayerEntity player, ItemStack stack) {
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)
                || stack.isEmpty()
                || !(stack.getItem() instanceof FossilSkeletonItem skeletonItem)
                || skeletonItem.family() != outputFamily
                || skeletonItem.rarity() != outputRarity) {
            return false;
        }

        java.util.Optional<FossilSpecimenData.Specimen> existing = FossilSpecimenData.read(stack);
        if (existing.isPresent()) {
            FossilSpecimenData.Specimen specimen = existing.get();
            return specimen.family() == outputFamily
                    && specimen.rarity() == outputRarity
                    && specimen.reconstructedBy().equals(serverPlayer.getUuid());
        }

        long day = world == null ? 0L : world.getTimeOfDay() / 24_000L;
        FossilSpecimenData.initializeSkeleton(
                stack,
                outputFamily,
                outputRarity,
                serverPlayer.getUuid(),
                day
        );
        return true;
    }

    public boolean recordSkeletonClaim(PlayerEntity player, ItemStack stack) {
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)
                || !prepareSkeletonClaim(player, stack)) {
            return false;
        }

        java.util.Optional<FossilSpecimenData.Specimen> specimen = FossilSpecimenData.read(stack);
        if (specimen.isEmpty()) {
            return false;
        }

        FossilSpecimenData.Specimen claimed = specimen.get();
        FossilCodexManager.recordReconstruction(
                serverPlayer,
                claimed.family(),
                claimed.rarity(),
                claimed.specimenId().toString(),
                claimed.reconstructedDay()
        );
        totalDurationTicks = 0;
        markDirtyAndSync();
        return true;
    }

    public void claimSkeleton(PlayerEntity player, ItemStack stack) {
        recordSkeletonClaim(player, stack);
    }

    private int getRemainingTicks() {
        if (!processing || world == null) {
            return 0;
        }
        long remaining = completionTime - world.getTime();
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, remaining));
    }

    private int getRemainingTicksForSync() {
        int remaining = getRemainingTicks();
        if (remaining <= 0) {
            return 0;
        }
        // ScreenHandler properties are checked every tick. Rounding to full
        // seconds keeps the same UI while only changing the synced value once
        // per second during incubations lasting up to 90 minutes.
        return ((remaining + 19) / 20) * 20;
    }

    private static int durationFor(FossilRarity rarity) {
        return rarity.incubationTicks();
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, FossilIncubatorBlockEntity blockEntity) {
        if (!(world instanceof ServerWorld serverWorld) || !blockEntity.processing) {
            return;
        }

        long time = serverWorld.getTime();
        long stagger = Math.floorMod(pos.asLong(), 10L);
        if (Math.floorMod(time, 10L) == stagger) {
            double angle = (time + Math.floorMod(pos.asLong(), 360L)) * 0.11;
            double radius = 0.23 + serverWorld.random.nextDouble() * 0.11;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = pos.getY() + 0.88 + serverWorld.random.nextDouble() * 0.34;

            serverWorld.spawnParticles(
                    new DustParticleEffect(blockEntity.outputRarity.particleColor(), 0.58F),
                    x, y, z,
                    1,
                    0.015, 0.025, 0.015,
                    0.0
            );
            if (Math.floorMod(time, 30L) == Math.floorMod(pos.asLong(), 30L)) {
                serverWorld.spawnParticles(
                        ParticleTypes.ENCHANT,
                        pos.getX() + 0.5,
                        pos.getY() + 0.95,
                        pos.getZ() + 0.5,
                        1,
                        0.18, 0.12, 0.18,
                        0.01
                );
            }
        }

        if (time < blockEntity.completionTime) {
            return;
        }

        if (time < blockEntity.nextCompletionRetryAt) {
            return;
        }

        Optional<Item> outputItem = FossilContentRegistry.skeletonItem(
                blockEntity.outputFamily,
                blockEntity.outputRarity
        );

        if (outputItem.isEmpty() || !blockEntity.items.get(RESULT_SLOT).isEmpty()) {
            blockEntity.nextCompletionRetryAt = time + 200L;
            MythicRPG.LOGGER.error(
                    "Incubator at {} cannot complete: output registered={}, result slot empty={}",
                    pos,
                    outputItem.isPresent(),
                    blockEntity.items.get(RESULT_SLOT).isEmpty()
            );
            return;
        }

        blockEntity.items.set(RESULT_SLOT, new ItemStack(outputItem.get()));
        blockEntity.processing = false;
        blockEntity.completionTime = 0L;
        blockEntity.nextCompletionRetryAt = 0L;
        blockEntity.markDirtyAndSync();

        serverWorld.spawnParticles(
                new DustParticleEffect(blockEntity.outputRarity.particleColor(), 1.0F),
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                18 + blockEntity.outputRarity.rank() * 3,
                0.35, 0.3, 0.35,
                0.025
        );
        serverWorld.spawnParticles(
                ParticleTypes.END_ROD,
                pos.getX() + 0.5,
                pos.getY() + 1.0,
                pos.getZ() + 0.5,
                5,
                0.22, 0.24, 0.22,
                0.015
        );
        serverWorld.playSound(
                null,
                pos,
                SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK,
                SoundCategory.BLOCKS,
                0.75F,
                1.1F + blockEntity.outputRarity.rank() * 0.04F
        );
    }

    private void markDirtyAndSync() {
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("screen.mythicrpg.fossil_incubator");
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
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new FossilIncubatorScreenHandler(syncId, playerInventory, this, properties);
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) {
            return false;
        }
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (processing || slot == RESULT_SLOT) {
            return false;
        }
        if (slot >= FOSSIL_SLOT_START && slot < FOSSIL_SLOT_END) {
            return FossilIncubationRecipe.canInsertFossil(this, slot, stack);
        }
        if (slot == WATER_SLOT) {
            return stack.isOf(Items.WATER_BUCKET);
        }
        if (slot == KELP_SLOT) {
            return stack.isOf(Items.KELP);
        }
        return false;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return AUTOMATION_SLOTS.clone();
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        if (!isValid(slot, stack)) {
            return false;
        }
        if (slot >= FOSSIL_SLOT_START && slot < FOSSIL_SLOT_END) {
            return items.get(slot).isEmpty();
        }
        if (slot == WATER_SLOT) {
            return items.get(slot).isEmpty();
        }
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == WATER_SLOT && stack.isOf(Items.BUCKET) && !processing;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items, registryLookup);
        processing = nbt.getBoolean("processing");
        completionTime = nbt.getLong("completion_time");
        totalDurationTicks = nbt.getInt("total_duration_ticks");
        outputFamily = FossilFamily.byId(nbt.getString("output_family"))
                .orElse(FossilFamily.SMALL_LAND);
        outputRarity = FossilRarity.byId(nbt.getString("output_rarity"))
                .orElse(FossilRarity.COMMON);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, items, registryLookup);
        nbt.putBoolean("processing", processing);
        nbt.putLong("completion_time", completionTime);
        nbt.putInt("total_duration_ticks", totalDurationTicks);
        nbt.putString("output_family", outputFamily.id());
        nbt.putString("output_rarity", outputRarity.id());
    }
}
