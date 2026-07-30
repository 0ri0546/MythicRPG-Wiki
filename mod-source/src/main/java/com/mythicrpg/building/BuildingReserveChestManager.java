package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.PlayerCooldownManager;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

/** Server-authoritative placement index, ownership protection and remote stack retrieval. */
public final class BuildingReserveChestManager {
    public static final int MAX_CHESTS_PER_PLAYER = 8;
    private static final int REQUEST_COOLDOWN_TICKS = 5;

    private BuildingReserveChestManager() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                BuildingReserveChestState.get(server).pruneMissingDimensions(server));
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(blockEntity instanceof BuildingReserveChestBlockEntity chest)
                    || !chest.hasOwner()
                    || chest.isOwner(player)
                    || player.isCreative()) {
                return true;
            }

            if (player instanceof ServerPlayerEntity serverPlayer
                    && PlayerCooldownManager.tryUse(serverPlayer, "building_reserve_break_denied", 20)) {
                serverPlayer.sendMessage(
                        Text.translatable("message.mythicrpg.building.reserve.not_owner")
                                .formatted(Formatting.RED),
                        true
                );
            }
            return false;
        });
    }

    public static boolean registerPlacedChest(ServerWorld world, BlockPos pos, UUID owner) {
        return BuildingReserveChestState.get(world.getServer()).add(
                world,
                pos,
                owner,
                MAX_CHESTS_PER_PLAYER,
                true
        );
    }

    public static void ensureIndexed(ServerWorld world, BlockPos pos, UUID owner) {
        BuildingReserveChestState state = BuildingReserveChestState.get(world.getServer());
        if (!state.contains(world, pos)) {
            state.add(world, pos, owner, MAX_CHESTS_PER_PLAYER, false);
        }
    }

    public static void removeChest(ServerWorld world, BlockPos pos) {
        BuildingReserveChestState.get(world.getServer()).remove(world, pos);
    }

    public static void requestHeldBlock(ServerPlayerEntity player) {
        int radius = reserveRadius(player);
        if (radius <= 0) {
            sendError(player, "message.mythicrpg.building.reserve.no_perk");
            return;
        }

        ItemStack template = requestedStack(player);
        if (template.isEmpty()
                || !(template.getItem() instanceof BlockItem blockItem)
                || !BuildingBlockCatalog.isEligible(blockItem.getBlock())) {
            sendError(player, "message.mythicrpg.building.reserve.invalid_item");
            return;
        }

        if (!PlayerCooldownManager.tryUse(player, "building_reserve_request", REQUEST_COOLDOWN_TICKS)) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        BuildingReserveChestState state = BuildingReserveChestState.get(world.getServer());
        List<BuildingReserveChestState.Entry> nearby = state.nearby(
                world,
                player.getBlockPos(),
                radius,
                player.getUuid()
        );

        boolean foundMatching = false;
        for (BuildingReserveChestState.Entry entry : nearby) {
            BlockPos pos = BlockPos.fromLong(entry.packedPos());
            if (!world.isChunkLoaded(pos)) {
                continue;
            }

            if (!(world.getBlockEntity(pos) instanceof BuildingReserveChestBlockEntity chest)
                    || !chest.isOwner(player)) {
                state.remove(world, pos);
                continue;
            }

            if (!chest.containsMatching(template)) {
                continue;
            }

            foundMatching = true;
            int moved = chest.transferOneStackTo(player, template);
            if (moved > 0) {
                player.currentScreenHandler.sendContentUpdates();
                player.sendMessage(
                        Text.translatable(
                                "message.mythicrpg.building.reserve.retrieved",
                                moved,
                                template.getName()
                        ).formatted(Formatting.GREEN),
                        true
                );
                return;
            }
        }

        sendError(
                player,
                foundMatching
                        ? "message.mythicrpg.building.reserve.inventory_full"
                        : "message.mythicrpg.building.reserve.not_found"
        );
    }

    private static int reserveRadius(ServerPlayerEntity player) {
        return (int) Math.round(SkillTreeManager.getBonusValue(
                player,
                SkillType.BUILDING,
                BonusType.BUILD_RESERVE_RANGE
        ));
    }

    private static ItemStack requestedStack(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (isEligible(mainHand)) {
            return mainHand;
        }
        ItemStack offHand = player.getOffHandStack();
        return isEligible(offHand) ? offHand : ItemStack.EMPTY;
    }

    private static boolean isEligible(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem
                && BuildingBlockCatalog.isEligible(blockItem.getBlock());
    }

    private static void sendError(ServerPlayerEntity player, String translationKey) {
        player.sendMessage(Text.translatable(translationKey).formatted(Formatting.RED), true);
    }
}
