package com.mythicrpg.traveling;

import com.mojang.datafixers.util.Pair;
import com.mythicrpg.core.BonusType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TravelingCompassManager {

    private static final int GENERIC_SEARCH_RADIUS_CHUNKS = 100;
    private static final int MODULE_SEARCH_RADIUS_CHUNKS = 512;
    private static final int NAVIGATION_UPDATE_INTERVAL_TICKS = 5;
    private static final long SEARCH_COOLDOWN_MILLIS = 2_000L;
    private static final int ARRIVAL_RADIUS_BLOCKS = 32;
    private static final double ARRIVAL_RADIUS_SQUARED = ARRIVAL_RADIUS_BLOCKS * ARRIVAL_RADIUS_BLOCKS;

    private static final Map<UUID, ActiveSearch> ACTIVE_SEARCHES = new HashMap<>();
    private static final Map<UUID, Long> SEARCH_COOLDOWN_UNTIL = new HashMap<>();

    private TravelingCompassManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TravelingCompassManager::tick);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerUuid = handler.player.getUuid();
            ACTIVE_SEARCHES.remove(playerUuid);
            SEARCH_COOLDOWN_UNTIL.remove(playerUuid);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            UUID playerUuid = newPlayer.getUuid();
            ACTIVE_SEARCHES.remove(playerUuid);
            SEARCH_COOLDOWN_UNTIL.remove(playerUuid);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ACTIVE_SEARCHES.clear();
            SEARCH_COOLDOWN_UNTIL.clear();
        });
    }

    public static boolean hasInterface(ServerPlayerEntity player) {
        return TravelingBonusCache.hasBonus(
                player,
                BonusType.MONUMENTAL_COMPASS_CRAFT
        );
    }

    public static boolean isSearching(UUID playerUuid) {
        return ACTIVE_SEARCHES.containsKey(playerUuid);
    }

    public static boolean startSearch(ServerPlayerEntity player, ItemStack moduleStack) {
        if (!hasInterface(player)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.compass.locked")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos().toImmutable();
        Optional<StructureModuleDefinition> module = StructureModuleItem.getDefinition(moduleStack);
        RegistryEntryList<Structure> structures;
        int searchRadius;

        if (!moduleStack.isEmpty() && module.isEmpty()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.compass.invalid_module")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        if (module.isPresent()) {
            StructureModuleDefinition definition = module.get();

            if (!TravelingBonusCache.hasBonus(
                    player,
                    definition.requiredBonus()
            )) {
                player.sendMessage(
                        Text.translatable("message.mythicrpg.compass.module_locked")
                                .formatted(Formatting.RED),
                        true
                );
                return false;
            }

            if (!definition.isUsableIn(world.getRegistryKey())) {
                player.sendMessage(
                        Text.translatable(
                                "message.mythicrpg.compass.wrong_dimension",
                                Text.translatable(definition.realm().translationKey())
                        ).formatted(Formatting.RED),
                        true
                );
                return false;
            }

            structures = StructureModuleRegistry.resolve(world, definition);
            searchRadius = MODULE_SEARCH_RADIUS_CHUNKS;
        } else {
            structures = StructureModuleRegistry.resolveAll(world);
            searchRadius = GENERIC_SEARCH_RADIUS_CHUNKS;
        }

        if (structures.size() == 0) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.compass.no_structures")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        UUID playerUuid = player.getUuid();
        long remainingCooldownMillis = getRemainingSearchCooldownMillis(playerUuid);
        if (remainingCooldownMillis > 0L) {
            long remainingSeconds = (remainingCooldownMillis + 999L) / 1_000L;
            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.compass.search_cooldown",
                            remainingSeconds
                    ).formatted(Formatting.RED),
                    true
            );
            return false;
        }

        Pair<BlockPos, RegistryEntry<Structure>> located = world.getChunkManager()
                .getChunkGenerator()
                .locateStructure(world, structures, origin, searchRadius, false);

        // Start the cooldown after the synchronous locate call completes so even a
        // slow search cannot be immediately repeated through Search / Stop spam.
        SEARCH_COOLDOWN_UNTIL.put(
                playerUuid,
                System.currentTimeMillis() + SEARCH_COOLDOWN_MILLIS
        );

        if (located == null || located.getSecond().getKey().isEmpty()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.compass.not_found")
                            .formatted(Formatting.RED),
                    true
            );
            return false;
        }

        RegistryKey<Structure> targetStructure = located.getSecond().getKey().orElseThrow();
        Text displayName = module
                .<Text>map(definition -> Text.translatable(definition.translationKey()))
                .orElseGet(() -> Text.translatable("structure.mythicrpg.unknown", targetStructure.getValue().toString()));

        ActiveSearch search = new ActiveSearch(
                world.getRegistryKey(),
                located.getFirst().toImmutable(),
                located.getSecond().value(),
                displayName
        );

        ACTIVE_SEARCHES.put(playerUuid, search);

        int distance = horizontalDistance(origin, search.target());
        player.sendMessage(
                Text.translatable(
                        "message.mythicrpg.compass.target_locked",
                        search.displayName(),
                        distance
                ).formatted(Formatting.AQUA),
                false
        );

        return true;
    }

    public static void stopSearch(ServerPlayerEntity player, boolean notify) {
        if (ACTIVE_SEARCHES.remove(player.getUuid()) == null) {
            return;
        }

        clearNavigationActionBar(player);

        if (notify) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.compass.stopped")
                            .formatted(Formatting.GRAY),
                    false
            );
        }
    }

    private static void tick(MinecraftServer server) {
        long tick = server.getOverworld().getTime();

        if (tick % NAVIGATION_UPDATE_INTERVAL_TICKS != 0L) {
            return;
        }

        boolean checkContainingStructure = tick % 20L == 0L;
        Iterator<Map.Entry<UUID, ActiveSearch>> iterator = ACTIVE_SEARCHES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveSearch> entry = iterator.next();
            UUID playerUuid = entry.getKey();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);

            if (player == null) {
                iterator.remove();
                continue;
            }

            if (!hasInterface(player)) {
                iterator.remove();
                clearNavigationActionBar(player);
                returnStoredModuleIfLocked(player);
                continue;
            }

            ActiveSearch search = entry.getValue();

            if (!player.getServerWorld().getRegistryKey().equals(search.dimension())) {
                iterator.remove();
                clearNavigationActionBar(player);
                player.sendMessage(
                        Text.translatable("message.mythicrpg.compass.dimension_changed")
                                .formatted(Formatting.RED),
                        false
                );
                continue;
            }

            if (hasArrived(player, search, checkContainingStructure)) {
                iterator.remove();
                clearNavigationActionBar(player);
                player.sendMessage(
                        Text.translatable(
                                "message.mythicrpg.compass.arrived",
                                search.displayName()
                        ).formatted(Formatting.GREEN),
                        false
                );
                continue;
            }

            sendNavigationActionBar(player, search);
        }

        if (checkContainingStructure) {
            TravelingCompassState state = TravelingCompassState.get(server);
            for (UUID playerUuid : state.getStoredPlayerUuidsSnapshot()) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
                if (player != null && !hasInterface(player)) {
                    returnStoredModuleIfLocked(player);
                }
            }
        }
    }

    private static long getRemainingSearchCooldownMillis(UUID playerUuid) {
        Long cooldownUntil = SEARCH_COOLDOWN_UNTIL.get(playerUuid);
        if (cooldownUntil == null) {
            return 0L;
        }

        long remaining = cooldownUntil - System.currentTimeMillis();
        if (remaining <= 0L) {
            SEARCH_COOLDOWN_UNTIL.remove(playerUuid);
            return 0L;
        }

        return remaining;
    }

    private static void clearNavigationActionBar(ServerPlayerEntity player) {
        player.sendMessage(Text.empty(), true);
    }

    private static void returnStoredModuleIfLocked(ServerPlayerEntity player) {
        TravelingCompassState state = TravelingCompassState.get(player.getServer());
        String moduleId = state.getModuleId(player.getUuid());

        if (moduleId.isEmpty()) {
            return;
        }

        ItemStack module = StructureModuleItem.create(moduleId);
        state.setModuleId(player.getUuid(), "");

        if (!player.getInventory().insertStack(module)) {
            player.dropItem(module, false);
        }

        player.sendMessage(
                Text.translatable("message.mythicrpg.compass.module_returned")
                        .formatted(Formatting.YELLOW),
                false
        );
    }

    private static boolean hasArrived(
            ServerPlayerEntity player,
            ActiveSearch search,
            boolean checkContainingStructure
    ) {
        BlockPos current = player.getBlockPos();
        double dx = current.getX() - search.target().getX();
        double dz = current.getZ() - search.target().getZ();

        if (dx * dx + dz * dz <= ARRIVAL_RADIUS_SQUARED) {
            return true;
        }

        if (!checkContainingStructure) {
            return false;
        }

        ServerWorld world = player.getServerWorld();
        StructureStart start = world.getStructureAccessor()
                .getStructureContaining(current, search.structure());

        return start != null && start.hasChildren();
    }

    private static void sendNavigationActionBar(
            ServerPlayerEntity player,
            ActiveSearch search
    ) {
        BlockPos target = search.target();
        double dx = target.getX() + 0.5D - player.getX();
        double dz = target.getZ() + 0.5D - player.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        float relativeYaw = MathHelper.wrapDegrees((float) targetYaw - player.getYaw());
        String arrow = arrowFor(relativeYaw);
        int distance = MathHelper.floor(Math.sqrt(dx * dx + dz * dz));

        MutableText message = search.displayName().copy()
                .formatted(Formatting.AQUA)
                .append(Text.literal("  "))
                .append(Text.literal(arrow).formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.translatable("message.mythicrpg.compass.distance", distance).formatted(Formatting.GRAY));

        player.sendMessage(message, true);
    }

    private static String arrowFor(float relativeYaw) {
        if (relativeYaw >= -22.5F && relativeYaw < 22.5F) {
            return "↑";
        }
        if (relativeYaw >= 22.5F && relativeYaw < 67.5F) {
            return "↗";
        }
        if (relativeYaw >= 67.5F && relativeYaw < 112.5F) {
            return "→";
        }
        if (relativeYaw >= 112.5F && relativeYaw < 157.5F) {
            return "↘";
        }
        if (relativeYaw >= 157.5F || relativeYaw < -157.5F) {
            return "↓";
        }
        if (relativeYaw >= -157.5F && relativeYaw < -112.5F) {
            return "↙";
        }
        if (relativeYaw >= -112.5F && relativeYaw < -67.5F) {
            return "←";
        }
        return "↖";
    }

    private static int horizontalDistance(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return MathHelper.floor(Math.sqrt(dx * dx + dz * dz));
    }


    private record ActiveSearch(
            RegistryKey<World> dimension,
            BlockPos target,
            Structure structure,
            Text displayName
    ) {
    }
}
