package net.createmod.catnip.gui.widget;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.TickableGuiEventListener;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public abstract class AbstractSimiWidget extends AbstractWidget implements TickableGuiEventListener {
   public static final Color HEADER_RGB = new Color(5476833, false);
   public static final Color HINT_RGB = new Color(9877472, false);
   public static final Couple<Color> COLOR_IDLE = Couple.create(new Color(-578111786, true), new Color(-1869957418, true)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_HOVER = Couple.create(new Color(-6636589, true), new Color(-795165741, true)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_CLICK = Couple.create(new Color(-1, true), new Color(-285212673, true)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_DISABLED = Couple.create(new Color(-2138009456, true), new Color(1620086928, true)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_SUCCESS = Couple.create(new Color(-863438968, true), new Color(-870265824, true)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_FAIL = Couple.create(new Color(-856192888, true), new Color(-859037664, true)).map(Color::setImmutable);
   protected float z;
   protected boolean wasHovered = false;
   protected List<Component> toolTip = new LinkedList<>();
   protected BiConsumer<Integer, Integer> onClick = (_$, _$$) -> {
   };
   public int lockedTooltipX = -1;
   public int lockedTooltipY = -1;

   protected AbstractSimiWidget(int x, int y) {
      this(x, y, 16, 16);
   }

   protected AbstractSimiWidget(int x, int y, int width, int height) {
      this(x, y, width, height, CommonComponents.EMPTY);
   }

   protected AbstractSimiWidget(int x, int y, int width, int height, Component message) {
      super(x, y, width, height, message);
   }

   public <T extends AbstractSimiWidget> T withCallback(BiConsumer<Integer, Integer> cb) {
      this.onClick = cb;
      return (T)this;
   }

   public <T extends AbstractSimiWidget> T withCallback(Runnable cb) {
      return this.withCallback((_$, _$$) -> cb.run());
   }

   public <T extends AbstractSimiWidget> T atZLevel(float z) {
      this.z = z;
      return (T)this;
   }

   public <T extends AbstractSimiWidget> T setActive(boolean active) {
      this.active = active;
      return (T)this;
   }

   public List<Component> getToolTip() {
      return this.toolTip;
   }

   @Override
   public void tick() {
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.visible) {
         this.isHovered = this.isMouseOver((double)mouseX, (double)mouseY);
         this.renderWidget(graphics, mouseX, mouseY, partialTicks);
         this.renderTooltip(graphics, mouseX, mouseY, partialTicks);
         this.wasHovered = this.isHoveredOrFocused();
      }
   }

   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      this.beforeRender(graphics, mouseX, mouseY, partialTicks);
      this.doRender(graphics, mouseX, mouseY, partialTicks);
      this.afterRender(graphics, mouseX, mouseY, partialTicks);
   }

   protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.isHovered()) {
         List<Component> tooltip = this.getToolTip();
         if (tooltip.isEmpty()) {
            return;
         }

         int ttx = this.lockedTooltipX == -1 ? mouseX : this.lockedTooltipX + this.getX();
         int tty = this.lockedTooltipY == -1 ? mouseY : this.lockedTooltipY + this.getY();
         Font font = Minecraft.getInstance().font;
         graphics.renderComponentTooltip(font, tooltip, ttx, tty);
      }
   }

   protected void beforeRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      graphics.pose().pushPose();
   }

   protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
   }

   protected void afterRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      graphics.pose().popPose();
   }

   public void runCallback(double mouseX, double mouseY) {
      this.onClick.accept((int)mouseX, (int)mouseY);
   }

   protected boolean clicked(double mouseX, double mouseY) {
      return this.isMouseOver(mouseX, mouseY);
   }

   public void onClick(double mouseX, double mouseY) {
      this.runCallback(mouseX, mouseY);
   }

   public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
      this.defaultButtonNarrationText(pNarrationElementOutput);
   }

   public void setHeight(int value) {
      this.height = value;
   }
}
