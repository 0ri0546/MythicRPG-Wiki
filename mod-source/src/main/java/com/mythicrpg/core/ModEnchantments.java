package com.mythicrpg.core;

import com.mythicrpg.MythicRPG;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEnchantments {

    public static final RegistryKey<Enchantment> GROWTH = RegistryKey.of(
            RegistryKeys.ENCHANTMENT,
            Identifier.of(MythicRPG.MOD_ID, "growth")
    );

    public static final RegistryKey<Enchantment> PORTABLE_FRIDGE = RegistryKey.of(
            RegistryKeys.ENCHANTMENT,
            Identifier.of(MythicRPG.MOD_ID, "portable_fridge")
    );

    private ModEnchantments() {
    }
}
