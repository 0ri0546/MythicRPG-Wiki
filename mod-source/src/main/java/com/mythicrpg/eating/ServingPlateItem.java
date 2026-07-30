package com.mythicrpg.eating;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import com.mythicrpg.core.ClientSkillUnlockSnapshot;
import com.mythicrpg.core.SkillType;

import java.util.List;
import java.util.Optional;

public final class ServingPlateItem extends Item {
    private final int capacity;

    public ServingPlateItem(int capacity, Settings settings) {
        super(settings);
        this.capacity = Math.max(1, Math.min(5, capacity));
    }

    public int capacity() {
        return capacity;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient() && Math.floorMod(entity.age + slot, 20) == 0) {
            EatingPreservationManager.PreservationMode mode = entity instanceof ServerPlayerEntity player
                    ? EatingPreservationManager.modeForPlayer(player)
                    : EatingPreservationManager.PreservationMode.NONE;
            boolean changed = ServingPlateData.updatePreservation(stack, world.getTime(), mode);
            if (changed && entity instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.getInventory().markDirty();
            }
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack plate = user.getStackInHand(hand);
        Hand otherHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        ItemStack otherStack = user.getStackInHand(otherHand);

        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            ServingPlateData.updatePreservation(
                    plate,
                    world.getTime(),
                    EatingPreservationManager.modeForPlayer(serverPlayer)
            );
        }

        if (PreparedDishData.read(otherStack).isPresent()) {
            if (world.isClient()) {
                return TypedActionResult.success(plate);
            }
            if (user instanceof ServerPlayerEntity serverPlayer
                    && serveDish(serverPlayer, plate, otherStack)) {
                return TypedActionResult.success(plate);
            }
            return TypedActionResult.fail(plate);
        }

        if (user.isSneaking()) {
            if (world.isClient()) {
                return TypedActionResult.success(plate);
            }
            int selected = ServingPlateData.cycle(plate);
            if (user instanceof ServerPlayerEntity serverPlayer) {
                if (selected < 0) {
                    serverPlayer.sendMessage(
                            Text.translatable("message.mythicrpg.eating.plate_empty")
                                    .formatted(Formatting.RED),
                            true
                    );
                } else {
                    ServingPlateData.selectedPortion(plate)
                            .flatMap(PreparedDishData::read)
                            .ifPresent(dish -> serverPlayer.sendMessage(
                                    Text.translatable(
                                            "message.mythicrpg.eating.plate_selected",
                                            Text.translatable("dish.mythicrpg." + dish.recipeId())
                                    ).formatted(Formatting.AQUA),
                                    true
                            ));
                }
            }
            return TypedActionResult.success(plate);
        }

        if (ServingPlateData.selectedPortion(plate).isEmpty()) {
            if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(
                        Text.translatable("message.mythicrpg.eating.plate_empty")
                                .formatted(Formatting.RED),
                        true
                );
            }
            return TypedActionResult.fail(plate);
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

