package com.simibubi.create.foundation.collision;

import net.createmod.catnip.data.Iterate;
import net.minecraft.world.phys.Vec3;

public class ContinuousOBBCollider {
   static final Vec3 uA0 = new Vec3(1.0, 0.0, 0.0);
   static final Vec3 uA1 = new Vec3(0.0, 1.0, 0.0);
   static final Vec3 uA2 = new Vec3(0.0, 0.0, 1.0);
   static int checkCount = 0;

   public static ContinuousOBBCollider.CollisionResponse collideMany(
      CollisionList collidableBBs, CollisionList denseViableColliders, OrientedBB obb, Vec3 motion, float entityMaxStep, boolean doHorizontalPass
   ) {
      Vec3 obbCenter = obb.center;
      Vec3 obbExtents = obb.extents;
      Matrix3d rotation = obb.rotation;
      double a00 = Math.abs(rotation.m00);
      double a01 = Math.abs(rotation.m01);
      double a02 = Math.abs(rotation.m02);
      double a10 = Math.abs(rotation.m10);
      double a11 = Math.abs(rotation.m11);
      double a12 = Math.abs(rotation.m12);
      double a20 = Math.abs(rotation.m20);
      double a21 = Math.abs(rotation.m21);
      double a22 = Math.abs(rotation.m22);
      double aabbInLocalX = (a00 + a01 + a02) * obbExtents.x + 0.5;
      double aabbInLocalY = (a10 + a11 + a12) * obbExtents.y + 0.5;
      double aabbInLocalZ = (a20 + a21 + a22) * obbExtents.z + 0.5;
      CollisionList.Populate populateDenseViableColliders = new CollisionList.Populate(denseViableColliders);
      denseViableColliders.size = 0;

      for (int bbIdx = 0; bbIdx < collidableBBs.size; bbIdx++) {
         if (!(Math.abs(obbCenter.x + motion.x - collidableBBs.centerX[bbIdx]) > collidableBBs.extentsX[bbIdx] + aabbInLocalX)
            && !(Math.abs(obbCenter.y + motion.y - collidableBBs.centerY[bbIdx]) > collidableBBs.extentsY[bbIdx] + aabbInLocalY)
            && !(Math.abs(obbCenter.z + motion.z - collidableBBs.centerZ[bbIdx]) > collidableBBs.extentsZ[bbIdx] + aabbInLocalZ)) {
            populateDenseViableColliders.appendFrom(collidableBBs, bbIdx);
         }
      }

      if (denseViableColliders.size == 0) {
         ContinuousOBBCollider.CollisionResponse out = new ContinuousOBBCollider.CollisionResponse();
         out.surfaceCollision = false;
         out.collisionResponse = Vec3.ZERO;
         out.normal = Vec3.ZERO;
         out.location = Vec3.ZERO;
         out.temporalResponse = 1.0;
         return out;
      } else {
         double collisionResponseX = 0.0;
         double collisionResponseY = 0.0;
         double collisionResponseZ = 0.0;
         double locationX = 0.0;
         double locationY = 0.0;
         double locationZ = 0.0;
         double normalX = 0.0;
         double normalY = 0.0;
         double normalZ = 0.0;
         boolean surfaceCollision = false;
         double temporalResponse = 1.0;
         Vec3 uB0 = new Vec3(rotation.m00, rotation.m10, rotation.m20).normalize();
         Vec3 uB1 = new Vec3(rotation.m01, rotation.m11, rotation.m21).normalize();
         Vec3 uB2 = new Vec3(rotation.m02, rotation.m12, rotation.m22).normalize();
         Vec3 motion2 = rotation.transformTransposed(motion);
         ContinuousOBBCollider.ContinuousSeparationManifold mf = new ContinuousOBBCollider.ContinuousSeparationManifold(uB1);

         for (boolean horizontalPass : Iterate.trueAndFalse) {
            boolean verticalPass = !horizontalPass || !doHorizontalPass;
            if (!horizontalPass || doHorizontalPass) {
               for (int bbIdxx = 0; bbIdxx < denseViableColliders.size; bbIdxx++) {
                  double deltaX = obbCenter.x + collisionResponseX - denseViableColliders.centerX[bbIdxx];
                  double deltaY = obbCenter.y + collisionResponseY - denseViableColliders.centerY[bbIdxx];
                  double deltaZ = obbCenter.z + collisionResponseZ - denseViableColliders.centerZ[bbIdxx];
                  checkCount = 0;
                  mf.reset();
                  double extentsX = denseViableColliders.extentsX[bbIdxx];
                  double extentsY = denseViableColliders.extentsY[bbIdxx];
                  double extentsZ = denseViableColliders.extentsZ[bbIdxx];
                  if (!mf.separate(uA0, deltaX, extentsX, a00 * obbExtents.x + a01 * obbExtents.y + a02 * obbExtents.z, motion.x, true)
                     && !mf.separate(uA1, deltaY, extentsY, a10 * obbExtents.x + a11 * obbExtents.y + a12 * obbExtents.z, motion.y, true)
                     && !mf.separate(uA2, deltaZ, extentsZ, a20 * obbExtents.x + a21 * obbExtents.y + a22 * obbExtents.z, motion.z, true)) {
                     Vec3 deltaEntityFrame = rotation.transformTransposed(deltaX, deltaY, deltaZ);
                     if (!mf.separate(uB0, deltaEntityFrame.x, extentsX * a00 + extentsY * a10 + extentsZ * a20, obbExtents.x, motion2.x, false)
                        && !mf.separate(uB1, deltaEntityFrame.y, extentsX * a01 + extentsY * a11 + extentsZ * a21, obbExtents.y, motion2.y, false)
                        && !mf.separate(uB2, deltaEntityFrame.z, extentsX * a02 + extentsY * a12 + extentsZ * a22, obbExtents.z, motion2.z, false)) {
                        if (verticalPass && !surfaceCollision) {
                           surfaceCollision = true;
                        }

                        double timeOfImpact = mf.getTimeOfImpact();
                        boolean isTemporal = timeOfImpact > 0.0 && timeOfImpact < 1.0;
                        if (!isTemporal && mf.isDiscreteCollision) {
                           if (mf.stepSeparation <= (double)entityMaxStep) {
                              double sep = ContinuousOBBCollider.ContinuousSeparationManifold.withSignedEpsilon(mf.stepSeparation);
                              collisionResponseX += mf.stepSeparationAxis.x * sep;
                              collisionResponseY += mf.stepSeparationAxis.y * sep;
                              collisionResponseZ += mf.stepSeparationAxis.z * sep;
                           } else {
                              double sep = ContinuousOBBCollider.ContinuousSeparationManifold.withSignedEpsilon(mf.separation);
                              collisionResponseX += mf.axis.x * sep;
                              collisionResponseY += mf.axis.y * sep;
                              collisionResponseZ += mf.axis.z * sep;
                           }

                           timeOfImpact = 0.0;
                        }

                        if (timeOfImpact >= 0.0 && temporalResponse > timeOfImpact) {
                           double scale = ContinuousOBBCollider.ContinuousSeparationManifold.withSignedEpsilon(mf.normalSeparation);
                           normalX = mf.normalAxis.x * scale;
                           normalY = mf.normalAxis.y * scale;
                           normalZ = mf.normalAxis.z * scale;
                           locationX = mf.collisionX;
                           locationY = mf.collisionY;
                           locationZ = mf.collisionZ;
                        }

                        if (isTemporal && temporalResponse > timeOfImpact) {
                           temporalResponse = timeOfImpact;
                        }
                     }
                  }
               }

               if (verticalPass) {
                  break;
               }

               boolean noVerticalMotionResponse = temporalResponse == 1.0;
               boolean noVerticalCollision = collisionResponseY == 0.0;
               if (noVerticalCollision && noVerticalMotionResponse) {
                  break;
               }

               collisionResponseX *= 1.0078125;
               collisionResponseZ *= 1.0078125;
            }
         }

         ContinuousOBBCollider.CollisionResponse out = new ContinuousOBBCollider.CollisionResponse();
         out.surfaceCollision = surfaceCollision;
         out.collisionResponse = new Vec3(collisionResponseX, collisionResponseY, collisionResponseZ);
         out.normal = new Vec3(normalX, normalY, normalZ);
         out.location = new Vec3(locationX, locationY, locationZ);
         out.temporalResponse = temporalResponse;
         return out;
      }
   }

