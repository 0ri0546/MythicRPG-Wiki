package com.mythicrpg.traveling;

import com.mythicrpg.crafting.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TravelingCompassScreenHandler extends ScreenHandler {

    private static final int PROPERTY_SEARCHING = 0;
    private static final int PROPERTY_COUNT = 1;

    private static final int MODULE_SLOT = 0;
    private static final int PLAYER_INVENTORY_START = 1;
    private static final int PLAYER_INVENTORY_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    private final PlayerEntity player;
    private final SimpleInventory moduleInventory;
    private final PropertyDelegate properties;

    public TravelingCompassScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(
                syncId,
                playerInventory,
                new SimpleInventory(1),
                new ArrayPropertyDelegate(PROPERTY_COUNT)
        );
    }

    private TravelingCompassScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            SimpleInventory moduleInventory,
            PropertyDelegate properties
    ) {
        super(ModScreenHandlers.TRAVELING_COMPASS, syncId);
        this.player = playerInventory.player;
        this.moduleInventory = moduleInventory;
        this.properties = properties;

        moduleInventory.onOpen(player);

        addSlot(new Slot(moduleInventory, 0, 30, 42) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return !isSearching() && StructureModuleItem.isValid(stack);
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerEntity) {
                return !isSearching();
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        addPlayerInventorySlots(playerInventory);
        addProperties(properties);
        refreshProperties();
    }

    public static void open(ServerPlayerEntity player) {
        if (!TravelingCompassManager.hasInterface(player)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.compass.locked")
                            .formatted(Formatting.RED),
                    true
            );
            return;
        }

        TravelingCompassState state = TravelingCompassState.get(player.getServer());
        CompassModuleInventory moduleInventory = new CompassModuleInventory(player, state);

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, owner) -> new TravelingCompassScreenHandler(
                        syncId,
                        inventory,
                        moduleInventory,
                        new ArrayPropertyDelegate(PROPERTY_COUNT)
                ),
                Text.translatable("screen.mythicrpg.monumental_compass")
        ));
    }

    public boolean isSearching() {
        return properties.get(PROPERTY_SEARCHING) > 0;
    }

    @Override
    public boolean onButtonClick(PlayerEntity playerEntity, int id) {
        if (id != 0 || !(playerEntity instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }

        if (TravelingCompassManager.isSearching(serverPlayer.getUuid())) {
            TravelingCompassManager.stopSearch(serverPlayer, true);
        } else {
            TravelingCompassManager.startSearch(
                    serverPlayer,
                    moduleInventory.getStack(MODULE_SLOT)
            );
        }

        refreshProperties();
        sendContentUpdates();
        return true;
    }

    @Override
    public void sendContentUpdates() {
        refreshProperties();
        super.sendContentUpdates();
    }

    @Override
    public boolean canUse(PlayerEntity playerEntity) {
        return playerEntity instanceof ServerPlayerEntity serverPlayer
                ? TravelingCompassManager.hasInterface(serverPlayer)
                : true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity playerEntity, int slotIndex) {
        Slot slot = slots.get(slotIndex);

        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getStack();
        ItemStack copy = original.copy();

        if (slotIndex == MODULE_SLOT) {
            if (isSearching()
                    || !insertItem(original, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= PLAYER_INVENTORY_START && slotIndex < HOTBAR_END) {
            if (!StructureModuleItem.isValid(original)
                    || isSearching()
                    || !insertItem(original, MODULE_SLOT, MODULE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        if (original.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTakeItem(playerEntity, original);
        return copy;
    }

    @Override
    public void onClosed(PlayerEntity playerEntity) {
        super.onClosed(playerEntity);
        moduleInventory.onClose(playerEntity);
    }

    private void addPlayerInventorySlots(PlayerInventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    private void refreshProperties() {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            properties.set(
                    PROPERTY_SEARCHING,
                    TravelingCompassManager.isSearching(serverPlayer.getUuid()) ? 1 : 0
            );
        }
    }

    private static final class CompassModuleInventory extends SimpleInventory {

        private final ServerPlayerEntity owner;
        private final TravelingCompassState state;
        private boolean loading;

        private CompassModuleInventory(
                ServerPlayerEntity owner,
                TravelingCompassState state
        ) {
            super(1);
            this.owner = owner;
            this.state = state;
            this.loading = true;

            String moduleId = state.getModuleId(owner.getUuid());
            if (!moduleId.isEmpty()) {
                setStack(0, StructureModuleItem.create(moduleId));
            }

            this.loading = false;
        }

        @Override
        public void markDirty() {
            super.markDirty();

            if (loading) {
                return;
            }

            String moduleId = StructureModuleItem.getDefinition(getStack(0))
                    .map(StructureModuleDefinition::id)
                    .orElse("");

            state.setModuleId(owner.getUuid(), moduleId);
        }
    }
}
