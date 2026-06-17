package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.nozzles;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.NozzleBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.neoforge.mixin.compatibility.create.nozzle.NozzleBlockEntityAccessor;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class NozzleHoveringHelper {
   public static List<Couple<Vec3>> gatherRaycastPoints(BlockState state) {
      if (!state.hasProperty(BlockStateProperties.FACING)) {
         return null;
      } else {
         List<Couple<Vec3>> rayPoints = new ArrayList<>();
         Direction facing = (Direction)state.getValue(BlockStateProperties.FACING);
         Direction startingDir;
         if (facing.getAxis() == Axis.Y) {
            startingDir = Direction.NORTH;
         } else {
            startingDir = facing.getClockWise();
         }

         int horizontalSamplePoints = 6;
         int theta = 60;
         double startScaling = 0.8;
         double endScaling = 8.0;

         for (boolean diagonal : Iterate.trueAndFalse) {
            for (int i = 0; i < 6; i++) {
               Vec3 start = Vec3.atLowerCornerOf(startingDir.getNormal()).scale(0.8).add(0.5, 0.5, 0.5);
               if (diagonal) {
                  Axis axis;
                  double angle;
                  if (facing.getAxis().isHorizontal()) {
                     axis = Axis.Y;
                     angle = 45.0;
                  } else {
                     axis = startingDir.getClockWise().getAxis();
                     angle = (double)(facing.getAxisDirection().getStep() * 45);
                  }

                  start = VecHelper.rotateCentered(start, angle, axis);
               }

               start = VecHelper.rotateCentered(start, (double)(60 * i), facing.getAxis());
               Vec3 end = start.add(start.subtract(0.5, 0.5, 0.5).scale(8.0));
               rayPoints.add(Couple.create(start, end));
            }
         }

         Vec3 start = Vec3.atLowerCornerOf(facing.getNormal()).scale(0.8).add(0.5, 0.5, 0.5);
         Vec3 end = start.add(start.subtract(0.5, 0.5, 0.5).scale(8.0));
         rayPoints.add(Couple.create(start, end));
         return rayPoints;
      }
   }

   @Nullable
   public static Vector3d gatherForceFromRays(
      SubLevel parentSublevel, double timeStep, Level level, BlockPos blockStart, NozzleBlockEntity nbe, List<Couple<Vec3>> rayPoints
   ) {
      if (((NozzleBlockEntityAccessor)nbe).getRange() == 0.0F) {
         return null;
      } else {
         Optional<EncasedFanBlockEntity> be = level.getBlockEntity(
            blockStart.relative(((Direction)nbe.getBlockState().getValue(BlockStateProperties.FACING)).getOpposite()),
            (BlockEntityType)AllBlockEntityTypes.ENCASED_FAN.get()
         );
         if (be.isPresent()) {
            EncasedFanBlockEntity fbe = be.get();
            Vector3d force = new Vector3d();
            Couple<Vec3> firstRay = rayPoints.getFirst();
            double startEndDistance = ((Vec3)firstRay.getSecond()).subtract((Vec3)firstRay.getFirst()).length();
            Vec3 blockCorner = Vec3.atLowerCornerOf(blockStart);

            for (Couple<Vec3> rayPoint : rayPoints) {
               Vec3 start = blockCorner.add((Vec3)rayPoint.getFirst());
               Vec3 end = blockCorner.add((Vec3)rayPoint.getSecond());
               ClipContext context = new ClipContext(start, end, Block.OUTLINE, Fluid.ANY, CollisionContext.empty());
               BlockHitResult clip = level.clip(context);
               if (clip.getType() != Type.MISS) {
                  ActiveSableCompanion helper = Sable.HELPER;
                  SubLevel hitSublevel = helper.getContaining(level, clip.getBlockPos());
                  if (hitSublevel != parentSublevel) {
                     Vec3 hitDiff = helper.projectOutOfSubLevel(level, clip.getLocation()).subtract(helper.projectOutOfSubLevel(level, start));
                     double inverseHitPercentage;
                     if (clip.isInside()) {
                        inverseHitPercentage = 1.0;
                     } else {
                        float curveScaling = 2.0F;
                        inverseHitPercentage = Math.clamp(2.0 - hitDiff.length() / startEndDistance * 2.0, 0.0, 1.0);
                     }

                     Vec3 modifiedDiff = hitDiff.normalize().scale(inverseHitPercentage).scale(1.0 / (double)rayPoints.size());
                     force.add(modifiedDiff.x, modifiedDiff.y, modifiedDiff.z);
                     if (hitSublevel instanceof ServerSubLevel hitServerSubLevel) {
                        ForceTotal forceTotal = hitServerSubLevel.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.PROPULSION.get()).getForceTotal();
                        Vector3d impulseLocation = JOMLConversion.toJOML(clip.getLocation());
                        Vector3d impulse = hitServerSubLevel.logicalPose()
                           .transformNormalInverse(JOMLConversion.toJOML(modifiedDiff))
                           .mul(-1.0)
                           .mul(getFanMagnitudeCalculation(parentSublevel, level, fbe) * timeStep);
                        forceTotal.applyImpulseAtPoint(hitServerSubLevel.getMassTracker(), impulseLocation, impulse);
                     }
                  }
               }
            }

            if (force.length() > 1.0E-8) {
               force.mul(getFanMagnitudeCalculation(parentSublevel, level, fbe) * timeStep);
               parentSublevel.logicalPose().transformNormalInverse(force);
            }

            return force;
         } else {
            return null;
         }
      }
   }

   private static double getFanMagnitudeCalculation(SubLevel parentSublevel, Level level, EncasedFanBlockEntity fbe) {
      float scale = ((Direction)fbe.getBlockState().getValue(EncasedFanBlock.FACING)).getAxisDirection() == AxisDirection.POSITIVE ? -1.0F : 1.0F;
      double airPressure = DimensionPhysicsData.getAirPressure(
         level, parentSublevel.logicalPose().transformPosition(JOMLConversion.atCenterOf(fbe.getBlockPos()))
      );
      int magnitude = 5;
      int softScaling = 4;
      float signumBefore = Math.signum(fbe.getSpeed());
      float speed = Math.abs(fbe.getSpeed());
      int maxSpeed = (Integer)AllConfigs.server().kinetics.maxRotationSpeed.get();
      float halfSpeed = (float)maxSpeed / 2.0F;
      if (speed >= halfSpeed) {
         speed = (speed - halfSpeed) / 4.0F + halfSpeed;
      }

      speed *= signumBefore;
      return (double)(5.0F * scale * speed) * airPressure;
   }

   public static void spawnWindHitParticle(Level level, SubLevel subLevel, BlockHitResult clip, Vector3dc origin, double airSpeed) {
      Vector3d end = JOMLConversion.toJOML(clip.getLocation());
      if (clip.getType() != Type.MISS && origin.distanceSquared(end.x, end.y, end.z) > 1.0) {
         BlockState hitState = level.getBlockState(clip.getBlockPos());
         net.minecraft.world.level.material.Fluid fluid = level.getFluidState(clip.getBlockPos()).getType();
         Vector3d start = new Vector3d(origin);
         if (subLevel != null) {
            subLevel.logicalPose().transformPosition(start);
         }

         Vector3d normal = new Vector3d((double)clip.getDirection().getStepX(), (double)clip.getDirection().getStepY(), (double)clip.getDirection().getStepZ());
         SubLevel other = Sable.HELPER.getContaining(level, clip.getBlockPos());
         if (other != null) {
            other.logicalPose().transformNormal(normal);
            other.logicalPose().transformPosition(end);
         }

         Vector3d offset = new Vector3d(level.random.nextDouble() * 2.0 - 1.0, level.random.nextDouble() * 2.0 - 1.0, level.random.nextDouble() * 2.0 - 1.0);
         projectOntoPlane(offset, normal, 1.0);
         end.add(offset);
         Vector3d delta = end.sub(start, new Vector3d());
         Vector3d particleVelocity = projectOntoPlane(new Vector3d(delta), normal, 1.0);
         particleVelocity.mul(airSpeed);
         particleVelocity.fma(0.25, normal);
         end.fma(0.1, normal);
         if (other != null) {
            other.logicalPose().orientation().transformInverse(particleVelocity);
         }

         level.addParticle(ParticleTypes.DUST_PLUME, end.x, end.y, end.z, particleVelocity.x, particleVelocity.y, particleVelocity.z);
         if (hitState.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            level.addParticle(
               new BlockParticleOption(ParticleTypes.BLOCK, hitState), end.x, end.y, end.z, particleVelocity.x, particleVelocity.y, particleVelocity.z
            );
         } else if (fluid.isSame(Fluids.WATER)) {
            level.addParticle(ParticleTypes.SPLASH, end.x, end.y, end.z, 0.0, 0.0, 0.0);
            if (level.getRandom().nextDouble() < 0.2) {
               level.addParticle(ParticleTypes.BUBBLE, end.x, end.y, end.z, 0.0, 0.0, 0.0);
            }
         } else if (fluid.isSame(Fluids.LAVA)) {
            level.addParticle(ParticleTypes.SMOKE, end.x, end.y, end.z, 0.0, 0.0, 0.0);
            if (level.getRandom().nextDouble() < 0.2) {
               level.addParticle(ParticleTypes.LAVA, end.x, end.y, end.z, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   private static Vector3d projectOntoPlane(Vector3d x, Vector3dc planeNormal, double scale) {
      return x.fma(-scale * x.dot(planeNormal), planeNormal);
   }
}
