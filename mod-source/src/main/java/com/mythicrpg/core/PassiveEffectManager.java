package com.mythicrpg.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PassiveEffectManager {
    private static final int REFRESH_INTERVAL_TICKS = 40; // une fois par seconde
    private static final int EFFECT_DURATION_TICKS = 100;  // large marge avant le prochain refresh
    private static int tickCounter = 0;

    private static final Set<RegistryEntry<StatusEffect>> MANAGED_EFFECTS = collectManagedEffects();

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
        Map<RegistryEntry<StatusEffect>, Integer> activeEffects = new HashMap<>();

        for (SkillType type : SkillType.values()) {
            for (var entry : SkillTreeManager.getPassiveEffects(player, type).entrySet()) {
                activeEffects.merge(entry.getKey(), entry.getValue(), Math::max);
            }
        }

        for (RegistryEntry<StatusEffect> effect : MANAGED_EFFECTS) {
            Integer amplifier = activeEffects.get(effect);

            if (amplifier == null) {
                continue;
            }

            player.addStatusEffect(new StatusEffectInstance(
                    effect, EFFECT_DURATION_TICKS, amplifier, true, false, true
            ));
        }
    }

    private static Set<RegistryEntry<StatusEffect>> collectManagedEffects() {
        Set<RegistryEntry<StatusEffect>> effects = new HashSet<>();
        for (SkillType type : SkillType.values()) {
            for (SkillTreeNode node : SkillTreeRegistry.getTree(type).values()) {
                effects.addAll(node.getPassiveEffects().keySet());
            }
        }
        return effects;
    }
}