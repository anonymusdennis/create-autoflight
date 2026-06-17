package net.createmod.catnip.config.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.FadableScreenElement;
import net.createmod.catnip.gui.element.TextStencilElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;

public class BaseConfigScreen extends ConfigScreen {
   public static final Color COLOR_TITLE_A = new Color(-3760196).setImmutable();
   public static final Color COLOR_TITLE_B = new Color(-608069).setImmutable();
   public static final Color COLOR_TITLE_C = new Color(-263788).setImmutable();
   public static final FadableScreenElement DISABLED_RENDERER = (ms, width, height, alpha) -> UIRenderHelper.angledGradient(
         ms, 0.0F, 0, height / 2, (float)height, (float)width, AbstractSimiWidget.COLOR_DISABLED
      );
   private static final Map<String, UnaryOperator<BaseConfigScreen>> DEFAULTS = new HashMap<>();
   @Nullable
   BoxWidget clientConfigWidget;
   @Nullable
   BoxWidget commonConfigWidget;
   @Nullable
   BoxWidget serverConfigWidget;
   @Nullable
   BoxWidget goBack;
   @Nullable
   BoxWidget others;
   @Nullable
   BoxWidget title;
   @Nullable
   ModConfigSpec clientSpec;
   @Nullable
   ModConfigSpec commonSpec;
   @Nullable
   ModConfigSpec serverSpec;
   String clientButtonLabel = "Client Config";
   String commonButtonLabel = "Common Config";
   String serverButtonLabel = "Server Config";
   String modID;
   protected boolean returnOnClose;

   public static void setDefaultActionFor(String modID, UnaryOperator<BaseConfigScreen> transform) {
      DEFAULTS.put(modID, transform);
   }

   public BaseConfigScreen(@Nullable Screen parent, String modID) {
      super(parent);
      this.modID = modID;
      if (DEFAULTS.containsKey(modID)) {
         DEFAULTS.get(modID).apply(this);
      } else {
         this.searchForConfigSpecs();
      }
   }

   public BaseConfigScreen searchForConfigSpecs() {
      if (!ConfigHelper.hasAnyForgeConfig(this.modID)) {
         return this;
      } else {
         try {
            this.clientSpec = ConfigHelper.findModConfigSpecFor(Type.CLIENT, this.modID);
         } catch (NullPointerException | ClassCastException var4) {
            Ponder.LOGGER.debug("Unable to find ClientConfigSpec for mod: " + this.modID);
         }

         try {
            this.commonSpec = ConfigHelper.findModConfigSpecFor(Type.COMMON, this.modID);
         } catch (NullPointerException | ClassCastException var3) {
            Ponder.LOGGER.debug("Unable to find CommonConfigSpec for mod: " + this.modID);
         }

         try {
            this.serverSpec = ConfigHelper.findModConfigSpecFor(Type.SERVER, this.modID);
         } catch (NullPointerException | ClassCastException var2) {
            Ponder.LOGGER.debug("Unable to find ServerConfigSpec for mod: " + this.modID);
         }

         return this;
      }
   }

   public BaseConfigScreen withSpecs(@Nullable ModConfigSpec client, @Nullable ModConfigSpec common, @Nullable ModConfigSpec server) {
      this.clientSpec = client;
      this.commonSpec = common;
      this.serverSpec = server;
      return this;
   }

   public BaseConfigScreen withButtonLabels(@Nullable String client, @Nullable String common, @Nullable String server) {
      if (client != null) {
         this.clientButtonLabel = client;
      }

      if (common != null) {
         this.commonButtonLabel = common;
      }

      if (server != null) {
         this.serverButtonLabel = server;
      }

      return this;
   }

