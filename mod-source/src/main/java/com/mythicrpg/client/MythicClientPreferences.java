package com.mythicrpg.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.building.BuildingMagnetTogglePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import com.mythicrpg.mining.VeinMiningTogglePayload;

/**
 * Small, client-only preference store for MythicRPG comfort settings.
 *
 * <p>The file is intentionally independent from Minecraft's options.txt: this keeps the
 * settings stable without mixing custom serialization into {@code GameOptions}, while the
 * options themselves can still be presented inside vanilla option screens.</p>
 */
public final class MythicClientPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger("mythicrpg-client-preferences");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("mythicrpg-client.json");

    private static final String DOUBLE_JUMP_KEY = "double_jump_enabled";
    private static final String BUILDING_MAGNET_KEY = "building_magnet_enabled";
    private static final String VEIN_MINING_KEY = "vein_mining_enabled";
    private static final String LAST_SKILL_KEY = "last_opened_skill";
    private static final String STATIC_DECORATIONS_KEY = "static_decorations_enabled";

    private static boolean doubleJumpEnabled = true;
    private static boolean buildingMagnetEnabled = true;
    private static boolean veinMiningEnabled = true;
    private static SkillType lastOpenedSkill = SkillType.MINING;
    private static boolean staticDecorationsEnabled = true;

    static {
        load();
    }

    private static final SimpleOption<Boolean> DOUBLE_JUMP_OPTION = SimpleOption.ofBoolean(
            "options.mythicrpg.double_jump",
            SimpleOption.constantTooltip(Text.translatable("options.mythicrpg.double_jump.tooltip")),
            doubleJumpEnabled,
            MythicClientPreferences::setDoubleJumpEnabled
    );

    private static final SimpleOption<Boolean> BUILDING_MAGNET_OPTION = SimpleOption.ofBoolean(
            "options.mythicrpg.building_magnet",
            SimpleOption.constantTooltip(Text.translatable("options.mythicrpg.building_magnet.tooltip")),
            buildingMagnetEnabled,
            MythicClientPreferences::setBuildingMagnetEnabled
    );

    private static final SimpleOption<Boolean> VEIN_MINING_OPTION =
            SimpleOption.ofBoolean(
                    "options.mythicrpg.vein_mining",
                    SimpleOption.constantTooltip(
                            Text.translatable(
                                    "options.mythicrpg.vein_mining.tooltip"
                            )
                    ),
                    veinMiningEnabled,
                    MythicClientPreferences::setVeinMiningEnabled
            );

    private static final SimpleOption<Boolean> STATIC_DECORATIONS_OPTION = SimpleOption.ofBoolean(
            "options.mythicrpg.static_decorations",
            SimpleOption.constantTooltip(Text.translatable("options.mythicrpg.static_decorations.tooltip")),
            staticDecorationsEnabled,
            MythicClientPreferences::setStaticDecorationsEnabled
    );

    private MythicClientPreferences() {
    }

    /** Forces the preference file to be loaded during client initialization. */
    public static void initialize() {
        // Static initialization performs the work.
    }

    public static SimpleOption<Boolean> doubleJumpOption() {
        return DOUBLE_JUMP_OPTION;
    }

    public static boolean isDoubleJumpEnabled() {
        return doubleJumpEnabled;
    }

    public static SimpleOption<Boolean> buildingMagnetOption() {
        return BUILDING_MAGNET_OPTION;
    }

    public static boolean isBuildingMagnetEnabled() {
        return buildingMagnetEnabled;
    }

    public static SimpleOption<Boolean> veinMiningOption() {
        return VEIN_MINING_OPTION;
    }

    public static boolean isVeinMiningEnabled() {
        return veinMiningEnabled;
    }

    public static SimpleOption<Boolean> staticDecorationsOption() {
        return STATIC_DECORATIONS_OPTION;
    }

    public static boolean areStaticDecorationsEnabled() {
        return staticDecorationsEnabled;
    }

    public static SkillType getLastOpenedSkill() {
        return lastOpenedSkill;
    }

    public static synchronized void setDoubleJumpEnabled(boolean enabled) {
        if (doubleJumpEnabled == enabled) {
            return;
        }

        doubleJumpEnabled = enabled;
        save();
    }

    public static synchronized void setBuildingMagnetEnabled(boolean enabled) {
        if (buildingMagnetEnabled == enabled) {
            return;
        }

        buildingMagnetEnabled = enabled;
        save();
        syncBuildingMagnetPreference();
    }

    public static synchronized void setVeinMiningEnabled(
            boolean enabled
    ) {
        if (veinMiningEnabled == enabled) {
            return;
        }

        veinMiningEnabled = enabled;
        save();
        syncVeinMiningPreference();
    }

    public static synchronized void setStaticDecorationsEnabled(boolean enabled) {
        if (staticDecorationsEnabled == enabled) return;
        staticDecorationsEnabled = enabled;
        save();
    }

    public static void syncBuildingMagnetPreference() {
        if (ClientPlayNetworking.canSend(BuildingMagnetTogglePayload.ID)) {
            ClientPlayNetworking.send(new BuildingMagnetTogglePayload(buildingMagnetEnabled));
        }
    }

    public static void syncVeinMiningPreference() {
        if (ClientPlayNetworking.canSend(
                VeinMiningTogglePayload.ID
        )) {
            ClientPlayNetworking.send(
                    new VeinMiningTogglePayload(
                            veinMiningEnabled
                    )
            );
        }
    }

    public static synchronized void rememberLastOpenedSkill(SkillType skill) {
        if (skill == null || lastOpenedSkill == skill) {
            return;
        }

        lastOpenedSkill = skill;
        save();
    }

    private static void load() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has(DOUBLE_JUMP_KEY) && root.get(DOUBLE_JUMP_KEY).isJsonPrimitive()) {
                doubleJumpEnabled = root.get(DOUBLE_JUMP_KEY).getAsBoolean();
            }

            if (root.has(BUILDING_MAGNET_KEY) && root.get(BUILDING_MAGNET_KEY).isJsonPrimitive()) {
                buildingMagnetEnabled = root.get(BUILDING_MAGNET_KEY).getAsBoolean();
            }

            if (root.has(VEIN_MINING_KEY) && root.get(VEIN_MINING_KEY).isJsonPrimitive()) {
                veinMiningEnabled = root.get(VEIN_MINING_KEY).getAsBoolean();
            }

            if (root.has(STATIC_DECORATIONS_KEY) && root.get(STATIC_DECORATIONS_KEY).isJsonPrimitive()) {
                staticDecorationsEnabled = root.get(STATIC_DECORATIONS_KEY).getAsBoolean();
            }

            if (root.has(LAST_SKILL_KEY) && root.get(LAST_SKILL_KEY).isJsonPrimitive()) {
                String savedSkill = root.get(LAST_SKILL_KEY).getAsString();
                lastOpenedSkill = SkillType.fromId(savedSkill).orElseGet(() -> {
                    LOGGER.warn("Unknown saved MythicRPG skill '{}'; falling back to Mining", savedSkill);
                    return SkillType.MINING;
                });
            }
        } catch (Exception exception) {
            LOGGER.warn("Could not read MythicRPG client preferences from {}. Defaults will be used.",
                    CONFIG_PATH, exception);
            doubleJumpEnabled = true;
            buildingMagnetEnabled = true;
            veinMiningEnabled = true;
            staticDecorationsEnabled = true;
            lastOpenedSkill = SkillType.MINING;
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty(DOUBLE_JUMP_KEY, doubleJumpEnabled);
        root.addProperty(BUILDING_MAGNET_KEY, buildingMagnetEnabled);
        root.addProperty(VEIN_MINING_KEY, veinMiningEnabled);
        root.addProperty(STATIC_DECORATIONS_KEY, staticDecorationsEnabled);
        root.addProperty(LAST_SKILL_KEY, lastOpenedSkill.name());

        Path parent = CONFIG_PATH.getParent();
        Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");

        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    temporary,
                    GSON.toJson(root) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            try {
                Files.move(
                        temporary,
                        CONFIG_PATH,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not save MythicRPG client preferences to {}", CONFIG_PATH, exception);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupException) {
                LOGGER.debug("Could not delete temporary MythicRPG preference file {}", temporary,
                        cleanupException);
            }
        }
    }
}
