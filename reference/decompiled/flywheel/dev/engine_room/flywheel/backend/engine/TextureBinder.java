package dev.engine_room.flywheel.backend.engine;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.engine_room.flywheel.backend.Samplers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

public class TextureBinder {
   public static void bind(ResourceLocation resourceLocation) {
      RenderSystem.bindTexture(byName(resourceLocation));
   }

   public static void bindLightAndOverlay() {
      GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
      Samplers.OVERLAY.makeActive();
      gameRenderer.overlayTexture().setupOverlayColor();
      RenderSystem.bindTexture(RenderSystem.getShaderTexture(1));
      Samplers.LIGHT.makeActive();
      gameRenderer.lightTexture().turnOnLightLayer();
      RenderSystem.bindTexture(RenderSystem.getShaderTexture(2));
   }

   public static void resetLightAndOverlay() {
      GameRenderer gameRenderer = Minecraft.getInstance().gameRenderer;
      gameRenderer.overlayTexture().teardownOverlayColor();
      gameRenderer.lightTexture().turnOffLightLayer();
   }

   public static int byName(ResourceLocation texture) {
      return Minecraft.getInstance().getTextureManager().getTexture(texture).getId();
   }
}
