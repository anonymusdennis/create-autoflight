package dev.createautoflight.client;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import dev.createautoflight.content.thrust.DynamicThrustControllerBlockEntity;
import dev.createautoflight.content.thrust.ThrustControlMode;
import dev.createautoflight.network.ConfigureThrustControllerPacket;
import dev.createautoflight.registry.ModBlocks;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ThrustControllerScreen extends AbstractSimiScreen {
    private static final int PANEL_WIDTH = 150;
    private static final int LABEL_X = 8;
    private static final int INPUT_X = 72;
    private static final int INPUT_WIDTH = 70;
    private static final int ROW_HEIGHT = 18;
    private static final int FIRST_ROW_Y = 18;
    private static final int FOOTER_HEIGHT = 22;
    private static final int ROW_COUNT = 10;
    private static final int TITLE_COLOR = 0x592424;

    private static final Component ON = Component.literal("On");
    private static final Component OFF = Component.literal("Off");

    private final DynamicThrustControllerBlockEntity blockEntity;
    private final ItemStack renderedItem = new ItemStack(ModBlocks.DYNAMIC_THRUST_CONTROLLER_ITEM.get());
    private int panelHeight;
    private int lastModification = -1;

    private SelectionScrollInput modeInput;
    private ScrollInput gainInput;
    private ScrollInput slewInput;
    private SelectionScrollInput debugInput;
    private SelectionScrollInput holdAltInput;
    private ScrollInput targetHeightInput;
    private Label statusLabel;

    public ThrustControllerScreen(DynamicThrustControllerBlockEntity blockEntity) {
        super(Component.translatable("gui.create_autoflight.thrust_controller.title"));
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
        clearWidgets();

        int rowY = guiTop + FIRST_ROW_Y;

        modeInput = addSelectionRow(rowY,
                Component.translatable("gui.create_autoflight.thrust_controller.mode"),
                List.of(Component.literal("Hover"), Component.literal("Nav")),
                blockEntity.getMode() == ThrustControlMode.HOVER ? 0 : 1);
        rowY += ROW_HEIGHT;

        gainInput = addScrollRow(rowY,
                Component.translatable("gui.create_autoflight.thrust_controller.gain"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(1, DynamicThrustControllerBlockEntity.MAX_GAIN_PERCENT + 1)
                        .format(v -> Component.literal(v + "%"))
                        .setState(blockEntity.getGainPercent()));
        rowY += ROW_HEIGHT;

        slewInput = addScrollRow(rowY,
                Component.translatable("gui.create_autoflight.thrust_controller.slew"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(1, DynamicThrustControllerBlockEntity.MAX_SLEW_RPM + 1)
                        .format(v -> Component.literal(v + " RPM/t"))
                        .setState(blockEntity.getMaxSlewRpm()));
        rowY += ROW_HEIGHT;

        holdAltInput = addSelectionRow(rowY,
                Component.translatable("gui.create_autoflight.thrust_controller.hold_alt"),
                List.of(ON, OFF),
                blockEntity.isHoldAltitude() ? 0 : 1);
        rowY += ROW_HEIGHT;

        targetHeightInput = addScrollRow(rowY,
                Component.translatable("gui.create_autoflight.thrust_controller.target_y"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(
                                DynamicThrustControllerBlockEntity.MIN_TARGET_HEIGHT,
                                DynamicThrustControllerBlockEntity.MAX_TARGET_HEIGHT + 1
                        )
                        .format(v -> Component.literal(v + " Y"))
                        .setState(blockEntity.getTargetHeightY()));
        rowY += ROW_HEIGHT;

        debugInput = addSelectionRow(rowY,
                Component.translatable("gui.create_autoflight.thrust_controller.debug"),
                List.of(ON, OFF),
                blockEntity.isDebugOverlay() ? 0 : 1);
        rowY += ROW_HEIGHT;

        addNameLabel(rowY, Component.translatable("gui.create_autoflight.thrust_controller.status"));
        statusLabel = new Label(guiLeft + INPUT_X, rowY + 1, statusText()).withShadow();
        addRenderableWidget(statusLabel);
        rowY += ROW_HEIGHT;

        addNameLabel(rowY, Component.literal("Meas Y"));
        Label measLabel = new Label(guiLeft + INPUT_X, rowY + 1,
                Component.literal(String.format("%.1f", blockEntity.getStatusMeasuredY()))).withShadow();
        addRenderableWidget(measLabel);
        rowY += ROW_HEIGHT;

        addNameLabel(rowY, Component.literal("Hover Y"));
        Label hoverLabel = new Label(guiLeft + INPUT_X, rowY + 1,
                Component.literal(String.format("%.1f", blockEntity.getStatusHoverY()))).withShadow();
        addRenderableWidget(hoverLabel);
        rowY += ROW_HEIGHT;

        addNameLabel(rowY, Component.literal("Target Y"));
        Label targetLabel = new Label(guiLeft + INPUT_X, rowY + 1,
                Component.literal(String.format("%.1f", blockEntity.getStatusTargetY()))).withShadow();
        addRenderableWidget(targetLabel);

        IconButton confirm = new IconButton(
                guiLeft + PANEL_WIDTH - 33,
                guiTop + panelHeight - 24,
                AllIcons.I_CONFIRM
        );
        confirm.withCallback(this::send);
        addRenderableWidget(confirm);
    }

    private Component statusText() {
        return Component.literal(blockEntity.getStatusText()
                + " (" + blockEntity.getStatusGearboxCount() + " GB)");
    }

    private SelectionScrollInput addSelectionRow(int rowY, Component label, List<Component> options, int state) {
        addNameLabel(rowY, label);
        SelectionScrollInput input = new SelectionScrollInput(guiLeft + INPUT_X, rowY - 1, INPUT_WIDTH, 18);
        input.forOptions(options);
        input.titled(label.copy());
        input.setState(state);
        attachValueLabel(input, rowY);
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
        addRenderableWidget(input);
        return input;
    }

    private void addNameLabel(int rowY, Component label) {
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

        renderInputBackground(graphics, modeInput.getX(), modeInput.getY());
        renderInputBackground(graphics, gainInput.getX(), gainInput.getY());
        renderInputBackground(graphics, slewInput.getX(), slewInput.getY());
        renderInputBackground(graphics, holdAltInput.getX(), holdAltInput.getY());
        renderInputBackground(graphics, targetHeightInput.getX(), targetHeightInput.getY());
        renderInputBackground(graphics, debugInput.getX(), debugInput.getY());

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
        if (statusLabel != null) {
            statusLabel.text = statusText();
        }
        if (lastModification >= 0 && ++lastModification >= 20) {
            lastModification = -1;
            send();
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
        CatnipServices.NETWORK.sendToServer(new ConfigureThrustControllerPacket(
                blockEntity.getBlockPos(),
                modeInput.getState(),
                gainInput.getState(),
                slewInput.getState(),
                debugInput.getState() == 0,
                holdAltInput.getState() == 0,
                targetHeightInput.getState()
        ));
    }
}
