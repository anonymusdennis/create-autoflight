package dev.ryanhcode.sable.sublevel.entity_collision;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.mixinterface.EntityExtension;
import dev.ryanhcode.sable.mixinterface.voxel_shape_iteration.FastVoxelShapeIterable;
import dev.ryanhcode.sable.physics.impl.SubLevelEntityCollisionContext;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.ryanhcode.sable.util.SableMathUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SubLevelEntityCollision {
   public static SubLevelEntityCollision.CollisionInfo collide(Entity entity, Vec3 collisionMotionMoj, Vec3 velocityMotionMoj, LevelReusedVectors sink) {
      if (entity instanceof ServerPlayer) {
         SubLevelEntityCollision.CollisionInfo collisionInfo = new SubLevelEntityCollision.CollisionInfo();
         collisionInfo.motion = collisionMotionMoj;
         SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(entity);
         if (trackingSubLevel != null) {
            entity.setOnGround(true);
            collisionInfo.verticalCollisionBelow = true;
            collisionInfo.verticalCollision = true;
            collisionInfo.trackingSubLevel = trackingSubLevel;
            if (entity.getDeltaMovement().y < 0.0) {
               entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
            }
         }

         return collisionInfo;
      } else {
         SubLevel existingTrackingSubLevel = Sable.HELPER.getTrackingSubLevel(entity);
         if (existingTrackingSubLevel != null && EntitySubLevelUtil.shouldKick(entity) && existingTrackingSubLevel.getPlot().contains(entity.position())) {
            EntitySubLevelUtil.kickEntity(existingTrackingSubLevel, entity);
         }

         BoundingBox3d fullContextBounds = sink.fullContextBounds.set(entity.getBoundingBox().minmax(entity.getBoundingBox().move(collisionMotionMoj)));
         BoundingBox3d rotatedContextBounds = sink.rotatedContextBounds;
         AABB entityBounds = entity.getBoundingBox();
         Vector3d collisionMotion = sink.collisionMotion.set(collisionMotionMoj.x, collisionMotionMoj.y, collisionMotionMoj.z);
         Vector3d velocityMotion = sink.velocityMotion.set(velocityMotionMoj.x, velocityMotionMoj.y, velocityMotionMoj.z);
         Level level = entity.level();
         LevelAccelerator accel = new LevelAccelerator(level);
         Quaterniondc customEntityOrientation = EntitySubLevelUtil.getCustomEntityOrientation(entity, 0.0F);
         sink.entityUpDirection.set(OrientedBoundingBox3d.UP);
         BoundingBox3d considerationBounds = sink.considerationBounds.set(fullContextBounds);
         if (customEntityOrientation != null) {
            customEntityOrientation.transform(sink.entityUpDirection);
            considerationBounds.expand((double)entity.getEyeHeight());
         }

         considerationBounds.expand(1.0);
         ObjectSet<SubLevel> intersecting = new ObjectOpenHashSet();

         for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, considerationBounds)) {
            intersecting.add(subLevel);
         }

         SubLevelEntityCollision.CollisionInfo collisionInfo = new SubLevelEntityCollision.CollisionInfo();
         collisionInfo.trackingSubLevel = existingTrackingSubLevel;
         if (collisionInfo.trackingSubLevel != null) {
            intersecting.add(collisionInfo.trackingSubLevel);
         }

         if (!intersecting.iterator().hasNext()) {
            collisionInfo.motion = collisionMotionMoj.add(velocityMotionMoj);
            return collisionInfo;
         } else {
            BoundingBox3d localBounds = sink.localBounds;
            BoundingBox3d localBounds2 = sink.localBounds2;
            int substeps = Math.min(10, Math.max(1, (int)(collisionMotion.length() / 0.015625)));
            if (entity instanceof Player player && player.isLocalPlayer()) {
               substeps = 8;
            }

            Vec3 originalEntityPosition = entity.position();
            Vector3dc originalEntityFootPosition = Sable.HELPER.getFeetPos(entity, 0.0F, customEntityOrientation);
            Vector3d entityBoundsCenter = JOMLConversion.getAABBCenter(entityBounds, sink.entityBoundsCenter);
            transformEntityBoundsCenter(sink, customEntityOrientation, entity, entityBoundsCenter);
            sink.entityBoxOrientation.identity();
            OrientedBoundingBox3d entityBoundsOBB = new OrientedBoundingBox3d(
               entityBoundsCenter.x + collisionMotion.x + velocityMotion.x,
               entityBoundsCenter.y + collisionMotion.y + velocityMotion.y,
               entityBoundsCenter.z + collisionMotion.z + velocityMotion.z,
               entityBounds.getXsize(),
               entityBounds.getYsize(),
               entityBounds.getZsize(),
               sink.entityBoxOrientation,
               sink
            );
            OrientedBoundingBox3d cubeOBB = new OrientedBoundingBox3d(sink);
            Pose3d lastPose = sink.lastPose;
            Pose3d lastSubLevelPose = sink.lastSubLevelPose;
            Pose3d subLevelPose = sink.subLevelPose;
            Matrix4d bakedMatrix = sink.bakedMatrix;
            Vector3d mtv = sink.mtv;
            Vector3d normalizedMtv = sink.normalizedMtv;
            Vector3d existingDeltaMovement = sink.existingDeltaMovement;
            Vector3d maxMTV = sink.maxMTV;
            BoundingBox3d maxAABB = sink.maxAABB;
            Vector3d center = sink.center;
            collisionMotion.zero();
            Vector3dc steppingMotion = JOMLConversion.toJOML(collisionMotionMoj);
            Vector3dc steppingVelocityMotion = JOMLConversion.toJOML(velocityMotionMoj);
            boolean swappedTrackingAlready = false;
            boolean stopTrackingAtEnd = false;
            Map<SubLevel, SubLevelEntityCollision.FirstCollisionInfo> firstCollisions = new Object2ObjectArrayMap();

            for (int i = 1; i <= substeps; i++) {
               double delta = 1.0 / (double)substeps;
               collisionMotion.fma(delta, steppingMotion);
               if (collisionInfo.trackingSubLevel == null) {
                  collisionMotion.fma(delta, steppingVelocityMotion);
               }

               sink.entityBoxOrientation.identity();
               double yaw = getHitBoxYaw(subLevelPose);
               sink.entityBoxOrientation.rotateY(yaw);
               Vector3d entityUp = sink.entityUpDirection;
               if (customEntityOrientation != null) {
                  entityBoundsCenter.fma((double)entity.getEyeHeight() - entity.getBoundingBox().getYsize() / 2.0, entityUp);
                  customEntityOrientation = EntitySubLevelUtil.getCustomEntityOrientation(entity, (float)i / (float)substeps);
                  entityUp.set(OrientedBoundingBox3d.UP);
                  transformEntityBoundingBox(customEntityOrientation, sink.entityBoxOrientation, entityUp);
                  entityBoundsCenter.fma(-((double)entity.getEyeHeight() - entity.getBoundingBox().getYsize() / 2.0), entityUp);
               } else {
                  entityUp.set(OrientedBoundingBox3d.UP);
               }

               entityBoundsOBB.setOrientation(sink.entityBoxOrientation);
               entityBoundsCenter.add(collisionMotion, entityBoundsOBB.getPosition());
               ObjectIterator var45 = intersecting.iterator();

               while (var45.hasNext()) {
                  SubLevel subLevel = (SubLevel)var45.next();
                  if (Sable.HELPER.getVehicleSubLevel(entity) != subLevel) {
                     Pose3d logicalPose = subLevel.logicalPose();
                     lastPose.set(subLevel.lastPose());
                     if (lastPose.rotationPoint().lengthSquared() <= 0.0) {
                        lastPose.rotationPoint().set(logicalPose.rotationPoint());
                     }

                     lastPose.lerp(logicalPose, (double)(i - 1) / (double)substeps, lastSubLevelPose);
                     lastPose.lerp(logicalPose, (double)i / (double)substeps, subLevelPose);
                     rotatedContextBounds.set(fullContextBounds);
                     if (customEntityOrientation != null) {
                        entityBoundsOBB.vertices(sink.a);

                        for (Vector3d vec : sink.a) {
                           rotatedContextBounds.expandTo(vec);
                           rotatedContextBounds.expandTo(vec.sub(collisionMotion.x, collisionMotion.y, collisionMotion.z));
                        }

                        rotatedContextBounds.expand(0.35F);
                     }

                     rotatedContextBounds.transformInverse(lastPose, bakedMatrix, localBounds);
                     rotatedContextBounds.transformInverse(logicalPose, bakedMatrix, localBounds2);
                     localBounds.expandTo(localBounds2, localBounds);
                     if (localBounds.volume() > 1.25E8) {
                        Sable.LOGGER.info("Enormous local sub-level collision bounds, quitting.");
                     } else {
                        Iterable<BlockPos> blocks = BlockPos.betweenClosed(
                           sink.minPos.set(localBounds.minX, localBounds.minY - 1.0, localBounds.minZ),
                           sink.maxPos.set(localBounds.maxX, localBounds.maxY, localBounds.maxZ)
                        );
                        cubeOBB.getOrientation().set(subLevelPose.orientation());
                        if (collisionInfo.trackingSubLevel == subLevel) {
                           float verticalAnchorPosition = 0.0F;
                           Vector3dc feetOffset = entityUp.mul(0.0 - entity.getBoundingBox().getYsize() / 2.0, sink.posMinusCenter);
                           sink.trackingPosition.set(entityBoundsCenter).add(feetOffset);
                           subLevelPose.transformPosition(lastSubLevelPose.transformPositionInverse(sink.trackingPosition)).sub(feetOffset, entityBoundsCenter);
                           entityBoundsCenter.add(collisionMotion, entityBoundsOBB.getPosition());
                           entityBoundsCenter.fma(0.0 - entity.getBoundingBox().getYsize() / 2.0, entityUp, sink.tempEyePosition).sub(0.0, 0.0, 0.0);
                           ((EntityExtension)entity).sable$setPosSuperRaw(new Vec3(sink.tempEyePosition.x, sink.tempEyePosition.y, sink.tempEyePosition.z));
                           boolean anySurroundingBlocksSolid = false;

                           for (BlockPos block : blocks) {
                              if (!accel.getBlockState(block).isAir()) {
                                 anySurroundingBlocksSolid = true;
                                 break;
                              }
                           }

                           if (!anySurroundingBlocksSolid) {
                              stopTrackingAtEnd = true;
                           }
                        }

                        for (int maxIter = 0; maxIter < 4; maxIter++) {
                           mtv.set(Double.MAX_VALUE);
                           maxMTV.zero();
                           double maxMTVLength = Double.MIN_VALUE;
                           MutableBlockPos maxBlockPos = sink.maxBlockPos;
                           BlockState maxBlockState = null;

                           for (BlockPos blockx : blocks) {
                              BlockState state = accel.getBlockState(blockx);
                              VoxelShape voxelShape = getSubLevelEntityCollisionShape(entity, entityBoundsCenter, subLevelPose, state, accel, blockx, sink);
                              if (!state.isAir()) {
                                 Iterator<BoundingBox3dc> iterator = ((FastVoxelShapeIterable)voxelShape).sable$allBoxes();

                                 while (iterator.hasNext()) {
                                    BoundingBox3dc box = iterator.next();
                                    box.center(center);
                                    cubeOBB.getPosition()
                                       .set((double)blockx.getX() + center.x, (double)blockx.getY() + center.y, (double)blockx.getZ() + center.z);
                                    subLevelPose.transformPosition(cubeOBB.getPosition());
                                    box.size(cubeOBB.getDimensions());
                                    OrientedBoundingBox3d.sat(entityBoundsOBB, cubeOBB, mtv);
                                    if (mtv.lengthSquared() > 0.0 && mtv.x != Double.MAX_VALUE && mtv.y != Double.MAX_VALUE && mtv.z != Double.MAX_VALUE) {
                                       double lengthMtv = mtv.lengthSquared();
                                       if (lengthMtv > maxMTVLength) {
                                          maxMTVLength = lengthMtv;
                                          maxMTV.set(mtv);
                                          box.move((double)blockx.getX(), (double)blockx.getY(), (double)blockx.getZ(), maxAABB);
                                          maxBlockPos.set(blockx);
                                          maxBlockState = state;
                                       }
                                    }
                                 }
                              }
                           }

                           if (maxMTV.lengthSquared() > 0.0) {
                              if (collisionInfo.trackingSubLevel == null) {
                                 collisionInfo.trackingSubLevel = subLevel;
                                 stopTrackingAtEnd = false;
                              }

                              Vector3dc localMtv = subLevelPose.transformNormalInverse(maxMTV, sink.localMtv).normalize();
                              int offsetX = (int)Math.round(localMtv.x());
                              int offsetY = (int)Math.round(localMtv.y());
                              int offsetZ = (int)Math.round(localMtv.z());
                              BlockPos newPos = sink.offsetPos.setWithOffset(maxBlockPos, offsetX, offsetY, offsetZ);
                              BlockState offsetState = accel.getBlockState(newPos);
                              VoxelShape offsetShape = getSubLevelEntityCollisionShape(
                                 entity, entityBoundsCenter, subLevelPose, offsetState, accel, newPos, sink
                              );
                              Direction direction = Direction.get(
                                 AxisDirection.POSITIVE, Direction.getNearest((float)offsetX, (float)offsetY, (float)offsetZ).getAxis()
                              );
                              BoundingBox3d offsetAABB = sink.offsetAABB;
                              BoundingBox3d compressedMinAABB = sink.compressedMinAABB;
                              BoundingBox3d compressedOffsetAABB = sink.compressedOffsetAABB;
                              BoundingBox3d intersection = sink.intersection;
                              boolean discard = false;
                              Iterator<BoundingBox3dc> iterator = ((FastVoxelShapeIterable)offsetShape).sable$allBoxes();

                              while (iterator.hasNext()) {
                                 BoundingBox3dc box = iterator.next();
                                 box.move((double)newPos.getX(), (double)newPos.getY(), (double)newPos.getZ(), offsetAABB).expand(0.001);
                                 if (maxAABB.intersects(offsetAABB)) {
                                    compressedMinAABB.set(
                                       maxAABB.minX * (1.0 - (double)direction.getStepX()),
                                       maxAABB.minY * (1.0 - (double)direction.getStepY()),
                                       maxAABB.minZ * (1.0 - (double)direction.getStepZ()),
                                       maxAABB.maxX * (1.0 - (double)direction.getStepX()) + (double)direction.getStepX(),
                                       maxAABB.maxY * (1.0 - (double)direction.getStepY()) + (double)direction.getStepY(),
                                       maxAABB.maxZ * (1.0 - (double)direction.getStepZ()) + (double)direction.getStepZ()
                                    );
                                    compressedOffsetAABB.set(
                                       offsetAABB.minX * (1.0 - (double)direction.getStepX()),
                                       offsetAABB.minY * (1.0 - (double)direction.getStepY()),
                                       offsetAABB.minZ * (1.0 - (double)direction.getStepZ()),
                                       offsetAABB.maxX * (1.0 - (double)direction.getStepX()) + (double)direction.getStepX(),
                                       offsetAABB.maxY * (1.0 - (double)direction.getStepY()) + (double)direction.getStepY(),
                                       offsetAABB.maxZ * (1.0 - (double)direction.getStepZ()) + (double)direction.getStepZ()
                                    );
                                    compressedMinAABB.intersect(compressedOffsetAABB, intersection);
                                    if (Math.abs(intersection.volume() - compressedMinAABB.volume()) < 0.01) {
                                       discard = true;
                                       break;
                                    }
                                 }
                              }

                              if (!discard) {
                                 maxMTV.normalize(normalizedMtv);
                                 double dot = normalizedMtv.dot(entityUp);
                                 boolean verticalCollision = Math.abs(dot) > 0.6;
                                 BlockState collidedBlockState = maxBlockState;
                                 firstCollisions.computeIfAbsent(
                                    subLevel,
                                    sl -> {
                                       Vector3d localBoundsCenter = subLevelPose.transformPositionInverse(new Vector3d(entityBoundsCenter));
                                       return new SubLevelEntityCollision.FirstCollisionInfo(
                                          localBoundsCenter,
                                          new Vector3d(maxMTV).normalize(),
                                          !verticalCollision,
                                          collidedBlockState.is(SableTags.BOUNCY),
                                          collidedBlockState
                                       );
                                    }
                                 );
                                 if (verticalCollision) {
                                    collisionInfo.verticalCollision = true;
                                    if (dot > 0.0) {
                                       entity.setOnGround(true);
                                       collisionInfo.verticalCollisionBelow = true;
                                       if (collisionInfo.trackingSubLevel != subLevel && !swappedTrackingAlready) {
                                          swappedTrackingAlready = true;
                                          collisionInfo.trackingSubLevel = subLevel;
                                       }
                                    }

                                    if (dot > 0.8) {
                                       double preLength = maxMTV.length();
                                       entityUp.mul(maxMTV.dot(entityUp), maxMTV).normalize(preLength);
                                    }
                                 } else {
                                    collisionInfo.subLevelHorizontalCollision = collisionInfo.subLevelHorizontalCollision
                                       | !tryStepUp(
                                          entity,
                                          accel,
                                          sink,
                                          subLevelPose,
                                          blocks,
                                          entityBoundsCenter,
                                          entityBounds,
                                          entityBoundsOBB,
                                          cubeOBB,
                                          maxMTV,
                                          normalizedMtv,
                                          collisionMotion
                                       );
                                    if (collisionInfo.subLevelHorizontalCollision) {
                                       JOMLConversion.toJOML(entity.getDeltaMovement(), existingDeltaMovement);
                                       Vector3d deltaMovementLoss = normalizedMtv.mul(normalizedMtv.dot(existingDeltaMovement));
                                       if (deltaMovementLoss.length() > existingDeltaMovement.length() * 0.1) {
                                          entity.setSprinting(false);
                                       }

                                       double friction = 0.995;
                                       Vector3d newDeltaMovement = existingDeltaMovement.sub(deltaMovementLoss);
                                       double upVelocity = entityUp.dot(newDeltaMovement);
                                       newDeltaMovement.fma(-upVelocity, entityUp).mul(0.995).fma(upVelocity, entityUp);
                                       entity.setDeltaMovement(JOMLConversion.toMojang(newDeltaMovement));
                                    }
                                 }

                                 collisionMotion.add(maxMTV);
                                 entityBoundsCenter.add(collisionMotion, entityBoundsOBB.getPosition());
                              }
                           }
                        }
                     }
                  }
               }
            }

            collisionInfo.inheritedMotion = JOMLConversion.toMojang(
               Sable.HELPER.getFeetPos(entity, 0.0F, customEntityOrientation).sub(originalEntityFootPosition)
            );
            if (collisionInfo.inheritedMotion.lengthSqr() < 1.0E-8) {
               collisionInfo.inheritedMotion = null;
            }

            if (stopTrackingAtEnd) {
               collisionInfo.trackingSubLevel = null;
            }

            ((EntityExtension)entity).sable$setPosSuperRaw(originalEntityPosition);
            collisionInfo.motion = JOMLConversion.toMojang(collisionMotion);
            collisionInfo.firstCollisions = firstCollisions;
            return collisionInfo;
         }
      }
   }

   public static void transformEntityBoundsCenter(LevelReusedVectors sink, Quaterniondc customOrientation, Entity entity, Vector3d center) {
      if (customOrientation != null) {
         Vector3d offset = sink.anchorRelativePosition.set(0.0, (double)entity.getEyeHeight() - entity.getBoundingBox().getYsize() / 2.0, 0.0);
         center.add(offset).sub(customOrientation.transform(offset));
      }
   }

   public static void transformEntityBoundingBox(Quaterniondc customOrientation, Quaterniond bounds, Vector3d upDir) {
      if (customOrientation != null) {
         bounds.premul(customOrientation);
         customOrientation.transform(upDir);
      }
   }

   public static double getHitBoxYaw(Pose3dc subLevelPose) {
      Quaterniondc subLevelOrientation = subLevelPose.orientation();
      Quaterniond snapped = SableMathUtils.clampQuaternionToGrid(subLevelOrientation, SableMathUtils.GridQuats.REAL, new Quaterniond());
      Quaterniond relativeOrientation = subLevelOrientation.div(snapped, snapped);
      double dot = OrientedBoundingBox3d.UP.dot(new Vector3d(relativeOrientation.x(), relativeOrientation.y(), relativeOrientation.z()));
      return -2.0 * Math.atan2(-dot, relativeOrientation.w());
   }

   @NotNull
   private static VoxelShape getSubLevelEntityCollisionShape(
      Entity entity, Vector3dc boundsCenter, Pose3dc subLevelPose, BlockState state, LevelAccelerator level, BlockPos pos, LevelReusedVectors sink
   ) {
      if (state.getBlock() instanceof ScaffoldingBlock) {
         VoxelShape originalShape = state.getCollisionShape(level, pos, new SubLevelEntityCollisionContext(entity));
         double skew = 0.05;
         if (entity.isShiftKeyDown()) {
            return originalShape;
         } else {
            return subLevelPose.transformPositionInverse(
                        boundsCenter.fma(-(entity.getBoundingBox().getYsize() / 2.0 - 0.05), sink.entityUpDirection, new Vector3d())
                     )
                     .y
                  > (double)pos.getY() + 1.0 + 0.05
               ? sink.SCAFFOLDING_TOP
               : originalShape;
         }
      } else {
         return state.getCollisionShape(level, pos);
      }
   }

   private static boolean tryStepUp(
      Entity entity,
      LevelAccelerator accel,
      LevelReusedVectors sink,
      Pose3dc subLevelPose,
      Iterable<BlockPos> blocks,
      Vector3dc entityBoundsCenter,
      AABB entityBounds,
      OrientedBoundingBox3d entityBoundsOBB,
      OrientedBoundingBox3d cubeOBB,
      Vector3dc maxMTV,
      Vector3dc normalizedMTV,
      Vector3d collisionMotion
   ) {
      if (!entity.onGround()) {
         return false;
      } else if (collisionMotion.dot(normalizedMTV) > 0.0) {
         return true;
      } else {
         double checkIncrement = 0.0625;
         double maxStepHeight = (double)entity.maxUpStep();
         Vector3d lastStepTestMTV = sink.lastStepTestMTV.zero();
         int collidingCount = 0;
         int freeCount = 0;
         double inflation = 0.1;
         entityBoundsOBB.getDimensions().set(entityBounds.getXsize(), entityBounds.getYsize(), entityBounds.getZsize()).add(0.1, 0.1, 0.1);

         double currentStepUp;
         for (currentStepUp = 0.0; currentStepUp <= maxStepHeight; currentStepUp += 0.0625) {
            Vector3d boundsCenter = sink.stepHeightEntityBoundsCenter;
            boundsCenter.set(entityBoundsCenter).fma(currentStepUp, sink.entityUpDirection).fma(-0.125, normalizedMTV);
            if (!hasCollision(accel, sink, subLevelPose, blocks, entityBoundsOBB, cubeOBB, boundsCenter)) {
               freeCount++;
               break;
            }

            lastStepTestMTV.set(sink.mtv);
            collidingCount++;
         }

         entityBoundsOBB.getDimensions().set(entityBounds.getXsize(), entityBounds.getYsize(), entityBounds.getZsize());
         if (freeCount > 0 && collidingCount > 0 && lastStepTestMTV.normalize().dot(sink.entityUpDirection) > 0.8) {
            collisionMotion.fma(currentStepUp, sink.entityUpDirection).fma(-0.0625, normalizedMTV);
            return true;
         } else {
            return false;
         }
      }
   }

   private static boolean hasCollision(
      LevelAccelerator accel,
      LevelReusedVectors sink,
      Pose3dc subLevelPose,
      Iterable<BlockPos> blocks,
      OrientedBoundingBox3d entityBoundsOBB,
      OrientedBoundingBox3d cubeOBB,
      Vector3d boundsCenter
   ) {
      entityBoundsOBB.setPosition(boundsCenter);

      for (BlockPos block : blocks) {
         BlockState state = accel.getBlockState(block);
         VoxelShape voxelShape = state.getCollisionShape(accel, block);
         if (!state.isAir()) {
            Iterator<BoundingBox3dc> iterator = ((FastVoxelShapeIterable)voxelShape).sable$allBoxes();
            Vector3d center = sink.center;
            Vector3d mtv = sink.mtv;

            while (iterator.hasNext()) {
               BoundingBox3dc box = iterator.next();
               box.center(center);
               cubeOBB.getPosition().set((double)block.getX() + center.x, (double)block.getY() + center.y, (double)block.getZ() + center.z);
               subLevelPose.transformPosition(cubeOBB.getPosition());
               box.size(cubeOBB.getDimensions());
               OrientedBoundingBox3d.sat(entityBoundsOBB, cubeOBB, mtv);
               if (mtv.lengthSquared() > 0.0 && mtv.x != Double.MAX_VALUE && mtv.y != Double.MAX_VALUE && mtv.z != Double.MAX_VALUE) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static class CollisionInfo {
      public SubLevel preTrackingSubLevel;
      public Vec3 preDeltaMovement;
      public boolean subLevelHorizontalCollision;
      public boolean horizontalCollision;
      public boolean verticalCollision;
      public boolean verticalCollisionBelow;
      public boolean minorHorizontalCollision;
      public Vec3 inheritedMotion;
      public Vec3 motion;
      public SubLevel trackingSubLevel;
      public Map<SubLevel, SubLevelEntityCollision.FirstCollisionInfo> firstCollisions;
   }

   public static record FirstCollisionInfo(Vector3dc localLocation, Vector3dc globalDirection, boolean horizontal, boolean bouncy, BlockState block) {
   }
}
