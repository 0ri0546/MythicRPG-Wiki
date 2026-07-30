package com.mythicrpg.crafting;

import com.mythicrpg.core.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LuckyBlockStructureGenerator {

    private static final int TEMPLE_HALF_LENGTH = 3;
    private static final int TEMPLE_HALF_WIDTH = 2;
    private static final int TEMPLE_TOP_OFFSET = 3;

    private static final List<TemplePalette> TEMPLE_PALETTES = List.of(
            new TemplePalette(
                    Blocks.CUT_SANDSTONE.getDefaultState(),
                    Blocks.SANDSTONE.getDefaultState(),
                    Blocks.CHISELED_SANDSTONE.getDefaultState()
            ),
            new TemplePalette(
                    Blocks.QUARTZ_BRICKS.getDefaultState(),
                    Blocks.QUARTZ_PILLAR.getDefaultState(),
                    Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState()
            ),
            new TemplePalette(
                    Blocks.NETHER_BRICKS.getDefaultState(),
                    Blocks.CHISELED_NETHER_BRICKS.getDefaultState(),
                    Blocks.CRACKED_NETHER_BRICKS.getDefaultState()
            ),
            new TemplePalette(
                    Blocks.STONE_BRICKS.getDefaultState(),
                    Blocks.CHISELED_STONE_BRICKS.getDefaultState(),
                    Blocks.CRACKED_STONE_BRICKS.getDefaultState()
            ),
            new TemplePalette(
                    Blocks.MOSSY_STONE_BRICKS.getDefaultState(),
                    Blocks.STONE_BRICKS.getDefaultState(),
                    Blocks.CHISELED_STONE_BRICKS.getDefaultState()
            ),
            new TemplePalette(
                    Blocks.DEEPSLATE_BRICKS.getDefaultState(),
                    Blocks.POLISHED_DEEPSLATE.getDefaultState(),
                    Blocks.CHISELED_DEEPSLATE.getDefaultState()
            )
    );

    private static final List<LootEntry> FOOD_LOOT = List.of(
            new LootEntry(Items.BREAD, 2, 5),
            new LootEntry(Items.APPLE, 1, 3),
            new LootEntry(Items.CARROT, 2, 6),
            new LootEntry(Items.BAKED_POTATO, 1, 4),
            new LootEntry(Items.COOKED_BEEF, 1, 3),
            new LootEntry(Items.COOKED_CHICKEN, 1, 3)
    );

    private static final List<LootEntry> FILLER_LOOT = List.of(
            new LootEntry(Items.STRING, 1, 4),
            new LootEntry(Items.STICK, 2, 6),
            new LootEntry(Items.SAND, 2, 8)
    );

    private LuckyBlockStructureGenerator() {
    }

    public static void generateTemple(ServerWorld world, BlockPos requestedCenter, Direction playerFacing) {
        Optional<BlockPos> centerResult = findTempleCenter(world, requestedCenter);

        if (centerResult.isEmpty()) {
            return;
        }

        BlockPos center = centerResult.get();
        BlockPos floorCenter = center.down();
        TemplePalette palette = TEMPLE_PALETTES.get(world.random.nextInt(TEMPLE_PALETTES.size()));

        clearTempleVolume(world, floorCenter);
        placeTempleFloor(world, floorCenter, palette);
        placeTemplePillars(world, floorCenter, palette);
        placeTempleRoofFrame(world, floorCenter, palette);
        placeTempleChest(world, center, playerFacing);

        world.spawnParticles(
                ParticleTypes.HAPPY_VILLAGER,
                center.getX() + 0.5,
                center.getY() + 1.0,
                center.getZ() + 0.5,
                30,
                1.6,
                0.8,
                1.2,
                0.08
        );

        world.playSound(
                null,
                center,
                SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.BLOCKS,
                0.8f,
                1.35f
        );
    }

    public static void generateCoinFlipChoice(
            ServerWorld world,
            BlockPos requestedCenter,
            Direction playerFacing
    ) {
        Direction lineDirection = playerFacing.rotateYClockwise();
        Optional<BlockPos> centerResult = findChoiceCenter(world, requestedCenter, lineDirection);

        if (centerResult.isEmpty()) {
            return;
        }

        BlockPos center = centerResult.get();
        BlockPos firstPos = center.offset(lineDirection);
        BlockPos secondPos = center.offset(lineDirection.getOpposite());

        boolean firstIsLucky = world.random.nextBoolean();
        int firstLuck = firstIsLucky ? LuckyBlockLuckManager.MAX_LUCK : LuckyBlockLuckManager.MIN_LUCK;
        int secondLuck = firstIsLucky ? LuckyBlockLuckManager.MIN_LUCK : LuckyBlockLuckManager.MAX_LUCK;

        BlockState firstState = ModBlocks.LUCKY_BLOCK.getDefaultState()
                .with(LuckyBlock.LUCK, LuckyBlock.encodeLuck(firstLuck));
        BlockState secondState = ModBlocks.LUCKY_BLOCK.getDefaultState()
                .with(LuckyBlock.LUCK, LuckyBlock.encodeLuck(secondLuck));

        world.setBlockState(firstPos, firstState, Block.NOTIFY_ALL);
        world.setBlockState(secondPos, secondState, Block.NOTIFY_ALL);
        placeChoiceSign(world, center, playerFacing);

        LuckyBlockChoiceManager.registerPair(world, firstPos, secondPos);

        world.spawnParticles(
                ParticleTypes.ENCHANT,
                center.getX() + 0.5,
                center.getY() + 0.8,
                center.getZ() + 0.5,
                40,
                1.3,
                0.55,
                1.3,
                0.08
        );

        world.playSound(
                null,
                center,
                SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                SoundCategory.BLOCKS,
                1.0f,
                1.0f
        );
    }

    private static Optional<BlockPos> findTempleCenter(ServerWorld world, BlockPos requestedCenter) {
        if (canPlaceTemple(world, requestedCenter, false)) {
            return Optional.of(requestedCenter.toImmutable());
        }

        for (int attempt = 0; attempt < 50; attempt++) {
            BlockPos candidate = requestedCenter.add(
                    world.random.nextBetween(-6, 6),
                    world.random.nextBetween(-2, 2),
                    world.random.nextBetween(-6, 6)
            );

            if (canPlaceTemple(world, candidate, false)) {
                return Optional.of(candidate.toImmutable());
            }
        }

        if (canPlaceTemple(world, requestedCenter, true)) {
            return Optional.of(requestedCenter.toImmutable());
        }

        return Optional.empty();
    }

    private static boolean canPlaceTemple(ServerWorld world, BlockPos center, boolean allowSolidVolume) {
        BlockPos floorCenter = center.down();
        int floorY = floorCenter.getY();

        if (floorY <= world.getBottomY() || floorY + TEMPLE_TOP_OFFSET >= world.getTopY()) {
            return false;
        }

        BlockPos centerSupportPos = floorCenter.down();

        if (!world.getBlockState(centerSupportPos).isSideSolidFullSquare(
                world,
                centerSupportPos,
                Direction.UP
        )) {
            return false;
        }

        for (int x = -TEMPLE_HALF_LENGTH; x <= TEMPLE_HALF_LENGTH; x++) {
            for (int z = -TEMPLE_HALF_WIDTH; z <= TEMPLE_HALF_WIDTH; z++) {
                BlockPos floorPos = floorCenter.add(x, 0, z);
                BlockState floorState = world.getBlockState(floorPos);

                if (world.getBlockEntity(floorPos) != null || floorState.getHardness(world, floorPos) < 0.0f) {
                    return false;
                }

                for (int y = 1; y <= TEMPLE_TOP_OFFSET; y++) {
                    BlockPos checkPos = floorCenter.add(x, y, z);
                    BlockState state = world.getBlockState(checkPos);

                    if (world.getBlockEntity(checkPos) != null || state.getHardness(world, checkPos) < 0.0f) {
                        return false;
                    }

                    if (!allowSolidVolume && !state.isAir() && !state.isReplaceable()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static void clearTempleVolume(ServerWorld world, BlockPos floorCenter) {
        for (int x = -TEMPLE_HALF_LENGTH; x <= TEMPLE_HALF_LENGTH; x++) {
            for (int z = -TEMPLE_HALF_WIDTH; z <= TEMPLE_HALF_WIDTH; z++) {
                for (int y = 1; y <= TEMPLE_TOP_OFFSET; y++) {
                    world.setBlockState(
                            floorCenter.add(x, y, z),
                            Blocks.AIR.getDefaultState(),
                            Block.NOTIFY_ALL
                    );
                }
            }
        }
    }

    private static void placeTempleFloor(
            ServerWorld world,
            BlockPos floorCenter,
            TemplePalette palette
    ) {
        for (int x = -TEMPLE_HALF_LENGTH; x <= TEMPLE_HALF_LENGTH; x++) {
            for (int z = -TEMPLE_HALF_WIDTH; z <= TEMPLE_HALF_WIDTH; z++) {
                BlockState state = x == 0 && z == 0 ? palette.trim() : palette.floor();
                world.setBlockState(floorCenter.add(x, 0, z), state, Block.NOTIFY_ALL);
            }
        }
    }

    private static void placeTemplePillars(
            ServerWorld world,
            BlockPos floorCenter,
            TemplePalette palette
    ) {
        int[] xs = {-TEMPLE_HALF_LENGTH, TEMPLE_HALF_LENGTH};
        int[] zs = {-TEMPLE_HALF_WIDTH, TEMPLE_HALF_WIDTH};

        for (int x : xs) {
            for (int z : zs) {
                for (int y = 1; y <= TEMPLE_TOP_OFFSET; y++) {
                    world.setBlockState(
                            floorCenter.add(x, y, z),
                            palette.pillar(),
                            Block.NOTIFY_ALL
                    );
                }
            }
        }
    }

    private static void placeTempleRoofFrame(
            ServerWorld world,
            BlockPos floorCenter,
            TemplePalette palette
    ) {
        for (int x = -TEMPLE_HALF_LENGTH; x <= TEMPLE_HALF_LENGTH; x++) {
            world.setBlockState(
                    floorCenter.add(x, TEMPLE_TOP_OFFSET, -TEMPLE_HALF_WIDTH),
                    palette.trim(),
                    Block.NOTIFY_ALL
            );
            world.setBlockState(
                    floorCenter.add(x, TEMPLE_TOP_OFFSET, TEMPLE_HALF_WIDTH),
                    palette.trim(),
                    Block.NOTIFY_ALL
            );
        }

        for (int z = -TEMPLE_HALF_WIDTH + 1; z < TEMPLE_HALF_WIDTH; z++) {
            world.setBlockState(
                    floorCenter.add(-TEMPLE_HALF_LENGTH, TEMPLE_TOP_OFFSET, z),
                    palette.trim(),
                    Block.NOTIFY_ALL
            );
            world.setBlockState(
                    floorCenter.add(TEMPLE_HALF_LENGTH, TEMPLE_TOP_OFFSET, z),
                    palette.trim(),
                    Block.NOTIFY_ALL
            );
        }
    }

    private static void placeTempleChest(
            ServerWorld world,
            BlockPos chestPos,
            Direction playerFacing
    ) {
        Direction chestFacing = playerFacing.getAxis().isHorizontal()
                ? playerFacing.getOpposite()
                : Direction.NORTH;

        BlockState chestState = Blocks.CHEST.getDefaultState()
                .with(ChestBlock.FACING, chestFacing);

        world.setBlockState(chestPos, chestState, Block.NOTIFY_ALL);

        BlockEntity blockEntity = world.getBlockEntity(chestPos);

        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            return;
        }

        fillTempleChest(world, chest);
        chest.markDirty();
        world.updateListeners(chestPos, chestState, chestState, Block.NOTIFY_LISTENERS);
    }

    private static void fillTempleChest(ServerWorld world, ChestBlockEntity chest) {
        List<ItemStack> loot = new ArrayList<>();

        int foodRolls = 2 + world.random.nextInt(3);
        int fillerRolls = 3 + world.random.nextInt(4);

        for (int i = 0; i < foodRolls; i++) {
            loot.add(randomLootStack(world, FOOD_LOOT));
        }

        for (int i = 0; i < fillerRolls; i++) {
            loot.add(randomLootStack(world, FILLER_LOOT));
        }

        float emeraldRoll = world.random.nextFloat();

        if (emeraldRoll < 0.35f) {
            loot.add(new ItemStack(Items.EMERALD, emeraldRoll < 0.05f ? 2 : 1));
        }

        List<Integer> slots = new ArrayList<>(chest.size());

        for (int slot = 0; slot < chest.size(); slot++) {
            slots.add(slot);
        }

        shuffle(world, slots);

        for (int i = 0; i < loot.size() && i < slots.size(); i++) {
            chest.setStack(slots.get(i), loot.get(i));
        }
    }

    private static ItemStack randomLootStack(ServerWorld world, List<LootEntry> pool) {
        LootEntry selected = pool.get(world.random.nextInt(pool.size()));
        int count = world.random.nextBetween(selected.minCount(), selected.maxCount());
        return new ItemStack(selected.item(), count);
    }

    private static void placeChoiceSign(ServerWorld world, BlockPos signPos, Direction playerFacing) {
        int rotation = RotationPropertyHelper.fromDirection(playerFacing.getOpposite());
        BlockState signState = Blocks.OAK_SIGN.getDefaultState()
                .with(SignBlock.ROTATION, rotation);

        world.setBlockState(signPos, signState, Block.NOTIFY_ALL);

        BlockEntity blockEntity = world.getBlockEntity(signPos);

        if (!(blockEntity instanceof SignBlockEntity sign)) {
            return;
        }

        Text message = Text.literal("Pile ou face");
        SignText frontText = sign.getFrontText().withMessage(1, message);
        SignText backText = sign.getBackText().withMessage(1, message);

        sign.setText(frontText, true);
        sign.setText(backText, false);
        sign.markDirty();
        world.updateListeners(signPos, signState, signState, Block.NOTIFY_LISTENERS);
    }

    private static Optional<BlockPos> findChoiceCenter(
            ServerWorld world,
            BlockPos requestedCenter,
            Direction lineDirection
    ) {
        if (canPlaceChoice(world, requestedCenter, lineDirection)) {
            return Optional.of(requestedCenter.toImmutable());
        }

        for (int attempt = 0; attempt < 40; attempt++) {
            BlockPos candidate = requestedCenter.add(
                    world.random.nextBetween(-5, 5),
                    world.random.nextBetween(-2, 2),
                    world.random.nextBetween(-5, 5)
            );

            if (canPlaceChoice(world, candidate, lineDirection)) {
                return Optional.of(candidate.toImmutable());
            }
        }

        return Optional.empty();
    }

    private static boolean canPlaceChoice(
            ServerWorld world,
            BlockPos center,
            Direction lineDirection
    ) {
        BlockPos[] positions = {
                center,
                center.offset(lineDirection),
                center.offset(lineDirection.getOpposite())
        };

        for (BlockPos pos : positions) {
            if (pos.getY() <= world.getBottomY() || pos.getY() >= world.getTopY() - 1) {
                return false;
            }

            BlockState state = world.getBlockState(pos);

            if ((!state.isAir() && !state.isReplaceable()) || !world.getFluidState(pos).isEmpty()) {
                return false;
            }

            BlockPos floorPos = pos.down();

            if (!world.getBlockState(floorPos).isSideSolidFullSquare(world, floorPos, Direction.UP)) {
                return false;
            }
        }

        return true;
    }

    private static <T> void shuffle(ServerWorld world, List<T> values) {
        for (int i = values.size() - 1; i > 0; i--) {
            int j = world.random.nextInt(i + 1);
            T value = values.get(i);
            values.set(i, values.get(j));
            values.set(j, value);
        }
    }

    private record TemplePalette(
            BlockState floor,
            BlockState pillar,
            BlockState trim
    ) {
    }

    private record LootEntry(
            Item item,
            int minCount,
            int maxCount
    ) {
    }
}
