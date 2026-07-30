package com.mythicrpg.mixin.client;

import com.mythicrpg.client.mining.relic.PaletteClientManager;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MousePaletteMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$extendedPaletteHotbar(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (PaletteClientManager.scroll(vertical)) ci.cancel();
    }
}
