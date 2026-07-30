package com.mythicrpg.traveling;

import com.mythicrpg.core.ModEntities;
import com.mythicrpg.core.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class TravelerBoatEntity extends BoatEntity {

    public static final float SPEED_MULTIPLIER = 1.5F;

    public TravelerBoatEntity(
            EntityType<? extends TravelerBoatEntity> entityType,
            World world
    ) {
        super(entityType, world);
        setVariant(Type.OAK);
    }

    public TravelerBoatEntity(World world, double x, double y, double z) {
        this(ModEntities.TRAVELER_BOAT, world);
        setPosition(x, y, z);
        setVelocity(0.0D, 0.0D, 0.0D);
        prevX = x;
        prevY = y;
        prevZ = z;
    }

    @Override
    public void tick() {
        super.tick();
        TravelerVehicleParticleEffects.tickBoat(this);
    }

    @Override
    public Item asItem() {
        return ModItems.TRAVELER_BOAT;
    }

    @Override
    public ItemStack getPickBlockStack() {
        return new ItemStack(ModItems.TRAVELER_BOAT);
    }
}
