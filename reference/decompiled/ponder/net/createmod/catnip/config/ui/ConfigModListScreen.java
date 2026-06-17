package net.createmod.catnip.config.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigModListScreen extends ConfigScreen {
   @Nullable
   ConfigScreenList list;
   @Nullable
   HintableTextFieldWidget search;
   @Nullable
   BoxWidget goBack;
   List<ConfigModListScreen.ModEntry> allEntries = new ArrayList<>();

   public ConfigModListScreen(@Nullable Screen parent) {
      super(parent);
   }

   @Override
   protected void init() {
      super.init();
      int listWidth = Math.min(this.width - 80, 300);
      this.list = new ConfigScreenList(this.minecraft, listWidth, this.height - 60, 15, 40);
      this.list.setX(this.width / 2 - this.list.getWidth() / 2);
      this.addRenderableWidget(this.list);
      this.allEntries = new ArrayList<>();
      CatnipServices.PLATFORM.getLoadedMods().forEach(id -> this.allEntries.add(new ConfigModListScreen.ModEntry(id, this)));
      this.allEntries.sort((e1, e2) -> {
         int empty = (e2.button.active ? 1 : 0) - (e1.button.active ? 1 : 0);
         return empty != 0 ? empty : e1.id.compareToIgnoreCase(e2.id);
      });
      this.list.children().clear();
      this.list.children().addAll(this.allEntries);
      this.goBack = new BoxWidget(this.width / 2 - listWidth / 2 - 30, this.height / 2 + 65, 20, 20)
         .<ElementWidget>withPadding(2.0F, 2.0F)
         .withCallback(() -> ScreenOpener.open(this.parent));
      this.goBack.showingElement(PonderGuiTextures.ICON_CONFIG_BACK.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(this.goBack)));
      this.goBack.getToolTip().add(Component.translatable("catnip.ui.go_back_button"));
      this.addRenderableWidget(this.goBack);
      this.search = new HintableTextFieldWidget(this.font, this.width / 2 - listWidth / 2, this.height - 35, listWidth, 20);
      this.search.setResponder(this::updateFilter);
      this.search.setHint(Component.translatable("catnip.ui.search_hint"));
      this.search.moveCursorToStart(false);
      this.addRenderableWidget(this.search);
   }

   public boolean mouseClicked(double x, double y, int button) {
      if (this.search != null && !this.search.isMouseOver(x, y)) {
         this.search.setFocused(false);
      }

      return super.mouseClicked(x, y, button);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else {
         if (this.search != null && Screen.hasControlDown() && keyCode == 70) {
            this.setFocused(this.search);
         }

         if (keyCode == 259) {
            ScreenOpener.open(this.parent);
         }

         return false;
      }
   }

   private void updateFilter(String search) {
      assert this.list != null;

      assert this.search != null;

      this.list.children().clear();

      for (ConfigModListScreen.ModEntry modEntry : this.allEntries) {
         if (modEntry.id.contains(search.toLowerCase(Locale.ROOT))) {
            this.list.children().add(modEntry);
         }
      }

      this.list.setScrollAmount(this.list.getScrollAmount());
      if (!this.list.children().isEmpty()) {
         this.search.setTextColor(UIRenderHelper.COLOR_TEXT.getFirst().getRGB());
      } else {
         this.search.setTextColor(AbstractSimiWidget.COLOR_FAIL.getFirst().getRGB());
      }
   }

   public static class ModEntry extends ConfigScreenList.LabeledEntry {
      protected BoxWidget button;
      protected String id;

      public ModEntry(String id, Screen parent) {
         super(CatnipServices.PLATFORM.getModDisplayName(id));
         this.id = id;
         this.button = new BoxWidget(0, 0, 35, 16).showingElement(PonderGuiTextures.ICON_CONFIG_OPEN.asStencil().at(10.0F, 0.0F));
         this.button.modifyElement(e -> ((DelegatedStencilElement)e).withElementRenderer(BoxWidget.gradientFactory.apply(this.button)));
         if (ConfigHelper.hasAnyForgeConfig(id)) {
            this.button.withCallback(() -> ScreenOpener.open(new BaseConfigScreen(parent, id)));
         } else {
            this.button.active = false;
            this.button.updateGradientFromState();
            this.button.modifyElement(e -> ((DelegatedStencilElement)e).withElementRenderer(BaseConfigScreen.DISABLED_RENDERER));
            this.labelTooltip.add(Component.literal(CatnipServices.PLATFORM.getModDisplayName(id)));
            this.labelTooltip
               .addAll(FontHelper.cutTextComponent(Component.translatable("catnip.ui.other_mods_config_unavailable"), FontHelper.Palette.ALL_GRAY));
         }

         this.listeners.add(this.button);
      }

      public String getId() {
         return this.id;
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
}
