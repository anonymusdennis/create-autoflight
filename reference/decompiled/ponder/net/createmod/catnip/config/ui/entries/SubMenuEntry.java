package net.createmod.catnip.config.ui.entries;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.createmod.catnip.config.ui.ConfigScreenList;
import net.createmod.catnip.config.ui.SubMenuConfigScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.common.ModConfigSpec;

public class SubMenuEntry extends ConfigScreenList.LabeledEntry {
   protected BoxWidget button;

   public SubMenuEntry(SubMenuConfigScreen parent, String label, ModConfigSpec spec, UnmodifiableConfig config) {
      super(label);
      this.button = new BoxWidget(0, 0, 35, 16)
         .<ElementWidget>showingElement(PonderGuiTextures.ICON_CONFIG_OPEN.asStencil().at(10.0F, 0.0F))
         .withCallback(() -> ScreenOpener.open(new SubMenuConfigScreen(parent, label, parent.type, spec, config)));
      this.button.modifyElement(e -> ((DelegatedStencilElement)e).withElementRenderer(BoxWidget.gradientFactory.apply(this.button)));
      this.listeners.add(this.button);
   }

   @Override
   public void tick() {
      super.tick();
      this.button.tick();
   }

   @Override
   public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
      super.render(graphics, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);
      this.button.setX(x + width - 108);
      this.button.setY(y + 10);
      this.button.setHeight(height - 20);
      this.button.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   protected int getLabelWidth(int totalWidth) {
      return (int)((float)totalWidth * 0.4F) + 30;
   }
}
