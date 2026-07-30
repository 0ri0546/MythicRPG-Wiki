package com.mythicrpg.mixin;

import com.mythicrpg.eating.EatingBalance;
import com.mythicrpg.eating.EatingXpManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(CakeBlock.class)
public abstract class CakeBlockEatingMixin {
    @Unique
    private static final ThreadLocal<Deque<CakeUseContext>> MYTHICRPG$CAKE_CONTEXT = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "tryEat", at = @At("HEAD"))
    private static void mythicrpg$beforeCakeSlice(
            WorldAccess world,
            BlockPos pos,
            BlockState state,
            PlayerEntity player,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        Deque<CakeUseContext> contexts = MYTHICRPG$CAKE_CONTEXT.get();
        contexts.push(new CakeUseContext(null, 0, 0.0F));
        if (!(player instanceof ServerPlayerEntity serverPlayer) || !serverPlayer.canConsume(false)) {
            return;
        }
        var hunger = serverPlayer.getHungerManager();
        contexts.pop();
        contexts.push(new CakeUseContext(
                serverPlayer,
                serverPlayer.getHungerManager().getFoodLevel(),
                hunger.getSaturationLevel()
        ));
    }

    @Inject(method = "tryEat", at = @At("RETURN"))
    private static void mythicrpg$afterCakeSlice(
            WorldAccess world,
            BlockPos pos,
            BlockState state,
            PlayerEntity player,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        Deque<CakeUseContext> contexts = MYTHICRPG$CAKE_CONTEXT.get();
        CakeUseContext context = contexts.isEmpty() ? null : contexts.pop();
        if (contexts.isEmpty()) {
            MYTHICRPG$CAKE_CONTEXT.remove();
        }
        if (context == null
                || context.player() != player
                || context.player().getHungerManager().getFoodLevel() <= context.foodBefore()) {
            return;
        }

        var hunger = context.player().getHungerManager();
        float after = hunger.getSaturationLevel();
        float gained = Math.max(0.0F, after - context.saturationBefore());
        if (gained > 0.0F) {
            hunger.setSaturationLevel(
                    context.saturationBefore() + gained * EatingBalance.VANILLA_SATURATION_MULTIPLIER
            );
        }
        if ("minecraft".equals(Registries.BLOCK.getId(state.getBlock()).getNamespace())) {
            EatingXpManager.awardCakeSlice(context.player());
        }
    }

    @Unique
    private record CakeUseContext(
            ServerPlayerEntity player,
            int foodBefore,
            float saturationBefore
    ) {
    }
}
