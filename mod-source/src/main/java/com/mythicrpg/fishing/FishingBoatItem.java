
package com.mythicrpg.fishing;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;


public final class FishingBoatItem extends Item {
    public FishingBoatItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        BlockHitResult hit = raycast(world, player, RaycastContext.FluidHandling.ANY);
        if (hit.getType() != HitResult.Type.BLOCK
                || !world.getFluidState(hit.getBlockPos()).isIn(FluidTags.WATER)) {
            return TypedActionResult.pass(stack);
        }

        Vec3d spawn = hit.getPos();
        FishingBoatEntity boat = new FishingBoatEntity(world, spawn.x, spawn.y, spawn.z);
        boat.setYaw(player.getYaw());
        if (!world.isSpaceEmpty(boat, boat.getBoundingBox())) {
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            if (world instanceof ServerWorld serverWorld) {
                EntityType.copier(serverWorld, stack, player).accept(boat);
            }
            world.spawnEntity(boat);
            world.emitGameEvent(player, GameEvent.ENTITY_PLACE, hit.getPos());
            stack.decrementUnlessCreative(1, player);
        }

        player.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(stack, world.isClient());
    }
}
