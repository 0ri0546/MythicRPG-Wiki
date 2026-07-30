package com.mythicrpg.fighting.barons;

import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BaronDeathMessageRegistry {
    private static final long RECENT_BARON_DAMAGE_TICKS = 20L * 12L;

    private static final Map<UUID, RecentBaronDamage> RECENT_DAMAGE_BY_PLAYER = new HashMap<>();

    private BaronDeathMessageRegistry() {
    }

    public static void rememberBaronDanger(LivingEntity victim, BaronType type) {
        if (!(victim instanceof ServerPlayerEntity player)) {
            return;
        }

        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }

        RECENT_DAMAGE_BY_PLAYER.put(player.getUuid(), new RecentBaronDamage(type, world.getTime()));
    }

    public static Text createDeathMessage(LivingEntity victim) {
        if (!(victim instanceof ServerPlayerEntity player)) {
            return null;
        }

        if (!(player.getWorld() instanceof ServerWorld world)) {
            return null;
        }

        RecentBaronDamage damage = RECENT_DAMAGE_BY_PLAYER.remove(player.getUuid());

        if (damage == null) {
            return null;
        }

        if (world.getTime() - damage.worldTime > RECENT_BARON_DAMAGE_TICKS) {
            return null;
        }

        return Text.translatable(getTranslationKey(damage.type), player.getDisplayName());
    }


    public static void clearPlayer(UUID playerUuid) {
        RECENT_DAMAGE_BY_PLAYER.remove(playerUuid);
    }

    public static void cleanupOldEntries(long currentTick) {
        RECENT_DAMAGE_BY_PLAYER.entrySet().removeIf(
                entry -> currentTick - entry.getValue().worldTime > RECENT_BARON_DAMAGE_TICKS
        );
    }

    public static void clearAll() {
        RECENT_DAMAGE_BY_PLAYER.clear();
    }

    public static String getTranslationKey(BaronType type) {
        return switch (type) {
            case DRUID -> "death.attack.mythicrpg.druid_baron";
            case BARRAGE -> "death.attack.mythicrpg.barrage_baron";
            case NUKE -> "death.attack.mythicrpg.nuke_baron";
            case SURVIVOR -> "death.attack.mythicrpg.survivor_baron";
            case FUGITIVE -> "death.attack.mythicrpg.fugitive_baron";
            case GOLDEN -> "death.attack.mythicrpg.golden_baron";
            case PANIC -> "death.attack.mythicrpg.panic_baron";
            case HOTHEAD -> "death.attack.mythicrpg.hothead_baron";
            case ALCHEMIST -> "death.attack.mythicrpg.alchemist_baron";
            case GIANT -> "death.attack.mythicrpg.giant_baron";
            case DARKNIGHT -> "death.attack.mythicrpg.darknight_baron";
            case SWIMMING -> "death.attack.mythicrpg.swimming_baron";
            case DROWNED_KING -> "death.attack.mythicrpg.drowned_king";
            case CHARGING -> "death.attack.mythicrpg.charging_baron";
            case BALLOON -> "death.attack.mythicrpg.balloon_baron";
            case DIAMOND -> "death.attack.mythicrpg.diamond_baron";
            case STALKER -> "death.attack.mythicrpg.stalker_baron";
            case HEAVY -> "death.attack.mythicrpg.heavy_baron";
            case MOLTEN -> "death.attack.mythicrpg.molten_baron";
            case RUNNER -> "death.attack.mythicrpg.runner_baron";
            case INK -> "death.attack.mythicrpg.ink_baron";
            case UNDYING_WOLF -> "death.attack.mythicrpg.undying_baron";
            case INFERNO -> "death.attack.mythicrpg.inferno_baron";
            case THROWER -> "death.attack.mythicrpg.thrower_baron";
            case NORMAL -> "death.attack.mythicrpg.normal_baron";
        };
    }

    private record RecentBaronDamage(BaronType type, long worldTime) {
    }
}
