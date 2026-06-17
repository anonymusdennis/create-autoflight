package net.createmod.catnip.config.ui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.gui.TickableGuiEventListener;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.gui.element.TextStencilElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ConfigScreenList extends ObjectSelectionList<ConfigScreenList.Entry> implements TickableGuiEventListener {
   @Nullable
   public static EditBox currentText;

   public ConfigScreenList(Minecraft client, int width, int height, int top, int elementHeight) {
      super(client, width, height, top, elementHeight);
      currentText = null;
      this.headerHeight = 3;
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      Color c = new Color(1610612736);
      UIRenderHelper.angledGradient(graphics, 90.0F, this.getX() + this.width / 2, this.getY(), (float)this.width, 5.0F, c, Color.TRANSPARENT_BLACK);
      UIRenderHelper.angledGradient(graphics, -90.0F, this.getX() + this.width / 2, this.getBottom(), (float)this.width, 5.0F, c, Color.TRANSPARENT_BLACK);
      UIRenderHelper.angledGradient(graphics, 0.0F, this.getX(), this.getY() + this.height / 2, (float)this.height, 5.0F, c, Color.TRANSPARENT_BLACK);
      UIRenderHelper.angledGradient(graphics, 180.0F, this.getRight(), this.getY() + this.height / 2, (float)this.height, 5.0F, c, Color.TRANSPARENT_BLACK);
      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   protected void renderListItems(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      Window window = this.minecraft.getWindow();
      double d0 = window.getGuiScale();
      RenderSystem.enableScissor(
         (int)((double)this.getX() * d0),
         (int)((double)window.getHeight() - (double)this.getBottom() * d0),
         (int)((double)this.width * d0),
         (int)((double)this.height * d0)
      );
      super.renderListItems(graphics, mouseX, mouseY, partialTick);
      RenderSystem.disableScissor();
   }

   public boolean mouseClicked(double x, double y, int button) {
      return super.mouseClicked(x, y, button);
   }

   public int getRowWidth() {
      return this.width - 16;
   }

   public int getWidth() {
      return this.width;
   }

   protected int getScrollbarPosition() {
      return this.getX() + this.width - 6;
   }

   @Override
   public void tick() {
      this.children().forEach(ConfigScreenList.Entry::tick);
   }

   public boolean search(String query) {
      if (query != null && !query.isEmpty()) {
         String q = query.toLowerCase(Locale.ROOT);
         Optional<ConfigScreenList.Entry> first = this.children().stream().filter(entry -> {
            if (entry.path == null) {
               return false;
            } else {
               String[] split = entry.path.split("\\.");
               String key = split[split.length - 1].toLowerCase(Locale.ROOT);
               return key.contains(q);
            }
         }).findFirst();
         if (first.isEmpty()) {
            this.setScrollAmount(0.0);
            return false;
         } else {
            ConfigScreenList.Entry e = first.get();
            e.annotations.put("highlight", "(:");
            this.centerScrollOn(e);
            return true;
         }
      } else {
         this.setScrollAmount(0.0);
         return true;
      }
   }

   public void bumpCog(float force) {
      ConfigScreen.cogSpin.bump(3, (double)force);
   }

   public abstract static class Entry
      extends net.minecraft.client.gui.components.ObjectSelectionList.Entry<ConfigScreenList.Entry>
      implements TickableGuiEventListener {
      protected List<GuiEventListener> listeners = new ArrayList<>();
      protected Map<String, String> annotations = new HashMap<>();
      @Nullable
      protected String path;

      protected Entry() {
      }

      public boolean mouseClicked(double x, double y, int button) {
         return this.getGuiListeners().stream().anyMatch(l -> l.mouseClicked(x, y, button));
      }

      public boolean keyPressed(int code, int keyPressed_2_, int keyPressed_3_) {
         return this.getGuiListeners().stream().anyMatch(l -> l.keyPressed(code, keyPressed_2_, keyPressed_3_));
      }

      public boolean charTyped(char ch, int code) {
         for (GuiEventListener l : this.getGuiListeners()) {
            if (l.charTyped(ch, code)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public void tick() {
      }

      public List<GuiEventListener> getGuiListeners() {
         return this.listeners;
      }

      protected void setEditable(boolean b) {
      }

      protected boolean isCurrentValueChanged() {
         return this.path == null ? false : ConfigHelper.changes.containsKey(this.path);
      }
   }

   public static class LabeledEntry extends ConfigScreenList.Entry {
      protected static final float labelWidthMult = 0.4F;
      protected TextStencilElement label;
      protected List<Component> labelTooltip;
      @Nullable
      protected String unit = null;
      protected LerpedFloat differenceAnimation = LerpedFloat.linear().startWithValue(0.0);
      protected LerpedFloat highlightAnimation = LerpedFloat.linear().startWithValue(0.0);

      public LabeledEntry(String label) {
         this.label = new TextStencilElement(Minecraft.getInstance().font, label);
         this.label
            .withElementRenderer(
               (graphics, width, height, alpha) -> UIRenderHelper.angledGradient(
                     graphics, 0.0F, 0, height / 2, (float)height, (float)width, UIRenderHelper.COLOR_TEXT_STRONG_ACCENT
                  )
            );
         this.labelTooltip = new ArrayList<>();
      }

      public LabeledEntry(String label, String path) {
         this(label);
         this.path = path;
      }

      @Override
      public void tick() {
         this.differenceAnimation.tickChaser();
         this.highlightAnimation.tickChaser();
         super.tick();
      }

      public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
         if (this.isCurrentValueChanged()) {
            if (this.differenceAnimation.getChaseTarget() != 1.0F) {
               this.differenceAnimation.chase(1.0, 0.5, LerpedFloat.Chaser.EXP);
            }
         } else if (this.differenceAnimation.getChaseTarget() != 0.0F) {
            this.differenceAnimation.chase(0.0, 0.6F, LerpedFloat.Chaser.EXP);
         }

         float animation = this.differenceAnimation.getValue(partialTicks);
         if (animation > 0.1F) {
            int offset = (int)(30.0F * (1.0F - animation));
            if (this.annotations.containsKey(ConfigAnnotations.RequiresRestart.CLIENT.getName())) {
               UIRenderHelper.streak(graphics, 180.0F, x + width + 10 + offset, y + height / 2, height - 6, 110, new Color(1348472848));
            } else if (this.annotations.containsKey(ConfigAnnotations.RequiresRelog.TRUE.getName())) {
               UIRenderHelper.streak(graphics, 180.0F, x + width + 10 + offset, y + height / 2, height - 6, 110, new Color(1089403671));
            }

            UIRenderHelper.breadcrumbArrow(graphics, x - 10 - offset, y + 6, 0, -20, 24, -18, new Color(1895825407), Color.TRANSPARENT_BLACK);
         }

         UIRenderHelper.streak(graphics, 0.0F, x - 10, y + height / 2, height - 6, width / 8 * 7, new Color(-587202560));
         UIRenderHelper.streak(graphics, 180.0F, x + (int)((float)width * 1.35F) + 10, y + height / 2, height - 6, width / 8 * 7, new Color(-587202560));
         MutableComponent component = this.label.getComponent();
         Font font = Minecraft.getInstance().font;
         if (font.width(component) > this.getLabelWidth(width) - 10) {
            this.label.withText(font.substrByWidth(component, this.getLabelWidth(width) - 15).getString() + "...");
         }

         if (this.unit != null) {
            int unitWidth = font.width(this.unit);
            graphics.drawString(
               font, this.unit, x + this.getLabelWidth(width) - unitWidth - 5, y + height / 2 + 2, UIRenderHelper.COLOR_TEXT_DARKER.getFirst().getRGB()
            );
            this.label.<RenderElement>at((float)(x + 10), (float)(y + height / 2 - 10), 0.0F).render(graphics);
         } else {
            this.label.<RenderElement>at((float)(x + 10), (float)(y + height / 2 - 4), 0.0F).render(graphics);
         }

         if (this.annotations.containsKey("highlight")) {
            this.highlightAnimation.startWithValue(1.0).chase(0.0, 0.1F, LerpedFloat.Chaser.LINEAR);
            this.annotations.remove("highlight");
         }

         animation = this.highlightAnimation.getValue(partialTicks);
         if (animation > 0.01F) {
            Color highlight = new Color(-1593835521).scaleAlpha(animation);
            UIRenderHelper.streak(graphics, 0.0F, x - 10, y + height / 2, height - 6, 5, highlight);
            UIRenderHelper.streak(graphics, 180.0F, x + width, y + height / 2, height - 6, 5, highlight);
            UIRenderHelper.streak(graphics, 90.0F, x + width / 2 - 5, y + 3, width + 10, 5, highlight);
            UIRenderHelper.streak(graphics, -90.0F, x + width / 2 - 5, y + height - 3, width + 10, 5, highlight);
         }

         if (mouseX > x && mouseX < x + this.getLabelWidth(width) && mouseY > y + 5 && mouseY < y + height - 5) {
            List<Component> tooltip = this.getLabelTooltip();
            if (tooltip.isEmpty()) {
               return;
            }

            RenderSystem.disableScissor();
            graphics.pose().pushPose();
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            graphics.flush();
            graphics.pose().popPose();
            GlStateManager._enableScissorTest();
         }
      }

      public List<Component> getLabelTooltip() {
         return this.labelTooltip;
      }

      protected int getLabelWidth(int totalWidth) {
         return totalWidth;
      }

      public Component getNarration() {
         return CommonComponents.EMPTY;
      }
   }
}
