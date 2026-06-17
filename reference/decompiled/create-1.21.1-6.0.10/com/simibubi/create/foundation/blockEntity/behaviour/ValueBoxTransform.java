package com.simibubi.create.foundation.blockEntity.behaviour;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.function.Function;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

public abstract class ValueBoxTransform {
   protected float scale = this.getScale();

   public abstract Vec3 getLocalOffset(LevelAccessor var1, BlockPos var2, BlockState var3);

   public abstract void rotate(LevelAccessor var1, BlockPos var2, BlockState var3, PoseStack var4);

   public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
      Vec3 offset = this.getLocalOffset(level, pos, state);
      return offset == null ? false : localHit.distanceTo(offset) < (double)(this.scale / 2.0F);
   }

   public void transform(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
      Vec3 position = this.getLocalOffset(level, pos, state);
      if (position != null) {
         ms.translate(position.x, position.y, position.z);
         this.rotate(level, pos, state, ms);
         ms.scale(this.scale, this.scale, this.scale);
      }
   }

   public boolean shouldRender(LevelAccessor level, BlockPos pos, BlockState state) {
      return !state.isAir() && this.getLocalOffset(level, pos, state) != null;
   }

   public int getOverrideColor() {
      return -1;
   }

   protected Vec3 rotateHorizontally(BlockState state, Vec3 vec) {
      float yRot = 0.0F;
      if (state.hasProperty(BlockStateProperties.FACING)) {
         yRot = AngleHelper.horizontalAngle((Direction)state.getValue(BlockStateProperties.FACING));
      }

      if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
         yRot = AngleHelper.horizontalAngle((Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING));
      }

      return VecHelper.rotateCentered(vec, (double)yRot, Axis.Y);
   }

   public float getScale() {
      return 0.5F;
   }

   public float getFontScale() {
      return 0.015625F;
   }

   public abstract static class Dual extends ValueBoxTransform {
      protected boolean first;

      public Dual(boolean first) {
         this.first = first;
      }

      public boolean isFirst() {
         return this.first;
      }

      public static Pair<ValueBoxTransform, ValueBoxTransform> makeSlots(Function<Boolean, ? extends ValueBoxTransform.Dual> factory) {
         return Pair.of(factory.apply(true), factory.apply(false));
      }

      @Override
      public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
         Vec3 offset = this.getLocalOffset(level, pos, state);
         return offset == null ? false : localHit.distanceTo(offset) < (double)(this.scale / 3.5F);
      }
   }

   public abstract static class Sided extends ValueBoxTransform {
      protected Direction direction = Direction.UP;

      public ValueBoxTransform.Sided fromSide(Direction direction) {
         this.direction = direction;
         return this;
      }

      @Override
      public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
         Vec3 location = this.getSouthLocation();
         location = VecHelper.rotateCentered(location, (double)AngleHelper.horizontalAngle(this.getSide()), Axis.Y);
         return VecHelper.rotateCentered(location, (double)AngleHelper.verticalAngle(this.getSide()), Axis.X);
      }

      protected abstract Vec3 getSouthLocation();

      @Override
      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         float yRot = AngleHelper.horizontalAngle(this.getSide()) + 180.0F;
         float xRot = this.getSide() == Direction.UP ? 90.0F : (this.getSide() == Direction.DOWN ? 270.0F : 0.0F);
         ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(yRot)).rotateXDegrees(xRot);
      }

      @Override
      public boolean shouldRender(LevelAccessor level, BlockPos pos, BlockState state) {
         return super.shouldRender(level, pos, state) && this.isSideActive(state, this.getSide());
      }

      @Override
      public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
         return this.isSideActive(state, this.getSide()) && super.testHit(level, pos, state, localHit);
      }

      protected boolean isSideActive(BlockState state, Direction direction) {
         return true;
      }

      public Direction getSide() {
         return this.direction;
      }
   }
}
