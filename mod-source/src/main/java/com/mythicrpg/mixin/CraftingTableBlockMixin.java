package com.mythicrpg.mixin;

import com.mythicrpg.crafting.MythicCraftingScreenHandler;
import com.mythicrpg.crafting.station.CraftingStationType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingTableBlock.class)
public abstract class CraftingTableBlockMixin {

    @Inject(method = "createScreenHandlerFactory", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$openDurableCraftingTable(
            BlockState state,
            World world,
            BlockPos pos,
            CallbackInfoReturnable<NamedScreenHandlerFactory> cir
    ) {
        if (!state.isOf(Blocks.CRAFTING_TABLE)) {
            return;
        }

        cir.setReturnValue(new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, player) -> new MythicCraftingScreenHandler(
                        syncId,
                        inventory,
                        ScreenHandlerContext.create(world, pos),
                        CraftingStationType.VANILLA_TABLE,
                        pos
                ),
                Text.translatable("container.crafting")
        ));
    }
}
