package com.mythicrpg.fighting.barons;

import com.mythicrpg.core.ModItems;
import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ItemScatterer;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class BaronRewardRegistry {

    private BaronRewardRegistry() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(BaronRewardRegistry::onAfterDeath);
    }

    private static void onAfterDeath(LivingEntity entity, DamageSource damageSource) {
        if (!BaronMobManager.isBaron(entity)) {
            return;
        }

        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }

        if (!(damageSource.getAttacker() instanceof ServerPlayerEntity)) {
            return;
        }

        BaronType type = BaronMobManager.getBaronType(entity);
        rollRewards(world, entity, type);
    }

    private static void rollRewards(ServerWorld world, LivingEntity entity, BaronType type) {
        switch (type) {
            case DRUID -> drop(world, entity, () -> PotionContentsComponent.createStack(Items.TIPPED_ARROW, Potions.HEALING));
            case BARRAGE -> drop(world, entity, new ItemStack(Items.ARROW, 8));
            case NUKE -> {
                drop(world, entity, new ItemStack(Items.GUNPOWDER));
                roll(world, entity, 0.25, () -> new ItemStack(Items.FERMENTED_SPIDER_EYE));
            }
            case SURVIVOR -> {
                roll(world, entity, 0.25, () -> new ItemStack(Items.IRON_INGOT));
                roll(world, entity, 0.25, () -> new ItemStack(Items.LEATHER));
            }
            case FUGITIVE -> {
                drop(world, entity, new ItemStack(Items.SUGAR));
                roll(world, entity, 0.25, () -> new ItemStack(Items.RABBIT_FOOT));
            }
            case GOLDEN -> rollGoldenRewards(world, entity);
            case PANIC -> roll(world, entity, 0.30, BaronRewardRegistry::createNauseaPotion);
            case HOTHEAD -> {
                roll(world, entity, 0.50, () -> new ItemStack(Items.FIRE_CHARGE));
                roll(world, entity, 0.05, () -> new ItemStack(ModItems.FIRE_WAND));
            }
            case ALCHEMIST -> {
                roll(world, entity, 0.50, () -> createRandomUsefulPotion(world));
                roll(world, entity, 0.25, () -> new ItemStack(Items.NETHER_WART));
            }
            case GIANT -> rollGiantRewards(world, entity);
            case DARKNIGHT -> drop(world, entity, () -> PotionContentsComponent.createStack(Items.POTION, Potions.NIGHT_VISION));
            case SWIMMING -> {
                roll(world, entity, 0.25, () -> new ItemStack(Items.PRISMARINE_SHARD));
                roll(world, entity, 0.10, () -> new ItemStack(Items.NAUTILUS_SHELL));
            }
            case DROWNED_KING -> roll(world, entity, 0.10, () -> createRiptideTrident(world));
            case CHARGING -> drop(world, entity, new ItemStack(Items.RED_CARPET));
            case BALLOON -> roll(world, entity, 0.20, () -> new ItemStack(Items.DRAGON_BREATH));
            case DIAMOND -> {
                roll(world, entity, 0.50, () -> new ItemStack(Items.PHANTOM_MEMBRANE));
                roll(world, entity, 0.03, () -> new ItemStack(Items.DIAMOND));
            }
            case STALKER -> {
                roll(world, entity, 0.50, () -> new ItemStack(Items.WITHER_ROSE));
                roll(world, entity, 0.10, () -> new ItemStack(ModItems.WITHER_SHIELD));
            }
            case HEAVY -> roll(world, entity, 0.40, () -> createEnchantedBook(world, Enchantments.BLAST_PROTECTION, 1));
            case MOLTEN -> roll(world, entity, 0.50, () -> new ItemStack(Items.MAGMA_CREAM));
            case RUNNER -> {
                // No specific reward.
            }
            case INK -> {
                drop(world, entity, new ItemStack(Items.INK_SAC, 2));
                roll(world, entity, 0.25, () -> new ItemStack(Items.GLOW_INK_SAC));
            }
            case UNDYING_WOLF -> {
                // No specific death reward. The reward is taming it alive.
            }
            case INFERNO -> roll(world, entity, 0.10, () -> new ItemStack(ModItems.HEART_OF_THE_BEAM));
            case THROWER -> roll(world, entity, 0.10, () -> new ItemStack(ModItems.SPIDER_WAND));
            case NORMAL -> roll(world, entity, 0.00001, () -> new ItemStack(ModItems.BARONS_DOLL));
        }
    }

    private static void rollGoldenRewards(ServerWorld world, LivingEntity entity) {
        drop(world, entity, () -> createGoldenReward(world));

        if (world.random.nextDouble() < BaronScaling.getGoldenSecondRewardChance(entity)) {
            drop(world, entity, () -> createGoldenReward(world));
        }

        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, entity.getX(), entity.getBodyY(0.6), entity.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        world.playSound(null, entity.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 0.5f, 1.5f);
    }

    private static ItemStack createGoldenReward(ServerWorld world) {
        return world.random.nextBoolean()
                ? new ItemStack(Items.GOLDEN_APPLE)
                : new ItemStack(Items.EMERALD);
    }

    private static void rollGiantRewards(ServerWorld world, LivingEntity entity) {
        if (entity instanceof MagmaCubeEntity) {
            drop(world, entity, new ItemStack(Items.MAGMA_CREAM, 2));
            roll(world, entity, 0.25, () -> new ItemStack(Items.MAGMA_BLOCK));
            return;
        }

        if (entity instanceof SlimeEntity) {
            drop(world, entity, new ItemStack(Items.SLIME_BALL, 4));
            roll(world, entity, 0.25, () -> new ItemStack(Items.SLIME_BLOCK));
        }
    }

    private static ItemStack createNauseaPotion() {
        ItemStack potion = new ItemStack(Items.POTION);
        potion.set(
                DataComponentTypes.POTION_CONTENTS,
                PotionContentsComponent.DEFAULT.with(new StatusEffectInstance(StatusEffects.NAUSEA, 20 * 10, 0))
        );
        return potion;
    }

    private static ItemStack createRandomUsefulPotion(ServerWorld world) {
        List<RegistryEntry<net.minecraft.potion.Potion>> pool = List.of(
                Potions.HEALING,
                Potions.REGENERATION,
                Potions.SWIFTNESS,
                Potions.FIRE_RESISTANCE,
                Potions.STRENGTH
        );

        return PotionContentsComponent.createStack(Items.POTION, pool.get(world.random.nextInt(pool.size())));
    }

    private static ItemStack createRiptideTrident(ServerWorld world) {
        ItemStack trident = new ItemStack(Items.TRIDENT);
        addEnchantment(world, trident, Enchantments.RIPTIDE, 1);
        trident.setDamage((int) Math.round(trident.getMaxDamage() * 0.85));
        return trident;
    }

    private static ItemStack createEnchantedBook(ServerWorld world, net.minecraft.registry.RegistryKey<Enchantment> enchantmentKey, int level) {
        Optional<RegistryEntry.Reference<Enchantment>> enchantment = getEnchantment(world, enchantmentKey);

        if (enchantment.isEmpty()) {
            return new ItemStack(Items.BOOK);
        }

        return EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(enchantment.get(), level));
    }

    private static void addEnchantment(ServerWorld world, ItemStack stack, net.minecraft.registry.RegistryKey<Enchantment> enchantmentKey, int level) {
        getEnchantment(world, enchantmentKey).ifPresent(enchantment -> stack.addEnchantment(enchantment, level));
    }

    private static Optional<RegistryEntry.Reference<Enchantment>> getEnchantment(ServerWorld world, net.minecraft.registry.RegistryKey<Enchantment> enchantmentKey) {
        RegistryEntryLookup<Enchantment> lookup = world.getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
        return lookup.getOptional(enchantmentKey);
    }

    private static void roll(ServerWorld world, LivingEntity entity, double chance, Supplier<ItemStack> rewardSupplier) {
        if (world.random.nextDouble() >= chance) {
            return;
        }

        drop(world, entity, rewardSupplier);
    }

    private static void drop(ServerWorld world, LivingEntity entity, Supplier<ItemStack> rewardSupplier) {
        drop(world, entity, rewardSupplier.get());
    }

    private static void drop(ServerWorld world, LivingEntity entity, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        ItemScatterer.spawn(world, entity.getX(), entity.getY(), entity.getZ(), stack);
    }
}
