package com.mythicrpg.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves the few translated strings that must be expanded on the logical server.
 *
 * <p>Most visible text stays as {@code Text.translatable(...)} and is translated by
 * each client normally. Gradient titles are the exception: their translated text must
 * be known before the server can assign a color to every glyph for the scoreboard
 * prefix. Both values still come exclusively from the regular lang JSON files.</p>
 */
public final class MythicLanguageResolver {
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final Map<String, Map<String, String>> LANGUAGES = Map.of(
            DEFAULT_LANGUAGE, load(DEFAULT_LANGUAGE),
            "fr_fr", load("fr_fr")
    );

    private MythicLanguageResolver() {
    }

    public static String resolve(String languageCode, String translationKey) {
        String language = normalizeLanguage(languageCode);
        Map<String, String> selected = LANGUAGES.getOrDefault(language, LANGUAGES.get(DEFAULT_LANGUAGE));
        String translated = selected.get(translationKey);
        if (translated != null) {
            return translated;
        }

        return LANGUAGES.get(DEFAULT_LANGUAGE).getOrDefault(translationKey, translationKey);
    }

    private static String normalizeLanguage(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return DEFAULT_LANGUAGE;
        }

        String normalized = languageCode.toLowerCase(Locale.ROOT).replace('-', '_');
        return normalized.startsWith("fr") ? "fr_fr" : DEFAULT_LANGUAGE;
    }

    private static Map<String, String> load(String language) {
        String resourcePath = "assets/mythicrpg/lang/" + language + ".json";
        ClassLoader classLoader = MythicLanguageResolver.class.getClassLoader();

        try (InputStream stream = classLoader.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return Map.of();
            }

            JsonObject json;
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }

            Map<String, String> entries = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isString()) {
                    entries.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            return Collections.unmodifiableMap(entries);
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
