package com.simibubi.create.content.logistics.factoryBoard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

class FactoryPanelSlotPositioning extends ValueBoxTransform {
   public FactoryPanelBlock.PanelSlot slot;

   public FactoryPanelSlotPositioning(FactoryPanelBlock.PanelSlot slot) {
      this.slot = slot;
   }

   @Override
   public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
      return getCenterOfSlot(state, this.slot);
   }

   public static Vec3 getCenterOfSlot(BlockState state, FactoryPanelBlock.PanelSlot slot) {
      Vec3 vec = new Vec3(0.25 + (double)slot.xOffset * 0.5, 0.09375, 0.25 + (double)slot.yOffset * 0.5);
      vec = VecHelper.rotateCentered(vec, 180.0, Axis.Y);
      vec = VecHelper.rotateCentered(vec, (double)((180.0F / (float)Math.PI) * FactoryPanelBlock.getXRot(state) + 90.0F), Axis.X);
      return VecHelper.rotateCentered(vec, (double)((180.0F / (float)Math.PI) * FactoryPanelBlock.getYRot(state)), Axis.Y);
   }

   @Override
   public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
      Vec3 offset = this.getLocalOffset(level, pos, state);
      return offset == null ? false : localHit.distanceTo(offset) < (double)(this.scale / 2.0F);
   }

   @Override
   public float getScale() {
      return super.getScale();
   }

   @Override
   public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
      ((PoseTransformStack)TransformStack.of(ms).rotate(FactoryPanelBlock.getYRot(state) + (float) Math.PI, Direction.UP))
         .rotate(-FactoryPanelBlock.getXRot(state), Direction.EAST);
   }
}
