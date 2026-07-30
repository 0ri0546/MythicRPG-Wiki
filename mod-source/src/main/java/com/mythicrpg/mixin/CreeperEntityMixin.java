package com.mythicrpg.mixin;

import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin {

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$runnerBaronNoGriefExplosion(CallbackInfo ci) {
        CreeperEntity creeper = (CreeperEntity) (Object) this;

        if (BaronMobManager.getBaronType(creeper) != BaronType.RUNNER) {
            return;
        }

        if (!(creeper.getWorld() instanceof ServerWorld world)) {
            return;
        }

        world.createExplosion(
                creeper,
                creeper.getX(),
                creeper.getY(),
                creeper.getZ(),
                3.0F,
                World.ExplosionSourceType.NONE
        );

        creeper.discard();
        ci.cancel();
    }
}
