
package com.mythicrpg.mixin;

import com.mythicrpg.fishing.FishingManager;
import com.mythicrpg.fishing.FishingRodLoadout;
import com.mythicrpg.fishing.FishingWeatherManager;
import com.mythicrpg.fishing.MythicFishingRodItem;
import com.mythicrpg.fishing.SeaMonsterManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Narrow integration with vanilla fishing.
 *
 * <p>The bobber position is captured before vanilla removes the entity. This is
 * essential: after {@code use(...)} returns, {@code player.fishHook} is already
 * cleared and using the player's feet as the catch position makes valid catches
 * disappear whenever the player is standing on land.</p>
 */
@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {
    @Shadow private int hookCountdown;
    @Shadow private int waitCountdown;

    @Unique private boolean mythicrpg$successfulCatch;
    @Unique private boolean mythicrpg$customRodCatch;
    @Unique private BlockPos mythicrpg$catchPos = BlockPos.ORIGIN;
    @Unique private ServerPlayerEntity mythicrpg$reelPlayer;

    @Inject(method = "use", at = @At("HEAD"))
    private void mythicrpg$captureReelContext(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        mythicrpg$successfulCatch = hookCountdown > 0;
        mythicrpg$customRodCatch = mythicrpg$successfulCatch
                && rod.getItem() instanceof MythicFishingRodItem;
        mythicrpg$catchPos = self.getBlockPos().toImmutable();
        mythicrpg$reelPlayer = self.getPlayerOwner() instanceof ServerPlayerEntity player
                ? player
                : null;
    }

    @Redirect(
            method = "use",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z")
    )
    private boolean mythicrpg$replaceVanillaFishingLoot(World world, Entity entity) {
        if (mythicrpg$customRodCatch && entity instanceof ItemEntity) {
            return true;
        }
        return world.spawnEntity(entity);
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void mythicrpg$finishCatch(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        if (self.getWorld().isClient()
                || !mythicrpg$successfulCatch
                || cir.getReturnValueI() <= 0
                || mythicrpg$reelPlayer == null) {
            return;
        }
        FishingManager.handleSuccessfulReel(
                mythicrpg$reelPlayer,
                rod,
                mythicrpg$catchPos
        );
    }

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void mythicrpg$damageSeaMonsterWithHook(
            EntityHitResult hitResult,
            CallbackInfo ci
    ) {
        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        if (!self.getWorld().isClient()) {
            SeaMonsterManager.tryDamageWithBobber(self, hitResult.getEntity());
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void mythicrpg$applyFishingSpeedModifiers(CallbackInfo ci) {
        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        if (!(self.getWorld() instanceof ServerWorld world)
                || !(self.getPlayerOwner() instanceof ServerPlayerEntity player)) {
            return;
        }

        ItemStack rod = mythicrpg$activeRod(player);
        if (!(rod.getItem() instanceof MythicFishingRodItem)) {
            return;
        }

        FishingRodLoadout loadout = FishingRodLoadout.read(rod);
        int extraProgress = loadout.speedRune() && Math.floorMod(self.age, 3) == 0 ? 1 : 0;
        FishingWeatherManager.Mode localWeather = FishingWeatherManager.modeAt(world, self.getBlockPos());
        if (localWeather == FishingWeatherManager.Mode.RAIN && Math.floorMod(self.age, 4) == 0) {
            extraProgress++;
        }
        if (localWeather == FishingWeatherManager.Mode.STORM && Math.floorMod(self.age, 2) == 0) {
            extraProgress++;
        }
        if (waitCountdown > 0 && extraProgress > 0) {
            waitCountdown = Math.max(0, waitCountdown - extraProgress);
        }
    }

    @Redirect(
            method = "removeIfInvalid",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z")
    )
    private boolean mythicrpg$acceptCustomFishingRods(ItemStack stack, Item expectedItem) {
        return stack.isOf(expectedItem) || stack.getItem() instanceof MythicFishingRodItem;
    }

    @Redirect(
            method = {"tick", "tickFishingLogic"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/fluid/FluidState;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"
            ),
            require = 0
    )
    private boolean mythicrpg$treatNetherLavaAsFishingFluid(FluidState state, TagKey<Fluid> tag) {
        boolean vanilla = state.isIn(tag);
        if (vanilla || !tag.equals(FluidTags.WATER)) {
            return vanilla;
        }

        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        if (!(self.getPlayerOwner() instanceof PlayerEntity player)) {
            return false;
        }
        ItemStack rod = mythicrpg$activeRod(player);
        return rod.getItem() instanceof MythicFishingRodItem custom
                && custom.forcedFamily() == com.mythicrpg.fishing.FishingFamily.INFERNAL
                && state.isIn(FluidTags.LAVA);
    }

    @Unique
    private static ItemStack mythicrpg$activeRod(PlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof MythicFishingRodItem) {
            return player.getMainHandStack();
        }
        if (player.getOffHandStack().getItem() instanceof MythicFishingRodItem) {
            return player.getOffHandStack();
        }
        return ItemStack.EMPTY;
    }
}
