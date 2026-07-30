package com.mythicrpg.mixin.client;

import com.mythicrpg.client.MythicClientPreferences;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AccessibilityOptionsScreen.class)
public abstract class AccessibilityOptionsScreenMixin {
    @Inject(method = "addOptions", at = @At("TAIL"))
    private void mythicrpg$addDoubleJumpToggle(
            CallbackInfo ci
    ) {
        ((GameOptionsScreenAccessor) this)
                .mythicrpg$getBody()
                .addSingleOptionEntry(
                        MythicClientPreferences.doubleJumpOption()
                );

        ((GameOptionsScreenAccessor) this)
                .mythicrpg$getBody()
                .addSingleOptionEntry(
                        MythicClientPreferences.buildingMagnetOption()
                );

        ((GameOptionsScreenAccessor) this)
                .mythicrpg$getBody()
                .addSingleOptionEntry(
                        MythicClientPreferences.veinMiningOption()
                );

        ((GameOptionsScreenAccessor) this)
                .mythicrpg$getBody()
                .addSingleOptionEntry(
                        MythicClientPreferences.staticDecorationsOption()
                );
    }
}
