package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillProgress;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.SkillXpManager;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;

public final class CraftMasteryManager {

    private static final double CRAFT_XP_TRANSFER_RATIO = 0.20;
    private static final double TARGET_SKILL_LEVEL_CAP_RATIO = 0.005;

    private CraftMasteryManager() {
    }

    public static void handleCraftMastery(
            ServerPlayerEntity player,
            ItemStack craftedResult,
            int craftingXpGained
    ) {
        if (craftedResult.isEmpty() || craftingXpGained <= 0) {
            return;
        }

        if (!SkillTreeManager.hasBonus(
                player,
                SkillType.CRAFTING,
                BonusType.CRAFT_MASTERY
        )) {
            return;
        }

        Optional<SkillType> targetSkill = getTargetSkill(craftedResult);

        if (targetSkill.isEmpty()) {
            return;
        }

        SkillType skill = targetSkill.get();

        if (skill == SkillType.CRAFTING) {
            return;
        }

        int rawBonusXp = Math.max(
                1,
                (int) Math.floor(craftingXpGained * CRAFT_XP_TRANSFER_RATIO)
        );

        int cap = getTargetSkillCap(player, skill);

        if (cap <= 0) {
            return;
        }

        int finalBonusXp = Math.min(rawBonusXp, cap);

        if (finalBonusXp <= 0) {
            return;
        }

        SkillXpManager.addXp(player, skill, finalBonusXp, false);

        player.sendMessage(
                Text.translatable("message.mythicrpg.craft_mastery", finalBonusXp, skill.displayName())
                        .formatted(Formatting.AQUA),
                true
        );
    }

    private static Optional<SkillType> getTargetSkill(ItemStack stack) {
        Item item = stack.getItem();

        if (stack.isIn(ItemTags.PICKAXES)
                || item == Items.FURNACE
                || item == Items.BLAST_FURNACE) {
            return Optional.of(SkillType.MINING);
        }

        if (stack.isIn(ItemTags.AXES)
                || isWoodcuttingRelated(item)) {
            return Optional.of(SkillType.WOODCUTTING);
        }

        if (item instanceof SwordItem
                || item instanceof ArmorItem
                || item == Items.BOW
                || item == Items.CROSSBOW
                || item == Items.TRIDENT
                || item == Items.SHIELD) {
            return Optional.of(SkillType.FIGHTING);
        }

        if (stack.isIn(ItemTags.HOES)
                || isFarmingRelated(item)) {
            return Optional.of(SkillType.FARMING);
        }

        if (item == Items.FISHING_ROD) {
            return Optional.of(SkillType.FISHING);
        }

        if (item == Items.MINECART
                || item == Items.COMPASS
                || item == Items.CLOCK) {
            return Optional.of(SkillType.TRAVELING);
        }

        if (stack.contains(net.minecraft.component.DataComponentTypes.FOOD)) {
            return Optional.of(SkillType.EATING);
        }

        return Optional.empty();
    }

    private static boolean isWoodcuttingRelated(Item item) {
        return item == Items.CRAFTING_TABLE
                || item == Items.CHEST
                || item == ModItems.CHEST_MODULE_I
                || item == ModItems.CHEST_MODULE_II
                || item == ModItems.CHEST_MODULE_III
                || item == Items.BARREL
                || item == Items.LADDER
                || item == Items.OAK_PLANKS
                || item == Items.SPRUCE_PLANKS
                || item == Items.BIRCH_PLANKS
                || item == Items.JUNGLE_PLANKS
                || item == Items.ACACIA_PLANKS
                || item == Items.DARK_OAK_PLANKS
                || item == Items.MANGROVE_PLANKS
                || item == Items.CHERRY_PLANKS
                || item == Items.CRIMSON_PLANKS
                || item == Items.WARPED_PLANKS;
    }

    private static boolean isFarmingRelated(Item item) {
        return item == Items.COMPOSTER
                || item == Items.BONE_MEAL
                || item == Items.WHEAT
                || item == Items.BREAD
                || item == Items.HAY_BLOCK
                || item == Items.CARROT
                || item == Items.POTATO
                || item == Items.BEETROOT
                || item == Items.WHEAT_SEEDS
                || item == Items.PUMPKIN_SEEDS
                || item == Items.MELON_SEEDS
                || item == Items.BEETROOT_SEEDS
                || item == Items.PUMPKIN_PIE
                || item == Items.MUSHROOM_STEW
                || item == Items.RABBIT_STEW;
    }

    private static int getTargetSkillCap(ServerPlayerEntity player, SkillType targetSkill) {
        SkillProgress progress = ModAttachments.getProgress(player, targetSkill);

        if (progress.getLevel() >= SkillProgress.MAX_LEVEL) {
            return 0;
        }

        int xpForNextLevel = SkillProgress.xpRequiredForLevel(progress.getLevel());

        return Math.max(
                1,
                (int) Math.floor(xpForNextLevel * TARGET_SKILL_LEVEL_CAP_RATIO)
        );
    }

}
