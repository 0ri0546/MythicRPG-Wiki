package com.mythicrpg.core;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModBlockTags {
    public static final TagKey<Block> ORES = TagKey.of(
            RegistryKeys.BLOCK,
            Identifier.of("mythicrpg", "ores")
    );
}