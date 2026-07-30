package com.mythicrpg.mixin.woodcutting;

import com.mythicrpg.woodcutting.chest.ChestModuleManager;
import com.mythicrpg.woodcutting.chest.ChestModuleStorage;
import com.mythicrpg.woodcutting.chest.ModularChestInventory;
import com.mythicrpg.woodcutting.chest.ModularChestScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {

    @Inject(method = "getComparatorOutput", at = @At("RETURN"), cancellable = true)
    private void mythicrpg$calculateExpandedComparatorOutput(
            BlockState state,
            World world,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        ChestBlock self = (ChestBlock) (Object) this;
        Inventory vanillaInventory = ChestBlock.getInventory(self, state, world, pos, false);
        if (!ChestModuleManager.hasActiveModule(vanillaInventory)) {
            return;
        }
        ModularChestInventory inventory = ChestModuleManager.modular(vanillaInventory);
        if (inventory != null) {
            cir.setReturnValue(ScreenHandler.calculateComparatorOutput(inventory.activeView()));
        }
    }

    @Inject(method = "createScreenHandlerFactory", at = @At("RETURN"), cancellable = true)
    private void mythicrpg$createModularChestFactory(
            BlockState state,
            World world,
            BlockPos pos,
            CallbackInfoReturnable<NamedScreenHandlerFactory> cir
    ) {
        NamedScreenHandlerFactory vanillaFactory = cir.getReturnValue();
        if (vanillaFactory == null) {
            return;
        }

        ChestBlock self = (ChestBlock) (Object) this;
        ModularChestInventory inventory = ChestModuleManager.modular(
                ChestBlock.getInventory(self, state, world, pos, false)
        );
        if (inventory == null) {
            return;
        }

        cir.setReturnValue(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return vanillaFactory.getDisplayName();
            }

            @Override
            @Nullable
            public ScreenHandler createMenu(
                    int syncId,
                    PlayerInventory playerInventory,
                    PlayerEntity player
            ) {
                if (!inventory.prepareForPlayer(player)) {
                    return null;
                }
                return new ModularChestScreenHandler(syncId, playerInventory, inventory);
            }
        });
    }

    @Inject(method = "onStateReplaced", at = @At("HEAD"))
    private void mythicrpg$dropModuleStorage(
            BlockState state,
            World world,
            BlockPos pos,
            BlockState newState,
            boolean moved,
            CallbackInfo ci
    ) {
        if (world.isClient
                || state.isOf(newState.getBlock())
                || (!state.isOf(Blocks.CHEST) && !state.isOf(Blocks.TRAPPED_CHEST))) {
            return;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof ChestBlockEntity chest)
                || !(chest instanceof ChestModuleStorage storage)) {
            return;
        }

        SimpleInventory drops = new SimpleInventory(ChestModuleStorage.EXTRA_STORAGE_SIZE + 1);
        for (int index = 0; index < ChestModuleStorage.EXTRA_STORAGE_SIZE; index++) {
            ItemStack stack = storage.mythicrpg$getExtraStacks().get(index);
            if (!stack.isEmpty()) {
                drops.setStack(index, stack.copy());
                storage.mythicrpg$getExtraStacks().set(index, ItemStack.EMPTY);
            }
        }

        ItemStack module = storage.mythicrpg$getModule();
        if (!module.isEmpty()) {
            drops.setStack(ChestModuleStorage.EXTRA_STORAGE_SIZE, module.copy());
            storage.mythicrpg$setModuleDirect(ItemStack.EMPTY);
        }

        if (!drops.isEmpty()) {
            ItemScatterer.spawn(world, pos, drops);
            storage.mythicrpg$markModuleStorageDirty();
        }
    }
}
