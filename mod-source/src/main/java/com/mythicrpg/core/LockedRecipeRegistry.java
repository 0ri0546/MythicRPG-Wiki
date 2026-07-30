package com.mythicrpg.core;

import com.mythicrpg.building.VerticalSlabRegistry;
import com.mythicrpg.eating.EatingPreservationManager;
import com.mythicrpg.traveling.AdoptionSaddleItem;
import com.mythicrpg.traveling.StructureModuleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public final class LockedRecipeRegistry {
    private static final List<LockedRecipeRule> RULES = new ArrayList<>();

    static {
        register(
                (player, result) -> result.isOf(ModBlocks.COOKING_POT.asItem()),
                SkillType.EATING,
                BonusType.EATING_COOKING,
                "skill_tree.mythicrpg.eating.1.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> result.isOf(ModItems.SMALL_PLATE),
                SkillType.EATING,
                BonusType.EATING_SMALL_PLATE,
                "skill_tree.mythicrpg.eating.5.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> result.isOf(ModItems.MEDIUM_PLATE),
                SkillType.EATING,
                BonusType.EATING_MEDIUM_PLATE,
                "skill_tree.mythicrpg.eating.6.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> result.isOf(ModItems.LARGE_PLATE),
                SkillType.EATING,
                BonusType.EATING_LARGE_PLATE,
                "skill_tree.mythicrpg.eating.7.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> result.isOf(ModBlocks.FRIDGE.asItem()),
                SkillType.EATING,
                BonusType.EATING_FRIDGE,
                "skill_tree.mythicrpg.eating.8.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.isOf(Items.ENCHANTED_BOOK)
                        && EatingPreservationManager.getPortableFridgeLevel(player, result) > 0,
                SkillType.EATING,
                BonusType.EATING_PORTABLE_FRIDGE,
                "skill_tree.mythicrpg.eating.19.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.isOf(ModItems.DELIVERY_PHONE),
                SkillType.EATING,
                BonusType.EATING_DELIVERY,
                "skill_tree.mythicrpg.eating.13.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.isOf(ModItems.CHEF_NOTEBOOK),
                SkillType.EATING,
                BonusType.EATING_SIGNATURE_DISH,
                "skill_tree.mythicrpg.eating.18.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> result.isOf(ModItems.ENCHANTED_AXE),
                SkillType.WOODCUTTING,
                BonusType.ENCHANTED_AXE_CRAFT,
                "skill_tree.mythicrpg.woodcutting.18.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> result.isOf(ModItems.CHEST_MODULE_I),
                SkillType.WOODCUTTING,
                BonusType.CHEST_MODULE_I_CRAFT,
                "skill_tree.mythicrpg.woodcutting.15.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.isOf(ModItems.CHEST_MODULE_II),
                SkillType.WOODCUTTING,
                BonusType.CHEST_MODULE_II_CRAFT,
                "skill_tree.mythicrpg.woodcutting.16.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.isOf(ModItems.CHEST_MODULE_III),
                SkillType.WOODCUTTING,
                BonusType.CHEST_MODULE_III_CRAFT,
                "skill_tree.mythicrpg.woodcutting.17.name",
                Formatting.LIGHT_PURPLE
        );

        register(
                (player, result) -> result.isOf(Items.ENCHANTED_BOOK)
                        && GrowthHealthManager.getGrowthLevelFromStack(player, result) > 0,
                SkillType.WOODCUTTING,
                BonusType.GROWTH_CRAFT,
                "skill_tree.mythicrpg.woodcutting.8.name",
                Formatting.GREEN
        );

        register(
                (player, result) -> result.isOf(ModItems.FOOD_BACKPACK),
                SkillType.FARMING,
                BonusType.FOOD_BACKPACK_CRAFT,
                "skill_tree.mythicrpg.farming.8.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> result.isOf(ModItems.ENCHANTED_FLOWER),
                SkillType.FARMING,
                BonusType.ENCHANTED_FLOWER_CRAFT,
                "skill_tree.mythicrpg.farming.18.name",
                Formatting.LIGHT_PURPLE
        );

        register(
                (player, result) -> result.isOf(ModItems.REPAIR_KIT),
                SkillType.CRAFTING,
                BonusType.REPAIR_KIT_CRAFT,
                "skill_tree.mythicrpg.crafting.5.name",
                Formatting.RED
        );

        register(
                (player, result) -> result.isOf(ModBlocks.INFINITE_CRAFTING_TABLE.asItem()),
                SkillType.CRAFTING,
                BonusType.INFINITE_CRAFTING_TABLE_CRAFT,
                "skill_tree.mythicrpg.crafting.9.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> result.isOf(ModItems.EXP_CHARM),
                SkillType.CRAFTING,
                BonusType.EXP_CHARM_CRAFT,
                "skill_tree.mythicrpg.crafting.10.name",
                Formatting.LIGHT_PURPLE
        );

        register(
                (player, result) -> result.isOf(ModBlocks.LUCKY_BLOCK.asItem()),
                SkillType.CRAFTING,
                BonusType.LUCKY_BLOCK_CRAFT,
                "skill_tree.mythicrpg.crafting.8.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> result.isOf(ModItems.MINIATURIZATION_CHARM),
                SkillType.TRAVELING,
                BonusType.TRAVEL_MINIATURIZATION,
                "skill_tree.mythicrpg.traveling.8.name",
                Formatting.LIGHT_PURPLE
        );


        register(
                (player, result) -> StructureModuleItem.isOverworldModule(result),
                SkillType.TRAVELING,
                BonusType.STRUCTURE_MODULES_OVERWORLD,
                "skill_tree.mythicrpg.traveling.10.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> StructureModuleItem.isDimensionalModule(result),
                SkillType.TRAVELING,
                BonusType.STRUCTURE_MODULES_NETHER_END,
                "skill_tree.mythicrpg.traveling.11.name",
                Formatting.DARK_PURPLE
        );


        register(
                (player, result) -> result.isOf(ModItems.TRAVELER_MINECART),
                SkillType.TRAVELING,
                BonusType.FAST_MINECART_CRAFT,
                "skill_tree.mythicrpg.traveling.15.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.isOf(ModItems.TRAVELER_BOAT),
                SkillType.TRAVELING,
                BonusType.FAST_BOAT_CRAFT,
                "skill_tree.mythicrpg.traveling.16.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.getItem() instanceof AdoptionSaddleItem saddle
                        && !saddle.getMountType().isFlying(),
                SkillType.TRAVELING,
                BonusType.LAND_MOUNTS,
                "skill_tree.mythicrpg.traveling.17.name",
                Formatting.GOLD
        );


        register(
                (player, result) -> result.getItem() instanceof AdoptionSaddleItem saddle
                        && saddle.getMountType().isFlying(),
                SkillType.TRAVELING,
                BonusType.FLYING_MOUNTS,
                "skill_tree.mythicrpg.traveling.19.name",
                Formatting.LIGHT_PURPLE
        );

        register(
                (player, result) -> result.isOf(ModItems.GRAPPLING_HOOK),
                SkillType.TRAVELING,
                BonusType.GRAPPLING_HOOK_CRAFT,
                "skill_tree.mythicrpg.traveling.20.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.isOf(ModItems.BUILDING_PLAN_2D),
                SkillType.BUILDING,
                BonusType.BUILD_PLAN_2D_8,
                "skill_tree.mythicrpg.building.2.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.isOf(ModItems.BUILDING_PLAN_3D),
                SkillType.BUILDING,
                BonusType.BUILD_PLAN_3D,
                "skill_tree.mythicrpg.building.4.name",
                Formatting.LIGHT_PURPLE
        );

        register(
                (player, result) -> result.isOf(ModItems.ARCHITECT_COMPASS),
                SkillType.BUILDING,
                BonusType.BUILD_ARCHITECT_COMPASS,
                "skill_tree.mythicrpg.building.14.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.isOf(ModItems.BUILDER_WAND),
                SkillType.BUILDING,
                BonusType.BUILD_WAND,
                "skill_tree.mythicrpg.building.20.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> VerticalSlabRegistry.isVerticalSlab(result),
                SkillType.BUILDING,
                BonusType.BUILD_VERTICAL_SLABS,
                "skill_tree.mythicrpg.building.8.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> result.isOf(ModBlocks.BLANK_BLOCK.asItem()),
                SkillType.BUILDING,
                BonusType.BUILD_BLANK_BLOCK,
                "skill_tree.mythicrpg.building.13.name",
                Formatting.WHITE
        );

        register(
                (player, result) -> result.isOf(ModBlocks.BUILDING_RESERVE_CHEST.asItem()),
                SkillType.BUILDING,
                BonusType.BUILD_RESERVE_RANGE,
                "skill_tree.mythicrpg.building.15.name",
                Formatting.GOLD
        );

        register(
                (player, result) -> result.isOf(ModItems.BUILDING_MINIATURE_PROJECT),
                SkillType.BUILDING,
                BonusType.BUILD_MINIATURE,
                "skill_tree.mythicrpg.building.18.name",
                Formatting.LIGHT_PURPLE
        );

        register(
                (player, result) -> result.isOf(ModBlocks.STATIC_DECORATION.asItem()),
                SkillType.BUILDING,
                BonusType.BUILD_STATIC_DECORATION,
                "skill_tree.mythicrpg.building.19.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.isOf(Items.BRUSH),
                SkillType.MINING,
                BonusType.FOSSIL_EXCAVATION,
                "skill_tree.mythicrpg.mining.2.name",
                Formatting.AQUA
        );

        register(
                (player, result) -> result.isOf(ModBlocks.FOSSIL_INCUBATOR.asItem()),
                SkillType.MINING,
                BonusType.FOSSIL_INCUBATION,
                "skill_tree.mythicrpg.mining.3.name",
                Formatting.GOLD
        );
        // Fishing V1
        register((player, result) -> result.isOf(ModItems.MYTHIC_FISHING_ROD), SkillType.FISHING, BonusType.FISHING_CUSTOM_ROD, "skill_tree.mythicrpg.fishing.1.name", Formatting.AQUA);
        register((player, result) -> result.isOf(ModItems.WEATHER_WAND), SkillType.FISHING, BonusType.FISHING_WEATHER_RAIN, "skill_tree.mythicrpg.fishing.2.name", Formatting.AQUA);
        register((player, result) -> result.isOf(ModItems.BAIT_I), SkillType.FISHING, BonusType.FISHING_BAIT_I, "skill_tree.mythicrpg.fishing.5.name", Formatting.GREEN);
        register((player, result) -> result.isOf(ModItems.BAIT_II), SkillType.FISHING, BonusType.FISHING_BAIT_II, "skill_tree.mythicrpg.fishing.6.name", Formatting.AQUA);
        register((player, result) -> result.isOf(ModItems.BAIT_III), SkillType.FISHING, BonusType.FISHING_BAIT_III, "skill_tree.mythicrpg.fishing.7.name", Formatting.LIGHT_PURPLE);
        register((player, result) -> result.isOf(ModItems.RUNE_RARITY), SkillType.FISHING, BonusType.FISHING_RUNE_RARITY, "skill_tree.mythicrpg.fishing.9.name", Formatting.LIGHT_PURPLE);
        register((player, result) -> result.isOf(ModItems.RUNE_SPEED), SkillType.FISHING, BonusType.FISHING_RUNE_SPEED, "skill_tree.mythicrpg.fishing.10.name", Formatting.AQUA);
        register((player, result) -> result.isOf(ModItems.RUNE_MASTERY), SkillType.FISHING, BonusType.FISHING_RUNE_MASTERY, "skill_tree.mythicrpg.fishing.11.name", Formatting.GOLD);
        register((player, result) -> result.isOf(ModBlocks.FISH_NET.asItem()), SkillType.FISHING, BonusType.FISHING_NET_3, "skill_tree.mythicrpg.fishing.12.name", Formatting.AQUA);
        register((player, result) -> result.isOf(ModBlocks.FISHERY_TABLE.asItem()), SkillType.FISHING, BonusType.FISHING_FISHERY_TABLE, "skill_tree.mythicrpg.fishing.15.name", Formatting.GOLD);
        register((player, result) -> result.getItem() instanceof com.mythicrpg.fishing.FishingScaleArmorItem, SkillType.FISHING, BonusType.FISHING_SCALE_ARMOR, "skill_tree.mythicrpg.fishing.16.name", Formatting.LIGHT_PURPLE);
        register((player, result) -> result.isOf(ModItems.FISHING_BOAT), SkillType.FISHING, BonusType.FISHING_BOAT, "skill_tree.mythicrpg.fishing.17.name", Formatting.AQUA);
        register((player, result) -> result.isOf(ModItems.BAIT_LEGENDARY), SkillType.FISHING, BonusType.FISHING_LEGENDARY_BAIT, "skill_tree.mythicrpg.fishing.18.name", Formatting.GOLD);
        register((player, result) -> result.isOf(ModItems.BASALT_FISHING_ROD), SkillType.FISHING, BonusType.FISHING_BASALT_ROD, "skill_tree.mythicrpg.fishing.19.name", Formatting.RED);
        register((player, result) -> result.isOf(ModItems.VOID_FISHING_ROD), SkillType.FISHING, BonusType.FISHING_VOID_ROD, "skill_tree.mythicrpg.fishing.20.name", Formatting.DARK_PURPLE);

    }

    private LockedRecipeRegistry() {
    }

    public static boolean canCraft(ServerPlayerEntity player, ItemStack result) {
        if (result.isEmpty()) {
            return true;
        }

        LockedRecipeRule rule = findRule(player, result);

        if (rule == null) {
            return true;
        }

        return SkillTreeManager.hasBonus(player, rule.skillType(), rule.bonusType());
    }

    public static Text getLockedMessage(ServerPlayerEntity player, ItemStack result) {
        LockedRecipeRule rule = findRule(player, result);

        if (rule == null) {
            return Text.translatable("message.mythicrpg.recipe.locked.generic")
                    .formatted(Formatting.RED);
        }

        Text requirement = Text.translatable(rule.requirementTranslationKey())
                .formatted(rule.formatting());
        return Text.translatable("message.mythicrpg.recipe.locked", requirement)
                .formatted(Formatting.RED);
    }

    private static void register(
            LockedRecipeMatcher matcher,
            SkillType skillType,
            BonusType bonusType,
            String requirementTranslationKey,
            Formatting formatting
    ) {
        RULES.add(new LockedRecipeRule(
                matcher,
                skillType,
                bonusType,
                requirementTranslationKey,
                formatting
        ));
    }

    private static LockedRecipeRule findRule(ServerPlayerEntity player, ItemStack result) {
        for (LockedRecipeRule rule : RULES) {
            if (rule.matcher().matches(player, result)) {
                return rule;
            }
        }

        return null;
    }

    @FunctionalInterface
    private interface LockedRecipeMatcher {
        boolean matches(ServerPlayerEntity player, ItemStack result);
    }

    private record LockedRecipeRule(
            LockedRecipeMatcher matcher,
            SkillType skillType,
            BonusType bonusType,
            String requirementTranslationKey,
            Formatting formatting
    ) {
    }
}
