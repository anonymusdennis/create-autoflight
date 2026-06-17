package dev.engine_room.flywheel.impl.visualization.storage;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class BlockEntityStorage extends Storage<BlockEntity> {
   private final Long2ObjectMap<BlockEntityVisual<?>> posLookup = new Long2ObjectOpenHashMap();

   @Nullable
   public BlockEntityVisual<?> visualAtPos(long pos) {
      return (BlockEntityVisual<?>)this.posLookup.get(pos);
   }

   public boolean willAccept(BlockEntity blockEntity) {
      if (blockEntity.isRemoved()) {
         return false;
      } else if (!VisualizationHelper.canVisualize(blockEntity)) {
         return false;
      } else {
         Level level = blockEntity.getLevel();
         if (level == null) {
            return false;
         } else if (level.isEmptyBlock(blockEntity.getBlockPos())) {
            return false;
         } else {
            BlockPos pos = blockEntity.getBlockPos();
            BlockGetter existingChunk = level.getChunkForCollisions(pos.getX() >> 4, pos.getZ() >> 4);
            return existingChunk != null;
         }
      }
   }

   @Nullable
   protected BlockEntityVisual<?> createRaw(VisualizationContext visualizationContext, BlockEntity obj, float partialTick) {
      BlockEntityVisualizer<BlockEntity> visualizer = VisualizationHelper.getVisualizer(obj);
      if (visualizer == null) {
         return null;
      } else {
         BlockEntityVisual<BlockEntity> visual = visualizer.createVisual(visualizationContext, obj, partialTick);
         BlockPos blockPos = obj.getBlockPos();
         this.posLookup.put(blockPos.asLong(), visual);
         return visual;
      }
   }

   public void remove(BlockEntity obj) {
      this.posLookup.remove(obj.getBlockPos().asLong());
      super.remove(obj);
   }

   @Override
   public void recreateAll(VisualizationContext visualizationContext, float partialTick) {
      this.posLookup.clear();
      super.recreateAll(visualizationContext, partialTick);
   }

   @Override
   public void invalidate() {
      this.posLookup.clear();
      super.invalidate();
   }
}
