package com.mythicrpg.traveling;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;
import java.util.function.Predicate;

public final class TravelerBoatItem extends Item {

    private static final Predicate<Entity> RIDERS =
            EntityPredicates.EXCEPT_SPECTATOR.and(Entity::canHit);

    public TravelerBoatItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(
            World world,
            PlayerEntity player,
            Hand hand
    ) {
        ItemStack stack = player.getStackInHand(hand);
        BlockHitResult hitResult = raycast(
                world,
                player,
                RaycastContext.FluidHandling.ANY
        );

        if (hitResult.getType() == HitResult.Type.MISS) {
            return TypedActionResult.pass(stack);
        }

        Vec3d look = player.getRotationVec(1.0F);
        List<Entity> blockingEntities = world.getOtherEntities(
                player,
                player.getBoundingBox().stretch(look.multiply(5.0D)).expand(1.0D),
                RIDERS
        );

        if (!blockingEntities.isEmpty()) {
            Vec3d eyePos = player.getEyePos();

            for (Entity entity : blockingEntities) {
                Box box = entity.getBoundingBox().expand(entity.getTargetingMargin());

                if (box.contains(eyePos)) {
                    return TypedActionResult.pass(stack);
                }
            }
        }

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return TypedActionResult.pass(stack);
        }

        Vec3d spawnPos = hitResult.getPos();
        TravelerBoatEntity boat = new TravelerBoatEntity(
                world,
                spawnPos.x,
                spawnPos.y,
                spawnPos.z
        );
        boat.setYaw(player.getYaw());

        if (!world.isSpaceEmpty(boat, boat.getBoundingBox())) {
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            if (world instanceof ServerWorld serverWorld) {
                EntityType.copier(serverWorld, stack, player).accept(boat);
            }

            world.spawnEntity(boat);
            world.emitGameEvent(
                    player,
                    GameEvent.ENTITY_PLACE,
                    hitResult.getPos()
            );
            stack.decrementUnlessCreative(1, player);
        }

        player.incrementStat(Stats.USED.getOrCreateStat(this));
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        tooltip.add(Text.translatable("tooltip.mythicrpg.traveler_boat.speed")
                .formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.mythicrpg.traveler_boat.use")
                .formatted(Formatting.GRAY));
    }
}
