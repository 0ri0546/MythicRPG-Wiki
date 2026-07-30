package com.mythicrpg.mixin;

import com.mythicrpg.traveling.MountSoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityLandMountSoundMixin {
    @Inject(method = "playSound(Lnet/minecraft/sound/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$hideAdoptedMountAggressiveSounds(
            SoundEvent sound,
            float volume,
            float pitch,
            CallbackInfo ci
    ) {
        Entity self = (Entity) (Object) this;

        if (MountSoundManager.shouldBlock(self, sound)) {
            ci.cancel();
        }
    }
}
