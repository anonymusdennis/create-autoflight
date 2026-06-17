package dev.ryanhcode.sable.render.dynamic_biome;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.RenderType;

public class DynamicBiomeTintRenderTypes extends RenderType {
   private static final String NAME = "dynamic_biome_tint";

   public DynamicBiomeTintRenderTypes(
      String string, VertexFormat vertexFormat, Mode mode, int i, boolean bl, boolean bl2, Runnable runnable, Runnable runnable2
   ) {
      super(string, vertexFormat, mode, i, bl, bl2, runnable, runnable2);
   }
}
