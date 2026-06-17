package dev.ryanhcode.sable.render.water_occlusion;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegion;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegionBuilder;
import java.util.Collection;
import net.minecraft.core.BlockPos;

public class WaterOcclusionRenderRegion extends SimpleCulledRenderRegion {
   public WaterOcclusionRenderRegion(Collection<BlockPos> blocks) {
      super(blocks);
   }

   @Override
   public SimpleCulledRenderRegionBuilder createMeshBuilder(int gridSize) {
      return new SimpleCulledRenderRegionBuilder(gridSize);
   }

   @Override
   public VertexFormat getVertexFormat() {
      return DefaultVertexFormat.POSITION;
   }
}
