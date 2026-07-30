package com.mythicrpg.mixin.woodcutting;

import com.mythicrpg.woodcutting.ChestModuleItem;
import com.mythicrpg.woodcutting.chest.ChestModuleManager;
import com.mythicrpg.woodcutting.chest.ChestModuleStorage;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin implements ChestModuleStorage {

    @Unique
    private static final String MYTHICRPG_EXTRA_KEY = "MythicRPGExtraStorage";
    @Unique
    private static final String MYTHICRPG_MODULE_KEY = "MythicRPGChestModule";

    @Unique
    private DefaultedList<ItemStack> mythicrpg$extraStacks = DefaultedList.ofSize(
            ChestModuleStorage.EXTRA_STORAGE_SIZE,
            ItemStack.EMPTY
    );

    @Unique
    private ItemStack mythicrpg$module = ItemStack.EMPTY;

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void mythicrpg$readModuleStorage(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registries,
            CallbackInfo ci
    ) {
        mythicrpg$extraStacks = DefaultedList.ofSize(
                ChestModuleStorage.EXTRA_STORAGE_SIZE,
                ItemStack.EMPTY
        );
        if (nbt.contains(MYTHICRPG_EXTRA_KEY, NbtElement.COMPOUND_TYPE)) {
            Inventories.readNbt(
                    nbt.getCompound(MYTHICRPG_EXTRA_KEY),
                    mythicrpg$extraStacks,
                    registries
            );
        }

        mythicrpg$module = ItemStack.EMPTY;
        if (nbt.contains(MYTHICRPG_MODULE_KEY, NbtElement.COMPOUND_TYPE)) {
            ItemStack decoded = ItemStack.fromNbtOrEmpty(
                    registries,
                    nbt.getCompound(MYTHICRPG_MODULE_KEY)
            );
            if (ChestModuleItem.isModule(decoded)) {
                mythicrpg$module = decoded.copyWithCount(1);
            }
        }
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void mythicrpg$writeModuleStorage(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registries,
            CallbackInfo ci
    ) {
        boolean hasExtraItems = false;
        for (ItemStack stack : mythicrpg$extraStacks) {
            if (!stack.isEmpty()) {
                hasExtraItems = true;
                break;
            }
        }

        if (hasExtraItems) {
            NbtCompound extraNbt = new NbtCompound();
            Inventories.writeNbt(extraNbt, mythicrpg$extraStacks, registries);
            nbt.put(MYTHICRPG_EXTRA_KEY, extraNbt);
        } else {
            nbt.remove(MYTHICRPG_EXTRA_KEY);
        }

        if (ChestModuleItem.isModule(mythicrpg$module)) {
            nbt.put(MYTHICRPG_MODULE_KEY, mythicrpg$module.encode(registries));
        } else {
            nbt.remove(MYTHICRPG_MODULE_KEY);
        }
    }

    /**
     * ChestBlockEntity periodically recomputes viewers, but vanilla only recognizes
     * GenericContainerScreenHandler. While a modular chest screen is active, keep
     * the already-correct open count and reschedule the normal safety recount.
     */
    @Inject(method = "onScheduledTick", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$keepLidOpenForModularScreen(CallbackInfo ci) {
        ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        if (!ChestModuleManager.hasActiveModularViewer(chest)
                || chest.getWorld() == null
                || chest.getWorld().isClient) {
            return;
        }

        chest.getWorld().scheduleBlockTick(
                chest.getPos(),
                chest.getCachedState().getBlock(),
                5
        );
        ci.cancel();
    }

    @Override
    public DefaultedList<ItemStack> mythicrpg$getExtraStacks() {
        return mythicrpg$extraStacks;
    }

    @Override
    public ItemStack mythicrpg$getModule() {
        return mythicrpg$module;
    }

    @Override
    public void mythicrpg$setModuleDirect(ItemStack stack) {
        mythicrpg$module = ChestModuleItem.isModule(stack)
                ? stack.copyWithCount(1)
                : ItemStack.EMPTY;
    }

    @Override
    public void mythicrpg$markModuleStorageDirty() {
        ((ChestBlockEntity) (Object) this).markDirty();
    }
}
