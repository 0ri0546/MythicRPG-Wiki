package com.mythicrpg.mixin;

import com.mythicrpg.traveling.TravelerBoatEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BoatEntity.class)
public abstract class BoatEntityMixin {

    @ModifyConstant(
            method = "updatePaddles",
            constant = @Constant(floatValue = 0.04F)
    )
    private float mythicrpg$increaseTravelerBoatForwardAcceleration(float original) {
        return (Object) this instanceof TravelerBoatEntity
                ? original * TravelerBoatEntity.SPEED_MULTIPLIER
                : original;
    }

    @ModifyConstant(
            method = "updatePaddles",
            constant = @Constant(floatValue = 0.005F)
    )
    private float mythicrpg$increaseTravelerBoatSecondaryAcceleration(float original) {
        return (Object) this instanceof TravelerBoatEntity
                ? original * TravelerBoatEntity.SPEED_MULTIPLIER
                : original;
    }
}
