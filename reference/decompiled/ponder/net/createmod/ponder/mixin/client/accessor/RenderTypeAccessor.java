package net.createmod.ponder.mixin.client.accessor;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeRenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({RenderType.class})
public interface RenderTypeAccessor {
   @Invoker("create")
   static CompositeRenderType catnip$create(String string, VertexFormat vertexFormat, Mode mode, int i, boolean bl, boolean bl2, CompositeState compositeState) {
      throw new AssertionError("Mixin application failed!");
   }
}
