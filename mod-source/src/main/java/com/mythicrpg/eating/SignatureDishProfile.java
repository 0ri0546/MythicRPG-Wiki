package com.mythicrpg.eating;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The single persistent signature-dish recipe owned by a player. */
public record SignatureDishProfile(
        String name,
        List<String> ingredientIds,
        String iconId,
        String bonusId
) {
    public static final Codec<SignatureDishProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(SignatureDishProfile::name),
            Codec.STRING.listOf().optionalFieldOf("ingredients", List.<String>of()).forGetter(SignatureDishProfile::ingredientIds),
            Codec.STRING.optionalFieldOf("icon", "").forGetter(SignatureDishProfile::iconId),
            Codec.STRING.optionalFieldOf("bonus", SignatureBonus.DAMAGE.id()).forGetter(SignatureDishProfile::bonusId)
    ).apply(instance, SignatureDishProfile::new));

    public SignatureDishProfile {
        name = name == null ? "" : ChefNotebookData.sanitizeName(name);
        ingredientIds = List.copyOf(ingredientIds == null ? List.of() : ingredientIds);
        iconId = iconId == null ? "" : iconId;
        bonusId = bonusId == null ? SignatureBonus.DAMAGE.id() : bonusId;
    }

    public static SignatureDishProfile empty() {
        return new SignatureDishProfile("", List.of(), "", SignatureBonus.DAMAGE.id());
    }

    public static SignatureDishProfile from(ChefNotebookData.Configuration configuration) {
        if (configuration == null || !configuration.isValid()) {
            return empty();
        }
        return new SignatureDishProfile(
                configuration.name(),
                configuration.ingredients().stream().map(SignatureIngredient::serialize).toList(),
                configuration.icon().toString(),
                configuration.bonus().id()
        );
    }

    public Optional<ChefNotebookData.Configuration> configuration() {
        if (ingredientIds.size() < 2 || ingredientIds.size() > 5) {
            return Optional.empty();
        }
        ArrayList<SignatureIngredient> ingredients = new ArrayList<>(ingredientIds.size());
        for (String raw : ingredientIds) {
            SignatureIngredient descriptor = SignatureIngredient.parse(raw).orElse(null);
            if (descriptor == null || ingredients.stream().anyMatch(existing -> existing.itemId().equals(descriptor.itemId()))) {
                return Optional.empty();
            }
            ingredients.add(descriptor);
        }
        Identifier icon = Identifier.tryParse(iconId);
        SignatureBonus bonus = SignatureBonus.byId(bonusId).orElse(null);
        ChefNotebookData.Configuration configuration = new ChefNotebookData.Configuration(name, ingredients, icon, bonus);
        return configuration.isValid() ? Optional.of(configuration) : Optional.empty();
    }
}
