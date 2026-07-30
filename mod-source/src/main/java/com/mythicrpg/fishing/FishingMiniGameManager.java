
package com.mythicrpg.fishing;

import com.mythicrpg.network.FishingMiniGameActionPayload;
import com.mythicrpg.network.FishingMiniGameOpenPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FishingMiniGameManager {
    private static final int GAME_PRECISION = 0;
    private static final int GAME_CARDS = 1;
    private static final int GAME_GRID = 2;

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private FishingMiniGameManager() {
    }

    public static void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                resolveInterrupted(newPlayer)
        );
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                resolveInterrupted(player);
            }
            PENDING.clear();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    public static void begin(
            ServerPlayerEntity player,
            FishingCatchData.Catch caught,
            ItemStack reward,
            boolean mastery,
            SeaMonsterHuntContext huntContext
    ) {
        Pending previous = PENDING.remove(player.getUuid());
        if (previous != null && previous.guaranteed) {
            FishingManager.grantCatch(player, previous.reward, previous.caught, previous.huntContext);
        }

        int gameType = switch (caught.rarity()) {
            case EPIC -> GAME_PRECISION;
            case LEGENDARY -> GAME_CARDS;
            case MYTHIC -> GAME_GRID;
            default -> GAME_PRECISION;
        };
        Pending pending = createChallenge(
                player,
                caught,
                reward.copy(),
                gameType,
                mastery,
                caught.rarity() == FishingRarity.MYTHIC,
                false,
                huntContext
        );
        PENDING.put(player.getUuid(), pending);
        send(player, pending);
    }

    public static void handle(
            ServerPlayerEntity player,
            FishingMiniGameActionPayload payload
    ) {
        Pending pending = PENDING.get(player.getUuid());
        if (pending == null) {
            return;
        }

        if (pending.gameType == GAME_CARDS
                && payload.action() == 1
                && pending.mastery
                && !pending.redrawn) {
            Pending redrawn = new Pending(
                    pending.caught,
                    pending.reward,
                    pending.gameType,
                    pending.a,
                    player.getRandom().nextInt(13) + 1,
                    pending.mastery,
                    pending.guaranteed,
                    true,
                    pending.huntContext
            );
            PENDING.put(player.getUuid(), redrawn);
            send(player, redrawn);
            return;
        }

        boolean success = switch (pending.gameType) {
            case GAME_PRECISION -> payload.action() == 0
                    && payload.value() >= 0
                    && payload.value() <= 100
                    && payload.value() >= pending.a
                    && payload.value() <= pending.b;
            case GAME_CARDS -> payload.action() == 0 && pending.b >= pending.a;
            case GAME_GRID -> gridSuccess(pending, payload);
            default -> false;
        };

        if (pending.guaranteed && !success) {
            send(player, pending);
            return;
        }

        PENDING.remove(player.getUuid());
        if (success) {
            FishingManager.grantCatch(player, pending.reward, pending.caught, pending.huntContext);
        } else {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.fishing.escaped")
                            .formatted(Formatting.RED),
                    false
            );
        }
    }

    public static void clear(ServerPlayerEntity player) {
        resolveInterrupted(player);
    }

    private static void resolveInterrupted(ServerPlayerEntity player) {
        Pending pending = PENDING.remove(player.getUuid());
        if (pending != null && pending.guaranteed) {
            FishingManager.grantCatch(player, pending.reward, pending.caught, pending.huntContext);
        }
    }

    private static Pending createChallenge(
            ServerPlayerEntity player,
            FishingCatchData.Catch caught,
            ItemStack reward,
            int gameType,
            boolean mastery,
            boolean guaranteed,
            boolean redrawn,
            SeaMonsterHuntContext huntContext
    ) {
        int a;
        int b;

        if (gameType == GAME_PRECISION) {
            int baseWidth = switch (caught.rarity()) {
                case EPIC -> 28;
                case LEGENDARY -> 18;
                case MYTHIC -> 14;
                default -> 30;
            };
            int width = Math.min(45, baseWidth + (mastery ? 10 : 0));
            a = player.getRandom().nextInt(101 - width);
            b = a + width;
        } else if (gameType == GAME_CARDS) {
            int minimumOpponent = switch (caught.rarity()) {
                case EPIC -> 1;
                case LEGENDARY -> 4;
                case MYTHIC -> 7;
                default -> 1;
            };
            a = minimumOpponent + player.getRandom().nextInt(14 - minimumOpponent);
            b = player.getRandom().nextInt(13) + 1;
        } else {
            int baseSize = 4;
            int size = Math.min(5, baseSize + (mastery ? 1 : 0));
            int targetCells = switch (caught.rarity()) {
                case EPIC -> 6;
                case LEGENDARY -> 8;
                case MYTHIC -> 10;
                default -> 6;
            };
            a = targetCells;
            b = size;
        }

        return new Pending(
                caught,
                reward,
                gameType,
                a,
                b,
                mastery,
                guaranteed,
                redrawn,
                huntContext
        );
    }

    private static boolean gridSuccess(
            Pending pending,
            FishingMiniGameActionPayload payload
    ) {
        if (payload.action() != 0) {
            return false;
        }

        int boardSize = pending.b;
        int cells = boardSize * boardSize;
        int validMask = cells >= 31 ? -1 : (1 << cells) - 1;
        int selected = payload.value() & validMask;
        if (payload.value() != selected || Integer.bitCount(selected) != pending.a) {
            return false;
        }

        GridShape[] shapes = switch (pending.caught.rarity()) {
            case EPIC -> new GridShape[]{GridShape.DOMINO, GridShape.DOMINO, GridShape.DOMINO};
            case LEGENDARY -> new GridShape[]{GridShape.DOMINO, GridShape.EL, GridShape.LINE};
            case MYTHIC -> new GridShape[]{
                    GridShape.DOMINO,
                    GridShape.DOMINO,
                    GridShape.EL,
                    GridShape.LINE
            };
            default -> new GridShape[0];
        };
        int[] budget = {20_000};
        return shapes.length > 0
                && canTile(selected, boardSize, shapes, 0, budget);
    }

    private static boolean canTile(
            int remaining,
            int boardSize,
            GridShape[] shapes,
            int usedShapes,
            int[] budget
    ) {
        if (--budget[0] < 0) {
            return false;
        }
        if (remaining == 0) {
            return usedShapes == (1 << shapes.length) - 1;
        }

        int targetCell = Integer.lowestOneBit(remaining);
        for (int shapeIndex = 0; shapeIndex < shapes.length; shapeIndex++) {
            int shapeBit = 1 << shapeIndex;
            if ((usedShapes & shapeBit) != 0) {
                continue;
            }

            GridShape shape = shapes[shapeIndex];
            for (int rotation = 0; rotation < 4; rotation++) {
                for (int y = 0; y < boardSize; y++) {
                    for (int x = 0; x < boardSize; x++) {
                        int placement = placementMask(shape, rotation, x, y, boardSize);
                        if (placement != 0
                                && (placement & targetCell) != 0
                                && (remaining & placement) == placement
                                && canTile(
                                        remaining ^ placement,
                                        boardSize,
                                        shapes,
                                        usedShapes | shapeBit,
                                        budget
                                )) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static int placementMask(
            GridShape shape,
            int rotation,
            int anchorX,
            int anchorY,
            int boardSize
    ) {
        int[][] source = switch (shape) {
            case DOMINO -> new int[][]{{0, 0}, {1, 0}};
            case EL -> new int[][]{{0, 0}, {0, 1}, {1, 1}};
            case LINE -> new int[][]{{0, 0}, {1, 0}, {2, 0}};
        };

        int[][] points = new int[source.length][2];
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int turns = Math.floorMod(rotation, 4);
        for (int index = 0; index < source.length; index++) {
            int x = source[index][0];
            int y = source[index][1];
            for (int turn = 0; turn < turns; turn++) {
                int oldX = x;
                x = -y;
                y = oldX;
            }
            points[index][0] = x;
            points[index][1] = y;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
        }

        int mask = 0;
        for (int[] point : points) {
            int x = anchorX + point[0] - minX;
            int y = anchorY + point[1] - minY;
            if (x < 0 || x >= boardSize || y < 0 || y >= boardSize) {
                return 0;
            }
            mask |= 1 << (y * boardSize + x);
        }
        return mask;
    }

    private static void send(ServerPlayerEntity player, Pending pending) {
        ServerPlayNetworking.send(
                player,
                new FishingMiniGameOpenPayload(
                        pending.gameType,
                        pending.a,
                        pending.b,
                        pending.mastery,
                        pending.guaranteed,
                        pending.caught.rarity().rank()
                )
        );
    }

    private enum GridShape {
        DOMINO,
        EL,
        LINE
    }

    private record Pending(
            FishingCatchData.Catch caught,
            ItemStack reward,
            int gameType,
            int a,
            int b,
            boolean mastery,
            boolean guaranteed,
            boolean redrawn,
            SeaMonsterHuntContext huntContext
    ) {
    }
}
