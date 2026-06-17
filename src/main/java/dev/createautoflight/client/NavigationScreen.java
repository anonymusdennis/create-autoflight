package dev.createautoflight.client;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import dev.createautoflight.content.navigation.NavigationBlockEntity;
import dev.createautoflight.content.navigation.NavigationSettings;
import dev.createautoflight.network.ConfigureNavigationPacket;
import dev.createautoflight.registry.ModBlocks;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

public class NavigationScreen extends AbstractSimiScreen {
    private static final int PANEL_WIDTH = 170;
    private static final int LABEL_X = 8;
    private static final int INPUT_X = 92;
    private static final int INPUT_WIDTH = 70;
    private static final int ROW_HEIGHT = 18;
    private static final int FIRST_ROW_Y = 18;
    private static final int FOOTER_HEIGHT = 28;
    private static final int ROW_COUNT = 13;
    private static final int TITLE_COLOR = 0x592424;

    private static final Component ON = Component.literal("On");
    private static final Component OFF = Component.literal("Off");

    private final BlockPos blockPos;
    private final ItemStack renderedItem = new ItemStack(ModBlocks.NAVIGATION.get());
    private int panelHeight;
    private int lastModification = -1;

    private ScrollInput activatedInput;
    private ScrollInput debugInput;
    private ScrollInput avoidanceInput;
    private ScrollInput arrivalInput;
    private ScrollInput cruiseInput;
    private ScrollInput slowInput;
    private ScrollInput ignoreTerrainInput;
    private ScrollInput idleBrakingInput;
    private ScrollInput helicopterInput;
    private ScrollInput invertAngleInput;
    private ScrollInput invertThrustInput;
    private ScrollInput helicopterPitchInput;
    private ScrollInput maxThrustInput;

    public NavigationScreen(BlockPos blockPos) {
        super(Component.literal("Navigation Settings"));
        this.blockPos = blockPos.immutable();
    }

    private NavigationBlockEntity blockEntity() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        BlockEntity be = mc.level.getBlockEntity(blockPos);
        return be instanceof NavigationBlockEntity nav ? nav : null;
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

        NavigationBlockEntity blockEntity = blockEntity();
        if (blockEntity == null) {
            onClose();
            return;
        }
        NavigationSettings settings = blockEntity.getSettings();
        int rowY = guiTop + FIRST_ROW_Y;

        activatedInput = addSelectionRow(rowY, Component.literal("Active"),
                List.of(ON, OFF), settings.isActivated() ? 0 : 1);
        rowY += ROW_HEIGHT;

        debugInput = addSelectionRow(rowY, Component.literal("Debug"),
                List.of(ON, OFF), settings.isDebugOverlayEnabled() ? 0 : 1);
        rowY += ROW_HEIGHT;