   @Override
   protected void init() {
      super.init();
      this.returnOnClose = true;
      TextStencilElement clientText = new TextStencilElement(this.font, Component.translatable("catnip.ui.client_config_button_label")).centered(true, true);
      this.addRenderableWidget(this.clientConfigWidget = new BoxWidget(this.width / 2 - 100, this.height / 2 - 15 - 30, 200, 16).showingElement(clientText));
      if (this.clientSpec != null) {
         this.clientConfigWidget.withCallback(() -> this.linkTo(new SubMenuConfigScreen(this, Type.CLIENT, this.clientSpec)));
         clientText.withElementRenderer(BoxWidget.gradientFactory.apply(this.clientConfigWidget));
      } else {
         this.clientConfigWidget.active = false;
         this.clientConfigWidget.updateGradientFromState();
         clientText.withElementRenderer(DISABLED_RENDERER);
      }

      TextStencilElement commonText = new TextStencilElement(this.font, Component.translatable("catnip.ui.common_config_button_label")).centered(true, true);
      this.addRenderableWidget(this.commonConfigWidget = new BoxWidget(this.width / 2 - 100, this.height / 2 - 15, 200, 16).showingElement(commonText));
      if (this.commonSpec != null) {
         this.commonConfigWidget.withCallback(() -> this.linkTo(new SubMenuConfigScreen(this, Type.COMMON, this.commonSpec)));
         commonText.withElementRenderer(BoxWidget.gradientFactory.apply(this.commonConfigWidget));
      } else {
         this.commonConfigWidget.active = false;
         this.commonConfigWidget.updateGradientFromState();
         commonText.withElementRenderer(DISABLED_RENDERER);
      }

      TextStencilElement serverText = new TextStencilElement(this.font, Component.translatable("catnip.ui.server_config_button_label")).centered(true, true);
      this.addRenderableWidget(this.serverConfigWidget = new BoxWidget(this.width / 2 - 100, this.height / 2 - 15 + 30, 200, 16).showingElement(serverText));
      if (this.serverSpec == null) {
         this.serverConfigWidget.active = false;
         this.serverConfigWidget.updateGradientFromState();
         serverText.withElementRenderer(DISABLED_RENDERER);
      } else if (this.minecraft.level == null) {
         serverText.withElementRenderer(DISABLED_RENDERER);
         this.serverConfigWidget.getToolTip().add(Component.translatable("catnip.ui.server_config_unavailable"));
         this.serverConfigWidget
            .getToolTip()
            .addAll(FontHelper.cutTextComponent(Component.translatable("catnip.ui.server_config_unavailable_tooltip"), FontHelper.Palette.ALL_GRAY));
      } else {
         this.serverConfigWidget.withCallback(() -> this.linkTo(new SubMenuConfigScreen(this, Type.SERVER, this.serverSpec)));
         serverText.withElementRenderer(BoxWidget.gradientFactory.apply(this.serverConfigWidget));
      }

      TextStencilElement titleText = new TextStencilElement(this.font, CatnipServices.PLATFORM.getModDisplayName(this.modID))
         .centered(true, true)
         .withElementRenderer((ms, w, h, alpha) -> {
            UIRenderHelper.angledGradient(ms, 0.0F, 0, h / 2, (float)h, (float)(w / 2), COLOR_TITLE_A, COLOR_TITLE_B);
            UIRenderHelper.angledGradient(ms, 0.0F, w / 2, h / 2, (float)h, (float)(w / 2), COLOR_TITLE_B, COLOR_TITLE_C);
         });
      int boxWidth = this.width + 10;
      int boxHeight = 39;
      int boxPadding = 4;
      this.title = new BoxWidget(-5, this.height / 2 - 110, boxWidth, boxHeight)
         .<BoxWidget>setActive(false)
         .<BoxWidget>withBorderColors(AbstractSimiWidget.COLOR_IDLE)
         .<ElementWidget>withPadding(0.0F, (float)boxPadding)
         .<ElementWidget>rescaleElement((float)boxWidth / 2.0F, (float)(boxHeight - 2 * boxPadding) / 2.0F)
         .showingElement(titleText.at(0.0F, 7.0F));
      this.addRenderableWidget(this.title);
      ConfigScreen.modID = this.modID;
      this.goBack = new BoxWidget(this.width / 2 - 134, this.height / 2, 20, 20)
         .<ElementWidget>withPadding(2.0F, 2.0F)
         .withCallback(() -> this.linkTo(this.parent));
      this.goBack.showingElement(PonderGuiTextures.ICON_CONFIG_BACK.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(this.goBack)));
      this.goBack.getToolTip().add(Component.translatable("catnip.ui.go_back_button"));
      this.addRenderableWidget(this.goBack);
      TextStencilElement othersText = new TextStencilElement(this.font, Component.translatable("catnip.ui.other_mods_config_button_label"))
         .centered(true, true);
      this.others = new BoxWidget(this.width / 2 - 100, this.height / 2 - 15 + 90, 200, 16).showingElement(othersText);
      othersText.withElementRenderer(BoxWidget.gradientFactory.apply(this.others));
      this.others.withCallback(() -> this.linkTo(new ConfigModListScreen(this)));
      this.addRenderableWidget(this.others);
   }

   @Override
   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      graphics.drawCenteredString(
         this.font,
         Component.translatable("catnip.ui.other_mods_config_title"),
         this.width / 2,
         this.height / 2 - 105,
         UIRenderHelper.COLOR_TEXT_STRONG_ACCENT.getFirst().getRGB()
      );
   }

   private void linkTo(@Nullable Screen screen) {
      this.returnOnClose = false;
      ScreenOpener.open(screen);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else {
         if (keyCode == 259) {
            this.linkTo(this.parent);
         }

         return false;
      }
   }
}
