package com.mythicrpg.client.eating;

import com.mythicrpg.eating.SignatureDishData;
import com.mythicrpg.core.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

/**
 * Renders a signature dish with the ingredient icon chosen by its chef.
 *
 * <p>The outer item renderer has already applied the GUI/hand/ground transform before
 * invoking a dynamic renderer. Rendering the delegated icon with the same mode again
 * applies that transform twice, which previously pushed icons across inventory slots and
 * down to the player's feet. NONE keeps the icon in the already transformed local space.</p>
 */
public final class SignatureDishItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    @Override
    public void render(
            ItemStack stack,
            ModelTransformationMode mode,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        ItemStack icon = SignatureDishData.read(stack)
                .map(data -> new ItemStack(Registries.ITEM.get(data.icon())))
                .filter(candidate -> !candidate.isEmpty() && !candidate.isOf(ModItems.SIGNATURE_DISH))
                .orElseGet(() -> new ItemStack(Items.BOWL));
        if (stack.hasGlint()) {
            icon.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        matrices.push();
        client.getItemRenderer().renderItem(
                icon,
                ModelTransformationMode.NONE,
                light,
                overlay,
                matrices,
                vertexConsumers,
                client.world,
                0
        );
        matrices.pop();
    }
}
