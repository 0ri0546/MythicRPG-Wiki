package com.mythicrpg.fishing;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public final class WeatherWandItem extends Item {
    public WeatherWandItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!player.isSneaking()) return TypedActionResult.pass(stack);

        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            SeaMonsterType seal = sealMaterialType(player.getStackInHand(opposite(hand)));
            if (seal != null) {
                return applySeal(serverPlayer, stack, player.getStackInHand(opposite(hand)), seal)
                        ? TypedActionResult.success(stack, false)
                        : TypedActionResult.fail(stack);
            }

            FishingWeatherManager.Mode next = WeatherWandData.nextMode(stack);
            player.sendMessage(
                    Text.translatable("message.mythicrpg.weather_wand.mode", modeName(next)),
                    true
            );
            player.playSound(net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 0.55F, modePitch(next));
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient()) return ActionResult.SUCCESS;
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) return ActionResult.PASS;

        ItemStack wand = context.getStack();
        ItemStack otherHand = player.getStackInHand(opposite(context.getHand()));
        SeaMonsterType seal = player.isSneaking() ? sealMaterialType(otherHand) : null;
        if (seal != null) {
            return applySeal(player, wand, otherHand, seal) ? ActionResult.CONSUME : ActionResult.FAIL;
        }

        if (!context.getWorld().getRegistryKey().equals(World.OVERWORLD)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.fishing.weather_overworld_only").formatted(Formatting.RED),
                    true
            );
            return ActionResult.FAIL;
        }

        FishingWeatherManager.Mode selected = WeatherWandData.mode(wand);
        BonusType required = switch (selected) {
            case RAIN -> BonusType.FISHING_WEATHER_RAIN;
            case SUN -> BonusType.FISHING_WEATHER_SUN;
            case STORM -> BonusType.FISHING_WEATHER_STORM;
        };
        if (!SkillTreeManager.hasBonus(player, SkillType.FISHING, required)) {
            int perkId = switch (selected) {
                case RAIN -> 2;
                case SUN -> 3;
                case STORM -> 4;
            };
            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.perk_required",
                            Text.translatable("skill_tree.mythicrpg.fishing." + perkId + ".name")
                    ).formatted(Formatting.RED),
                    true
            );
            return ActionResult.FAIL;
        }

        SeaMonsterType monster = SeaMonsterType.forWeather(selected).orElseThrow();
        FishingWeatherManager.cast(
                player,
                selected,
                context.getBlockPos().offset(context.getSide()),
                WeatherWandData.hasSeal(wand, monster),
                WeatherWandData.isHarmonized(wand)
        );
        player.sendMessage(
                Text.translatable(
                        "message.mythicrpg.weather_wand.cast",
                        modeName(selected),
                        WeatherWandData.hasSeal(wand, monster) ? 15 : 10,
                        WeatherWandData.isHarmonized(wand)
                                ? FishingWeatherManager.HARMONIZED_RADIUS
                                : FishingWeatherManager.BASE_RADIUS
                ).formatted(Formatting.AQUA),
                true
        );
        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        FishingWeatherManager.Mode mode = WeatherWandData.mode(stack);
        SeaMonsterType monster = SeaMonsterType.forWeather(mode).orElseThrow();
        boolean sealedMode = WeatherWandData.hasSeal(stack, monster);
        tooltip.add(Text.translatable("tooltip.mythicrpg.weather_wand.mode", modeName(mode)).formatted(modeFormatting(mode)));
        tooltip.add(Text.translatable(
                "tooltip.mythicrpg.weather_wand.stats",
                sealedMode ? 15 : 10,
                WeatherWandData.isHarmonized(stack)
                        ? FishingWeatherManager.HARMONIZED_RADIUS
                        : FishingWeatherManager.BASE_RADIUS
        ).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mythicrpg.weather_wand.local").formatted(Formatting.DARK_GRAY));
        for (SeaMonsterType typeValue : SeaMonsterType.values()) {
            tooltip.add(Text.translatable(
                    WeatherWandData.hasSeal(stack, typeValue)
                            ? "tooltip.mythicrpg.weather_wand.seal_installed"
                            : "tooltip.mythicrpg.weather_wand.seal_missing",
                    typeValue.displayName()
            ).formatted(WeatherWandData.hasSeal(stack, typeValue) ? Formatting.GREEN : Formatting.DARK_GRAY));
        }
        if (WeatherWandData.isHarmonized(stack)) {
            tooltip.add(Text.translatable("tooltip.mythicrpg.weather_wand.harmonized").formatted(Formatting.GOLD));
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return WeatherWandData.hasAnySeal(stack);
    }

    public static float modelPredicate(ItemStack stack) {
        return WeatherWandData.modePredicate(stack);
    }

    private static boolean applySeal(
            ServerPlayerEntity player,
            ItemStack wand,
            ItemStack material,
            SeaMonsterType type
    ) {
        if (!WeatherWandData.applySeal(wand, type)) {
            player.sendMessage(Text.translatable("message.mythicrpg.weather_wand.seal_already", type.displayName()).formatted(Formatting.RED), true);
            return false;
        }
        if (!player.getAbilities().creativeMode) material.decrement(1);
        player.getInventory().markDirty();
        player.playSound(net.minecraft.sound.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.8F, 1.1F);
        player.sendMessage(Text.translatable("message.mythicrpg.weather_wand.seal_applied", type.displayName()).formatted(Formatting.GOLD), false);
        return true;
    }

    private static SeaMonsterType sealMaterialType(ItemStack stack) {
        if (stack.isOf(ModItems.NESSIE_SCALE)) return SeaMonsterType.NESSIE;
        if (stack.isOf(ModItems.MEGALODON_TOOTH)) return SeaMonsterType.MEGALODON;
        if (stack.isOf(ModItems.WHALE_AMBERGRIS)) return SeaMonsterType.WHALE;
        return null;
    }

    private static Hand opposite(Hand hand) {
        return hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    private static Text modeName(FishingWeatherManager.Mode mode) {
        return Text.translatable("fishing.weather.mythicrpg." + mode.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static Formatting modeFormatting(FishingWeatherManager.Mode mode) {
        return switch (mode) {
            case RAIN -> Formatting.AQUA;
            case SUN -> Formatting.GOLD;
            case STORM -> Formatting.LIGHT_PURPLE;
        };
    }

    private static float modePitch(FishingWeatherManager.Mode mode) {
        return switch (mode) {
            case RAIN -> 1.0F;
            case SUN -> 1.35F;
            case STORM -> 0.7F;
        };
    }
}
