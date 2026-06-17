package dev.createautoflight.client;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import dev.createautoflight.content.gyroscope.GyroscopeBlockEntity;
import dev.createautoflight.network.ConfigureGyroscopePacket;
import dev.createautoflight.registry.ModBlocks;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GyroscopeScreen extends AbstractSimiScreen {
    private static final int PANEL_WIDTH = 150;
    private static final int LABEL_X = 8;
    private static final int INPUT_X = 72;
    private static final int INPUT_WIDTH = 70;
    private static final int ROW_HEIGHT = 18;
    private static final int FIRST_ROW_Y = 18;
    private static final int FOOTER_HEIGHT = 22;
    private static final int ROW_COUNT = 13;
    private static final int TITLE_COLOR = 0x592424;

    private static final Component ON = Component.literal("On");
    private static final Component OFF = Component.literal("Off");

    private final GyroscopeBlockEntity blockEntity;
    private final ItemStack renderedItem = new ItemStack(ModBlocks.GYROSCOPE.get());
    private final List<ScrollInput> inputs = new ArrayList<>();

    private ScrollInput modeInput;
    private ScrollInput autoStabilizeInput;
    private ScrollInput forceInput;
    private ScrollInput dampingInput;
    private ScrollInput acceptAngleInput;
    private ScrollInput pitchInput;
    private ScrollInput yawInput;
    private ScrollInput rollInput;
    private ScrollInput bidirectionalInput;
    private ScrollInput downFaceInput;
    private ScrollInput targetPitchInput;
    private ScrollInput targetYawInput;
    private ScrollInput targetRollInput;

    private int panelHeight;
    private int lastModification = -1;

    public GyroscopeScreen(GyroscopeBlockEntity blockEntity) {
        super(Component.literal("Gyroscope Settings"));
        this.blockEntity = blockEntity;
    }

    @Override
    protected void init() {
        int topHeight = AllGuiTextures.STATION_TEXTBOX_TOP.getHeight();
        int bottomHeight = AllGuiTextures.STATION_TEXTBOX_BOTTOM.getHeight();
        panelHeight = topHeight + ROW_COUNT * ROW_HEIGHT + bottomHeight + FOOTER_HEIGHT;

        setWindowSize(PANEL_WIDTH, panelHeight);
        setWindowOffset(-20, 0);
        super.init();

        inputs.clear();
        clearWidgets();

        int rowY = guiTop + FIRST_ROW_Y;

        modeInput = addSelectionRow(rowY,
                Component.literal("Mode"),
                List.of(Component.literal("Auto"), Component.literal("Manual")),
                blockEntity.getMode() == GyroscopeBlockEntity.GyroMode.AUTO ? 0 : 1);
        rowY += ROW_HEIGHT;

        autoStabilizeInput = addSelectionRow(rowY,
                Component.literal("Auto-Stabilize"),
                List.of(ON, OFF),
                blockEntity.isAutoStabilize() ? 0 : 1);
        rowY += ROW_HEIGHT;

        forceInput = addScrollRow(rowY,
                Component.literal("Force"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(0, 101)
                        .format(v -> Component.literal(v + "%"))
                        .setState(blockEntity.getForcePercent()));
        rowY += ROW_HEIGHT;

        dampingInput = addScrollRow(rowY,
                Component.literal("Damping"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(0, GyroscopeBlockEntity.MAX_DAMPING_PERCENT + 1)
                        .format(v -> Component.literal(v + "%"))
                        .setState(Math.min(blockEntity.getDampingPercent(), GyroscopeBlockEntity.MAX_DAMPING_PERCENT)));
        rowY += ROW_HEIGHT;

        acceptAngleInput = addScrollRow(rowY,
                Component.literal("Accept Angle"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(0, 46)
                        .format(v -> Component.literal(v + "\u00b0"))
                        .setState(blockEntity.getAcceptAngleDeg()));
        rowY += ROW_HEIGHT;

        targetPitchInput = addScrollRow(rowY,
                Component.literal("Target Pitch"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(-90, 91)
                        .format(v -> Component.literal(v + "\u00b0"))
                        .setState(blockEntity.getTargetPitchDeg()));
        rowY += ROW_HEIGHT;

        targetYawInput = addScrollRow(rowY,
                Component.literal("Target Yaw"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(-180, 181)
                        .format(v -> Component.literal(v + "\u00b0"))
                        .setState(blockEntity.getTargetYawDeg()));
        rowY += ROW_HEIGHT;

        targetRollInput = addScrollRow(rowY,
                Component.literal("Target Roll"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(-90, 91)
                        .format(v -> Component.literal(v + "\u00b0"))
                        .setState(blockEntity.getTargetRollDeg()));
        rowY += ROW_HEIGHT;

        pitchInput = addSelectionRow(rowY,
                Component.literal("Pitch Axis"),
                List.of(ON, OFF),
                blockEntity.isStabilizePitch() ? 0 : 1);
        rowY += ROW_HEIGHT;

        yawInput = addSelectionRow(rowY,
                Component.literal("Yaw Axis"),
                List.of(ON, OFF),
                blockEntity.isStabilizeYaw() ? 0 : 1);
        rowY += ROW_HEIGHT;

        rollInput = addSelectionRow(rowY,
                Component.literal("Roll Axis"),
                List.of(ON, OFF),
                blockEntity.isStabilizeRoll() ? 0 : 1);
        rowY += ROW_HEIGHT;

        bidirectionalInput = addSelectionRow(rowY,
                Component.literal("Torque Direction"),
                List.of(Component.literal("Both Ways"), Component.literal("One Way")),
                blockEntity.isBidirectionalTorque() ? 0 : 1);
        rowY += ROW_HEIGHT;

        downFaceInput = addSelectionRow(rowY,
                Component.literal("Down Face"),
                List.of(
                        Component.literal("Down"),
                        Component.literal("Up"),
                        Component.literal("North"),
                        Component.literal("South"),
                        Component.literal("West"),
                        Component.literal("East")
                ),
                blockEntity.getDownFace().get3DDataValue());

        IconButton confirm = new IconButton(
                guiLeft + PANEL_WIDTH - 33,
                guiTop + panelHeight - 24,
                AllIcons.I_CONFIRM
        );
        confirm.withCallback(this::send);
        addRenderableWidget(confirm);
    }

    private ScrollInput addSelectionRow(int rowY, Component label, List<Component> options, int state) {
        addNameLabel(rowY, label);
        SelectionScrollInput input = new SelectionScrollInput(guiLeft + INPUT_X, rowY - 1, INPUT_WIDTH, 18);
        input.forOptions(options);
        input.titled(label.copy());
        input.setState(state);
        attachValueLabel(input, rowY);
        inputs.add(input);
        addRenderableWidget(input);
        return input;
    }

    private ScrollInput addScrollRow(int rowY, Component label, ScrollInput input) {
        addNameLabel(rowY, label);
        input.setX(guiLeft + INPUT_X);
        input.setY(rowY - 1);
        input.setWidth(INPUT_WIDTH);
        input.titled(label.copy());
        attachValueLabel(input, rowY);
        inputs.add(input);
        addRenderableWidget(input);
        return input;
    }

    private void addNameLabel(int rowY, Component label) {
        // Create's Label ctor only uses the component for width — it always defaults text to "Label"
        Label nameLabel = new Label(guiLeft + LABEL_X, rowY + 4, label);
        nameLabel.text = label;
        addRenderableWidget(nameLabel);
    }

    private void attachValueLabel(ScrollInput input, int rowY) {
        Label valueLabel = new Label(input.getX() + 5, rowY + 1, Component.empty()).withShadow();
        input.writingTo(valueLabel);
        input.onChanged();
        addRenderableWidget(valueLabel);
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        AllGuiTextures.STATION_TEXTBOX_TOP.render(graphics, x, y);
        int middleY = y + AllGuiTextures.STATION_TEXTBOX_TOP.getHeight();
        int middleHeight = panelHeight - AllGuiTextures.STATION_TEXTBOX_TOP.getHeight()
                - AllGuiTextures.STATION_TEXTBOX_BOTTOM.getHeight();
        UIRenderHelper.drawStretched(graphics, x, middleY, PANEL_WIDTH, middleHeight, 0,
                AllGuiTextures.STATION_TEXTBOX_MIDDLE);
        AllGuiTextures.STATION_TEXTBOX_BOTTOM.render(graphics, x, y + panelHeight
                - AllGuiTextures.STATION_TEXTBOX_BOTTOM.getHeight());

        for (ScrollInput input : inputs) {
            renderInputBackground(graphics, input.getX(), input.getY());
        }

        graphics.drawString(
                font,
                title,
                x + (PANEL_WIDTH - font.width(title)) / 2,
                y + 4,
                TITLE_COLOR,
                false
        );
        graphics.renderItem(renderedItem, x + 6, y + 4);
    }

    private static void renderInputBackground(GuiGraphics graphics, int x, int y) {
        UIRenderHelper.drawStretched(graphics, x, y, INPUT_WIDTH, 18, 0, AllGuiTextures.DATA_AREA);
        AllGuiTextures.DATA_AREA_START.render(graphics, x, y);
        AllGuiTextures.DATA_AREA_END.render(graphics, x + INPUT_WIDTH - 2, y);
    }

    @Override
    public void tick() {
        super.tick();
        if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
            refreshTargetInputsFromBlockEntity();
        }
        if (lastModification >= 0 && ++lastModification >= 20) {
            lastModification = -1;
            send();
        }
    }

    private void refreshTargetInputsFromBlockEntity() {
        if (targetPitchInput == null) {
            return;
        }
        if (blockEntity.isNavTargetOverride()) {
            updateScrollInput(targetPitchInput, blockEntity.getTargetPitchDeg());
            updateScrollInput(targetYawInput, blockEntity.getTargetYawDeg());
            updateScrollInput(targetRollInput, blockEntity.getTargetRollDeg());
        }
    }

    private static void updateScrollInput(ScrollInput input, int value) {
        if (input.getState() != value) {
            input.setState(value);
            input.onChanged();
        }
    }

    @Override
    public void removed() {
        send();
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            lastModification = 0;
            return true;
        }
        return false;
    }

    private void send() {
        if (modeInput == null) {
            return;
        }
        CatnipServices.NETWORK.sendToServer(new ConfigureGyroscopePacket(
                blockEntity.getBlockPos(),
                modeInput.getState() == 0 ? 0 : 1,
                autoStabilizeInput.getState() == 0,
                forceInput.getState(),
                dampingInput.getState(),
                acceptAngleInput.getState(),
                pitchInput.getState() == 0,
                yawInput.getState() == 0,
                rollInput.getState() == 0,
                bidirectionalInput.getState() == 0,
                downFaceInput.getState(),
                targetPitchInput.getState(),
                targetYawInput.getState(),
                targetRollInput.getState()
        ));
    }
}
