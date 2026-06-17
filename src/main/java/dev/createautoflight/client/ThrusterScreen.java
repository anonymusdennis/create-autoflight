package dev.createautoflight.client;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import dev.createautoflight.content.thruster.ThrusterBlockEntity;
import dev.createautoflight.network.ConfigureThrusterPacket;
import dev.createautoflight.registry.ModBlocks;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ThrusterScreen extends AbstractSimiScreen {
    private static final int PANEL_WIDTH = 150;
    private static final int LABEL_X = 8;
    private static final int INPUT_X = 72;
    private static final int INPUT_WIDTH = 70;
    private static final int ROW_HEIGHT = 18;
    private static final int FIRST_ROW_Y = 18;
    private static final int FOOTER_HEIGHT = 22;
    private static final int ROW_COUNT = 6;
    private static final int TITLE_COLOR = 0x592424;

    private static final Component ON = Component.literal("On");
    private static final Component OFF = Component.literal("Off");

    private final ThrusterBlockEntity blockEntity;
    private final ItemStack renderedItem = new ItemStack(ModBlocks.THRUSTER.get());
    private int panelHeight;
    private int lastModification = -1;

    private ScrollInput enabledInput;
    private ScrollInput modeInput;
    private ScrollInput strengthInput;
    private ScrollInput maxThrustInput;
    private ScrollInput invertDirectionInput;
    private ScrollInput smokeInput;

    public ThrusterScreen(ThrusterBlockEntity blockEntity) {
        super(Component.literal("Thruster Settings"));
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

        enabledInput = addSelectionRow(rowY,
                Component.literal("Enabled"),
                List.of(ON, OFF),
                blockEntity.isEnabled() ? 0 : 1);
        rowY += ROW_HEIGHT;

        modeInput = addSelectionRow(rowY,
                Component.literal("Mode"),
                List.of(Component.literal("Brake"), Component.literal("Nav")),
                blockEntity.getMode() == ThrusterBlockEntity.ThrusterMode.BRAKE ? 0 : 1);
        rowY += ROW_HEIGHT;

        strengthInput = addScrollRow(rowY,
                Component.literal("Response"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(0, ThrusterBlockEntity.MAX_STRENGTH_PERCENT + 1)
                        .format(v -> Component.literal(v + "%"))
                        .setState(blockEntity.getStrengthPercent()));
        rowY += ROW_HEIGHT;

        maxThrustInput = addScrollRow(rowY,
                Component.literal("Max Thrust"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(0, ThrusterBlockEntity.MAX_THRUST_LIMIT + 1)
                        .format(v -> Component.literal(v + " pN"))
                        .setState(blockEntity.getMaxThrust()));
        rowY += ROW_HEIGHT;

        invertDirectionInput = addSelectionRow(rowY,
                Component.literal("Direction"),
                List.of(Component.literal("Forward"), Component.literal("Reverse")),
                blockEntity.isInvertDirection() ? 1 : 0);
        rowY += ROW_HEIGHT;

        smokeInput = addSelectionRow(rowY,
                Component.literal("Smoke"),
                List.of(ON, OFF),
                blockEntity.isSmokeParticles() ? 0 : 1);

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

        renderInputBackground(graphics, enabledInput.getX(), enabledInput.getY());
        renderInputBackground(graphics, modeInput.getX(), modeInput.getY());
        renderInputBackground(graphics, strengthInput.getX(), strengthInput.getY());
        renderInputBackground(graphics, maxThrustInput.getX(), maxThrustInput.getY());
        renderInputBackground(graphics, invertDirectionInput.getX(), invertDirectionInput.getY());
        renderInputBackground(graphics, smokeInput.getX(), smokeInput.getY());

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
        if (enabledInput == null) {
            return;
        }
        CatnipServices.NETWORK.sendToServer(new ConfigureThrusterPacket(
                blockEntity.getBlockPos(),
                enabledInput.getState() == 0,
                modeInput.getState() == 0
                        ? ThrusterBlockEntity.ThrusterMode.BRAKE
                        : ThrusterBlockEntity.ThrusterMode.NAVIGATION,
                strengthInput.getState(),
                maxThrustInput.getState(),
                smokeInput.getState() == 0,
                invertDirectionInput.getState() == 1
        ));
    }
}
