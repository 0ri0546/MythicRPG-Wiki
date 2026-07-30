package com.mythicrpg.building;

import com.mythicrpg.core.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Non-ticking storage for the six compact material identifiers of a Blank Block. */
public final class BlankBlockEntity extends BlockEntity {
    private static final String APPEARANCE_KEY = "Appearance";

    private BlankBlockAppearance appearance = BlankBlockAppearance.EMPTY;

    public BlankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLANK_BLOCK, pos, state);
    }

    public BlankBlockAppearance appearance() {
        return appearance;
    }

    public Identifier material(Direction face) {
        return appearance.material(face);
    }

    public Identifier setFace(Direction face, Identifier material) {
        if (material != null && BlankBlockMaterialRegistry.resolve(material).isEmpty()) {
            throw new IllegalArgumentException("Unsupported Blank Block material: " + material);
        }
        Identifier previous = appearance.material(face);
        if (java.util.Objects.equals(previous, material)) {
            return previous;
        }
        setAppearance(appearance.with(face, material));
        return previous;
    }

    public void setAppearance(BlankBlockAppearance appearance) {
        BlankBlockAppearance safe = appearance == null ? BlankBlockAppearance.EMPTY : appearance;
        if (!BlankBlockMaterialRegistry.isValid(safe)) {
            throw new IllegalArgumentException("Invalid Blank Block appearance");
        }
        if (this.appearance.equals(safe)) {
            return;
        }
        this.appearance = safe;
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (!nbt.contains(APPEARANCE_KEY, NbtElement.COMPOUND_TYPE)) {
            appearance = BlankBlockAppearance.EMPTY;
            return;
        }
        appearance = BlankBlockItemData.readAppearance(nbt.getCompound(APPEARANCE_KEY))
                .orElse(BlankBlockAppearance.EMPTY);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (!appearance.isEmpty()) {
            nbt.put(APPEARANCE_KEY, BlankBlockItemData.writeAppearance(appearance));
        }
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
