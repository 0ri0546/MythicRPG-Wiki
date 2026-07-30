package com.mythicrpg.fishing;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum FishingFamily {
    SALMON,
    CRUSTACEAN,
    SHARK,
    INFERNAL,
    VOID;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<FishingFamily> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (FishingFamily value : values()) {
            if (value.id().equalsIgnoreCase(id)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public Text displayName() {
        return Text.translatable("fishing.family.mythicrpg." + id());
    }

    public static FishingFamily select(World world, BlockPos pos, Random random) {
        RegistryKey<World> key = world.getRegistryKey();
        if (key.equals(World.NETHER)) {
            return INFERNAL;
        }
        if (key.equals(World.END)) {
            return VOID;
        }

        FishingFamily primary = primaryOverworld(world.getBiome(pos));
        if (world instanceof ServerWorld serverWorld) {
            FishingWeatherManager.Mode local = FishingWeatherManager.modeAt(serverWorld, pos);
            if (local == FishingWeatherManager.Mode.RAIN) {
                primary = SALMON;
            } else if (local == FishingWeatherManager.Mode.SUN) {
                primary = CRUSTACEAN;
            } else if (local == FishingWeatherManager.Mode.STORM) {
                primary = SHARK;
            }
        }

        if (random.nextInt(100) < 75) {
            return primary;
        }
        List<FishingFamily> others = switch (primary) {
            case SALMON -> List.of(CRUSTACEAN, SHARK);
            case CRUSTACEAN -> List.of(SALMON, SHARK);
            case SHARK -> List.of(SALMON, CRUSTACEAN);
            default -> List.of(SALMON, CRUSTACEAN);
        };
        return others.get(random.nextInt(others.size()));
    }

    private static FishingFamily primaryOverworld(RegistryEntry<Biome> biome) {
        String id = biome.getKey().map(key -> key.getValue().getPath()).orElse("");
        if (id.contains("river") || id.contains("cold") || id.contains("frozen")) {
            return SALMON;
        }
        if (id.contains("beach")
                || id.contains("warm_ocean")
                || id.contains("mangrove")
                || id.contains("swamp")) {
            return CRUSTACEAN;
        }
        if (id.contains("deep_ocean") || id.contains("ocean")) {
            return SHARK;
        }
        return SALMON;
    }
}
