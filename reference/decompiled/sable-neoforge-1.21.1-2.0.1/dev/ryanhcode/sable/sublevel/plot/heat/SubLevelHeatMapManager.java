package dev.ryanhcode.sable.sublevel.plot.heat;

import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.HeatDataChunkSection;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class SubLevelHeatMapManager {
   private static final Collection<SubLevelHeatMapManager.SplitListener> LISTENERS = new ObjectArraySet();
   private static final BlockPos[] DIRECTION_OFFSETS = new BlockPos[]{
      new BlockPos(1, 0, 0),
      new BlockPos(-1, 0, 0),
      new BlockPos(0, 1, 0),
      new BlockPos(0, -1, 0),
      new BlockPos(0, 0, 1),
      new BlockPos(0, 0, -1),
      new BlockPos(1, 1, 0),
      new BlockPos(-1, -1, 0),
      new BlockPos(1, -1, 0),
      new BlockPos(-1, 1, 0),
      new BlockPos(1, 0, 1),
      new BlockPos(-1, 0, -1),
      new BlockPos(1, 0, -1),
      new BlockPos(-1, 0, 1),
      new BlockPos(0, 1, 1),
      new BlockPos(0, -1, -1),
      new BlockPos(0, -1, 1),
      new BlockPos(0, 1, -1)
   };
   @NotNull
   private final ServerSubLevel subLevel;
   private final Long2IntOpenHashMap subLevelSplits = new Long2IntOpenHashMap();
   private final ObjectList<BlockPos> floodfill = new ObjectArrayList();
   private final ObjectList<BlockPos> removed = new ObjectArrayList();
   private final ObjectList<BlockPos> newStarts = new ObjectArrayList();
   private final IntArrayList splitIndexMap = new IntArrayList();
   private HeatMapPropagationState state = HeatMapPropagationState.FILLING;
   private boolean initialized = false;
   private boolean splitComplete = false;
   private int solidCount = 0;

   public SubLevelHeatMapManager(@NotNull ServerSubLevel subLevel) {
      this.subLevel = subLevel;
   }

   public void tick() {
      int steps = SableConfig.SUB_LEVEL_SPLITTING_HEATMAP_STEPS_PER_TICK.getAsInt();
      int i = 0;

      while (i < steps && !this.step()) {
         i++;
      }
   }

   private boolean step() {
      if (this.state == HeatMapPropagationState.FILLING) {
         if (!this.floodfill.isEmpty()) {
            BlockPos p = new BlockPos((Vec3i)this.floodfill.getFirst());
            this.floodfill.removeFirst();
            if (this.heatMapContains(p)) {
               int currentHeat = this.heatMapGet(p);

               for (BlockPos dir : DIRECTION_OFFSETS) {
                  BlockPos p2 = p.offset(dir);
                  boolean solid = this.isSolidAt(p2);
                  boolean contains = this.heatMapContains(p2);
                  if (solid && !contains) {
                     this.heatMapSet(p2, (short)(currentHeat + 1));
                     this.subLevelSplits.remove(p2.asLong());
                     this.floodfill.add(p2);
                  }
               }
            }
         }

         if (this.floodfill.isEmpty()) {
            this.splitComplete = true;
            this.state = HeatMapPropagationState.CLEARING;
            if (!this.subLevelSplits.isEmpty()) {
               this.split();
            }
         }
      }

      if (this.state == HeatMapPropagationState.CLEARING) {
         if (!this.floodfill.isEmpty()) {
            BlockPos p = new BlockPos((Vec3i)this.floodfill.getFirst());
            this.floodfill.removeFirst();
            if (this.heatMapContains(p)) {
               int currentHeat = this.heatMapGet(p);
               int currentIndex = this.splitIndexMap.getInt(this.subLevelSplits.get(p.asLong()));

               for (BlockPos dirx : DIRECTION_OFFSETS) {
                  BlockPos p2 = p.offset(dirx);
                  if (this.isSolidAt(p2)) {
                     if (this.subLevelSplits.containsKey(p2.asLong())) {
                        int otherIndex = this.splitIndexMap.getInt(this.subLevelSplits.get(p2.asLong()));
                        if (currentIndex != otherIndex) {
                           this.splitIndexMap.set(this.subLevelSplits.get(p2.asLong()), currentIndex);
                        }
                     }

                     if (this.heatMapContains(p2)) {
                        if (this.heatMapGet(p2) > currentHeat) {
                           this.floodfill.add(p2);
                           this.subLevelSplits.put(p2.asLong(), currentIndex);
                        } else {
                           this.newStarts.add(p2);
                        }
                     }
                  }
               }

               this.heatMapRemove(p);
            }
         } else if (!this.removed.isEmpty()) {
            ObjectListIterator var17 = this.removed.iterator();

            while (var17.hasNext()) {
               BlockPos index = (BlockPos)var17.next();
               BlockPos p = new BlockPos(index);
               if (this.heatMapContains(p)) {
                  int currentHeat = this.heatMapGet(p);

                  for (BlockPos dirxx : DIRECTION_OFFSETS) {
                     BlockPos p2 = p.offset(dirxx);
                     if (this.isSolidAt(p2) && this.heatMapContains(p2) && this.heatMapGet(p2) > currentHeat) {
                        boolean canRemove = true;

                        for (BlockPos dir2 : DIRECTION_OFFSETS) {
                           if (!new BlockPos(-dirxx.getX(), -dirxx.getY(), -dirxx.getZ()).equals(dir2)) {
                              BlockPos p3 = p2.offset(dir2);
                              if (this.isSolidAt(p3) && this.heatMapContains(p3) && this.heatMapGet(p3) < this.heatMapGet(p2)) {
                                 canRemove = false;
                              }
                           }
                        }

                        if (canRemove) {
                           this.floodfill.add(p2);
                           int newIndex = this.splitIndexMap.size();
                           this.subLevelSplits.put(p2.asLong(), newIndex);
                           this.splitIndexMap.add(newIndex);
                        }
                     }
                  }
               }

               this.heatMapRemove(p);
            }

            this.removed.clear();
         } else if (!this.newStarts.isEmpty()) {
            this.floodfill.addAll(this.newStarts);
            this.newStarts.clear();
            this.state = HeatMapPropagationState.FILLING;
         } else {
            if (this.subLevelSplits.isEmpty()) {
               this.splitComplete = true;
               return true;
            }

            this.splitComplete = true;
            this.split();
         }
      }

      return false;
   }

   private void split() {
      Int2ObjectMap<List<BlockPos>> newSubLevelBlocks = new Int2ObjectOpenHashMap();
      LongIterator splittingWholeSubLevel = this.subLevelSplits.keySet().iterator();

      while (splittingWholeSubLevel.hasNext()) {
         long l = (Long)splittingWholeSubLevel.next();
         int splitIndex = this.splitIndexMap.get(this.subLevelSplits.get(l));
         ((List)newSubLevelBlocks.computeIfAbsent(splitIndex, x -> new ObjectArrayList())).add(BlockPos.of(l));
      }

      boolean splittingWholeSubLevelx = newSubLevelBlocks.size() == 1
         && this.solidCount == ((List)newSubLevelBlocks.values().stream().findFirst().orElseThrow()).size();
      if (splittingWholeSubLevelx) {
         List<BlockPos> allBlocks = (List<BlockPos>)newSubLevelBlocks.values().stream().findFirst().orElseThrow();
         this.rebuildHeatmapFrom(allBlocks);
         newSubLevelBlocks.clear();
      }

      int totalSplitBlocks = 0;
      ObjectIterator level = newSubLevelBlocks.values().iterator();

      while (level.hasNext()) {
         List<BlockPos> blocks = (List<BlockPos>)level.next();
         totalSplitBlocks += blocks.size();
      }

      if (!splittingWholeSubLevelx && totalSplitBlocks != 0 && totalSplitBlocks == this.solidCount) {
         Entry<Integer, List<BlockPos>> minSize = newSubLevelBlocks.entrySet()
            .stream()
            .sorted(Comparator.comparingInt(a -> -a.getValue().size()))
            .findFirst()
            .orElseThrow();
         this.rebuildHeatmapFrom(minSize.getValue());
         newSubLevelBlocks.remove(minSize.getKey());
      }

      this.subLevelSplits.clear();
      this.splitIndexMap.clear();
      this.splitIndexMap.add(0);
      Level levelx = this.subLevel.getLevel();
      ObjectIterator var16 = newSubLevelBlocks.values().iterator();

      while (var16.hasNext()) {
         List<BlockPos> blocks = (List<BlockPos>)var16.next();
         BoundingBox3i bounds = Objects.requireNonNull(BoundingBox3i.from(blocks)).expand(1, 1, 1);

         for (SubLevelHeatMapManager.SplitListener listener : LISTENERS) {
            listener.addBlocks(levelx, bounds, blocks);
         }

         ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks((ServerLevel)levelx, blocks.get(0), blocks, bounds);
         if (subLevel.getSelfMassTracker().getCenterOfMass() == null || subLevel.getSelfMassTracker().getMass() <= 0.0) {
            subLevel.getPlot().destroyAllBlocks();
            SubLevelContainer container = Objects.requireNonNull(SubLevelContainer.getContainer(levelx));
            container.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
         }
      }
   }

   private void rebuildHeatmapFrom(List<BlockPos> blocks) {
      this.state = HeatMapPropagationState.FILLING;
      this.initialized = false;
      this.splitComplete = false;
      this.solidCount = 0;
      this.newStarts.clear();
      this.floodfill.clear();
      this.removed.clear();
      blocks.forEach(this::heatMapRemove);
      blocks.forEach(this::onSolidAdded);
   }

   private boolean isSolidAt(BlockPos blockPos) {
      Level level = this.subLevel.getLevel();
      return !level.getBlockState(blockPos).isAir();
   }

   public void onSolidAdded(BlockPos blockPos) {
      this.solidCount++;
      if (!this.initialized) {
         this.initialized = true;
         this.heatMapSet(blockPos, (short)1);
         this.floodfill.add(blockPos);
         this.splitIndexMap.add(0);
      } else {
         int minimumAdjacentHeat = Integer.MAX_VALUE;
         if (!this.removed.remove(blockPos)) {
            for (BlockPos direction : DIRECTION_OFFSETS) {
               BlockPos neighbor = blockPos.offset(direction);
               if (this.heatMapContains(neighbor)) {
                  short heat = this.heatMapGet(neighbor);
                  if (heat < minimumAdjacentHeat) {
                     minimumAdjacentHeat = heat;
                  }
               }
            }

            if (minimumAdjacentHeat == Integer.MAX_VALUE) {
               if (!this.splitComplete) {
                  this.subLevelSplits.put(blockPos.asLong(), 0);
               }
            } else {
               this.heatMapSet(blockPos, (short)(minimumAdjacentHeat + 1));
               if (this.state == HeatMapPropagationState.FILLING) {
                  this.floodfill.add(blockPos);
               } else {
                  this.newStarts.add(blockPos);
               }
            }
         }
      }
   }

   public void onSolidRemoved(BlockPos blockPos) {
      this.solidCount--;
      this.removed.add(blockPos);
   }

   private void heatMapRemove(BlockPos blockPos) {
      this.heatMapSet(blockPos, (short)0);
   }

   private boolean heatMapContains(BlockPos neighbor) {
      return this.heatMapGet(neighbor) != 0;
   }

   private short heatMapGet(BlockPos blockPos) {
      LevelPlot plot = this.subLevel.getPlot();
      SectionPos section = SectionPos.of(blockPos);
      PlotChunkHolder chunkHolder = plot.getChunkHolder(plot.toLocal(section.chunk()));
      if (chunkHolder == null) {
         return 0;
      } else {
         HeatDataChunkSection heatSection = chunkHolder.getHeatSection(section.y());
         return heatSection == null ? 0 : heatSection.get(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
      }
   }

   private void heatMapSet(BlockPos blockPos, short value) {
      LevelPlot plot = this.subLevel.getPlot();
      SectionPos section = SectionPos.of(blockPos);
      PlotChunkHolder chunkHolder = plot.getChunkHolder(plot.toLocal(section.chunk()));
      if (chunkHolder != null) {
         HeatDataChunkSection heatSection = chunkHolder.getHeatSection(section.y());
         if (heatSection == null) {
            heatSection = new HeatDataChunkSection();
            chunkHolder.setHeatSection(section.y(), heatSection);
         }

         heatSection.set(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15, value);
      }
   }

   public static void addSplitListener(SubLevelHeatMapManager.SplitListener listener) {
      LISTENERS.add(listener);
   }

   @FunctionalInterface
   public interface SplitListener {
      void addBlocks(Level var1, BoundingBox3ic var2, Collection<BlockPos> var3);
   }
}
