package com.simibubi.create.content.logistics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class FlapStuffs {
   public static final int FLAP_COUNT = 4;
   public static final float X_OFFSET = 0.0046875F;
   public static final float SEGMENT_STEP = -0.190625F;
   public static final Vec3 TUNNEL_PIVOT = VecHelper.voxelSpace(0.0, 10.0, 1.0);
   public static final Vec3 FUNNEL_PIVOT = VecHelper.voxelSpace(0.0, 10.0, 9.5);

   public static void renderFlaps(
      PoseStack ms, VertexConsumer vb, SuperByteBuffer flapBuffer, Vec3 pivot, Direction funnelFacing, float flapness, float zOffset, int light
   ) {
      float horizontalAngle = AngleHelper.horizontalAngle(funnelFacing.getOpposite());
      PoseTransformStack msr = TransformStack.of(ms);
      ms.pushPose();
      ((PoseTransformStack)((PoseTransformStack)msr.center()).rotateYDegrees(horizontalAngle)).uncenter();
      ms.translate(0.0046875F, 0.0F, zOffset);

      for (int segment = 0; segment < 4; segment++) {
         ms.pushPose();
         ((PoseTransformStack)((PoseTransformStack)msr.translate(pivot)).rotateXDegrees(flapAngle(flapness, segment))).translateBack(pivot);
         flapBuffer.light(light).renderInto(ms, vb);
         ms.popPose();
         ms.translate(-0.190625F, 0.0F, 0.0F);
      }

      ms.popPose();
   }

   public static float flapAngle(float flapness, int segment) {
      float intensity = segment == 3 ? 1.5F : (float)(segment + 1);
      float abs = Math.abs(flapness);
      float flapAngle = Mth.sin((float)((double)(1.0F - abs) * Math.PI * (double)intensity)) * 30.0F * flapness;
      if (flapness < 0.0F) {
         flapAngle *= 0.5F;
      }

      return flapAngle;
   }

   public static Matrix4f commonTransform(BlockPos visualPosition, Direction side, float baseZOffset) {
      float horizontalAngle = AngleHelper.horizontalAngle(side.getOpposite());
      return new Matrix4f()
         .translate((float)visualPosition.getX(), (float)visualPosition.getY(), (float)visualPosition.getZ())
         .translate(0.5F, 0.5F, 0.5F)
         .rotateY((float) (Math.PI / 180.0) * horizontalAngle)
         .translate(-0.5F, -0.5F, -0.5F)
         .translate(0.0046875F, 0.0F, baseZOffset);
   }

   public static class Visual {
      private final TransformedInstance[] flaps;
      private final Matrix4f commonTransform = new Matrix4f();
      private final Vec3 pivot;

      public Visual(InstancerProvider instancerProvider, Matrix4fc commonTransform, Vec3 pivot, Model flapModel) {
         this.pivot = pivot;
         this.commonTransform.set(commonTransform).translate((float)pivot.x, (float)pivot.y, (float)pivot.z);
         this.flaps = new TransformedInstance[4];
         instancerProvider.instancer(InstanceTypes.TRANSFORMED, flapModel).createInstances(this.flaps);
      }

      public void update(float f) {
         for (int segment = 0; segment < 4; segment++) {
            TransformedInstance flap = this.flaps[segment];
            ((TransformedInstance)((TransformedInstance)flap.setTransform(this.commonTransform).rotateXDegrees(FlapStuffs.flapAngle(f, segment)))
                  .translateBack(this.pivot))
               .translate((float)segment * -0.190625F, 0.0F, 0.0F)
               .setChanged();
         }
      }

      public void delete() {
         for (TransformedInstance flap : this.flaps) {
            flap.delete();
         }
      }

      public void updateLight(int light) {
         for (TransformedInstance flap : this.flaps) {
            flap.light(light).setChanged();
         }
      }

      public void collectCrumblingInstances(Consumer<Instance> consumer) {
         for (TransformedInstance flap : this.flaps) {
            consumer.accept(flap);
         }
      }
   }
}
