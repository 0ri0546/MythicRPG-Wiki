package com.mythicrpg.crafting;

import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.core.ModBlocks;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillProgress;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.SkillXpManager;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.registry.RegistryKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LuckyBlockEventManager {

    private static final List<WeightedLuckyEvent> POSITIVE_EVENTS = List.of(
            new WeightedLuckyEvent(10, LuckyBlockEventManager::randomOreDrop),
            new WeightedLuckyEvent(5, LuckyBlockEventManager::mythicResourceDrop),
            new WeightedLuckyEvent(2, LuckyBlockEventManager::randomSkillSpark),
            new WeightedLuckyEvent(2, LuckyBlockEventManager::boundDiamondArmor),
            new WeightedLuckyEvent(2, LuckyBlockEventManager::redstoneEngineerPack),
            new WeightedLuckyEvent(2, LuckyBlockEventManager::coinTossBlessed),
            new WeightedLuckyEvent(4, LuckyBlockEventManager::templeVariant)
    );

    private static final List<WeightedLuckyEvent> NEUTRAL_EVENTS = List.of(
            new WeightedLuckyEvent(3, LuckyBlockEventManager::nothingHappens),
            new WeightedLuckyEvent(5, LuckyBlockEventManager::luckyBlockBlink),
            new WeightedLuckyEvent(5, LuckyBlockEventManager::visitAFriend),
            new WeightedLuckyEvent(3, LuckyBlockEventManager::impossibleChoice),
            new WeightedLuckyEvent(4, LuckyBlockEventManager::uselessBlastProtectionStick),
            new WeightedLuckyEvent(4, LuckyBlockEventManager::rainbowSheep)
    );

    private static final List<WeightedLuckyEvent> NEGATIVE_EVENTS = List.of(
            new WeightedLuckyEvent(10, LuckyBlockEventManager::minorCurse),
            new WeightedLuckyEvent(5, LuckyBlockEventManager::chickenJockeySquad),
            new WeightedLuckyEvent(2, LuckyBlockEventManager::fallingAnvil),
            new WeightedLuckyEvent(3, LuckyBlockEventManager::safeTnt),
            new WeightedLuckyEvent(3, LuckyBlockEventManager::coinTossCursed),
            new WeightedLuckyEvent(2, LuckyBlockEventManager::skyTrial),
            new WeightedLuckyEvent(4, LuckyBlockEventManager::baronRitual),
            new WeightedLuckyEvent(4, LuckyBlockEventManager::shuffleInventory)
    );

    private static final List<WeightedItem> ORE_POOL = List.of(
            new WeightedItem(Items.COAL, 10),
            new WeightedItem(Items.RAW_COPPER, 10),
            new WeightedItem(Items.RAW_IRON, 10),
            new WeightedItem(Items.RAW_GOLD, 6),
            new WeightedItem(Items.REDSTONE, 6),
            new WeightedItem(Items.LAPIS_LAZULI, 6),
            new WeightedItem(Items.DIAMOND, 2),
            new WeightedItem(Items.EMERALD, 2)
    );

    private LuckyBlockEventManager() {
    }

    public static void trigger(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        playOpeningFeedback(world, pos);

        LuckyEventCategory category = rollCategory(world, luck);

        switch (category) {
            case POSITIVE -> runWeightedEvent(world, pos, player, luck, POSITIVE_EVENTS);
            case NEUTRAL -> runWeightedEvent(world, pos, player, luck, NEUTRAL_EVENTS);
            case NEGATIVE -> runWeightedEvent(world, pos, player, luck, NEGATIVE_EVENTS);
        }
    }

    private static LuckyEventCategory rollCategory(ServerWorld world, int luck) {
        int clampedLuck = LuckyBlockLuckManager.clamp(luck);

        int neutralChance = Math.abs(clampedLuck) >= 9 ? 15 : 20;
        int positiveChance = 40 + (clampedLuck * 5);
        int negativeChance = 100 - neutralChance - positiveChance;

        positiveChance = Math.max(0, positiveChance);
        negativeChance = Math.max(0, negativeChance);

        int total = positiveChance + neutralChance + negativeChance;

        if (total <= 0) {
            return LuckyEventCategory.NEUTRAL;
        }

        int roll = world.random.nextInt(total);

        if (roll < positiveChance) {
            return LuckyEventCategory.POSITIVE;
        }

        roll -= positiveChance;

        if (roll < neutralChance) {
            return LuckyEventCategory.NEUTRAL;
        }

        return LuckyEventCategory.NEGATIVE;
    }

    private static void runWeightedEvent(
            ServerWorld world,
            BlockPos pos,
            ServerPlayerEntity player,
            int luck,
            List<WeightedLuckyEvent> events
    ) {
        int totalWeight = 0;

        for (WeightedLuckyEvent event : events) {
            totalWeight += event.weight();
        }

        if (totalWeight <= 0) {
            return;
        }

        int roll = world.random.nextInt(totalWeight);

        for (WeightedLuckyEvent event : events) {
            roll -= event.weight();

            if (roll < 0) {
                event.event().run(world, pos, player, luck);
                return;
            }
        }
    }

    // ---------------------------------------------------------------------
    // Positive events
    // ---------------------------------------------------------------------

    private static void randomOreDrop(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        WeightedItem selected = chooseWeightedItem(world, ORE_POOL);

        if (selected == null) {
            return;
        }

        dropStack(world, pos, new ItemStack(selected.item(), 1));
    }

    private static void randomSkillSpark(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        List<SkillType> eligibleSkills = new ArrayList<>();

        for (SkillType skill : SkillType.values()) {
            SkillProgress progress = ModAttachments.getProgress(player, skill);

            if (progress.getLevel() < SkillProgress.MAX_LEVEL) {
                eligibleSkills.add(skill);
            }
        }

        if (eligibleSkills.isEmpty()) {
            return;
        }

        SkillType selectedSkill = eligibleSkills.get(world.random.nextInt(eligibleSkills.size()));
        SkillProgress progress = ModAttachments.getProgress(player, selectedSkill);

        int xpForNextLevel = SkillProgress.xpRequiredForLevel(progress.getLevel());
        int xpGain = Math.max(1, (int) Math.floor(xpForNextLevel * 0.01));

        SkillXpManager.addXp(player, selectedSkill, xpGain, false);
    }

    private static void boundDiamondArmor(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        Item[] armorPieces = new Item[]{
                Items.DIAMOND_HELMET,
                Items.DIAMOND_CHESTPLATE,
                Items.DIAMOND_LEGGINGS,
                Items.DIAMOND_BOOTS
        };

        ItemStack armor = new ItemStack(armorPieces[world.random.nextInt(armorPieces.length)]);

        addEnchantment(world, armor, Enchantments.BINDING_CURSE, 1);
        addEnchantment(world, armor, Enchantments.PROJECTILE_PROTECTION, 3);

        dropStack(world, pos, armor);
    }

    private static void redstoneEngineerPack(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        dropStack(world, pos, new ItemStack(Items.REDSTONE_BLOCK, 5));
        dropStack(world, pos, new ItemStack(Items.PISTON, 5));
        dropStack(world, pos, new ItemStack(Items.SLIME_BLOCK, 5));
        dropStack(world, pos, new ItemStack(Items.QUARTZ, 5));
        dropStack(world, pos, new ItemStack(Items.REPEATER, 5));
        dropStack(world, pos, new ItemStack(Items.OBSERVER, 5));
        dropStack(world, pos, new ItemStack(Items.COMPARATOR, 5));
        dropStack(world, pos, new ItemStack(Items.HONEY_BLOCK, 5));
    }

    private static void mythicResourceDrop(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        List<ItemStack> pool = new ArrayList<>();

        pool.add(new ItemStack(ModItems.ENCHANTED_SEED));
        pool.add(new ItemStack(ModBlocks.ENCHANTED_WOOD));

        if (pool.isEmpty()) {
            return;
        }

        ItemStack reward = pool.get(world.random.nextInt(pool.size())).copy();
        dropStack(world, pos, reward);
    }

    private static void coinTossBlessed(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        dropStack(world, pos, new ItemStack(ModItems.COIN_TOSS_BLESSED, 1));
    }

    private static void templeVariant(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        LuckyBlockDelayedEventManager.scheduleTemple(
                world,
                pos.toImmutable(),
                player.getHorizontalFacing()
        );
    }

    // ---------------------------------------------------------------------
    // Neutral events
    // ---------------------------------------------------------------------

    private static void nothingHappens(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        // Intentionally empty.
    }

    private static void luckyBlockBlink(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        Optional<BlockPos> target = findNearbySafePosition(world, pos, 5);

        if (target.isEmpty()) {
            return;
        }

        BlockState state = ModBlocks.LUCKY_BLOCK.getDefaultState()
                .with(LuckyBlock.LUCK, LuckyBlock.encodeLuck(luck));

        world.setBlockState(target.get(), state);
    }

    private static void visitAFriend(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        List<ServerPlayerEntity> candidates = world.getPlayers(other ->
                other != player
                        && !other.isSpectator()
                        && !other.isRemoved()
        );

        if (candidates.isEmpty()) {
            return;
        }

        ServerPlayerEntity targetPlayer = candidates.get(world.random.nextInt(candidates.size()));
        Optional<BlockPos> safeTarget = findNearbySafePosition(world, targetPlayer.getBlockPos(), 6);

        if (safeTarget.isEmpty()) {
            return;
        }

        BlockPos teleportPos = safeTarget.get();

        player.requestTeleport(
                teleportPos.getX() + 0.5,
                teleportPos.getY(),
                teleportPos.getZ() + 0.5
        );
    }

    private static void impossibleChoice(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        LuckyBlockDelayedEventManager.scheduleCoinFlipChoice(
                world,
                pos.toImmutable(),
                player.getHorizontalFacing()
        );
    }

    private static void uselessBlastProtectionStick(
            ServerWorld world,
            BlockPos pos,
            ServerPlayerEntity player,
            int luck
    ) {
        ItemStack stick = new ItemStack(Items.STICK);
        addEnchantment(world, stick, Enchantments.BLAST_PROTECTION, 1);
        dropStack(world, pos, stick);
    }

    private static void rainbowSheep(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        Optional<BlockPos> spawnPos = findNearbySafePosition(world, pos, 3);
        BlockPos targetPos = spawnPos.orElse(pos.up());

        SheepEntity sheep = EntityType.SHEEP.create(world);

        if (sheep == null) {
            return;
        }

        sheep.refreshPositionAndAngles(
                targetPos.getX() + 0.5,
                targetPos.getY(),
                targetPos.getZ() + 0.5,
                world.random.nextFloat() * 360.0f,
                0.0f
        );
        sheep.setCustomName(Text.literal("jeb_"));
        sheep.setCustomNameVisible(true);

        world.spawnEntity(sheep);
    }

    // ---------------------------------------------------------------------
    // Negative events
    // ---------------------------------------------------------------------

    private static void baronRitual(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        LuckyBlockDelayedEventManager.scheduleBaronRitual(
                world,
                pos.toImmutable(),
                player.getUuid()
        );
    }

    private static void minorCurse(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        List<CurseEffect> effects = List.of(
                new CurseEffect(StatusEffects.SLOWNESS, 20 * 12, 1),
                new CurseEffect(StatusEffects.WEAKNESS, 20 * 12, 0),
                new CurseEffect(StatusEffects.MINING_FATIGUE, 20 * 12, 0),
                new CurseEffect(StatusEffects.HUNGER, 20 * 15, 0),
                new CurseEffect(StatusEffects.NAUSEA, 20 * 6, 0),
                new CurseEffect(StatusEffects.BLINDNESS, 20 * 4, 0),
                new CurseEffect(StatusEffects.POISON, 20 * 6, 0)
        );

        CurseEffect selected = effects.get(world.random.nextInt(effects.size()));

        player.addStatusEffect(new StatusEffectInstance(
                selected.effect(),
                selected.duration(),
                selected.amplifier()
        ));
    }

    private static void safeTnt(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        TntEntity tnt = new TntEntity(
                world,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                null
        );

        tnt.setFuse(60);
        world.spawnEntity(tnt);

        LuckyBlockDelayedEventManager.scheduleSafeTnt(
                world,
                tnt.getUuid(),
                tnt.getBlockPos(),
                58
        );
    }

    private static void chickenJockeySquad(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        for (int i = 0; i < 5; i++) {
            Optional<BlockPos> spawnPos = findNearbySafePosition(world, pos, 3);
            BlockPos targetPos = spawnPos.orElse(pos.up());

            ChickenEntity chicken = EntityType.CHICKEN.create(world);
            ZombieEntity zombie = EntityType.ZOMBIE.create(world);

            if (chicken == null || zombie == null) {
                continue;
            }

            chicken.refreshPositionAndAngles(
                    targetPos.getX() + 0.5,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5,
                    world.random.nextFloat() * 360.0f,
                    0.0f
            );

            zombie.refreshPositionAndAngles(
                    targetPos.getX() + 0.5,
                    targetPos.getY(),
                    targetPos.getZ() + 0.5,
                    world.random.nextFloat() * 360.0f,
                    0.0f
            );

            zombie.setBaby(true);

            world.spawnEntity(chicken);
            world.spawnEntity(zombie);

            zombie.startRiding(chicken, true);
        }
    }

    private static void skyTrial(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        if (world.getRegistryKey() != World.OVERWORLD) {
            return;
        }

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOW_FALLING,
                20 * 2,
                0
        ));

        player.getInventory().insertStack(new ItemStack(Items.WATER_BUCKET, 1));

        player.requestTeleport(
                player.getX(),
                255.0,
                player.getZ()
        );
    }

    private static void fallingAnvil(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        BlockPos playerPos = player.getBlockPos();
        int maxY = Math.min(255, world.getTopY() - 1);

        int startY = playerPos.getY() + 2;

        if (startY >= maxY) {
            return;
        }

        int highestAirY = -1;

        for (int y = startY; y <= maxY; y++) {
            BlockPos checkPos = new BlockPos(playerPos.getX(), y, playerPos.getZ());

            if (!world.getBlockState(checkPos).isAir()) {
                break;
            }

            highestAirY = y;
        }

        if (highestAirY < startY + 2) {
            return;
        }

        BlockPos anvilPos = new BlockPos(playerPos.getX(), highestAirY, playerPos.getZ());

        FallingBlockEntity.spawnFromBlock(
                world,
                anvilPos,
                Blocks.ANVIL.getDefaultState()
        );
    }

    private static void coinTossCursed(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        dropStack(world, pos, new ItemStack(ModItems.COIN_TOSS_CURSED, 1));
    }

    private static void shuffleInventory(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck) {
        PlayerInventory inventory = player.getInventory();

        for (int i = inventory.main.size() - 1; i > 0; i--) {
            int swapIndex = world.random.nextInt(i + 1);
            ItemStack current = inventory.main.get(i);

            inventory.main.set(i, inventory.main.get(swapIndex));
            inventory.main.set(swapIndex, current);
        }

        inventory.markDirty();
        player.currentScreenHandler.sendContentUpdates();

        world.spawnParticles(
                ParticleTypes.REVERSE_PORTAL,
                player.getX(),
                player.getBodyY(0.5),
                player.getZ(),
                35,
                0.45,
                0.7,
                0.45,
                0.08
        );

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS,
                0.8f,
                1.4f
        );
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static WeightedItem chooseWeightedItem(ServerWorld world, List<WeightedItem> items) {
        int totalWeight = 0;

        for (WeightedItem item : items) {
            totalWeight += item.weight();
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = world.random.nextInt(totalWeight);

        for (WeightedItem item : items) {
            roll -= item.weight();

            if (roll < 0) {
                return item;
            }
        }

        return null;
    }

    private static void addEnchantment(
            ServerWorld world,
            ItemStack stack,
            RegistryKey<Enchantment> enchantmentKey,
            int level
    ) {
        RegistryEntryLookup<Enchantment> enchantmentLookup =
                world.getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);

        enchantmentLookup.getOptional(enchantmentKey).ifPresent(enchantment ->
                stack.addEnchantment(enchantment, level)
        );
    }

    private static Optional<BlockPos> findNearbySafePosition(ServerWorld world, BlockPos center, int radius) {
        for (int attempt = 0; attempt < 60; attempt++) {
            int dx = world.random.nextBetween(-radius, radius);
            int dz = world.random.nextBetween(-radius, radius);
            int dy = world.random.nextBetween(-3, 3);

            BlockPos candidate = center.add(dx, dy, dz);

            if (isSafeStandingPosition(world, candidate)) {
                return Optional.of(candidate.toImmutable());
            }
        }

        return Optional.empty();
    }

    private static boolean isSafeStandingPosition(ServerWorld world, BlockPos pos) {
        if (pos.getY() <= world.getBottomY() + 1 || pos.getY() >= world.getTopY() - 2) {
            return false;
        }

        if (!world.getBlockState(pos).isAir()) {
            return false;
        }

        if (!world.getBlockState(pos.up()).isAir()) {
            return false;
        }

        if (!world.getFluidState(pos).isEmpty()) {
            return false;
        }

        if (!world.getFluidState(pos.up()).isEmpty()) {
            return false;
        }

        BlockPos floorPos = pos.down();

        return world.getBlockState(floorPos).isSideSolidFullSquare(
                world,
                floorPos,
                Direction.UP
        );
    }

    private static void dropStack(ServerWorld world, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemEntity itemEntity = new ItemEntity(
                world,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                stack
        );

        itemEntity.setToDefaultPickupDelay();
        world.spawnEntity(itemEntity);
    }

    private static void playOpeningFeedback(ServerWorld world, BlockPos pos) {
        world.spawnParticles(
                ParticleTypes.ENCHANT,
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                35,
                0.35,
                0.35,
                0.35,
                0.08
        );

        world.playSound(
                null,
                pos,
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.BLOCKS,
                0.8f,
                1.2f
        );
    }

    private enum LuckyEventCategory {
        POSITIVE,
        NEUTRAL,
        NEGATIVE
    }

    @FunctionalInterface
    private interface LuckyEventAction {
        void run(ServerWorld world, BlockPos pos, ServerPlayerEntity player, int luck);
    }

    private record WeightedLuckyEvent(int weight, LuckyEventAction event) {
    }

    private record WeightedItem(Item item, int weight) {
    }

    private record CurseEffect(
            RegistryEntry<StatusEffect> effect,
            int duration,
            int amplifier
    ) {
    }
}