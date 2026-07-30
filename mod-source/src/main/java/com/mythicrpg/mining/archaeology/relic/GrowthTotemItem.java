package com.mythicrpg.mining.archaeology.relic;

import com.mythicrpg.core.ModBlocks;
import com.mythicrpg.mining.archaeology.polish.ArchaeologyPolishEffects;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public final class GrowthTotemItem extends LeveledRelicItem {

    private static final Vector3f PREVIEW_COLOR = new Vector3f(0.32F, 1.00F, 0.42F);

    public GrowthTotemItem(Settings settings) {
        super(settings, "tooltip.mythicrpg.growth_totem.description");
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        BlockPos pos = context.getBlockPos().offset(context.getSide());
        if (!context.getWorld().getBlockState(pos).isReplaceable()
                || !context.getWorld().getBlockState(pos.up()).isReplaceable()) {
            return ActionResult.FAIL;
        }

        int level = RelicItemData.getLevel(context.getStack()).value();
        var lower = ModBlocks.GROWTH_TOTEM_BLOCK.getDefaultState()
                .with(GrowthTotemBlock.HALF, DoubleBlockHalf.LOWER);
        var upper = lower.with(GrowthTotemBlock.HALF, DoubleBlockHalf.UPPER);
        context.getWorld().setBlockState(pos, lower, 3);
        context.getWorld().setBlockState(pos.up(), upper, 3);

        if (context.getWorld().getBlockEntity(pos) instanceof GrowthTotemBlockEntity blockEntity) {
            blockEntity.setLevel(level);
        }
        context.getStack().decrementUnlessCreative(1, context.getPlayer());

        if (context.getWorld() instanceof ServerWorld world) {
            int radius = GrowthTotemManager.radiusForLevel(level);
            ArchaeologyPolishEffects.spawnHorizontalRing(
                    world,
                    new DustParticleEffect(PREVIEW_COLOR, 0.65F),
                    Vec3d.ofCenter(pos),
                    radius,
                    Math.min(72, Math.max(24, radius * 7)),
                    -0.34
            );
            if (context.getPlayer() instanceof ServerPlayerEntity player) {
                int crops = GrowthTotemBlockEntity.countCompatibleCrops(world, pos, radius);
                player.sendMessage(
                        Text.translatable(
                                "message.mythicrpg.growth_totem.placed",
                                radius,
                                crops
                        ).formatted(crops > 0 ? Formatting.GREEN : Formatting.YELLOW),
                        true
                );
            }
        }

        return ActionResult.CONSUME;
    }
}
