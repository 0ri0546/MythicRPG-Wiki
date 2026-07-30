package com.mythicrpg.eating;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class SignatureDishData {
    private static final String ROOT = "mythicrpg_signature_dish";
    private static final String NAME = "name";
    private static final String ICON = "icon";
    private static final String BONUS = "bonus";
    private static final String DURATION = "duration";

    private SignatureDishData() {
    }

    public static void write(ItemStack stack, String customName, Identifier icon, SignatureBonus bonus, int durationTicks) {
        NbtCompound data = new NbtCompound();
        data.putString(NAME, sanitizeName(customName));
        data.putString(ICON, icon == null ? "minecraft:bowl" : icon.toString());
        data.putString(BONUS, bonus.id());
        data.putInt(DURATION, Math.max(200, Math.min(600, durationTicks)));
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.put(ROOT, data));
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(sanitizeName(customName)));
    }


    public static void clear(ItemStack stack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.remove(ROOT));
    }

    public static Optional<SignatureData> read(ItemStack stack) {
        NbtCompound custom = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!custom.contains(ROOT)) {
            return Optional.empty();
        }
        NbtCompound data = custom.getCompound(ROOT);
        String name = sanitizeName(data.getString(NAME));
        Identifier icon = Identifier.tryParse(data.getString(ICON));
        Optional<SignatureBonus> bonus = SignatureBonus.byId(data.getString(BONUS));
        if (name.isBlank() || icon == null || bonus.isEmpty() || !Registries.ITEM.containsId(icon)) {
            return Optional.empty();
        }
        return Optional.of(new SignatureData(
                name,
                icon,
                bonus.get(),
                Math.max(200, Math.min(600, data.getInt(DURATION)))
        ));
    }

    private static String sanitizeName(String value) {
        if (value == null) {
            return "Signature Dish";
        }
        String cleaned = value.strip().replaceAll("[\\p{Cntrl}]", "");
        if (cleaned.isBlank()) {
            return "Signature Dish";
        }
        return cleaned.length() > 32 ? cleaned.substring(0, 32) : cleaned;
    }

    public record SignatureData(String name, Identifier icon, SignatureBonus bonus, int durationTicks) {
    }
}
