package com.mythicrpg.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientParticleManagerAccessor {
    @Accessor("particleManager")
    ParticleManager mythicrpg$getParticleManager();
}
