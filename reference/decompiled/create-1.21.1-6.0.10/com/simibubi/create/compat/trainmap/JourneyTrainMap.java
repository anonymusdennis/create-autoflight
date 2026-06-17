package com.simibubi.create.compat.trainmap;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.List;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.JourneyMapPlugin;
import journeymap.api.v2.client.display.Context.UI;
import journeymap.api.v2.client.event.FullscreenRenderEvent;
import journeymap.api.v2.client.fullscreen.IFullscreen;
import journeymap.api.v2.client.util.UIState;
import journeymap.api.v2.common.event.FullscreenEventRegistry;
import journeymap.client.ui.fullscreen.Fullscreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre;

@JourneyMapPlugin(
   apiVersion = "2.0.0"
)
public class JourneyTrainMap implements IClientPlugin {
   private static boolean requesting;

   public void initialize(IClientAPI jmClientApi) {
      FullscreenEventRegistry.FULLSCREEN_RENDER_EVENT.subscribe("create", JourneyTrainMap::onRender);
   }

   public String getModId() {
      return "create";
   }

   public static void tick() {
      if ((Boolean)AllConfigs.client().showTrainMapOverlay.get() && Minecraft.getInstance().screen instanceof Fullscreen) {
         TrainMapManager.tick();
         requesting = true;
         TrainMapSyncClient.requestData();
      } else {
         if (requesting) {
            TrainMapSyncClient.stopRequesting();
         }

         requesting = false;
      }
   }

   public static void mouseClick(Pre event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.screen instanceof Fullscreen screen) {
         Window var8 = mc.getWindow();
         double mX = mc.mouseHandler.xpos() * (double)var8.getGuiScaledWidth() / (double)var8.getScreenWidth();
         double mY = mc.mouseHandler.ypos() * (double)var8.getGuiScaledHeight() / (double)var8.getScreenHeight();
         if (TrainMapManager.handleToggleWidgetClick(Mth.floor(mX), Mth.floor(mY), 3, 30)) {
            event.setCanceled(true);
         }
      }
   }

   public static void onRender(FullscreenRenderEvent event) {
      GuiGraphics graphics = event.getGraphics();
      IFullscreen fullscreen = event.getFullscreen();
      Screen screen = fullscreen.getScreen();
      double x = fullscreen.getCenterBlockX(true);
      double z = fullscreen.getCenterBlockZ(true);
      int mX = event.getMouseX();
      int mY = event.getMouseY();
      float pt = event.getPartialTicks();
      UIState state = fullscreen.getUiState();
      if (state != null) {
         if (state.ui == UI.Fullscreen) {
            if (state.active) {
               if (!(Boolean)AllConfigs.client().showTrainMapOverlay.get()) {
                  renderToggleWidgetAndTooltip(graphics, screen, mX, mY);
               } else {
                  Minecraft mc = Minecraft.getInstance();
                  Window window = mc.getWindow();
                  double guiScale = (double)window.getScreenWidth() / (double)window.getGuiScaledWidth();
                  double scale = state.blockSize / guiScale;
                  PoseStack pose = graphics.pose();
                  pose.pushPose();
                  pose.translate((float)screen.width / 2.0F, (float)screen.height / 2.0F, 0.0F);
                  pose.scale((float)scale, (float)scale, 1.0F);
                  pose.translate(-x, -z, 0.0);
                  float mouseX = (float)mX - (float)screen.width / 2.0F;
                  float mouseY = (float)mY - (float)screen.height / 2.0F;
                  mouseX /= (float)scale;
                  mouseY /= (float)scale;
                  Rect2i bounds = new Rect2i(
                     Mth.floor((double)((float)(-screen.width) / 2.0F) / scale + x),
                     Mth.floor((double)((float)(-screen.height) / 2.0F) / scale + z),
                     Mth.floor((double)screen.width / scale),
                     Mth.floor((double)screen.height / scale)
                  );
                  List<FormattedText> tooltip = TrainMapManager.renderAndPick(graphics, Mth.floor(mouseX), Mth.floor(mouseY), false, bounds);
                  pose.popPose();
                  if (!renderToggleWidgetAndTooltip(graphics, screen, mX, mY) && tooltip != null) {
                     RemovedGuiUtils.drawHoveringText(graphics, tooltip, mX, mY, screen.width, screen.height, 256, mc.font);
                  }
               }
            }
         }
      }
   }

   private static boolean renderToggleWidgetAndTooltip(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
      TrainMapManager.renderToggleWidget(graphics, 3, 30);
      if (!TrainMapManager.isToggleWidgetHovered(mouseX, mouseY, 3, 30)) {
         return false;
      } else {
         RemovedGuiUtils.drawHoveringText(
            graphics,
            List.of(CreateLang.translate("train_map.toggle").component()),
            mouseX,
            mouseY + 20,
            screen.width,
            screen.height,
            256,
            Minecraft.getInstance().font
         );
         return true;
      }
   }
}
