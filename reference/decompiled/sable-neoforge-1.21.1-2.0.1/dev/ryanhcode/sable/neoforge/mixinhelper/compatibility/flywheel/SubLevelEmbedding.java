package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.flywheel;

import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import java.util.List;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SubLevelEmbedding {
   private final VisualEmbedding embedding;
   private final List<BlockEntity> blockEntities;
   private int latestSkyLightScale;

   public SubLevelEmbedding(VisualEmbedding embedding, List<BlockEntity> blockEntities, int latestSkyLightScale) {
      this.embedding = embedding;
      this.blockEntities = blockEntities;
      this.latestSkyLightScale = latestSkyLightScale;
   }

   public VisualEmbedding embedding() {
      return this.embedding;
   }

   public List<BlockEntity> blockEntities() {
      return this.blockEntities;
   }

   public int latestSkyLightScale() {
      return this.latestSkyLightScale;
   }

   public void setLatestSkyLightScale(int latestSkyLightScale) {
      this.latestSkyLightScale = latestSkyLightScale;
   }

   @Override
   public String toString() {
      return "SubLevelEmbedding[embedding="
         + this.embedding
         + ", blockEntities="
         + this.blockEntities
         + ", latestSkyLightScale="
         + this.latestSkyLightScale
         + "]";
   }
}
