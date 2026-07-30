
package com.mythicrpg.fishing;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class MythicFishingRodItem extends FishingRodItem {
    private final FishingFamily forcedFamily;

    public MythicFishingRodItem(FishingFamily forcedFamily, Settings settings) {
        super(settings);
        this.forcedFamily = forcedFamily;
    }

    public FishingFamily forcedFamily() {
        return forcedFamily;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient && forcedFamily == FishingFamily.INFERNAL && !world.getRegistryKey().equals(World.NETHER)) {
            player.sendMessage(Text.translatable("message.mythicrpg.fishing.nether_only"), true);
            return TypedActionResult.fail(stack);
        }
        if (!world.isClient && forcedFamily == FishingFamily.VOID && !world.getRegistryKey().equals(World.END)) {
            player.sendMessage(Text.translatable("message.mythicrpg.fishing.end_only"), true);
            return TypedActionResult.fail(stack);
        }

        if (player.isSneaking() && player.fishHook == null) {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
                UUID rodId = FishingRodData.ensureId(stack);
                int sourceSlot = hand == Hand.OFF_HAND ? 40 : player.getInventory().selectedSlot;
                FishingRodInventory inventory = new FishingRodInventory(stack);
                serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId, playerInventory, ignored) -> new FishingRodScreenHandler(
                                syncId,
                                playerInventory,
                                inventory,
                                sourceSlot,
                                rodId
                        ),
                        Text.translatable("screen.mythicrpg.fishing_rod")
                ));
            }
            return TypedActionResult.success(stack, world.isClient());
        }

        return super.use(world, player, hand);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        FishingRodLoadout loadout = FishingRodLoadout.read(stack);
        tooltip.add(Text.translatable("tooltip.mythicrpg.fishing_rod.open").formatted(Formatting.DARK_GRAY));
        if (loadout.bait() != null) {
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.fishing_rod.bait",
                    upgradeName(loadout.bait())
            ).formatted(Formatting.GREEN));
        }
        if (loadout.rarityRune()) {
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.fishing_rod.rune",
                    upgradeName(FishingUpgradeItem.Kind.RUNE_RARITY)
            ).formatted(Formatting.LIGHT_PURPLE));
        }
        if (loadout.speedRune()) {
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.fishing_rod.rune",
                    upgradeName(FishingUpgradeItem.Kind.RUNE_SPEED)
            ).formatted(Formatting.AQUA));
        }
        if (loadout.masteryRune()) {
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.fishing_rod.rune",
                    upgradeName(FishingUpgradeItem.Kind.RUNE_MASTERY)
            ).formatted(Formatting.GOLD));
        }
    }

    private static Text upgradeName(FishingUpgradeItem.Kind kind) {
        return switch (kind) {
            case BAIT_I -> Text.translatable("item.mythicrpg.fishing_bait_i");
            case BAIT_II -> Text.translatable("item.mythicrpg.fishing_bait_ii");
            case BAIT_III -> Text.translatable("item.mythicrpg.fishing_bait_iii");
            case BAIT_LEGENDARY -> Text.translatable("item.mythicrpg.fishing_bait_legendary");
            case RUNE_RARITY -> Text.translatable("item.mythicrpg.fishing_rune_rarity");
            case RUNE_SPEED -> Text.translatable("item.mythicrpg.fishing_rune_speed");
            case RUNE_MASTERY -> Text.translatable("item.mythicrpg.fishing_rune_mastery");
        };
    }
}
