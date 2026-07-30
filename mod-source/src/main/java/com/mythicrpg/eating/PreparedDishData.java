package com.mythicrpg.eating;

import com.mythicrpg.core.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.Optional;
import java.util.UUID;

public final class PreparedDishData {
    private static final String ROOT = "mythicrpg_prepared_dish";
    private static final String RECIPE = "recipe";
    private static final String CATEGORY = "category";
    private static final String RARITY = "rarity";
    private static final String CREATED_AT = "created_at";
    private static final String EXPIRES_AT = "expires_at";
    private static final String REMAINING_TICKS = "remaining_ticks";
    private static final String PORTABLE_TICK = "portable_tick";
    private static final String CHEF = "chef";
    private static final String DUBIOUS = "dubious";

    /**
     * Portable storage is refreshed once per second. A short grace period absorbs slot moves,
     * lag spikes and screen transitions without allowing an item left in an ordinary container
     * to stay preserved indefinitely.
     */
    private static final long PORTABLE_CONTINUITY_TICKS = 100L;

    private static final FoodComponent VALID_FOOD = new FoodComponent.Builder()
            .nutrition(20)
            .saturationModifier(1.0F)
            .build();
    private static final FoodComponent DUBIOUS_FOOD = new FoodComponent.Builder()
            .nutrition(3)
            .saturationModifier(0.2F)
            .build();

    private PreparedDishData() {
    }

    public static ItemStack create(CookingResult result, UUID chef, long gameTime) {
        return create(result, chef, gameTime, ModItems.PREPARED_DISH);
    }

    public static ItemStack createSignature(CookingResult result, UUID chef, long gameTime) {
        return create(result, chef, gameTime, ModItems.SIGNATURE_DISH);
    }

