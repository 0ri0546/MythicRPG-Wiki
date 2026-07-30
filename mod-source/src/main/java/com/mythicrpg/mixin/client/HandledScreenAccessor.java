package com.mythicrpg.mixin.client;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {

    @Accessor("x")
    int mythicrpg$getX();

    @Accessor("y")
    int mythicrpg$getY();

    @Accessor("backgroundWidth")
    int mythicrpg$getBackgroundWidth();
}