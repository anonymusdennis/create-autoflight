package dev.simulated_team.simulated.content.blocks.rope.strand.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimMathUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Objects;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class RopeStrandRenderer {
   public static void render(SmartBlockEntity be, RopeStrandHolderBehavior ropeHolder, float partialTick, PoseStack ps, MultiBufferSource buffer) {
      Level level = be.getLevel();

      assert level != null;

      BlockPos ownerPos = be.getBlockPos();
      SuperByteBuffer middle = CachedBuffers.partialFacing(SimPartialModels.ROPE, AllBlocks.ROPE.getDefaultState(), Direction.NORTH);
      SuperByteBuffer knot = CachedBuffers.partialFacing(SimPartialModels.ROPE_KNOT, AllBlocks.ROPE.getDefaultState(), Direction.NORTH);
      VertexConsumer vb = buffer.getBuffer(RenderType.solid());
      SubLevel subLevel = Sable.HELPER.getContaining(be);
      Pose3dc containingPose = null;
      if (subLevel instanceof ClientSubLevel clientSubLevel) {
         containingPose = clientSubLevel.renderPose();
      }

      ClientRopeStrand rope = ropeHolder.getClientStrand();
      float rad = 0.1875F;
      if (ropeHolder.ownsRope() && rope != null && ropeHolder.getClientStrand() != null) {
         ClientRopeStrand clientStrand = ropeHolder.getClientStrand();
         List<ClientRopePoint> points = clientStrand.getPoints();
         if (points.size() <= 1) {
            return;
         }

         ObjectArrayList<RopeStrandRenderer.RopeRenderPoint> ropeRenderPoints = buildRenderPoints(partialTick, points);
         if (ropeRenderPoints.isEmpty()) {
            return;
         }

         ps.pushPose();

         for (int i = 1; i < ropeRenderPoints.size(); i++) {
            RopeStrandRenderer.RopeRenderPoint renderPoint0 = (RopeStrandRenderer.RopeRenderPoint)ropeRenderPoints.get(i - 1);
            RopeStrandRenderer.RopeRenderPoint renderPoint1 = (RopeStrandRenderer.RopeRenderPoint)ropeRenderPoints.get(i);
            Vector3d globalRenderPos = new Vector3d(renderPoint0.position());
            Vector3d renderPos = renderPoint0.position();
            Quaternionf orientation = renderPoint0.orientation();
            double length = renderPoint1.position().distance(renderPoint0.position());
            if (containingPose != null) {
               containingPose.transformPositionInverse(renderPos);
               orientation.premul(new Quaternionf(containingPose.orientation()).conjugate());
            }

            ps.pushPose();
            ps.translate(renderPos.x - (double)ownerPos.getX(), renderPos.y - (double)ownerPos.getY(), renderPos.z - (double)ownerPos.getZ());
            ps.mulPose(orientation);
            ps.translate(-0.5, -0.5, -0.5);
            BlockPos pos = BlockPos.containing(globalRenderPos.x, globalRenderPos.y, globalRenderPos.z);
            int worldLight = LevelRenderer.getLightColor(level, pos);
            if (i > 1) {
               knot.light(worldLight).renderInto(ps, vb);
            }

            ps.translate(0.0, 0.5, 0.0);
            ps.scale(1.0F, (float)length, 1.0F);
            middle.light(worldLight).renderInto(ps, vb);
            ps.popPose();
         }

         ps.popPose();
         RopeStrandRenderer.RopeRenderPoint last = (RopeStrandRenderer.RopeRenderPoint)ropeRenderPoints.getLast();
         if (containingPose != null) {
            Vector3d renderPosx = last.position();
            Quaternionf orientationx = last.orientation();
            containingPose.transformPositionInverse(renderPosx);
            orientationx.premul(new Quaternionf(containingPose.orientation()).conjugate());
         }

         if (Objects.equals(ZiplineClientManager.hoveringRope, clientStrand.getUuid())) {
            renderOutline(ps, buffer, 0.1875F, ropeRenderPoints, ownerPos);
         }
      }
   }

   private static void renderOutline(
      PoseStack ps, MultiBufferSource buffer, float rad, ObjectArrayList<RopeStrandRenderer.RopeRenderPoint> ropeRenderPoints, BlockPos ownerPos
   ) {
      Vector3d previousCorner = new Vector3d();
      Vector3d currentCorner = new Vector3d();
      Vector3d cornerDiff = new Vector3d();
      Vector3d[] ropeCorners = new Vector3d[]{
         new Vector3d((double)(-rad), 0.0, (double)(-rad)),
         new Vector3d((double)(-rad), 0.0, (double)rad),
         new Vector3d((double)rad, 0.0, (double)rad),
         new Vector3d((double)rad, 0.0, (double)(-rad))
      };
      VertexConsumer linesVB = buffer.getBuffer(RenderType.lines());
      Matrix4f pose = ps.last().pose();

      for (int i = 0; i < ropeRenderPoints.size() + 1; i++) {
         RopeStrandRenderer.RopeRenderPoint renderPoint0 = (RopeStrandRenderer.RopeRenderPoint)ropeRenderPoints.get(Math.max(0, i - 1));
         RopeStrandRenderer.RopeRenderPoint renderPoint1 = (RopeStrandRenderer.RopeRenderPoint)ropeRenderPoints.get(Math.min(ropeRenderPoints.size() - 1, i));
         boolean start = i == 0;
         boolean end = i == ropeRenderPoints.size();

         for (Vector3d ropeCorner : ropeCorners) {
            renderPoint0.orientation()
               .transform(start ? ropeCorner.rotateY(Math.PI / 2, previousCorner) : ropeCorner, previousCorner)
               .add(renderPoint0.position())
               .sub((double)ownerPos.getX(), (double)ownerPos.getY(), (double)ownerPos.getZ());
            renderPoint1.orientation()
               .transform(end ? ropeCorner.rotateY(Math.PI / 2, ropeCorner) : ropeCorner, currentCorner)
               .add(renderPoint1.position())
               .sub((double)ownerPos.getX(), (double)ownerPos.getY(), (double)ownerPos.getZ());
            currentCorner.sub(previousCorner, cornerDiff).normalize();
            linesVB.addVertex(pose, (float)previousCorner.x, (float)previousCorner.y, (float)previousCorner.z)
               .setColor(0.0F, 0.0F, 0.0F, 0.4F)
               .setNormal(ps.last(), (float)cornerDiff.x, (float)cornerDiff.y, (float)cornerDiff.z);
            linesVB.addVertex(pose, (float)currentCorner.x, (float)currentCorner.y, (float)currentCorner.z)
               .setColor(0.0F, 0.0F, 0.0F, 0.4F)
               .setNormal(ps.last(), (float)cornerDiff.x, (float)cornerDiff.y, (float)cornerDiff.z);
         }
      }
   }

   @NotNull
   private static ObjectArrayList<RopeStrandRenderer.RopeRenderPoint> buildRenderPoints(float partialTick, List<ClientRopePoint> inputPoints) {
      ObjectArrayList<RopeStrandRenderer.RopeRenderPoint> ropeRenderPoints = new ObjectArrayList();
      ObjectArrayList<ClientRopePoint> points = new ObjectArrayList(inputPoints);

      while (points.size() >= 2 && ((ClientRopePoint)points.getFirst()).position().distanceSquared(((ClientRopePoint)points.get(1)).position()) < 0.001) {
         points.removeFirst();
      }

      if (points.size() <= 1) {
         return new ObjectArrayList();
      } else {
         Vector3dc pointZeroPosition = ((ClientRopePoint)points.get(0)).renderPos(partialTick, new Vector3d());
         Vector3dc pointOnePosition = ((ClientRopePoint)points.get(1)).renderPos(partialTick, new Vector3d());
         Vector3d normal = pointOnePosition.sub(pointZeroPosition, new Vector3d()).normalize();
         Quaternionf runningRotation;
         if (normal.dot(OrientedBoundingBox3d.UP) < 0.0) {
            runningRotation = SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0.0, -1.0, 0.0), normal);
            runningRotation.rotateZ((float) Math.PI);
         } else {
            runningRotation = SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0.0, 1.0, 0.0), normal);
         }

         ropeRenderPoints.add(new RopeStrandRenderer.RopeRenderPoint(new Quaternionf(runningRotation), new Vector3d(pointZeroPosition)));
         Vector3d runningNormal = new Vector3d();
         Vector3d bPos = new Vector3d();
         Vector3d aPos = new Vector3d();

         for (int i = 2; i < points.size(); i++) {
            ClientRopePoint pointA = (ClientRopePoint)points.get(i - 1);
            ClientRopePoint pointB = (ClientRopePoint)points.get(i);
            runningNormal.set(pointB.renderPos(partialTick, bPos)).sub(pointA.renderPos(partialTick, aPos)).normalize();
            if (runningNormal.dot(OrientedBoundingBox3d.UP) < -0.15) {
               runningRotation.set(SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0.0, -1.0, 0.0), runningNormal));
               runningRotation.rotateZ((float) Math.PI);
            } else {
               runningRotation.set(SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0.0, 1.0, 0.0), runningNormal));
            }

            ropeRenderPoints.add(new RopeStrandRenderer.RopeRenderPoint(new Quaternionf(runningRotation), pointA.renderPos(partialTick, new Vector3d())));
            normal.set(runningNormal);
         }

         ropeRenderPoints.add(
            new RopeStrandRenderer.RopeRenderPoint(new Quaternionf(runningRotation), ((ClientRopePoint)points.getLast()).renderPos(partialTick, new Vector3d()))
         );
         return ropeRenderPoints;
      }
   }

   public static record RopeRenderPoint(Quaternionf orientation, Vector3d position) {
   }
}
