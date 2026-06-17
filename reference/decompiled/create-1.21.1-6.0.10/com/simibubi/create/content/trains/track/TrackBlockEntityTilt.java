package com.simibubi.create.content.trains.track;

import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class TrackBlockEntityTilt {
   public static final ModelProperty<Double> ASCENDING_PROPERTY = new ModelProperty();
   public Optional<Double> smoothingAngle;
   private Couple<Pair<Vec3, Integer>> previousSmoothingHandles;
   private TrackBlockEntity blockEntity;

   public TrackBlockEntityTilt(TrackBlockEntity blockEntity) {
      this.blockEntity = blockEntity;
      this.smoothingAngle = Optional.empty();
   }

   public void tryApplySmoothing() {
      if (!this.smoothingAngle.isPresent()) {
         Couple<BezierConnection> discoveredSlopes = Couple.create(null, null);
         Vec3 axis = null;
         BlockState blockState = this.blockEntity.getBlockState();
         BlockPos worldPosition = this.blockEntity.getBlockPos();
         Level level = this.blockEntity.getLevel();
         if (blockState.getBlock() instanceof ITrackBlock itb) {
            List<Vec3> axes = itb.getTrackAxes(level, worldPosition, blockState);
            if (axes.size() == 1) {
               if (axes.get(0).y == 0.0) {
                  if (this.blockEntity.boundLocation == null) {
                     for (BezierConnection bezierConnection : this.blockEntity.connections.values()) {
                        if (((Vec3)bezierConnection.starts.getFirst()).y != ((Vec3)bezierConnection.starts.getSecond()).y) {
                           Vec3 normedAxis = ((Vec3)bezierConnection.axes.getFirst()).normalize();
                           if (axis != null) {
                              if (discoveredSlopes.getSecond() != null) {
                                 return;
                              }

                              if (normedAxis.dot(axis) > -0.984375) {
                                 return;
                              }

                              discoveredSlopes.setSecond(bezierConnection);
                           } else {
                              axis = normedAxis;
                              discoveredSlopes.setFirst(bezierConnection);
                           }
                        }
                     }

                     if (!discoveredSlopes.either(Objects::isNull)) {
                        if (((Vec3)((BezierConnection)discoveredSlopes.getFirst()).starts.getSecond()).y
                           > ((Vec3)((BezierConnection)discoveredSlopes.getSecond()).starts.getSecond()).y) {
                           discoveredSlopes = discoveredSlopes.swap();
                        }

                        Couple<Vec3> lowStarts = ((BezierConnection)discoveredSlopes.getFirst()).starts;
                        Couple<Vec3> highStarts = ((BezierConnection)discoveredSlopes.getSecond()).starts;
                        Vec3 lowestPoint = (Vec3)lowStarts.getSecond();
                        Vec3 highestPoint = (Vec3)highStarts.getSecond();
                        if (!(lowestPoint.y > ((Vec3)lowStarts.getFirst()).y)) {
                           if (!(highestPoint.y < ((Vec3)highStarts.getFirst()).y)) {
                              this.blockEntity.removeInboundConnections(false);
                              this.blockEntity.connections.clear();
                              TrackPropagator.onRailRemoved(level, worldPosition, blockState);
                              double hDistance = ((BezierConnection)discoveredSlopes.getFirst()).getLength()
                                 + ((BezierConnection)discoveredSlopes.getSecond()).getLength();
                              Vec3 baseAxis = (Vec3)((BezierConnection)discoveredSlopes.getFirst()).axes.getFirst();
                              double baseAxisLength = baseAxis.x != 0.0 && baseAxis.z != 0.0 ? Math.sqrt(2.0) : 1.0;
                              double vDistance = highestPoint.y - lowestPoint.y;
                              double m = vDistance / hDistance;
                              Vec3 diff = ((Vec3)highStarts.getFirst()).subtract((Vec3)lowStarts.getFirst());
                              boolean flipRotation = diff.dot(new Vec3(1.0, 0.0, 2.0).normalize()) <= 0.0;
                              this.smoothingAngle = Optional.of(Math.toDegrees(Mth.atan2(m, 1.0)) * (double)(flipRotation ? -1 : 1));
                              int smoothingParam = Mth.clamp((int)(m * baseAxisLength * 16.0), 0, 15);
                              Couple<Integer> smoothingResult = Couple.create(0, smoothingParam);
                              Vec3 raisedOffset = diff.normalize().add(0.0, Mth.clamp(m, 0.0, 0.9980469F), 0.0).normalize().scale(baseAxisLength);
                              highStarts.setFirst(((Vec3)lowStarts.getFirst()).add(raisedOffset));
                              boolean first = true;

                              for (BezierConnection bezierConnectionx : discoveredSlopes) {
                                 int smoothingToApply = (Integer)smoothingResult.get(first);
                                 if (bezierConnectionx.smoothing == null) {
                                    bezierConnectionx.smoothing = Couple.create(0, 0);
                                 }

                                 bezierConnectionx.smoothing.setFirst(smoothingToApply);
                                 bezierConnectionx.axes
                                    .setFirst(((Vec3)bezierConnectionx.axes.getFirst()).add(0.0, (double)(first ? 1 : -1) * -m, 0.0).normalize());
                                 first = false;
                                 BlockPos otherPosition = bezierConnectionx.getKey();
                                 BlockState otherState = level.getBlockState(otherPosition);
                                 if (otherState.getBlock() instanceof TrackBlock) {
                                    level.setBlockAndUpdate(otherPosition, (BlockState)otherState.setValue(TrackBlock.HAS_BE, true));
                                    if (level.getBlockEntity(otherPosition) instanceof TrackBlockEntity tbe) {
                                       this.blockEntity.addConnection(bezierConnectionx);
                                       tbe.addConnection(bezierConnectionx.secondary());
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void captureSmoothingHandles() {
      boolean first = true;
      this.previousSmoothingHandles = Couple.create(null, null);

      for (BezierConnection bezierConnection : this.blockEntity.connections.values()) {
         this.previousSmoothingHandles
            .set(
               first,
               Pair.of((Vec3)bezierConnection.starts.getFirst(), bezierConnection.smoothing == null ? 0 : (Integer)bezierConnection.smoothing.getFirst())
            );
         first = false;
      }
   }

   public void undoSmoothing() {
      if (!this.smoothingAngle.isEmpty()) {
         if (this.previousSmoothingHandles != null) {
            if (this.blockEntity.connections.size() != 2) {
               BlockState blockState = this.blockEntity.getBlockState();
               BlockPos worldPosition = this.blockEntity.getBlockPos();
               Level level = this.blockEntity.getLevel();
               List<BezierConnection> validConnections = new ArrayList<>();

               for (BezierConnection bezierConnection : this.blockEntity.connections.values()) {
                  BlockPos otherPosition = bezierConnection.getKey();
                  BlockEntity otherBE = level.getBlockEntity(otherPosition);
                  if (otherBE instanceof TrackBlockEntity) {
                     TrackBlockEntity tbe = (TrackBlockEntity)otherBE;
                     if (tbe.connections.containsKey(worldPosition)) {
                        validConnections.add(bezierConnection);
                     }
                  }
               }

               this.blockEntity.removeInboundConnections(false);
               TrackPropagator.onRailRemoved(level, worldPosition, blockState);
               this.blockEntity.connections.clear();
               this.smoothingAngle = Optional.empty();

               for (BezierConnection bezierConnectionx : validConnections) {
                  this.blockEntity.addConnection(this.restoreToOriginalCurve(bezierConnectionx));
                  BlockPos otherPosition = bezierConnectionx.getKey();
                  BlockState otherState = level.getBlockState(otherPosition);
                  if (otherState.getBlock() instanceof TrackBlock) {
                     level.setBlockAndUpdate(otherPosition, (BlockState)otherState.setValue(TrackBlock.HAS_BE, true));
                     if (level.getBlockEntity(otherPosition) instanceof TrackBlockEntity tbe) {
                        tbe.addConnection(bezierConnectionx.secondary());
                     }
                  }
               }

               this.blockEntity.notifyUpdate();
               this.previousSmoothingHandles = null;
               TrackPropagator.onRailAdded(level, worldPosition, blockState);
            }
         }
      }
   }

   public BezierConnection restoreToOriginalCurve(BezierConnection bezierConnection) {
      if (bezierConnection.smoothing != null) {
         bezierConnection.smoothing.setFirst(0);
         if ((Integer)bezierConnection.smoothing.getFirst() == 0 && (Integer)bezierConnection.smoothing.getSecond() == 0) {
            bezierConnection.smoothing = null;
         }
      }

      Vec3 raisedStart = (Vec3)bezierConnection.starts.getFirst();
      bezierConnection.starts.setFirst(new TrackNodeLocation(raisedStart).getLocation());
      bezierConnection.axes.setFirst(((Vec3)bezierConnection.axes.getFirst()).multiply(1.0, 0.0, 1.0).normalize());
      return bezierConnection;
   }

   public int getYOffsetForAxisEnd(Vec3 end) {
      if (this.smoothingAngle.isEmpty()) {
         return 0;
      } else {
         for (BezierConnection bezierConnection : this.blockEntity.connections.values()) {
            if (compareHandles((Vec3)bezierConnection.starts.getFirst(), end)) {
               return bezierConnection.yOffsetAt(end);
            }
         }

         if (this.previousSmoothingHandles == null) {
            return 0;
         } else {
            for (Pair<Vec3, Integer> handle : this.previousSmoothingHandles) {
               if (handle != null && compareHandles((Vec3)handle.getFirst(), end)) {
                  return (Integer)handle.getSecond();
               }
            }

            return 0;
         }
      }
   }

   public static boolean compareHandles(Vec3 handle1, Vec3 handle2) {
      return new TrackNodeLocation(handle1)
            .getLocation()
            .multiply(1.0, 0.0, 1.0)
            .distanceToSqr(new TrackNodeLocation(handle2).getLocation().multiply(1.0, 0.0, 1.0))
         < 0.001953125;
   }
}
