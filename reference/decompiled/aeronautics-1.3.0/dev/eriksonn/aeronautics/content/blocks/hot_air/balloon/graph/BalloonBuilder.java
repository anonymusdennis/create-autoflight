package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph;

import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ClientBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.ryanhcode.sable.util.LevelAccelerator;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BalloonBuilder {
   protected static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

   public static boolean isCandidatePosition(
      BlockPos pos, BlockState state, BalloonLayerGraph graph, BalloonLayerGraph existingGraph, BalloonLayerGraph mainGraph
   ) {
      if (containsBlockAt(pos, graph)) {
         return false;
      } else if (existingGraph != null && containsBlockAt(pos, existingGraph)) {
         return false;
      } else {
         return mainGraph != null && containsBlockAt(pos, mainGraph) ? false : state.isAir() || !state.is(AeroTags.BlockTags.AIRTIGHT);
      }
   }

   public static boolean isCandidatePosition(
      LevelAccelerator accelerator, BlockPos pos, BalloonLayerGraph graph, BalloonLayerGraph existingGraph, BalloonLayerGraph mainGraph
   ) {
      BlockState state = accelerator.getBlockState(pos);
      return isCandidatePosition(pos, state, graph, existingGraph, mainGraph);
   }

   private static boolean containsBlockAt(BlockPos pos, BalloonLayerGraph graph) {
      for (BalloonLayerData layer : graph.getLayersAtY(pos.getY())) {
         if (layer.getHotAirBlock(pos.getX(), pos.getZ())) {
            return true;
         }
      }

      return false;
   }

   public static BalloonLayerGraph buildBalloon(Level level, BlockPos startPos, @Nullable BalloonLayerGraph mainGraph) {
      LevelAccelerator accelerator = new LevelAccelerator(level);
      BalloonLayerGraph graph = new BalloonLayerGraph(startPos.getY());
      boolean firstSafe = upwardsBiasedFloodFill(accelerator, startPos, graph, null, mainGraph);
      if (!firstSafe) {
         return null;
      } else {
         completeBalloon(accelerator, graph, mainGraph);
         graph.rebuildConnections(startPos);
         return graph;
      }
   }

   public static void completeBalloon(LevelAccelerator accelerator, BalloonLayerGraph graph, @Nullable BalloonLayerGraph mainGraph) {
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      boolean progressMade = true;

      while (progressMade) {
         progressMade = false;
         List<BalloonLayerData>[] layerMap = graph.getAllLayers();
         int minY = graph.getMinY();
         int maxY = minY + layerMap.length;

         for (int layerY = minY; layerY < maxY; layerY++) {
            List<BalloonLayerData> layersAtY = layerMap[layerY - minY];
            ObjectListIterator var10 = new ObjectArrayList(layersAtY).iterator();

            while (var10.hasNext()) {
               BalloonLayerData layer = (BalloonLayerData)var10.next();
               if (layer.getState() != BalloonLayerData.State.COMPLETE) {
                  LongLinkedOpenHashSet queue = new LongLinkedOpenHashSet();
                  Iterator<BlockPos> candidates = layer.blockIterator();

                  while (candidates.hasNext()) {
                     queue.add(mutableBlockPos.set((Vec3i)candidates.next()).move(0, -1, 0).asLong());
                  }

                  while (!queue.isEmpty()) {
                     long l = queue.removeFirstLong();
                     BlockPos candidate = mutableBlockPos.set(l);
                     BalloonLayerGraph newGraph = new BalloonLayerGraph(candidate.getY());
                     if (isCandidatePosition(accelerator, candidate, newGraph, graph, mainGraph)) {
                        boolean safe = upwardsBiasedFloodFill(accelerator, candidate, newGraph, graph, mainGraph);
                        if (!safe) {
                           removeReachable(accelerator, queue, mutableBlockPos);
                        } else {
                           List<BalloonLayerData> newLayersAtY = newGraph.getLayersAtY(candidate.getY());
                           queue.removeIf(x -> {
                              for (BalloonLayerData layerData : newLayersAtY) {
                                 if (layerData.getHotAirBlock(BlockPos.getX(x), BlockPos.getZ(x))) {
                                    return true;
                                 }
                              }

                              return false;
                           });
                           graph.addAll(newGraph);
                           progressMade = true;
                        }
                     }
                  }

                  layer.setState(BalloonLayerData.State.COMPLETE);
               }
            }
         }
      }
   }

   private static void removeReachable(LevelAccelerator accelerator, LongLinkedOpenHashSet queue, BlockPos floodfillOrigin) {
      LongLinkedOpenHashSet visited = new LongLinkedOpenHashSet();
      LongLinkedOpenHashSet frontier = new LongLinkedOpenHashSet();
      long startLong = floodfillOrigin.asLong();
      frontier.add(startLong);
      visited.add(startLong);
      queue.remove(startLong);
      MutableBlockPos currentPos = new MutableBlockPos();
      MutableBlockPos adjacentPos = new MutableBlockPos();

      while (!frontier.isEmpty()) {
         long current = frontier.removeFirstLong();
         currentPos.set(current);

         for (Direction dir : HORIZONTAL_DIRECTIONS) {
            adjacentPos.setWithOffset(currentPos, dir);
            long neighborLong = adjacentPos.asLong();
            if (queue.contains(neighborLong) && !visited.contains(neighborLong)) {
               BlockState neighborState = accelerator.getBlockState(adjacentPos);
               if (neighborState.isAir() || !neighborState.is(AeroTags.BlockTags.AIRTIGHT)) {
                  queue.remove(neighborLong);
                  frontier.add(neighborLong);
                  visited.add(neighborLong);
               }
            }
         }
      }
   }

   public static boolean upwardsBiasedFloodFill(
      LevelAccelerator accelerator, BlockPos startPos, BalloonLayerGraph outputGraph, BalloonLayerGraph existingGraph, BalloonLayerGraph mainGraph
   ) {
      if (accelerator.isOutsideBuildHeight(startPos)) {
         return false;
      } else {
         long startPosLong = startPos.asLong();
         if (!isCandidatePosition(accelerator, startPos, outputGraph, existingGraph, mainGraph)) {
            return false;
         } else {
            LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
            LongSet visited = new LongOpenHashSet();
            queue.enqueue(startPosLong);
            visited.add(startPosLong);
            MutableBlockPos currentPos = new MutableBlockPos();
            BalloonLayerData newLayer = new BalloonLayerData(startPos.getY());
            int yLevel = startPos.getY();

            while (!queue.isEmpty()) {
               long currentPosLong = queue.dequeueLastLong();
               currentPos.set(currentPosLong);
               BlockState state = accelerator.getBlockState(currentPos);
               if (isCandidatePosition(currentPos, state, outputGraph, existingGraph, mainGraph)) {
                  newLayer.hotAirCount++;
                  newLayer.addHotAirBlock(currentPos.getX(), currentPos.getZ());
                  if (isSolid(state)) {
                     newLayer.solidCount++;
                     newLayer.addSolidBlock(currentPos.getX(), currentPos.getZ());
                  }

                  BlockPos posAbove = currentPos.above();
                  long posAboveLong = currentPosLong + 1L;
                  if (!visited.contains(posAboveLong)
                     && isCandidatePosition(accelerator, posAbove, outputGraph, existingGraph, mainGraph)
                     && !upwardsBiasedFloodFill(accelerator, posAbove, outputGraph, existingGraph, mainGraph)) {
                     return false;
                  }

                  for (Direction direction : HORIZONTAL_DIRECTIONS) {
                     BlockPos neighborPos = currentPos.relative(direction);
                     long neighborPosLong = neighborPos.asLong();
                     if (!visited.contains(neighborPosLong)) {
                        visited.add(neighborPosLong);
                        queue.enqueue(neighborPosLong);
                     }
                  }
               }
            }

            outputGraph.addLayer(yLevel, newLayer);
            return true;
         }
      }
   }

   public static Balloon attemptBuildBalloon(BlockEntityLiftingGasProvider heater, BlockPos startPos) {
      Level level = heater.getLevel();
      LevelAccelerator accelerator = new LevelAccelerator(level);
      ObjectArrayList<BlockEntityLiftingGasProvider> heaters = new ObjectArrayList();
      heaters.add(heater);
      BalloonLayerGraph graph = buildBalloon(level, startPos, null);
      if (graph == null) {
         return null;
      } else {
         return (Balloon)(level instanceof ServerLevel
            ? new ServerBalloon(level, accelerator, startPos, graph, heaters)
            : new ClientBalloon(level, accelerator, startPos, graph, heaters));
      }
   }

   public static boolean isSolid(BlockState state) {
      return !state.isAir();
   }
}
