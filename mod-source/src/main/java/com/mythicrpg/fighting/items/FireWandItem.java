package com.mythicrpg.fighting.items;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;

public class FireWandItem extends LegendaryTooltipItem {
    private static final double RANGE = 24.0;
    private static final int FIRE_SECONDS = 5;
    private static final int COOLDOWN_TICKS = 20 * 10;

    public FireWandItem(Settings settings) {
        super(settings, "tooltip.mythicrpg.fire_wand.flavor");
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

        Optional<BaronItemTargeting.EntityTarget> target = BaronItemTargeting.findLivingTarget(player, RANGE);

        if (target.isEmpty()) {
            return TypedActionResult.fail(stack);
        }

        LivingEntity entity = target.get().entity();
        entity.setOnFireFor(FIRE_SECONDS);

        spawnLineParticles(serverWorld, player.getCameraPosVec(1.0f), target.get().hitPos());
        serverWorld.playSound(null, player.getBlockPos(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.75f, 1.35f);
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

        return TypedActionResult.success(stack);
    }

    private static void spawnLineParticles(ServerWorld world, Vec3d start, Vec3d end) {
        Vec3d delta = end.subtract(start);
        int points = Math.max(4, (int) Math.round(delta.length() * 2.0));

        for (int i = 1; i <= points; i++) {
            Vec3d pos = start.add(delta.multiply(i / (double) points));
            world.spawnParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }
}
