package dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller;

import com.simibubi.create.Create;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlockEntity;
import dev.eriksonn.aeronautics.content.particle.PropellerAirParticleData;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4d;
import org.joml.Matrix3f;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class SmartPropellerBlockEntity extends BasePropellerBlockEntity {
   public final Vector3d thrustDir;
   public LerpedFloat hingeAngle;

   public SmartPropellerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
      this.hingeAngle = LerpedFloat.linear().startWithValue(state.getValue(SmartPropellerBlock.CEILING) ? 180.0 : 0.0).chase(0.0, 0.1F, Chaser.LINEAR);
      this.thrustDir = new Vector3d();
      this.prop.setThrustDirection(this.thrustDir);
   }

   @Override
   public double getConfigThrust() {
      return (Double)AeroConfig.server().physics.smartPropellerThrust.get();
   }

   @Override
   public double getConfigAirflow() {
      return (Double)AeroConfig.server().physics.smartPropellerAirflow.get();
   }

   @Override
   public float getRadius() {
      return 1.0F;
   }

   @Override
   public float getOffset() {
      return 0.625F;
   }

   @Override
   public Direction getBlockDirection() {
      return Direction.UP;
   }

   @Override
   public void tick() {
      super.tick();
      this.hingeAngle.tickChaser();
      this.hingeAngle
         .setValue((double)this.getHingeAngle((Axis)this.getBlockState().getValue(SmartPropellerBlock.HORIZONTAL_AXIS), this.hingeAngle.getValue()));
      this.setThrustDirection();
   }

   public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
      this.setThrustDirection();
      if (this.isActive()) {
         super.applyForces(subLevel, JOMLConversion.toMojang(this.thrustDir), timeStep);
      }
   }

   private void setThrustDirection() {
      Vector3d thrustDirection = new Vector3d(0.0, 1.0, 0.0);
      Direction dir = Direction.get(AxisDirection.POSITIVE, (Axis)this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_AXIS)).getClockWise();
      thrustDirection.rotate(
         new Quaterniond(
            new AxisAngle4d(-Math.toRadians((double)this.hingeAngle.getValue()), (double)dir.getStepX(), (double)dir.getStepY(), (double)dir.getStepZ())
         )
      );
      this.thrustDir.set(thrustDirection);
   }

   @Override
   public void onActiveTick() {
      this.prop.pushEntities();
      this.spawnParticles();
   }

   public void spawnParticles() {
      Direction dir = Direction.get(AxisDirection.POSITIVE, (Axis)this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_AXIS)).getClockWise();
      Quaterniond rot = new Quaterniond(
         new AxisAngle4d(-Math.toRadians((double)this.hingeAngle.getValue()), (double)dir.getStepX(), (double)dir.getStepY(), (double)dir.getStepZ())
      );
      double particleCount = (double)(1.0F + Create.RANDOM.nextFloat() - 1.0F);
      particleCount = Math.min(particleCount, 10.0);

      for (int i = 0; (double)i < particleCount; i++) {
         double R = (double)this.getRadius() * Math.sqrt((double)Create.RANDOM.nextFloat());
         double angle = (Math.PI * 2) * (double)Create.RANDOM.nextFloat();
         Vec3 particlePos = new Vec3(Math.cos(angle) * R, (double)this.getOffset(), Math.sin(angle) * R);
         Vec3 speedVector = new Vec3(0.0, this.getAirflow() / 40.0, 0.0);
         particlePos = SimMathUtils.rotateQuatReverse(particlePos, rot);
         speedVector = SimMathUtils.rotateQuatReverse(speedVector, rot);
         particlePos = particlePos.add(VecHelper.getCenterOf(this.getBlockPos()));
         this.level
            .addParticle(
               new PropellerAirParticleData(true, false), particlePos.x, particlePos.y, particlePos.z, speedVector.x(), speedVector.y(), speedVector.z()
            );
      }
   }

   public float getLerpedHingeAngle(float partialTick) {
      return this.hingeAngle.getValue(partialTick);
   }

   public float getHingeAngle(Axis horizontal, float hingeAngle) {
      SubLevel subLevel = Sable.HELPER.getContaining(this);
      if (subLevel != null) {
         Quaterniond Q = new Quaterniond(subLevel.logicalPose().orientation());
         Quaterniond pendulumOrientation = new Quaterniond();
         pendulumOrientation.set(Q);
         if (horizontal == Axis.Z) {
            pendulumOrientation.mul(new Quaterniond(new AxisAngle4d(Math.toRadians(90.0), 0.0, 1.0, 0.0)));
         }

         Matrix3f rotMatrix = new Matrix3f();
         rotMatrix.set(pendulumOrientation);
         float pitch = (float)Math.atan2((double)rotMatrix.m01, (double)rotMatrix.m11);
         hingeAngle = -((float)Math.toDegrees((double)pitch));
         if (horizontal == Axis.X) {
            hingeAngle *= -1.0F;
         }

         hingeAngle = Mth.clamp(hingeAngle, -45.0F, 45.0F);
         if ((Boolean)this.getBlockState().getValue(SmartPropellerBlock.CEILING)) {
            hingeAngle = 180.0F - hingeAngle;
         }
      }

      if (subLevel == null) {
         hingeAngle = this.getBlockState().getValue(SmartPropellerBlock.CEILING) ? 180.0F : 0.0F;
      }

      return hingeAngle;
   }
}
