package com.mythicrpg.mixin;

import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Targeted chunk serialization used to commit completed Building plan jobs safely. */
@Mixin(ServerChunkLoadingManager.class)
public interface ServerChunkLoadingManagerBuildingPlanInvoker {
    @Invoker("save")
    boolean mythicrpg$saveBuildingPlanChunk(Chunk chunk);
}
