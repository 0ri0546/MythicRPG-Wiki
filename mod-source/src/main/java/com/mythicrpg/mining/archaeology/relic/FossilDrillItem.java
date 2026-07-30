package com.mythicrpg.mining.archaeology.relic;

import com.mythicrpg.core.ModBlocks;
import com.mythicrpg.mining.archaeology.polish.ArchaeologyPolishEffects;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FossilDrillItem extends LeveledRelicItem {

    private static final int COAL_PER_BLOCK = 16;

    public FossilDrillItem(Settings settings) {
        super(settings, "tooltip.mythicrpg.fossil_drill.description");
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)
                || !(context.getWorld() instanceof ServerWorld world)) {
            return ActionResult.PASS;
        }

        long cooldownMillis = FossilDrillManager.cooldownRemainingMillis(
                world.getServer(),
                player.getUuid()
        );
        if (cooldownMillis > 0L) {
            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.fossil_drill.cooldown",
                            Math.max(1L, (cooldownMillis + 999L) / 1000L)
                    ).formatted(Formatting.RED),
                    true
            );
            return ActionResult.FAIL;
        }

        if (FossilDrillManager.hasActive(world.getServer(), player.getUuid())) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.fossil_drill.already_active")
                            .formatted(Formatting.RED),
                    true
            );
            return ActionResult.FAIL;
        }

        BlockPos start = context.getBlockPos();
        if (!isOre(world.getBlockState(start))) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.fossil_drill.invalid_ore")
                            .formatted(Formatting.RED),
                    true
            );
            return ActionResult.FAIL;
        }

        RelicLevel level = RelicItemData.getLevel(context.getStack());
        int maxBlocks = level.value() + 3;
        VeinScan scan = findVein(world, start, maxBlocks);
        int coalRequired = scan.positions().size() * COAL_PER_BLOCK;
        int availableCoal = countCoal(player);
        if (availableCoal < coalRequired) {
            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.fossil_drill.coal_missing",
                            coalRequired,
                            availableCoal
                    ).formatted(Formatting.RED),
                    true
            );
            return ActionResult.FAIL;
        }

        BlockPos placePos = start.offset(context.getSide());
        if (!world.getBlockState(placePos).isReplaceable()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.fossil_drill.no_space")
                            .formatted(Formatting.RED),
                    true
            );
            return ActionResult.FAIL;
        }

        long durationTicks = 0L;
        for (BlockPos orePos : scan.positions()) {
            durationTicks += secondsFor(world.getBlockState(orePos)) * 20L;
        }

        if (!FossilDrillManager.claim(world, player.getUuid(), placePos)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.fossil_drill.already_active")
                            .formatted(Formatting.RED),
                    true
            );
            return ActionResult.FAIL;
        }

        boolean placed = world.setBlockState(
                placePos,
                ModBlocks.FOSSIL_DRILL_BLOCK.getDefaultState(),
                3
        );
        long startAt = world.getTime();
        if (!placed
                || !(world.getBlockEntity(placePos) instanceof FossilDrillBlockEntity blockEntity)) {
            FossilDrillManager.remove(world, player.getUuid(), placePos);
            if (placed) {
                world.removeBlock(placePos, false);
            }
            player.sendMessage(
                    Text.translatable("message.mythicrpg.fossil_drill.no_space")
                            .formatted(Formatting.RED),
                    true
            );
            return ActionResult.FAIL;
        }

        blockEntity.configure(
                scan.positions(),
                startAt,
                startAt + durationTicks,
                level.value() + 1,
                player.getUuid()
        );
        removeCoal(player, coalRequired);

        // 9 / 8 / 7 / 6 / 5 minutes, persisted independently from the ItemStack.
        FossilDrillManager.startCooldown(player, (10 - level.value()) * 1200);
        world.playSound(
                null,
                placePos,
                SoundEvents.BLOCK_PISTON_EXTEND,
                SoundCategory.BLOCKS,
                0.55F,
                0.78F
        );
        world.spawnParticles(
                net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,
                placePos.getX() + 0.5,
                placePos.getY() + 0.65,
                placePos.getZ() + 0.5,
                6,
                0.2,
                0.18,
                0.2,
                0.025
        );

        player.sendMessage(
                Text.translatable(
                        scan.capped()
                                ? "message.mythicrpg.fossil_drill.started_capped"
                                : "message.mythicrpg.fossil_drill.started",
                        scan.positions().size(),
                        coalRequired,
                        ArchaeologyPolishEffects.formatTicks(durationTicks),
                        level.value() + 1
                ).formatted(scan.capped() ? Formatting.YELLOW : Formatting.GREEN),
                true
        );
        return ActionResult.CONSUME;
    }

    public static boolean isOre(BlockState state) {
        return state.isIn(BlockTags.COAL_ORES)
                || state.isIn(BlockTags.COPPER_ORES)
                || state.isIn(BlockTags.IRON_ORES)
                || state.isIn(BlockTags.GOLD_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES)
                || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES)
                || state.isIn(BlockTags.EMERALD_ORES);
    }

    private static int secondsFor(BlockState state) {
        if (state.isIn(BlockTags.COAL_ORES)) return 20;
        if (state.isIn(BlockTags.COPPER_ORES)) return 25;
        if (state.isIn(BlockTags.IRON_ORES) || state.isIn(BlockTags.REDSTONE_ORES)) return 30;
        if (state.isIn(BlockTags.LAPIS_ORES)) return 35;
        if (state.isIn(BlockTags.GOLD_ORES)) return 40;
        if (state.isIn(BlockTags.EMERALD_ORES)) return 50;
        return 60;
    }

    private static VeinScan findVein(ServerWorld world, BlockPos start, int maximum) {
        List<BlockPos> positions = new ArrayList<>(maximum);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        boolean capped = false;
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!visited.add(current) || !isOre(world.getBlockState(current))) {
                continue;
            }
            if (positions.size() >= maximum) {
                capped = true;
                break;
            }

            positions.add(current.toImmutable());
            for (Direction direction : Direction.values()) {
                queue.addLast(current.offset(direction));
            }
        }
        return new VeinScan(List.copyOf(positions), capped);
    }

    private static int countCoal(PlayerEntity player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(Items.COAL)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeCoal(PlayerEntity player, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().size() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isOf(Items.COAL)) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.decrement(removed);
            remaining -= removed;
        }
    }

    private record VeinScan(List<BlockPos> positions, boolean capped) {
    }
}
