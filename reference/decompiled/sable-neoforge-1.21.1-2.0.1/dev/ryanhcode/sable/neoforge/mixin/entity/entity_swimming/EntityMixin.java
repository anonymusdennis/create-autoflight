package dev.ryanhcode.sable.neoforge.mixin.entity.entity_swimming;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.neoforge.mixinhelper.entity.SableInterimCalculation;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.extensions.IEntityExtension;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Entity.class},
   priority = 500
)
public abstract class EntityMixin implements IEntityExtension {
   @Shadow
   private Level level;
   @Shadow
   private Vec3 position;
   @Shadow
   private FluidType forgeFluidTypeOnEyes;

   @Shadow
   public abstract boolean touchingUnloadedChunk();

   @Shadow
   public abstract AABB getBoundingBox();

   @Shadow
   @Deprecated
   public abstract boolean isPushedByFluid();

   @Shadow
   public abstract Vec3 getDeltaMovement();

   @Shadow
   public abstract void setDeltaMovement(Vec3 var1);

   @Shadow
   protected abstract void setFluidTypeHeight(FluidType var1, double var2);

   @Shadow
   public abstract BlockPos blockPosition();

   @Shadow
   public abstract Level level();

   @Shadow
   public abstract Vec3 getEyePosition();

   @Overwrite
   public void updateFluidHeightAndDoFluidPushing() {
      if (!this.touchingUnloadedChunk()) {
         AABB aabb = this.getBoundingBox().deflate(0.001);
         int i = Mth.floor(aabb.minX);
         int j = Mth.ceil(aabb.maxX);
         int k = Mth.floor(aabb.minY);
         int l = Mth.ceil(aabb.maxY);
         int i1 = Mth.floor(aabb.minZ);
         int j1 = Mth.ceil(aabb.maxZ);
         MutableBlockPos blockpos$mutableblockpos = new MutableBlockPos();
         Object2ObjectMap<FluidType, SableInterimCalculation> interimCalcs = null;

         for (int l1 = i; l1 < j; l1++) {
            for (int i2 = k; i2 < l; i2++) {
               for (int j2 = i1; j2 < j1; j2++) {
                  blockpos$mutableblockpos.set(l1, i2, j2);
                  FluidState fluidstate = this.level.getFluidState(blockpos$mutableblockpos);
                  FluidType fluidType = fluidstate.getFluidType();
                  if (!fluidType.isAir()) {
                     double d1 = (double)((float)i2 + fluidstate.getHeight(this.level, blockpos$mutableblockpos));
                     if (d1 >= aabb.minY) {
                        if (interimCalcs == null) {
                           interimCalcs = new Object2ObjectArrayMap();
                        }

                        SableInterimCalculation interim = (SableInterimCalculation)interimCalcs.computeIfAbsent(fluidType, t -> new SableInterimCalculation());
                        interim.fluidHeight = Math.max(d1 - aabb.minY, interim.fluidHeight);
                        if (this.isPushedByFluid(fluidType)) {
                           Vec3 vec31 = fluidstate.getFlow(this.level, blockpos$mutableblockpos);
                           if (interim.fluidHeight < 0.4) {
                              vec31 = vec31.scale(interim.fluidHeight);
                           }

                           interim.flowVector = interim.flowVector.add(vec31);
                           interim.blockCount++;
                        }
                     }
                  }
               }
            }
         }

         ActiveSableCompanion helper = Sable.HELPER;
         BoundingBox3d globalBounds = new BoundingBox3d(aabb);
         BoundingBox3d localBounds = new BoundingBox3d();
         Iterable<SubLevel> intersecting = helper.getAllIntersecting(this.level, globalBounds);
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         Vector3d playerCenter = new Vector3d();
         Vector3d playerSize = new Vector3d();
         Quaterniond playerOrientation = new Quaterniond();

         for (SubLevel subLevel : intersecting) {
            Pose3dc pose = subLevel.lastPose();
            globalBounds.transformInverse(pose, localBounds);
            LevelReusedVectors jomlSink = ((LevelExtension)this.level).sable$getJOMLSink();
            Quaterniond localPlayerBox = pose.orientation().conjugate(playerOrientation);
            double yaw = SubLevelEntityCollision.getHitBoxYaw(pose);
            localPlayerBox.rotateY(yaw);
            OrientedBoundingBox3d playerBox = new OrientedBoundingBox3d(
               pose.transformPositionInverse(globalBounds.center(playerCenter)), globalBounds.size(playerSize), localPlayerBox, jomlSink
            );
            OrientedBoundingBox3d fluidBox = new OrientedBoundingBox3d(new Vector3d(), new Vector3d(1.0), JOMLConversion.QUAT_IDENTITY, jomlSink);
            int minX = Mth.floor(localBounds.minX);
            int maxX = Mth.ceil(localBounds.maxX);
            int minY = Mth.floor(localBounds.minY);
            int maxY = Mth.ceil(localBounds.maxY);
            int minZ = Mth.floor(localBounds.minZ);
            int maxZ = Mth.ceil(localBounds.maxZ);
            double minYVertex = Float.MAX_VALUE;
            boolean hasComputedMinYVertex = false;

            for (int x = minX; x < maxX; x++) {
               for (int y = minY; y < maxY; y++) {
                  for (int z = minZ; z < maxZ; z++) {
                     mutableBlockPos.set(x, y, z);
                     FluidState fluidState = this.level.getFluidState(mutableBlockPos);
                     FluidType fluidType = fluidState.getFluidType();
                     if (!fluidType.isAir()) {
                        double fluidLevelY = (double)((float)y + fluidState.getHeight(this.level, mutableBlockPos));
                        if (!hasComputedMinYVertex) {
                           Vector3d[] vertices = playerBox.vertices(jomlSink.a);

                           for (Vector3d vertex : vertices) {
                              minYVertex = Math.min(minYVertex, vertex.y);
                           }

                           hasComputedMinYVertex = true;
                        }

                        if (fluidLevelY >= minYVertex) {
                           fluidBox.getPosition().set((double)x + 0.5, (double)y + 0.5, (double)z + 0.5);
                           if (OrientedBoundingBox3d.sat(playerBox, fluidBox).lengthSquared() > 0.0) {
                              if (interimCalcs == null) {
                                 interimCalcs = new Object2ObjectArrayMap();
                              }

                              SableInterimCalculation interim = (SableInterimCalculation)interimCalcs.computeIfAbsent(
                                 fluidType, t -> new SableInterimCalculation()
                              );
                              interim.fluidHeight = Math.max(fluidLevelY - minYVertex, interim.fluidHeight);
                              if (Sable.HELPER.getTrackingSubLevel((Entity)this) == null && helper.getContaining((Entity)this) != subLevel) {
                                 ((EntityMovementExtension)this).sable$setTrackingSubLevel(subLevel);
                              }

                              if (this.isPushedByFluid(fluidType)) {
                                 Vec3 flowVec = fluidState.getFlow(this.level, mutableBlockPos);
                                 if (interim.fluidHeight < 0.4) {
                                    flowVec = flowVec.scale(interim.fluidHeight);
                                 }

                                 flowVec = pose.transformNormal(flowVec);
                                 interim.flowVector = interim.flowVector.add(flowVec);
                                 interim.blockCount++;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (interimCalcs != null) {
            interimCalcs.forEach((fluidTypex, interimx) -> {
               if (interimx.flowVector.length() > 0.0) {
                  if (interimx.blockCount > 0) {
                     interimx.flowVector = interimx.flowVector.scale(1.0 / (double)interimx.blockCount);
                  }

                  if (!(this instanceof Player)) {
                     interimx.flowVector = interimx.flowVector.normalize();
                  }

                  Vec3 vec32 = this.getDeltaMovement();
                  interimx.flowVector = interimx.flowVector.scale(this.getFluidMotionScale(fluidTypex));
                  double d2 = 0.003;
                  if (Math.abs(vec32.x) < 0.003 && Math.abs(vec32.z) < 0.003 && interimx.flowVector.length() < 0.0045000000000000005) {
                     interimx.flowVector = interimx.flowVector.normalize().scale(0.0045000000000000005);
                  }

                  this.setDeltaMovement(this.getDeltaMovement().add(interimx.flowVector));
               }

               this.setFluidTypeHeight(fluidTypex, interimx.fluidHeight);
            });
         }
      }
   }

   public boolean canStartSwimming() {
      Level level = this.level();
      BlockPos globalBlockPos = this.blockPosition();
      FluidType fluidType = level.getFluidState(globalBlockPos).getFluidType();
      if (fluidType == Fluids.EMPTY.getFluidType()) {
         for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(this.level, new BoundingBox3d(globalBlockPos).expand(0.5))) {
            Pose3dc pose = subLevel.lastPose();
            BlockPos localBlockPos = BlockPos.containing(pose.transformPositionInverse(this.position));
            fluidType = level.getFluidState(localBlockPos).getFluidType();
            if (fluidType != Fluids.EMPTY.getFluidType()) {
               break;
            }
         }
      }

      return !this.getEyeInFluidType().isAir() && this.canSwimInFluidType(this.getEyeInFluidType()) && this.canSwimInFluidType(fluidType);
   }

   @Inject(
      method = {"updateFluidOnEyes"},
      at = {@At("TAIL")}
   )
   public void sable$subLevelFluidOnEyes(CallbackInfo ci) {
      if (this.forgeFluidTypeOnEyes == NeoForgeMod.EMPTY_TYPE.value() || this.forgeFluidTypeOnEyes == Fluids.EMPTY.getFluidType()) {
         Vec3 globalEyePos = this.getEyePosition();

         for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(this.level, new BoundingBox3d(BlockPos.containing(globalEyePos)).expand(0.5))) {
            Pose3dc pose = subLevel.lastPose();
            Vec3 localEyePos = pose.transformPositionInverse(globalEyePos);
            BlockPos blockPos = BlockPos.containing(localEyePos);
            FluidState fluidState = this.level.getFluidState(blockPos);
            double e = (double)((float)blockPos.getY() + fluidState.getHeight(this.level, blockPos));
            if (e > localEyePos.y) {
               this.forgeFluidTypeOnEyes = fluidState.getFluidType();
               if (this.forgeFluidTypeOnEyes != NeoForgeMod.EMPTY_TYPE.value() && this.forgeFluidTypeOnEyes != Fluids.EMPTY.getFluidType()) {
                  return;
               }
            }
         }
      }
   }
}
