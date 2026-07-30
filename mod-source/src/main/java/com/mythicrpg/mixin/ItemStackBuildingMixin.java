package com.mythicrpg.mixin;

import com.mythicrpg.building.BuildingBlockCatalog;
import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Keeps the normal post-mine callbacks and statistics while suppressing only
 * the durability damage granted by Building perk 7 on eligible blocks.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackBuildingMixin {
    @Unique
    private static final ThreadLocal<ProtectedMining> MYTHICRPG_BUILDING_MINING = new ThreadLocal<>();

    @Inject(
            method = "postMine(Lnet/minecraft/world/World;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At("HEAD")
    )
    private void mythicrpg$beginBuildingMining(
            World world,
            BlockState state,
            BlockPos pos,
            PlayerEntity miner,
            CallbackInfo ci
    ) {
        MYTHICRPG_BUILDING_MINING.remove();

        if (world.isClient
                || !(miner instanceof ServerPlayerEntity player)
                || !BuildingBlockCatalog.isEligible(state.getBlock())
                || !SkillTreeManager.hasBonus(
                player,
                SkillType.BUILDING,
                BonusType.BUILD_NO_TOOL_DURABILITY
        )) {
            return;
        }

        MYTHICRPG_BUILDING_MINING.set(new ProtectedMining(
                (ItemStack) (Object) this,
                player.getUuid(),
                world,
                world.getTime(),
                System.nanoTime() + 100_000_000L
        ));
    }

    @Inject(
            method = "postMine(Lnet/minecraft/world/World;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At("RETURN")
    )
    private void mythicrpg$endBuildingMining(
            World world,
            BlockState state,
            BlockPos pos,
            PlayerEntity miner,
            CallbackInfo ci
    ) {
        ProtectedMining protectedMining = MYTHICRPG_BUILDING_MINING.get();
        if (protectedMining != null && protectedMining.stack() == (Object) this) {
            MYTHICRPG_BUILDING_MINING.remove();
        }
    }

    @Inject(
            method = "damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mythicrpg$skipLivingDurabilityDamage(
            int amount,
            LivingEntity entity,
            EquipmentSlot slot,
            CallbackInfo ci
    ) {
        if (mythicrpg$consumeProtectedMining(entity.getWorld(), entity.getUuid())) {
            ci.cancel();
        }
    }

    @Inject(
            method = "damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mythicrpg$skipServerDurabilityDamage(
            int amount,
            ServerWorld world,
            ServerPlayerEntity player,
            Consumer<Item> breakCallback,
            CallbackInfo ci
    ) {
        if (player != null && mythicrpg$consumeProtectedMining(world, player.getUuid())) {
            ci.cancel();
        }
    }

    @Unique
    private boolean mythicrpg$consumeProtectedMining(World world, UUID playerId) {
        ProtectedMining protectedMining = MYTHICRPG_BUILDING_MINING.get();
        if (protectedMining == null) {
            return false;
        }
        boolean matches = protectedMining.stack() == (Object) this
                && protectedMining.playerId().equals(playerId)
                && protectedMining.world() == world
                && protectedMining.worldTick() == world.getTime()
                && System.nanoTime() <= protectedMining.deadlineNanos();
        // Durability is applied at most once by ItemStack.postMine. Consuming the
        // marker here also prevents a stale context surviving an exception path.
        MYTHICRPG_BUILDING_MINING.remove();
        return matches;
    }

    private record ProtectedMining(
            ItemStack stack,
            UUID playerId,
            World world,
            long worldTick,
            long deadlineNanos
    ) {
    }
}
