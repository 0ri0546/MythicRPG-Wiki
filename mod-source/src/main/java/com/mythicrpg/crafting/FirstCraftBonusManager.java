package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class FirstCraftBonusManager {

    private static final int FIRST_CRAFT_MULTIPLIER = 3;

    private FirstCraftBonusManager() {
    }

    public static int applyFirstCraftBonus(
            ServerPlayerEntity player,
            ItemStack result,
            int craftXp
    ) {
        if (craftXp <= 0 || result.isEmpty()) {
            return craftXp;
        }

        if (!SkillTreeManager.hasBonus(
                player,
                SkillType.CRAFTING,
                BonusType.FIRST_CRAFT_BONUS
        )) {
            return craftXp;
        }

        MinecraftServer server = player.getServer();

        if (server == null) {
            return craftXp;
        }

        Identifier itemId = Registries.ITEM.getId(result.getItem());
        CraftFirstCraftState state = CraftFirstCraftState.get(server);

        boolean firstTime = state.markCrafted(player.getUuid(), itemId);

        if (!firstTime) {
            return craftXp;
        }

        player.sendMessage(
                Text.translatable("message.mythicrpg.first_craft_bonus")
                        .formatted(Formatting.GOLD),
                true
        );

        return Math.max(1, craftXp * FIRST_CRAFT_MULTIPLIER);
    }
}