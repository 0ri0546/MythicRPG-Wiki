package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MythicInspirationManager {

    private static final int MYTHIC_INSPIRATION_MULTIPLIER = 3;
    private static final Set<UUID> INSPIRED_PLAYERS = new HashSet<>();

    private MythicInspirationManager() {
    }

    public static int applyIfReady(
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
                BonusType.MYTHIC_INSPIRATION
        )) {
            INSPIRED_PLAYERS.remove(player.getUuid());
            return craftXp;
        }

        boolean hadInspiration = INSPIRED_PLAYERS.remove(player.getUuid());

        if (!hadInspiration) {
            return craftXp;
        }

        player.sendMessage(
                Text.translatable("message.mythicrpg.mythic_inspiration.bonus")
                        .formatted(Formatting.LIGHT_PURPLE),
                true
        );

        return Math.max(1, craftXp * MYTHIC_INSPIRATION_MULTIPLIER);
    }

    public static void tryGrantFromCraft(
            ServerPlayerEntity player,
            ItemStack result
    ) {
        if (result.isEmpty()) {
            return;
        }

        if (!SkillTreeManager.hasBonus(
                player,
                SkillType.CRAFTING,
                BonusType.MYTHIC_INSPIRATION
        )) {
            return;
        }

        if (!isMythicRpgItem(result)) {
            return;
        }

        INSPIRED_PLAYERS.add(player.getUuid());

        player.sendMessage(
                Text.translatable("message.mythicrpg.mythic_inspiration.ready")
                        .formatted(Formatting.LIGHT_PURPLE),
                true
        );
    }

    public static void clearPlayer(UUID playerUuid) {
        INSPIRED_PLAYERS.remove(playerUuid);
    }

    private static boolean isMythicRpgItem(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return "mythicrpg".equals(id.getNamespace());
    }
}