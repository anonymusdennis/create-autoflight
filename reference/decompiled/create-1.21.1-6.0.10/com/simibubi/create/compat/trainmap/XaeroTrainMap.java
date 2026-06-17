package com.simibubi.create.compat.trainmap;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.foundation.mixin.compat.xaeros.XaeroFullscreenMapAccessor;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre;
import xaero.lib.client.gui.ScreenBase;
import xaero.map.gui.GuiMap;

public class XaeroTrainMap {
   private static boolean requesting;
   private static ResourceKey<Level> renderedDimension;
   private static boolean encounteredException = false;

   public static void tick() {
      if ((Boolean)AllConfigs.client().showTrainMapOverlay.get() && isMapOpen(Minecraft.getInstance().screen)) {
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
      if (!encounteredException) {
         Minecraft mc = Minecraft.getInstance();

         try {
            if (!(mc.screen instanceof GuiMap)) {
               return;
            }
         } catch (Throwable var7) {
            Create.LOGGER.error("Failed to handle mouseClick for Xaero's World Map train map integration:", var7);
            encounteredException = true;
            return;
         }

         Window window = mc.getWindow();
         double mX = mc.mouseHandler.xpos() * (double)window.getGuiScaledWidth() / (double)window.getScreenWidth();
         double mY = mc.mouseHandler.ypos() * (double)window.getGuiScaledHeight() / (double)window.getScreenHeight();
         if (TrainMapManager.handleToggleWidgetClick(Mth.floor(mX), Mth.floor(mY), 3, 30)) {
            event.setCanceled(true);
         }
      }
   }

   public static void onRender(GuiGraphics graphics, GuiMap screen, int mX, int mY, float pt) {
      double x = ((XaeroFullscreenMapAccessor)screen).create$getCameraX();
      double z = ((XaeroFullscreenMapAccessor)screen).create$getCameraZ();
      double mapScale = ((XaeroFullscreenMapAccessor)screen).create$getScale();
      renderedDimension = ((XaeroFullscreenMapAccessor)screen).create$getMapProcessor().getMapWorld().getCurrentDimension().getDimId();
      if (!(Boolean)AllConfigs.client().showTrainMapOverlay.get()) {
         renderToggleWidgetAndTooltip(graphics, screen, mX, mY);
      } else {
         Minecraft mc = Minecraft.getInstance();
         Window window = mc.getWindow();
         double guiScale = (double)window.getScreenWidth() / (double)window.getGuiScaledWidth();
         double interfaceScale = (double)window.getWidth() / (double)window.getScreenWidth();
         double scale = mapScale / guiScale / interfaceScale;
         PoseStack pose = graphics.pose();
         pose.pushPose();
         pose.translate((float)screen.width / 2.0F, (float)screen.height / 2.0F, 0.0F);
         pose.scale((float)scale, (float)scale, 1.0F);
         pose.translate(-x, -z, 0.0);
         float mouseX = (float)mX - (float)screen.width / 2.0F;
         float mouseY = (float)mY - (float)screen.height / 2.0F;
         mouseX = (float)((double)mouseX / scale);
         mouseY = (float)((double)mouseY / scale);
         mouseX = (float)((double)mouseX + x);
         mouseY = (float)((double)mouseY + z);
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

   private static boolean renderToggleWidgetAndTooltip(GuiGraphics graphics, GuiMap screen, int mouseX, int mouseY) {
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

   public static ResourceKey<Level> getRenderedDimension() {
      return renderedDimension;
   }

   public static boolean isMapOpen(Screen screen) {
      if (encounteredException) {
         return false;
      } else {
         try {
            if (screen instanceof ScreenBase screenBase && (screenBase instanceof GuiMap || screenBase.parent instanceof GuiMap)) {
               return true;
            }

            return false;
         } catch (Throwable var2) {
            Create.LOGGER.error("Failed to check if Xaero's World Map was open for train map integration:", var2);
            encounteredException = true;
            return false;
         }
      }
   }
}
