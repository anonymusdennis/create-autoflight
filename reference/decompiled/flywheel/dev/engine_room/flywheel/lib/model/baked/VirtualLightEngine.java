package dev.engine_room.flywheel.lib.model.baked;

import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jetbrains.annotations.Nullable;

public final class VirtualLightEngine extends LevelLightEngine {
   private final LayerLightEventListener blockListener;
   private final LayerLightEventListener skyListener;

   public VirtualLightEngine(ToIntFunction<BlockPos> blockLightFunc, ToIntFunction<BlockPos> skyLightFunc, BlockGetter level) {
      super(new LightChunkGetter() {
         @Nullable
         public LightChunk getChunkForLighting(int x, int z) {
            return null;
         }

         public BlockGetter getLevel() {
            return level;
         }
      }, false, false);
      this.blockListener = new VirtualLightEngine.VirtualLayerLightEventListener(blockLightFunc);
      this.skyListener = new VirtualLightEngine.VirtualLayerLightEventListener(skyLightFunc);
   }

   public LayerLightEventListener getLayerListener(LightLayer layer) {
      return layer == LightLayer.BLOCK ? this.blockListener : this.skyListener;
   }

   public int getRawBrightness(BlockPos pos, int amount) {
      int i = this.skyListener.getLightValue(pos) - amount;
      int j = this.blockListener.getLightValue(pos);
      return Math.max(j, i);
   }

   private static class VirtualLayerLightEventListener implements LayerLightEventListener {
      private final ToIntFunction<BlockPos> lightFunc;

      public VirtualLayerLightEventListener(ToIntFunction<BlockPos> lightFunc) {
         this.lightFunc = lightFunc;
      }

      public void checkBlock(BlockPos pos) {
      }

      public boolean hasLightWork() {
         return false;
      }

      public int runLightUpdates() {
         return 0;
      }

      public void updateSectionStatus(SectionPos pos, boolean isSectionEmpty) {
      }

      public void setLightEnabled(ChunkPos pos, boolean lightEnabled) {
      }

      public void propagateLightSources(ChunkPos pos) {
      }

      public DataLayer getDataLayerData(SectionPos pos) {
         return null;
      }

      public int getLightValue(BlockPos pos) {
         return this.lightFunc.applyAsInt(pos);
      }
   }
}
