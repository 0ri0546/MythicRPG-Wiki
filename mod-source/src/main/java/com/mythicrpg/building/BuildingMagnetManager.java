package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Small bounded item attraction pass for the Building magnet perk. */
public final class BuildingMagnetManager {
    private static final int INTERVAL_TICKS = 5;
    private static final double RADIUS = 5.0;
    private static final int MAX_ITEMS_PER_PASS = 16;
    private static final double PULL_STRENGTH = 0.18;
    private static int tickCounter;

    private BuildingMagnetManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % INTERVAL_TICKS != 0) {
                return;
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                attractFor(player);
            }
        });
    }

    private static void attractFor(ServerPlayerEntity player) {
        if (!BuildingMagnetState.isEnabled(player.getUuid())
                || !SkillTreeManager.hasBonus(
                player,
                SkillType.BUILDING,
                BonusType.BUILD_DECORATIVE_MAGNET
        )) {
            return;
        }

        Box searchBox = player.getBoundingBox().expand(RADIUS);
        List<ItemEntity> nearby = new ArrayList<>(MAX_ITEMS_PER_PASS);
        player.getWorld().collectEntitiesByType(
                TypeFilter.instanceOf(ItemEntity.class),
                searchBox,
                BuildingMagnetManager::isEligibleItem,
                nearby,
                MAX_ITEMS_PER_PASS
        );

        Vec3d destination = player.getPos().add(0.0, player.getHeight() * 0.45, 0.0);
        for (ItemEntity itemEntity : nearby) {

            Vec3d delta = destination.subtract(itemEntity.getPos());
            double squaredDistance = delta.lengthSquared();
            if (squaredDistance < 0.16 || squaredDistance > RADIUS * RADIUS) {
                continue;
            }

            Vec3d pull = delta.normalize().multiply(PULL_STRENGTH);
            itemEntity.setVelocity(itemEntity.getVelocity().multiply(0.65).add(pull));
            itemEntity.velocityModified = true;
        }
    }

    private static boolean isEligibleItem(ItemEntity entity) {
        if (entity.isRemoved() || entity.cannotPickup()) {
            return false;
        }
        if (!(entity.getStack().getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        return BuildingBlockCatalog.isEligible(blockItem.getBlock());
    }
}
