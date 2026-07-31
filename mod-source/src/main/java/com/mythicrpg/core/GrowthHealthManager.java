package com.mythicrpg.core;

import com.mythicrpg.MythicRPG;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class GrowthHealthManager {

    private static final int REFRESH_INTERVAL_TICKS = 10;

    private static final Identifier GROWTH_HEALTH_MODIFIER_ID =
            Identifier.of(MythicRPG.MOD_ID, "growth_health_bonus");

    private static int tickCounter = 0;

    private GrowthHealthManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter % REFRESH_INTERVAL_TICKS != 0) {
                return;
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                refreshPlayer(player);
            }
        });
    }

    private static void refreshPlayer(ServerPlayerEntity player) {
        int growthLevel = getHighestGrowthLevel(player);

        // Growth I = +1 coeur = +2 HP
        // Growth II = +2 coeurs = +4 HP
        // Growth III = +3 coeurs = +6 HP
        double bonusHealth = growthLevel * 2.0;

        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        EntityAttributeModifier current = maxHealth.getModifier(GROWTH_HEALTH_MODIFIER_ID);
        if (bonusHealth <= 0.0) {
            if (current != null) {
                maxHealth.removeModifier(GROWTH_HEALTH_MODIFIER_ID);
            }
        } else if (current == null
                || current.operation() != EntityAttributeModifier.Operation.ADD_VALUE
                || Double.compare(current.value(), bonusHealth) != 0) {
            if (current != null) {
                maxHealth.removeModifier(GROWTH_HEALTH_MODIFIER_ID);
            }
            maxHealth.addTemporaryModifier(new EntityAttributeModifier(
                    GROWTH_HEALTH_MODIFIER_ID,
                    bonusHealth,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static int getHighestGrowthLevel(ServerPlayerEntity player) {
        int highest = 0;

        for (ItemStack armorStack : player.getArmorItems()) {
            int level = getGrowthLevelFromStack(player, armorStack);

            if (level > highest) {
                highest = level;
            }
        }

        return highest;
    }

    public static int getGrowthLevelFromStack(ServerPlayerEntity player, ItemStack stack) {
        return player.getServerWorld()
                .getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(ModEnchantments.GROWTH)
                .map(entry -> EnchantmentHelper.getLevel(entry, stack))
                .orElse(0);
    }
}