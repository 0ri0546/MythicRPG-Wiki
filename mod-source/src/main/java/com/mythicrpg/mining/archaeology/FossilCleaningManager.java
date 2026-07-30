package com.mythicrpg.mining.archaeology;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.PlayerCooldownManager;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class FossilCleaningManager {

    public static final int MAX_BRUSH_USE_TICKS = 72_000;
    private static final int BRUSH_PULSE_INTERVAL = 10;
    private static final int BRUSH_PROGRESS_PER_PULSE = 10;

    private FossilCleaningManager() {
    }

    public static boolean isLookingAtFossil(LivingEntity user) {
        if (!(user instanceof net.minecraft.entity.player.PlayerEntity player)) {
            return false;
        }
        HitResult hitResult = player.raycast(player.getBlockInteractionRange(), 0.0F, false);
        return hitResult instanceof BlockHitResult blockHit
                && hitResult.getType() == HitResult.Type.BLOCK
                && user.getWorld().getBlockEntity(blockHit.getBlockPos()) instanceof FossilBlockEntity;
    }

    public static boolean handleUsageTick(
            World world,
            LivingEntity user,
            ItemStack brush,
            int remainingUseTicks
    ) {
        if (!(user instanceof net.minecraft.entity.player.PlayerEntity player)) {
            return false;
        }

        HitResult hitResult = player.raycast(player.getBlockInteractionRange(), 0.0F, false);
        if (!(hitResult instanceof BlockHitResult blockHit) || hitResult.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockPos pos = blockHit.getBlockPos();
        if (!(world.getBlockEntity(pos) instanceof FossilBlockEntity fossil)) {
            return false;
        }

        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer
                && !SkillTreeManager.hasBonus(
                        serverPlayer,
                        SkillType.MINING,
                        BonusType.FOSSIL_EXCAVATION
                )) {
            if (PlayerCooldownManager.tryUse(serverPlayer, "fossil_excavation_locked", 20)) {
                serverPlayer.sendMessage(
                        Text.translatable("message.mythicrpg.fossil.locked")
                                .formatted(Formatting.RED),
                        true
                );
            }
            user.stopUsingItem();
            return true;
        }

        int usedTicks = MAX_BRUSH_USE_TICKS - remainingUseTicks + 1;
        if (usedTicks % BRUSH_PULSE_INTERVAL != 5) {
            return true;
        }

        BlockState state = world.getBlockState(pos);
        world.addParticle(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, state),
                blockHit.getPos().x,
                blockHit.getPos().y,
                blockHit.getPos().z,
                0.0,
                0.02,
                0.0
        );
        world.playSound(
                player,
                pos,
                SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC,
                SoundCategory.BLOCKS,
                0.8F,
                1.0F
        );

        if (world instanceof ServerWorld serverWorld && player instanceof ServerPlayerEntity serverPlayer) {
            serverWorld.spawnParticles(
                    serverPlayer,
                    new DustParticleEffect(fossil.rarity().particleColor(), 0.65F),
                    false,
                    blockHit.getPos().x,
                    blockHit.getPos().y,
                    blockHit.getPos().z,
                    1,
                    0.04,
                    0.04,
                    0.04,
                    0.0
            );
            boolean completed = fossil.applyBrush(serverPlayer, BRUSH_PROGRESS_PER_PULSE);
            if (completed) {
                EquipmentSlot equipmentSlot = serverPlayer.getActiveHand() == Hand.OFF_HAND
                        ? EquipmentSlot.OFFHAND
                        : EquipmentSlot.MAINHAND;
                brush.damage(1, serverPlayer, equipmentSlot);
                user.stopUsingItem();
            }
        }

        return true;
    }
}
