package com.mythicrpg.titles;

import com.mythicrpg.core.GlobalLevelManager;
import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.core.PlayerTabNameManager;
import com.mythicrpg.network.TitleStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public final class TitleManager {
    private TitleManager() {
    }

    public static void initializePlayer(ServerPlayerEntity player) {
        reconcileAutomaticUnlocks(player, false);
        sanitizeStoredProfile(player);
        PlayerTabNameManager.refresh(player);
        sendState(player);
    }

    public static void onSkillProgressChanged(ServerPlayerEntity player) {
        reconcileAutomaticUnlocks(player, true);
        PlayerTabNameManager.refresh(player);
        sendState(player);
    }

    public static void sendState(ServerPlayerEntity player) {
        reconcileAutomaticUnlocks(player, false);
        TitleProfile profile = sanitizeStoredProfile(player);
        List<String> selectableUnlocked = TitleRegistry.selectableUnlocked(profile.unlockedTitleIds())
                .stream()
                .map(TitleDefinition::id)
                .toList();

        ServerPlayNetworking.send(player, new TitleStatePayload(
                selectableUnlocked,
                profile.activeTitleId(),
                profile.primaryColorId(),
                profile.secondaryColorId(),
                profile.gradient(),
                profile.finishId()
        ));
    }

    public static boolean applySelection(
            ServerPlayerEntity player,
            String titleId,
            String primaryColorId,
            String secondaryColorId,
            boolean gradient,
            String finishId
    ) {
        reconcileAutomaticUnlocks(player, false);
        TitleProfile current = sanitizeStoredProfile(player);

        String normalizedTitleId = titleId == null ? "" : titleId;
        if (!normalizedTitleId.isBlank()) {
            Optional<TitleDefinition> selected = TitleRegistry.get(normalizedTitleId);
            if (selected.isEmpty()
                    || !selected.get().selectable()
                    || !current.unlockedTitleIds().contains(normalizedTitleId)) {
                player.sendMessage(
                        Text.translatable("message.mythicrpg.title.invalid_selection").formatted(Formatting.RED),
                        true
                );
                sendState(player);
                return false;
            }
        }

        Optional<TitleColor> primary = TitleColor.fromId(primaryColorId);
        Optional<TitleColor> secondary = TitleColor.fromId(secondaryColorId);
        Optional<TitleFinish> finish = TitleFinish.fromId(finishId);
        if (primary.isEmpty() || secondary.isEmpty() || finish.isEmpty()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.title.invalid_selection").formatted(Formatting.RED),
                    true
            );
            sendState(player);
            return false;
        }

        boolean normalizedGradient = gradient && primary.get() != secondary.get();
        TitleProfile updated = current.withSelection(
                normalizedTitleId,
                primary.get(),
                secondary.get(),
                normalizedGradient,
                finish.get()
        );
        ModAttachments.setTitleProfile(player, updated);
        PlayerTabNameManager.refresh(player);
        sendState(player);
        player.sendMessage(Text.translatable("message.mythicrpg.title.applied").formatted(Formatting.GREEN), true);
        return true;
    }

    public static boolean grantSpecialTitle(ServerPlayerEntity player, String titleId, boolean notify) {
        Optional<TitleDefinition> definition = TitleRegistry.get(titleId);
        if (definition.isEmpty() || definition.get().category() != TitleCategory.SPECIAL) {
            return false;
        }

        TitleProfile current = sanitizeStoredProfile(player);
        if (current.unlockedTitleIds().contains(titleId)) {
            return false;
        }

        LinkedHashSet<String> unlocked = new LinkedHashSet<>(current.unlockedTitleIds());
        unlocked.add(titleId);
        ModAttachments.setTitleProfile(player, current.withUnlockedTitles(unlocked));

        if (notify && definition.get().selectable()) {
            notifyUnlock(player, definition.get());
        }
        PlayerTabNameManager.refresh(player);
        sendState(player);
        return true;
    }

    public static int unlockAllForTesting(ServerPlayerEntity player) {
        TitleProfile current = sanitizeStoredProfile(player);
        LinkedHashSet<String> unlocked = new LinkedHashSet<>(current.unlockedTitleIds());

        int previousCount = unlocked.size();

        TitleRegistry.all().stream()
                .filter(TitleDefinition::selectable)
                .map(TitleDefinition::id)
                .forEach(unlocked::add);

        int addedCount = unlocked.size() - previousCount;

        if (addedCount > 0) {
            ModAttachments.setTitleProfile(
                    player,
                    current.withUnlockedTitles(unlocked)
            );
        }

        PlayerTabNameManager.refresh(player);
        sendState(player);

        return addedCount;
    }

    public static MutableText buildEquippedPrefix(ServerPlayerEntity player) {
        TitleProfile profile = sanitizeStoredProfile(player);
        if (profile.activeTitleId().isBlank()) {
            return Text.empty();
        }

        Optional<TitleDefinition> definition = TitleRegistry.get(profile.activeTitleId());
        if (definition.isEmpty()
                || !definition.get().selectable()
                || !profile.unlockedTitleIds().contains(definition.get().id())) {
            return Text.empty();
        }

        TitleColor primary = TitleColor.fromId(profile.primaryColorId()).orElse(TitleColor.WHITE);
        TitleColor secondary = TitleColor.fromId(profile.secondaryColorId()).orElse(primary);
        TitleFinish finish = TitleFinish.fromId(profile.finishId()).orElse(TitleFinish.NONE);
        String localized = definition.get().localizedLiteral(player.getClientOptions().language());
        return TitleTextFormatter.prefix(localized, primary, secondary, profile.gradient(), finish);
    }

    public static TitleProfile sanitizeStoredProfile(ServerPlayerEntity player) {
        TitleProfile current = ModAttachments.getTitleProfile(player);
        LinkedHashSet<String> validUnlocked = new LinkedHashSet<>();
        for (String unlockedId : current.unlockedTitleIds()) {
            if (TitleRegistry.get(unlockedId).isPresent()) {
                validUnlocked.add(unlockedId);
            }
        }

        TitleColor primary = TitleColor.fromId(current.primaryColorId()).orElse(TitleColor.WHITE);
        TitleColor secondary = TitleColor.fromId(current.secondaryColorId()).orElse(primary);
        TitleFinish finish = TitleFinish.fromId(current.finishId()).orElse(TitleFinish.NONE);
        String activeId = current.activeTitleId();
        if (!activeId.isBlank()) {
            Optional<TitleDefinition> active = TitleRegistry.get(activeId);
            if (active.isEmpty() || !active.get().selectable() || !validUnlocked.contains(activeId)) {
                activeId = "";
            }
        }

        boolean normalizedGradient = current.gradient() && primary != secondary;
        TitleProfile sanitized = new TitleProfile(
                activeId,
                primary.id(),
                normalizedGradient ? secondary.id() : primary.id(),
                normalizedGradient,
                finish.id(),
                validUnlocked
        );

        if (!sanitized.equals(current)) {
            ModAttachments.setTitleProfile(player, sanitized);
        }
        return sanitized;
    }

    private static void reconcileAutomaticUnlocks(ServerPlayerEntity player, boolean notify) {
        TitleProfile current = sanitizeStoredProfile(player);
        LinkedHashSet<String> unlocked = new LinkedHashSet<>(current.unlockedTitleIds());
        LinkedHashSet<TitleDefinition> newlyUnlocked = new LinkedHashSet<>();
        int globalLevel = GlobalLevelManager.getGlobalLevel(player);

        for (TitleDefinition definition : TitleRegistry.all()) {
            if (!definition.isAutomaticallyUnlocked() || unlocked.contains(definition.id())) {
                continue;
            }

            boolean eligible = definition.globalLevelRequirement() > 0
                    ? globalLevel >= definition.globalLevelRequirement()
                    : definition.skillRequirement() != null
                    && ModAttachments.getProgress(player, definition.skillRequirement()).getLevel()
                    >= definition.skillLevelRequirement();

            if (eligible) {
                unlocked.add(definition.id());
                newlyUnlocked.add(definition);
            }
        }

        if (!unlocked.equals(current.unlockedTitleIds())) {
            ModAttachments.setTitleProfile(player, current.withUnlockedTitles(unlocked));
        }

        if (notify) {
            newlyUnlocked.stream()
                    .filter(TitleDefinition::selectable)
                    .forEach(definition -> notifyUnlock(player, definition));
        }
    }

    private static void notifyUnlock(ServerPlayerEntity player, TitleDefinition definition) {
        TitleProfile profile = sanitizeStoredProfile(player);
        TitleColor primary = TitleColor.fromId(profile.primaryColorId()).orElse(TitleColor.WHITE);
        TitleColor secondary = TitleColor.fromId(profile.secondaryColorId()).orElse(primary);
        TitleFinish finish = TitleFinish.fromId(profile.finishId()).orElse(TitleFinish.NONE);
        MutableText styledTitle = TitleTextFormatter.format(
                definition.localizedLiteral(player.getClientOptions().language()),
                primary,
                secondary,
                profile.gradient(),
                finish,
                true
        );

        player.sendMessage(
                Text.translatable("message.mythicrpg.title.unlocked", styledTitle)
                        .formatted(Formatting.GOLD),
                false
        );
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundCategory.PLAYERS,
                0.8F,
                1.0F
        );
    }
}
