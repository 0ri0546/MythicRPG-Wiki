package com.mythicrpg.fighting.items;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

public class SpiderWandItem extends LegendaryTooltipItem {
    private static final double RANGE = 20.0;
    private static final int COOLDOWN_TICKS = 20 * 20;

    public SpiderWandItem(Settings settings) {
        super(settings, "tooltip.mythicrpg.spider_wand.flavor");
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity player) || !(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.pass(stack);
        }

        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (!player.isCreative() && !hasString(player)) {
            return TypedActionResult.fail(stack);
        }

        Optional<BlockPos> targetPos = findCobwebPosition(player);

        if (targetPos.isEmpty()) {
            return TypedActionResult.fail(stack);
        }

        BlockPos pos = targetPos.get();

        if (!canPlaceCobweb(serverWorld, pos)) {
            return TypedActionResult.fail(stack);
        }

        if (!player.isCreative()) {
            consumeString(player);
        }

        serverWorld.setBlockState(pos, Blocks.COBWEB.getDefaultState());
        spawnLineParticles(serverWorld, player.getCameraPosVec(1.0f), Vec3d.ofCenter(pos));
        serverWorld.playSound(null, pos, SoundEvents.ENTITY_SPIDER_AMBIENT, SoundCategory.PLAYERS, 0.55f, 1.6f);
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

        return TypedActionResult.success(stack);
    }

    private static Optional<BlockPos> findCobwebPosition(ServerPlayerEntity player) {
        Optional<BaronItemTargeting.EntityTarget> entityTarget = BaronItemTargeting.findLivingTarget(player, RANGE);
        BlockHitResult blockHit = BaronItemTargeting.raycastBlock(player, RANGE);

        if (entityTarget.isPresent()) {
            return Optional.of(entityTarget.get().entity().getBlockPos());
        }

        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }

        return Optional.of(blockHit.getBlockPos().offset(blockHit.getSide()));
    }

    private static boolean canPlaceCobweb(ServerWorld world, BlockPos pos) {
        if (!world.isInBuildLimit(pos)) {
            return false;
        }

        return world.getBlockState(pos).isReplaceable() && world.getFluidState(pos).isEmpty();
    }

    private static boolean hasString(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(Items.STRING)) {
                return true;
            }
        }

        return false;
    }

    private static void consumeString(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);

            if (stack.isOf(Items.STRING)) {
                stack.decrement(1);
                player.getInventory().markDirty();
                return;
            }
        }
    }

    private static void spawnLineParticles(ServerWorld world, Vec3d start, Vec3d end) {
        Vec3d delta = end.subtract(start);
        int points = Math.max(4, (int) Math.round(delta.length() * 2.0));

        for (int i = 1; i <= points; i++) {
            Vec3d pos = start.add(delta.multiply(i / (double) points));
            world.spawnParticles(ParticleTypes.POOF, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }
}
