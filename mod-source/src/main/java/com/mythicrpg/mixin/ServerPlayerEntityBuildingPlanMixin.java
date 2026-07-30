package com.mythicrpg.mixin;

import com.mythicrpg.building.BuildingPlanJobHolder;
import com.mythicrpg.building.BuildingXpDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityBuildingPlanMixin implements BuildingPlanJobHolder, BuildingXpDataHolder {
    @Unique
    private static final String MYTHICRPG_BUILDING_PLAN_JOB_KEY = "MythicRPGBuildingPlanJob";
    @Unique
    private static final String MYTHICRPG_BUILDING_XP_KEY = "MythicRPGBuildingXp";

    @Unique
    private NbtCompound mythicrpg$buildingPlanJob = new NbtCompound();
    @Unique
    private NbtCompound mythicrpg$buildingXp = new NbtCompound();

    @Override
    public NbtCompound mythicrpg$getBuildingPlanJobData() {
        return mythicrpg$buildingPlanJob.copy();
    }

    @Override
    public void mythicrpg$setBuildingPlanJobData(NbtCompound data) {
        mythicrpg$buildingPlanJob = data == null ? new NbtCompound() : data.copy();
    }


    @Override
    public boolean mythicrpg$updateBuildingPlanJobProgress(NbtCompound progress) {
        if (progress == null
                || mythicrpg$buildingPlanJob.isEmpty()
                || !mythicrpg$buildingPlanJob.contains("Placements", NbtElement.LIST_TYPE)
                || !progress.contains("Escrow", NbtElement.LIST_TYPE)) {
            return false;
        }
        mythicrpg$buildingPlanJob.putInt("Cursor", progress.getInt("Cursor"));
        mythicrpg$buildingPlanJob.put(
                "Escrow",
                progress.getList("Escrow", NbtElement.COMPOUND_TYPE).copy()
        );
        return true;
    }

    @Override
    public NbtCompound mythicrpg$getBuildingXpData() {
        return mythicrpg$buildingXp.copy();
    }

    @Override
    public void mythicrpg$setBuildingXpData(NbtCompound data) {
        mythicrpg$buildingXp = data == null ? new NbtCompound() : data.copy();
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void mythicrpg$readBuildingPlanJob(NbtCompound nbt, CallbackInfo ci) {
        mythicrpg$buildingPlanJob = nbt.contains(MYTHICRPG_BUILDING_PLAN_JOB_KEY, NbtElement.COMPOUND_TYPE)
                ? nbt.getCompound(MYTHICRPG_BUILDING_PLAN_JOB_KEY).copy()
                : new NbtCompound();
        mythicrpg$buildingXp = nbt.contains(MYTHICRPG_BUILDING_XP_KEY, NbtElement.COMPOUND_TYPE)
                ? nbt.getCompound(MYTHICRPG_BUILDING_XP_KEY).copy()
                : new NbtCompound();
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void mythicrpg$writeBuildingPlanJob(NbtCompound nbt, CallbackInfo ci) {
        if (mythicrpg$buildingPlanJob.isEmpty()) {
            nbt.remove(MYTHICRPG_BUILDING_PLAN_JOB_KEY);
        } else {
            nbt.put(MYTHICRPG_BUILDING_PLAN_JOB_KEY, mythicrpg$buildingPlanJob.copy());
        }
        if (mythicrpg$buildingXp.isEmpty()) {
            nbt.remove(MYTHICRPG_BUILDING_XP_KEY);
        } else {
            nbt.put(MYTHICRPG_BUILDING_XP_KEY, mythicrpg$buildingXp.copy());
        }
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void mythicrpg$copyBuildingPlanJob(
            ServerPlayerEntity oldPlayer,
            boolean alive,
            CallbackInfo ci
    ) {
        if (oldPlayer instanceof BuildingPlanJobHolder holder) {
            mythicrpg$buildingPlanJob = holder.mythicrpg$getBuildingPlanJobData();
        }
        if (oldPlayer instanceof BuildingXpDataHolder holder) {
            mythicrpg$buildingXp = holder.mythicrpg$getBuildingXpData();
        }
    }
}
