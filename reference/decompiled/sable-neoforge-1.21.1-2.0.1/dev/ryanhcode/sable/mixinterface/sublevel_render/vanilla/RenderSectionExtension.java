package dev.ryanhcode.sable.mixinterface.sublevel_render.vanilla;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;

public interface RenderSectionExtension {
   void sable$addDirtyListener(RenderSectionExtension.DirtyListener var1);

   void sable$setListening(boolean var1);

   @FunctionalInterface
   public interface DirtyListener {
      void markDirty(RenderSection var1);
   }
}
