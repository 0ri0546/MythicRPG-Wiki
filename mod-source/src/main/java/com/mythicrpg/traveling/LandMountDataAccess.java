package com.mythicrpg.traveling;

import java.util.Optional;
import java.util.UUID;

public interface LandMountDataAccess {
    Optional<UUID> mythicrpg$getLandMountOwnerUuid();

    String mythicrpg$getLandMountOwnerName();

    void mythicrpg$setLandMountOwner(UUID ownerUuid, String ownerName);

    boolean mythicrpg$isAdoptedLandMount();

    boolean mythicrpg$hasLandMountAnchor();

    int mythicrpg$getLandMountAnchorX();

    int mythicrpg$getLandMountAnchorZ();

    void mythicrpg$setLandMountAnchor(int x, int z);

    double mythicrpg$getTravelDistance();

    void mythicrpg$setTravelDistance(double distance);

    void mythicrpg$addTravelDistance(double distance);

    boolean mythicrpg$wasRiderJumpPressed();

    void mythicrpg$setRiderJumpPressed(boolean pressed);
}
