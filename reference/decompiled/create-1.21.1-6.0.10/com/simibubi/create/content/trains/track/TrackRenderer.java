package com.simibubi.create.content.trains.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TrackRenderer extends SafeBlockEntityRenderer<TrackBlockEntity> {
   public TrackRenderer(Context context) {
   }

   protected void renderSafe(TrackBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      Level level = be.getLevel();
      if (!VisualizationManager.supportsVisualization(level)) {
         VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
         be.connections.values().forEach(bc -> renderBezierTurn(level, bc, ms, vb));
      }
   }

   public static void renderBezierTurn(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb) {
      if (bc.isPrimary()) {
         ms.pushPose();
         BlockPos bePosition = (BlockPos)bc.bePositions.getFirst();
         BlockState air = Blocks.AIR.defaultBlockState();
         BezierConnection.SegmentAngles segment = bc.getBakedSegments();
         renderGirder(level, bc, ms, vb, bePosition);

         for (int i = 1; i < segment.length; i++) {
            int light = LevelRenderer.getLightColor(level, segment.lightPosition[i].offset(bePosition));
            TrackMaterial.TrackModelHolder modelHolder = bc.getMaterial().getModelHolder();
            ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(modelHolder.tie(), air).mulPose(segment.tieTransform[i].pose()))
                  .mulNormal(segment.tieTransform[i].normal()))
               .light(light)
               .renderInto(ms, vb);

            for (boolean first : Iterate.trueAndFalse) {
               Pose transform = (Pose)segment.railTransforms[i].get(first);
               ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(first ? modelHolder.leftSegment() : modelHolder.rightSegment(), air)
                        .mulPose(transform.pose()))
                     .mulNormal(transform.normal()))
                  .light(light)
                  .renderInto(ms, vb);
            }
         }

         ms.popPose();
      }
   }

   private static void renderGirder(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb, BlockPos tePosition) {
      if (bc.hasGirder) {
         BlockState air = Blocks.AIR.defaultBlockState();
         BezierConnection.GirderAngles segment = bc.getBakedGirders();

         for (int i = 1; i < segment.length; i++) {
            int light = LevelRenderer.getLightColor(level, segment.lightPosition[i].offset(tePosition));

            for (boolean first : Iterate.trueAndFalse) {
               Pose beamTransform = (Pose)segment.beams[i].get(first);
               ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.GIRDER_SEGMENT_MIDDLE, air).mulPose(beamTransform.pose()))
                     .mulNormal(beamTransform.normal()))
                  .light(light)
                  .renderInto(ms, vb);

               for (boolean top : Iterate.trueAndFalse) {
                  Pose beamCapTransform = (Pose)((Couple)segment.beamCaps[i].get(top)).get(first);
                  ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(
                              top ? AllPartialModels.GIRDER_SEGMENT_TOP : AllPartialModels.GIRDER_SEGMENT_BOTTOM, air
                           )
                           .mulPose(beamCapTransform.pose()))
                        .mulNormal(beamCapTransform.normal()))
                     .light(light)
                     .renderInto(ms, vb);
               }
            }
         }
      }
   }

   public static Vec3 getModelAngles(Vec3 normal, Vec3 diff) {
      double diffX = diff.x();
      double diffY = diff.y();
      double diffZ = diff.z();
      double len = (double)Mth.sqrt((float)(diffX * diffX + diffZ * diffZ));
      double yaw = Mth.atan2(diffX, diffZ);
      double pitch = Mth.atan2(len, diffY) - (Math.PI / 2);
      Vec3 yawPitchNormal = VecHelper.rotate(
         VecHelper.rotate(new Vec3(0.0, 1.0, 0.0), (double)AngleHelper.deg(pitch), Axis.X), (double)AngleHelper.deg(yaw), Axis.Y
      );
      double signum = Math.signum(yawPitchNormal.dot(normal));
      if (Math.abs(signum) < 0.5) {
         signum = yawPitchNormal.distanceToSqr(normal) < 0.5 ? -1.0 : 1.0;
      }

      double dot = diff.cross(normal).normalize().dot(yawPitchNormal);
      double roll = Math.acos(Mth.clamp(dot, -1.0, 1.0)) * signum;
      return new Vec3(pitch, yaw, roll);
   }

   public boolean shouldRenderOffScreen(TrackBlockEntity pBlockEntity) {
      return true;
   }

   public int getViewDistance() {
      return 192;
   }
}
