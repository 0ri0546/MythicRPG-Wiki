package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.BuildingMiniatureData;
import com.mythicrpg.building.BuildingPlanUiActionPayload;
import com.mythicrpg.building.BuildingPlanUiStatePayload;
import com.mythicrpg.building.BuildingRotationAxis;
import com.mythicrpg.building.BuildingStructureRotation;
import com.mythicrpg.building.BuildingUiTool;
import com.mythicrpg.client.building.BuildingSelectionBoxClient;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.core.ModItems;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/** Vanilla-styled, server-authoritative workflow used to lock a 5x5x5 miniature project. */
public final class BuildingMiniatureScreen extends VanillaBuildingScreen {
    private static final int PANEL_WIDTH = 390;
    private static final int PANEL_HEIGHT = 232;
    private static final int PREVIEW_X_OFFSET = 10;
    private static final int PREVIEW_Y_OFFSET = 24;
    private static final int PREVIEW_WIDTH = 184;
    private static final int PREVIEW_HEIGHT = 180;
    private static final int RIGHT_X_OFFSET = 202;
    private static final int RIGHT_WIDTH = 178;
    private static final int DRAFT_SYNC_DELAY = 6;

    private final Hand hand;
    private String dimensionId;
    private BuildingStructureRotation rotation;
    private BlockPos initialFirst;
    private BlockPos initialSecond;
    private String statusKey;
    private boolean statusError;

    private BuildingCoordinateEditor firstEditor;
    private BuildingCoordinateEditor secondEditor;
    private BuildingRotationControls rotationControls;
    private ButtonWidget miniatureButton;
    private BuildingPreviewModel sourceModel = BuildingPreviewModel.EMPTY;
    private boolean previewValid;
    private boolean draftDirty;
    private int draftSyncCountdown;
    private boolean waitingForServer;
    private boolean finalized;
    private float automaticYaw;

    public BuildingMiniatureScreen(BuildingPlanUiStatePayload state) {
        super(Text.translatable("screen.mythicrpg.building_miniature_ui.title"), PANEL_WIDTH, PANEL_HEIGHT);
        this.hand = state.handId() == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        this.dimensionId = state.dimensionId();
        this.rotation = state.rotation();
        this.initialFirst = state.first();
        this.initialSecond = state.second();
        this.statusKey = state.messageKey();
        this.statusError = state.error();
        this.finalized = state.locked();
    }

