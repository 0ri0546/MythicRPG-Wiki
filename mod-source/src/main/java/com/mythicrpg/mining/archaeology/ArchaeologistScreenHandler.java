package com.mythicrpg.mining.archaeology;

import com.mythicrpg.crafting.ModScreenHandlers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

public final class ArchaeologistScreenHandler extends ScreenHandler {

    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;

    private static final int INTERNAL_SLOT_COUNT = 2;
    private static final int STATUS_PROPERTY = 0;
    private static final int PROPERTY_COUNT = 1;

    private static final int PLAYER_INVENTORY_START = INTERNAL_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory inventory;
    private final PropertyDelegate properties;
    private final int villagerEntityId;
    private boolean analysisRunning;

    public ArchaeologistScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(
                syncId,
                playerInventory,
                new SimpleInventory(INTERNAL_SLOT_COUNT),
                new ArrayPropertyDelegate(PROPERTY_COUNT),
                -1
        );
    }

    public ArchaeologistScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            int villagerEntityId
    ) {
        this(
                syncId,
                playerInventory,
                new SimpleInventory(INTERNAL_SLOT_COUNT),
                new ArrayPropertyDelegate(PROPERTY_COUNT),
                villagerEntityId
        );
    }

    private ArchaeologistScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory,
            PropertyDelegate properties,
            int villagerEntityId
    ) {
        super(ModScreenHandlers.ARCHAEOLOGIST, syncId);
        checkSize(inventory, INTERNAL_SLOT_COUNT);
        checkDataCount(properties, PROPERTY_COUNT);
        this.inventory = inventory;
        this.properties = properties;
        this.villagerEntityId = villagerEntityId;

        addSlot(new SkeletonSlot(inventory, INPUT_SLOT, 44, 32));
        addSlot(new ResultSlot(inventory, RESULT_SLOT, 116, 32));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }

        addProperties(properties);
    }

    public ArchaeologistInteractionManager.AnalysisStatus getStatus() {
        return ArchaeologistInteractionManager.AnalysisStatus.byId(properties.get(STATUS_PROPERTY));
    }

    public boolean canAnalyzeClient() {
        return !getStatus().isBusy()
                && inventory.getStack(INPUT_SLOT).getItem() instanceof FossilSkeletonItem
                && inventory.getStack(RESULT_SLOT).isEmpty();
    }

    ItemStack inputStack() {
        return inventory.getStack(INPUT_SLOT);
    }

    boolean isAnalysisRunningFor(UUID specimenId) {
        if (!analysisRunning) {
            return false;
        }
        return FossilSpecimenData.read(inventory.getStack(INPUT_SLOT))
                .map(specimen -> specimen.specimenId().equals(specimenId))
                .orElse(false);
    }

    void completeAnalysis(ArchaeologistInteractionManager.AnalysisResult result) {
        analysisRunning = false;
        properties.set(STATUS_PROPERTY, result.status().id());
        if (!result.dossier().isEmpty()) {
            inventory.setStack(RESULT_SLOT, result.dossier());
        }
        inventory.markDirty();
        sendContentUpdates();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id != 0 || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        if (analysisRunning) {
            return true;
        }
        ServerWorld world = serverPlayer.getServerWorld();

        ItemStack skeleton = inventory.getStack(INPUT_SLOT);
        if (!(skeleton.getItem() instanceof FossilSkeletonItem)
                || !inventory.getStack(RESULT_SLOT).isEmpty()) {
            properties.set(
                    STATUS_PROPERTY,
                    ArchaeologistInteractionManager.AnalysisStatus.INVALID_SPECIMEN.id()
            );
            sendContentUpdates();
            return true;
        }

        Entity entity = world.getEntityById(villagerEntityId);
        if (!(entity instanceof VillagerEntity villager)
                || villager.getVillagerData().getProfession() != ModVillagers.ARCHAEOLOGIST) {
            properties.set(
                    STATUS_PROPERTY,
                    ArchaeologistInteractionManager.AnalysisStatus.GENERATION_FAILED.id()
            );
            sendContentUpdates();
            return true;
        }

        ArchaeologistInteractionManager.AnalysisResult result =
                ArchaeologistInteractionManager.beginAnalysisForInterface(
                        serverPlayer,
                        world,
                        villager,
                        skeleton,
                        this
                );

        analysisRunning = result.status().isBusy();
        properties.set(STATUS_PROPERTY, result.status().id());
        if (!result.dossier().isEmpty()) {
            inventory.setStack(RESULT_SLOT, result.dossier());
        }
        inventory.markDirty();
        sendContentUpdates();
        return true;
    }

    @Override
    public void onContentChanged(Inventory changedInventory) {
        super.onContentChanged(changedInventory);
        if (!analysisRunning
                && changedInventory == inventory
                && inventory.getStack(RESULT_SLOT).isEmpty()) {
            properties.set(STATUS_PROPERTY, ArchaeologistInteractionManager.AnalysisStatus.IDLE.id());
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (villagerEntityId < 0 || !(player.getWorld() instanceof ServerWorld world)) {
            return true;
        }
        Entity entity = world.getEntityById(villagerEntityId);
        return entity instanceof VillagerEntity villager
                && villager.isAlive()
                && villager.getVillagerData().getProfession() == ModVillagers.ARCHAEOLOGIST
                && player.squaredDistanceTo(villager) <= 64.0D;
    }

    @Override
    public void onSlotClick(
            int slotIndex,
            int button,
            SlotActionType actionType,
            PlayerEntity player
    ) {
        if (getStatus().isBusy() && slotIndex == INPUT_SLOT) {
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        if (getStatus().isBusy() && slotIndex == INPUT_SLOT) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        if (slotIndex < INTERNAL_SLOT_COUNT) {
            if (!insertItem(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FossilSkeletonItem) {
            if (!insertItem(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex < PLAYER_INVENTORY_END) {
            if (!insertItem(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!insertItem(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return copy;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        ArchaeologistExpeditionManager.cancel(this);
        analysisRunning = false;
        super.onClosed(player);
        dropInventory(player, inventory);
    }

    private final class SkeletonSlot extends Slot {
        private SkeletonSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return stack.getItem() instanceof FossilSkeletonItem;
        }

        @Override
        public int getMaxItemCount(ItemStack stack) {
            return 1;
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return !getStatus().isBusy() && super.canTakeItems(playerEntity);
        }
    }

    private static final class ResultSlot extends Slot {
        private ResultSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }
}
