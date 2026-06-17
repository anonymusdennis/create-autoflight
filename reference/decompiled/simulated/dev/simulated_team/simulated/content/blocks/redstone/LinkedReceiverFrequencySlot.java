package dev.simulated_team.simulated.content.blocks.redstone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Dual;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class LinkedReceiverFrequencySlot extends Dual {
   Vec3 horizontal = VecHelper.voxelSpace(5.0, 3.0, 2.5);
   Vec3 vertical = VecHelper.voxelSpace(11.0, 2.5, 3.0);

   public LinkedReceiverFrequencySlot(boolean first) {
      super(first);
   }

   public Vec3 getLocalOffset(LevelAccessor levelAccessor, BlockPos blockPos, BlockState state) {
      Direction facing = (Direction)state.getValue(RedstoneLinkBlock.FACING);
      Vec3 location = this.vertical;
      if (facing.getAxis().isHorizontal()) {
         location = this.horizontal;
         if (this.isFirst()) {
            location = location.add(0.0, 0.625, 0.0);
         }

         return this.rotateHorizontally(state, location);
      } else {
         if (this.isFirst()) {
            location = location.add(0.0, 0.0, 0.625);
         }

         return VecHelper.rotateCentered(location, facing == Direction.DOWN ? 180.0 : 0.0, Axis.X);
      }
   }

   public void rotate(LevelAccessor levelAccessor, BlockPos blockPos, BlockState state, PoseStack poseStack) {
      Direction facing = (Direction)state.getValue(RedstoneLinkBlock.FACING);
      float yRot = facing.getAxis().isVertical() ? 0.0F : AngleHelper.horizontalAngle(facing) + 180.0F;
      float xRot = facing == Direction.UP ? 90.0F : (facing == Direction.DOWN ? 270.0F : 0.0F);
      ((PoseTransformStack)TransformStack.of(poseStack).rotateYDegrees(yRot)).rotateXDegrees(xRot);
   }

   public float getScale() {
      return 0.5F;
   }
}
