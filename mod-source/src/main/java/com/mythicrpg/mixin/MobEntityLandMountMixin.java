package com.mythicrpg.mixin;

import com.mythicrpg.traveling.FlyingMountController;
import com.mythicrpg.traveling.LandMountDataAccess;
import com.mythicrpg.traveling.LandMountManager;
import com.mythicrpg.traveling.LandMountType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(MobEntity.class)
public abstract class MobEntityLandMountMixin implements LandMountDataAccess {
    @Unique
    private static final String OWNER_UUID_NBT = "MythicRpgLandMountOwner";

    @Unique
    private static final String OWNER_NAME_NBT = "MythicRpgLandMountOwnerName";

    @Unique
    private static final String ANCHOR_X_NBT = "MythicRpgLandMountAnchorX";

    @Unique
    private static final String ANCHOR_Z_NBT = "MythicRpgLandMountAnchorZ";

    @Unique
    private static final String TRAVEL_DISTANCE_NBT = "MythicRpgMountTravelDistance";

    @Unique
    private static final TrackedData<Optional<UUID>> MYTHICRPG_LAND_MOUNT_OWNER =
            DataTracker.registerData(MobEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    @Unique
    private static final TrackedData<String> MYTHICRPG_LAND_MOUNT_OWNER_NAME =
            DataTracker.registerData(MobEntity.class, TrackedDataHandlerRegistry.STRING);

    @Unique
    private boolean mythicrpg$riderJumpPressed;

    @Unique
    private boolean mythicrpg$hasLandMountAnchor;

    @Unique
    private int mythicrpg$landMountAnchorX;

    @Unique
    private int mythicrpg$landMountAnchorZ;

    @Unique
    private boolean mythicrpg$hadOwnerPassenger;

    @Unique
    private double mythicrpg$travelDistance;

    @Unique
    private boolean mythicrpg$hasDistanceSample;

    @Unique
    private double mythicrpg$lastDistanceX;

    @Unique
    private double mythicrpg$lastDistanceY;

    @Unique
    private double mythicrpg$lastDistanceZ;

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void mythicrpg$initLandMountData(
            DataTracker.Builder builder,
            CallbackInfo ci
    ) {
        builder.add(MYTHICRPG_LAND_MOUNT_OWNER, Optional.empty());
        builder.add(MYTHICRPG_LAND_MOUNT_OWNER_NAME, "");
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void mythicrpg$writeLandMountData(NbtCompound nbt, CallbackInfo ci) {
        Optional<UUID> ownerUuid = mythicrpg$getLandMountOwnerUuid();

        if (ownerUuid.isEmpty()) {
            return;
        }

        nbt.putUuid(OWNER_UUID_NBT, ownerUuid.get());
        nbt.putString(OWNER_NAME_NBT, mythicrpg$getLandMountOwnerName());

        if (mythicrpg$hasLandMountAnchor()) {
            nbt.putInt(ANCHOR_X_NBT, mythicrpg$getLandMountAnchorX());
            nbt.putInt(ANCHOR_Z_NBT, mythicrpg$getLandMountAnchorZ());
        }

        nbt.putDouble(TRAVEL_DISTANCE_NBT, mythicrpg$getTravelDistance());
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void mythicrpg$readLandMountData(NbtCompound nbt, CallbackInfo ci) {
        if (!nbt.containsUuid(OWNER_UUID_NBT)) {
            return;
        }

        MobEntity self = (MobEntity) (Object) this;

        if (LandMountType.fromEntity(self).isEmpty()) {
            return;
        }

        String ownerName = nbt.getString(OWNER_NAME_NBT);

        if (ownerName.isBlank()) {
            ownerName = "Unknown";
        }

        mythicrpg$setLandMountOwner(nbt.getUuid(OWNER_UUID_NBT), ownerName);

        if (nbt.contains(ANCHOR_X_NBT) && nbt.contains(ANCHOR_Z_NBT)) {
            mythicrpg$setLandMountAnchor(
                    nbt.getInt(ANCHOR_X_NBT),
                    nbt.getInt(ANCHOR_Z_NBT)
            );
        } else {
            mythicrpg$setLandMountAnchor(self.getBlockX(), self.getBlockZ());
        }

        mythicrpg$setTravelDistance(nbt.getDouble(TRAVEL_DISTANCE_NBT));
        LandMountManager.refreshPresentation(self);
    }

    /**
     * Checks held-item interactions before vanilla animal breeding logic can
     * consume the click. Empty-hand mounting stays in interactMob so special
     * entities such as Panda can prepare their pose first.
     */
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$interactWithAdoptedMountItem(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        MobEntity self = (MobEntity) (Object) this;

        if (!mythicrpg$isAdoptedLandMount()) {
            return;
        }

        ItemStack heldStack = player.getStackInHand(hand);
        if (heldStack.isEmpty()) {
            return;
        }

        ActionResult result = LandMountManager.handleAdoptedInteraction(
                self,
                player,
                heldStack
        );

        if (result != ActionResult.PASS) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$interactWithAdoptedMountEmptyHand(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        MobEntity self = (MobEntity) (Object) this;

        if (!mythicrpg$isAdoptedLandMount()) {
            return;
        }

        ItemStack heldStack = player.getStackInHand(hand);
        if (!heldStack.isEmpty()) {
            return;
        }

        ActionResult result = LandMountManager.handleAdoptedInteraction(
                self,
                player,
                heldStack
        );

        if (result != ActionResult.PASS) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$getLandMountController(
            CallbackInfoReturnable<LivingEntity> cir
    ) {
        MobEntity self = (MobEntity) (Object) this;

        if (!mythicrpg$isAdoptedLandMount()) {
            return;
        }

        if (self.getFirstPassenger() instanceof PlayerEntity player
                && LandMountManager.isOwner(self, player)) {
            cir.setReturnValue(player);
        }
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedMountTargeting(
            @Nullable LivingEntity target,
            CallbackInfo ci
    ) {
        if (target != null && mythicrpg$isAdoptedLandMount()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void mythicrpg$tickAdoptedLandMount(CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;

        if (!mythicrpg$isAdoptedLandMount()) {
            mythicrpg$riderJumpPressed = false;
            mythicrpg$hadOwnerPassenger = false;
            return;
        }

        if (self.getTarget() != null) {
            self.setTarget(null);
        }
        if (self.isAttacking()) {
            self.setAttacking(false);
        }

        if (self instanceof AnimalEntity animal && animal.isInLove()) {
            animal.resetLoveTicks();
        }

        if (self.age % 5 == 0) {
            LandMountManager.maintainAdoptedMountState(self);
        }

        boolean ownerPassenger = false;

        if (self.getFirstPassenger() instanceof PlayerEntity passenger) {
            if (!LandMountManager.isOwner(self, passenger)) {
                passenger.stopRiding();
                mythicrpg$riderJumpPressed = false;
            } else {
                ownerPassenger = true;
                self.clearPositionTarget();
                self.getNavigation().stop();

                if (LandMountManager.isFlyingMount(self)) {
                    boolean ascendPressed = ((LivingEntityJumpingAccessor) passenger)
                            .mythicrpg$isJumping();
                    FlyingMountController.tickFlyingMount(
                            self,
                            passenger,
                            ascendPressed
                    );
                }
            }
        } else {
            mythicrpg$riderJumpPressed = false;
        }

        if (ownerPassenger) {
            if (!self.getWorld().isClient) {
                if (!mythicrpg$hadOwnerPassenger) {
                    LandMountManager.playMountedSound(self);
                }
                mythicrpg$recordTravelDistance(self);
            }
            mythicrpg$hadOwnerPassenger = true;
        } else {
            mythicrpg$hasDistanceSample = false;

            if (mythicrpg$hadOwnerPassenger && !self.getWorld().isClient) {
                LandMountManager.playDismountedSound(self);
            }
            if (LandMountManager.isFlyingMount(self)) {
                self.setNoGravity(false);
            }

            if (mythicrpg$hadOwnerPassenger || !mythicrpg$hasLandMountAnchor()) {
                mythicrpg$setLandMountAnchor(self.getBlockX(), self.getBlockZ());
            }

            mythicrpg$hadOwnerPassenger = false;

            if (!self.getWorld().isClient) {
                LandMountManager.keepNearAnchor(self);
            }
        }

        if (!self.getWorld().isClient && self.age % 100 == 0) {
            LandMountManager.refreshOwnerNameIfOnline(self);
        }
    }

    @Override
    public Optional<UUID> mythicrpg$getLandMountOwnerUuid() {
        MobEntity self = (MobEntity) (Object) this;
        return self.getDataTracker().get(MYTHICRPG_LAND_MOUNT_OWNER);
    }

    @Override
    public String mythicrpg$getLandMountOwnerName() {
        MobEntity self = (MobEntity) (Object) this;
        return self.getDataTracker().get(MYTHICRPG_LAND_MOUNT_OWNER_NAME);
    }

    @Override
    public void mythicrpg$setLandMountOwner(UUID ownerUuid, String ownerName) {
        MobEntity self = (MobEntity) (Object) this;
        self.getDataTracker().set(
                MYTHICRPG_LAND_MOUNT_OWNER,
                Optional.ofNullable(ownerUuid)
        );
        self.getDataTracker().set(
                MYTHICRPG_LAND_MOUNT_OWNER_NAME,
                ownerName == null ? "" : ownerName
        );
    }

    @Override
    public boolean mythicrpg$isAdoptedLandMount() {
        MobEntity self = (MobEntity) (Object) this;
        return mythicrpg$getLandMountOwnerUuid().isPresent()
                && LandMountType.fromEntity(self).isPresent();
    }

    @Override
    public boolean mythicrpg$hasLandMountAnchor() {
        return mythicrpg$hasLandMountAnchor;
    }

    @Override
    public int mythicrpg$getLandMountAnchorX() {
        return mythicrpg$landMountAnchorX;
    }

    @Override
    public int mythicrpg$getLandMountAnchorZ() {
        return mythicrpg$landMountAnchorZ;
    }

    @Override
    public void mythicrpg$setLandMountAnchor(int x, int z) {
        mythicrpg$hasLandMountAnchor = true;
        mythicrpg$landMountAnchorX = x;
        mythicrpg$landMountAnchorZ = z;
    }

    @Override
    public double mythicrpg$getTravelDistance() {
        return mythicrpg$travelDistance;
    }

    @Override
    public void mythicrpg$setTravelDistance(double distance) {
        mythicrpg$travelDistance = Math.max(0.0D, distance);
    }

    @Override
    public void mythicrpg$addTravelDistance(double distance) {
        if (distance > 0.0D) {
            mythicrpg$travelDistance += distance;
        }
    }

    @Unique
    private void mythicrpg$recordTravelDistance(MobEntity self) {
        double x = self.getX();
        double y = self.getY();
        double z = self.getZ();

        if (mythicrpg$hasDistanceSample) {
            double deltaX = x - mythicrpg$lastDistanceX;
            double deltaY = y - mythicrpg$lastDistanceY;
            double deltaZ = z - mythicrpg$lastDistanceZ;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

            // Ignore tiny animation jitter and abnormal teleport-sized jumps.
            if (distance >= 0.01D && distance <= 8.0D) {
                mythicrpg$addTravelDistance(distance);
            }
        }

        mythicrpg$lastDistanceX = x;
        mythicrpg$lastDistanceY = y;
        mythicrpg$lastDistanceZ = z;
        mythicrpg$hasDistanceSample = true;
    }

    @Override
    public boolean mythicrpg$wasRiderJumpPressed() {
        return mythicrpg$riderJumpPressed;
    }

    @Override
    public void mythicrpg$setRiderJumpPressed(boolean pressed) {
        mythicrpg$riderJumpPressed = pressed;
    }
}
