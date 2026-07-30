package com.mythicrpg.mixin.client;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ParticleManager.class)
public interface ParticleManagerAccessor {
    /**
     * The concrete map value is ParticleManager.SimpleSpriteProvider, a private
     * vanilla class which implements the public SpriteProvider interface.
     * Keeping the accessor value wildcarded avoids exposing that private type.
     */
    @Accessor("spriteAwareFactories")
    Map<Identifier, ?> mythicrpg$getSpriteAwareFactories();
}
