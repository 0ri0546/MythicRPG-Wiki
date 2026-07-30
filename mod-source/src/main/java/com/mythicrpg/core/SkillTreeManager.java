package com.mythicrpg.core;

import com.mythicrpg.network.TreeStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class SkillTreeManager {
    public static final int NODE_UNLOCK_COST = 10;

    /**
     * Hot-path aggregate cache. Unlock lists are replaced rather than mutated by
     * ModAttachments, so list identity is a reliable and allocation-free
     * invalidation token. Weak player keys prevent stale player instances from
     * being retained after respawn or disconnect.
     */
    private static final Map<ServerPlayerEntity, EnumMap<SkillType, BonusSnapshot>> BONUS_CACHE =
            new WeakHashMap<>();

    public static boolean tryUnlock(ServerPlayerEntity player, SkillType type, int nodeId) {
        Map<Integer, SkillTreeNode> tree = SkillTreeRegistry.getTree(type);
        SkillTreeNode node = tree.get(nodeId);
        if (node == null) {
            return false;
        }

        List<Integer> unlocked = new ArrayList<>(ModAttachments.getUnlocks(player, type));
        if (unlocked.contains(nodeId)) {
            return false;
        }

        boolean prerequisiteMet = node.isRoot() || node.getParentIds().stream().anyMatch(unlocked::contains);
        if (!prerequisiteMet) {
            return false;
        }

        if (node.getForkId() != -1) {
            boolean conflictsWithOtherBranch = unlocked.stream()
                    .map(tree::get)
                    .anyMatch(n -> n != null && n.getForkId() == node.getForkId() && n.getBranchId() != node.getBranchId());
            if (conflictsWithOtherBranch) {
                return false;
            }
        }

        SkillProgress progress = ModAttachments.getProgress(player, type);
        if (!progress.spendPoints(NODE_UNLOCK_COST)) {
            return false;
        }

        unlocked.add(nodeId);
        ModAttachments.setUnlocks(player, type, unlocked);
        ModAttachments.setProgress(player, type, progress);

        node.getPerk().apply(player);
        return true;
    }

    public static void sendStateTo(ServerPlayerEntity player, SkillType type) {
        List<Integer> unlocked = ModAttachments.getUnlocks(player, type);
        SkillProgress progress = ModAttachments.getProgress(player, type);
        int xpForNext = progress.getLevel() >= SkillProgress.MAX_LEVEL ? 0 : SkillProgress.xpRequiredForLevel(progress.getLevel());

        ServerPlayNetworking.send(player, new TreeStatePayload(
                type.name(), unlocked, progress.getSkillPoints(),
                progress.getLevel(), progress.getXp(), xpForNext
        ));
    }

    public static void sendAllStatesTo(ServerPlayerEntity player) {
        for (SkillType type : SkillType.values()) {
            sendStateTo(player, type);
        }
    }

    public static boolean resetTree(ServerPlayerEntity player, SkillType type) {
        List<Integer> unlocked = ModAttachments.getUnlocks(player, type);
        int levelCost = unlocked.size();

        if (levelCost == 0 || player.experienceLevel < levelCost) {
            return false;
        }

        int pointsRefunded = unlocked.size() * NODE_UNLOCK_COST;

        SkillProgress progress = ModAttachments.getProgress(player, type);
        progress.addSkillPoints(pointsRefunded);
        ModAttachments.setProgress(player, type, progress);
        ModAttachments.setUnlocks(player, type, new ArrayList<>());
        player.addExperienceLevels(-levelCost);
        playResetFeedback(player);

        sendStateTo(player, type);
        return true;
    }

    /** Returns the aggregated value declared by {@link BonusType#aggregation()}. */
    public static double getBonusValue(ServerPlayerEntity player, SkillType type, BonusType bonusType) {
        return snapshot(player, type).bonuses.getOrDefault(bonusType, 0.0D);
    }

    /** Kept for compatibility with existing code. */
    public static double getBonusTotal(ServerPlayerEntity player, SkillType type, BonusType bonusType) {
        return getBonusValue(player, type, bonusType);
    }

    public static double getBonusValueFromUnlocked(SkillType type, List<Integer> unlockedIds, BonusType bonusType) {
        return aggregateBonusValues(type, unlockedIds, bonusType, 0.0D);
    }

    /** Kept for compatibility with existing client-side calls. */
    public static double getBonusTotalFromUnlocked(SkillType type, List<Integer> unlockedIds, BonusType bonusType) {
        return getBonusValueFromUnlocked(type, unlockedIds, bonusType);
    }

    public static double getBonusSum(ServerPlayerEntity player, SkillType type, BonusType bonusType) {
        BonusSnapshot snapshot = snapshot(player, type);
        return bonusType.aggregation() == BonusAggregation.SUM
                ? snapshot.bonuses.getOrDefault(bonusType, 0.0D)
                : sumBonusValues(type, snapshot.unlocks, bonusType);
    }

    public static double getBonusMax(ServerPlayerEntity player, SkillType type, BonusType bonusType) {
        BonusSnapshot snapshot = snapshot(player, type);
        return bonusType.aggregation() == BonusAggregation.MAX
                ? snapshot.bonuses.getOrDefault(bonusType, 0.0D)
                : maxBonusValues(type, snapshot.unlocks, bonusType, 0.0D);
    }

    public static double getBonusMin(ServerPlayerEntity player, SkillType type, BonusType bonusType, double defaultValue) {
        BonusSnapshot snapshot = snapshot(player, type);
        if (bonusType.aggregation() == BonusAggregation.MIN) {
            return snapshot.bonuses.getOrDefault(bonusType, defaultValue);
        }
        return minBonusValues(type, snapshot.unlocks, bonusType, defaultValue);
    }

    public static boolean hasBonus(ServerPlayerEntity player, SkillType type, BonusType bonusType) {
        return getBonusValue(player, type, bonusType) > 0.0D;
    }

    public static Map<RegistryEntry<StatusEffect>, Integer> getPassiveEffects(
            ServerPlayerEntity player,
            SkillType type
    ) {
        return snapshot(player, type).passiveEffects;
    }

    public static PoisonOnHit getBestPoisonOnHit(ServerPlayerEntity player, SkillType type) {
        return snapshot(player, type).poisonOnHit;
    }

    private static BonusSnapshot snapshot(ServerPlayerEntity player, SkillType type) {
        List<Integer> unlocks = ModAttachments.getUnlocks(player, type);
        EnumMap<SkillType, BonusSnapshot> playerCache = BONUS_CACHE.computeIfAbsent(
                player,
                ignored -> new EnumMap<>(SkillType.class)
        );
        BonusSnapshot cached = playerCache.get(type);
        if (cached != null && cached.unlocks == unlocks) {
            return cached;
        }

        BonusSnapshot rebuilt = buildSnapshot(type, unlocks);
        playerCache.put(type, rebuilt);
        return rebuilt;
    }

    private static BonusSnapshot buildSnapshot(SkillType type, List<Integer> unlocks) {
        Map<Integer, SkillTreeNode> tree = SkillTreeRegistry.getTree(type);
        EnumMap<BonusType, Double> bonuses = new EnumMap<>(BonusType.class);
        Map<RegistryEntry<StatusEffect>, Integer> passiveEffects = new HashMap<>();
        int bestPoisonAmplifier = -1;
        int bestPoisonDuration = 0;

        for (int nodeId : unlocks) {
            SkillTreeNode node = tree.get(nodeId);
            if (node == null) {
                continue;
            }

            for (Map.Entry<BonusType, Double> entry : node.getBonuses().entrySet()) {
                BonusType bonusType = entry.getKey();
                double value = entry.getValue();
                switch (bonusType.aggregation()) {
                    case SUM -> bonuses.merge(bonusType, value, Double::sum);
                    case MAX -> bonuses.merge(bonusType, value, Math::max);
                    case MIN -> bonuses.merge(bonusType, value, Math::min);
                    case OVERRIDE -> bonuses.put(bonusType, value);
                }
            }

            for (Map.Entry<RegistryEntry<StatusEffect>, Integer> entry
                    : node.getPassiveEffects().entrySet()) {
                passiveEffects.merge(entry.getKey(), entry.getValue(), Math::max);
            }

            PoisonOnHit poison = node.getPoisonOnHit();
            if (poison != null) {
                bestPoisonAmplifier = Math.max(bestPoisonAmplifier, poison.amplifier());
                bestPoisonDuration = Math.max(bestPoisonDuration, poison.durationTicks());
            }
        }

        PoisonOnHit poison = bestPoisonAmplifier < 0
                ? null
                : new PoisonOnHit(bestPoisonAmplifier, bestPoisonDuration);
        return new BonusSnapshot(
                unlocks,
                bonuses,
                Map.copyOf(passiveEffects),
                poison
        );
    }

    private static double aggregateBonusValues(
            SkillType type,
            List<Integer> unlockedIds,
            BonusType bonusType,
            double defaultValue
    ) {
        return switch (bonusType.aggregation()) {
            case SUM -> sumBonusValues(type, unlockedIds, bonusType);
            case MAX -> maxBonusValues(type, unlockedIds, bonusType, defaultValue);
            case MIN -> minBonusValues(type, unlockedIds, bonusType, defaultValue);
            case OVERRIDE -> lastBonusValue(type, unlockedIds, bonusType, defaultValue);
        };
    }

    private static double sumBonusValues(SkillType type, List<Integer> unlockedIds, BonusType bonusType) {
        Map<Integer, SkillTreeNode> tree = SkillTreeRegistry.getTree(type);
        double total = 0.0D;
        for (int nodeId : unlockedIds) {
            SkillTreeNode node = tree.get(nodeId);
            if (node != null) {
                total += node.getBonuses().getOrDefault(bonusType, 0.0D);
            }
        }
        return total;
    }

    private static double maxBonusValues(
            SkillType type,
            List<Integer> unlockedIds,
            BonusType bonusType,
            double defaultValue
    ) {
        Map<Integer, SkillTreeNode> tree = SkillTreeRegistry.getTree(type);
        double max = defaultValue;
        boolean found = false;
        for (int nodeId : unlockedIds) {
            SkillTreeNode node = tree.get(nodeId);
            if (node == null) {
                continue;
            }
            Double value = node.getBonuses().get(bonusType);
            if (value != null) {
                max = found ? Math.max(max, value) : value;
                found = true;
            }
        }
        return found ? max : defaultValue;
    }

    private static double minBonusValues(
            SkillType type,
            List<Integer> unlockedIds,
            BonusType bonusType,
            double defaultValue
    ) {
        Map<Integer, SkillTreeNode> tree = SkillTreeRegistry.getTree(type);
        double min = defaultValue;
        boolean found = false;
        for (int nodeId : unlockedIds) {
            SkillTreeNode node = tree.get(nodeId);
            if (node == null) {
                continue;
            }
            Double value = node.getBonuses().get(bonusType);
            if (value != null) {
                min = found ? Math.min(min, value) : value;
                found = true;
            }
        }
        return found ? min : defaultValue;
    }

    private static double lastBonusValue(
            SkillType type,
            List<Integer> unlockedIds,
            BonusType bonusType,
            double defaultValue
    ) {
        Map<Integer, SkillTreeNode> tree = SkillTreeRegistry.getTree(type);
        double value = defaultValue;
        for (int nodeId : unlockedIds) {
            SkillTreeNode node = tree.get(nodeId);
            if (node == null) {
                continue;
            }
            Double nodeValue = node.getBonuses().get(bonusType);
            if (nodeValue != null) {
                value = nodeValue;
            }
        }
        return value;
    }

    private static final class BonusSnapshot {
        private final List<Integer> unlocks;
        private final EnumMap<BonusType, Double> bonuses;
        private final Map<RegistryEntry<StatusEffect>, Integer> passiveEffects;
        private final PoisonOnHit poisonOnHit;

        private BonusSnapshot(
                List<Integer> unlocks,
                EnumMap<BonusType, Double> bonuses,
                Map<RegistryEntry<StatusEffect>, Integer> passiveEffects,
                PoisonOnHit poisonOnHit
        ) {
            this.unlocks = unlocks;
            this.bonuses = bonuses;
            this.passiveEffects = passiveEffects;
            this.poisonOnHit = poisonOnHit;
        }
    }

    private static void playResetFeedback(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();

        world.spawnParticles(
                ParticleTypes.POOF,
                player.getX(),
                player.getBodyY(0.5),
                player.getZ(),
                30,
                0.45,
                0.6,
                0.45,
                0.04
        );

        world.spawnParticles(
                ParticleTypes.ENCHANT,
                player.getX(),
                player.getBodyY(0.5),
                player.getZ(),
                20,
                0.5,
                0.7,
                0.5,
                0.08
        );

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_GRINDSTONE_USE,
                SoundCategory.PLAYERS,
                0.8f,
                1.0f
        );
    }
}
