package com.mythicrpg.mining.archaeology.relic;

import com.mythicrpg.core.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public final class GrowthTotemBlockEntity extends BlockEntity {

    private static final Vector3f TOTEM_COLOR = new Vector3f(0.30F, 0.92F, 0.38F);

    private int level = 1;

    public GrowthTotemBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GROWTH_TOTEM, pos, state);
    }

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(5, level));
        markDirty();
    }

    public int level() {
        return level;
    }

    public int radius() {
        return GrowthTotemManager.radiusForLevel(level);
    }

    public static void tick(
            net.minecraft.world.World baseWorld,
            BlockPos pos,
            BlockState state,
            GrowthTotemBlockEntity blockEntity
    ) {
        if (!(baseWorld instanceof ServerWorld world)) {
            return;
        }

        long stagger = Math.floorMod(pos.asLong(), 20L);
        if (Math.floorMod(world.getTime(), 20L) != stagger) {
            return;
        }
        GrowthTotemManager.touch(world, pos, blockEntity.level);

        if (Math.floorMod(world.getTime(), 40L) == Math.floorMod(pos.asLong(), 40L)) {
            world.spawnParticles(
                    new DustParticleEffect(TOTEM_COLOR, 0.55F),
                    pos.getX() + 0.5,
                    pos.getY() + 1.35,
                    pos.getZ() + 0.5,
                    2,
                    0.16,
                    0.32,
                    0.16,
                    0.005
            );
            world.spawnParticles(
                    ParticleTypes.COMPOSTER,
                    pos.getX() + 0.5,
                    pos.getY() + 0.85,
                    pos.getZ() + 0.5,
                    1,
                    0.12,
                    0.15,
                    0.12,
                    0.0
            );
        }

        int radius = blockEntity.radius();
        int attempts = 40 + blockEntity.level * 20;
        Set<Long> visited = new HashSet<>(attempts);

        for (int index = 0; index < attempts; index++) {
            int dx = world.random.nextBetween(-radius, radius);
            int dz = world.random.nextBetween(-radius, radius);
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            BlockPos target = pos.add(dx, world.random.nextBetween(-2, 4), dz);
            if (!world.getChunkManager().isChunkLoaded(target.getX() >> 4, target.getZ() >> 4)
                    || !visited.add(target.asLong())) {
                continue;
            }

            BlockState before = world.getBlockState(target);
            if (!canGrowNow(before)
                    || !GrowthTotemManager.isDominantFor(world, target, pos, blockEntity.level)) {
                continue;
            }

            before.randomTick(world, target, world.random);
            if (before != world.getBlockState(target) && world.random.nextInt(4) == 0) {
                world.spawnParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        target.getX() + 0.5,
                        target.getY() + 0.65,
                        target.getZ() + 0.5,
                        1,
                        0.08,
                        0.12,
                        0.08,
                        0.0
                );
            }
        }
    }

    public static int countCompatibleCrops(ServerWorld world, BlockPos center, int radius) {
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                for (int dy = -2; dy <= 4; dy++) {
                    BlockPos target = center.add(dx, dy, dz);
                    if (world.getChunkManager().isChunkLoaded(target.getX() >> 4, target.getZ() >> 4)
                            && canGrowNow(world.getBlockState(target))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean canGrowNow(BlockState state) {
        return canGrow(state) && state.hasRandomTicks();
    }

    private static boolean canGrow(BlockState state) {
        Block block = state.getBlock();
        return block instanceof CropBlock
                || state.isOf(Blocks.NETHER_WART)
                || state.isOf(Blocks.COCOA)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.CAVE_VINES)
                || state.isOf(Blocks.CAVE_VINES_PLANT)
                || state.isOf(Blocks.MELON_STEM)
                || state.isOf(Blocks.PUMPKIN_STEM)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.SUGAR_CANE)
                || state.isOf(Blocks.BAMBOO)
                || state.isOf(Blocks.KELP)
                || state.isOf(Blocks.KELP_PLANT)
                || state.isOf(Blocks.TWISTING_VINES)
                || state.isOf(Blocks.TWISTING_VINES_PLANT)
                || state.isOf(Blocks.WEEPING_VINES)
                || state.isOf(Blocks.WEEPING_VINES_PLANT);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("Level", level);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        level = Math.max(1, Math.min(5, nbt.getInt("Level")));
    }
}
