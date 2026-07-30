package com.mythicrpg.core;

import com.mojang.serialization.Codec;
import com.mythicrpg.mining.archaeology.FossilCodexData;
import com.mythicrpg.eating.EatingCodexData;
import com.mythicrpg.fishing.FishingCodexData;
import com.mythicrpg.fishing.SeaMonsterProgressData;
import com.mythicrpg.eating.EatingRuntimeData;
import com.mythicrpg.eating.SignatureDishProfile;
import com.mythicrpg.titles.TitleProfile;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModAttachments {

    private static final Codec<Map<SkillType, SkillProgress>> SKILL_PROGRESS_CODEC =
            Codec.unboundedMap(Codec.STRING, SkillProgress.CODEC)
                    .xmap(ModAttachments::decodeSkillMap, ModAttachments::encodeSkillMap);

    private static final Codec<Map<SkillType, List<Integer>>> SKILL_UNLOCKS_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.INT.listOf())
                    .xmap(ModAttachments::decodeSkillMap, ModAttachments::encodeSkillMap);

    public static final AttachmentType<Map<SkillType, SkillProgress>> SKILL_PROGRESS =
            AttachmentRegistry.create(
                    Identifier.of("mythicrpg", "skill_progress"),
                    builder -> builder
                            .initializer(() -> new EnumMap<>(SkillType.class))
                            .persistent(SKILL_PROGRESS_CODEC)
                            .copyOnDeath()
            );

    public static final AttachmentType<Map<SkillType, List<Integer>>> SKILL_UNLOCKS =
            AttachmentRegistry.create(
                    Identifier.of("mythicrpg", "skill_unlocks"),
                    builder -> builder
                            .initializer(() -> new EnumMap<>(SkillType.class))
                            .persistent(SKILL_UNLOCKS_CODEC)
                            .copyOnDeath()
            );

    public static final AttachmentType<FossilCodexData> FOSSIL_CODEX =
            AttachmentRegistry.create(
                    Identifier.of("mythicrpg", "fossil_codex"),
                    builder -> builder
                            .initializer(FossilCodexData::new)
                            .persistent(FossilCodexData.CODEC)
                            .copyOnDeath()
            );

    public static final AttachmentType<FishingCodexData> FISHING_CODEX =
            AttachmentRegistry.create(
                    Identifier.of("mythicrpg", "fishing_codex"),
                    builder -> builder.initializer(FishingCodexData::new).persistent(FishingCodexData.CODEC).copyOnDeath()
            );

    public static final AttachmentType<SeaMonsterProgressData> SEA_MONSTER_PROGRESS =
            AttachmentRegistry.create(
                    Identifier.of("mythicrpg", "sea_monster_progress"),
                    builder -> builder
                            .initializer(SeaMonsterProgressData::new)
                            .persistent(SeaMonsterProgressData.CODEC)
                            .copyOnDeath()
            );

    public static final AttachmentType<EatingCodexData> EATING_CODEX =
            AttachmentRegistry.create(
                    Identifier.of("mythicrpg", "eating_codex"),
                    builder -> builder
                            .initializer(EatingCodexData::new)
                            .persistent(EatingCodexData.CODEC)
                            .copyOnDeath()
            );


    public static final AttachmentType<EatingRuntimeData> EATING_RUNTIME =
            AttachmentRegistry.create(
                    Identifier.of("mythicrpg", "eating_runtime"),
                    builder -> builder
                            .initializer(EatingRuntimeData::defaults)
                            .persistent(EatingRuntimeData.CODEC)
                            .copyOnDeath()
            );

    public static final AttachmentType<SignatureDishProfile> SIGNATURE_DISH_PROFILE =
            AttachmentRegistry.create(
                    Identifier.of("mythicrpg", "signature_dish_profile"),
                    builder -> builder
                            .initializer(SignatureDishProfile::empty)
                            .persistent(SignatureDishProfile.CODEC)
                            .copyOnDeath()
            );

    public static final AttachmentType<TitleProfile> TITLE_PROFILE =
            AttachmentRegistry.create(
                    Identifier.of("mythicrpg", "title_profile"),
                    builder -> builder
                            .initializer(TitleProfile::defaults)
                            .persistent(TitleProfile.CODEC)
                            .copyOnDeath()
            );


    public static final AttachmentType<Boolean> MINING_AREA_3X3_ENABLED =
            AttachmentRegistry.create(
                    Identifier.of("mythicrpg", "mining_area_3x3_enabled"),
                    builder -> builder
                            .initializer(() -> false)
                            .persistent(Codec.BOOL)
                            .copyOnDeath()
            );

    public static final AttachmentType<String> ACTIVE_FOOD_BACKPACK_ID =
            AttachmentRegistry.create(
                    Identifier.of("mythicrpg", "active_food_backpack_id"),
                    builder -> builder
                            .initializer(() -> "")
                            .persistent(Codec.STRING)
                            .copyOnDeath()
            );

    private static <T> Map<SkillType, T> decodeSkillMap(Map<String, T> serialized) {
        EnumMap<SkillType, T> decoded = new EnumMap<>(SkillType.class);

        serialized.forEach((id, value) ->
                SkillType.fromId(id).ifPresent(type -> decoded.put(type, value))
        );

        return decoded;
    }

    private static <T> Map<String, T> encodeSkillMap(Map<SkillType, T> skills) {
        Map<String, T> encoded = new LinkedHashMap<>();

        for (SkillType type : SkillType.values()) {
            T value = skills.get(type);
            if (value != null) {
                encoded.put(type.name(), value);
            }
        }

        return encoded;
    }

    public static SkillProgress getProgress(ServerPlayerEntity player, SkillType type) {
        Map<SkillType, SkillProgress> map = player.getAttachedOrCreate(SKILL_PROGRESS);
        SkillProgress progress = map.getOrDefault(type, new SkillProgress());
        if (!progress.isNormalized()) {
            progress = progress.normalizedCopy();
            setProgress(player, type, progress);
        }
        return progress;
    }

    public static void setProgress(ServerPlayerEntity player, SkillType type, SkillProgress progress) {
        Map<SkillType, SkillProgress> oldMap = player.getAttachedOrCreate(SKILL_PROGRESS);
        SkillProgress normalized = progress == null ? new SkillProgress() : progress.normalizedCopy();

        EnumMap<SkillType, SkillProgress> newMap = new EnumMap<>(SkillType.class);
        newMap.putAll(oldMap);
        newMap.put(type, normalized);

        player.setAttached(SKILL_PROGRESS, newMap);
    }

    public static List<Integer> getUnlocks(ServerPlayerEntity player, SkillType type) {
        Map<SkillType, List<Integer>> map = player.getAttachedOrCreate(SKILL_UNLOCKS);
        List<Integer> stored = map.getOrDefault(type, List.of());
        List<Integer> normalized = normalizeUnlocks(type, stored);
        if (!normalized.equals(stored)) {
            setUnlocks(player, type, normalized);
            return normalized;
        }
        return stored;
    }

    public static void setUnlocks(ServerPlayerEntity player, SkillType type, List<Integer> unlocked) {
        Map<SkillType, List<Integer>> oldMap = player.getAttachedOrCreate(SKILL_UNLOCKS);

        EnumMap<SkillType, List<Integer>> newMap = new EnumMap<>(SkillType.class);
        newMap.putAll(oldMap);
        newMap.put(type, normalizeUnlocks(type, unlocked));

        player.setAttached(SKILL_UNLOCKS, newMap);
    }

    private static List<Integer> normalizeUnlocks(SkillType type, List<Integer> raw) {
        Map<Integer, SkillTreeNode> tree = SkillTreeRegistry.getTree(type);
        if (tree.isEmpty() || raw == null || raw.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Integer> accepted = new LinkedHashSet<>();
        Map<Integer, Integer> selectedBranches = new HashMap<>();
        int limit = tree.size();

        for (Integer id : raw) {
            if (id == null || accepted.size() >= limit || accepted.contains(id)) continue;
            SkillTreeNode node = tree.get(id);
            if (node == null) continue;
            if (!node.isRoot() && node.getParentIds().stream().noneMatch(accepted::contains)) continue;
            if (node.getForkId() != -1) {
                Integer selected = selectedBranches.get(node.getForkId());
                if (selected != null && selected != node.getBranchId()) continue;
                selectedBranches.putIfAbsent(node.getForkId(), node.getBranchId());
            }
            accepted.add(id);
        }
        return List.copyOf(accepted);
    }

    public static FossilCodexData getFossilCodex(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(FOSSIL_CODEX);
    }

    public static void setFossilCodex(ServerPlayerEntity player, FossilCodexData data) {
        player.setAttached(FOSSIL_CODEX, data);
    }

    public static FishingCodexData getFishingCodex(ServerPlayerEntity player) { return player.getAttachedOrCreate(FISHING_CODEX); }
    public static void setFishingCodex(ServerPlayerEntity player, FishingCodexData data) { player.setAttached(FISHING_CODEX, data); }

    public static SeaMonsterProgressData getSeaMonsterProgress(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(SEA_MONSTER_PROGRESS);
    }

    public static void setSeaMonsterProgress(ServerPlayerEntity player, SeaMonsterProgressData data) {
        player.setAttached(SEA_MONSTER_PROGRESS, data == null ? new SeaMonsterProgressData() : data);
    }

    public static EatingCodexData getEatingCodex(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(EATING_CODEX);
    }

    public static void setEatingCodex(ServerPlayerEntity player, EatingCodexData data) {
        player.setAttached(EATING_CODEX, data);
    }


    public static EatingRuntimeData getEatingRuntime(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(EATING_RUNTIME);
    }

    public static void setEatingRuntime(ServerPlayerEntity player, EatingRuntimeData data) {
        player.setAttached(EATING_RUNTIME, data == null ? EatingRuntimeData.defaults() : data);
    }

    public static SignatureDishProfile getSignatureDishProfile(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(SIGNATURE_DISH_PROFILE);
    }

    public static void setSignatureDishProfile(ServerPlayerEntity player, SignatureDishProfile profile) {
        player.setAttached(SIGNATURE_DISH_PROFILE, profile == null ? SignatureDishProfile.empty() : profile);
    }

    public static TitleProfile getTitleProfile(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(TITLE_PROFILE);
    }

    public static void setTitleProfile(ServerPlayerEntity player, TitleProfile profile) {
        player.setAttached(TITLE_PROFILE, profile);
    }

    public static String getActiveFoodBackpackId(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(ACTIVE_FOOD_BACKPACK_ID);
    }

    public static void setActiveFoodBackpackId(ServerPlayerEntity player, String backpackId) {
        player.setAttached(ACTIVE_FOOD_BACKPACK_ID, backpackId == null ? "" : backpackId);
    }

}