package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class PortableCraftingManager {

    private PortableCraftingManager() {
    }

    public static boolean hasPortableCrafting(ServerPlayerEntity player) {
        return SkillTreeManager.hasBonus(
                player,
                SkillType.CRAFTING,
                BonusType.CRAFT_PORTABLE_TABLE
        );
    }

    public static int getDurability(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return 0;
        }

        return PortableCraftingState.get(server).getDurability(player.getUuid());
    }

    public static boolean tryConsumeCharge(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return false;
        }

        PortableCraftingState state = PortableCraftingState.get(server);

        if (state.getDurability(player.getUuid()) <= 0) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.portable_crafting.broken")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        state.consumeCharge(player.getUuid());
        return true;
    }

    public static int repair(ServerPlayerEntity player, double repairPower) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return 0;
        }

        int amount = Math.max(
                1,
                (int) Math.ceil(PortableCraftingState.MAX_DURABILITY * repairPower)
        );

        return PortableCraftingState.get(server).repair(player.getUuid(), amount);
    }

    public static void sendDurability(ServerPlayerEntity player) {
        int durability = getDurability(player);

        player.sendMessage(
                Text.translatable("message.mythicrpg.portable_crafting.durability", durability, PortableCraftingState.MAX_DURABILITY)
                        .formatted(durability > 0 ? Formatting.YELLOW : Formatting.RED),
                true
        );
    }

    public static boolean tryConsumeCharges(ServerPlayerEntity player, int amount) {
        if (amount <= 0) {
            return false;
        }

        MinecraftServer server = player.getServer();

        if (server == null) {
            return false;
        }

        PortableCraftingState state = PortableCraftingState.get(server);
        int durability = state.getDurability(player.getUuid());

        if (durability < amount) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.portable_crafting.not_enough", durability, amount)
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        state.setDurability(player.getUuid(), durability - amount);
        return true;
    }
}