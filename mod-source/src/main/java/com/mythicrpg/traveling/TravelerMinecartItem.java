package com.mythicrpg.traveling;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;

public final class TravelerMinecartItem extends Item {

    public TravelerMinecartItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (!state.isIn(BlockTags.RAILS)) {
            return ActionResult.FAIL;
        }

        ItemStack stack = context.getStack();

        if (world instanceof ServerWorld serverWorld) {
            RailShape railShape = state.getBlock() instanceof AbstractRailBlock railBlock
                    ? state.get(railBlock.getShapeProperty())
                    : RailShape.NORTH_SOUTH;

            double verticalOffset = railShape.isAscending() ? 0.5D : 0.0D;

            TravelerMinecartEntity minecart = new TravelerMinecartEntity(
                    serverWorld,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.0625D + verticalOffset,
                    pos.getZ() + 0.5D
            );

            EntityType.copier(serverWorld, stack, context.getPlayer()).accept(minecart);
            serverWorld.spawnEntity(minecart);
            serverWorld.emitGameEvent(
                    GameEvent.ENTITY_PLACE,
                    pos,
                    GameEvent.Emitter.of(
                            context.getPlayer(),
                            serverWorld.getBlockState(pos.down())
                    )
            );
        }

        stack.decrement(1);
        return ActionResult.success(world.isClient);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        tooltip.add(Text.translatable("tooltip.mythicrpg.traveler_minecart.speed")
                .formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.mythicrpg.traveler_minecart.use")
                .formatted(Formatting.GRAY));
    }
}
