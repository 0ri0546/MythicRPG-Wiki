package com.mythicrpg.fishing;

import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.SkillXpManager;
import com.mythicrpg.network.SeaMonsterStatePayload;
import com.mythicrpg.titles.TitleManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative legendary hunt loop.
 *
 * <p>The temporary encounter entity is deliberately a vanilla slime until dedicated models exist.
 * All combat state, loot ownership and boss bars remain in this manager.</p>
 */
public final class SeaMonsterManager {
    public static final int NORMAL_GAUGE_GAIN = 15;
    public static final int SEALED_GAUGE_GAIN = 20;

    private static final String ENTITY_TAG = "mythicrpg_sea_monster";
    private static final String BARON_PROMOTION_CHECKED_TAG = "mythicrpg_baron_promotion_checked";
    private static final double OWNER_MAX_DISTANCE = 80.0D;
    private static final double BOSS_BAR_DISTANCE = 56.0D;
    private static final int OWNER_XP = FishingRarity.MYTHIC.xp() * 25;
    private static final int ASSIST_XP = 250;
    private static final float BASE_HOOK_DAMAGE = 8.0F;
    private static final float SHARPNESS_DAMAGE_PER_LEVEL = 2.5F;

    private static final Map<UUID, ActiveEncounter> ACTIVE_BY_OWNER = new HashMap<>();
    private static final Map<UUID, ActiveEncounter> ACTIVE_BY_ENTITY = new HashMap<>();
    private static final Set<UUID> REDUCING_NESSIE_DAMAGE = new HashSet<>();
    private static final Map<UUID, Long> LAST_NESSIE_DURABILITY_TICK = new HashMap<>();
    private static final Map<UUID, Long> WHALE_FALL_PROTECTION_UNTIL = new HashMap<>();

