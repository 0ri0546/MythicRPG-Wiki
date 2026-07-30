package com.mythicrpg.traveling;

import com.mythicrpg.core.ModEntities;
import com.mythicrpg.core.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class TravelerMinecartEntity extends MinecartEntity {

    public static final double SPEED_MULTIPLIER = 1.5D;

    public TravelerMinecartEntity(
            EntityType<? extends TravelerMinecartEntity> entityType,
            World world
    ) {
        super(entityType, world);
    }

    public TravelerMinecartEntity(World world, double x, double y, double z) {
        this(ModEntities.TRAVELER_MINECART, world);
        setPosition(x, y, z);
        setVelocity(0.0D, 0.0D, 0.0D);
        prevX = x;
        prevY = y;
        prevZ = z;
    }

    @Override
    public void tick() {
        super.tick();
        TravelerVehicleParticleEffects.tickMinecart(this);
    }

    @Override
    protected double getMaxSpeed() {
        return super.getMaxSpeed() * SPEED_MULTIPLIER;
    }

    @Override
    protected Item asItem() {
        return ModItems.TRAVELER_MINECART;
    }

    @Override
    public ItemStack getPickBlockStack() {
        return new ItemStack(ModItems.TRAVELER_MINECART);
    }
}
