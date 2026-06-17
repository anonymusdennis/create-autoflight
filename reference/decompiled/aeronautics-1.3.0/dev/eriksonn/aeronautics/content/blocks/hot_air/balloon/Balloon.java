package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon;

import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonBuilder;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerData;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph;
import dev.eriksonn.aeronautics.content.blocks.hot_air.gust.GustEntity;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public abstract class Balloon {
   protected final Level level;
   protected final Set<BlockEntityLiftingGasProvider> heaters;
   protected final BoundingBox3i bounds = new BoundingBox3i();
   protected final LevelAccelerator accelerator;
   protected BalloonLayerGraph graph;
   protected BlockPos controllerPos;
   protected int capacity;
   private boolean assembling;

   protected Balloon(
      Level level, LevelAccelerator accelerator, BlockPos controllerPos, BalloonLayerGraph graph, ObjectArrayList<BlockEntityLiftingGasProvider> heaters
   ) {
      this.level = level;
      this.accelerator = accelerator;
      this.controllerPos = controllerPos;
      this.graph = graph;
      this.heaters = new ObjectOpenHashSet(heaters);
      this.recomputeBalloonData();
   }

   public void tick() {
      if (this.assembling) {
         this.rebuildBalloonFromController();
         this.assembling = false;
      }

      this.checkHeaters();
   }

   protected void checkHeaters() {
      Iterator<BlockEntityLiftingGasProvider> iterator = this.heaters.iterator();
      BlockPos highestPos = null;
      SubLevel balloonSubLevel = Sable.HELPER.getContaining(this.level, this.controllerPos);

      while (iterator.hasNext()) {
         BlockEntityLiftingGasProvider heater = iterator.next();
         BlockPos heaterPos = heater.getCastPosition();
         if (heater.canOutputGas() && heaterPos != null && Sable.HELPER.getContaining(this.level, heaterPos) == balloonSubLevel) {
            if (highestPos == null) {
               highestPos = heaterPos;
            }

            if (heaterPos.getY() > highestPos.getY()) {
               highestPos = heaterPos;
            }
         } else {
            heater.setBalloon(null);
            iterator.remove();
         }
      }

      if (highestPos != null) {
         this.moveController(highestPos);
      }

      this.splitHeaters();
   }

   private void splitHeaters() {
      Iterator<BlockEntityLiftingGasProvider> iterator = this.heaters.iterator();

      while (iterator.hasNext()) {
         BlockEntityLiftingGasProvider heater = iterator.next();
         BlockPos heaterPos = heater.getCastPosition();

         assert heaterPos != null;

         if (!this.graph.hasBlockAt(heaterPos)) {
            heater.setBalloon(null);
            iterator.remove();
         }
      }
   }

   public void onSolidBlockAdded(BlockPos pos) {
      if (!this.assembling) {
         BalloonLayerData layer = this.graph.getLayerAt(pos);
         if (layer != null) {
            int x = pos.getX();
            int z = pos.getZ();
            if (!layer.getSolidBlock(x, z)) {
               layer.addSolidBlock(x, z);
               layer.solidCount++;
               this.capacity--;
               this.onHotAirRemoved(pos);
            }
         }
      }
   }

   public void onSolidBlockRemoved(BlockPos pos) {
      BalloonLayerData layer = this.graph.getLayerAt(pos);
      if (layer != null) {
         int x = pos.getX();
         int z = pos.getZ();
         if (layer.getSolidBlock(x, z)) {
            layer.removeSolidBlock(x, z);
            layer.solidCount--;
            this.capacity++;
            this.onHotAirAdded(pos);
         }
      }
   }

   public void onAirtightBlockRemoved(BlockPos pos) {
      if (!this.assembling) {
         MutableBlockPos adjacentPos = new MutableBlockPos();
         boolean shouldSpawnGust = this.shouldSpawnGust(pos);
         boolean gusted = false;

         for (Direction dir : SimDirectionUtil.VALUES) {
            adjacentPos.setWithOffset(pos, dir);
            BalloonLayerData layer = this.graph.getLayerAt(adjacentPos);
            if (layer != null) {
               layer.removeHotAirBlock(adjacentPos.getX(), adjacentPos.getZ());
               BalloonLayerGraph result = BalloonBuilder.buildBalloon(this.level, adjacentPos, this.graph);
               layer.addHotAirBlock(adjacentPos.getX(), adjacentPos.getZ());
               if (result != null) {
                  this.rebuildBalloonFromController();
                  return;
               }

               Iterable<Direction> gustDirs = shouldSpawnGust ? this.findGustDirections(pos) : null;
               if (layer.inwardConnections.isEmpty()) {
                  if (shouldSpawnGust && !gusted) {
                     for (Direction gustDir : gustDirs) {
                        this.spawnGust(this.level, pos.relative(gustDir), gustDir);
                     }
                  }

                  this.setLeaking();
                  return;
               }

               int beforeCapacity = this.capacity;

               for (BalloonLayerData removedLayer : this.graph.propagateRemoval(layer)) {
                  this.onHotAirRemoved(removedLayer::nonSolidBlockIterator);
               }

               int lostHotAir = beforeCapacity - this.capacity;
               if (lostHotAir > 0 && shouldSpawnGust && !gusted) {
                  for (Direction gustDir : gustDirs) {
                     this.spawnGust(this.level, pos.relative(gustDir), gustDir);
                  }

                  gusted = true;
               }

               this.graph.rebuildConnections(this.controllerPos);
               this.recomputeBalloonData();
            }
         }
      }
   }

   private Iterable<Direction> findGustDirections(BlockPos pos) {
      List<Direction> directions = new ObjectArrayList();

      for (Direction dir : SimDirectionUtil.VALUES) {
         Direction oppositeDir = dir.getOpposite();
         BlockState state = this.accelerator.getBlockState(pos.relative(oppositeDir));
         if ((state.isAir() || !state.is(AeroTags.BlockTags.AIRTIGHT)) && this.graph.hasBlockAt(pos.relative(dir))) {
            directions.add(oppositeDir);
         }
      }

      return directions;
   }

   public void onAirtightBlockAdded(BlockPos pos) {
      if (!this.assembling) {
         this.rebuildBalloonFromController();
      }
   }

   public abstract boolean shouldSpawnGust(BlockPos var1);

   public void spawnGust(Level level, BlockPos pos, Direction dir) {
      GustEntity.addGust(level, pos, dir);
   }

   public void setLeaking() {
   }

   protected void onRebuilt() {
   }

   protected void onHotAirAdded(BlockPos blockPos) {
   }

   protected void onHotAirRemoved(BlockPos blockPos) {
   }

   protected void onHotAirRemoved(Iterable<BlockPos> blockPos) {
   }

   protected void onHotAirAdded(Iterable<BlockPos> hotAir) {
   }

   public void moveController(BlockPos newControllerPos) {
      if (!this.controllerPos.equals(newControllerPos)) {
         boolean needsRebuild = !Objects.equals(this.graph.getLayerAt(newControllerPos), this.graph.getLayerAt(this.controllerPos));
         this.controllerPos = newControllerPos;
         if (needsRebuild) {
            this.rebuildBalloonFromController();
         }
      }
   }

   protected void rebuildBalloonFromController() {
      BalloonLayerGraph result = BalloonBuilder.buildBalloon(this.level, this.controllerPos, null);
      if (result == null) {
         this.setLeaking();
      } else {
         this.graph = result;
         this.recomputeBalloonData();
         this.onRebuilt();
      }
   }

   private void recomputeBalloonData() {
      assert this.graph != null;

      this.bounds.minY = this.graph.getMinY();
      this.bounds.maxY = this.graph.getMaxY();
      this.bounds.minX = this.controllerPos.getX();
      this.bounds.maxX = this.controllerPos.getX();
      this.bounds.minZ = this.controllerPos.getZ();
      this.bounds.maxZ = this.controllerPos.getZ();
      this.capacity = 0;

      for (int y = this.graph.getMinY(); y <= this.graph.getMaxY(); y++) {
         for (BalloonLayerData layer : this.graph.getLayersAtY(y)) {
            this.capacity = this.capacity + layer.hotAirCount;
            this.capacity = this.capacity - layer.solidCount;
            LongIterator var5 = layer.getChunks().keySet().iterator();

            while (var5.hasNext()) {
               long chunkLong = (Long)var5.next();
               int chunkOriginX = BalloonLayerData.getChunkX(chunkLong) << 3;
               int chunkOriginZ = BalloonLayerData.getChunkZ(chunkLong) << 3;
               this.bounds.expandTo(chunkOriginX, y, chunkOriginZ);
               this.bounds.expandTo(chunkOriginX + 7, y, chunkOriginZ + 7);
            }
         }
      }
   }

   public int getCapacity() {
      return this.capacity;
   }

   public void addHeater(BlockEntityLiftingGasProvider heater) {
      this.heaters.add(heater);
   }

   public void removeHeater(BlockEntityLiftingGasProvider heater) {
      this.heaters.remove(heater);
   }

   public Collection<BlockEntityLiftingGasProvider> getHeaters() {
      return this.heaters;
   }

   public float getHeight() {
      return (float)(this.bounds.maxY - this.bounds.minY + 1);
   }

   public abstract boolean isValid();

   public void onRemoved() {
      for (BlockEntityLiftingGasProvider heater : this.heaters) {
         heater.setBalloon(null);
      }
   }

   public void merge(Balloon other) {
      this.heaters.addAll(other.heaters);

      for (BlockEntityLiftingGasProvider heater : other.heaters) {
         heater.setBalloon(this);
      }
   }

   public BalloonLayerGraph getGraph() {
      return this.graph;
   }

   public BlockPos getControllerPos() {
      return this.controllerPos;
   }

   public BoundingBox3ic getBounds() {
      return this.bounds;
   }

   public void setAssembling(AssemblyTransform transform) {
      this.assembling = true;
      this.controllerPos = transform.apply(this.controllerPos);

      for (BlockEntityLiftingGasProvider heater : this.heaters) {
         heater.setBalloon(null);
      }

      this.heaters.clear();
      BlockPos minPos = transform.apply(new BlockPos(this.bounds.minX(), this.bounds.minY(), this.bounds.minZ()));
      BlockPos maxPos = transform.apply(new BlockPos(this.bounds.maxX(), this.bounds.maxY(), this.bounds.maxZ()));
      this.bounds.set(minPos.getX(), minPos.getY(), minPos.getZ(), maxPos.getX(), maxPos.getY(), maxPos.getZ());
   }

   public boolean isAssembling() {
      return this.assembling;
   }
}
