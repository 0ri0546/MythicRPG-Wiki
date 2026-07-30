package com.mythicrpg.crafting.station;

import com.mythicrpg.crafting.PortableCraftingManager;
import com.mythicrpg.crafting.PortableCraftingState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CraftingStationDurabilityManager {

    private static final int DROP_FIX_TICKS = 3;
    private static final int PLACEMENT_FIX_TICKS = 3;

    private static final List<PendingTableDrop> PENDING_TABLE_DROPS = new ArrayList<>();
    private static final List<PendingTablePlacement> PENDING_TABLE_PLACEMENTS = new ArrayList<>();

    private CraftingStationDurabilityManager() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) {
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);

            if (!stack.isOf(Items.CRAFTING_TABLE)) {
                return ActionResult.PASS;
            }

            int durability = CraftingTableDurabilityItemData.getDurabilityOrDefault(stack);
            BlockPos clickedPos = hitResult.getBlockPos();
            BlockPos offsetPos = clickedPos.offset(hitResult.getSide());

            if (world instanceof ServerWorld serverWorld) {
                PENDING_TABLE_PLACEMENTS.add(new PendingTablePlacement(serverWorld, clickedPos, durability, PLACEMENT_FIX_TICKS));
                PENDING_TABLE_PLACEMENTS.add(new PendingTablePlacement(serverWorld, offsetPos, durability, PLACEMENT_FIX_TICKS));
            }

            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) {
                return;
            }

            if (!state.isOf(Blocks.CRAFTING_TABLE)) {
                return;
            }

            MinecraftServer server = world.getServer();

            if (server == null) {
                return;
            }

            CraftingTableDurabilityState tableState = CraftingTableDurabilityState.get(server);
            int durability = tableState.getDurability(world, pos);
            tableState.remove(world, pos);

            if (world instanceof ServerWorld serverWorld && !player.isCreative()) {
                PENDING_TABLE_DROPS.add(new PendingTableDrop(serverWorld, pos, durability, DROP_FIX_TICKS));
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            processPendingTablePlacements(server);
            processPendingTableDrops(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PENDING_TABLE_DROPS.clear();
            PENDING_TABLE_PLACEMENTS.clear();
        });
    }

    public static int getDurability(
            ServerPlayerEntity player,
            CraftingStationType type,
            World world,
            BlockPos pos
    ) {
        return switch (type) {
            case PORTABLE -> PortableCraftingManager.getDurability(player);
            case VANILLA_TABLE -> getVanillaTableDurability(player, world, pos);
            case INFINITE_TABLE -> getMaxDurability(type);
        };
    }

    public static int getMaxDurability(CraftingStationType type) {
        return switch (type) {
            case PORTABLE -> PortableCraftingState.MAX_DURABILITY;
            case VANILLA_TABLE -> CraftingTableDurabilityState.MAX_DURABILITY;
            case INFINITE_TABLE -> 0;
        };
    }

    public static boolean hasEnoughDurability(
            ServerPlayerEntity player,
            CraftingStationType type,
            World world,
            BlockPos pos,
            int amount
    ) {
        if (!type.hasFiniteDurability()) {
            return true;
        }

        return getDurability(player, type, world, pos) >= amount;
    }

    public static boolean tryConsume(
            ServerPlayerEntity player,
            CraftingStationType type,
            World world,
            BlockPos pos,
            int amount
    ) {
        if (amount <= 0) {
            return false;
        }

        if (!type.hasFiniteDurability()) {
            return true;
        }

        return switch (type) {
            case PORTABLE -> PortableCraftingManager.tryConsumeCharges(player, amount);
            case VANILLA_TABLE -> tryConsumeVanillaTable(player, world, pos, amount);
            case INFINITE_TABLE -> true;
        };
    }

    public static int repairVanillaTable(
            ServerPlayerEntity player,
            World world,
            BlockPos pos,
            double repairPower
    ) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return 0;
        }

        int amount = Math.max(
                1,
                (int) Math.ceil(CraftingTableDurabilityState.MAX_DURABILITY * repairPower)
        );

        return CraftingTableDurabilityState.get(server).repair(world, pos, amount);
    }

    public static int estimateShiftCrafts(RecipeInputInventory input, ItemStack result) {
        if (result.isEmpty()) {
            return 0;
        }

        int min = Integer.MAX_VALUE;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStack(i);

            if (stack.isEmpty()) {
                continue;
            }

            min = Math.min(min, stack.getCount());
        }

        return min == Integer.MAX_VALUE ? 0 : Math.max(1, min);
    }

    public static void sendBrokenMessage(ServerPlayerEntity player, CraftingStationType type) {
        player.sendMessage(
                Text.translatable(type == CraftingStationType.VANILLA_TABLE
                                ? "message.mythicrpg.crafting_station.table_broken"
                                : "message.mythicrpg.portable_crafting.broken")
                        .formatted(Formatting.RED),
                true
        );
    }

    public static void sendNotEnoughDurabilityMessage(
            ServerPlayerEntity player,
            CraftingStationType type,
            int current,
            int required
    ) {
        player.sendMessage(
                Text.translatable(type == CraftingStationType.VANILLA_TABLE
                                ? "message.mythicrpg.crafting_station.not_enough"
                                : "message.mythicrpg.portable_crafting.not_enough", current, required)
                        .formatted(Formatting.RED),
                true
        );
    }

    private static int getVanillaTableDurability(
            ServerPlayerEntity player,
            World world,
            BlockPos pos
    ) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return 0;
        }

        return CraftingTableDurabilityState.get(server).getDurability(world, pos);
    }

    private static boolean tryConsumeVanillaTable(
            ServerPlayerEntity player,
            World world,
            BlockPos pos,
            int amount
    ) {
        MinecraftServer server = player.getServer();

        if (server == null) {
            return false;
        }

        CraftingTableDurabilityState state = CraftingTableDurabilityState.get(server);
        int durability = state.getDurability(world, pos);

        if (durability < amount) {
            sendNotEnoughDurabilityMessage(player, CraftingStationType.VANILLA_TABLE, durability, amount);
            return false;
        }

        state.consume(world, pos, amount);
        return true;
    }

    private static void processPendingTablePlacements(MinecraftServer server) {
        Iterator<PendingTablePlacement> iterator = PENDING_TABLE_PLACEMENTS.iterator();

        while (iterator.hasNext()) {
            PendingTablePlacement pending = iterator.next();
            if (pending.world.getServer() != server) { iterator.remove(); continue; }

            if (pending.world.getBlockState(pending.pos).isOf(Blocks.CRAFTING_TABLE)) {
                CraftingTableDurabilityState.get(pending.world.getServer())
                        .setDurability(pending.world, pending.pos, pending.durability);
                iterator.remove();
                continue;
            }

            pending.ticksLeft--;

            if (pending.ticksLeft <= 0) {
                iterator.remove();
            }
        }
    }

    private static void processPendingTableDrops(MinecraftServer server) {
        Iterator<PendingTableDrop> iterator = PENDING_TABLE_DROPS.iterator();

        while (iterator.hasNext()) {
            PendingTableDrop pending = iterator.next();
            if (pending.world.getServer() != server) { iterator.remove(); continue; }

            if (applyDurabilityToDroppedTable(pending.world, pending.pos, pending.durability)) {
                iterator.remove();
                continue;
            }

            pending.ticksLeft--;

            if (pending.ticksLeft <= 0) {
                iterator.remove();
            }
        }
    }

    private static boolean applyDurabilityToDroppedTable(ServerWorld world, BlockPos pos, int durability) {
        if (durability >= CraftingTableDurabilityState.MAX_DURABILITY) {
            return true;
        }

        Box box = new Box(pos).expand(1.25);
        List<ItemEntity> itemEntities = world.getEntitiesByClass(
                ItemEntity.class,
                box,
                entity -> entity.getStack().isOf(Items.CRAFTING_TABLE)
                        && !CraftingTableDurabilityItemData.hasDurability(entity.getStack())
        );

        if (itemEntities.isEmpty()) {
            return false;
        }

        ItemEntity entity = itemEntities.get(0);
        ItemStack stack = entity.getStack();

        if (stack.getCount() <= 1) {
            CraftingTableDurabilityItemData.setDurability(stack, durability);
            entity.setStack(stack);
            return true;
        }

        stack.decrement(1);
        entity.setStack(stack);

        ItemStack durableStack = CraftingTableDurabilityItemData.createStackWithDurability(durability);
        ItemEntity durableEntity = new ItemEntity(
                world,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                durableStack
        );
        durableEntity.setVelocity(entity.getVelocity());
        world.spawnEntity(durableEntity);
        return true;
    }

    private static final class PendingTableDrop {
        private final ServerWorld world;
        private final BlockPos pos;
        private final int durability;
        private int ticksLeft;

        private PendingTableDrop(ServerWorld world, BlockPos pos, int durability, int ticksLeft) {
            this.world = world;
            this.pos = pos.toImmutable();
            this.durability = durability;
            this.ticksLeft = ticksLeft;
        }
    }

    private static final class PendingTablePlacement {
        private final ServerWorld world;
        private final BlockPos pos;
        private final int durability;
        private int ticksLeft;

        private PendingTablePlacement(ServerWorld world, BlockPos pos, int durability, int ticksLeft) {
            this.world = world;
            this.pos = pos.toImmutable();
            this.durability = durability;
            this.ticksLeft = ticksLeft;
        }
    }
}
