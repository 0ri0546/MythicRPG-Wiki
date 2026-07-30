package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/** Server authority for the Architect's Compass configuration screen. */
public final class ArchitectCompassUiManager {
    private ArchitectCompassUiManager() {
    }

    /**
     * Intercepts block interaction before interactive vanilla blocks consume it.
     * This keeps "right-click a block = choose the center" true without requiring sneak.
     */
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(ModItems.ARCHITECT_COMPASS)) {
                return ActionResult.PASS;
            }
            if (world.isClient) {
                return ActionResult.SUCCESS;
            }
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.FAIL;
            }
            return setCenter(
                    serverPlayer,
                    stack,
                    world.getRegistryKey().getValue().toString(),
                    hitResult.getBlockPos()
            );
        });
    }

    public static ActionResult useOnBlock(net.minecraft.item.ItemUsageContext context) {
        if (context.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }
        return setCenter(
                player,
                context.getStack(),
                context.getWorld().getRegistryKey().getValue().toString(),
                context.getBlockPos()
        );
    }

    public static void open(ServerPlayerEntity player, Hand hand) {
        if (!canUse(player)) {
            locked(player);
            return;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(ModItems.ARCHITECT_COMPASS)) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        ArchitectCompassData.State state = ArchitectCompassData.read(stack);
        String currentDimension = dimensionId(world);
        boolean hasUsableCenter = state.hasCenter() && currentDimension.equals(state.dimensionId());
        BlockPos center = hasUsableCenter ? state.center() : player.getBlockPos();
        BuildingUiSessionManager.open(
                player,
                BuildingUiSessionManager.Tool.ARCHITECT_COMPASS,
                hand,
                stack,
                Long.MIN_VALUE
        );

        sendState(
                player,
                hand,
                true,
                hasUsableCenter,
                currentDimension,
                center,
                state.radius(),
                state.plane().axisId(),
                "",
                false
        );
    }

    public static void handle(ServerPlayerEntity player, ArchitectCompassUiActionPayload payload) {
        Hand hand = payload.handId() == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        if (!canUse(player)) {
            locked(player);
            sendState(
                    player,
                    hand,
                    false,
                    false,
                    dimensionId(player.getServerWorld()),
                    player.getBlockPos(),
                    ArchitectCompassData.DEFAULT_RADIUS,
                    ArchitectCompassData.Plane.HORIZONTAL.axisId(),
                    "message.mythicrpg.architect_compass.locked",
                    false
            );
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(ModItems.ARCHITECT_COMPASS)) {
            return;
        }
        if (!BuildingUiSessionManager.allow(
                player,
                BuildingUiSessionManager.Tool.ARCHITECT_COMPASS,
                hand,
                stack,
                BuildingUiSessionManager.ActionCost.MUTATION,
                payload.hashCode(),
                Long.MIN_VALUE
        )) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos center = payload.center();
        if (!world.isInBuildLimit(center) || !world.getWorldBorder().contains(center)) {
            BuildingSoundFeedback.error(player);
            sendState(
                    player,
                    hand,
                    true,
                    true,
                    dimensionId(world),
                    center,
                    payload.radius(),
                    payload.axisId(),
                    "screen.mythicrpg.architect_compass_ui.invalid_center",
                    true
            );
            return;
        }

        ArchitectCompassData.Plane plane = ArchitectCompassData.Plane.fromAxisId(payload.axisId());
        ArchitectCompassData.setConfiguration(
                stack,
                dimensionId(world),
                center,
                payload.radius(),
                plane
        );
        player.getInventory().markDirty();
        BuildingSoundFeedback.compassApplied(player);
        player.sendMessage(
                Text.translatable("message.mythicrpg.architect_compass.saved")
                        .formatted(Formatting.AQUA),
                true
        );
        sendState(
                player,
                hand,
                false,
                true,
                dimensionId(world),
                center,
                payload.radius(),
                payload.axisId(),
                "screen.mythicrpg.architect_compass_ui.saved",
                false
        );
        BuildingUiSessionManager.close(player);
    }


    private static ActionResult setCenter(
            ServerPlayerEntity player,
            ItemStack stack,
            String dimensionId,
            BlockPos center
    ) {
        if (!canUse(player)) {
            locked(player);
            return ActionResult.FAIL;
        }
        if (!stack.isOf(ModItems.ARCHITECT_COMPASS)) {
            return ActionResult.PASS;
        }

        ArchitectCompassData.setCenter(stack, dimensionId, center.toImmutable());
        player.getInventory().markDirty();
        ArchitectCompassData.State state = ArchitectCompassData.read(stack);
        BuildingSoundFeedback.compassCenter(player, center);
        player.sendMessage(
                Text.translatable(
                        "message.mythicrpg.architect_compass.center_set",
                        state.radius(),
                        Text.translatable(state.plane().translationKey())
                ).formatted(Formatting.AQUA),
                true
        );
        return ActionResult.SUCCESS;
    }

    private static void sendState(
            ServerPlayerEntity player,
            Hand hand,
            boolean openScreen,
            boolean hasCenter,
            String dimensionId,
            BlockPos center,
            int radius,
            int axisId,
            String messageKey,
            boolean error
    ) {
        ServerPlayNetworking.send(player, new ArchitectCompassUiStatePayload(
                hand == Hand.MAIN_HAND ? 0 : 1,
                openScreen,
                hasCenter,
                dimensionId,
                center.asLong(),
                radius,
                axisId,
                messageKey,
                error
        ));
    }

    private static boolean canUse(ServerPlayerEntity player) {
        return SkillTreeManager.hasBonus(
                player,
                SkillType.BUILDING,
                BonusType.BUILD_ARCHITECT_COMPASS
        );
    }

    private static void locked(ServerPlayerEntity player) {
        BuildingSoundFeedback.error(player);
        player.sendMessage(
                Text.translatable("message.mythicrpg.architect_compass.locked")
                        .formatted(Formatting.RED),
                true
        );
    }

    private static String dimensionId(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }
}
