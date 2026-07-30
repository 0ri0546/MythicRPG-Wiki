package com.mythicrpg.mining.archaeology;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;
import java.util.UUID;

public final class FossilSpecimenData {

    private static final String FAMILY_KEY = "mythicrpg_fossil_family";
    private static final String RARITY_KEY = "mythicrpg_fossil_rarity";
    private static final String SPECIMEN_ID_KEY = "mythicrpg_specimen_id";
    private static final String RECONSTRUCTED_BY_KEY = "mythicrpg_reconstructed_by";
    private static final String RECONSTRUCTED_DAY_KEY = "mythicrpg_reconstructed_day";
    private static final String ANALYZED_KEY = "mythicrpg_analyzed";
    private static final String ANALYZED_SITE_KEY = "mythicrpg_analyzed_site";

    private FossilSpecimenData() {
    }

    public static ItemStack initializeSkeleton(
            ItemStack stack,
            FossilFamily family,
            FossilRarity rarity,
            UUID playerUuid,
            long reconstructedDay
    ) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.putString(FAMILY_KEY, family.id());
            nbt.putString(RARITY_KEY, rarity.id());
            nbt.putString(SPECIMEN_ID_KEY, UUID.randomUUID().toString());
            nbt.putString(RECONSTRUCTED_BY_KEY, playerUuid.toString());
            nbt.putLong(RECONSTRUCTED_DAY_KEY, reconstructedDay);
            nbt.putBoolean(ANALYZED_KEY, false);
        });
        return stack;
    }

    public static Optional<Specimen> read(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = component.copyNbt();

        if (!nbt.contains(FAMILY_KEY) || !nbt.contains(RARITY_KEY) || !nbt.contains(SPECIMEN_ID_KEY)) {
            return Optional.empty();
        }

        Optional<FossilFamily> family = FossilFamily.byId(nbt.getString(FAMILY_KEY));
        Optional<FossilRarity> rarity = FossilRarity.byId(nbt.getString(RARITY_KEY));

        if (family.isEmpty() || rarity.isEmpty()) {
            return Optional.empty();
        }

        try {
            UUID specimenId = UUID.fromString(nbt.getString(SPECIMEN_ID_KEY));
            UUID reconstructedBy = UUID.fromString(nbt.getString(RECONSTRUCTED_BY_KEY));
            return Optional.of(new Specimen(
                    family.get(),
                    rarity.get(),
                    specimenId,
                    reconstructedBy,
                    nbt.getLong(RECONSTRUCTED_DAY_KEY),
                    nbt.getBoolean(ANALYZED_KEY)
            ));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }


    public static Optional<UUID> analyzedSiteId(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        ).copyNbt();
        if (!nbt.contains(ANALYZED_SITE_KEY)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(nbt.getString(ANALYZED_SITE_KEY)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static void markAnalyzed(ItemStack stack, UUID siteId) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.putBoolean(ANALYZED_KEY, true);
            if (siteId != null) {
                nbt.putString(ANALYZED_SITE_KEY, siteId.toString());
            }
        });
    }

    public static void markAnalyzed(ItemStack stack) {
        markAnalyzed(stack, null);
    }

    public static void markUnanalyzed(ItemStack stack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.putBoolean(ANALYZED_KEY, false);
            nbt.remove(ANALYZED_SITE_KEY);
        });
    }

    public record Specimen(
            FossilFamily family,
            FossilRarity rarity,
            UUID specimenId,
            UUID reconstructedBy,
            long reconstructedDay,
            boolean analyzed
    ) {
    }
}