            return TypedActionResult.fail(plate);
        }

        user.setCurrentHand(hand);
        return TypedActionResult.consume(plate);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        World world = user.getWorld();

        if (entity instanceof HostileEntity hostile) {
            if (world.isClient()) {
                boolean validGift = ServingPlateData.selectedPortion(stack)
                        .flatMap(PreparedDishData::read)
                        .filter(dish -> !dish.dubious())
                        .filter(dish -> dish.expiresAt() <= 0L || world.getTime() < dish.expiresAt())
                        .isPresent();
                if (validGift) {
                    return ActionResult.SUCCESS;
                }
            } else if (user instanceof ServerPlayerEntity serverPlayer) {
                ServingPlateData.updatePreservation(
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
            return ServingPlateData.selectedPortion(stack)
                    .flatMap(PreparedDishData::read)
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

        ServingPlateData.updatePreservation(
                stack,
                world.getTime(),
                EatingPreservationManager.modeForPlayer(serverPlayer)
        );
        Optional<ItemStack> selected = ServingPlateData.selectedPortion(stack);
        if (selected.isEmpty()) {
            return ActionResult.PASS;
        }

        PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(
                selected.get(),
                world.getTime()
        );
        if (!dish.dubious() || !DubiousDishInteractions.canHeal(user, entity)) {
            return ActionResult.PASS;
        }

        DubiousDishInteractions.healCompanion(world, entity);
        if (!user.isCreative()) {
            ServingPlateData.removeSelectedPortion(stack);
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
            return ServingPlateData.selectedPortion(context.getStack())
                    .flatMap(PreparedDishData::read)
                    .filter(dish -> dish.dubious()
                            || dish.expiresAt() > 0L && world.getTime() >= dish.expiresAt())
                    .isPresent()
                    ? ActionResult.SUCCESS
                    : ActionResult.PASS;
        }
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        ItemStack plate = context.getStack();
        ServingPlateData.updatePreservation(
                plate,
                world.getTime(),
                EatingPreservationManager.modeForPlayer(player)
        );
        Optional<ItemStack> selected = ServingPlateData.selectedPortion(plate);
        if (selected.isEmpty()) {
            return ActionResult.PASS;
        }

        PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(
                selected.get(),
                world.getTime()
        );
        if (!dish.dubious() || !DubiousDishInteractions.compost(
                player,
                world,
                context.getBlockPos(),
                state
        )) {
            return ActionResult.PASS;
        }

        if (!player.isCreative()) {
            ServingPlateData.removeSelectedPortion(plate);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            ServingPlateData.updatePreservation(
                    stack,
                    world.getTime(),
                    EatingPreservationManager.modeForPlayer(player)
            );
            Optional<ItemStack> selected = player.isCreative()
                    ? ServingPlateData.selectedPortion(stack).map(portion -> portion.copyWithCount(1))
                    : ServingPlateData.removeSelectedPortion(stack);

            selected.ifPresent(portion -> {
                PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(portion, world.getTime());
                PreparedDishConsumption.consumeFromPlate(player, dish, portion);
                world.playSound(
                        null,
                        player.getBlockPos(),
                        SoundEvents.ENTITY_GENERIC_EAT,
                        SoundCategory.PLAYERS,
                        0.8F,
                        0.9F + world.random.nextFloat() * 0.2F
                );
            });
        }
        return stack;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.EAT;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        int count = ServingPlateData.count(stack);
        int selected = ServingPlateData.selectedIndex(stack);
        tooltip.add(Text.translatable(
                "tooltip.mythicrpg.serving_plate.portions",
                count,
                capacity
        ).formatted(Formatting.GOLD));

        List<ItemStack> contents = ServingPlateData.contents(stack);
        for (int index = 0; index < contents.size(); index++) {
            ItemStack portion = contents.get(index);
            if (portion.isEmpty()) {
                continue;
            }

            int portionIndex = index;

            PreparedDishData.read(portion).ifPresent(dish -> tooltip.add(
                    Text.literal(portionIndex == selected ? "▶ " : "• ")
                            .append(portion.getName())
                            .formatted(portionIndex == selected ? Formatting.AQUA : Formatting.GRAY)
            ));
        }

        ServingPlateData.selectedPortion(stack).ifPresent(portion ->
                PreparedDishData.read(portion).ifPresent(dish -> {
                    FoodComponent food = portion.get(DataComponentTypes.FOOD);
                    float saturation = dish.dubious() && food != null
                            ? food.saturation()
                            : dish.rarity().saturation();
                    tooltip.add(Text.translatable(
                            "tooltip.mythicrpg.food.saturation",
                            String.format(java.util.Locale.ROOT, "%.1f", saturation)
                    ).formatted(Formatting.GOLD));
                })
        );

        tooltip.add(Text.translatable("tooltip.mythicrpg.serving_plate.controls")
                .formatted(Formatting.DARK_AQUA));
        tooltip.add(Text.translatable("tooltip.mythicrpg.serving_plate.serve")
                .formatted(Formatting.GREEN));
    }

    public static boolean serveDish(ServerPlayerEntity player, ItemStack plate, ItemStack dishStack) {
        if (!(plate.getItem() instanceof ServingPlateItem) || PreparedDishData.read(dishStack).isEmpty()) {
            return false;
        }
        if (!ServingPlateData.hasSpace(plate)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.eating.plate_full")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        long gameTime = player.getWorld().getTime();
        EatingPreservationManager.PreservationMode mode = EatingPreservationManager.modeForPlayer(player);
        PreparedDishData.updatePreservation(dishStack, gameTime, mode);
        PreparedDishData.refreshExpiration(dishStack, gameTime);
        if (!ServingPlateData.addPortion(plate, dishStack)) {
            return false;
        }

        if (!player.isCreative()) {
            dishStack.decrement(1);
            ItemStack bowl = new ItemStack(Items.BOWL);
            if (!player.getInventory().insertStack(bowl) && !bowl.isEmpty()) {
                player.dropItem(bowl, false);
            }
        }
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ITEM_BUNDLE_INSERT,
                SoundCategory.PLAYERS,
                0.55F,
                1.15F
        );
        player.sendMessage(
                Text.translatable("message.mythicrpg.eating.plate_loaded")
                        .formatted(Formatting.GREEN),
                true
        );
        return true;
    }
}
