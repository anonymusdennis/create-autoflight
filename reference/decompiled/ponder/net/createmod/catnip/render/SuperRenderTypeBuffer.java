package net.createmod.catnip.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public interface SuperRenderTypeBuffer extends MultiBufferSource {
   VertexConsumer getEarlyBuffer(RenderType var1);

   VertexConsumer getBuffer(RenderType var1);

   VertexConsumer getLateBuffer(RenderType var1);

   void draw();

   void draw(RenderType var1);
}
