package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;

public final class ReinforcedCraftManager {

    private ReinforcedCraftManager() {
    }

    public static ItemStack apply(ServerPlayerEntity player, ItemStack result) {
        if (result.isEmpty()) {
            return result;
        }

        if (!SkillTreeManager.hasBonus(
                player,
                SkillType.CRAFTING,
                BonusType.REINFORCED_CRAFT_CHANCE
        )) {
            return result;
        }

        double chance = SkillTreeManager.getBonusTotal(
                player,
                SkillType.CRAFTING,
                BonusType.REINFORCED_CRAFT_CHANCE
        );

        if (chance <= 0.0 || player.getRandom().nextDouble() >= chance) {
            return result;
        }

        if (!isEligible(result)) {
            return result;
        }

        Optional<RegistryEntry.Reference<Enchantment>> unbreaking = player.getWorld()
                .getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(Enchantments.UNBREAKING);

        if (unbreaking.isEmpty()) {
            return result;
        }

        ItemEnchantmentsComponent enchantments = result.getOrDefault(
                DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT
        );

        if (enchantments.getLevel(unbreaking.get()) > 0) {
            return result;
        }

        result.addEnchantment(unbreaking.get(), 1);

        player.sendMessage(
                Text.translatable("message.mythicrpg.reinforced_craft")
                        .formatted(Formatting.AQUA),
                true
        );

        return result;
    }

    private static boolean isEligible(ItemStack stack) {
        return stack.getItem() instanceof ToolItem
                || stack.getItem() instanceof ArmorItem
                || stack.isDamageable();
    }
}