        avoidanceInput = addScrollRow(rowY, Component.literal("Avoid Off"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(0, NavigationSettings.MAX_AVOIDANCE_OFF_DISTANCE + 1)
                        .format(v -> Component.literal(v + " m"))
                        .setState(settings.getAvoidanceOffDistance()));
        rowY += ROW_HEIGHT;

        arrivalInput = addScrollRow(rowY, Component.literal("Arrival"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(1, 256)
                        .format(v -> Component.literal(v + " m"))
                        .setState(settings.getArrivalRadius()));
        rowY += ROW_HEIGHT;

        cruiseInput = addScrollRow(rowY, Component.literal("Cruise"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(0, 101)
                        .format(v -> Component.literal(v + "%"))
                        .setState(settings.getCruiseSpeedPercent()));
        rowY += ROW_HEIGHT;

        slowInput = addScrollRow(rowY, Component.literal("Slow"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(0, 101)
                        .format(v -> Component.literal(v + "%"))
                        .setState(settings.getSlowSpeedPercent()));
        rowY += ROW_HEIGHT;

        ignoreTerrainInput = addSelectionRow(rowY, Component.literal("Skip Terrain"),
                List.of(ON, OFF), settings.isIgnoreTerrain() ? 0 : 1);
        rowY += ROW_HEIGHT;

        idleBrakingInput = addSelectionRow(rowY, Component.literal("Idle Brake"),
                List.of(ON, OFF), settings.isIdleBraking() ? 0 : 1);
        rowY += ROW_HEIGHT;

        helicopterInput = addSelectionRow(rowY, Component.literal("Helicopter"),
                List.of(ON, OFF), settings.isHelicopterMode() ? 0 : 1);
        rowY += ROW_HEIGHT;

        invertAngleInput = addSelectionRow(rowY, Component.literal("Angle"),
                List.of(Component.literal("Toward"), Component.literal("Away")),
                settings.isInvertAngle() ? 1 : 0);
        rowY += ROW_HEIGHT;

        invertThrustInput = addSelectionRow(rowY, Component.literal("Thrust"),
                List.of(Component.literal("Toward"), Component.literal("Away")),
                settings.isInvertThrust() ? 1 : 0);
        rowY += ROW_HEIGHT;

        helicopterPitchInput = addScrollRow(rowY, Component.literal("Max Pitch"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(0, NavigationSettings.MAX_HELICOPTER_PITCH_DEG + 1)
                        .format(v -> Component.literal(v + "°"))
                        .setState(settings.getHelicopterMaxPitchDeg()));
        rowY += ROW_HEIGHT;

        maxThrustInput = addScrollRow(rowY, Component.literal("Max Thrust"),
                new ScrollInput(0, 0, INPUT_WIDTH, 18)
                        .withRange(1, NavigationSettings.MAX_NAV_MAX_THRUST + 1)
                        .format(v -> Component.literal(String.valueOf(v)))
                        .setState(settings.getNavMaxThrust()));

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

        renderInputBackground(graphics, activatedInput.getX(), activatedInput.getY());
        renderInputBackground(graphics, debugInput.getX(), debugInput.getY());
        renderInputBackground(graphics, avoidanceInput.getX(), avoidanceInput.getY());
        renderInputBackground(graphics, arrivalInput.getX(), arrivalInput.getY());
        renderInputBackground(graphics, cruiseInput.getX(), cruiseInput.getY());
        renderInputBackground(graphics, slowInput.getX(), slowInput.getY());
        renderInputBackground(graphics, ignoreTerrainInput.getX(), ignoreTerrainInput.getY());
        renderInputBackground(graphics, idleBrakingInput.getX(), idleBrakingInput.getY());
        renderInputBackground(graphics, helicopterInput.getX(), helicopterInput.getY());
        renderInputBackground(graphics, invertAngleInput.getX(), invertAngleInput.getY());
        renderInputBackground(graphics, invertThrustInput.getX(), invertThrustInput.getY());
        renderInputBackground(graphics, helicopterPitchInput.getX(), helicopterPitchInput.getY());
        renderInputBackground(graphics, maxThrustInput.getX(), maxThrustInput.getY());

        graphics.drawString(font, title, x + (PANEL_WIDTH - font.width(title)) / 2, y + 4, TITLE_COLOR, false);
        graphics.renderItem(renderedItem, x + 6, y + 4);

        NavigationBlockEntity blockEntity = blockEntity();
        if (blockEntity == null) {
            return;
        }
        Component status = Component.literal("Status: " + blockEntity.getStatus().name());
        graphics.drawString(font, status, x + 8, y + panelHeight - 20, 0x404040, false);
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
        if (activatedInput == null) {
            return;
        }
        NavigationBlockEntity blockEntity = blockEntity();
        if (blockEntity == null) {
            return;
        }
        CatnipServices.NETWORK.sendToServer(new ConfigureNavigationPacket(
                blockEntity.getBlockPos(),
                activatedInput.getState() == 0,
                debugInput.getState() == 0,
                avoidanceInput.getState(),
                arrivalInput.getState(),
                cruiseInput.getState(),
                slowInput.getState(),
                ignoreTerrainInput.getState() == 0,
                idleBrakingInput.getState() == 0,
                helicopterInput.getState() == 0,
                invertAngleInput.getState() == 1,
                invertThrustInput.getState() == 1,
                helicopterPitchInput.getState(),
                maxThrustInput.getState()
        ));
    }
}
