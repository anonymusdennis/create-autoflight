package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing;

import com.simibubi.create.content.contraptions.DirectionalExtenderScrollOptionSlot;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerActorBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class GyroscopicPropellerBearingBlockEntity extends PropellerBearingBlockEntity {
   private static final Vector3d PHYSICS_THRUST = new Vector3d();
   protected Quaternionf previousTiltQuat;
   protected Quaternionf tiltQuat;
   protected final Vector3d blockNormal = new Vector3d();
   protected final Vector3d tiltVector = new Vector3d(0.0, 1.0, 0.0);
   boolean powered = false;
   boolean initialized = false;
   double currentGravity;
   double physicsTimer = 0.0;

   public GyroscopicPropellerBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.tiltQuat = new Quaternionf();
      this.previousTiltQuat = new Quaternionf();
      this.tiltQuat.normalize();
   }

   @Override
   public PropellerActorBehaviour createProp() {
      return new GyroActorBehaviour<>(this);
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putBoolean("IsPowered", this.powered);
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.powered = compound.getBoolean("IsPowered");
      super.read(compound, registries, clientPacket);
   }

   @Override
   public void tick() {
      super.tick();
      this.physicsTimer = 0.0;
      Direction facing = (Direction)this.getBlockState().getValue(BlockStateProperties.FACING);
      this.blockNormal.set((double)facing.getStepX(), (double)facing.getStepY(), (double)facing.getStepZ());
      if (!this.initialized) {
         this.tiltVector.set(this.blockNormal);
         this.initialized = true;
         this.applyTilt();
      }

      this.previousTiltQuat.set(this.tiltQuat);
      this.updateSignal();
      if (!this.isVirtual()) {
         SubLevel subLevel = Sable.HELPER.getContaining(this);
         this.updateTilt(this.tiltVector, subLevel, 1.0);
         this.applyTilt();
         PropellerBearingContraptionEntity propellerContraption = this.getMovedContraption();
         if (propellerContraption != null) {
            propellerContraption.tiltQuat = new Quaternionf(this.tiltQuat);
            propellerContraption.previousTiltQuat = new Quaternionf(this.previousTiltQuat);
            propellerContraption.direction = (Direction)this.getBlockState().getValue(PropellerBearingBlock.FACING);
         }
      }
   }

   public void updateSignal() {
      this.powered = this.level.hasNeighborSignal(this.worldPosition);
   }

   public void forceTilt(BlockState state) {
      Direction facing = (Direction)state.getValue(GyroscopicPropellerBearingBlock.FACING);
      this.blockNormal.set((double)facing.getStepX(), (double)facing.getStepY(), (double)facing.getStepZ());
      this.tiltVector.set(this.blockNormal);
      this.applyTilt();
      this.previousTiltQuat.set(this.tiltQuat);
   }

   private void updateTilt(Vector3d tilt, SubLevel subLevel, double stepScale) {
      Level level = this.getLevel();
      Vector3dc gravityVector = DimensionPhysicsData.getGravity(level, Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.atCenterOf(this.getBlockPos())));
      this.currentGravity = gravityVector.length();
      Vector3d target = this.currentGravity > 0.0 && subLevel != null
         ? subLevel.logicalPose().orientation().transformInverse(new Vector3d(gravityVector).mul(-1.0 / this.currentGravity))
         : this.blockNormal;
      this.setTilt(tilt, target, 0.05 * stepScale);
   }

   public void setTilt(Vector3d tilt, Vector3d target, double maxStep) {
      if (this.powered) {
         target = this.blockNormal;
      }

      Direction direction = (Direction)this.getBlockState().getValue(PropellerBearingBlock.FACING);
      this.blockNormal.set((double)direction.getStepX(), (double)direction.getStepY(), (double)direction.getStepZ());
      SimMathUtils.clampIntoCone(target, this.blockNormal, Math.toRadians(12.0));
      target.lerp(this.blockNormal, 1.0 - this.getLerpDistance());
      Vector3d difference = new Vector3d(target).sub(tilt);
      if (difference.lengthSquared() > maxStep * maxStep) {
         tilt.add(difference.normalize().mul(maxStep));
      } else {
         tilt.set(target);
      }
   }

   public void setStrictTilt(Vector3d target, double lerpAmount, double maxStep) {
      if (this.powered) {
         target = this.blockNormal;
      }

      Direction direction = (Direction)this.getBlockState().getValue(PropellerBearingBlock.FACING);
      this.blockNormal.set((double)direction.getStepX(), (double)direction.getStepY(), (double)direction.getStepZ());
      SimMathUtils.clampIntoCone(target, this.blockNormal, Math.toRadians(12.0));
      target.lerp(this.blockNormal, 1.0 - lerpAmount);
      Vector3d difference = new Vector3d(target).sub(this.tiltVector);
      if (difference.lengthSquared() > maxStep * maxStep) {
         this.tiltVector.add(difference.normalize(maxStep));
      } else {
         this.tiltVector.set(target);
      }

      this.applyTilt();
   }

   private double getLerpDistance() {
      double lerpDistance = 1.0;
      if (this.getMovedContraption() == null) {
         lerpDistance = 0.0;
      }

      double currentSpeed = (double)Math.abs(this.getSpeed());
      if (currentSpeed < 1.0) {
         lerpDistance *= currentSpeed;
      }

      if (this.disassemblySlowdown) {
         lerpDistance *= (double)(this.slowdownController.getCountdown() / this.slowdownController.getMaxTime());
      }

      return lerpDistance;
   }

   public void applyTilt() {
      this.tiltVector.normalize();
      this.tiltQuat = SimMathUtils.getQuaternionfFromVectorRotation(this.blockNormal, this.tiltVector);
      this.thrustDirection.set(this.tiltVector);
      PropellerBearingContraptionEntity propellerContraption = this.getMovedContraption();
      if (propellerContraption != null) {
         propellerContraption.tiltQuat = new Quaternionf(this.tiltQuat);
         propellerContraption.direction = (Direction)this.getBlockState().getValue(PropellerBearingBlock.FACING);
      }
   }

   public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
      this.physicsTimer += timeStep * 20.0;
      PHYSICS_THRUST.set(this.tiltVector);
      this.updateTilt(PHYSICS_THRUST, subLevel, this.physicsTimer);
      this.thrustDirection.set(PHYSICS_THRUST);
      if (this.isActive()) {
         super.applyForces(subLevel, JOMLConversion.toMojang(this.thrustDirection), timeStep);
      }
   }

   public ValueBoxTransform getMovementModeSlot() {
      return new GyroscopicPropellerBearingBlockEntity.GyroscopicPropellerValueBoxTransform();
   }

   private static class GyroscopicPropellerValueBoxTransform extends DirectionalExtenderScrollOptionSlot {
      public GyroscopicPropellerValueBoxTransform() {
         super((state, d) -> {
            Axis axis = d.getAxis();
            Axis bearingAxis = ((Direction)state.getValue(BearingBlock.FACING)).getAxis();
            return bearingAxis != axis;
         });
      }

      public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
         return super.getLocalOffset(level, pos, state)
            .add(Vec3.atLowerCornerOf(((Direction)state.getValue(BlockStateProperties.FACING)).getNormal()).scale(-0.125));
      }
   }
}
