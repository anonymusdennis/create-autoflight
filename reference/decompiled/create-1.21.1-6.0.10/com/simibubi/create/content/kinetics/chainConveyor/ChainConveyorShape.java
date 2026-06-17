package com.simibubi.create.content.kinetics.chainConveyor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class ChainConveyorShape {
   @Nullable
   public abstract Vec3 intersect(Vec3 var1, Vec3 var2);

   public abstract float getChainPosition(Vec3 var1);

   protected abstract void drawOutline(BlockPos var1, PoseStack var2, VertexConsumer var3);

   public abstract Vec3 getVec(BlockPos var1, float var2);

   public static class ChainConveyorBB extends ChainConveyorShape {
      Vec3 lb;
      Vec3 rb;
      final double radius = 0.875;
      AABB bounds;

      public ChainConveyorBB(Vec3 center) {
         this.lb = center.add(0.0, 0.0, 0.0);
         this.rb = center.add(0.0, 0.5, 0.0);
         this.bounds = new AABB(this.lb, this.rb).inflate(1.0, 0.0, 1.0);
      }

      @Override
      public Vec3 intersect(Vec3 from, Vec3 to) {
         return (Vec3)this.bounds.clip(from, to).orElse(null);
      }

      @Override
      public void drawOutline(BlockPos anchor, PoseStack ms, VertexConsumer vb) {
         TrackBlockOutline.renderShape(AllShapes.CHAIN_CONVEYOR_INTERACTION, ms, vb, null);
      }

      @Override
      public float getChainPosition(Vec3 intersection) {
         Vec3 diff = this.bounds.getCenter().subtract(intersection);
         float angle = (float)(180.0F / (float)Math.PI * Mth.atan2(diff.x, diff.z) + 360.0 + 180.0) % 360.0F;
         return (float)Math.round(angle / 45.0F) * 45.0F;
      }

      @Override
      public Vec3 getVec(BlockPos anchor, float position) {
         Vec3 point = this.bounds.getCenter();
         point = point.add(VecHelper.rotate(new Vec3(0.0, 0.0, 0.875), (double)position, Axis.Y));
         return point.add(Vec3.atLowerCornerOf(anchor)).add(0.0, -0.125, 0.0);
      }
   }

   public static class ChainConveyorOBB extends ChainConveyorShape {
      BlockPos connection;
      double yaw;
      double pitch;
      AABB bounds;
      Vec3 pivot;
      final double radius = 0.175;
      VoxelShape voxelShape;
      Vec3[] linePoints;

      public ChainConveyorOBB(BlockPos connection, Vec3 start, Vec3 end) {
         this.connection = connection;
         Vec3 diff = end.subtract(start);
         double d = diff.length();
         double dxz = diff.multiply(1.0, 0.0, 1.0).length();
         this.yaw = 180.0F / (float)Math.PI * Mth.atan2(diff.x, diff.z);
         this.pitch = 180.0F / (float)Math.PI * Mth.atan2(-diff.y, dxz);
         this.bounds = new AABB(start, start).expandTowards(new Vec3(0.0, 0.0, d)).inflate(0.175, 0.175, 0.0);
         this.pivot = start;
         this.voxelShape = Shapes.create(this.bounds);
      }

      @Override
      public Vec3 intersect(Vec3 from, Vec3 to) {
         from = this.counterTransform(from);
         to = this.counterTransform(to);
         Vec3 result = (Vec3)this.bounds.clip(from, to).orElse(null);
         return result == null ? null : this.transform(result);
      }

      private Vec3 counterTransform(Vec3 from) {
         from = from.subtract(this.pivot);
         from = VecHelper.rotate(from, -this.yaw, Axis.Y);
         from = VecHelper.rotate(from, -this.pitch, Axis.X);
         return from.add(this.pivot);
      }

      private Vec3 transform(Vec3 result) {
         result = result.subtract(this.pivot);
         result = VecHelper.rotate(result, this.pitch, Axis.X);
         result = VecHelper.rotate(result, this.yaw, Axis.Y);
         return result.add(this.pivot);
      }

      @Override
      public void drawOutline(BlockPos anchor, PoseStack ms, VertexConsumer vb) {
         ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(ms).translate(this.pivot)).rotateYDegrees((float)this.yaw))
               .rotateXDegrees((float)this.pitch))
            .translateBack(this.pivot);
         TrackBlockOutline.renderShape(this.voxelShape, ms, vb, null);
      }

      @Override
      public float getChainPosition(Vec3 intersection) {
         int dots = (int)Math.round(Vec3.atLowerCornerOf(this.connection).length() - 3.0);
         double length = this.bounds.getZsize();
         double selection = Math.min(this.bounds.getZsize(), intersection.distanceTo(this.pivot));
         double margin = length - (double)dots;
         selection = Mth.clamp(selection - margin, 0.0, length - margin * 2.0);
         selection = (double)Math.round(selection);
         return (float)(selection + margin + 0.025);
      }

      @Override
      public Vec3 getVec(BlockPos anchor, float position) {
         float x = (float)this.bounds.getCenter().x;
         float y = (float)this.bounds.getCenter().y;
         Vec3 from = new Vec3((double)x, (double)y, this.bounds.minZ);
         Vec3 to = new Vec3((double)x, (double)y, this.bounds.maxZ);
         Vec3 point = from.lerp(to, Mth.clamp((double)position / from.distanceTo(to), 0.0, 1.0));
         point = this.transform(point);
         return point.add(Vec3.atLowerCornerOf(anchor));
      }
   }
}
