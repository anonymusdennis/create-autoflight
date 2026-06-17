package com.simibubi.create.content.kinetics.base;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RotatingInstance extends ColoredLitOverlayInstance {
   public static final float SPEED_MULTIPLIER = 6.0F;
   public byte rotationAxisX;
   public byte rotationAxisY;
   public byte rotationAxisZ;
   public float x;
   public float y;
   public float z;
   public float rotationalSpeed;
   public float rotationOffset;
   public final Quaternionf rotation = new Quaternionf();

   public RotatingInstance(InstanceType<? extends RotatingInstance> type, InstanceHandle handle) {
      super(type, handle);
   }

   public static int colorFromBE(KineticBlockEntity be) {
      return be.hasNetwork() ? Color.generateFromLong(be.network).getRGB() : 16777215;
   }

   public RotatingInstance setup(KineticBlockEntity blockEntity) {
      BlockState blockState = blockEntity.getBlockState();
      Axis axis = KineticBlockEntityVisual.rotationAxis(blockState);
      return this.setup(blockEntity, axis, blockEntity.getSpeed());
   }

   public RotatingInstance setup(KineticBlockEntity blockEntity, Axis axis) {
      return this.setup(blockEntity, axis, blockEntity.getSpeed());
   }

   public RotatingInstance setup(KineticBlockEntity blockEntity, float speed) {
      BlockState blockState = blockEntity.getBlockState();
      Axis axis = KineticBlockEntityVisual.rotationAxis(blockState);
      return this.setup(blockEntity, axis, speed);
   }

   public RotatingInstance setup(KineticBlockEntity blockEntity, Axis axis, float speed) {
      BlockState blockState = blockEntity.getBlockState();
      BlockPos pos = blockEntity.getBlockPos();
      return this.setRotationAxis(axis)
         .setRotationalSpeed(speed * 6.0F)
         .setRotationOffset(KineticBlockEntityVisual.rotationOffset(blockState, axis, pos) + (float)blockEntity.getRotationAngleOffset(axis));
   }

   public RotatingInstance rotateToFace(Axis axis) {
      Direction orientation = Direction.get(AxisDirection.POSITIVE, axis);
      return this.rotateToFace(orientation);
   }

   public RotatingInstance rotateToFace(Direction from, Axis axis) {
      Direction orientation = Direction.get(AxisDirection.POSITIVE, axis);
      return this.rotateToFace(from, orientation);
   }

   public RotatingInstance rotateToFace(Direction orientation) {
      return this.rotateToFace((float)orientation.getStepX(), (float)orientation.getStepY(), (float)orientation.getStepZ());
   }

   public RotatingInstance rotateToFace(Direction from, Direction orientation) {
      return this.rotateTo(
         (float)from.getStepX(),
         (float)from.getStepY(),
         (float)from.getStepZ(),
         (float)orientation.getStepX(),
         (float)orientation.getStepY(),
         (float)orientation.getStepZ()
      );
   }

   public RotatingInstance rotateToFace(float stepX, float stepY, float stepZ) {
      return this.rotateTo(0.0F, 1.0F, 0.0F, stepX, stepY, stepZ);
   }

   public RotatingInstance rotateTo(float fromX, float fromY, float fromZ, float toX, float toY, float toZ) {
      this.rotation.rotateTo(fromX, fromY, fromZ, toX, toY, toZ);
      return this;
   }

   public RotatingInstance setRotationAxis(Axis axis) {
      Direction orientation = Direction.get(AxisDirection.POSITIVE, axis);
      return this.setRotationAxis(orientation.step());
   }

   public RotatingInstance setRotationAxis(Vector3f axis) {
      return this.setRotationAxis(axis.x(), axis.y(), axis.z());
   }

   public RotatingInstance setRotationAxis(float rotationAxisX, float rotationAxisY, float rotationAxisZ) {
      this.rotationAxisX = (byte)((int)(rotationAxisX * 127.0F));
      this.rotationAxisY = (byte)((int)(rotationAxisY * 127.0F));
      this.rotationAxisZ = (byte)((int)(rotationAxisZ * 127.0F));
      return this;
   }

   public RotatingInstance setPosition(Vec3i pos) {
      return this.setPosition((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
   }

   public RotatingInstance setPosition(Vector3f pos) {
      return this.setPosition(pos.x(), pos.y(), pos.z());
   }

   public RotatingInstance setPosition(float x, float y, float z) {
      this.x = x;
      this.y = y;
      this.z = z;
      return this;
   }

   public RotatingInstance nudge(float x, float y, float z) {
      this.x += x;
      this.y += y;
      this.z += z;
      return this;
   }

   public RotatingInstance setColor(KineticBlockEntity blockEntity) {
      this.colorRgb(colorFromBE(blockEntity));
      return this;
   }

   public RotatingInstance setColor(Color c) {
      this.color(c.getRed(), c.getGreen(), c.getBlue());
      return this;
   }

   public RotatingInstance setRotationalSpeed(float rotationalSpeed) {
      this.rotationalSpeed = rotationalSpeed;
      return this;
   }

   public RotatingInstance setRotationOffset(float rotationOffset) {
      this.rotationOffset = rotationOffset;
      return this;
   }
}