    private SeaMonsterManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(SeaMonsterManager::tick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(SeaMonsterManager::allowDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) clearPlayer(player);
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof SlimeEntity
                    && entity.getCommandTags().contains(ENTITY_TAG)
                    && !ACTIVE_BY_ENTITY.containsKey(entity.getUuid())) {
                entity.discard();
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            ActiveEncounter active = ACTIVE_BY_ENTITY.remove(entity.getUuid());
            if (active != null) {
                ACTIVE_BY_OWNER.remove(active.ownerUuid);
                active.bossBar.clearPlayers();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(SeaMonsterManager::clearAll);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ACTIVE_BY_OWNER.clear();
            ACTIVE_BY_ENTITY.clear();
            REDUCING_NESSIE_DAMAGE.clear();
            LAST_NESSIE_DURABILITY_TICK.clear();
            WHALE_FALL_PROTECTION_UNTIL.clear();
        });
    }

    public static void onSuccessfulCatch(ServerPlayerEntity player, SeaMonsterHuntContext context) {
        if (context == null || context.gaugeGain() <= 0) return;

        SeaMonsterProgressData current = ModAttachments.getSeaMonsterProgress(player);
        SeaMonsterProgressData updated = current.addGauge(context.type(), context.gaugeGain());
        ModAttachments.setSeaMonsterProgress(player, updated);
        sendState(player);

        SeaMonsterProgressEntry entry = updated.get(context.type());
        player.sendMessage(
                Text.translatable(
                        "message.mythicrpg.sea_monster.gauge",
                        context.type().displayName(),
                        String.format(java.util.Locale.ROOT, "%.1f", entry.gauge() / 10.0D)
                ).formatted(Formatting.AQUA),
                true
        );

        if (entry.gauge() >= SeaMonsterProgressData.MAX_GAUGE
                && !ACTIVE_BY_OWNER.containsKey(player.getUuid())) {
            spawn(player, context.type(), context.spawnPos());
        }
    }

    public static void sendState(ServerPlayerEntity player) {
        SeaMonsterProgressData data = ModAttachments.getSeaMonsterProgress(player);
        ArrayList<Integer> gauges = new ArrayList<>(SeaMonsterType.values().length);
        ArrayList<Integer> victories = new ArrayList<>(SeaMonsterType.values().length);
        ArrayList<Long> firstDays = new ArrayList<>(SeaMonsterType.values().length);
        ArrayList<String> dimensions = new ArrayList<>(SeaMonsterType.values().length);
        for (SeaMonsterType type : SeaMonsterType.values()) {
            SeaMonsterProgressEntry entry = data.get(type);
            gauges.add(entry.gauge());
            victories.add(entry.victories());
            firstDays.add(entry.firstVictoryDay());
            dimensions.add(entry.firstVictoryDimension());
        }
        ServerPlayNetworking.send(player, new SeaMonsterStatePayload(gauges, victories, firstDays, dimensions));
    }

    public static boolean debugSpawnAtFullGauge(ServerPlayerEntity player, SeaMonsterType type) {
        if (player == null
                || type == null
                || ACTIVE_BY_OWNER.containsKey(player.getUuid())) {
            return false;
        }

        SeaMonsterProgressData current = ModAttachments.getSeaMonsterProgress(player);
        SeaMonsterProgressEntry currentEntry = current.get(type);

        int missingGauge = SeaMonsterProgressData.MAX_GAUGE - currentEntry.gauge();
        SeaMonsterProgressData updated = missingGauge > 0
                ? current.addGauge(type, missingGauge)
                : current;

        ModAttachments.setSeaMonsterProgress(player, updated);
        sendState(player);

        spawn(player, type, null);
        return ACTIVE_BY_OWNER.containsKey(player.getUuid());
    }

    public static void protectWhaleLaunch(ServerPlayerEntity player) {
        WHALE_FALL_PROTECTION_UNTIL.put(player.getUuid(), player.getWorld().getTime() + 20L * 12L);
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        ActiveEncounter active = ACTIVE_BY_OWNER.get(player.getUuid());
        if (active != null) abort(active, false);
        REDUCING_NESSIE_DAMAGE.remove(player.getUuid());
        LAST_NESSIE_DURABILITY_TICK.remove(player.getUuid());
        WHALE_FALL_PROTECTION_UNTIL.remove(player.getUuid());
    }

    private static void spawn(ServerPlayerEntity owner, SeaMonsterType type, BlockPos requestedPos) {
        ServerWorld world = owner.getServerWorld();
        SlimeEntity slime = EntityType.SLIME.create(world);
        if (slime == null) return;

        BlockPos position = requestedPos == null ? owner.getBlockPos().offset(owner.getHorizontalFacing(), 4) : requestedPos;
        slime.refreshPositionAndAngles(
                position.getX() + 0.5D,
                position.getY() + 1.5D,
                position.getZ() + 0.5D,
                owner.getYaw() + 180.0F,
                0.0F
        );
        slime.setSize(type.slimeSize(), true);
        if (slime.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH) != null) {
            slime.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(type.maxHealth());
        }
        if (slime.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE) != null) {
            slime.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(0.0D);
        }
        slime.setHealth(type.maxHealth());
        slime.setAiDisabled(true);
        slime.setNoGravity(true);
        slime.setPersistent();
        slime.setSilent(true);
        slime.setCustomName(type.displayName());
        slime.setCustomNameVisible(true);
        slime.addCommandTag(ENTITY_TAG);
        slime.addCommandTag(BARON_PROMOTION_CHECKED_TAG);

        ServerBossBar bossBar = new ServerBossBar(type.displayName(), type.bossBarColor(), BossBar.Style.PROGRESS);
        bossBar.setPercent(1.0F);
        bossBar.setDarkenSky(type == SeaMonsterType.MEGALODON);

        ActiveEncounter active = new ActiveEncounter(
                owner.getUuid(),
                type,
                slime,
                bossBar,
                slime.getPos(),
                world.getTime() + 40L
        );
        ACTIVE_BY_OWNER.put(owner.getUuid(), active);
        ACTIVE_BY_ENTITY.put(slime.getUuid(), active);

        if (!world.spawnEntity(slime)) {
            ACTIVE_BY_OWNER.remove(owner.getUuid());
            ACTIVE_BY_ENTITY.remove(slime.getUuid());
            bossBar.clearPlayers();
            return;
        }

        updateBossBarPlayers(active, world);
        world.playSound(null, slime.getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.2F, 0.75F);
        world.spawnParticles(ParticleTypes.SPLASH, slime.getX(), slime.getY() + 1.0D, slime.getZ(), 80, 2.5D, 1.2D, 2.5D, 0.25D);
        owner.sendMessage(Text.translatable("message.mythicrpg.sea_monster.appears", type.displayName()).formatted(Formatting.GOLD), false);
    }

    private static boolean allowDamage(net.minecraft.entity.LivingEntity entity, DamageSource source, float amount) {
        if (ACTIVE_BY_ENTITY.containsKey(entity.getUuid())) {
            // The visual slime is never damaged through Minecraft's normal damage pipeline.
            // Sea-monster health is reduced only by a modded fishing bobber impact.
            return false;
        }

        if (!(entity instanceof ServerPlayerEntity player)) return true;

        if (source.isOf(DamageTypes.FALL)) {
            Long until = WHALE_FALL_PROTECTION_UNTIL.get(player.getUuid());
            if (until != null) {
                WHALE_FALL_PROTECTION_UNTIL.remove(player.getUuid());
                if (player.getWorld().getTime() <= until) return false;
            }
        }

        UUID uuid = player.getUuid();
        if (REDUCING_NESSIE_DAMAGE.contains(uuid)
                || amount <= 0.0F
                || !player.getOffHandStack().isOf(ModItems.NESSIE_CHARM)
                || FishingWeatherManager.modeAt(player.getServerWorld(), player.getBlockPos()) == null) {
            return true;
        }

        REDUCING_NESSIE_DAMAGE.add(uuid);
        try {
            if (player.damage(source, amount * 0.75F)) {
                damageNessieCharm(player);
            }
        } finally {
            REDUCING_NESSIE_DAMAGE.remove(uuid);
        }
        return false;
    }

    public static boolean tryDamageWithBobber(
            FishingBobberEntity bobber,
            Entity hitEntity
    ) {
        if (!(bobber.getWorld() instanceof ServerWorld world)
                || !(bobber.getPlayerOwner() instanceof ServerPlayerEntity attacker)) {
            return false;
        }

        ActiveEncounter active = ACTIVE_BY_ENTITY.get(hitEntity.getUuid());
        if (active == null
                || hitEntity != active.entity
                || attacker.getWorld() != active.entity.getWorld()
                || !active.damagingBobbers.add(bobber.getUuid())) {
            return false;
        }

        ItemStack rod = findCombatRod(attacker);
        if (!isSeaMonsterCombatRod(rod)) {
            active.damagingBobbers.remove(bobber.getUuid());
            return false;
        }

        int sharpness = attacker.getServerWorld()
                .getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(Enchantments.SHARPNESS)
                .map(entry -> EnchantmentHelper.getLevel(entry, rod))
                .orElse(0);
        float damage = BASE_HOOK_DAMAGE + sharpness * SHARPNESS_DAMAGE_PER_LEVEL;

        active.contributors.add(attacker.getUuid());
        world.spawnParticles(
                ParticleTypes.CRIT,
                active.entity.getX(),
                active.entity.getBodyY(0.6D),
                active.entity.getZ(),
                12,
                0.7D,
                0.7D,
                0.7D,
                0.1D
        );
        world.playSound(
                null,
                active.entity.getBlockPos(),
                SoundEvents.ENTITY_SLIME_HURT,
                SoundCategory.HOSTILE,
                0.9F,
                0.65F
        );

        if (damage >= active.health) {
            // Never set the vanilla slime to zero health: an alive discard cannot split.
            complete(active);
            return true;
        }

        active.health -= damage;
        active.bossBar.setPercent(Math.max(0.0F, active.health / active.type.maxHealth()));
        return true;
    }

    private static ItemStack findCombatRod(ServerPlayerEntity player) {
        if (isSeaMonsterCombatRod(player.getMainHandStack())) {
            return player.getMainHandStack();
        }
        if (isSeaMonsterCombatRod(player.getOffHandStack())) {
            return player.getOffHandStack();
        }
        return ItemStack.EMPTY;
    }

    private static boolean isSeaMonsterCombatRod(ItemStack stack) {
        return stack.isOf(ModItems.MYTHIC_FISHING_ROD)
                || stack.isOf(ModItems.BASALT_FISHING_ROD)
                || stack.isOf(ModItems.VOID_FISHING_ROD);
    }

    private static void damageNessieCharm(ServerPlayerEntity player) {
        long now = player.getWorld().getTime();
        long previous = LAST_NESSIE_DURABILITY_TICK.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2L);
        if (now - previous < 10L) return;
        LAST_NESSIE_DURABILITY_TICK.put(player.getUuid(), now);
        player.getOffHandStack().damage(1, player, EquipmentSlot.OFFHAND);
    }

    private static void tick(MinecraftServer server) {
        for (ActiveEncounter active : new ArrayList<>(ACTIVE_BY_OWNER.values())) {
            if (ACTIVE_BY_OWNER.get(active.ownerUuid) != active) continue;
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(active.ownerUuid);
            if (owner == null
                    || !owner.isAlive()
                    || owner.getWorld() != active.entity.getWorld()
                    || owner.squaredDistanceTo(active.entity) > OWNER_MAX_DISTANCE * OWNER_MAX_DISTANCE
                    || !active.entity.isAlive()) {
                abort(active, owner != null);
                continue;
            }

            ServerWorld world = (ServerWorld) active.entity.getWorld();
            active.entity.setVelocity(Vec3d.ZERO);
            active.entity.velocityModified = true;
            if (active.entity.getPos().squaredDistanceTo(active.anchor) > 4.0D) {
                active.entity.requestTeleport(active.anchor.x, active.anchor.y, active.anchor.z);
            }

            long now = world.getTime();
            if (now % 20L == 0L) updateBossBarPlayers(active, world);
            if (now >= active.nextAttackTick) {
                performAreaAttack(active, world);
                active.nextAttackTick = now + active.type.attackIntervalTicks();
            }
        }

        WHALE_FALL_PROTECTION_UNTIL.entrySet().removeIf(entry -> {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            return player == null || player.getWorld().getTime() > entry.getValue();
        });
    }

    private static void updateBossBarPlayers(ActiveEncounter active, ServerWorld world) {
        double maxSq = BOSS_BAR_DISTANCE * BOSS_BAR_DISTANCE;
        Set<ServerPlayerEntity> nearby = new HashSet<>(world.getPlayers(candidate ->
                candidate.squaredDistanceTo(active.entity) <= maxSq));
        for (ServerPlayerEntity tracked : new ArrayList<>(active.bossBar.getPlayers())) {
            if (!nearby.contains(tracked)) active.bossBar.removePlayer(tracked);
        }
        for (ServerPlayerEntity player : nearby) active.bossBar.addPlayer(player);
    }

    private static void performAreaAttack(ActiveEncounter active, ServerWorld world) {
        double radiusSq = active.type.attackRadius() * active.type.attackRadius();
        for (ServerPlayerEntity player : world.getPlayers(candidate ->
                candidate.isAlive()
                        && !candidate.isSpectator()
                        && !candidate.getAbilities().creativeMode
                        && candidate.squaredDistanceTo(active.entity) <= radiusSq)) {
            player.damage(world.getDamageSources().mobAttack(active.entity), active.type.attackDamage());
            double dx = player.getX() - active.entity.getX();
            double dz = player.getZ() - active.entity.getZ();
            double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
            player.addVelocity(
                    dx / length * active.type.horizontalKnockback(),
                    active.type.verticalKnockback(),
                    dz / length * active.type.horizontalKnockback()
            );
            player.velocityModified = true;
        }

        switch (active.type) {
            case NESSIE -> {
                world.spawnParticles(ParticleTypes.BUBBLE, active.entity.getX(), active.entity.getY() + 1.0D, active.entity.getZ(), 45, 2.2D, 1.2D, 2.2D, 0.2D);
                world.playSound(null, active.entity.getBlockPos(), SoundEvents.ENTITY_DOLPHIN_SPLASH, SoundCategory.HOSTILE, 1.2F, 0.65F);
            }
            case MEGALODON -> {
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK, active.entity.getX(), active.entity.getY() + 1.0D, active.entity.getZ(), 18, 2.5D, 0.8D, 2.5D, 0.0D);
                world.playSound(null, active.entity.getBlockPos(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.1F, 0.75F);
            }
            case WHALE -> {
                world.spawnParticles(ParticleTypes.SPLASH, active.entity.getX(), active.entity.getY() + 2.0D, active.entity.getZ(), 70, 2.0D, 2.5D, 2.0D, 0.3D);
                world.playSound(null, active.entity.getBlockPos(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.HOSTILE, 1.3F, 0.55F);
            }
        }
    }

    private static void complete(ActiveEncounter active) {
        ServerWorld world = (ServerWorld) active.entity.getWorld();
        MinecraftServer server = world.getServer();
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(active.ownerUuid);
        removeActive(active, true);
        if (owner == null) return;

        give(owner, new ItemStack(active.type.material()));
        give(owner, new ItemStack(ModItems.BAIT_LEGENDARY));
        give(owner, new ItemStack(ModItems.BAIT_III, 1 + owner.getRandom().nextInt(2)));
        give(owner, new ItemStack(ModItems.BAIT_II, 2 + owner.getRandom().nextInt(3)));
        give(owner, new ItemStack(ModItems.BAIT_I, 4 + owner.getRandom().nextInt(5)));
        SkillXpManager.addXp(owner, SkillType.FISHING, OWNER_XP, false);
        TitleManager.grantSpecialTitle(owner, active.type.titleId(), true);

        SeaMonsterProgressData current = ModAttachments.getSeaMonsterProgress(owner);
        SeaMonsterProgressData updated = current.recordVictory(
                active.type,
                world.getTimeOfDay() / 24000L,
                world.getRegistryKey().getValue().toString()
        );
        ModAttachments.setSeaMonsterProgress(owner, updated);
        sendState(owner);

        for (UUID contributor : active.contributors) {
            if (contributor.equals(owner.getUuid())) continue;
            ServerPlayerEntity assistant = server.getPlayerManager().getPlayer(contributor);
            if (assistant != null) SkillXpManager.addXp(assistant, SkillType.FISHING, ASSIST_XP, false);
        }

        world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, active.entity.getX(), active.entity.getY() + 1.0D, active.entity.getZ(), 80, 2.2D, 1.5D, 2.2D, 0.2D);
        world.playSound(null, active.entity.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0F, 0.9F);
        owner.sendMessage(Text.translatable("message.mythicrpg.sea_monster.defeated", active.type.displayName()).formatted(Formatting.GOLD), false);
    }

    private static void abort(ActiveEncounter active, boolean notifyOwner) {
        MinecraftServer server = active.entity.getWorld().getServer();
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(active.ownerUuid);
        removeActive(active, true);
        if (notifyOwner && owner != null) {
            owner.sendMessage(Text.translatable("message.mythicrpg.sea_monster.retreat", active.type.displayName()).formatted(Formatting.GRAY), false);
        }
    }

    private static void removeActive(ActiveEncounter active, boolean discardEntity) {
        ACTIVE_BY_OWNER.remove(active.ownerUuid, active);
        ACTIVE_BY_ENTITY.remove(active.entity.getUuid(), active);
        active.bossBar.clearPlayers();
        if (discardEntity && !active.entity.isRemoved()) active.entity.discard();
    }

    private static void clearAll(MinecraftServer server) {
        for (ActiveEncounter active : new ArrayList<>(ACTIVE_BY_OWNER.values())) {
            removeActive(active, true);
        }
    }

    private static void give(ServerPlayerEntity player, ItemStack stack) {
        ItemStack remaining = stack.copy();
        player.getInventory().insertStack(remaining);
        if (!remaining.isEmpty()) {
            ItemEntity entity = new ItemEntity(
                    player.getWorld(),
                    player.getX(),
                    player.getY() + 0.5D,
                    player.getZ(),
                    remaining
            );
            entity.setOwner(player.getUuid());
            player.getWorld().spawnEntity(entity);
        }
        player.getInventory().markDirty();
    }

    private static final class ActiveEncounter {
        private final UUID ownerUuid;
        private final SeaMonsterType type;
        private final SlimeEntity entity;
        private final ServerBossBar bossBar;
        private final Vec3d anchor;
        private final Set<UUID> contributors = new HashSet<>();
        private final Set<UUID> damagingBobbers = new HashSet<>();
        private float health;
        private long nextAttackTick;

        private ActiveEncounter(
                UUID ownerUuid,
                SeaMonsterType type,
                SlimeEntity entity,
                ServerBossBar bossBar,
                Vec3d anchor,
                long nextAttackTick
        ) {
            this.ownerUuid = ownerUuid;
            this.type = type;
            this.entity = entity;
            this.bossBar = bossBar;
            this.anchor = anchor;
            this.health = type.maxHealth();
            this.nextAttackTick = nextAttackTick;
        }
    }
}
