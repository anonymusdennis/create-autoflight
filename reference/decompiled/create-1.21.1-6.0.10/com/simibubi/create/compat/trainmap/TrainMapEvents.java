package com.simibubi.create.compat.trainmap;

import com.simibubi.create.compat.Mods;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.InputEvent.MouseButton.Pre;

@EventBusSubscriber({Dist.CLIENT})
public class TrainMapEvents {
   @SubscribeEvent
   public static void tick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         if (Mods.FTBCHUNKS.isLoaded()) {
            FTBChunksTrainMap.tick();
         }

         if (Mods.JOURNEYMAP.isLoaded()) {
            JourneyTrainMap.tick();
         }

         if (Mods.XAEROWORLDMAP.isLoaded()) {
            XaeroTrainMap.tick();
         }
      }
   }

   @SubscribeEvent
   public static void mouseClick(Pre event) {
      if (event.getAction() == 1) {
         if (Mods.FTBCHUNKS.isLoaded()) {
            FTBChunksTrainMap.mouseClick(event);
         }

         if (Mods.JOURNEYMAP.isLoaded()) {
            JourneyTrainMap.mouseClick(event);
         }

         if (Mods.XAEROWORLDMAP.isLoaded()) {
            XaeroTrainMap.mouseClick(event);
         }
      }
   }

   @SubscribeEvent
   public static void cancelTooltips(net.neoforged.neoforge.client.event.RenderTooltipEvent.Pre event) {
      if (Mods.FTBCHUNKS.isLoaded()) {
         FTBChunksTrainMap.cancelTooltips(event);
      }
   }

   @SubscribeEvent
   public static void renderGui(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
      if (Mods.FTBCHUNKS.isLoaded()) {
         FTBChunksTrainMap.renderGui(event);
      }
   }
}
