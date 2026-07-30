package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A bounded debug-stick-like tool that only cycles explicitly safe block-state properties. */
public final class BuilderWandItem extends Item {
    private static final List<String> PROPERTY_PRIORITY = List.of(
            "facing", "axis", "rotation", "half", "type", "hinge", "face"
    );
    private static final Set<String> DANGEROUS_PROPERTIES = Set.of(
            "powered", "lit", "open", "enabled", "triggered", "extended", "occupied",
            "age", "level", "moisture", "charges", "bites", "delay", "mode",
            "conditional", "inverted", "locked", "unstable", "signal_fire",
            "has_book", "crafting", "trial_spawner_state", "vault_state"
    );

    public BuilderWandItem(Settings settings) { super(settings); }

    private static boolean canUse(ServerPlayerEntity player) {
        return SkillTreeManager.hasBonus(player, SkillType.BUILDING, BonusType.BUILD_WAND);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) return ActionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)
                || !(context.getWorld() instanceof ServerWorld world)) return ActionResult.PASS;
        if (!canUse(player)) {
            player.sendMessage(Text.translatable("message.mythicrpg.builder_wand.locked")
                    .formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        BlockPos pos = context.getBlockPos();
        if (!world.isInBuildLimit(pos)
                || !world.getWorldBorder().contains(pos)
                || !world.canPlayerModifyAt(player, pos)) {
            player.sendMessage(Text.translatable("message.mythicrpg.builder_wand.protected")
                    .formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        BlockState state = world.getBlockState(pos);
        if (state.isAir() || state.getBlock().asItem() == Items.AIR || world.getBlockEntity(pos) != null
                || hasDangerousProperty(state)) {
            player.sendMessage(Text.translatable("message.mythicrpg.builder_wand.unsupported")
                    .formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        Property<?> property = firstSafeProperty(state);
        if (property == null) {
            player.sendMessage(Text.translatable("message.mythicrpg.builder_wand.no_property")
                    .formatted(Formatting.YELLOW), true);
            return ActionResult.FAIL;
        }

        BlockState changed = cycleProperty(state, property, player.isSneaking(), world, pos, player);
        if (changed == null || changed.equals(state)) {
            player.sendMessage(Text.translatable("message.mythicrpg.builder_wand.no_valid_state")
                    .formatted(Formatting.YELLOW), true);
            return ActionResult.FAIL;
        }
        if (!world.setBlockState(pos, changed, Block.NOTIFY_ALL)) {
            player.sendMessage(Text.translatable("message.mythicrpg.builder_wand.failed")
                    .formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        ItemStack wand = context.getStack();
        if (!player.isCreative()) {
            wand.damage(1, player, LivingEntity.getSlotForHand(context.getHand()));
        }
        player.sendMessage(Text.translatable(
                "message.mythicrpg.builder_wand.changed",
                Text.translatable(state.getBlock().getTranslationKey()),
                property.getName(),
                serializedValue(changed, property)
        ).formatted(Formatting.AQUA), true);
        return ActionResult.CONSUME;
    }

    private static boolean hasDangerousProperty(BlockState state) {
        for (String name : DANGEROUS_PROPERTIES) {
            if (state.getBlock().getStateManager().getProperty(name) != null) return true;
        }
        return false;
    }

    private static Property<?> firstSafeProperty(BlockState state) {
        for (String name : PROPERTY_PRIORITY) {
            Property<?> property = state.getBlock().getStateManager().getProperty(name);
            if (property != null && property.getValues().size() > 1) return property;
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState cycleProperty(
            BlockState state,
            Property<?> rawProperty,
            boolean backwards,
            ServerWorld world,
            BlockPos pos,
            ServerPlayerEntity player
    ) {
        Property property = rawProperty;
        List<Comparable> values = new ArrayList<>(property.getValues());
        Comparable current = (Comparable) state.get(property);
        int currentIndex = values.indexOf(current);
        int direction = backwards ? -1 : 1;
        for (int step = 1; step <= values.size(); step++) {
            int index = Math.floorMod(currentIndex + direction * step, values.size());
            Comparable next = values.get(index);
            if ("type".equals(property.getName()) && "double".equals(property.name(next))) continue;
            BlockState candidate = state.with(property, next);
            if (candidate.canPlaceAt(world, pos)
                    && world.canPlace(candidate, pos, ShapeContext.of(player))) {
                return candidate;
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String serializedValue(BlockState state, Property<?> rawProperty) {
        Property property = rawProperty;
        return property.name((Comparable) state.get(property));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.mythicrpg.builder_wand.use").formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.mythicrpg.builder_wand.reverse").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mythicrpg.builder_wand.safe_only").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("tooltip.mythicrpg.builder_wand.durability", stack.getMaxDamage())
                .formatted(Formatting.DARK_AQUA));
    }
}
