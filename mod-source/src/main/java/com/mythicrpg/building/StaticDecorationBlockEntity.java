package com.mythicrpg.building;

import com.mythicrpg.core.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** Non-ticking storage for one static visual and its owner. */
public final class StaticDecorationBlockEntity extends BlockEntity {
    private StaticDecorationEffect effect = StaticDecorationEffect.ELECTRIC_SPARK;
    private UUID owner;
    private long lastClientEmissionTick = Long.MIN_VALUE;

    public StaticDecorationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATIC_DECORATION, pos, state);
    }

    public StaticDecorationEffect effect() {
        return effect;
    }

    public UUID owner() {
        return owner;
    }

    public boolean isOwner(UUID player) {
        return owner != null && owner.equals(player);
    }

    /** Client-only timing state; deliberately not persisted or ticked on the server. */
    public boolean shouldEmitClientParticle(long worldTick, int intervalTicks) {
        int safeInterval = Math.max(1, intervalTicks);
        if (lastClientEmissionTick != Long.MIN_VALUE
                && worldTick - lastClientEmissionTick < safeInterval) {
            return false;
        }
        lastClientEmissionTick = worldTick;
        return true;
    }

    public void configure(UUID owner, StaticDecorationEffect effect) {
        this.owner = owner;
        this.effect = effect == null ? StaticDecorationEffect.ELECTRIC_SPARK : effect;
        sync();
    }

    public void setEffect(StaticDecorationEffect effect) {
        StaticDecorationEffect safe = effect == null ? StaticDecorationEffect.ELECTRIC_SPARK : effect;
        if (this.effect == safe) return;
        this.effect = safe;
        sync();
    }

    private void sync() {
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        effect = StaticDecorationEffect.byId(nbt.getString("Effect")).orElse(StaticDecorationEffect.ELECTRIC_SPARK);
        owner = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("Effect", effect.id());
        if (owner != null) nbt.putUuid("Owner", owner);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
