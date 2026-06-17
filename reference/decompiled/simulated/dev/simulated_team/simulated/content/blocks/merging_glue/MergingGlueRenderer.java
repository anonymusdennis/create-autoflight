package dev.simulated_team.simulated.content.blocks.merging_glue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class MergingGlueRenderer extends SmartBlockEntityRenderer<MergingGlueBlockEntity> {
   public MergingGlueRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(MergingGlueBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
      if (be.isController()) {
         MergingGlueBlockEntity other = be.getPartnerGlue();
         if (other != null) {
            SubLevel otherSubLevel = Sable.HELPER.getContainingClient(other);
            SubLevel subLevel = Sable.HELPER.getContainingClient(be);
            BlockPos blockPos = be.getBlockPos();
            Vector3dc center = be.getCenter(new Vector3d());
            Vector3d otherCenter = other.getCenter(new Vector3d());
            BlockState state = be.getBlockState();
            Direction facing = (Direction)state.getValue(SpringBlock.FACING);
            Direction otherFacing = (Direction)other.getBlockState().getValue(SpringBlock.FACING);
            Vector3dc normalA = JOMLConversion.atLowerCornerOf(facing.getNormal());
            Vector3d normalB = JOMLConversion.atLowerCornerOf(otherFacing.getNormal());
            VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutout(Simulated.path("textures/block/merging_glue/strand.png")));
            Pose3dc renderPose = subLevel != null ? ((ClientSubLevel)subLevel).renderPose() : null;
            Pose3dc otherRenderPose = otherSubLevel != null ? ((ClientSubLevel)otherSubLevel).renderPose() : null;
            boolean horizontal = facing.getAxis().isHorizontal();
            Vector3dc rightA = (Vector3dc)(horizontal
               ? JOMLConversion.atLowerCornerOf(facing.getClockWise().getNormal(), new Vector3d())
               : OrientedBoundingBox3d.FORWARD);
            Vector3d rightB = horizontal
               ? JOMLConversion.atLowerCornerOf(otherFacing.getCounterClockWise().getNormal(), new Vector3d())
               : new Vector3d(OrientedBoundingBox3d.FORWARD);
            Vector3dc upA = (Vector3dc)(horizontal ? new Vector3d(0.0, 1.0, 0.0) : OrientedBoundingBox3d.RIGHT);
            Vector3d upB = horizontal ? new Vector3d(0.0, 1.0, 0.0) : new Vector3d(OrientedBoundingBox3d.RIGHT);
            if (otherRenderPose != null) {
               otherRenderPose.transformNormal(normalB);
               otherRenderPose.transformNormal(rightB);
               otherRenderPose.transformNormal(upB);
               otherRenderPose.transformPosition(otherCenter);
            }

            if (renderPose != null) {
               renderPose.transformNormalInverse(normalB);
               renderPose.transformNormalInverse(rightB);
               renderPose.transformNormalInverse(upB);
               renderPose.transformPositionInverse(otherCenter);
            }

            Vector3d strandCenterA = center.sub(JOMLConversion.atLowerCornerOf(blockPos), new Vector3d());
            Vector3d strandCenterB = otherCenter.sub(JOMLConversion.atLowerCornerOf(blockPos), new Vector3d());
            Vector3d strandPosA = new Vector3d();
            Vector3d strandPosB = new Vector3d();
            Vector2d[] strandPositions = new Vector2d[]{new Vector2d(0.25, 0.25), new Vector2d(0.45, 0.3), new Vector2d(0.6, 0.6), new Vector2d(0.65, 0.7)};

            for (int i = 0; i < 2; i++) {
               Vector2d strandA = strandPositions[i * 2].sub(0.5, 0.5, new Vector2d()).mul(0.75);
               Vector2d strandB = strandPositions[i * 2 + 1].sub(0.5, 0.5, new Vector2d()).mul(0.75);
               renderGlueCross(
                  strandPosA.set(strandCenterA).fma(strandA.x, rightA).fma(strandA.y, upA),
                  upA,
                  rightA,
                  strandPosB.set(strandCenterB).fma(strandB.x, rightB).fma(strandB.y, upB),
                  upB,
                  rightB,
                  buffer,
                  ms,
                  light
               );
            }
         }
      }
   }

   private static VertexConsumer addVertex(VertexConsumer buffer, Matrix4f pose, Vector3dc pos) {
      return buffer.addVertex(pose, (float)pos.x(), (float)pos.y(), (float)pos.z());
   }

   private static void renderGlueCross(
      Vector3dc posA, Vector3dc upA, Vector3dc rightA, Vector3dc posB, Vector3dc upB, Vector3dc rightB, VertexConsumer buffer, PoseStack ms, int light
   ) {
      Matrix4f pose = ms.last().pose();
      Vector3d vertex = new Vector3d();
      addVertex(buffer, pose, posA.fma(-0.5, upA, vertex))
         .setColor(-1)
         .setUv(0.0F, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posA.fma(0.5, upA, vertex))
         .setColor(-1)
         .setUv(0.0F, 1.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posB.fma(0.5, upB, vertex))
         .setColor(-1)
         .setUv(1.0F, 1.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posB.fma(-0.5, upB, vertex))
         .setColor(-1)
         .setUv(1.0F, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posB.fma(-0.5, upB, vertex))
         .setColor(-1)
         .setUv(1.0F, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posB.fma(0.5, upB, vertex))
         .setColor(-1)
         .setUv(1.0F, 1.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posA.fma(0.5, upA, vertex))
         .setColor(-1)
         .setUv(0.0F, 1.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posA.fma(-0.5, upA, vertex))
         .setColor(-1)
         .setUv(0.0F, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posA.fma(-0.5, rightA, vertex))
         .setColor(-1)
         .setUv(0.0F, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posA.fma(0.5, rightA, vertex))
         .setColor(-1)
         .setUv(0.0F, 1.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posB.fma(0.5, rightB, vertex))
         .setColor(-1)
         .setUv(1.0F, 1.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posB.fma(-0.5, rightB, vertex))
         .setColor(-1)
         .setUv(1.0F, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posB.fma(-0.5, rightB, vertex))
         .setColor(-1)
         .setUv(1.0F, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posB.fma(0.5, rightB, vertex))
         .setColor(-1)
         .setUv(1.0F, 1.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posA.fma(0.5, rightA, vertex))
         .setColor(-1)
         .setUv(0.0F, 1.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
      addVertex(buffer, pose, posA.fma(-0.5, rightA, vertex))
         .setColor(-1)
         .setUv(0.0F, 0.0F)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(ms.last(), 0.0F, 1.0F, 0.0F);
   }
}
