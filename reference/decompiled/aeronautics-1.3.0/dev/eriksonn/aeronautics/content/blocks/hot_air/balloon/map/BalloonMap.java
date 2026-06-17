package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map;

import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonBuilder;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BalloonMap {
   public static WorldAttached<BalloonMap> MAP = new WorldAttached(BalloonMap::new);
   private final Level level;
   private final ObjectSet<Balloon> balloons = new ObjectOpenHashSet();
   private final ObjectSet<SavedBalloon> unloadedBalloons = new ObjectOpenHashSet();
   private boolean initialized;

   public BalloonMap(LevelAccessor level) {
      this.level = (Level)level;
   }

   public static void tick(Level level) {
      ((BalloonMap)MAP.get(level)).tick();
   }

   public static void physicsTick(ServerLevel level, double timeStep) {
      BalloonMap handler = (BalloonMap)MAP.get(level);
      ObjectIterator var4 = handler.balloons.iterator();

      while (var4.hasNext()) {
         Balloon balloon = (Balloon)var4.next();
         if (balloon instanceof ServerBalloon serverBalloon) {
            serverBalloon.applyForces(timeStep);
         }
      }
   }

   public static SavedBalloon saveBalloon(ServerBalloon balloon) {
      return new SavedBalloon(new BoundingBox3i(balloon.getBounds()), balloon.getControllerPos(), balloon.getLiftingGasHolders());
   }

   public void addBalloon(Balloon balloon) {
      this.balloons.add(balloon);
   }

   public void markDirty() {
      if (this.level instanceof ServerLevel serverLevel) {
         BalloonLevelSavedData.get(serverLevel).setDirty();
      }
   }

   public void updateNearbyBalloons(BlockPos blockPos, BlockState oldState, BlockState newState) {
      boolean oldAirtight = oldState.is(AeroTags.BlockTags.AIRTIGHT);
      boolean newAirtight = newState.is(AeroTags.BlockTags.AIRTIGHT);
      if (oldAirtight != newAirtight) {
         for (Balloon balloon : this.getBalloonsNear(blockPos)) {
            if (newAirtight) {
               balloon.onAirtightBlockAdded(blockPos);
            } else {
               balloon.onAirtightBlockRemoved(blockPos);
            }
         }
      } else {
         boolean oldSolid = BalloonBuilder.isSolid(oldState);
         boolean newSolid = BalloonBuilder.isSolid(newState);
         if (oldSolid != newSolid) {
            for (Balloon balloonx : this.getBalloonsNear(blockPos)) {
               if (newSolid) {
                  balloonx.onSolidBlockAdded(blockPos);
               } else {
                  balloonx.onSolidBlockRemoved(blockPos);
               }
            }
         }
      }
   }

   public void tick() {
      if (!this.initialized) {
         if (this.level instanceof ServerLevel serverLevel) {
            BalloonLevelSavedData.get(serverLevel);
         }

         this.initialized = true;
      }

      this.balloons.forEach(Balloon::tick);
      this.removeBalloons();
      this.mergeBalloons();
      this.markDirty();
   }

   private void mergeBalloons() {
      ObjectIterator<Balloon> iter = this.balloons.iterator();

      while (iter.hasNext()) {
         Balloon balloon = (Balloon)iter.next();
         ObjectIterator var3 = this.balloons.iterator();

         while (var3.hasNext()) {
            Balloon otherBalloon = (Balloon)var3.next();
            if (balloon != otherBalloon
               && balloon.getBounds().intersects(otherBalloon.getBounds())
               && balloon.getGraph().getLayerAt(otherBalloon.getControllerPos()) != null) {
               balloon.onRemoved();
               otherBalloon.merge(balloon);
               iter.remove();
               break;
            }
         }
      }
   }

   private void removeBalloons() {
      ObjectIterator<Balloon> iter = this.balloons.iterator();

      while (iter.hasNext()) {
         Balloon balloon = (Balloon)iter.next();
         if (!balloon.isValid()) {
            iter.remove();
            balloon.onRemoved();
         }
      }
   }

   public void removeBalloon(Balloon balloon) {
      balloon.onRemoved();
   }

   @Nullable
   public Balloon getBalloon(BlockPos blockPos) {
      ObjectIterator var2 = this.balloons.iterator();

      while (var2.hasNext()) {
         Balloon balloon = (Balloon)var2.next();
         BoundingBox3ic bounds = balloon.getBounds();
         if (bounds.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ()) && balloon.getGraph().hasBlockAt(blockPos)) {
            return balloon;
         }
      }

      return null;
   }

   private Iterable<Balloon> getBalloonsNear(BlockPos blockPos) {
      ObjectList<Balloon> balloons = null;
      int padding = 24;
      ObjectIterator var4 = this.balloons.iterator();

      while (var4.hasNext()) {
         Balloon balloon = (Balloon)var4.next();
         BoundingBox3ic bounds = balloon.getBounds();
         if (blockPos.getX() > bounds.minX() - 24
            && blockPos.getY() > bounds.minY() - 24
            && blockPos.getZ() > bounds.minZ() - 24
            && blockPos.getX() < bounds.maxX() + 24
            && blockPos.getY() < bounds.maxY() + 24
            && blockPos.getZ() < bounds.maxZ() + 24) {
            if (balloons == null) {
               balloons = new ObjectArrayList();
            }

            balloons.add(balloon);
         }
      }

      return (Iterable<Balloon>)(balloons != null ? balloons : List.of());
   }

   public Iterable<Balloon> getBalloons() {
      return this.balloons;
   }

   public boolean isEmpty() {
      return this.balloons.isEmpty();
   }

   public void unloadBalloon(ServerBalloon balloon) {
      SavedBalloon unloadedBalloon = saveBalloon(balloon);
      this.balloons.remove(balloon);
      this.unloadedBalloons.add(unloadedBalloon);
      this.markDirty();
   }

   public Collection<SavedBalloon> getUnloadedBalloons() {
      return this.unloadedBalloons;
   }

   public static class BalloonSubLevelObserver implements SubLevelObserver {
      private final Level level;

      public BalloonSubLevelObserver(Level level) {
         this.level = level;
      }

      public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
         if (reason == SubLevelRemovalReason.REMOVED) {
            LevelPlot plot = subLevel.getPlot();
            BalloonMap map = (BalloonMap)BalloonMap.MAP.get(this.level);
            Iterator<Balloon> iter = map.getBalloons().iterator();

            while (iter.hasNext()) {
               Balloon balloon = iter.next();
               if (!balloon.isAssembling()) {
                  BlockPos controllerPos = balloon.getControllerPos();
                  if (plot.contains((double)controllerPos.getX(), (double)controllerPos.getZ())) {
                     balloon.onRemoved();
                     iter.remove();
                  }
               }
            }
         }
      }
   }
}
