package dev.ryanhcode.sable.neoforge.mixin.dynamic_directional_shading;

import com.mojang.blaze3d.vertex.VertexSorting;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.dynamic_directional_shading.ModelBlockRendererCacheExtension;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionCompiler.Results;
import net.minecraft.core.SectionPos;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent.AdditionalSectionRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SectionCompiler.class})
public class SectionCompilerMixin {
   @Inject(
      method = {"compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;"},
      at = {@At("HEAD")}
   )
   private void sable$preCompile(
      SectionPos sectionPos,
      RenderChunkRegion region,
      VertexSorting sorting,
      SectionBufferBuilderPack pack,
      List<AdditionalSectionRenderer> list,
      CallbackInfoReturnable<Results> cir
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      SubLevelContainer container = SubLevelContainer.getContainer(level);
      LevelPlot plot = container.getPlot(sectionPos.chunk());
      ((ModelBlockRendererCacheExtension)ModelBlockRenderer.CACHE.get()).sable$setOnSubLevel(plot != null);
   }

   @Inject(
      method = {"compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;"},
      at = {@At("TAIL")}
   )
   private void sable$postCompile(
      SectionPos arg,
      RenderChunkRegion arg2,
      VertexSorting arg3,
      SectionBufferBuilderPack arg4,
      List<AdditionalSectionRenderer> additionalRenderers,
      CallbackInfoReturnable<Results> cir
   ) {
      ((ModelBlockRendererCacheExtension)ModelBlockRenderer.CACHE.get()).sable$setOnSubLevel(false);
   }
}
