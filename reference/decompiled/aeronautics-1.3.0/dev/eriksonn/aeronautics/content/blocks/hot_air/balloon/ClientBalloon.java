package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon;

import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect.HeatedCulledRenderRegion;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph;
import dev.ryanhcode.sable.util.LevelAccelerator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ClientBalloon extends Balloon {
   @Nullable
   private HeatedCulledRenderRegion renderRegion = null;
   private boolean leaking;

   @Internal
   public ClientBalloon(
      Level level, LevelAccelerator accelerator, BlockPos controllerPos, BalloonLayerGraph graph, ObjectArrayList<BlockEntityLiftingGasProvider> heaters
   ) {
      super(level, accelerator, controllerPos, graph, heaters);
      this.rebuildRenderRegion();
   }

   @Override
   public boolean shouldSpawnGust(BlockPos pos) {
      return false;
   }

   @Override
   public void setLeaking() {
      super.setLeaking();
      this.leaking = true;
   }

   @Override
   protected void onHotAirAdded(BlockPos blockPos) {
      super.onHotAirAdded(blockPos);
      this.rebuildRenderRegion();
   }

   @Override
   protected void onHotAirAdded(Iterable<BlockPos> hotAir) {
      super.onHotAirAdded(hotAir);
      this.rebuildRenderRegion();
   }

   @Override
   protected void onHotAirRemoved(BlockPos blockPos) {
      super.onHotAirRemoved(blockPos);
      this.rebuildRenderRegion();
   }

   @Override
   protected void onHotAirRemoved(Iterable<BlockPos> blockPos) {
      super.onHotAirRemoved(blockPos);
      this.rebuildRenderRegion();
   }

   @Override
   protected void onRebuilt() {
      super.onRebuilt();
      this.rebuildRenderRegion();
   }

   public void rebuildRenderRegion() {
      this.freeRenderer();
      if (this.capacity > 0) {
         this.renderRegion = new HeatedCulledRenderRegion(this.accelerator, this);
      }
   }

   private void freeRenderer() {
      if (this.renderRegion != null) {
         this.renderRegion.free();
      }

      this.renderRegion = null;
   }

   @Nullable
   public HeatedCulledRenderRegion getRenderRegion() {
      return this.renderRegion;
   }

   @Override
   public boolean isValid() {
      return !this.leaking && !this.heaters.isEmpty();
   }

   @Override
   public void onRemoved() {
      super.onRemoved();
      this.freeRenderer();
   }
}
