package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.crafting.station.CraftingStationDurabilityManager;
import com.mythicrpg.crafting.station.CraftingTableDurabilityState;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class RepairKitItem extends Item {

    private static final double BASE_REPAIR_POWER = 0.10;

    public RepairKitItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        tooltip.add(Text.translatable("tooltip.mythicrpg.repair_kit.description")
                .formatted(Formatting.GRAY));

        tooltip.add(Text.translatable("tooltip.mythicrpg.repair_kit.use")
                .formatted(Formatting.GREEN));

        tooltip.add(Text.translatable("tooltip.mythicrpg.repair_kit.power")
                .formatted(Formatting.YELLOW));

        tooltip.add(Text.translatable("tooltip.mythicrpg.repair_kit.portable")
                .formatted(Formatting.DARK_AQUA));
    }


    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();

        if (!world.getBlockState(context.getBlockPos()).isOf(Blocks.CRAFTING_TABLE)) {
            return super.useOnBlock(context);
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        ItemStack repairKit = context.getStack();
        int before = CraftingTableDurabilityState.get(serverPlayer.getServer())
                .getDurability(world, context.getBlockPos());

        if (before >= CraftingTableDurabilityState.MAX_DURABILITY) {
            sendFail(serverPlayer, "message.mythicrpg.repair_kit.table_full");
            return ActionResult.FAIL;
        }

        double repairPower = getRepairPower(serverPlayer);
        int repaired = CraftingStationDurabilityManager.repairVanillaTable(
                serverPlayer,
                world,
                context.getBlockPos(),
                repairPower
        );

        if (repaired <= 0) {
            sendFail(serverPlayer, "message.mythicrpg.repair_kit.table_cannot_repair");
            return ActionResult.FAIL;
        }

        if (!serverPlayer.isCreative()) {
            repairKit.decrement(1);
        }

        world.playSound(
                null,
                context.getBlockPos(),
                SoundEvents.BLOCK_ANVIL_USE,
                SoundCategory.PLAYERS,
                0.55f,
                1.35f
        );

        serverPlayer.sendMessage(
                Text.translatable("message.mythicrpg.repair_kit.table_repaired", repaired)
                        .formatted(Formatting.GREEN),
                true
        );

        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack repairKit = player.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(repairKit);
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.pass(repairKit);
        }

        ItemStack target = getTargetStack(player, hand);

        if (target.isEmpty()) {
            return tryRepairPortableCraftingTable(world, serverPlayer, repairKit);
        }

        if (!target.isDamageable()) {
            sendFail(serverPlayer, "message.mythicrpg.repair_kit.cannot_repair");
            return TypedActionResult.fail(repairKit);
        }

        if (target.contains(DataComponentTypes.UNBREAKABLE)) {
            sendFail(serverPlayer, "message.mythicrpg.repair_kit.unbreakable");
            return TypedActionResult.fail(repairKit);
        }

        if (!target.isDamaged()) {
            sendFail(serverPlayer, "message.mythicrpg.repair_kit.already_repaired");
            return TypedActionResult.fail(repairKit);
        }

        double repairPower = getRepairPower(serverPlayer);
        int maxDamage = target.getMaxDamage();
        int repairAmount = Math.max(1, (int) Math.ceil(maxDamage * repairPower));

        int oldDamage = target.getDamage();
        int newDamage = Math.max(0, oldDamage - repairAmount);
        int repaired = oldDamage - newDamage;

        if (repaired <= 0) {
            sendFail(serverPlayer, "message.mythicrpg.repair_kit.cannot_repair");
            return TypedActionResult.fail(repairKit);
        }

        target.setDamage(newDamage);

        if (!serverPlayer.isCreative()) {
            repairKit.decrement(1);
        }

        world.playSound(
                null,
                serverPlayer.getBlockPos(),
                SoundEvents.BLOCK_ANVIL_USE,
                SoundCategory.PLAYERS,
                0.55f,
                1.35f
        );

        serverPlayer.sendMessage(
                Text.translatable("message.mythicrpg.repair_kit.item_repaired", repaired)
                        .formatted(Formatting.GREEN),
                true
        );

        return TypedActionResult.success(repairKit);
    }

    private static ItemStack getTargetStack(PlayerEntity player, Hand repairKitHand) {
        return repairKitHand == Hand.MAIN_HAND
                ? player.getOffHandStack()
                : player.getMainHandStack();
    }

    private static double getRepairPower(ServerPlayerEntity player) {
        double bonus = SkillTreeManager.getBonusTotal(
                player,
                SkillType.CRAFTING,
                BonusType.REPAIR_KIT_POWER
        );

        return BASE_REPAIR_POWER + bonus;
    }

    private static void sendFail(ServerPlayerEntity player, String translationKey) {
        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_ANVIL_LAND,
                SoundCategory.PLAYERS,
                0.25f,
                1.8f
        );

        player.sendMessage(
                Text.translatable(translationKey).formatted(Formatting.RED),
                true
        );
    }

    private static TypedActionResult<ItemStack> tryRepairPortableCraftingTable(
            World world,
            ServerPlayerEntity player,
            ItemStack repairKit
    ) {
        if (!PortableCraftingManager.hasPortableCrafting(player)) {
            sendFail(player, "message.mythicrpg.repair_kit.hold_damaged_item");
            return TypedActionResult.fail(repairKit);
        }

        if (PortableCraftingManager.getDurability(player) >= PortableCraftingState.MAX_DURABILITY) {
            sendFail(player, "message.mythicrpg.repair_kit.portable_full");
            return TypedActionResult.fail(repairKit);
        }

        double repairPower = getRepairPower(player);
        int repaired = PortableCraftingManager.repair(player, repairPower);

        if (repaired <= 0) {
            sendFail(player, "message.mythicrpg.repair_kit.portable_cannot_repair");
            return TypedActionResult.fail(repairKit);
        }

        if (!player.isCreative()) {
            repairKit.decrement(1);
        }

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_ANVIL_USE,
                SoundCategory.PLAYERS,
                0.55f,
                1.35f
        );

        player.sendMessage(
                Text.translatable("message.mythicrpg.repair_kit.portable_repaired", repaired)
                        .formatted(Formatting.GREEN),
                true
        );

        return TypedActionResult.success(repairKit);
    }
}