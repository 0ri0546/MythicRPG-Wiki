package com.mythicrpg.mixin;

import com.mythicrpg.mining.archaeology.relic.PaletteSelectionHolder;
import com.mythicrpg.mining.archaeology.relic.PaletteSelectionManager;
import com.mythicrpg.mining.archaeology.relic.PaletteSelectionState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public class PlayerInventoryPaletteMixin implements PaletteSelectionHolder {
    @Unique
    private final PaletteSelectionState mythicrpg$paletteSelectionState = new PaletteSelectionState();

    @Override
    public PaletteSelectionState mythicrpg$getPaletteSelectionState() {
        return mythicrpg$paletteSelectionState;
    }

    @Inject(method = "getMainHandStack", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$paletteMainHand(CallbackInfoReturnable<ItemStack> cir) {
        PlayerInventory inventory = (PlayerInventory) (Object) this;
        if (PaletteSelectionManager.selected(inventory.player) >= 0) {
            cir.setReturnValue(PaletteSelectionManager.handStack(inventory.player));
        }
    }
}
