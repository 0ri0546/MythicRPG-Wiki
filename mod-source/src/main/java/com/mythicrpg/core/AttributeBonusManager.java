package com.mythicrpg.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItem;
import com.mythicrpg.building.BuildingBlockCatalog;
import net.minecraft.registry.tag.ItemTags;

public class AttributeBonusManager {
    private static final int REFRESH_INTERVAL_TICKS = 5; // 4 fois par seconde
    private static int tickCounter = 0;

    private static final Identifier REACH_MODIFIER_ID = Identifier.of("mythicrpg", "fighting_reach_bonus");
    private static final Identifier BUILDING_REACH_MODIFIER_ID = Identifier.of("mythicrpg", "building_block_reach_bonus");
    private static final Identifier ATTACK_SPEED_MODIFIER_ID = Identifier.of("mythicrpg", "fighting_attack_speed_bonus");

    private static final double BASE_ATTACK_SPEED = 4.0;
    private static final double MAX_ATTACK_SPEED = 1000.0; // approxime "0 cooldown" sans diviser par 0

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
        refreshReach(player);
        refreshBuildingReach(player);
        refreshAttackSpeed(player);
    }

    private static void refreshReach(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        boolean holdingSword = mainHand.isIn(ItemTags.SWORDS);

        double bonus = holdingSword
                ? SkillTreeManager.getBonusTotal(player, SkillType.FIGHTING, BonusType.SWORD_REACH)
                : 0.0;

        applyModifier(player, EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, REACH_MODIFIER_ID, bonus);
    }

    private static void refreshBuildingReach(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        boolean holdingBuildingBlock = mainHand.getItem() instanceof BlockItem blockItem
                && BuildingBlockCatalog.isEligible(blockItem.getBlock());

        double bonus = holdingBuildingBlock
                ? SkillTreeManager.getBonusTotal(
                player,
                SkillType.BUILDING,
                BonusType.BUILD_REACH
        )
                : 0.0;

        applyModifier(
                player,
                EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE,
                BUILDING_REACH_MODIFIER_ID,
                bonus
        );
    }

    private static void refreshAttackSpeed(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        boolean holdingSword = mainHand.isIn(ItemTags.SWORDS);

        double remainingCooldown = holdingSword
                ? SkillTreeManager.getBonusMin(
                player,
                SkillType.FIGHTING,
                BonusType.ATTACK_COOLDOWN_MULTIPLIER,
                1.0
        )
                : 1.0;

        double targetAttackSpeed = remainingCooldown <= 0.0
                ? MAX_ATTACK_SPEED
                : BASE_ATTACK_SPEED / remainingCooldown;

        double delta = targetAttackSpeed - BASE_ATTACK_SPEED;
        applyModifier(player, EntityAttributes.GENERIC_ATTACK_SPEED, ATTACK_SPEED_MODIFIER_ID, delta);
    }

    private static void applyModifier(ServerPlayerEntity player, RegistryEntry<EntityAttribute> attribute,
                                      Identifier modifierId, double value) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }

        EntityAttributeModifier current = instance.getModifier(modifierId);
        if (value == 0.0) {
            if (current != null) {
                instance.removeModifier(modifierId);
            }
            return;
        }

        if (current != null
                && current.operation() == EntityAttributeModifier.Operation.ADD_VALUE
                && Double.compare(current.value(), value) == 0) {
            return;
        }

        if (current != null) {
            instance.removeModifier(modifierId);
        }
        instance.addTemporaryModifier(new EntityAttributeModifier(
                modifierId, value, EntityAttributeModifier.Operation.ADD_VALUE
        ));
    }
}