    @Override
    protected void initBuildingScreen() {
        int rightX = panelX + RIGHT_X_OFFSET + 8;
        firstEditor = new BuildingCoordinateEditor(
                textRenderer,
                rightX,
                panelY + 31,
                38,
                Text.translatable("screen.mythicrpg.building_plan_ui.point_a"),
                initialFirst,
                this::addDrawableChild,
                this::onCoordinatesChanged
        );
        secondEditor = new BuildingCoordinateEditor(
                textRenderer,
                rightX,
                panelY + 75,
                38,
                Text.translatable("screen.mythicrpg.building_plan_ui.point_b"),
                initialSecond,
                this::addDrawableChild,
                this::onCoordinatesChanged
        );
        rotationControls = new BuildingRotationControls(
                rightX,
                panelY + 121,
                rotation,
                this::addDrawableChild,
                this::onRotationPressed
        );
        miniatureButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.mythicrpg.building_miniature_ui.miniaturize"),
                        button -> miniaturize()
                )
                .dimensions(rightX, panelY + 169, 166, 20)
                .build());
        refreshPreview(true);
    }

    @Override
    protected void renderBuildingContent(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        drawSection(
                context,
                panelX + RIGHT_X_OFFSET,
                panelY + 24,
                RIGHT_WIDTH,
                180,
                null
        );
        BuildingStructurePreviewRenderer.renderMiniature3D(
                context,
                sourceModel,
                rotation,
                panelX + PREVIEW_X_OFFSET,
                panelY + PREVIEW_Y_OFFSET,
                PREVIEW_WIDTH,
                PREVIEW_HEIGHT,
                automaticYaw
        );

        firstEditor.render(context, textRenderer);
        secondEditor.render(context, textRenderer);
        rotationControls.render(context, textRenderer);

        context.drawText(
                textRenderer,
                Text.translatable("screen.mythicrpg.building_miniature_ui.state_draft"),
                panelX + RIGHT_X_OFFSET + 8,
                panelY + 195,
                VanillaContainerUi.DISABLED_TEXT,
                false
        );

        if (!sourceModel.isEmpty()) {
            BuildingStructureRotation.Size rotatedSize = rotation.rotatedSize(
                    sourceModel.sizeX(),
                    sourceModel.sizeY(),
                    sourceModel.sizeZ()
            );
            context.drawText(
                    textRenderer,
                    Text.translatable(
                            "screen.mythicrpg.building_miniature_ui.info",
                            rotatedSize.x(),
                            rotatedSize.y(),
                            rotatedSize.z(),
                            sourceModel.blockCount()
                    ),
                    panelX + 12,
                    panelY + 207,
                    VanillaContainerUi.DISABLED_TEXT,
                    false
            );
        }

        if (!statusKey.isBlank()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.translatable(statusKey),
                    panelX + panelWidth / 2,
                    panelY + 219,
                    statusError ? 0xFFE05050 : 0xFF55AA55
            );
        }
    }

    @Override
    public void tick() {
        super.tick();
        automaticYaw = (automaticYaw + 0.35F) % 360.0F;
        if (!waitingForServer && draftDirty && previewValid && editorsContainCoordinates()) {
            if (draftSyncCountdown > 0) {
                draftSyncCountdown--;
            } else {
                draftDirty = false;
                sendAction(BuildingPlanUiActionPayload.SAVE_DRAFT);
            }
        }
        if (!isHeldToolValid() || !isCurrentDimension()) {
            close();
        }
    }

    public boolean accepts(BuildingPlanUiStatePayload state) {
        return state != null
                && state.toolId() == BuildingPlanUiStatePayload.TOOL_MINIATURE
                && state.handId() == (hand == Hand.MAIN_HAND ? 0 : 1);
    }

    public void acceptState(BuildingPlanUiStatePayload state) {
        if (!accepts(state)) {
            return;
        }
        waitingForServer = false;
        draftDirty = false;
        dimensionId = state.dimensionId();
        rotation = state.rotation();
        initialFirst = state.first();
        initialSecond = state.second();
        statusKey = state.messageKey();
        statusError = state.error();

        if (state.locked()) {
            finalized = true;
            BuildingSelectionBoxClient.clearUiSelection();
            close();
            return;
        }

        if (firstEditor != null) {
            firstEditor.setPosition(initialFirst);
            secondEditor.setPosition(initialSecond);
            rotationControls.setRotation(rotation);
        }
        refreshPreview(false);
    }

    @Override
    public void close() {
        if (!finalized && !waitingForServer && previewValid
                && editorsContainCoordinates() && isCurrentDimension()) {
            sendAction(BuildingPlanUiActionPayload.SAVE_DRAFT);
        }
        BuildingSelectionBoxClient.releaseUiOwnership();
        super.close();
    }

    @Override
    public void removed() {
        BuildingSelectionBoxClient.releaseUiOwnership();
        super.removed();
    }

    private void onCoordinatesChanged() {
        statusKey = "";
        statusError = false;
        refreshPreview(true);
        draftDirty = true;
        draftSyncCountdown = DRAFT_SYNC_DELAY;
    }

    private void onRotationPressed(BuildingRotationAxis axis) {
        rotation = rotationControls.rotation();
        BuildingUiSounds.rotate();
        statusKey = "";
        statusError = false;
        draftDirty = true;
        draftSyncCountdown = DRAFT_SYNC_DELAY;
        refreshPreview(false);
    }

    private void miniaturize() {
        if (waitingForServer || !previewValid || !editorsContainCoordinates()) {
            return;
        }
        waitingForServer = true;
        miniatureButton.active = false;
        rotationControls.setActive(false);
        firstEditor.setEditable(false);
        secondEditor.setEditable(false);
        statusKey = "screen.mythicrpg.building_miniature_ui.miniaturizing";
        statusError = false;
        sendAction(BuildingPlanUiActionPayload.MINIATURIZE);
    }

    private void refreshPreview(boolean updateWorldBox) {
        if (firstEditor != null && secondEditor != null
                && firstEditor.position().isPresent() && secondEditor.position().isPresent()
                && client != null && client.world != null) {
            BlockPos first = firstEditor.position().get();
            BlockPos second = secondEditor.position().get();
            BuildingClientPlanCapture.Result result = BuildingClientPlanCapture.captureMiniature(
                    client.world,
                    dimensionId,
                    first,
                    second,
                    5
            );
            sourceModel = result.model();
            previewValid = result.valid();
            if (!result.valid()) {
                statusKey = result.messageKey();
                statusError = true;
            } else if (statusError) {
                statusKey = "";
                statusError = false;
            }

            if (updateWorldBox) {
                BuildingSelectionBoxClient.setUiSelection(
                        BuildingUiTool.MINIATURE,
                        dimensionId,
                        first,
                        second,
                        previewValid
                );
            }
        } else {
            sourceModel = BuildingPreviewModel.EMPTY;
            previewValid = false;
            statusKey = "screen.mythicrpg.building_plan_ui.invalid_coordinates";
            statusError = true;
        }

        if (miniatureButton != null) {
            miniatureButton.active = previewValid && !waitingForServer;
        }
        if (rotationControls != null) {
            rotationControls.setActive(!waitingForServer);
        }
        if (firstEditor != null && !waitingForServer) {
            firstEditor.setEditable(true);
            secondEditor.setEditable(true);
        }
    }

    private boolean editorsContainCoordinates() {
        return firstEditor != null
                && secondEditor != null
                && firstEditor.position().isPresent()
                && secondEditor.position().isPresent();
    }

    private boolean isCurrentDimension() {
        return client != null
                && client.world != null
                && client.world.getRegistryKey().getValue().toString().equals(dimensionId);
    }

    private boolean isHeldToolValid() {
        ItemStack stack = heldStack();
        return stack.isOf(ModItems.BUILDING_MINIATURE_PROJECT)
                && BuildingMiniatureData.readProject(stack).isEmpty();
    }

    private void sendAction(int action) {
        BlockPos first = firstEditor == null
                ? initialFirst
                : firstEditor.position().orElse(initialFirst);
        BlockPos second = secondEditor == null
                ? initialSecond
                : secondEditor.position().orElse(initialSecond);
        if (first == null) {
            first = BlockPos.ORIGIN;
        }
        if (second == null) {
            second = first;
        }
        if (action == BuildingPlanUiActionPayload.SAVE_DRAFT) {
            initialFirst = first;
            initialSecond = second;
        }
        ClientPlayNetworking.send(new BuildingPlanUiActionPayload(
                BuildingPlanUiStatePayload.TOOL_MINIATURE,
                hand == Hand.MAIN_HAND ? 0 : 1,
                action,
                first.asLong(),
                second.asLong(),
                0,
                rotation.xQuarterTurns(),
                rotation.yQuarterTurns(),
                rotation.zQuarterTurns()
        ));
    }

    private ItemStack heldStack() {
        if (client == null || client.player == null) {
            return ItemStack.EMPTY;
        }
        return client.player.getStackInHand(hand);
    }
}