    private static ItemStack create(CookingResult result, UUID chef, long gameTime, Item item) {
        ItemStack stack = new ItemStack(item);
        long shelfLifeTicks = Math.max(1L, result.recipe().shelfLifeDays()) * 24_000L;
        long expiresAt = result.dubious() ? 0L : safeAdd(gameTime, shelfLifeTicks);
        long remainingTicks = result.dubious() ? 0L : shelfLifeTicks;
        NbtCompound dish = new NbtCompound();
        dish.putString(RECIPE, result.recipe().id());
        dish.putString(CATEGORY, result.recipe().category().id());
        dish.putString(RARITY, result.rarity().id());
        dish.putLong(CREATED_AT, gameTime);
        dish.putLong(EXPIRES_AT, expiresAt);
        dish.putLong(REMAINING_TICKS, remainingTicks);
        dish.putLong(PORTABLE_TICK, 0L);
        dish.putString(CHEF, chef == null ? "" : chef.toString());
        dish.putBoolean(DUBIOUS, result.dubious());
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.put(ROOT, dish));
        applyPresentation(stack, result.recipe().id(), result.rarity(), result.dubious());
        return stack;
    }

    public static Optional<Dish> read(ItemStack stack) {
        if (!stack.isOf(ModItems.PREPARED_DISH) && !stack.isOf(ModItems.SIGNATURE_DISH)) {
            return Optional.empty();
        }
        NbtCompound custom = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        ).copyNbt();
        if (!custom.contains(ROOT)) {
            return Optional.empty();
        }
        NbtCompound dish = custom.getCompound(ROOT);
        Optional<DishCategory> category = DishCategory.byId(dish.getString(CATEGORY));
        Optional<DishRarity> rarity = DishRarity.byId(dish.getString(RARITY));
        if (category.isEmpty() || rarity.isEmpty() || dish.getString(RECIPE).isBlank()) {
            return Optional.empty();
        }
        UUID chef = null;
        if (!dish.getString(CHEF).isBlank()) {
            try {
                chef = UUID.fromString(dish.getString(CHEF));
            } catch (IllegalArgumentException ignored) {
                // Old or malformed chef identifiers are treated as unknown.
            }
        }
        return Optional.of(new Dish(
                dish.getString(RECIPE),
                category.get(),
                rarity.get(),
                dish.getLong(CREATED_AT),
                dish.getLong(EXPIRES_AT),
                dish.getLong(REMAINING_TICKS),
                dish.getLong(PORTABLE_TICK),
                chef,
                dish.getBoolean(DUBIOUS)
        ));
    }

    public static Dish refreshExpiration(ItemStack stack, long gameTime) {
        Dish current = read(stack).orElseGet(PreparedDishData::fallbackDish);
        if (current.dubious() || current.expiresAt() <= 0L || gameTime < current.expiresAt()) {
            return current;
        }
        Dish expired = dubiousFrom(current);
        write(stack, expired);
        return expired;
    }

    /**
     * Applies one of the three shelf-life contexts:
     * NONE lets world time advance normally, PORTABLE compensates only continuous inventory
     * ticks, and FRIDGE freezes the remaining duration persistently across chunk unloads.
     */
    public static boolean updatePreservation(
            ItemStack stack,
            long gameTime,
            EatingPreservationManager.PreservationMode mode
    ) {
        Optional<Dish> optional = read(stack);
        if (optional.isEmpty()) {
            return false;
        }

        Dish current = optional.get();
        if (current.dubious()) {
            return false;
        }

        return switch (mode) {
            case NONE -> updateUnpreserved(stack, current, gameTime);
            case PORTABLE -> updatePortable(stack, current, gameTime, false);
            case PORTABLE_CONTINUOUS -> updatePortable(stack, current, gameTime, true);
            case FRIDGE -> updateFridge(stack, current, gameTime);
        };
    }

    private static boolean updateUnpreserved(ItemStack stack, Dish current, long gameTime) {
        if (current.expiresAt() <= 0L) {
            long remaining = Math.max(0L, current.remainingTicks());
            if (remaining <= 0L) {
                write(stack, dubiousFrom(current));
                return true;
            }
            writeTiming(stack, withTiming(
                    current,
                    safeAdd(gameTime, remaining),
                    remaining,
                    0L
            ));
            return true;
        }

        if (gameTime >= current.expiresAt()) {
            write(stack, dubiousFrom(current));
            return true;
        }

        if (current.portableTick() != 0L) {
            writeTiming(stack, withTiming(
                    current,
                    current.expiresAt(),
                    Math.max(0L, current.expiresAt() - gameTime),
                    0L
            ));
            return true;
        }
        return false;
    }

    private static boolean updatePortable(
            ItemStack stack,
            Dish current,
            long gameTime,
            boolean allowLongGap
    ) {
        long expiresAt = current.expiresAt();
        if (expiresAt <= 0L) {
            long remaining = Math.max(0L, current.remainingTicks());
            if (remaining <= 0L) {
                write(stack, dubiousFrom(current));
                return true;
            }
            expiresAt = safeAdd(gameTime, remaining);
        } else if (current.portableTick() > 0L) {
            long elapsed = gameTime - current.portableTick();
            if (elapsed >= 0L && (allowLongGap || elapsed <= PORTABLE_CONTINUITY_TICKS)) {
                expiresAt = safeAdd(expiresAt, elapsed);
            }
        }

        if (gameTime >= expiresAt) {
            write(stack, dubiousFrom(current));
            return true;
        }

        Dish updated = withTiming(
                current,
                expiresAt,
                Math.max(0L, expiresAt - gameTime),
                gameTime
        );
        if (updated.equals(current)) {
            return false;
        }
        writeTiming(stack, updated);
        return true;
    }

    private static boolean updateFridge(ItemStack stack, Dish current, long gameTime) {
        if (current.expiresAt() <= 0L) {
            if (current.remainingTicks() <= 0L) {
                write(stack, dubiousFrom(current));
                return true;
            }
            if (current.portableTick() == 0L) {
                return false;
            }
            writeTiming(stack, withTiming(current, 0L, current.remainingTicks(), 0L));
            return true;
        }

        long remaining = remainingTicks(current, gameTime);
        if (remaining <= 0L) {
            write(stack, dubiousFrom(current));
            return true;
        }
        writeTiming(stack, withTiming(current, 0L, remaining, 0L));
        return true;
    }

    public static long remainingTicks(Dish dish, long gameTime) {
        if (dish.dubious()) {
            return 0L;
        }
        if (dish.expiresAt() <= 0L) {
            return Math.max(0L, dish.remainingTicks());
        }
        return Math.max(0L, dish.expiresAt() - gameTime);
    }

    public static boolean isPreserved(Dish dish) {
        return !dish.dubious()
                && dish.remainingTicks() > 0L
                && (dish.expiresAt() <= 0L || dish.portableTick() > 0L);
    }

    public static void write(ItemStack stack, Dish dish) {
        writeData(stack, dish);
        if (dish.dubious()) {
            SignatureDishData.clear(stack);
            if (stack.isOf(ModItems.SIGNATURE_DISH)) {
                stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false);
            }
        }
        applyPresentation(stack, dish.recipeId(), dish.rarity(), dish.dubious());
    }

    private static void writeTiming(ItemStack stack, Dish dish) {
        writeData(stack, dish);
    }

    private static void writeData(ItemStack stack, Dish dish) {
        NbtCompound data = new NbtCompound();
        data.putString(RECIPE, dish.recipeId());
        data.putString(CATEGORY, dish.category().id());
        data.putString(RARITY, dish.rarity().id());
        data.putLong(CREATED_AT, dish.createdAt());
        data.putLong(EXPIRES_AT, dish.expiresAt());
        data.putLong(REMAINING_TICKS, dish.remainingTicks());
        data.putLong(PORTABLE_TICK, dish.portableTick());
        data.putString(CHEF, dish.chef() == null ? "" : dish.chef().toString());
        data.putBoolean(DUBIOUS, dish.dubious());
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.put(ROOT, data));
    }

    private static Dish withTiming(
            Dish current,
            long expiresAt,
            long remainingTicks,
            long portableTick
    ) {
        return new Dish(
                current.recipeId(),
                current.category(),
                current.rarity(),
                current.createdAt(),
                expiresAt,
                remainingTicks,
                portableTick,
                current.chef(),
                false
        );
    }

    private static Dish dubiousFrom(Dish current) {
        return new Dish(
                "dubious_dish",
                DishCategory.MAIN,
                DishRarity.COMMON,
                current.createdAt(),
                current.expiresAt(),
                0L,
                0L,
                current.chef(),
                true
        );
    }

    private static void applyPresentation(ItemStack stack, String recipeId, DishRarity rarity, boolean dubious) {
        Text name = Text.translatable("dish.mythicrpg." + (dubious ? "dubious_dish" : recipeId))
                .formatted(dubious ? net.minecraft.util.Formatting.DARK_GREEN : rarity.formatting());
        stack.set(DataComponentTypes.CUSTOM_NAME, name);
        stack.set(DataComponentTypes.FOOD, dubious ? DUBIOUS_FOOD : VALID_FOOD);
    }

    private static long safeAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        if (right < 0L && left < Long.MIN_VALUE - right) {
            return Long.MIN_VALUE;
        }
        return left + right;
    }

    private static Dish fallbackDish() {
        return new Dish(
                "dubious_dish",
                DishCategory.MAIN,
                DishRarity.COMMON,
                0L,
                0L,
                0L,
                0L,
                null,
                true
        );
    }

    public record Dish(
            String recipeId,
            DishCategory category,
            DishRarity rarity,
            long createdAt,
            long expiresAt,
            long remainingTicks,
            long portableTick,
            UUID chef,
            boolean dubious
    ) {
    }
}
