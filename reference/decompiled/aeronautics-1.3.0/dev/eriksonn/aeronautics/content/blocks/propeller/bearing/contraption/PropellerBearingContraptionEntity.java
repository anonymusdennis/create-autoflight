package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.index.AeroEntityTypes;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

public class PropellerBearingContraptionEntity extends ControlledContraptionEntity {
   public Quaternionf tiltQuat = new Quaternionf();
   public Quaternionf previousTiltQuat = new Quaternionf();
   public Direction direction = Direction.UP;
   Quaternionf interpolatedQuat = new Quaternionf();

   public PropellerBearingContraptionEntity(EntityType<?> type, Level world) {
      super(type, world);
   }

   public static ControlledContraptionEntity create(Level world, IControlContraption controller, Contraption contraption) {
      PropellerBearingContraptionEntity entity = new PropellerBearingContraptionEntity(
         (EntityType<?>)AeroEntityTypes.PROPELLER_CONTROLLED_CONTRAPTION.get(), world
      );
      entity.setControllerPos(controller.getBlockPosition());
      entity.setContraption(contraption);
      return entity;
   }

   public PropellerBearingBlockEntity getBearingEntity() {
      if (this.controllerPos == null) {
         return null;
      } else if (!this.level().isLoaded(this.controllerPos)) {
         return null;
      } else {
         BlockEntity te = this.level().getBlockEntity(this.controllerPos);
         return !(te instanceof PropellerBearingBlockEntity) ? null : (PropellerBearingBlockEntity)te;
      }
   }

   Quaternionf getInterpolatedQuat(float partialTick) {
      return this.previousTiltQuat.slerp(this.tiltQuat, partialTick, this.interpolatedQuat);
   }

   public Vec3 applyRotation(Vec3 localPos, float partialTicks) {
      localPos = VecHelper.rotate(localPos, (double)this.getAngle(partialTicks), this.rotationAxis);
      return SimMathUtils.rotateQuatReverse(localPos, this.getInterpolatedQuat(partialTicks));
   }

   public Vec3 reverseRotation(Vec3 localPos, float partialTicks) {
      localPos = SimMathUtils.rotateQuat(localPos, this.getInterpolatedQuat(partialTicks));
      return VecHelper.rotate(localPos, (double)(-this.getAngle(partialTicks)), this.rotationAxis);
   }

   public float getAngle(float partialTicks) {
      if (this.getController() instanceof PropellerBearingBlockEntity tile && tile.disassemblySlowdown) {
         return tile.getInterpolatedAngle(partialTicks - 1.0F);
      }

      return partialTicks == 1.0F ? this.angle : AngleHelper.angleLerp((double)partialTicks, (double)this.prevAngle, (double)this.angle);
   }

   @OnlyIn(Dist.CLIENT)
   public void applyLocalTransforms(PoseStack poseStack, float partialTicks) {
      float angle = this.getAngle(partialTicks);
      Axis axis = this.getRotationAxis();
      Vec3 normal = new Vec3((double)this.direction.getStepX(), (double)this.direction.getStepY(), (double)this.direction.getStepZ());
      normal = normal.scale(0.75);
      ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(poseStack).nudge(this.getId()))
                     .center())
                  .translate(normal.scale(-1.0)))
               .rotate(this.getInterpolatedQuat(partialTicks))
               .translate(normal))
            .rotateDegrees(angle, axis))
         .uncenter();
   }

   public void setControllerPos(BlockPos controllerPos) {
      this.controllerPos = controllerPos;
   }

   public void setContraption(Contraption contraption) {
      super.setContraption(contraption);
   }
}
