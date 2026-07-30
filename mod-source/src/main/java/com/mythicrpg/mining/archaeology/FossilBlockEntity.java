package com.mythicrpg.mining.archaeology;

import com.mythicrpg.core.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.UUID;

public final class FossilBlockEntity extends BlockEntity {

    private static final Direction[] DIRECTIONS = Direction.values();

    private FossilFamily family = FossilFamily.SMALL_LAND;
    private FossilRarity rarity = FossilRarity.COMMON;
    private FossilRarity dominantRarity = FossilRarity.COMMON;
    private int cleaningProgressTicks;

    private UUID siteId;
    private BlockPos siteCenter = BlockPos.ORIGIN;
    private int siteInitialBlocks = 1;
    private boolean registeredInSiteState;
    private boolean grandSite;

    public FossilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOSSIL, pos, state);
    }

    public void configureSite(
            UUID siteId,
            BlockPos siteCenter,
            FossilFamily family,
            FossilRarity rarity,
            FossilRarity dominantRarity,
            int siteInitialBlocks
    ) {
        this.siteId = siteId;
        this.siteCenter = siteCenter.toImmutable();
        this.family = family;
        this.rarity = rarity;
        this.dominantRarity = dominantRarity;
        this.siteInitialBlocks = Math.max(1, siteInitialBlocks);
        this.registeredInSiteState = false;
        this.grandSite = false;
        markDirty();
        if (world instanceof ServerWorld serverWorld) {
            registerSiteIfNeeded(serverWorld);
        }
    }

    public void configureGrandSite(
            UUID siteId,
            BlockPos siteCenter,
            FossilFamily family,
            FossilRarity rarity,
            FossilRarity dominantRarity,
            int siteInitialBlocks
    ) {
        this.siteId = siteId;
        this.siteCenter = siteCenter.toImmutable();
        this.family = family;
        this.rarity = rarity;
        this.dominantRarity = dominantRarity;
        this.siteInitialBlocks = Math.max(1, siteInitialBlocks);
        this.registeredInSiteState = true;
        this.grandSite = true;
        markDirty();
    }

    public FossilFamily family() {
        return family;
    }

    public FossilRarity rarity() {
        return rarity;
    }

    public FossilRarity dominantRarity() {
        return dominantRarity;
    }

    public boolean belongsToSite(UUID expectedSiteId) {
        return expectedSiteId != null && expectedSiteId.equals(siteId);
    }

    public boolean belongsToGrandSite(UUID expectedSiteId) {
        return grandSite && belongsToSite(expectedSiteId);
    }

    public boolean isGrandSiteFossil() {
        return grandSite;
    }

    public void recordRemovalFromSite() {
        if (!(world instanceof ServerWorld serverWorld) || siteId == null) {
            return;
        }

        if (grandSite) {
            GrandFossilSiteState.get(serverWorld.getServer()).markFossilRemoved(siteId);
        } else {
            FossilSiteState siteState = FossilSiteState.get(serverWorld.getServer());
            // Registration is idempotent and guarantees a correct decrement even if the block
            // is removed before its first scheduled server tick.
            siteState.registerSite(
                    siteId,
                    siteCenter,
                    family,
                    dominantRarity,
                    siteInitialBlocks
            );
            siteState.markExtracted(siteId);
        }

        // Empêche tout double comptage pendant le remplacement du bloc.
        siteId = null;
    }

    public int cleaningProgressTicks() {
        return cleaningProgressTicks;
    }

    public int requiredCleaningTicks() {
        return rarity.cleaningTicks();
    }

    public boolean applyBrush(ServerPlayerEntity player, int progressTicks) {
        if (world == null || world.isClient() || progressTicks <= 0) {
            return false;
        }

        cleaningProgressTicks = Math.min(requiredCleaningTicks(), cleaningProgressTicks + progressTicks);
        markDirty();
        if (cleaningProgressTicks >= requiredCleaningTicks()
                || cleaningProgressTicks % 20 == 0) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }

        if (cleaningProgressTicks < requiredCleaningTicks()) {
            return false;
        }

        completeExtraction(player);
        return true;
    }

    private void completeExtraction(ServerPlayerEntity player) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        ItemStack fossil = FossilContentRegistry.fossilItem(family, rarity)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
        if (fossil.isEmpty()) {
            return;
        }
        player.getInventory().offerOrDrop(fossil);

        serverWorld.spawnParticles(
                new DustParticleEffect(rarity.particleColor(), 1.15F),
                pos.getX() + 0.5,
                pos.getY() + 0.55,
                pos.getZ() + 0.5,
                14 + rarity.rank() * 3,
                0.32,
                0.32,
                0.32,
                0.025
        );
        serverWorld.spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.getDefaultState()),
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                8 + rarity.rank() * 2,
                0.28,
                0.28,
                0.28,
                0.05
        );
        spawnFamilyExtractionParticles(serverWorld);
        serverWorld.playSound(
                null,
                pos,
                SoundEvents.ITEM_BRUSH_BRUSHING_GRAVEL_COMPLETE,
                SoundCategory.BLOCKS,
                0.9F + rarity.rank() * 0.05F,
                0.86F + rarity.rank() * 0.07F
        );

        BlockState replacement = pos.getY() >= 0
                ? Blocks.STONE.getDefaultState()
                : Blocks.DEEPSLATE.getDefaultState();
        serverWorld.setBlockState(pos, replacement, Block.NOTIFY_ALL);
    }

    private void spawnFamilyExtractionParticles(ServerWorld world) {
        var particle = switch (family) {
            case SMALL_LAND -> ParticleTypes.HAPPY_VILLAGER;
            case MARINE -> ParticleTypes.SPLASH;
            case FLYING -> ParticleTypes.CLOUD;
            case INSECT -> ParticleTypes.CRIT;
            case LARGE_LAND -> ParticleTypes.POOF;
        };
        world.spawnParticles(
                particle,
                pos.getX() + 0.5,
                pos.getY() + 0.58,
                pos.getZ() + 0.5,
                4 + rarity.rank(),
                0.24,
                0.22,
                0.24,
                0.025
        );
    }

    void registerSiteIfNeeded(ServerWorld serverWorld) {
        if (registeredInSiteState || siteId == null || grandSite) {
            return;
        }
        FossilSiteState.get(serverWorld.getServer()).registerSite(
                siteId,
                siteCenter,
                family,
                dominantRarity,
                siteInitialBlocks
        );
        registeredInSiteState = true;
    }

    Direction findExposedFace(ServerWorld world) {
        int start = Math.floorMod((int) (world.getTime() + pos.asLong()), DIRECTIONS.length);
        for (int offset = 0; offset < DIRECTIONS.length; offset++) {
            Direction direction = DIRECTIONS[(start + offset) % DIRECTIONS.length];
            BlockPos adjacent = pos.offset(direction);
            if (!world.getChunkManager().isChunkLoaded(
                    adjacent.getX() >> 4,
                    adjacent.getZ() >> 4
            )) {
                continue;
            }
            BlockState adjacentState = world.getBlockState(adjacent);
            if (adjacentState.isAir()
                    || adjacentState.getCollisionShape(world, adjacent).isEmpty()) {
                return direction;
            }
        }
        return null;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        family = FossilFamily.byId(nbt.getString("family")).orElse(FossilFamily.SMALL_LAND);
        rarity = FossilRarity.byId(nbt.getString("rarity")).orElse(FossilRarity.COMMON);
        dominantRarity = nbt.contains("dominant_rarity")
                ? FossilRarity.byId(nbt.getString("dominant_rarity")).orElse(rarity)
                : rarity;
        cleaningProgressTicks = Math.max(0, Math.min(rarity.cleaningTicks(), nbt.getInt("cleaning_progress")));
        siteInitialBlocks = Math.max(1, nbt.getInt("site_initial_blocks"));
        siteCenter = nbt.contains("site_center")
                ? BlockPos.fromLong(nbt.getLong("site_center"))
                : pos;
        String siteIdString = nbt.getString("site_id");
        try {
            siteId = siteIdString.isEmpty() ? null : UUID.fromString(siteIdString);
        } catch (IllegalArgumentException ignored) {
            siteId = null;
        }
        grandSite = nbt.getBoolean("grand_site");
        registeredInSiteState = grandSite;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("family", family.id());
        nbt.putString("rarity", rarity.id());
        nbt.putString("dominant_rarity", dominantRarity.id());
        nbt.putInt("cleaning_progress", cleaningProgressTicks);
        nbt.putLong("site_center", siteCenter.asLong());
        nbt.putInt("site_initial_blocks", siteInitialBlocks);
        nbt.putBoolean("grand_site", grandSite);
        if (siteId != null) {
            nbt.putString("site_id", siteId.toString());
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
