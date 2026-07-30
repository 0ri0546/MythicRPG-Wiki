package com.mythicrpg.mixin;

import com.mythicrpg.eating.EatingBalance;
import com.mythicrpg.eating.EatingXpManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(ItemStack.class)
public abstract class ItemStackEatingMixin {
    @Unique
    private static final ThreadLocal<Deque<FoodUseContext>> MYTHICRPG$FOOD_CONTEXT = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void mythicrpg$beforeFinishUsing(
            World world,
            LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        Deque<FoodUseContext> contexts = MYTHICRPG$FOOD_CONTEXT.get();
        contexts.push(new FoodUseContext(null, ItemStack.EMPTY, false, 0.0F));
        if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;
        boolean vanilla = "minecraft".equals(
                Registries.ITEM.getId(self.getItem()).getNamespace()
        );
        boolean scaleVanillaFood = vanilla && self.contains(DataComponentTypes.FOOD);
        boolean vanillaSoup = vanilla && EatingXpManager.isVanillaSoup(self);
        if (!scaleVanillaFood && !vanillaSoup) {
            return;
        }

        float saturationBefore = scaleVanillaFood
                ? player.getHungerManager().getSaturationLevel()
                : 0.0F;
        contexts.pop();
        contexts.push(new FoodUseContext(
                player,
                self.copyWithCount(1),
                scaleVanillaFood,
                saturationBefore
        ));
    }

    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void mythicrpg$afterFinishUsing(
            World world,
            LivingEntity user,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        Deque<FoodUseContext> contexts = MYTHICRPG$FOOD_CONTEXT.get();
        FoodUseContext context = contexts.isEmpty() ? null : contexts.pop();
        if (contexts.isEmpty()) {
            MYTHICRPG$FOOD_CONTEXT.remove();
        }
        if (context == null || context.player() != user) {
            return;
        }

        if (!context.consumedStack().isEmpty()) {
            EatingXpManager.awardVanillaFood(
                    context.player(),
                    context.consumedStack()
            );
        }

        if (context.scaleVanillaFood()) {
            var hunger = context.player().getHungerManager();
            float after = hunger.getSaturationLevel();
            float gained = Math.max(0.0F, after - context.saturationBefore());
            if (gained > 0.0F) {
                hunger.setSaturationLevel(
                        context.saturationBefore()
                                + gained * EatingBalance.VANILLA_SATURATION_MULTIPLIER
                );
            }
        }
    }

    @Unique
    private record FoodUseContext(
            ServerPlayerEntity player,
            ItemStack consumedStack,
            boolean scaleVanillaFood,
            float saturationBefore
    ) {
    }
}
