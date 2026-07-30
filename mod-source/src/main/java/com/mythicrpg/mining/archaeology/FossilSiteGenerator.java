package com.mythicrpg.mining.archaeology;

import com.mythicrpg.core.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Shared generator used by natural worldgen and operator test commands. */
public final class FossilSiteGenerator {

    public static final int MIN_SITE_SIZE = 2;
    public static final int MAX_SITE_SIZE = 6;
    public static final int MIN_GENERATION_Y = -56;
    public static final int MAX_GENERATION_Y = 48;

    private static final Direction[] DIRECTIONS = Direction.values();

    private FossilSiteGenerator() {
    }

    public static Optional<GeneratedSite> generateAt(
            WorldAccess world,
            BlockPos seed,
            FossilFamily family,
            FossilRarity dominantRarity,
            int requestedSize,
            Random random
    ) {
        int siteSize = Math.max(MIN_SITE_SIZE, Math.min(MAX_SITE_SIZE, requestedSize));
        List<BlockPos> positions = collectConnectedPositions(world, seed, siteSize, random);
        if (positions.size() != siteSize) {
            return Optional.empty();
        }

        UUID siteId = new UUID(random.nextLong(), random.nextLong());
        BlockPos center = averagePosition(positions);
        EnumMap<FossilRarity, Integer> rarityCounts = new EnumMap<>(FossilRarity.class);
        List<FossilRarity> actualRarities = new ArrayList<>(positions.size());
        List<BlockState> originalStates = new ArrayList<>(positions.size());

        for (int index = 0; index < positions.size(); index++) {
            FossilRarity actualRarity = rollBlockRarity(random, dominantRarity);
            actualRarities.add(actualRarity);
            rarityCounts.merge(actualRarity, 1, Integer::sum);
            originalStates.add(world.getBlockState(positions.get(index)));
        }

        // Place every block before configuring block entities so the site is all-or-nothing.
        for (BlockPos pos : positions) {
            world.setBlockState(pos, ModBlocks.FOSSIL_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
        }

        List<FossilBlockEntity> blockEntities = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (!(blockEntity instanceof FossilBlockEntity fossil)) {
                for (int rollbackIndex = 0; rollbackIndex < positions.size(); rollbackIndex++) {
                    world.setBlockState(
                            positions.get(rollbackIndex),
                            originalStates.get(rollbackIndex),
                            Block.NOTIFY_LISTENERS
                    );
                }
                return Optional.empty();
            }
            blockEntities.add(fossil);
        }

        for (int index = 0; index < blockEntities.size(); index++) {
            blockEntities.get(index).configureSite(
                    siteId,
                    center,
                    family,
                    actualRarities.get(index),
                    dominantRarity,
                    positions.size()
            );
        }

        return Optional.of(new GeneratedSite(
                siteId,
                center,
                family,
                dominantRarity,
                positions.size(),
                Collections.unmodifiableMap(rarityCounts),
                List.copyOf(positions)
        ));
    }

    public static boolean isValidFossilPosition(WorldAccess world, BlockPos pos) {
        if (pos.getY() < MIN_GENERATION_Y || pos.getY() > MAX_GENERATION_Y) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        return state.isOf(Blocks.STONE) || state.isOf(Blocks.DEEPSLATE);
    }

    private static FossilRarity rollBlockRarity(Random random, FossilRarity dominantRarity) {
        // 75% inherit the site rarity. The remaining 25% use the global weighted table and
        // may legitimately roll the dominant rarity again.
        return random.nextInt(4) == 0
                ? FossilRarity.rollGeneration(random)
                : dominantRarity;
    }

    private static List<BlockPos> collectConnectedPositions(
            WorldAccess world,
            BlockPos seed,
            int desiredCount,
            Random random
    ) {
        if (!isValidFossilPosition(world, seed)) {
            return List.of();
        }

        ArrayList<BlockPos> positions = new ArrayList<>(desiredCount);
        Set<BlockPos> seen = new HashSet<>();
        positions.add(seed.toImmutable());
        seen.add(seed.toImmutable());

        int attempts = 0;
        while (positions.size() < desiredCount && attempts++ < 96) {
            BlockPos parent = positions.get(random.nextInt(positions.size()));
            Direction direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            BlockPos candidate = parent.offset(direction).toImmutable();
            if (seen.add(candidate) && isValidFossilPosition(world, candidate)) {
                positions.add(candidate);
            }
        }
        return positions;
    }

    private static BlockPos averagePosition(List<BlockPos> positions) {
        long x = 0L;
        long y = 0L;
        long z = 0L;
        for (BlockPos pos : positions) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }
        int size = positions.size();
        return new BlockPos((int) (x / size), (int) (y / size), (int) (z / size));
    }

    public record GeneratedSite(
            UUID id,
            BlockPos center,
            FossilFamily family,
            FossilRarity dominantRarity,
            int blockCount,
            Map<FossilRarity, Integer> rarityCounts,
            List<BlockPos> positions
    ) {
    }
}
