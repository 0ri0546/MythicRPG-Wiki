package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Event-driven comfort perks for Building phase 1. */
public final class BuildingComfortManager {
    private static final Map<UUID, RestockRequest> RESTOCK_REQUESTS = new HashMap<>();

    private BuildingComfortManager() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)
                    || !(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            ItemStack heldStack = player.getStackInHand(hand);
            if (!(heldStack.getItem() instanceof BlockItem blockItem)) {
                return ActionResult.PASS;
            }

            armRestockIfUnlocked(serverPlayer, hand, heldStack, serverWorld.getTime());

            if (!BuildingBlockCatalog.isEligible(blockItem.getBlock())
                    || player.isSneaking()
                    || !SkillTreeManager.hasBonus(
                    serverPlayer,
                    SkillType.BUILDING,
                    BonusType.BUILD_QUICK_REPLACE
            )) {
                return ActionResult.PASS;
            }

            return tryQuickReplace(serverPlayer, serverWorld, hand, hitResult.getBlockPos(), hitResult);
        });

        ServerTickEvents.END_SERVER_TICK.register(BuildingComfortManager::processRestocks);
    }

    public static void clearPlayer(UUID playerId) {
        RESTOCK_REQUESTS.remove(playerId);
    }

    private static ActionResult tryQuickReplace(
            ServerPlayerEntity player,
            ServerWorld world,
            Hand hand,
            BlockPos targetPos,
            net.minecraft.util.hit.BlockHitResult hitResult
    ) {
        if (!world.isInBuildLimit(targetPos)
                || !world.canPlayerModifyAt(player, targetPos)
                || world.getBlockEntity(targetPos) != null) {
            return ActionResult.PASS;
        }

        ItemStack heldStack = player.getStackInHand(hand);
        if (!(heldStack.getItem() instanceof BlockItem blockItem)) {
            return ActionResult.PASS;
        }

        BlockState oldState = world.getBlockState(targetPos);
        Block oldBlock = oldState.getBlock();
        Block newBlock = blockItem.getBlock();

        if (!BuildingBlockCatalog.isEligible(oldBlock)
                || !BuildingBlockCatalog.isEligible(newBlock)
                || oldState.isOf(newBlock)) {
            return ActionResult.PASS;
        }

        world.setBlockState(
                targetPos,
                Blocks.AIR.getDefaultState(),
                Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS
        );

        ItemPlacementContext placementContext = new ItemPlacementContext(
                player,
                hand,
                heldStack,
                hitResult
        );
        if (!placementContext.getBlockPos().equals(targetPos)) {
            world.setBlockState(targetPos, oldState, Block.NOTIFY_ALL);
            return ActionResult.PASS;
        }

        ActionResult result = blockItem.place(placementContext);

        if (!result.isAccepted() || !world.getBlockState(targetPos).isOf(newBlock)) {
            world.setBlockState(targetPos, oldState, Block.NOTIFY_ALL);
            return result == ActionResult.FAIL ? ActionResult.FAIL : ActionResult.PASS;
        }

        if (!player.isCreative()) {
            Item oldItem = oldBlock.asItem();
            if (oldItem != net.minecraft.item.Items.AIR) {
                ItemStack recovered = new ItemStack(oldItem);
                if (!player.getInventory().insertStack(recovered)) {
                    player.dropItem(recovered, false);
                }
            }
        }

        BuildingXpManager.recordDirectPlacement(player, newBlock, targetPos);
        return ActionResult.SUCCESS;
    }

    private static void armRestockIfUnlocked(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack prototype,
            long now
    ) {
        if (!SkillTreeManager.hasBonus(
                player,
                SkillType.BUILDING,
                BonusType.BUILD_AUTO_RESTOCK
        )) {
            return;
        }

        RESTOCK_REQUESTS.put(
                player.getUuid(),
                new RestockRequest(hand, prototype.copyWithCount(1), now + 2L)
        );
    }

    private static void processRestocks(MinecraftServer server) {
        Iterator<Map.Entry<UUID, RestockRequest>> iterator = RESTOCK_REQUESTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RestockRequest> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            RestockRequest request = entry.getValue();

            if (player == null) {
                iterator.remove();
                continue;
            }

            long now = player.getWorld().getTime();
            ItemStack handStack = player.getStackInHand(request.hand());
            if (!handStack.isEmpty()) {
                iterator.remove();
                continue;
            }

            if (tryMoveMatchingStackToHand(player, request.hand(), request.prototype())) {
                iterator.remove();
                continue;
            }

            if (now >= request.expiresAt()) {
                iterator.remove();
            }
        }
    }

    private static boolean tryMoveMatchingStackToHand(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack prototype
    ) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack candidate = player.getInventory().getStack(slot);
            if (candidate.isEmpty()
                    || !ItemStack.areItemsEqual(candidate, prototype)
                    || !candidate.getComponentChanges().equals(prototype.getComponentChanges())) {
                continue;
            }

            player.setStackInHand(hand, candidate);
            player.getInventory().setStack(slot, ItemStack.EMPTY);
            player.getInventory().markDirty();
            return true;
        }
        return false;
    }

    private record RestockRequest(Hand hand, ItemStack prototype, long expiresAt) {
    }
}
