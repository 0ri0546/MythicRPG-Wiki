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
import net.minecraft.world.World;

import java.util.Optional;

public class HeartOfTheBeamItem extends LegendaryTooltipItem {
    private static final double RANGE = 12.0;
    private static final int COOLDOWN_TICKS = 20 * 20;

    public HeartOfTheBeamItem(Settings settings) {
        super(settings, "tooltip.mythicrpg.heart_of_the_beam.flavor");
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

        BaronLegendaryItemEffects.startBeam(player, target.get().entity());
        BaronLegendaryItemEffects.spawnBeamTargetMarker(serverWorld, target.get().entity(), true);
        serverWorld.spawnParticles(ParticleTypes.FLAME, target.get().hitPos().x, target.get().hitPos().y, target.get().hitPos().z, 12, 0.2, 0.2, 0.2, 0.01);
        serverWorld.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.65f, 1.55f);
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

        return TypedActionResult.success(stack);
    }
}
