
package com.mythicrpg.fishing;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.SkillXpManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Server-authoritative entry point for every Fishing reward. */
public final class FishingManager {
    private FishingManager() {
    }

    public static void handleSuccessfulReel(ServerPlayerEntity player, ItemStack rod, BlockPos bobberPos) {
        if (!(rod.getItem() instanceof MythicFishingRodItem customRod)) {
            SkillXpManager.addXp(player, SkillType.FISHING, 1, false);
            return;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos catchPos = resolveFluidPosition(
                world,
                bobberPos == null ? player.getBlockPos() : bobberPos,
                customRod.forcedFamily()
        );
        FluidState fluid = world.getFluidState(catchPos);
        FishingFamily forcedFamily = customRod.forcedFamily();

        if (forcedFamily == FishingFamily.INFERNAL) {
            if (!world.getRegistryKey().equals(World.NETHER) || !fluid.isIn(FluidTags.LAVA)) {
                player.sendMessage(Text.translatable("message.mythicrpg.fishing.lava_required"), true);
                return;
            }
        } else if (forcedFamily == FishingFamily.VOID) {
            if (!world.getRegistryKey().equals(World.END) || !fluid.isIn(FluidTags.WATER)) {
                player.sendMessage(Text.translatable("message.mythicrpg.fishing.end_water_required"), true);
                return;
            }
        } else {
            if (!world.getRegistryKey().equals(World.OVERWORLD)) {
                player.sendMessage(
                        Text.translatable("message.mythicrpg.fishing.overworld_rod_required"),
                        true
                );
                return;
            }
            if (!fluid.isIn(FluidTags.WATER)) {
                return;
            }
        }

        FishingWeatherManager.HuntWeather huntWeather = FishingWeatherManager.ownedHuntWeatherAt(player, catchPos);
        SeaMonsterHuntContext huntContext = null;
        if (huntWeather != null) {
            SeaMonsterType monster = SeaMonsterType.forWeather(huntWeather.mode()).orElse(null);
            if (monster != null) {
                huntContext = new SeaMonsterHuntContext(
                        monster,
                        huntWeather.correspondingSeal()
                                ? SeaMonsterManager.SEALED_GAUGE_GAIN
                                : SeaMonsterManager.NORMAL_GAUGE_GAIN,
                        catchPos
                );
            }
        }

        FishingRodLoadout loadout = FishingRodLoadout.read(rod);
        FishingFamily family = forcedFamily != null
                ? forcedFamily
                : FishingFamily.select(world, catchPos, player.getRandom());
        FishingRarity rarity = FishingBalance.roll(player.getRandom(), loadout.bait(), loadout.rarityRune());

        if (loadout.bait() != null) {
            FishingRodLoadout.consumeOneBait(rod);
        }

        String biome = world.getBiome(catchPos)
                .getKey()
                .map(key -> key.getValue().toString())
                .orElse("");
        String dimension = world.getRegistryKey().getValue().toString();
        ItemStack reward = createCatch(family, rarity, biome, dimension, "rod");
        FishingCatchData.Catch caught = FishingCatchData.read(reward).orElseThrow();

        if (rarity.rank() >= FishingRarity.EPIC.rank()) {
            FishingMiniGameManager.begin(player, caught, reward, loadout.masteryRune(), huntContext);
        } else {
            grantCatch(player, reward, caught, huntContext);
        }
    }

    private static BlockPos resolveFluidPosition(
            ServerWorld world,
            BlockPos origin,
            FishingFamily forcedFamily
    ) {
        BlockPos[] candidates = {origin, origin.down(), origin.up()};
        for (BlockPos candidate : candidates) {
            FluidState fluid = world.getFluidState(candidate);
            if (forcedFamily == FishingFamily.INFERNAL) {
                if (fluid.isIn(FluidTags.LAVA)) {
                    return candidate.toImmutable();
                }
            } else if (fluid.isIn(FluidTags.WATER)) {
                return candidate.toImmutable();
            }
        }
        return origin.toImmutable();
    }

    public static ItemStack createCatch(
            FishingFamily family,
            FishingRarity rarity,
            String biome,
            String dimension,
            String source
    ) {
        ItemStack stack = new ItemStack(ModItems.FISHING_CATCH);
        FishingCatchData.write(stack, family, rarity, biome, dimension, source);
        return stack;
    }

    public static void grantCatch(ServerPlayerEntity player, ItemStack reward, FishingCatchData.Catch caught) {
        grantCatch(player, reward, caught, null);
    }

    public static void grantCatch(
            ServerPlayerEntity player,
            ItemStack reward,
            FishingCatchData.Catch caught,
            SeaMonsterHuntContext huntContext
    ) {
        boolean eligibleForCharm = "rod".equals(caught.source())
                && player.getOffHandStack().isOf(ModItems.MEGALODON_CHARM);
        boolean doubled = eligibleForCharm && player.getRandom().nextFloat() < 0.20F;
        int copies = doubled ? 2 : 1;

        if (eligibleForCharm) {
            player.getOffHandStack().damage(1, player, EquipmentSlot.OFFHAND);
        }

        for (int index = 0; index < copies; index++) {
            giveCatchStack(player, reward.copy());
            FishingCodexManager.record(player, caught);
            SkillXpManager.addXp(player, SkillType.FISHING, caught.rarity().xp(), false);
        }

        player.sendMessage(
                Text.translatable(
                        "message.mythicrpg.fishing.caught",
                        caught.rarity().displayName(),
                        caught.family().displayName()
                ).formatted(caught.rarity().formatting()),
                false
        );
        if (doubled) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.fishing.double_catch").formatted(Formatting.GOLD),
                    false
            );
        }
        SeaMonsterManager.onSuccessfulCatch(player, huntContext);
    }

    private static void giveCatchStack(ServerPlayerEntity player, ItemStack reward) {
        ItemStack remaining = reward.copy();
        player.getInventory().insertStack(remaining);
        if (!remaining.isEmpty()) {
            ItemEntity entity = new ItemEntity(
                    player.getWorld(),
                    player.getX(),
                    player.getY() + 0.5,
                    player.getZ(),
                    remaining
            );
            entity.setOwner(player.getUuid());
            player.getWorld().spawnEntity(entity);
        }
        player.getInventory().markDirty();
    }

    public static int netCapacity(ServerPlayerEntity player) {
        if (SkillTreeManager.hasBonus(player, SkillType.FISHING, BonusType.FISHING_NET_5)) return 5;
        if (SkillTreeManager.hasBonus(player, SkillType.FISHING, BonusType.FISHING_NET_4)) return 4;
        if (SkillTreeManager.hasBonus(player, SkillType.FISHING, BonusType.FISHING_NET_3)) return 3;
        return 0;
    }
}
