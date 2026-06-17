package dev.engine_room.flywheel.backend;

import dev.engine_room.flywheel.api.material.CutoutShader;
import dev.engine_room.flywheel.api.material.FogShader;
import dev.engine_room.flywheel.backend.compile.PipelineCompiler;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Unmodifiable;

public final class MaterialShaderIndices {
   private static final MaterialShaderIndices.Index fogSources = new MaterialShaderIndices.Index();
   private static final MaterialShaderIndices.Index cutoutSources = new MaterialShaderIndices.Index();

   private MaterialShaderIndices() {
   }

   public static MaterialShaderIndices.Index fogSources() {
      return fogSources;
   }

   public static MaterialShaderIndices.Index cutoutSources() {
      return cutoutSources;
   }

   public static int fogIndex(FogShader fogShader) {
      return fogSources().index(fogShader.source());
   }

   public static int cutoutIndex(CutoutShader cutoutShader) {
      return cutoutSources().index(cutoutShader.source());
   }

   public static class Index {
      private final Object2IntMap<ResourceLocation> sources2Index = new Object2IntOpenHashMap();
      private final ObjectList<ResourceLocation> sources;

      private Index() {
         this.sources2Index.defaultReturnValue(-1);
         this.sources = new ObjectArrayList();
      }

      public ResourceLocation get(int index) {
         return (ResourceLocation)this.sources.get(index);
      }

      public int index(ResourceLocation source) {
         int out = this.sources2Index.getInt(source);
         if (out == -1) {
            this.add(source);
            PipelineCompiler.deleteAll();
            return this.sources2Index.getInt(source);
         } else {
            return out;
         }
      }

      @Unmodifiable
      public List<ResourceLocation> all() {
         return this.sources;
      }

      private void add(ResourceLocation source) {
         if (this.sources2Index.putIfAbsent(source, this.sources.size()) == -1) {
            this.sources.add(source);
         }
      }
   }
}
