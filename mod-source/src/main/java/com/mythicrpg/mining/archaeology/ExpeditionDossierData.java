package com.mythicrpg.mining.archaeology;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;
import java.util.UUID;

public final class ExpeditionDossierData {

    private static final String DOSSIER_ID = "mythicrpg_dossier_id";
    private static final String SITE_ID = "mythicrpg_grand_site_id";
    private static final String FAMILY = "mythicrpg_site_family";
    private static final String RARITY = "mythicrpg_site_rarity";
    private static final String BIOME = "mythicrpg_site_biome";
    private static final String MIN_X = "mythicrpg_site_min_x";
    private static final String MAX_X = "mythicrpg_site_max_x";
    private static final String MIN_Y = "mythicrpg_site_min_y";
    private static final String MAX_Y = "mythicrpg_site_max_y";
    private static final String MIN_Z = "mythicrpg_site_min_z";
    private static final String MAX_Z = "mythicrpg_site_max_z";

    private ExpeditionDossierData() {
    }

    public static ItemStack initialize(
            ItemStack stack,
            GrandFossilSiteState.GrandSiteRecord site
    ) {
        int minX = Math.floorDiv(site.center().getX(), 32) * 32;
        int minY = Math.floorDiv(site.center().getY(), 16) * 16;
        int minZ = Math.floorDiv(site.center().getZ(), 32) * 32;

        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            nbt.putString(DOSSIER_ID, UUID.randomUUID().toString());
            nbt.putString(SITE_ID, site.id().toString());
            nbt.putString(FAMILY, site.family().id());
            nbt.putString(RARITY, site.dominantRarity().id());
            nbt.putString(BIOME, site.biomeId());
            nbt.putInt(MIN_X, minX);
            nbt.putInt(MAX_X, minX + 31);
            nbt.putInt(MIN_Y, minY);
            nbt.putInt(MAX_Y, minY + 15);
            nbt.putInt(MIN_Z, minZ);
            nbt.putInt(MAX_Z, minZ + 31);
        });
        return stack;
    }

    public static Optional<Dossier> read(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!nbt.contains(SITE_ID)
                || !nbt.contains(FAMILY)
                || !nbt.contains(RARITY)
                || !nbt.contains(BIOME)) {
            return Optional.empty();
        }

        try {
            Optional<FossilFamily> family = FossilFamily.byId(nbt.getString(FAMILY));
            Optional<FossilRarity> rarity = FossilRarity.byId(nbt.getString(RARITY));
            if (family.isEmpty() || rarity.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Dossier(
                    UUID.fromString(nbt.getString(SITE_ID)),
                    family.get(),
                    rarity.get(),
                    nbt.getString(BIOME),
                    nbt.getInt(MIN_X),
                    nbt.getInt(MAX_X),
                    nbt.getInt(MIN_Y),
                    nbt.getInt(MAX_Y),
                    nbt.getInt(MIN_Z),
                    nbt.getInt(MAX_Z)
            ));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public record Dossier(
            UUID siteId,
            FossilFamily family,
            FossilRarity rarity,
            String biomeId,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ
    ) {
    }
}
