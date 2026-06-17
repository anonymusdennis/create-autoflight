package com.simibubi.create.content.fluids.pipes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public class SmartFluidPipeBlockEntity extends SmartBlockEntity implements Clearable {
   private FilteringBehaviour filter;

   public SmartFluidPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(new SmartFluidPipeBlockEntity.SmartPipeBehaviour(this));
      behaviours.add(
         this.filter = new FilteringBehaviour(this, new SmartFluidPipeBlockEntity.SmartPipeFilterSlot()).forFluids().withCallback(this::onFilterChanged)
      );
      this.registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
   }

   public void clearContent() {
      this.filter.setFilter(ItemStack.EMPTY);
   }

   private void onFilterChanged(ItemStack newFilter) {
      if (!this.level.isClientSide) {
         FluidPropagator.propagateChangedPipe(this.level, this.worldPosition, this.getBlockState());
      }
   }

   class SmartPipeBehaviour extends StraightPipeBlockEntity.StraightPipeFluidTransportBehaviour {
      public SmartPipeBehaviour(SmartBlockEntity be) {
         super(be);
      }

      @Override
      public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction direction) {
         return !fluid.isEmpty() && (SmartFluidPipeBlockEntity.this.filter == null || !SmartFluidPipeBlockEntity.this.filter.test(fluid))
            ? false
            : super.canPullFluidFrom(fluid, state, direction);
      }

      @Override
      public boolean canHaveFlowToward(BlockState state, Direction direction) {
         return state.getBlock() instanceof SmartFluidPipeBlock && SmartFluidPipeBlock.getPipeAxis(state) == direction.getAxis();
      }
   }

   static class SmartPipeFilterSlot extends ValueBoxTransform {
      @Override
      public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
         AttachFace face = (AttachFace)state.getValue(SmartFluidPipeBlock.FACE);
         float y = face == AttachFace.CEILING ? 0.55F : (face == AttachFace.WALL ? 11.4F : 15.45F);
         float z = face == AttachFace.CEILING ? 4.6F : (face == AttachFace.WALL ? 0.55F : 4.625F);
         return VecHelper.rotateCentered(VecHelper.voxelSpace(8.0, (double)y, (double)z), (double)this.angleY(state), Axis.Y);
      }

      @Override
      public float getScale() {
         return super.getScale() * 1.02F;
      }

      @Override
      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         AttachFace face = (AttachFace)state.getValue(SmartFluidPipeBlock.FACE);
         ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(this.angleY(state))).rotateXDegrees(face == AttachFace.CEILING ? -45.0F : 45.0F);
      }

      protected float angleY(BlockState state) {
         AttachFace face = (AttachFace)state.getValue(SmartFluidPipeBlock.FACE);
         float horizontalAngle = AngleHelper.horizontalAngle((Direction)state.getValue(SmartFluidPipeBlock.FACING));
         if (face == AttachFace.WALL) {
            horizontalAngle += 180.0F;
         }

         return horizontalAngle;
      }
   }
}
