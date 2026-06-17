package dev.simulated_team.simulated.ponder.elements.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.ponder.instructions.ModifyRopeInstruction;
import dev.simulated_team.simulated.util.SimMathUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class RopeStrandElement extends AnimatedSceneElementBase implements AnimatedSceneElement {
   public final PonderRopePose pose;
   public final PonderRopePose lastPose;
   public final PonderRopePose startPose;
   public final PonderRopePose scenePose;

   public RopeStrandElement(Vec3 from, Vec3 to, double length, double sog, float floorHeight) {
      this.pose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, (double)floorHeight);
      this.lastPose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, (double)floorHeight);
      this.startPose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, (double)floorHeight);
      this.scenePose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, (double)floorHeight);
   }

   public RopeStrandElement(Vec3 from, Vec3 to, double length, double sog) {
      this.pose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, -Float.MAX_VALUE);
      this.lastPose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, -Float.MAX_VALUE);
      this.startPose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, -Float.MAX_VALUE);
      this.scenePose = new PonderRopePose(JOMLConversion.toJOML(from), JOMLConversion.toJOML(to), length, sog, -Float.MAX_VALUE);
   }

   public void reset(PonderScene scene) {
      super.reset(scene);
      this.pose.set(this.startPose);
      this.lastPose.set(this.startPose);
   }

   public void lerp(Vec3 from, Vec3 to, double length, double sog, double t) {
      this.lastPose.set(this.pose);
      this.pose.lerp(from, to, length, sog, t);
   }

   public void set(PonderRopePose pose) {
      this.lastPose.set(this.pose);
      this.pose.set(pose);
   }

   public ModifyRopeInstruction modify(int duration) {
      return new ModifyRopeInstruction(duration, this);
   }

   protected void renderLast(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float fade, float pt) {
      SuperByteBuffer middle = CachedBuffers.partialFacing(SimPartialModels.ROPE, AllBlocks.ROPE.getDefaultState(), Direction.NORTH);
      SuperByteBuffer knot = CachedBuffers.partialFacing(SimPartialModels.ROPE_KNOT, AllBlocks.ROPE.getDefaultState(), Direction.NORTH);
      VertexConsumer vb = buffer.getBuffer(RenderType.solid());
      PoseStack ps = graphics.pose();
      PonderRopePose currentPose = new PonderRopePose();
      currentPose.set(this.lastPose);
      currentPose.lerp(this.pose, (double)pt);
      List<Vector3d> points = new ObjectArrayList();
      Vector3d currentPos = new Vector3d();
      int knots = (int)Math.ceil(currentPose.length) + 1;
      double extra = currentPose.length - (double)knots;

      for (int i = 0; i < knots; i++) {
         double t = 1.0 - (double)i / ((double)knots + extra);
         Vector3d pos = currentPose.start.lerp(currentPose.end, Math.max(0.0, t), currentPos);
         double y = Math.pow(t - 0.5, 2.0) * 4.0;
         pos.sub(0.0, Mth.clamp(1.0 - y, 0.0, 1.0) * currentPose.sog, 0.0);
         pos.set(pos.x, Math.max(pos.y, currentPose.floorHeight), pos.z);
         points.add(new Vector3d(pos));
      }

      ObjectArrayList<RopeStrandRenderer.RopeRenderPoint> renderPoints = buildRenderPoints(pt, points);
      ps.pushPose();
      this.applyFade(ps, pt);
      ps.translate(currentPose.start.x, currentPose.start.y, currentPose.start.z);

      for (int i = 1; i < renderPoints.size(); i++) {
         RopeStrandRenderer.RopeRenderPoint renderPoint0 = (RopeStrandRenderer.RopeRenderPoint)renderPoints.get(i - 1);
         RopeStrandRenderer.RopeRenderPoint renderPoint1 = (RopeStrandRenderer.RopeRenderPoint)renderPoints.get(i);
         Vector3d globalRenderPos = new Vector3d(renderPoint0.position());
         Vector3d renderPos = renderPoint0.position();
         Quaternionf orientation = renderPoint0.orientation();
         double length = renderPoint1.position().distance(renderPoint0.position());
         ps.pushPose();
         ps.translate(renderPos.x - currentPose.start.x, renderPos.y - currentPose.start.y, renderPos.z - currentPose.start.z);
         ps.mulPose(orientation);
         ps.translate(-0.5, -0.5, -0.5);
         BlockPos pos = BlockPos.containing(globalRenderPos.x, globalRenderPos.y, globalRenderPos.z);
         int worldLight = 240;
         knot.light(240).renderInto(ps, vb);
         ps.pushPose();
         ps.translate(0.0, 0.5, 0.0);
         ps.scale(1.0F, (float)length, 1.0F);
         middle.light(240).renderInto(ps, vb);
         ps.popPose();
         if (renderPoint1 == renderPoints.getLast()) {
            ps.translate(0.0, length, 0.0);
            knot.light(240).renderInto(ps, vb);
         }

         ps.popPose();
      }

      ps.popPose();
   }

   @NotNull
   private static ObjectArrayList<RopeStrandRenderer.RopeRenderPoint> buildRenderPoints(float partialTick, List<Vector3d> inputPoints) {
      ObjectArrayList<RopeStrandRenderer.RopeRenderPoint> ropeRenderPoints = new ObjectArrayList();
      ObjectArrayList<Vector3d> points = new ObjectArrayList(inputPoints);

      while (points.size() >= 2 && ((Vector3d)points.getFirst()).distanceSquared((Vector3dc)points.get(1)) < 1.0E-6) {
         points.removeFirst();
      }

      if (points.size() <= 1) {
         return new ObjectArrayList();
      } else {
         Vector3dc pointZeroPosition = (Vector3dc)points.get(0);
         Vector3dc pointOnePosition = (Vector3dc)points.get(1);
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

         for (int i = 2; i < points.size(); i++) {
            Vector3d pointA = (Vector3d)points.get(i - 1);
            Vector3d pointB = (Vector3d)points.get(i);
            runningNormal.set(pointB).sub(pointA).normalize();
            if (runningNormal.dot(OrientedBoundingBox3d.UP) < -0.15) {
               runningRotation.set(SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0.0, -1.0, 0.0), runningNormal));
               runningRotation.rotateZ((float) Math.PI);
            } else {
               runningRotation.set(SimMathUtils.getQuaternionfFromVectorRotation(new Vector3d(0.0, 1.0, 0.0), runningNormal));
            }

            ropeRenderPoints.add(new RopeStrandRenderer.RopeRenderPoint(new Quaternionf(runningRotation), pointA));
            normal.set(runningNormal);
         }

         ropeRenderPoints.add(new RopeStrandRenderer.RopeRenderPoint(new Quaternionf(runningRotation), (Vector3d)points.getLast()));
         return ropeRenderPoints;
      }
   }
}
