package com.simibubi.create.foundation.map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MapDecorationTextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.client.gui.map.IMapDecorationRenderer;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class StationMapDecorationRenderer implements IMapDecorationRenderer {
   public boolean render(
      MapDecoration decoration,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      @NotNull MapItemSavedData mapData,
      MapDecorationTextureManager decorationTextures,
      boolean inItemFrame,
      int packedLight,
      int index
   ) {
      poseStack.pushPose();
      poseStack.translate((double)decoration.x() / 2.0 + 64.0, (double)decoration.y() / 2.0 + 64.0, -0.02);
      poseStack.pushPose();
      poseStack.translate(0.5F, 0.0F, 0.0F);
      poseStack.scale(4.5F, 4.5F, 3.0F);
      TextureAtlasSprite sprite = decorationTextures.get(decoration);
      float U0 = sprite.getU0();
      float V0 = sprite.getV0();
      float U1 = sprite.getU1();
      float V1 = sprite.getV1();
      VertexConsumer buffer = bufferSource.getBuffer(RenderType.text(sprite.atlasLocation()));
      Matrix4f mat = poseStack.last().pose();
      float zOffset = -0.001F;
      buffer.addVertex(mat, -1.0F, 1.0F, (float)index * zOffset).setColor(-1).setUv(U0, V0).setLight(packedLight);
      buffer.addVertex(mat, 1.0F, 1.0F, (float)index * zOffset).setColor(-1).setUv(U1, V0).setLight(packedLight);
      buffer.addVertex(mat, 1.0F, -1.0F, (float)index * zOffset).setColor(-1).setUv(U1, V1).setLight(packedLight);
      buffer.addVertex(mat, -1.0F, -1.0F, (float)index * zOffset).setColor(-1).setUv(U0, V1).setLight(packedLight);
      poseStack.popPose();
      if (decoration.name().isPresent()) {
         Font font = Minecraft.getInstance().font;
         Component component = (Component)decoration.name().get();
         float f6 = (float)font.width(component);
         poseStack.pushPose();
         poseStack.translate(0.0, 6.0, -0.005F);
         poseStack.scale(0.8F, 0.8F, 1.0F);
         poseStack.translate(-f6 / 2.0F + 0.5F, 0.0F, 0.0F);
         font.drawInBatch(component, 0.0F, 0.0F, -1, false, poseStack.last().pose(), bufferSource, DisplayMode.NORMAL, Integer.MIN_VALUE, packedLight);
         poseStack.popPose();
      }

      poseStack.popPose();
      return true;
   }
}
