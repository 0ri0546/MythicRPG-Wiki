package com.mythicrpg.crafting;

import com.mythicrpg.core.ModBlocks;
import com.mythicrpg.crafting.station.CraftingStationDurabilityManager;
import com.mythicrpg.crafting.station.CraftingStationType;
import com.mythicrpg.mixin.CraftingScreenHandlerInvoker;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class MythicCraftingScreenHandler extends ScreenHandler {

    public static final int PROPERTY_COUNT = 6;
    public static final int PROPERTY_STATION_TYPE = 0;
    public static final int PROPERTY_STATION_DURABILITY = 1;
    public static final int PROPERTY_STATION_MAX_DURABILITY = 2;
    public static final int PROPERTY_CRAFT_CHARGE_PERCENT = 3;
    public static final int PROPERTY_TRANSFORMATION_UNLOCKED = 4;
    public static final int PROPERTY_STATION_FINITE = 5;

    private static final int CRAFT_RESULT_SLOT = 0;
    private static final int CRAFT_INPUT_START = 1;
    private static final int CRAFT_INPUT_END = 10;
    private static final int TRANSFORMATION_INPUT_SLOT = 10;
    private static final int TRANSFORMATION_OUTPUT_SLOT = 11;
    private static final int PLAYER_INVENTORY_START = 12;
    private static final int PLAYER_INVENTORY_END = 39;
    private static final int HOTBAR_START = 39;
    private static final int HOTBAR_END = 48;

    private final PlayerEntity player;
    private final ScreenHandlerContext context;
    private final CraftingStationType stationType;
    private final BlockPos stationPos;
    private final PropertyDelegate properties;

    private final CraftingInventory craftingInput = new CraftingInventory(this, 3, 3);
    private final CraftingResultInventory craftingResult = new CraftingResultInventory();
    private final SimpleInventory transformationInput = new SimpleInventory(1);
    private final SimpleInventory transformationOutput = new SimpleInventory(1);

    public MythicCraftingScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(
                syncId,
                playerInventory,
                ScreenHandlerContext.EMPTY,
                CraftingStationType.PORTABLE,
                BlockPos.ORIGIN,
                new ArrayPropertyDelegate(PROPERTY_COUNT)
        );
    }

    public MythicCraftingScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            ScreenHandlerContext context,
            CraftingStationType stationType,
            BlockPos stationPos
    ) {
        this(
                syncId,
                playerInventory,
                context,
                stationType,
                stationPos,
                new ArrayPropertyDelegate(PROPERTY_COUNT)
        );
    }

    private MythicCraftingScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            ScreenHandlerContext context,
            CraftingStationType stationType,
            BlockPos stationPos,
            PropertyDelegate properties
    ) {
        super(ModScreenHandlers.MYTHIC_CRAFTING, syncId);
        this.player = playerInventory.player;
        this.context = context;
        this.stationType = stationType;
        this.stationPos = stationPos;
        this.properties = properties;

        addCraftingSlots();
        addTransformationSlots();
        addPlayerInventorySlots(playerInventory);

        addProperties(properties);
        refreshProperties();
    }

    public static void openPortable(ServerPlayerEntity player) {
        if (!PortableCraftingManager.hasPortableCrafting(player)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.perk_required", Text.translatable("skill_tree.mythicrpg.crafting.1.name"))
                            .formatted(Formatting.RED),
                    true
            );
            return;
        }

        player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                (syncId, inventory, p) -> new MythicCraftingScreenHandler(
                        syncId,
                        inventory,
                        ScreenHandlerContext.create(p.getWorld(), p.getBlockPos()),
                        CraftingStationType.PORTABLE,
                        p.getBlockPos()
                ),
                Text.translatable("screen.mythicrpg.crafting")
        ));
    }

    public CraftingStationType getClientStationType() {
        return CraftingStationType.byId(properties.get(PROPERTY_STATION_TYPE));
    }

    public int getStationDurability() {
        return properties.get(PROPERTY_STATION_DURABILITY);
    }

    public int getStationMaxDurability() {
        return properties.get(PROPERTY_STATION_MAX_DURABILITY);
    }

    public int getCraftChargePercent() {
        return properties.get(PROPERTY_CRAFT_CHARGE_PERCENT);
    }

    public boolean isTransformationUnlocked() {
        return properties.get(PROPERTY_TRANSFORMATION_UNLOCKED) > 0;
    }

    public boolean stationHasFiniteDurability() {
        return properties.get(PROPERTY_STATION_FINITE) > 0;
    }

    public boolean canTakeCraftingResult(ServerPlayerEntity player, ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }

        if (!stationType.hasFiniteDurability()) {
            return true;
        }

        int durability = getServerStationDurability(player);

        if (durability <= 0) {
            CraftingStationDurabilityManager.sendBrokenMessage(player, stationType);
            return false;
        }

        return true;
    }

    public boolean canQuickMoveCraftingResult(ServerPlayerEntity player, ItemStack result) {
        if (!canTakeCraftingResult(player, result)) {
            return false;
        }

        if (!stationType.hasFiniteDurability()) {
            return true;
        }

        int required = CraftingStationDurabilityManager.estimateShiftCrafts(craftingInput, result);
        int current = getServerStationDurability(player);

        if (required <= current) {
            return true;
        }

        CraftingStationDurabilityManager.sendNotEnoughDurabilityMessage(
                player,
                stationType,
                current,
                required
        );
        return false;
    }

    public boolean tryConsumeStationDurability(ServerPlayerEntity player, int amount) {
        boolean consumed = context.get((world, pos) -> CraftingStationDurabilityManager.tryConsume(
                player,
                stationType,
                world,
                resolveStationPos(pos),
                amount
        )).orElse(false);

        refreshProperties();
        return consumed;
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        if (inventory == craftingInput) {
            context.run((world, pos) -> CraftingScreenHandlerInvoker.mythicrpg$updateResult(
                    this,
                    world,
                    player,
                    craftingInput,
                    craftingResult,
                    null
            ));
            return;
        }

        if (inventory == transformationInput) {
            updateTransformationResult();
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return switch (stationType) {
            case PORTABLE -> player instanceof ServerPlayerEntity serverPlayer
                    ? PortableCraftingManager.hasPortableCrafting(serverPlayer)
                    : true;
            case VANILLA_TABLE -> canUse(context, player, Blocks.CRAFTING_TABLE);
            case INFINITE_TABLE -> canUse(context, player, ModBlocks.INFINITE_CRAFTING_TABLE);
        };
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        if (slotIndex == CRAFT_RESULT_SLOT) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(slotIndex);

        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getStack();
        ItemStack copy = original.copy();

        if (slotIndex == CRAFT_RESULT_SLOT) {
            if (player instanceof ServerPlayerEntity serverPlayer
                    && !canQuickMoveCraftingResult(serverPlayer, original)) {
                return ItemStack.EMPTY;
            }

            if (!insertItem(original, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, original);
            return copy;
        }

        if (slotIndex == TRANSFORMATION_OUTPUT_SLOT) {
            return ItemStack.EMPTY;
        }

        if (slotIndex >= CRAFT_INPUT_START && slotIndex < CRAFT_INPUT_END) {
            if (!insertItem(original, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex == TRANSFORMATION_INPUT_SLOT) {
            if (!insertItem(original, PLAYER_INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= PLAYER_INVENTORY_START && slotIndex < HOTBAR_END) {
            if (isTransformationUnlocked() && TransformationSlotManager.isTransformable(original)) {
                if (insertItem(original, TRANSFORMATION_INPUT_SLOT, TRANSFORMATION_INPUT_SLOT + 1, false)) {
                    return copy;
                }
            }

            if (!insertItem(original, CRAFT_INPUT_START, CRAFT_INPUT_END, false)) {
                if (slotIndex < PLAYER_INVENTORY_END) {
                    if (!insertItem(original, HOTBAR_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!insertItem(original, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return copy;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        dropInventory(player, craftingInput);
        dropInventory(player, transformationInput);
    }

    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return slot.inventory != craftingResult
                && slot.inventory != transformationOutput
                && super.canInsertIntoSlot(stack, slot);
    }

    @Override
    public void sendContentUpdates() {
        refreshProperties();
        super.sendContentUpdates();
    }

    private void addCraftingSlots() {
        addSlot(new CraftingResultSlot(player, craftingInput, craftingResult, 0, 124, 32));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(craftingInput, column + row * 3, 30 + column * 18, 24 + row * 18));
            }
        }
    }

    private void addTransformationSlots() {
        addSlot(new TransformationInputSlot(transformationInput, 0, 48, 92));
        addSlot(new TransformationOutputSlot(transformationOutput, 0, 112, 92));
    }

    private void addPlayerInventorySlots(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 156 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 214));
        }
    }

    private void updateTransformationResult() {
        ItemStack input = transformationInput.getStack(0);

        if (!(player instanceof ServerPlayerEntity serverPlayer)
                || !TransformationSlotManager.hasTransformationSlot(serverPlayer)
                || input.isEmpty()) {
            transformationOutput.setStack(0, ItemStack.EMPTY);
            sendContentUpdates();
            return;
        }

        Item outputItem = TransformationSlotManager.getOutputItem(input.getItem());

        if (outputItem == null) {
            transformationOutput.setStack(0, ItemStack.EMPTY);
            sendContentUpdates();
            return;
        }

        transformationOutput.setStack(0, new ItemStack(outputItem));
        sendContentUpdates();
    }

    private ItemStack quickMoveTransformationOutput(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ItemStack.EMPTY;
        }

        ItemStack input = transformationInput.getStack(0);
        ItemStack preview = transformationOutput.getStack(0);

        if (input.isEmpty() || preview.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!TransformationSlotManager.hasTransformationSlot(serverPlayer)) {
            return ItemStack.EMPTY;
        }

        int durability = getServerStationDurability(serverPlayer);
        int amount = stationType.hasFiniteDurability()
                ? Math.min(input.getCount(), durability)
                : input.getCount();

        if (amount <= 0) {
            CraftingStationDurabilityManager.sendBrokenMessage(serverPlayer, stationType);
            return ItemStack.EMPTY;
        }

        ItemStack output = preview.copyWithCount(amount);
        ItemStack remaining = output.copy();

        if (!insertItem(remaining, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
            return ItemStack.EMPTY;
        }

        int moved = output.getCount() - remaining.getCount();

        if (moved <= 0) {
            return ItemStack.EMPTY;
        }

        if (!tryConsumeStationDurability(serverPlayer, moved)) {
            return ItemStack.EMPTY;
        }

        input.decrement(moved);
        transformationInput.setStack(0, input.isEmpty() ? ItemStack.EMPTY : input);
        updateTransformationResult();
        return output.copyWithCount(moved);
    }

    private boolean canTakeTransformationOutput(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }

        if (!TransformationSlotManager.hasTransformationSlot(serverPlayer)) {
            return false;
        }

        if (transformationInput.getStack(0).isEmpty() || transformationOutput.getStack(0).isEmpty()) {
            return false;
        }

        if (!stationType.hasFiniteDurability()) {
            return true;
        }

        int durability = getServerStationDurability(serverPlayer);

        if (durability <= 0) {
            CraftingStationDurabilityManager.sendBrokenMessage(serverPlayer, stationType);
            return false;
        }

        return true;
    }

    private void onTakeTransformationOutput(PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        if (!tryConsumeStationDurability(serverPlayer, 1)) {
            return;
        }

        ItemStack input = transformationInput.getStack(0);
        input.decrement(1);
        transformationInput.setStack(0, input.isEmpty() ? ItemStack.EMPTY : input);
        updateTransformationResult();
    }

    private boolean canInsertTransformationInput(ItemStack stack) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }

        return TransformationSlotManager.hasTransformationSlot(serverPlayer)
                && TransformationSlotManager.isTransformable(stack);
    }

    private int getServerStationDurability(ServerPlayerEntity player) {
        return context.get((world, pos) -> CraftingStationDurabilityManager.getDurability(
                player,
                stationType,
                world,
                resolveStationPos(pos)
        )).orElse(0);
    }

    private BlockPos resolveStationPos(BlockPos contextPos) {
        return stationType == CraftingStationType.PORTABLE ? stationPos : contextPos;
    }

    private void refreshProperties() {
        properties.set(PROPERTY_STATION_TYPE, stationType.getId());
        properties.set(PROPERTY_STATION_MAX_DURABILITY, CraftingStationDurabilityManager.getMaxDurability(stationType));
        properties.set(PROPERTY_STATION_FINITE, stationType.hasFiniteDurability() ? 1 : 0);

        if (player instanceof ServerPlayerEntity serverPlayer) {
            properties.set(PROPERTY_STATION_DURABILITY, getServerStationDurability(serverPlayer));
            properties.set(PROPERTY_TRANSFORMATION_UNLOCKED,
                    TransformationSlotManager.hasTransformationSlot(serverPlayer) ? 1 : 0);

            if (serverPlayer.getServer() != null) {
                int charge = (int) Math.floor(
                        CraftChargeState.get(serverPlayer.getServer()).getCharge(serverPlayer.getUuid())
                );
                properties.set(PROPERTY_CRAFT_CHARGE_PERCENT, charge);
            }
        }
    }

    private class TransformationInputSlot extends Slot {
        TransformationInputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return canInsertTransformationInput(stack);
        }

        @Override
        public void markDirty() {
            super.markDirty();
            updateTransformationResult();
        }
    }

    private class TransformationOutputSlot extends Slot {
        TransformationOutputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return canTakeTransformationOutput(playerEntity);
        }

        @Override
        public void onTakeItem(PlayerEntity playerEntity, ItemStack stack) {
            onTakeTransformationOutput(playerEntity);
            super.onTakeItem(playerEntity, stack);
        }
    }
}
