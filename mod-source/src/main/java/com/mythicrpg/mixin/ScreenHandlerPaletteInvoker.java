package com.mythicrpg.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenHandler.class)
public interface ScreenHandlerPaletteInvoker {
    @Invoker("addSlot")
    Slot mythicrpg$addPaletteSlot(Slot slot);

    @Invoker("insertItem")
    boolean mythicrpg$insertPaletteItem(
            ItemStack stack,
            int startIndex,
            int endIndex,
            boolean fromLast
    );
}
