package com.simibubi.create.compat.trainmap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.gui.RegionMapPanel;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.ScreenWrapper;
import dev.ftb.mods.ftblibrary.ui.Widget;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.client.event.RenderTooltipEvent.Pre;
import net.neoforged.neoforge.client.event.ScreenEvent.Render.Post;

public class FTBChunksTrainMap {
   private static int cancelTooltips = 0;
   private static boolean renderingTooltip = false;
   private static boolean requesting;

   public static void tick() {
      if (cancelTooltips > 0) {
         cancelTooltips--;
      }

      LargeMapScreen mapScreen = getAsLargeMapScreen(Minecraft.getInstance().screen);
      if ((Boolean)AllConfigs.client().showTrainMapOverlay.get() && mapScreen != null) {
         TrainMapManager.tick(mapScreen.currentDimension());
         requesting = true;
         TrainMapSyncClient.requestData();
      } else {
         if (requesting) {
            TrainMapSyncClient.stopRequesting();
         }

         requesting = false;
      }
   }

   public static void cancelTooltips(Pre event) {
      if (getAsLargeMapScreen(Minecraft.getInstance().screen) != null) {
         if (!renderingTooltip && cancelTooltips != 0) {
            event.setCanceled(true);
         }
      }
   }

   public static void mouseClick(net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre event) {
      LargeMapScreen screen = getAsLargeMapScreen(Minecraft.getInstance().screen);
      if (screen != null) {
         if (TrainMapManager.handleToggleWidgetClick(screen.getMouseX(), screen.getMouseY(), 20, 2)) {
            event.setCanceled(true);
         }
      }
   }

   public static void renderGui(Post event) {
      LargeMapScreen largeMapScreen = getAsLargeMapScreen(event.getScreen());
      if (largeMapScreen != null) {
         Object panel = ObfuscationReflectionHelper.getPrivateValue(LargeMapScreen.class, largeMapScreen, "regionPanel");
         if (panel instanceof RegionMapPanel regionMapPanel) {
            GuiGraphics graphics = event.getGuiGraphics();
            if (!(Boolean)AllConfigs.client().showTrainMapOverlay.get()) {
               renderToggleWidgetAndTooltip(event, largeMapScreen, graphics);
            } else {
               int blocksPerRegion = 512;
               int minX = Mth.floor(regionMapPanel.getScrollX());
               int minY = Mth.floor(regionMapPanel.getScrollY());
               float regionTileSize = (float)largeMapScreen.getRegionTileSize() / (float)blocksPerRegion;
               int regionMinX = (Integer)ObfuscationReflectionHelper.getPrivateValue(RegionMapPanel.class, regionMapPanel, "regionMinX");
               int regionMinZ = (Integer)ObfuscationReflectionHelper.getPrivateValue(RegionMapPanel.class, regionMapPanel, "regionMinZ");
               float mouseX = (float)event.getMouseX();
               float mouseY = (float)event.getMouseY();
               boolean linearFiltering = (double)largeMapScreen.getRegionTileSize() * Minecraft.getInstance().getWindow().getGuiScale() < 512.0;
               PoseStack pose = graphics.pose();
               pose.pushPose();
               pose.translate((float)(-minX), (float)(-minY), 0.0F);
               pose.scale(regionTileSize, regionTileSize, 1.0F);
               pose.translate((float)(-regionMinX * blocksPerRegion), (float)(-regionMinZ * blocksPerRegion), 0.0F);
               mouseX += (float)minX;
               mouseY += (float)minY;
               mouseX /= regionTileSize;
               mouseY /= regionTileSize;
               mouseX += (float)(regionMinX * blocksPerRegion);
               mouseY += (float)(regionMinZ * blocksPerRegion);
               Rect2i bounds = new Rect2i(
                  Mth.floor((float)minX / regionTileSize + (float)(regionMinX * blocksPerRegion)),
                  Mth.floor((float)minY / regionTileSize + (float)(regionMinZ * blocksPerRegion)),
                  Mth.floor((float)largeMapScreen.width / regionTileSize),
                  Mth.floor((float)largeMapScreen.height / regionTileSize)
               );
               List<FormattedText> tooltip = TrainMapManager.renderAndPick(graphics, Mth.floor(mouseX), Mth.floor(mouseY), linearFiltering, bounds);
               pose.popPose();
               if (!renderToggleWidgetAndTooltip(event, largeMapScreen, graphics) && tooltip != null) {
                  renderingTooltip = true;
                  RemovedGuiUtils.drawHoveringText(
                     graphics, tooltip, event.getMouseX(), event.getMouseY(), largeMapScreen.width, largeMapScreen.height, 256, Minecraft.getInstance().font
                  );
                  renderingTooltip = false;
                  cancelTooltips = 5;
               }

               pose.pushPose();
               pose.translate(0.0F, 0.0F, 300.0F);

               for (Widget widget : largeMapScreen.getWidgets()) {
                  if (widget.isEnabled() && widget != panel) {
                     widget.draw(graphics, largeMapScreen.getTheme(), widget.getPosX(), widget.getPosY(), widget.getWidth(), widget.getHeight());
                  }
               }

               pose.popPose();
            }
         }
      }
   }

   private static boolean renderToggleWidgetAndTooltip(Post event, LargeMapScreen largeMapScreen, GuiGraphics graphics) {
      TrainMapManager.renderToggleWidget(graphics, 20, 2);
      if (!TrainMapManager.isToggleWidgetHovered(event.getMouseX(), event.getMouseY(), 20, 2)) {
         return false;
      } else {
         renderingTooltip = true;
         RemovedGuiUtils.drawHoveringText(
            graphics,
            List.of(CreateLang.translate("train_map.toggle").component()),
            event.getMouseX(),
            event.getMouseY() + 20,
            largeMapScreen.width,
            largeMapScreen.height,
            256,
            Minecraft.getInstance().font
         );
         renderingTooltip = false;
         cancelTooltips = 5;
         return true;
      }
   }

   private static LargeMapScreen getAsLargeMapScreen(Screen screen) {
      if (screen instanceof ScreenWrapper screenWrapper) {
         BaseScreen wrapped = screenWrapper.getGui();
         return wrapped instanceof LargeMapScreen ? (LargeMapScreen)wrapped : null;
      } else {
         return null;
      }
   }
}
