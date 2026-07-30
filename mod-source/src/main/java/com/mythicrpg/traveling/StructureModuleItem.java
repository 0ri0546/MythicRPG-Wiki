package com.mythicrpg.traveling;

import com.mythicrpg.core.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Optional;

public final class StructureModuleItem extends Item {

    private static final String MODULE_ID_KEY = "mythicrpg_structure_module";

    public StructureModuleItem(Settings settings) {
        super(settings);
    }

    public static ItemStack create(String moduleId) {
        ItemStack stack = new ItemStack(ModItems.STRUCTURE_MODULE);
        NbtCompound data = new NbtCompound();
        data.putString(MODULE_ID_KEY, moduleId);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
        return stack;
    }

    public static Optional<StructureModuleDefinition> getDefinition(ItemStack stack) {
        if (!stack.isOf(ModItems.STRUCTURE_MODULE)) {
            return Optional.empty();
        }

        NbtComponent data = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        );

        String moduleId = data.copyNbt().getString(MODULE_ID_KEY);
        return StructureModuleRegistry.get(moduleId);
    }

    public static boolean isValid(ItemStack stack) {
        return getDefinition(stack).isPresent();
    }

    public static boolean isOverworldModule(ItemStack stack) {
        return getDefinition(stack)
                .map(definition -> definition.realm() == StructureModuleDefinition.Realm.OVERWORLD)
                .orElse(false);
    }

    public static boolean isDimensionalModule(ItemStack stack) {
        return getDefinition(stack)
                .map(definition -> definition.realm() != StructureModuleDefinition.Realm.OVERWORLD)
                .orElse(false);
    }

    @Override
    public Text getName(ItemStack stack) {
        return getDefinition(stack)
                .<Text>map(definition -> Text.translatable(definition.translationKey())
                        .formatted(Formatting.AQUA))
                .orElseGet(() -> Text.translatable("item.mythicrpg.structure_module")
                        .formatted(Formatting.RED));
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        Optional<StructureModuleDefinition> definition = getDefinition(stack);

        if (definition.isEmpty()) {
            tooltip.add(Text.translatable("tooltip.mythicrpg.structure_module.invalid")
                    .formatted(Formatting.RED));
            return;
        }

        StructureModuleDefinition module = definition.get();

        tooltip.add(Text.translatable(
                        "tooltip.mythicrpg.structure_module.linked_structure",
                        Text.translatable(module.translationKey())
                )
                .formatted(Formatting.AQUA));
        tooltip.add(Text.translatable(module.realm().translationKey())
                .formatted(Formatting.DARK_AQUA));
        tooltip.add(Text.translatable("tooltip.mythicrpg.structure_module.use")
                .formatted(Formatting.GREEN));
    }
}