   public static class CollisionResponse {
      public boolean surfaceCollision;
      public Vec3 collisionResponse;
      public Vec3 normal;
      public Vec3 location;
      public double temporalResponse;
   }

   private static class ContinuousSeparationManifold {
      static final double UNDEFINED = -1.0;
      double latestCollisionEntryTime = -1.0;
      double earliestCollisionExitTime = Double.MAX_VALUE;
      boolean isDiscreteCollision = true;
      double collisionX;
      double collisionY;
      double collisionZ;
      final Vec3 stepSeparationAxis;
      double stepSeparation;
      Vec3 normalAxis;
      double normalSeparation;
      Vec3 axis;
      double separation;

      public ContinuousSeparationManifold(Vec3 stepSeparationAxis) {
         this.stepSeparationAxis = stepSeparationAxis;
      }

      boolean separate(Vec3 axis, double TL, double rA, double rB, double projectedMotion, boolean axisOfObjA) {
         ContinuousOBBCollider.checkCount++;
         double distance = Math.abs(TL);
         double diff = distance - (rA + rB);
         boolean discreteCollision = diff <= 0.0;
         if (!discreteCollision && Math.signum(projectedMotion) == Math.signum(TL)) {
            return true;
         } else {
            double sTL = Math.signum(TL);
            double separation = sTL * Math.abs(diff);
            if (!discreteCollision) {
               this.isDiscreteCollision = false;
               if (Math.abs(separation) > Math.abs(projectedMotion)) {
                  return true;
               }

               double entryTime = Math.abs(separation) / Math.abs(projectedMotion);
               double exitTime = (diff + Math.abs(rA) + Math.abs(rB)) / Math.abs(projectedMotion);
               this.latestCollisionEntryTime = Math.max(entryTime, this.latestCollisionEntryTime);
               this.earliestCollisionExitTime = Math.min(exitTime, this.earliestCollisionExitTime);
            }

            if (axisOfObjA && distance != 0.0 && -diff <= Math.abs(this.normalSeparation)) {
               this.normalAxis = axis;
               this.normalSeparation = separation;
            }

            double dot = this.stepSeparationAxis.dot(axis);
            if (dot != 0.0 && discreteCollision) {
               Vec3 cross = axis.cross(this.stepSeparationAxis);
               double dotSeparation = Math.signum(dot) * TL - (rA + rB);
               double stepSeparation = -dotSeparation;
               if (!cross.equals(Vec3.ZERO)) {
                  Vec3 sepVec = axis.scale(dotSeparation);
                  Vec3 axisPlane = axis.cross(cross);
                  Vec3 stepPlane = this.stepSeparationAxis.cross(cross);
                  Vec3 stepSeparationVec = sepVec.subtract(axisPlane.scale(sepVec.dot(stepPlane) / axisPlane.dot(stepPlane)));
                  stepSeparation = stepSeparationVec.length();
                  if (Math.abs(this.stepSeparation) > Math.abs(stepSeparation) && stepSeparation != 0.0) {
                     this.stepSeparation = stepSeparation;
                  }
               } else if (Math.abs(this.stepSeparation) > stepSeparation) {
                  this.stepSeparation = stepSeparation;
               }
            }

            if (distance != 0.0 && -diff <= Math.abs(this.separation)) {
               this.axis = axis;
               this.separation = separation;
               double scale = Math.signum(TL) * (axisOfObjA ? -rA : -rB) - Math.signum(separation) * 0.125;
               this.collisionX = axis.x * scale;
               this.collisionY = axis.y * scale;
               this.collisionZ = axis.z * scale;
            }

            return false;
         }
      }

      public double getTimeOfImpact() {
         if (this.latestCollisionEntryTime == -1.0) {
            return -1.0;
         } else {
            return this.latestCollisionEntryTime > this.earliestCollisionExitTime ? -1.0 : this.latestCollisionEntryTime;
         }
      }

      private static double withSignedEpsilon(double sep) {
         return sep + Math.signum(sep) * 1.0E-4;
      }

      public void reset() {
         this.axis = null;
         this.normalAxis = null;
         this.separation = Double.MAX_VALUE;
         this.stepSeparation = Double.MAX_VALUE;
         this.normalSeparation = Double.MAX_VALUE;
         this.latestCollisionEntryTime = -1.0;
         this.earliestCollisionExitTime = Double.MAX_VALUE;
         this.isDiscreteCollision = true;
      }
   }
}
