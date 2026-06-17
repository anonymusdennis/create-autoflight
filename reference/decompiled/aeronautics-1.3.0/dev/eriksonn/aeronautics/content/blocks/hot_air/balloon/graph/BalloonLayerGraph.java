package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public class BalloonLayerGraph {
   private List<BalloonLayerData>[] layerMap;
   private int minY;

   public BalloonLayerGraph(int yLevel) {
      this.minY = yLevel;
      this.layerMap = new List[]{new ObjectArrayList()};
   }

   public void addLayer(int y, BalloonLayerData layer) {
      int index = this.getIndex(y);
      if (index < 0) {
         this.resizeDownwards(y);
         index = this.getIndex(y);
      } else if (index >= this.layerMap.length) {
         this.resizeUpwards(y);
      }

      this.layerMap[index].add(layer);
   }

   public void removeLayer(BalloonLayerData layer) {
      int index = this.getIndex(layer.getYLevel());
      List<BalloonLayerData> layers = this.layerMap[index];
      layers.remove(layer);
   }

   public void trim() {
      int start = 0;
      int end = this.layerMap.length - 1;

      while (start <= end && this.layerMap[start].isEmpty()) {
         start++;
      }

      while (end >= start && this.layerMap[end].isEmpty()) {
         end--;
      }

      if (start != 0 || end != this.layerMap.length - 1) {
         int newSize = end - start + 1;
         if (newSize > 0) {
            List<BalloonLayerData>[] newLayerMap = new List[newSize];
            System.arraycopy(this.layerMap, start, newLayerMap, 0, newSize);
            this.layerMap = newLayerMap;
            this.minY += start;
         }
      }
   }

   private int getIndex(int y) {
      return y - this.minY;
   }

   public List<BalloonLayerData> getLayersAtY(int y) {
      int index = this.getIndex(y);
      return index >= 0 && index < this.layerMap.length ? this.layerMap[index] : Collections.emptyList();
   }

   private void resizeUpwards(int targetY) {
      int requiredSize = targetY - this.minY + 1;
      if (requiredSize > this.layerMap.length) {
         List<BalloonLayerData>[] newLayers = new List[requiredSize];
         System.arraycopy(this.layerMap, 0, newLayers, 0, this.layerMap.length);

         for (int i = this.layerMap.length; i < requiredSize; i++) {
            newLayers[i] = new ObjectArrayList();
         }

         this.layerMap = newLayers;
      }
   }

   private void resizeDownwards(int newMinY) {
      int deltaY = this.minY - newMinY;
      int oldLength = this.layerMap.length;
      int newLength = oldLength + deltaY;
      List<BalloonLayerData>[] newLayers = new List[newLength];

      for (int i = 0; i < deltaY; i++) {
         newLayers[i] = new ObjectArrayList();
      }

      System.arraycopy(this.layerMap, 0, newLayers, deltaY, oldLength);
      this.layerMap = newLayers;
      this.minY = newMinY;
   }

   public List<BalloonLayerData>[] getAllLayers() {
      return this.layerMap;
   }

   public int getMinY() {
      return this.minY;
   }

   public int getMaxY() {
      return this.minY + this.layerMap.length - 1;
   }

   public void addAll(BalloonLayerGraph otherGraph) {
      List<BalloonLayerData>[] otherLayerMap = otherGraph.getAllLayers();

      for (int index = 0; index < otherLayerMap.length; index++) {
         List<BalloonLayerData> layersAtY = otherLayerMap[index];
         int layerY = index + otherGraph.getMinY();

         for (BalloonLayerData otherLayer : layersAtY) {
            this.addLayer(layerY, otherLayer);
         }
      }
   }

   @Nullable
   public BalloonLayerData getLayerAt(BlockPos pos) {
      for (BalloonLayerData layer : this.getLayersAtY(pos.getY())) {
         int x = pos.getX();
         int z = pos.getZ();
         if (layer.getHotAirBlock(x, z)) {
            return layer;
         }
      }

      return null;
   }

   public boolean hasBlockAt(BlockPos pos) {
      return this.getLayerAt(pos) != null;
   }

   public void rebuildConnections(BlockPos startPos) {
      List[] startLayer = this.layerMap;
      int queue = startLayer.length;

      for (int visited = 0; visited < queue; visited++) {
         for (BalloonLayerData layer : startLayer[visited]) {
            layer.inwardConnections.clear();
            layer.outwardConnections.clear();
         }
      }

      BalloonLayerData startLayerx = this.getLayerAt(startPos);
      if (startLayerx != null) {
         ObjectArrayList<BalloonLayerData> queuex = new ObjectArrayList();
         ObjectArrayList<BalloonLayerData> visited = new ObjectArrayList();
         queuex.add(startLayerx);
         visited.add(startLayerx);

         while (!queuex.isEmpty()) {
            BalloonLayerData current = (BalloonLayerData)queuex.removeLast();
            int currentY = current.getYLevel();

            for (int dy = -1; dy <= 1; dy += 2) {
               int neighborY = currentY + dy;
               List<BalloonLayerData> neighborLayers = this.getLayersAtY(neighborY);
               if (!neighborLayers.isEmpty()) {
                  for (BalloonLayerData neighbor : neighborLayers) {
                     if (current.overlaps(neighbor) && !neighbor.outwardConnections.contains(current) && !neighbor.inwardConnections.contains(current)) {
                        current.outwardConnections.add(neighbor);
                        neighbor.inwardConnections.add(current);
                        if (!visited.contains(neighbor)) {
                           visited.add(neighbor);
                           queuex.add(neighbor);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public Iterable<BalloonLayerData> propagateRemoval(BalloonLayerData startLayer) {
      if (startLayer == null) {
         return null;
      } else {
         ObjectArrayList<BalloonLayerData> frontier = new ObjectArrayList();
         ObjectArrayList<BalloonLayerData> visited = new ObjectArrayList();
         frontier.add(startLayer);
         visited.add(startLayer);

         while (!frontier.isEmpty()) {
            BalloonLayerData current = (BalloonLayerData)frontier.removeLast();

            for (BalloonLayerData outward : current.outwardConnections) {
               if (!visited.contains(outward)) {
                  visited.add(outward);
                  frontier.add(outward);
               }
            }

            for (BalloonLayerData inward : current.inwardConnections) {
               if (current.getYLevel() > inward.getYLevel() && !visited.contains(inward)) {
                  visited.add(inward);
                  frontier.add(inward);
               }
            }
         }

         ObjectListIterator var7 = visited.iterator();

         while (var7.hasNext()) {
            BalloonLayerData layer = (BalloonLayerData)var7.next();
            this.removeLayer(layer);
         }

         this.trim();
         return visited;
      }
   }
}
