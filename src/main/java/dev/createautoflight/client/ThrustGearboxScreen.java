package dev.createautoflight.client;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import dev.createautoflight.content.thrust.ThrustVectoringGearboxBlockEntity;
import dev.createautoflight.network.ConfigureThrustGearboxPacket;
import dev.createautoflight.registry.ModBlocks;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ThrustGearboxScreen extends AbstractSimiScreen {
    private static final int PANEL_WIDTH = 150;
    private static final int LABEL_X = 8;
    private static final int INPUT_X = 72;
    private static final int INPUT_WIDTH = 70;
    private static final int ROW_HEIGHT = 18;
    private static final int FIRST_ROW_Y = 18;
    private static final int FOOTER_HEIGHT = 22;
    private static final int ROW_COUNT = 1;
    private static final int TITLE_COLOR = 0x592424;

    private final ThrustVectoringGearboxBlockEntity blockEntity;
    private final ItemStack renderedItem = new ItemStack(ModBlocks.THRUST_VECTORING_GEARBOX_ITEM.get());
    private int panelHeight;
    private int lastModification = -1;
    private SelectionScrollInput axisInput;

    public ThrustGearboxScreen(ThrustVectoringGearboxBlockEntity blockEntity) {
        super(Component.translatable("gui.create_autoflight.thrust_gearbox.title"));
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
        List<Component> axisOptions = List.of(
                Component.literal("Up"),
                Component.literal("Down"),
                Component.literal("North"),
                Component.literal("South"),
                Component.literal("East"),
                Component.literal("West")
        );
        axisInput = addSelectionRow(rowY,
                Component.translatable("gui.create_autoflight.thrust_gearbox.axis"),
                axisOptions,
                blockEntity.getThrustAxis().get3DDataValue());

        IconButton confirm = new IconButton(
                guiLeft + PANEL_WIDTH - 33,
                guiTop + panelHeight - 24,
                AllIcons.I_CONFIRM
        );
        confirm.withCallback(this::send);
        addRenderableWidget(confirm);
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

    private void addNameLabel(int rowY, Component label) {
        Label nameLabel = new Label(guiLeft + LABEL_X, rowY + 4, label);
        nameLabel.text = label;
        addRenderableWidget(nameLabel);
    }

    private void attachValueLabel(SelectionScrollInput input, int rowY) {
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

        renderInputBackground(graphics, axisInput.getX(), axisInput.getY());

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
        if (axisInput == null) {
            return;
        }
        CatnipServices.NETWORK.sendToServer(new ConfigureThrustGearboxPacket(
                blockEntity.getBlockPos(),
                Direction.from3DDataValue(axisInput.getState())
        ));
    }
}
