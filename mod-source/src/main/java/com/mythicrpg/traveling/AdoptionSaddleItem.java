package com.mythicrpg.traveling;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.List;

public final class AdoptionSaddleItem extends Item {
    private final LandMountType mountType;

    public AdoptionSaddleItem(LandMountType mountType, Settings settings) {
        super(settings);
        this.mountType = mountType;
    }

    public LandMountType getMountType() {
        return mountType;
    }

    @Override
    public ActionResult useOnEntity(
            ItemStack stack,
            PlayerEntity user,
            LivingEntity entity,
            Hand hand
    ) {
        if (!(entity instanceof MobEntity mob)) {
            return ActionResult.PASS;
        }

        if (!mountType.matches(mob)) {
            if (!user.getWorld().isClient) {
                LandMountManager.sendFeedback(user, Text.translatable(
                        "message.mythicrpg.land_mount.wrong_saddle",
                        mountType.displayName()
                ).formatted(Formatting.RED));
            }

            return ActionResult.FAIL;
        }

        if (user.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        return LandMountManager.tryAdopt(user, mob, stack, mountType);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        tooltip.add(Text.translatable(
                        "tooltip.mythicrpg.adoption_saddle.target",
                        mountType.displayName()
                )
                .formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.mythicrpg.adoption_saddle.health")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mythicrpg.adoption_saddle.use")
                .formatted(Formatting.GREEN));
        tooltip.add(Text.translatable(
                        "tooltip.mythicrpg.adoption_saddle.distance",
                        Math.round(MountSaddleData.getDistance(stack))
                )
                .formatted(Formatting.DARK_GRAY, Formatting.ITALIC));

        if (mountType.isFlying()) {
            tooltip.add(Text.translatable("tooltip.mythicrpg.adoption_saddle.flying_controls")
                    .formatted(Formatting.YELLOW));
        }
    }
}
