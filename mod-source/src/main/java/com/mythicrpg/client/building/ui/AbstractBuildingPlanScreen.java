package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.BuildingPlanTransforms;
import com.mythicrpg.building.BuildingPlanUiActionPayload;
import com.mythicrpg.building.BuildingPlanUiStatePayload;
import com.mythicrpg.building.BuildingRotationAxis;
import com.mythicrpg.building.BuildingStructureRotation;
import com.mythicrpg.building.BuildingUiTool;
import com.mythicrpg.client.building.BuildingSelectionBoxClient;
import com.mythicrpg.client.ui.VanillaContainerUi;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Shared vanilla workflow for the 2D and 3D plan configuration screens. */
public abstract class AbstractBuildingPlanScreen extends VanillaBuildingScreen {
    private static final int PANEL_WIDTH = 390;
    private static final int PANEL_HEIGHT = 232;
    private static final int PREVIEW_X_OFFSET = 10;
    private static final int PREVIEW_Y_OFFSET = 24;
    private static final int PREVIEW_WIDTH = 184;
    private static final int PREVIEW_HEIGHT = 180;
    private static final int RIGHT_X_OFFSET = 202;
    private static final int RIGHT_WIDTH = 178;
    private static final int DRAFT_SYNC_DELAY = 6;

    protected final int toolId;
    protected final Hand hand;
    protected boolean locked;
    protected String dimensionId;
    protected Direction.Axis normalAxis;
    protected int maxSize;
    protected BuildingStructureRotation rotation;
    protected BuildingPreviewModel sourceModel = BuildingPreviewModel.EMPTY;
    protected boolean previewValid;
    protected String statusKey = "";
    protected boolean statusError;
    protected float automaticYaw;

    private BlockPos initialFirst;
    private BlockPos initialSecond;
    private boolean hasFirst;
    private boolean hasSecond;
    private BuildingCoordinateEditor firstEditor;
    private BuildingCoordinateEditor secondEditor;
    private BuildingRotationControls rotationControls;
    private ButtonWidget copyButton;
    private boolean draftDirty;
    private int draftSyncCountdown;
    private boolean waitingForServer;

    protected AbstractBuildingPlanScreen(Text title, BuildingPlanUiStatePayload state) {
        super(title, PANEL_WIDTH, PANEL_HEIGHT);
        this.toolId = state.toolId();
        this.hand = state.handId() == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        this.locked = state.locked();
        this.dimensionId = state.dimensionId();
        this.normalAxis = axis(state.normalAxisId());
        this.maxSize = state.maxSize();
        this.rotation = state.rotation();
        this.initialFirst = state.first();
        this.initialSecond = state.second();
        this.hasFirst = state.hasFirst();
        this.hasSecond = state.hasSecond();
        this.statusKey = state.messageKey();
        this.statusError = state.error();
    }

    @Override
    protected final void initBuildingScreen() {
        int rightX = panelX + RIGHT_X_OFFSET + 8;
        firstEditor = new BuildingCoordinateEditor(
                textRenderer,
                rightX,
                panelY + 31,
                38,
                Text.translatable("screen.mythicrpg.building_plan_ui.point_a"),
                hasFirst ? initialFirst : BlockPos.ORIGIN,
                this::addDrawableChild,
                this::onCoordinatesChanged
        );
        secondEditor = new BuildingCoordinateEditor(
                textRenderer,
                rightX,
                panelY + 75,
                38,
                Text.translatable("screen.mythicrpg.building_plan_ui.point_b"),
                hasSecond ? initialSecond : (hasFirst ? initialFirst : BlockPos.ORIGIN),
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
        copyButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable(locked
                                ? "screen.mythicrpg.building_plan_ui.locked"
                                : "screen.mythicrpg.building_plan_ui.copy"),
                        button -> copyPlan()
                )
                .dimensions(rightX, panelY + 169, 166, 20)
                .build());

