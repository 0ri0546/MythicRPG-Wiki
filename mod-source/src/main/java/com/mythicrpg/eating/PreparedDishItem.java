package com.mythicrpg.eating;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import com.mythicrpg.core.ClientSkillUnlockSnapshot;
import com.mythicrpg.core.SkillType;

import java.util.List;
import java.util.Locale;

public final class PreparedDishItem extends Item {
    public PreparedDishItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient() && Math.floorMod(entity.age + slot, 20) == 0) {
            EatingPreservationManager.PreservationMode mode = entity instanceof ServerPlayerEntity player
                    ? EatingPreservationManager.modeForPlayer(player)
                    : EatingPreservationManager.PreservationMode.NONE;
            boolean changed = PreparedDishData.updatePreservation(stack, world.getTime(), mode);
            if (changed && entity instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.getInventory().markDirty();
            }
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack dishStack = user.getStackInHand(hand);
        Hand otherHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack otherStack = user.getStackInHand(otherHand);

        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            PreparedDishData.updatePreservation(
                    dishStack,
                    world.getTime(),
                    EatingPreservationManager.modeForPlayer(serverPlayer)
            );
        }

        if (otherStack.getItem() instanceof ServingPlateItem) {
            if (world.isClient()) {
                return TypedActionResult.success(dishStack);
            }
            if (user instanceof ServerPlayerEntity serverPlayer
                    && ServingPlateItem.serveDish(serverPlayer, otherStack, dishStack)) {
                return TypedActionResult.success(dishStack);
            }
            return TypedActionResult.fail(dishStack);
        }

        boolean canConsume = user.getHungerManager().isNotFull()
                || (world.isClient()
                ? ClientSkillUnlockSnapshot.isUnlocked(SkillType.EATING, 9)
                : user instanceof ServerPlayerEntity serverPlayer
                  && EatingPerks.canEatWhenFull(serverPlayer));

        if (!canConsume) {
            if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(
                        Text.translatable("message.mythicrpg.eating.full_hunger_locked")
                                .formatted(Formatting.RED),
                        true
                );
            }

            return TypedActionResult.fail(dishStack);
        }

        user.setCurrentHand(hand);
        return TypedActionResult.consume(dishStack);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!(user instanceof PlayerEntity player)) {
            return super.finishUsing(stack, world, user);
        }

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            PreparedDishData.updatePreservation(
                    stack,
                    world.getTime(),
                    EatingPreservationManager.modeForPlayer(serverPlayer)
            );
        }
        PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(stack, world.getTime());
        ItemStack consumedStack = stack.copyWithCount(1);
        ItemStack remainder = super.finishUsing(stack, world, user);

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            PreparedDishConsumption.consume(serverPlayer, dish, consumedStack);
        }

        if (player.getAbilities().creativeMode) {
            return stack;
        }
        return remainder.isEmpty() ? new ItemStack(Items.BOWL) : remainder;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();

        if (entity instanceof HostileEntity hostile) {
            if (world.isClient()) {
                boolean validGift = PreparedDishData.read(stack)
                        .filter(dish -> !dish.dubious())
                        .filter(dish -> dish.expiresAt() <= 0L || world.getTime() < dish.expiresAt())
                        .isPresent();
                if (validGift) {
                    return ActionResult.SUCCESS;
                }
            } else if (user instanceof ServerPlayerEntity serverPlayer) {
                PreparedDishData.updatePreservation(
                        stack,
                        world.getTime(),
                        EatingPreservationManager.modeForPlayer(serverPlayer)
                );
                ActionResult offered = EatingAdvancedManager.tryOfferDish(serverPlayer, hostile, stack);
                if (offered.isAccepted()) {
                    return offered;
                }
            }
        }

        if (world.isClient()) {
            return PreparedDishData.read(stack)
                    .filter(dish -> dish.dubious()
                            || dish.expiresAt() > 0L && world.getTime() >= dish.expiresAt())
                    .filter(dish -> DubiousDishInteractions.canHeal(user, entity))
                    .isPresent()
                    ? ActionResult.SUCCESS
                    : ActionResult.PASS;
        }
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        PreparedDishData.updatePreservation(
                stack,
                world.getTime(),
                EatingPreservationManager.modeForPlayer(serverPlayer)
        );
        PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(stack, world.getTime());
        if (!dish.dubious() || !DubiousDishInteractions.canHeal(user, entity)) {
            return ActionResult.PASS;
        }

        DubiousDishInteractions.healCompanion(world, entity);
        if (!user.isCreative()) {
            stack.decrement(1);
            giveBowl(user);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockState state = world.getBlockState(context.getBlockPos());
        if (!state.isOf(Blocks.COMPOSTER)) {
            return super.useOnBlock(context);
        }
        if (world.isClient()) {
            return PreparedDishData.read(context.getStack())
                    .filter(dish -> dish.dubious()
                            || dish.expiresAt() > 0L && world.getTime() >= dish.expiresAt())
                    .isPresent()
                    ? ActionResult.SUCCESS
                    : ActionResult.PASS;
        }
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        PreparedDishData.updatePreservation(
                context.getStack(),
                world.getTime(),
                EatingPreservationManager.modeForPlayer(player)
        );
        PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(
                context.getStack(),
                world.getTime()
        );
        if (!dish.dubious() || !EatingPerks.canCompostDubiousDish(player)) {
            return ActionResult.PASS;
        }

        if (!DubiousDishInteractions.compost(
                player,
                world,
                context.getBlockPos(),
                state
        )) {
            return ActionResult.PASS;
        }
        if (!player.isCreative()) {
            context.getStack().decrement(1);
            giveBowl(player);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        PreparedDishData.read(stack).ifPresent(dish -> {
            SignatureDishData.read(stack).ifPresent(signature -> tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.signature_dish.bonus",
                    Text.translatable("signature_bonus.mythicrpg." + signature.bonus().id()),
                    signature.durationTicks() / 20
            ).formatted(Formatting.LIGHT_PURPLE)));
            tooltip.add(dish.category().displayName().copy().formatted(Formatting.GRAY));
            if (!dish.dubious()) {
                tooltip.add(Text.translatable(
                        "tooltip.mythicrpg.prepared_dish.rarity",
                        dish.rarity().displayName()
                ).formatted(Formatting.GRAY));
                tooltip.add(Text.translatable(
                        "tooltip.mythicrpg.prepared_dish.saturation",
                        String.format(Locale.ROOT, "%.1f", dish.rarity().saturation())
                ).formatted(Formatting.GOLD));
                if (PreparedDishData.isPreserved(dish)) {
                    tooltip.add(Text.translatable("tooltip.mythicrpg.prepared_dish.preserved")
                            .formatted(Formatting.AQUA));
                }
            } else {
                tooltip.add(Text.translatable("tooltip.mythicrpg.prepared_dish.dubious")
                        .formatted(Formatting.DARK_GREEN));
                tooltip.add(Text.translatable("tooltip.mythicrpg.prepared_dish.dubious_companion")
                        .formatted(Formatting.GRAY));
            }
        });
    }

    private static void giveBowl(PlayerEntity player) {
        ItemStack bowl = new ItemStack(Items.BOWL);
        if (!player.getInventory().insertStack(bowl) && !bowl.isEmpty()) {
            player.dropItem(bowl, false);
        }
    }
}
