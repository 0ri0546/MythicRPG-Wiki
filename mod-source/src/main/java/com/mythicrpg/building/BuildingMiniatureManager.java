package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative lifecycle for ongoing projects and static miniature entities. */
public final class BuildingMiniatureManager {
    private BuildingMiniatureManager() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                BuildingMiniatureState.get(server).pruneMissingDimensions(server));
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                interact(player, world, hand, entity));
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof BuildingMiniatureEntity miniature)) return;
            Optional<UUID> owner = miniature.owner();
            if (owner.isEmpty() || BuildingMiniatureData.readProject(miniature.miniatureStack()).isEmpty()) {
                miniature.discard();
                return;
            }
            if (!BuildingMiniatureState.get(world.getServer()).add(
                    miniature.getUuid(), world, miniature.getBlockPos(), owner.get())) {
                miniature.discard();
            }
        });
    }

    public static boolean canUse(ServerPlayerEntity player) {
        return player != null && SkillTreeManager.hasBonus(
                player,
                SkillType.BUILDING,
                BonusType.BUILD_MINIATURE
        );
    }

    public static boolean finish(ServerPlayerEntity player, ItemStack stack) {
        Optional<BuildingMiniatureData.Selection> optional = BuildingMiniatureData.readSelection(stack);
        if (optional.isEmpty() || !optional.get().complete()) {
            error(player, "message.mythicrpg.building_miniature.selection_incomplete");
            return false;
        }
        BuildingMiniatureData.Selection selection = optional.get();
        ServerWorld world = player.getServerWorld();
        String dimension = world.getRegistryKey().getValue().toString();
        if (!dimension.equals(selection.dimensionId())) {
            error(player, "message.mythicrpg.building_miniature.wrong_dimension");
            return false;
        }

        BlockPos min = selection.min();
        BlockPos max = selection.max();
        int sizeX = max.getX() - min.getX() + 1;
        int sizeY = max.getY() - min.getY() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;
        if (sizeX < 1 || sizeY < 1 || sizeZ < 1 || sizeX > 5 || sizeY > 5 || sizeZ > 5) {
            player.sendMessage(Text.translatable(
                    "message.mythicrpg.building_miniature.too_large", sizeX, sizeY, sizeZ
            ).formatted(Formatting.RED), true);
            BuildingSoundFeedback.error(player);
            return false;
        }
        List<BuildingMiniatureData.Entry> entries = new ArrayList<>(sizeX * sizeY * sizeZ);

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!world.isInBuildLimit(pos) || !world.getWorldBorder().contains(pos)) {
                        error(player, "message.mythicrpg.building_miniature.outside_world");
                        return false;
                    }
                    if (!world.isChunkLoaded(pos)) {
                        error(player, "message.mythicrpg.building_miniature.chunk_unloaded");
                        return false;
                    }
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) continue;
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (blockEntity != null || !BuildingMiniatureData.isSupportedState(state)) {
                        player.sendMessage(Text.translatable(
                                "message.mythicrpg.building_miniature.invalid_block",
                                state.getBlock().getName(), pos.toShortString()
                        ).formatted(Formatting.RED), true);
                        BuildingSoundFeedback.error(player);
                        return false;
                    }
                    entries.add(new BuildingMiniatureData.Entry(
                            x - min.getX(),
                            y - min.getY(),
                            z - min.getZ(),
                            state
                    ));
                }
            }
        }

        if (entries.isEmpty()) {
            error(player, "message.mythicrpg.building_miniature.empty");
            return false;
        }

        BuildingMiniatureData.writeProject(stack, new BuildingMiniatureData.Project(
                UUID.randomUUID(),
                player.getUuid(),
                player.getGameProfile().getName(),
                sizeX,
                sizeY,
                sizeZ,
                List.copyOf(entries),
                selection.rotation()
        ));
        BuildingSelectionBoxManager.clear(player);
        player.sendMessage(Text.translatable(
                "message.mythicrpg.building_miniature.finished",
                entries.size()
        ).formatted(Formatting.GREEN), true);
        return true;
    }

    public static boolean place(ServerPlayerEntity player, ItemUsageContext context,
                                BuildingMiniatureData.Project project) {
        if (context.getSide() != net.minecraft.util.math.Direction.UP) {
            error(player, "message.mythicrpg.building_miniature.ground_only");
            return false;
        }
        ServerWorld world = player.getServerWorld();
        BlockPos position = context.getBlockPos().up();
        if (!world.isInBuildLimit(position)
                || !world.getWorldBorder().contains(position)
                || !world.isAir(position)
                || !world.canPlayerModifyAt(player, position)
                || !world.getEntitiesByClass(
                        BuildingMiniatureEntity.class,
                        new Box(position),
                        Entity::isAlive
                ).isEmpty()) {
            error(player, "message.mythicrpg.building_miniature.blocked");
            return false;
        }
        BuildingMiniatureState quota = BuildingMiniatureState.get(world.getServer());
        if (!quota.canPlace(world, position, player.getUuid())) {
            error(player, "message.mythicrpg.building_miniature.limit");
            return false;
        }

        BuildingMiniatureEntity entity = new BuildingMiniatureEntity(world);
        entity.setPosition(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
        entity.setYaw(player.getYaw() + 180.0F);
        ItemStack stored = context.getStack().copyWithCount(1);
        entity.configure(player.getUuid(), stored);
        if (!world.spawnEntity(entity)) {
            error(player, "message.mythicrpg.building_miniature.spawn_failed");
            return false;
        }
        if (!quota.add(entity.getUuid(), world, position, player.getUuid())) {
            entity.discard();
            error(player, "message.mythicrpg.building_miniature.limit");
            return false;
        }
        if (!player.isCreative()) context.getStack().decrement(1);
        BuildingSoundFeedback.miniaturePlaced(player, position);
        player.sendMessage(Text.translatable("message.mythicrpg.building_miniature.placed")
                .formatted(Formatting.GREEN), true);
        return true;
    }

    private static ActionResult interact(
            PlayerEntity player,
            net.minecraft.world.World world,
            Hand hand,
            Entity target
    ) {
        if (!(target instanceof BuildingMiniatureEntity miniature)) return ActionResult.PASS;
        if (player.isSpectator()) return ActionResult.FAIL;

        ItemStack held = player.getStackInHand(hand);
        if (held.getItem() instanceof BuilderWandItem) {
            return rotateWithWand(player, world, hand, miniature);
        }

        // Require both hands to be empty so the off-hand callback cannot retrieve accidentally.
        if (player.getMainHandStack().isEmpty() && player.getOffHandStack().isEmpty()) {
            return retrieve(player, world, miniature);
        }
        return ActionResult.PASS;
    }

    private static ActionResult rotateWithWand(
            PlayerEntity player,
            net.minecraft.world.World world,
            Hand hand,
            BuildingMiniatureEntity miniature
    ) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.FAIL;
        if (!miniature.isOwner(player.getUuid()) && !player.isCreative()) {
            error(serverPlayer, "message.mythicrpg.building_miniature.not_owner");
            return ActionResult.FAIL;
        }
        if (!SkillTreeManager.hasBonus(serverPlayer, SkillType.BUILDING, BonusType.BUILD_WAND)) {
            error(serverPlayer, "message.mythicrpg.builder_wand.locked");
            return ActionResult.FAIL;
        }

        float angle = miniature.rotateRollZ(10.0F);
        if (!player.isCreative()) {
            player.getStackInHand(hand).damage(1, player, LivingEntity.getSlotForHand(hand));
        }
        BuildingSoundFeedback.miniatureRotated(serverPlayer, miniature.getBlockPos());
        serverPlayer.sendMessage(Text.translatable(
                "message.mythicrpg.building_miniature.rotated_z",
                Math.round(angle)
        ).formatted(Formatting.AQUA), true);
        return ActionResult.SUCCESS;
    }

    private static ActionResult retrieve(PlayerEntity player, net.minecraft.world.World world, Entity target) {
        if (!(target instanceof BuildingMiniatureEntity miniature)) return ActionResult.PASS;
        if (player.isSpectator()) return ActionResult.FAIL;
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.FAIL;
        if (!miniature.isOwner(player.getUuid()) && !player.isCreative()) {
            error(serverPlayer, "message.mythicrpg.building_miniature.not_owner");
            return ActionResult.FAIL;
        }
        if (!player.isCreative()) {
            ItemStack refund = miniature.recoverableStack();
            if (!refund.isOf(ModItems.BUILDING_MINIATURE_PROJECT)) return ActionResult.FAIL;
            player.getInventory().insertStack(refund);
            if (!refund.isEmpty()) player.dropItem(refund, false);
        }
        BuildingMiniatureState.get(serverPlayer.getServerWorld().getServer()).remove(miniature.getUuid());
        BlockPos soundPos = miniature.getBlockPos();
        miniature.discard();
        BuildingSoundFeedback.miniatureRetrieved(serverPlayer, soundPos);
        serverPlayer.sendMessage(Text.translatable("message.mythicrpg.building_miniature.retrieved")
                .formatted(Formatting.YELLOW), true);
        return ActionResult.SUCCESS;
    }

    private static void error(ServerPlayerEntity player, String key) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.RED), true);
        BuildingSoundFeedback.error(player);
    }
}
