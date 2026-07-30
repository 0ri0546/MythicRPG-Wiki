package com.mythicrpg.farming;

import com.mythicrpg.core.ItemContainerUtils;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.PlayerCooldownManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class EnchantedFlowerSmeltManager {
    private static final int BACKPACK_SLOTS = 54;
    private static final int FEEDBACK_COOLDOWN_TICKS = 10;

    private EnchantedFlowerSmeltManager() {
    }

    public static void trySmeltPickup(ServerPlayerEntity player, ItemEntity itemEntity) {
        if (!player.getOffHandStack().isOf(ModItems.ENCHANTED_FLOWER)) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        ItemStack original = itemEntity.getStack();

        if (original.isEmpty()) {
            return;
        }

        ItemStack cookedSingle = getSmeltingResult(world, original);

        if (cookedSingle.isEmpty()) {
            return;
        }

        int availableSeeds = countEnchantedSeeds(player);

        if (availableSeeds <= 0) {
            return;
        }

        int smeltAmount = Math.min(original.getCount(), availableSeeds);

        if (smeltAmount <= 0) {
            return;
        }

        int consumed = consumeEnchantedSeeds(player, smeltAmount);

        if (consumed <= 0) {
            return;
        }

        ItemStack cookedStack = cookedSingle.copy();
        cookedStack.setCount(cookedSingle.getCount() * consumed);

        int remainingRaw = original.getCount() - consumed;

        if (remainingRaw > 0) {
            ItemStack rawRemainder = original.copy();
            rawRemainder.setCount(remainingRaw);

            ItemEntity remainderEntity = new ItemEntity(
                    world,
                    itemEntity.getX(),
                    itemEntity.getY(),
                    itemEntity.getZ(),
                    rawRemainder
            );

            remainderEntity.setPickupDelay(10);
            world.spawnEntity(remainderEntity);
        }

        itemEntity.setStack(cookedStack);

        playEnchantedFlowerFeedback(player, world, consumed);
    }

    private static ItemStack getSmeltingResult(ServerWorld world, ItemStack inputStack) {
        SingleStackRecipeInput input = new SingleStackRecipeInput(inputStack.copyWithCount(1));

        Optional<RecipeEntry<SmeltingRecipe>> recipe = world.getRecipeManager()
                .getFirstMatch(RecipeType.SMELTING, input, world);

        if (recipe.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = recipe.get().value().craft(input, world.getRegistryManager());

        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return result;
    }

    private static int countEnchantedSeeds(ServerPlayerEntity player) {
        int total = 0;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);

            if (stack.isOf(ModItems.ENCHANTED_SEED)) {
                total += stack.getCount();
                continue;
            }

            if (stack.isOf(ModItems.FOOD_BACKPACK)) {
                total += countSeedsInBackpack(stack);
            }
        }

        return total;
    }

    private static int countSeedsInBackpack(ItemStack backpack) {
        return ItemContainerUtils.countMatching(
                backpack,
                BACKPACK_SLOTS,
                stack -> stack.isOf(ModItems.ENCHANTED_SEED)
        );
    }

    private static int consumeEnchantedSeeds(ServerPlayerEntity player, int amount) {
        int remaining = amount;

        for (int i = 0; i < player.getInventory().size(); i++) {
            if (remaining <= 0) {
                break;
            }

            ItemStack stack = player.getInventory().getStack(i);

            if (!stack.isOf(ModItems.ENCHANTED_SEED)) {
                continue;
            }

            int consumed = Math.min(stack.getCount(), remaining);
            stack.decrement(consumed);
            remaining -= consumed;
        }

        for (int i = 0; i < player.getInventory().size(); i++) {
            if (remaining <= 0) {
                break;
            }

            ItemStack stack = player.getInventory().getStack(i);

            if (!stack.isOf(ModItems.FOOD_BACKPACK)) {
                continue;
            }

            remaining = consumeSeedsFromBackpack(stack, remaining);
        }

        return amount - remaining;
    }

    private static int consumeSeedsFromBackpack(ItemStack backpack, int amount) {
        int consumed = ItemContainerUtils.removeMatching(
                backpack,
                BACKPACK_SLOTS,
                stack -> stack.isOf(ModItems.ENCHANTED_SEED),
                amount
        );

        return amount - consumed;
    }

    private static void playEnchantedFlowerFeedback(
            ServerPlayerEntity player,
            ServerWorld world,
            int smeltedAmount
    ) {
        spawnFlowerParticlesAroundPlayer(player, world);

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.PLAYERS,
                0.30f,
                1.8f
        );

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                0.22f,
                1.6f
        );

        sendSmeltActionbar(player, smeltedAmount);
    }

    private static void sendSmeltActionbar(ServerPlayerEntity player, int smeltedAmount) {
        if (!PlayerCooldownManager.tryUse(
                player,
                "enchanted_flower_smelt_feedback",
                FEEDBACK_COOLDOWN_TICKS
        )) {
            return;
        }

        player.sendMessage(
                Text.translatable(
                            "message.mythicrpg.enchanted_flower.smelted",
                            smeltedAmount,
                            smeltedAmount
                    ).formatted(Formatting.LIGHT_PURPLE),
                true
        );
    }

    private static void spawnFlowerParticlesAroundPlayer(ServerPlayerEntity player, ServerWorld world) {
        Vec3d look = player.getRotationVec(1.0f).normalize();

        Vec3d behind = new Vec3d(-look.x, 0.0, -look.z).normalize();
        Vec3d side = new Vec3d(-look.z, 0.0, look.x).normalize();

        Vec3d base = player.getPos()
                .add(0.0, 1.0, 0.0)
                .add(behind.multiply(0.85));

        Vec3d left = base.add(side.multiply(0.45));
        Vec3d right = base.add(side.multiply(-0.45));

        spawnSmallFlowerBurst(world, left);
        spawnSmallFlowerBurst(world, right);
    }

    private static void spawnSmallFlowerBurst(ServerWorld world, Vec3d pos) {
        world.spawnParticles(
                ParticleTypes.ENCHANT,
                pos.x,
                pos.y,
                pos.z,
                6,
                0.12,
                0.18,
                0.12,
                0.025
        );

        world.spawnParticles(
                ParticleTypes.FLAME,
                pos.x,
                pos.y - 0.05,
                pos.z,
                2,
                0.08,
                0.10,
                0.08,
                0.006
        );
    }
}