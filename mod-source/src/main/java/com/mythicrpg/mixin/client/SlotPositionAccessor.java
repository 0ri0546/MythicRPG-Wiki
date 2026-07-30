package com.mythicrpg.mixin.client;

import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client-only repositioning for the virtual Palette slots. */
@Mixin(Slot.class)
public interface SlotPositionAccessor {

    @Mutable
    @Accessor("x")
    void mythicrpg$setX(int x);

    @Mutable
    @Accessor("y")
    void mythicrpg$setY(int y);
}
