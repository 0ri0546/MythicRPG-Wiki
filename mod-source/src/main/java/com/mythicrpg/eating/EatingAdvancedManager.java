package com.mythicrpg.eating;

import com.mythicrpg.core.ItemContainerUtils;
import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.farming.FoodBackpackSessionManager;
import com.mythicrpg.farming.FoodBackpackPerishableIndex;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EatingAdvancedManager {
    private static final int CHEF_AURA_RADIUS = 8;
    private static final int CHEF_AURA_DURATION = 5 * 60 * 20;
    private static final int CHEF_AURA_COOLDOWN = 20 * 60 * 20;
    private static final int COMPLETE_MEAL_STEP_WINDOW = 5 * 60 * 20;
    private static final int COMPLETE_MEAL_DURATION = 10 * 60 * 20;
    private static final int INTERNATIONAL_DURATION = 3 * 60 * 20;
    private static final int RISK_RADIUS = 16;
    private static final int RISK_EFFECT_DURATION = 2 * 60 * 20;
    private static final int RISK_COOLDOWN = 10 * 60 * 20;
    private static final int AUTO_FEED_THRESHOLD = 10;
    private static final int BACKPACK_SLOTS = 54;
    private static final String RENOWN_USED_TAG = "mythicrpg:chef_renown_used";
    private static final String RENOWN_STUN_TAG = "mythicrpg:chef_renown_stunned";
    private static final Map<UUID, StunEntry> ACTIVE_STUNS = new HashMap<>();

    private static final Identifier SIGNATURE_DAMAGE_ID = Identifier.of("mythicrpg", "signature_damage");
    private static final Identifier SIGNATURE_SPEED_ID = Identifier.of("mythicrpg", "signature_speed");

    private static final List<RegistryEntry<StatusEffect>> RISK_EFFECTS = List.of(
            StatusEffects.SPEED,
            StatusEffects.HASTE,
            StatusEffects.STRENGTH,
            StatusEffects.REGENERATION,
            StatusEffects.RESISTANCE,
            StatusEffects.FIRE_RESISTANCE,
            StatusEffects.WATER_BREATHING,
            StatusEffects.NIGHT_VISION,
            StatusEffects.LUCK
    );

    private EatingAdvancedManager() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof MobEntity mob && mob.getCommandTags().contains(RENOWN_STUN_TAG)) {
                mob.removeCommandTag(RENOWN_STUN_TAG);
                if (mob.isAiDisabled()) {
                    mob.setAiDisabled(false);
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickStuns();
            long time = server.getOverworld().getTime();
            if (time % 20L == 0L) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    tickAutoFeed(player);
                    refreshSignatureModifier(player, player.getWorld().getTime());
                }
            }
        });
    }

    public static ActionResult tryOfferDish(
            ServerPlayerEntity player,
            HostileEntity hostile,
            ItemStack held
    ) {
        if (BaronMobManager.isBaron(hostile)
                || hostile.isAiDisabled()
                || !EatingPerks.hasChefRenown(player)
                || hostile.getCommandTags().contains(RENOWN_USED_TAG)) {
            return ActionResult.PASS;
        }

        Optional<GiftDish> gift = findGiftDish(player, held, player.getWorld().getTime());
        if (gift.isEmpty()) {
            return ActionResult.PASS;
        }

        hostile.addCommandTag(RENOWN_USED_TAG);
        stun(hostile, player.getWorld().getTime() + 40L);
        hostile.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 255, false, false, true));
        hostile.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 255, false, false, true));
        hostile.setVelocity(0.0, Math.min(0.0, hostile.getVelocity().y), 0.0);
        hostile.velocityModified = true;

        if (!player.isCreative()) {
            gift.get().consume();
        }
        player.getWorld().playSound(
                null,
                hostile.getBlockPos(),
                SoundEvents.ENTITY_VILLAGER_YES,
                SoundCategory.HOSTILE,
                0.8F,
                0.8F
        );
        player.sendMessage(
                Text.translatable("message.mythicrpg.eating.chef_renown")
                        .formatted(Formatting.GOLD),
                true
        );
        return ActionResult.SUCCESS;
    }

    public static void clear() {
        for (StunEntry entry : ACTIVE_STUNS.values()) {
            restoreAi(entry);
        }
        ACTIVE_STUNS.clear();
    }

    private static void stun(HostileEntity hostile, long releaseAt) {
        boolean wasAiDisabled = hostile.isAiDisabled();
        hostile.setAiDisabled(true);
        hostile.setTarget(null);
        hostile.getNavigation().stop();
        hostile.addCommandTag(RENOWN_STUN_TAG);
        ACTIVE_STUNS.put(hostile.getUuid(), new StunEntry(hostile, releaseAt, wasAiDisabled));
    }

    private static void tickStuns() {
        Iterator<Map.Entry<UUID, StunEntry>> iterator = ACTIVE_STUNS.entrySet().iterator();
        while (iterator.hasNext()) {
            StunEntry entry = iterator.next().getValue();
            MobEntity mob = entry.mob();
            if (mob.isRemoved() || mob.getWorld().getTime() >= entry.releaseAt()) {
                restoreAi(entry);
                iterator.remove();
            }
        }
    }

    private static void restoreAi(StunEntry entry) {
        MobEntity mob = entry.mob();
        mob.removeCommandTag(RENOWN_STUN_TAG);
        if (!entry.wasAiDisabled() && mob.isAiDisabled()) {
            mob.setAiDisabled(false);
        }
    }

    public static void onCookingCompleted(ServerWorld world, BlockPos potPos, UUID chefUuid) {
        if (chefUuid == null) {
            return;
        }
        ServerPlayerEntity chef = world.getServer().getPlayerManager().getPlayer(chefUuid);
        if (chef == null || chef.getWorld() != world || !EatingPerks.hasChefAura(chef)) {
            return;
        }
        Box box = new Box(potPos).expand(CHEF_AURA_RADIUS);
        if (world.getEntitiesByClass(VillagerEntity.class, box, LivingEntity::isAlive).isEmpty()) {
            return;
        }

        long now = world.getTime();
        EatingRuntimeData runtime = ModAttachments.getEatingRuntime(chef);
        if (now < runtime.chefAuraReadyAt()) {
            return;
        }
        chef.addStatusEffect(new StatusEffectInstance(
                StatusEffects.HERO_OF_THE_VILLAGE,
                CHEF_AURA_DURATION,
                0,
                false,
                true,
                true
        ));
        ModAttachments.setEatingRuntime(chef, runtime.withChefAuraReadyAt(now + CHEF_AURA_COOLDOWN));
        chef.sendMessage(Text.translatable("message.mythicrpg.eating.chef_aura")
                .formatted(Formatting.GREEN), true);
    }

    public static void onDishConsumed(ServerPlayerEntity player, PreparedDishData.Dish dish, ItemStack consumedStack) {
        if (dish.dubious()) {
            return;
        }
        handleCompleteMeal(player, dish.category());
        handleRiskTaste(player);
        handleInternationalGastronomy(player, dish);
        SignatureDishData.read(consumedStack).ifPresent(signature -> applySignatureBonus(player, signature));
    }

    public static float modifyIncomingDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        if (source.getAttacker() == null && source.getSource() == null) {
            return amount;
        }
        EatingRuntimeData runtime = ModAttachments.getEatingRuntime(player);
        SignatureBonus bonus = SignatureBonus.byId(runtime.activeSignatureBonus()).orElse(null);
        if (bonus != SignatureBonus.RESISTANCE
                || player.getWorld().getTime() >= runtime.activeSignatureExpiresAt()) {
            return amount;
        }
        return amount * 0.90F;
    }

    private static void handleCompleteMeal(ServerPlayerEntity player, DishCategory category) {
        if (!EatingPerks.hasCompleteMeal(player) || category == DishCategory.DRINK) {
            return;
        }
        long now = player.getWorld().getTime();
        EatingRuntimeData runtime = ModAttachments.getEatingRuntime(player);
        int stage = now <= runtime.mealDeadline() ? runtime.mealStage() : 0;

        if (category == DishCategory.STARTER) {
            ModAttachments.setEatingRuntime(player, runtime.withMeal(1, now + COMPLETE_MEAL_STEP_WINDOW));
            return;
        }
        if (category == DishCategory.MAIN && stage == 1) {
            ModAttachments.setEatingRuntime(player, runtime.withMeal(2, now + COMPLETE_MEAL_STEP_WINDOW));
            return;
        }
        if (category == DishCategory.DESSERT && stage == 2) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SATURATION,
                    COMPLETE_MEAL_DURATION,
                    0,
                    false,
                    true,
                    true
            ));
            ModAttachments.setEatingRuntime(player, runtime.withMeal(0, 0L));
            player.sendMessage(Text.translatable("message.mythicrpg.eating.complete_meal")
                    .formatted(Formatting.GOLD), true);
            return;
        }
        ModAttachments.setEatingRuntime(player, runtime.withMeal(0, 0L));
    }

    private static void handleRiskTaste(ServerPlayerEntity player) {
        if (!EatingPerks.hasRiskTaste(player) || !(player.getWorld() instanceof ServerWorld world)) {
            return;
        }
        long now = world.getTime();
        EatingRuntimeData runtime = ModAttachments.getEatingRuntime(player);
        if (now < runtime.riskTasteReadyAt()) {
            return;
        }
        Box box = player.getBoundingBox().expand(RISK_RADIUS);
        boolean baronNearby = !world.getEntitiesByClass(
                LivingEntity.class,
                box,
                entity -> entity.isAlive() && BaronMobManager.isBaron(entity)
        ).isEmpty();
        if (!baronNearby) {
            return;
        }
        RegistryEntry<StatusEffect> effect = RISK_EFFECTS.get(world.random.nextInt(RISK_EFFECTS.size()));
        player.addStatusEffect(new StatusEffectInstance(effect, RISK_EFFECT_DURATION, 0, false, true, true));
        ModAttachments.setEatingRuntime(player, runtime.withRiskTasteReadyAt(now + RISK_COOLDOWN));
        player.sendMessage(Text.translatable("message.mythicrpg.eating.risk_taste")
                .formatted(Formatting.LIGHT_PURPLE), true);
    }

    private static void handleInternationalGastronomy(ServerPlayerEntity player, PreparedDishData.Dish dish) {
        if (!EatingPerks.hasInternationalGastronomy(player)
                || dish.chef() == null
                || dish.chef().equals(player.getUuid())) {
            return;
        }
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SATURATION,
                INTERNATIONAL_DURATION,
                0,
                false,
                true,
                true
        ));
        player.sendMessage(Text.translatable("message.mythicrpg.eating.international")
                .formatted(Formatting.AQUA), true);
    }

    private static void applySignatureBonus(ServerPlayerEntity player, SignatureDishData.SignatureData signature) {
        long now = player.getWorld().getTime();
        long expiresAt = now + signature.durationTicks();
        EatingRuntimeData runtime = ModAttachments.getEatingRuntime(player);
        ModAttachments.setEatingRuntime(player, runtime.withActiveSignature(signature.bonus(), expiresAt));
        refreshSignatureModifier(player, now);
        player.sendMessage(Text.translatable(
                "message.mythicrpg.eating.signature_bonus",
                Text.translatable("signature_bonus.mythicrpg." + signature.bonus().id()),
                signature.durationTicks() / 20
        ).formatted(Formatting.GOLD), true);
    }

    private static void refreshSignatureModifier(ServerPlayerEntity player, long now) {
        EatingRuntimeData runtime = ModAttachments.getEatingRuntime(player);
        SignatureBonus bonus = SignatureBonus.byId(runtime.activeSignatureBonus()).orElse(null);
        if (bonus == null || now >= runtime.activeSignatureExpiresAt()) {
            if (!runtime.activeSignatureBonus().isBlank() || runtime.activeSignatureExpiresAt() != 0L) {
                ModAttachments.setEatingRuntime(player, runtime.withActiveSignature(null, 0L));
            }
            removeModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, SIGNATURE_DAMAGE_ID);
            removeModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SIGNATURE_SPEED_ID);
            return;
        }
        applyModifier(
                player,
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                SIGNATURE_DAMAGE_ID,
                bonus == SignatureBonus.DAMAGE ? 0.10 : 0.0
        );
        applyModifier(
                player,
                EntityAttributes.GENERIC_MOVEMENT_SPEED,
                SIGNATURE_SPEED_ID,
                bonus == SignatureBonus.SPEED ? 0.10 : 0.0
        );
    }

    private static void applyModifier(
            ServerPlayerEntity player,
            RegistryEntry<EntityAttribute> attribute,
            Identifier id,
            double value
    ) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null) {
            return;
        }
        EntityAttributeModifier current = instance.getModifier(id);
        if (value <= 0.0) {
            if (current != null) {
                instance.removeModifier(id);
            }
            return;
        }
        if (current != null
                && current.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
                && Double.compare(current.value(), value) == 0) {
            return;
        }
        if (current != null) {
            instance.removeModifier(id);
        }
        instance.addTemporaryModifier(new EntityAttributeModifier(
                id,
                value,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));
    }

    private static void removeModifier(
            ServerPlayerEntity player,
            RegistryEntry<EntityAttribute> attribute,
            Identifier id
    ) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null && instance.getModifier(id) != null) {
            instance.removeModifier(id);
        }
    }

    private static Optional<GiftDish> findGiftDish(ServerPlayerEntity player, ItemStack held, long gameTime) {
        Optional<PreparedDishData.Dish> direct = PreparedDishData.read(held);
        if (direct.isPresent()) {
            PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(held, gameTime);
            if (dish.dubious()) {
                return Optional.empty();
            }
            return Optional.of(new GiftDish(() -> {
                held.decrement(1);
                giveBowl(player);
            }));
        }
        if (!(held.getItem() instanceof ServingPlateItem)) {
            return Optional.empty();
        }
        Optional<ItemStack> selected = ServingPlateData.selectedPortion(held);
        if (selected.isEmpty()) {
            return Optional.empty();
        }
        PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(selected.get(), gameTime);
        if (dish.dubious()) {
            return Optional.empty();
        }
        return Optional.of(new GiftDish(() -> ServingPlateData.removeSelectedPortion(held)));
    }

    private static void tickAutoFeed(ServerPlayerEntity player) {
        if (!EatingPerks.hasAutoFeed(player)
                || !player.isAlive()
                || player.isSpectator()
                || player.isCreative()
                || player.getHungerManager().getFoodLevel() > AUTO_FEED_THRESHOLD) {
            return;
        }
        if (consumeBestAvailableDish(player)) {
            playAutoFeedSound(player);
        }
    }

    private static boolean consumeBestAvailableDish(ServerPlayerEntity player) {
        long time = player.getWorld().getTime();
        AutoFeedTarget best = null;

        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(ModItems.FOOD_BACKPACK)) {
                best = betterTarget(best, bestBackpackTarget(player, stack, time));
            } else {
                long expiry = candidateExpiry(stack, time);
                if (expiry != Long.MAX_VALUE) {
                    best = betterTarget(best, new AutoFeedTarget(
                            stack,
                            expiry,
                            player.getInventory()::markDirty
                    ));
                }
            }
        }

        if (best == null || !consumeCandidate(player, best.stack())) {
            return false;
        }
        best.commit().run();
        return true;
    }

    private static AutoFeedTarget bestBackpackTarget(
            ServerPlayerEntity player,
            ItemStack backpack,
            long time
    ) {
        var openInventory = FoodBackpackSessionManager.inventory(player, backpack);
        if (openInventory.isPresent()) {
            AutoFeedTarget best = null;
            for (int slot = 0; slot < openInventory.get().size(); slot++) {
                ItemStack stack = openInventory.get().getStack(slot);
                long expiry = candidateExpiry(stack, time);
                if (expiry != Long.MAX_VALUE) {
                    best = betterTarget(best, new AutoFeedTarget(
                            stack,
                            expiry,
                            openInventory.get()::markDirty
                    ));
                }
            }
            return best;
        }

        if (!FoodBackpackPerishableIndex.hasPerishables(backpack, BACKPACK_SLOTS)) {
            return null;
        }

        DefaultedList<ItemStack> contents = ItemContainerUtils.read(backpack, BACKPACK_SLOTS);
        AutoFeedTarget best = null;
        for (ItemStack stack : contents) {
            long expiry = candidateExpiry(stack, time);
            if (expiry != Long.MAX_VALUE) {
                best = betterTarget(best, new AutoFeedTarget(
                        stack,
                        expiry,
                        () -> {
                            ItemContainerUtils.write(backpack, contents);
                            FoodBackpackPerishableIndex.refresh(backpack, BACKPACK_SLOTS);
                            player.getInventory().markDirty();
                        }
                ));
            }
        }
        return best;
    }

    private static AutoFeedTarget betterTarget(AutoFeedTarget current, AutoFeedTarget candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.expiresAt() < current.expiresAt() ? candidate : current;
    }

    private static long candidateExpiry(ItemStack stack, long time) {
        Optional<PreparedDishData.Dish> direct = PreparedDishData.read(stack);
        if (direct.isPresent()) {
            PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(stack, time);
            return dish.dubious() ? Long.MAX_VALUE : expirySortValue(dish, time);
        }
        if (!(stack.getItem() instanceof ServingPlateItem)) {
            return Long.MAX_VALUE;
        }
        return ServingPlateData.selectedPortion(stack)
                .map(portion -> PreparedDishData.refreshExpiration(portion, time))
                .filter(dish -> !dish.dubious())
                .map(dish -> expirySortValue(dish, time))
                .orElse(Long.MAX_VALUE);
    }

    private static long expirySortValue(PreparedDishData.Dish dish, long time) {
        long remaining = PreparedDishData.remainingTicks(dish, time);
        return remaining <= 0L ? Long.MAX_VALUE : remaining;
    }

    private static boolean consumeCandidate(ServerPlayerEntity player, ItemStack stack) {
        long time = player.getWorld().getTime();
        if (PreparedDishData.read(stack).isPresent()) {
            ItemStack consumed = stack.copyWithCount(1);
            PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(consumed, time);
            if (dish.dubious()) {
                return false;
            }
            PreparedDishConsumption.consume(player, dish, consumed);
            if (!player.isCreative()) {
                stack.decrement(1);
                giveBowl(player);
            }
            return true;
        }
        if (!(stack.getItem() instanceof ServingPlateItem)) {
            return false;
        }
        Optional<ItemStack> preview = ServingPlateData.selectedPortion(stack)
                .map(value -> value.copyWithCount(1));
        if (preview.isEmpty()) {
            return false;
        }
        PreparedDishData.Dish previewDish = PreparedDishData.refreshExpiration(preview.get(), time);
        if (previewDish.dubious()) {
            return false;
        }
        Optional<ItemStack> portion = player.isCreative()
                ? preview
                : ServingPlateData.removeSelectedPortion(stack);
        if (portion.isEmpty()) {
            return false;
        }
        PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(portion.get(), time);
        PreparedDishConsumption.consumeFromPlate(player, dish, portion.get());
        return true;
    }

    private static void giveBowl(ServerPlayerEntity player) {
        ItemStack bowl = new ItemStack(Items.BOWL);
        if (!player.getInventory().insertStack(bowl) && !bowl.isEmpty()) {
            player.dropItem(bowl, false);
        }
    }

    private static void playAutoFeedSound(ServerPlayerEntity player) {
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_EAT,
                SoundCategory.PLAYERS, 0.7F, 1.15F);
        player.sendMessage(Text.translatable("message.mythicrpg.eating.auto_feed")
                .formatted(Formatting.GREEN), true);
    }

    private record AutoFeedTarget(ItemStack stack, long expiresAt, Runnable commit) {
    }

    private record GiftDish(Runnable consume) {
    }

    private record StunEntry(MobEntity mob, long releaseAt, boolean wasAiDisabled) {
    }
}
