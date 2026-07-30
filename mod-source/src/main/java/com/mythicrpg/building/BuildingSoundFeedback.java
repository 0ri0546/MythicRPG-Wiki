package com.mythicrpg.building;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

/** Centralized, deliberately restrained vanilla SFX language for Building tools. */
public final class BuildingSoundFeedback {
    private BuildingSoundFeedback() {
    }

    public static void pointA(ServerPlayerEntity player, BlockPos pos) {
        play(player, pos, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.35F, 1.15F);
    }

    public static void pointB(ServerPlayerEntity player, BlockPos pos) {
        play(player, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.55F, 1.20F);
    }

    public static void planLocked(ServerPlayerEntity player) {
        play(player, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.60F, 1.05F);
    }

    public static void buildStarted(ServerPlayerEntity player, BlockPos pos) {
        play(player, pos, SoundEvents.BLOCK_PISTON_EXTEND, 0.45F, 1.15F);
    }

    public static void buildCompleted(ServerPlayerEntity player) {
        play(player, player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.55F, 1.25F);
    }

    public static void miniatureFinalized(ServerPlayerEntity player) {
        play(player, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.50F, 1.35F);
    }

    public static void miniaturePlaced(ServerPlayerEntity player, BlockPos pos) {
        play(player, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.50F, 0.95F);
    }

    public static void miniatureRotated(ServerPlayerEntity player, BlockPos pos) {
        play(player, pos, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.35F, 1.45F);
    }

    public static void miniatureRetrieved(ServerPlayerEntity player, BlockPos pos) {
        play(player, pos, SoundEvents.ENTITY_ITEM_PICKUP, 0.45F, 1.10F);
    }

    public static void compassCenter(ServerPlayerEntity player, BlockPos pos) {
        play(player, pos, SoundEvents.BLOCK_LODESTONE_PLACE, 0.45F, 1.25F);
    }

    public static void compassApplied(ServerPlayerEntity player) {
        play(player, player.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.45F, 1.30F);
    }

    public static void decorationApplied(ServerPlayerEntity player, BlockPos pos) {
        play(player, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.45F, 1.35F);
    }

    public static void error(ServerPlayerEntity player) {
        play(player, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.30F, 0.75F);
    }

    private static void play(
            ServerPlayerEntity player,
            BlockPos pos,
            SoundEvent sound,
            float volume,
            float pitch
    ) {
        if (player == null || player.getWorld() == null) return;
        player.getWorld().playSound(null, pos, sound, SoundCategory.PLAYERS, volume, pitch);
    }
}
