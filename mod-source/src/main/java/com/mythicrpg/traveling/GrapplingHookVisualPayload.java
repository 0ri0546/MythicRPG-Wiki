package com.mythicrpg.traveling;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/** Synchronizes the client-side rope renderer for connected players. */
public record GrapplingHookVisualPayload(
        int playerEntityId,
        int action,
        long startedAtWorldTick,
        double launchX,
        double launchY,
        double launchZ,
        double anchorX,
        double anchorY,
        double anchorZ,
        double destinationX,
        double destinationY,
        double destinationZ
) implements CustomPayload {
    public static final int START_EXTENDING = 0;
    public static final int START_PULLING = 1;
    public static final int STOP = 2;

    public static final Id<GrapplingHookVisualPayload> ID =
            new Id<>(Identifier.of("mythicrpg", "grappling_hook_visual"));

    public static final PacketCodec<RegistryByteBuf, GrapplingHookVisualPayload> CODEC =
            PacketCodec.ofStatic(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.playerEntityId());
                        buffer.writeVarInt(payload.action());
                        buffer.writeLong(payload.startedAtWorldTick());
                        buffer.writeDouble(payload.launchX());
                        buffer.writeDouble(payload.launchY());
                        buffer.writeDouble(payload.launchZ());
                        buffer.writeDouble(payload.anchorX());
                        buffer.writeDouble(payload.anchorY());
                        buffer.writeDouble(payload.anchorZ());
                        buffer.writeDouble(payload.destinationX());
                        buffer.writeDouble(payload.destinationY());
                        buffer.writeDouble(payload.destinationZ());
                    },
                    buffer -> new GrapplingHookVisualPayload(
                            buffer.readVarInt(),
                            buffer.readVarInt(),
                            buffer.readLong(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble(),
                            buffer.readDouble()
                    )
            );

    public static GrapplingHookVisualPayload extending(
            int playerEntityId,
            long startedAtWorldTick,
            Vec3d launch,
            Vec3d anchor,
            Vec3d destination
    ) {
        return create(playerEntityId, START_EXTENDING, startedAtWorldTick, launch, anchor, destination);
    }

    public static GrapplingHookVisualPayload pulling(
            int playerEntityId,
            long startedAtWorldTick,
            Vec3d launch,
            Vec3d anchor,
            Vec3d destination
    ) {
        return create(playerEntityId, START_PULLING, startedAtWorldTick, launch, anchor, destination);
    }

    public static GrapplingHookVisualPayload stop(int playerEntityId) {
        return create(playerEntityId, STOP, 0L, Vec3d.ZERO, Vec3d.ZERO, Vec3d.ZERO);
    }

    private static GrapplingHookVisualPayload create(
            int playerEntityId,
            int action,
            long startedAtWorldTick,
            Vec3d launch,
            Vec3d anchor,
            Vec3d destination
    ) {
        return new GrapplingHookVisualPayload(
                playerEntityId,
                action,
                startedAtWorldTick,
                launch.x,
                launch.y,
                launch.z,
                anchor.x,
                anchor.y,
                anchor.z,
                destination.x,
                destination.y,
                destination.z
        );
    }

    public Vec3d launch() {
        return new Vec3d(launchX, launchY, launchZ);
    }

    public Vec3d anchor() {
        return new Vec3d(anchorX, anchorY, anchorZ);
    }

    public Vec3d destination() {
        return new Vec3d(destinationX, destinationY, destinationZ);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