        firstEditor.setEditable(!locked);
        secondEditor.setEditable(!locked);
        copyButton.active = !locked;
        if (locked) {
            sourceModel = readLockedModel(heldStack());
        }
        refreshPreview(false);
    }

    @Override
    protected final void renderBuildingContent(
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
        renderPlanPreview(
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

        String stateKey = locked
                ? "screen.mythicrpg.building_plan_ui.state_locked"
                : "screen.mythicrpg.building_plan_ui.state_draft";
        context.drawText(
                textRenderer,
                Text.translatable(stateKey),
                panelX + RIGHT_X_OFFSET + 8,
                panelY + 195,
                locked ? 0xFF55AA55 : VanillaContainerUi.DISABLED_TEXT,
                false
        );

        if (!sourceModel.isEmpty()) {
            BuildingPreviewModel rotated = sourceModel.rotated(rotation);
            context.drawText(
                    textRenderer,
                    Text.translatable(
                            "screen.mythicrpg.building_plan_ui.info",
                            rotated.sizeX(),
                            rotated.sizeY(),
                            rotated.sizeZ(),
                            sourceModel.blockCount()
                    ),
                    panelX + 12,
                    panelY + 207,
                    VanillaContainerUi.DISABLED_TEXT,
                    false
            );
        }

        if (!statusKey.isBlank()) {
            Text status = Text.translatable(statusKey);
            int color = statusError ? 0xFFE05050 : 0xFF55AA55;
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    status,
                    panelX + panelWidth / 2,
                    panelY + 219,
                    color
            );
        }
    }

    @Override
    public void tick() {
        super.tick();
        automaticYaw = (automaticYaw + 0.35F) % 360.0F;
        if (!locked && draftDirty && previewValid && editorsContainCoordinates()) {
            if (draftSyncCountdown > 0) {
                draftSyncCountdown--;
            } else {
                draftDirty = false;
                sendAction(BuildingPlanUiActionPayload.SAVE_DRAFT);
            }
        }
        if (!isHeldToolValid(heldStack()) || !isCurrentDimension()) {
            close();
        }
    }

    public final boolean accepts(BuildingPlanUiStatePayload state) {
        return state != null
                && state.toolId() == toolId
                && state.handId() == (hand == Hand.MAIN_HAND ? 0 : 1);
    }

    public final void acceptState(BuildingPlanUiStatePayload state) {
        if (!accepts(state)) {
            return;
        }
        boolean becameLocked = !locked && state.locked();
        locked = state.locked();
        dimensionId = state.dimensionId();
        normalAxis = axis(state.normalAxisId());
        maxSize = state.maxSize();
        rotation = state.rotation();
        statusKey = state.messageKey();
        statusError = state.error();
        waitingForServer = false;
        draftDirty = false;

        initialFirst = state.first();
        initialSecond = state.second();
        hasFirst = state.hasFirst();
        hasSecond = state.hasSecond();
        if (firstEditor != null) {
            firstEditor.setPosition(initialFirst);
            secondEditor.setPosition(initialSecond);
            firstEditor.setEditable(!locked);
            secondEditor.setEditable(!locked);
            rotationControls.setRotation(rotation);
            copyButton.setMessage(Text.translatable(locked
                    ? "screen.mythicrpg.building_plan_ui.locked"
                    : "screen.mythicrpg.building_plan_ui.copy"));
            copyButton.active = !locked;
        }

        if (locked) {
            if (!becameLocked || sourceModel.isEmpty()) {
                BuildingPreviewModel loaded = readLockedModel(heldStack());
                if (!loaded.isEmpty()) {
                    sourceModel = loaded;
                }
            }
            BuildingSelectionBoxClient.clearUiSelection();
        }
        refreshPreview(false);
    }

    @Override
    public void close() {
        if (!locked && !waitingForServer && previewValid
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

    protected abstract BuildingClientPlanCapture.Result captureDraft(
            BlockPos first,
            BlockPos second
    );

    protected abstract BuildingPreviewModel readLockedModel(ItemStack stack);

    protected abstract void renderPlanPreview(
            DrawContext context,
            BuildingPreviewModel model,
            BuildingStructureRotation rotation,
            int x,
            int y,
            int width,
            int height,
            float automaticYaw
    );

    protected abstract boolean isHeldToolValid(ItemStack stack);

    protected abstract BuildingUiTool uiTool();

    private void onCoordinatesChanged() {
        if (locked) {
            return;
        }
        statusKey = "";
        statusError = false;
        refreshPreview(true);
        draftDirty = true;
        draftSyncCountdown = DRAFT_SYNC_DELAY;
    }

    private void onRotationPressed(BuildingRotationAxis axis) {
        BuildingStructureRotation candidate = rotationControls.rotation();
        if (!sourceModel.isEmpty()
                && !BuildingPlanTransforms.canRotate(sourceModel.entries(), candidate)) {
            rotationControls.setRotation(rotation);
            statusKey = "screen.mythicrpg.building_plan_ui.rotation_unsupported";
            statusError = true;
            BuildingUiSounds.error();
            return;
        }

        rotation = candidate;
        BuildingUiSounds.rotate();
        statusKey = "";
        statusError = false;
        if (locked) {
            waitingForServer = true;
            rotationControls.setActive(false);
            sendAction(BuildingPlanUiActionPayload.SET_LOCKED_ROTATION);
        } else {
            draftDirty = true;
            draftSyncCountdown = DRAFT_SYNC_DELAY;
        }
        refreshPreview(false);
    }

    private void copyPlan() {
        if (locked || waitingForServer || !previewValid) {
            return;
        }
        waitingForServer = true;
        copyButton.active = false;
        rotationControls.setActive(false);
        statusKey = "screen.mythicrpg.building_plan_ui.copying";
        statusError = false;
        sendAction(BuildingPlanUiActionPayload.COPY);
    }

    private void refreshPreview(boolean updateWorldBox) {
        if (locked) {
            previewValid = !sourceModel.isEmpty()
                    && BuildingPlanTransforms.canRotate(sourceModel.entries(), rotation);
        } else if (firstEditor != null && secondEditor != null
                && firstEditor.position().isPresent() && secondEditor.position().isPresent()) {
            BuildingClientPlanCapture.Result result = captureDraft(
                    firstEditor.position().get(),
                    secondEditor.position().get()
            );
            sourceModel = result.model();
            previewValid = result.valid()
                    && BuildingPlanTransforms.canRotate(sourceModel.entries(), rotation);
            if (!result.valid()) {
                statusKey = result.messageKey();
                statusError = true;
            } else if (!previewValid) {
                statusKey = "screen.mythicrpg.building_plan_ui.rotation_unsupported";
                statusError = true;
            } else if (statusError) {
                statusKey = "";
                statusError = false;
            }

            if (updateWorldBox || !locked) {
                BuildingSelectionBoxClient.setUiSelection(
                        uiTool(),
                        dimensionId,
                        firstEditor.position().get(),
                        secondEditor.position().get(),
                        previewValid
                );
            }
        } else {
            sourceModel = BuildingPreviewModel.EMPTY;
            previewValid = false;
            statusKey = "screen.mythicrpg.building_plan_ui.invalid_coordinates";
            statusError = true;
        }

        if (copyButton != null) {
            copyButton.active = !locked && previewValid && !waitingForServer;
        }
        if (rotationControls != null) {
            rotationControls.setActive(!waitingForServer);
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
            hasFirst = true;
            hasSecond = true;
        }
        ClientPlayNetworking.send(new BuildingPlanUiActionPayload(
                toolId,
                hand == Hand.MAIN_HAND ? 0 : 1,
                action,
                first.asLong(),
                second.asLong(),
                axisId(normalAxis),
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

    private static Direction.Axis axis(int id) {
        return switch (Math.floorMod(id, 3)) {
            case 0 -> Direction.Axis.X;
            case 1 -> Direction.Axis.Y;
            default -> Direction.Axis.Z;
        };
    }

    private static int axisId(Direction.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }
}
