package com.mythicrpg.client.building.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/** Client-only UI feedback; all sounds are vanilla and intentionally quiet. */
public final class BuildingUiSounds {
    private BuildingUiSounds() {
    }

    public static void open() {
        play(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.10F);
    }

    public static void navigate() {
        play(SoundEvents.UI_BUTTON_CLICK.value(), 1.25F);
    }

    public static void rotate() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 1.35F);
    }


    public static void error() {
        play(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.75F);
    }

    private static void play(SoundEvent sound, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getSoundManager() == null) return;
        client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch));
    }
}

