package com.mythicrpg.mining.archaeology.relic;

import com.mythicrpg.core.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FossilDrillBlockEntity extends BlockEntity {

    private final List<BlockPos> ores = new ArrayList<>();
    private long startAt;
    private long finishAt;
    private int totalDurationTicks;
    private int multiplier = 2;
    private UUID owner;
    private transient boolean registryConflict;
    private transient boolean registryChecked;

    public FossilDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOSSIL_DRILL, pos, state);
    }

    public void configure(
            List<BlockPos> positions,
            long startAt,
            long finishAt,
            int multiplier,
            UUID owner
    ) {
        ores.clear();
        ores.addAll(positions.stream().map(BlockPos::toImmutable).toList());
        this.startAt = startAt;
        this.finishAt = finishAt;
        this.totalDurationTicks = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, finishAt - startAt));
        this.multiplier = Math.max(2, Math.min(6, multiplier));
        this.owner = owner;
        this.registryConflict = false;
        this.registryChecked = false;
        markDirty();
    }

    public boolean isProcessing() {
        return finishAt > 0L && !ores.isEmpty();
    }

    public UUID owner() {
        return owner;
    }

    public int oreCount() {
        return ores.size();
    }

    public int multiplier() {
        return multiplier;
    }

    public long remainingTicks(long worldTime) {
        return isProcessing() ? Math.max(0L, finishAt - worldTime) : 0L;
    }

    public int progressPercent(long worldTime) {
        if (!isProcessing() || totalDurationTicks <= 0) {
            return 100;
        }
        long elapsed = Math.max(0L, worldTime - startAt);
        return Math.max(0, Math.min(100, (int) (elapsed * 100L / totalDurationTicks)));
    }

    public static void tick(
            net.minecraft.world.World baseWorld,
            BlockPos pos,
            BlockState state,
            FossilDrillBlockEntity blockEntity
    ) {
        if (!(baseWorld instanceof ServerWorld world) || !blockEntity.isProcessing()) {
            return;
        }

        long time = world.getTime();
        boolean conflictChanged = false;
        if (!blockEntity.registryChecked
                || Math.floorMod(time, 20L) == Math.floorMod(pos.asLong(), 20L)) {
            boolean conflict = !FossilDrillManager.ensureRegistered(
                    world,
                    blockEntity.owner,
                    pos
            );
            if (conflict != blockEntity.registryConflict) {
                blockEntity.registryConflict = conflict;
                conflictChanged = true;
            }
            blockEntity.registryChecked = true;
        }
        if (blockEntity.registryConflict) {
            // A legacy/conflicting drill must not progress while another drill owns
            // the player's single global slot. Preserve its remaining duration and
            // persist the paused clock once per second for unload/restart safety.
            blockEntity.startAt++;
            blockEntity.finishAt++;
            if (conflictChanged
                    || Math.floorMod(time, 20L) == Math.floorMod(pos.asLong(), 20L)) {
                blockEntity.markDirty();
            }
            return;
        }
        if (conflictChanged) {
            blockEntity.markDirty();
        }
        long particleStagger = Math.floorMod(pos.asLong(), 10L);
        if (Math.floorMod(time, 10L) == particleStagger) {
            world.spawnParticles(
                    ParticleTypes.SMOKE,
                    pos.getX() + 0.5,
                    pos.getY() + 0.82,
                    pos.getZ() + 0.5,
                    1,
                    0.12,
                    0.12,
                    0.12,
                    0.005
            );
            world.spawnParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5,
                    pos.getY() + 0.55,
                    pos.getZ() + 0.5,
                    1,
                    0.16,
                    0.12,
                    0.16,
                    0.015
            );

            BlockState drilledState = blockEntity.firstValidOreState(world);
            if (drilledState != null && Math.floorMod(time, 20L) == Math.floorMod(pos.asLong(), 20L)) {
                world.spawnParticles(
                        new BlockStateParticleEffect(ParticleTypes.BLOCK, drilledState),
                        pos.getX() + 0.5,
                        pos.getY() + 0.45,
                        pos.getZ() + 0.5,
                        2,
                        0.18,
                        0.12,
                        0.18,
                        0.02
                );
            }
        }

        if (Math.floorMod(time, 80L) == Math.floorMod(pos.asLong(), 80L)) {
            world.playSound(
                    null,
                    pos,
                    SoundEvents.BLOCK_GRINDSTONE_USE,
                    SoundCategory.BLOCKS,
                    0.22F,
                    0.72F + world.random.nextFloat() * 0.12F
            );
        }

        if (time < blockEntity.finishAt) {
            return;
        }

        if (!blockEntity.allOreChunksLoaded(world)) {
            return;
        }

        int extractedBlocks = 0;
        for (BlockPos orePos : blockEntity.ores) {
            BlockState oreState = world.getBlockState(orePos);
            if (!FossilDrillItem.isOre(oreState)) {
                continue;
            }

            List<ItemStack> drops = Block.getDroppedStacks(
                    oreState,
                    world,
                    orePos,
                    null,
                    null,
                    ItemStack.EMPTY
            );
            world.setBlockState(orePos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            extractedBlocks++;
            for (ItemStack drop : drops) {
                blockEntity.spawnMultipliedDrop(world, pos, drop);
            }
        }

        UUID completedOwner = blockEntity.owner;
        FossilDrillManager.remove(world, completedOwner, pos);
        blockEntity.finishAt = 0L;
        blockEntity.startAt = 0L;
        blockEntity.totalDurationTicks = 0;
        blockEntity.ores.clear();
        blockEntity.markDirty();

        world.spawnParticles(
                ParticleTypes.POOF,
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                12,
                0.3,
                0.25,
                0.3,
                0.035
        );
        world.spawnParticles(
                ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5,
                pos.getY() + 0.65,
                pos.getZ() + 0.5,
                8,
                0.25,
                0.2,
                0.25,
                0.04
        );
        world.playSound(
                null,
                pos,
                SoundEvents.BLOCK_ANVIL_USE,
                SoundCategory.BLOCKS,
                0.65F,
                1.25F
        );

        if (completedOwner != null) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(completedOwner);
            if (player != null) {
                player.sendMessage(
                        Text.translatable(
                                "message.mythicrpg.fossil_drill.complete",
                                extractedBlocks,
                                blockEntity.multiplier
                        ).formatted(Formatting.AQUA),
                        true
                );
            }
        }

        world.breakBlock(pos, false);
    }

    private BlockState firstValidOreState(ServerWorld world) {
        for (BlockPos orePos : ores) {
            if (!world.getChunkManager().isChunkLoaded(orePos.getX() >> 4, orePos.getZ() >> 4)) {
                continue;
            }
            BlockState state = world.getBlockState(orePos);
            if (FossilDrillItem.isOre(state)) {
                return state;
            }
        }
        return null;
    }

    private boolean allOreChunksLoaded(ServerWorld world) {
        for (BlockPos orePos : ores) {
            if (!world.getChunkManager().isChunkLoaded(orePos.getX() >> 4, orePos.getZ() >> 4)) {
                return false;
            }
        }
        return true;
    }

    private void spawnMultipliedDrop(ServerWorld world, BlockPos outputPos, ItemStack baseDrop) {
        int remaining = Math.max(0, baseDrop.getCount() * multiplier);
        while (remaining > 0) {
            ItemStack output = baseDrop.copy();
            int count = Math.min(remaining, output.getMaxCount());
            output.setCount(count);
            remaining -= count;
            world.spawnEntity(new ItemEntity(
                    world,
                    outputPos.getX() + 0.5,
                    outputPos.getY() + 0.8,
                    outputPos.getZ() + 0.5,
                    output
            ));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putLong("Start", startAt);
        nbt.putLong("Finish", finishAt);
        nbt.putInt("TotalDuration", totalDurationTicks);
        nbt.putInt("Multiplier", multiplier);
        if (owner != null) {
            nbt.putUuid("Owner", owner);
        }
        NbtList list = new NbtList();
        for (BlockPos orePos : ores) {
            NbtCompound compound = new NbtCompound();
            compound.putLong("Pos", orePos.asLong());
            list.add(compound);
        }
        nbt.put("Ores", list);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        startAt = nbt.getLong("Start");
        finishAt = nbt.getLong("Finish");
        totalDurationTicks = nbt.contains("TotalDuration")
                ? Math.max(0, nbt.getInt("TotalDuration"))
                : (int) Math.max(0L, finishAt - startAt);
        multiplier = Math.max(2, Math.min(6, nbt.getInt("Multiplier")));
        owner = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        registryConflict = false;
        registryChecked = false;
        ores.clear();
        NbtList list = nbt.getList("Ores", 10);
        for (int index = 0; index < list.size(); index++) {
            ores.add(BlockPos.fromLong(list.getCompound(index).getLong("Pos")));
        }
    }
}
