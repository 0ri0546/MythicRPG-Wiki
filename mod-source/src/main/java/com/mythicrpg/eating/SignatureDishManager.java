package com.mythicrpg.eating;

import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.PlayerCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;

public final class SignatureDishManager {
    private static final int COOKING_COOLDOWN = 60 * 20;
    private static final int SAVE_COOLDOWN = 10;

    private SignatureDishManager() {
    }

    /** Saves the player's single signature-dish recipe. No ingredients are moved here. */
    public static void handle(ServerPlayerEntity player, SignatureDishCreatePayload payload) {
        Hand hand = payload.handId() == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        ItemStack notebook = player.getStackInHand(hand);
        if (!notebook.isOf(ModItems.CHEF_NOTEBOOK) || !EatingPerks.hasSignatureDish(player)) {
            return;
        }
        if (!PlayerCooldownManager.tryUse(player, "signature_dish_save", SAVE_COOLDOWN)) {
            return;
        }

        ArrayList<SignatureIngredient> ingredients = new ArrayList<>();
        Set<Integer> reservedSlots = new HashSet<>();
        for (String rawId : payload.ingredientIds()) {
            Identifier id = Identifier.tryParse(rawId);
            if (id == null || !Registries.ITEM.containsId(id)) {
                rejectInvalid(player);
                return;
            }
            int foundSlot = -1;
            for (int slot = 0; slot < player.getInventory().size(); slot++) {
                if (reservedSlots.contains(slot)) {
                    continue;
                }
                ItemStack candidate = player.getInventory().getStack(slot);
                if (!candidate.isEmpty()
                        && Registries.ITEM.getId(candidate.getItem()).equals(id)
                        && CulinaryIngredientRegistry.isCulinaryIngredient(candidate)) {
                    foundSlot = slot;
                    break;
                }
            }
            SignatureIngredient descriptor = foundSlot < 0
                    ? null
                    : SignatureIngredient.fromStack(player.getInventory().getStack(foundSlot)).orElse(null);
            if (descriptor == null || ingredients.stream().anyMatch(existing -> existing.itemId().equals(id))) {
                rejectInvalid(player);
                return;
            }
            reservedSlots.add(foundSlot);
            ingredients.add(descriptor);
        }
        Identifier icon = Identifier.tryParse(payload.iconId());
        ChefNotebookData.Configuration configuration = new ChefNotebookData.Configuration(
                payload.name(),
                ingredients,
                icon,
                SignatureBonus.byOrdinal(payload.bonusId())
        );
        if (!configuration.isValid()) {
            rejectInvalid(player);
            return;
        }

        SignatureDishProfile updatedProfile = SignatureDishProfile.from(configuration);
        if (updatedProfile.equals(ModAttachments.getSignatureDishProfile(player))) {
            return;
        }
        ModAttachments.setSignatureDishProfile(player, updatedProfile);
        EatingDeliveryManager.invalidatePreparedSignatures(player.getUuid());
        clearLegacyNotebookData(player);
        player.getInventory().markDirty();
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ITEM_BOOK_PAGE_TURN,
                SoundCategory.PLAYERS,
                0.8F,
                1.1F
        );
        player.sendMessage(Text.translatable("message.mythicrpg.eating.signature_saved")
                .formatted(Formatting.GOLD), true);
    }

    /** Returns the unique player-owned recipe, migrating old notebook-local data once. */
    public static Optional<ChefNotebookData.Configuration> getConfiguration(ServerPlayerEntity player) {
        Optional<ChefNotebookData.Configuration> current = ModAttachments
                .getSignatureDishProfile(player)
                .configuration();
        if (current.isPresent()) {
            return current;
        }

        Optional<ChefNotebookData.Configuration> legacy = findLegacyConfiguration(player);
        if (legacy.isPresent()) {
            ModAttachments.setSignatureDishProfile(player, SignatureDishProfile.from(legacy.get()));
            clearLegacyNotebookData(player);
            player.getInventory().markDirty();
        }
        return legacy;
    }

    /** Moves the configured ingredients into an empty pot without consuming them. */
    public static boolean preparePot(ServerPlayerEntity player, CookingPotBlockEntity pot) {
        if (!EatingPerks.hasSignatureDish(player)) {
            player.sendMessage(Text.translatable("message.mythicrpg.eating.signature_locked")
                    .formatted(Formatting.RED), true);
            return false;
        }
        Optional<ChefNotebookData.Configuration> configuration = getConfiguration(player);
        if (configuration.isEmpty()) {
            player.sendMessage(Text.translatable("message.mythicrpg.eating.signature_no_configuration")
                    .formatted(Formatting.RED), true);
            return false;
        }
        CookingPotBlockEntity.SignaturePrepareResult result = pot.prepareSignatureIngredients(
                player,
                configuration.get(),
                EatingPerks.maxIngredients(player)
        );
        if (result != CookingPotBlockEntity.SignaturePrepareResult.SUCCESS) {
            String key = switch (result) {
                case POT_NOT_EMPTY -> "message.mythicrpg.eating.signature_pot_not_empty";
                case TOO_MANY_INGREDIENTS -> "message.mythicrpg.eating.signature_not_enough_slots";
                case MISSING_INGREDIENT -> "message.mythicrpg.eating.signature_missing_ingredient";
                default -> "message.mythicrpg.eating.signature_invalid";
            };
            player.sendMessage(Text.translatable(key).formatted(Formatting.RED), true);
            return false;
        }
        player.getWorld().playSound(
                null,
                pot.getPos(),
                SoundEvents.ITEM_BUNDLE_INSERT,
                SoundCategory.PLAYERS,
                0.8F,
                1.05F
        );
        player.sendMessage(Text.translatable("message.mythicrpg.eating.signature_pot_prepared")
                .formatted(Formatting.GREEN), true);
        return true;
    }

    /** Starts the previously prepared signature dish and applies its cooldown. */
    public static boolean startPreparedCooking(ServerPlayerEntity player, CookingPotBlockEntity pot) {
        if (!pot.hasHeatSource()) {
            player.sendMessage(Text.translatable("message.mythicrpg.eating.signature_no_heat")
                    .formatted(Formatting.RED), true);
            return false;
        }
        long now = player.getWorld().getTime();
        EatingRuntimeData runtime = ModAttachments.getEatingRuntime(player);
        if (now < runtime.signatureReadyAt()) {
            long seconds = Math.max(1L, (runtime.signatureReadyAt() - now + 19L) / 20L);
            player.sendMessage(Text.translatable("message.mythicrpg.eating.signature_cooldown", seconds)
                    .formatted(Formatting.RED), true);
            return false;
        }
        ChefNotebookData.Configuration configuration = getConfiguration(player).orElse(null);
        if (!pot.startPreparedSignatureCooking(
                player,
                configuration,
                EatingPerks.maxIngredients(player)
        )) {
            player.sendMessage(Text.translatable("message.mythicrpg.eating.signature_preparation_changed")
                    .formatted(Formatting.RED), true);
            return false;
        }
        ModAttachments.setEatingRuntime(player, runtime.withSignatureReadyAt(now + COOKING_COOLDOWN));
        player.sendMessage(Text.translatable("message.mythicrpg.eating.signature_started")
                .formatted(Formatting.GOLD), true);
        return true;
    }

    private static void rejectInvalid(ServerPlayerEntity player) {
        player.sendMessage(Text.translatable("message.mythicrpg.eating.signature_invalid")
                .formatted(Formatting.RED), true);
    }

    private static Optional<ChefNotebookData.Configuration> findLegacyConfiguration(ServerPlayerEntity player) {
        Optional<ChefNotebookData.Configuration> main = configurationFrom(player.getMainHandStack());
        if (main.isPresent()) {
            return main;
        }
        Optional<ChefNotebookData.Configuration> off = configurationFrom(player.getOffHandStack());
        if (off.isPresent()) {
            return off;
        }
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            Optional<ChefNotebookData.Configuration> configuration = configurationFrom(
                    player.getInventory().getStack(slot)
            );
            if (configuration.isPresent()) {
                return configuration;
            }
        }
        return Optional.empty();
    }

    private static Optional<ChefNotebookData.Configuration> configurationFrom(ItemStack stack) {
        return stack.isOf(ModItems.CHEF_NOTEBOOK) ? ChefNotebookData.read(stack) : Optional.empty();
    }

    private static void clearLegacyNotebookData(ServerPlayerEntity player) {
        clearLegacyNotebook(player.getMainHandStack());
        clearLegacyNotebook(player.getOffHandStack());
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            clearLegacyNotebook(player.getInventory().getStack(slot));
        }
    }

    private static void clearLegacyNotebook(ItemStack stack) {
        if (stack.isOf(ModItems.CHEF_NOTEBOOK)) {
            ChefNotebookData.clear(stack);
        }
    }
}
