package com.mythicrpg.client.mining.relic;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.ShieldEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

/**
 * Rendu builtin du bouclier vanilla pour l'Égide colossale.
 *
 * <p>Le modèle JSON {@code builtin/entity} ne produit aucune géométrie par lui-même.
 * Le bouclier vanilla est dessiné par {@code BuiltinModelItemRenderer}, mais celui-ci
 * ne reconnaît que {@code Items.SHIELD}. Une relique qui étend {@code ShieldItem}
 * doit donc enregistrer explicitement son propre renderer.</p>
 */
public final class ColossalAegisRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    private ShieldEntityModel shieldModel;

    private ShieldEntityModel model() {
        if (shieldModel == null) {
            shieldModel = new ShieldEntityModel(
                    MinecraftClient.getInstance()
                            .getEntityModelLoader()
                            .getModelPart(EntityModelLayers.SHIELD)
            );
        }
        return shieldModel;
    }

    @Override
    public void render(
            ItemStack stack,
            ModelTransformationMode mode,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        ShieldEntityModel model = model();
        SpriteIdentifier texture = ModelLoader.SHIELD_BASE_NO_PATTERN;

        matrices.push();
        matrices.scale(1.0F, -1.0F, -1.0F);

        VertexConsumer consumer = texture.getSprite().getTextureSpecificVertexConsumer(
                ItemRenderer.getDirectItemGlintConsumer(
                        vertexConsumers,
                        model.getLayer(texture.getAtlasId()),
                        true,
                        stack.hasGlint()
                )
        );

        model.getHandle().render(matrices, consumer, light, overlay);
        model.getPlate().render(matrices, consumer, light, overlay);
        matrices.pop();
    }
}
