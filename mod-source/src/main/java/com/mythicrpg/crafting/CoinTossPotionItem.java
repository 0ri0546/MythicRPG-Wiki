package com.mythicrpg.crafting;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

public class CoinTossPotionItem extends Item {

    private final boolean blessed;

    public CoinTossPotionItem(boolean blessed, Settings settings) {
        super(settings);
        this.blessed = blessed;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            if (blessed) {
                applyBlessedEffects(player);
            } else {
                applyCursedEffects(player);
            }
        }

        if (!(user instanceof PlayerEntity player) || !player.isCreative()) {
            stack.decrement(1);
        }

        return stack;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        tooltip.add(Text.translatable("tooltip.mythicrpg.coin_toss.description")
                .formatted(Formatting.GRAY));

        tooltip.add(Text.translatable("tooltip.mythicrpg.coin_toss.effects")
                .formatted(Formatting.LIGHT_PURPLE));

        tooltip.add(Text.translatable("tooltip.mythicrpg.coin_toss.use")
                .formatted(Formatting.DARK_GRAY));
    }

    private static void applyBlessedEffects(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 20 * 18, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 90, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 90, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 20 * 90, 0));
    }

    private static void applyCursedEffects(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 25, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 20 * 12, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 20 * 6, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 1));
    }
}