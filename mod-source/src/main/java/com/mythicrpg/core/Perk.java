package com.mythicrpg.core;

import net.minecraft.server.network.ServerPlayerEntity;

public interface Perk {
    void apply(ServerPlayerEntity player);
}