package com.mythicrpg.fighting.items;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class BaronsDollItem extends LegendaryTooltipItem {
    private static final int COOLDOWN_TICKS = 20 * 120;

    public BaronsDollItem(Settings settings) {
        super(settings, "tooltip.mythicrpg.barons_doll.flavor");
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity player) || !(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.pass(stack);
        }

        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        ArmorStandEntity doll = new ArmorStandEntity(EntityType.ARMOR_STAND, serverWorld);
        doll.setPosition(player.getX(), player.getY(), player.getZ());
        doll.setCustomName(Text.translatable("item.mythicrpg.barons_doll.named", player.getName()).formatted(Formatting.GOLD));
        doll.setCustomNameVisible(true);
        doll.setShowArms(true);
        doll.setHideBasePlate(true);
        doll.setHealth(20.0f);
        applyPlayerResistance(player, doll);
        doll.addCommandTag(BaronLegendaryItemEffects.BARONS_DOLL_TAG);

        serverWorld.spawnEntity(doll);
        BaronLegendaryItemEffects.trackDoll(player, doll);

        serverWorld.spawnParticles(ParticleTypes.SOUL, doll.getX(), doll.getBodyY(0.6), doll.getZ(), 24, 0.45, 0.55, 0.45, 0.04);
        serverWorld.playSound(null, doll.getBlockPos(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.6f, 1.7f);
        player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

        return TypedActionResult.success(stack);
    }


    private static void applyPlayerResistance(ServerPlayerEntity player, ArmorStandEntity doll) {
        EntityAttributeInstance dollArmor = doll.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (dollArmor != null) {
            dollArmor.setBaseValue(player.getArmor());
        }

        EntityAttributeInstance dollArmorToughness = doll.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
        if (dollArmorToughness != null) {
            dollArmorToughness.setBaseValue(player.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
        }
    }

}
