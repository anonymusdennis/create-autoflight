package dev.simulated_team.simulated.content.blocks.spring;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimMathUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SpringRenderer extends SmartBlockEntityRenderer<SpringBlockEntity> {
   private final Vector3d controlPointA = new Vector3d();
   private final Vector3d controlPointB = new Vector3d();
   private final Vector3d segmentALerp = new Vector3d();
   private final Vector3d segmentBLerp = new Vector3d();
   private final Vector3d segmentCLerp = new Vector3d();
   private final Vector3d startUp = new Vector3d();
   private final Vector3d endUp = new Vector3d();
   private final Vector3d startLeft = new Vector3d();
   private final Vector3d endLeft = new Vector3d();
   private final Vector3d normalizedNormal = new Vector3d();
   private final Vector3d vertex = new Vector3d();

   public SpringRenderer(Context context) {
      super(context);
   }

   private static int getStressColor(SpringBlockEntity be, float partialTicks, Vector3d otherCenter, Vector3dc center, Minecraft minecraft) {
      double distance = otherCenter.distance(center);
      double snapDistance = be.getSnappingDistance();
      double flashingStartExtension = Mth.lerp(0.7, be.getRenderLength(partialTicks) - 0.75, snapDistance);
      float stressAlpha = 0.0F;
      if (distance > flashingStartExtension) {
         double renderTime = (double)((float)minecraft.player.tickCount + partialTicks);
         stressAlpha = Mth.clamp((float)((distance - flashingStartExtension) / (snapDistance - flashingStartExtension)), 0.0F, 1.0F) * 0.3F;
         stressAlpha *= Mth.lerp(0.25F, (float)Math.sin(renderTime / 3.0) * 0.5F + 0.5F, 1.0F);
      }

      return SimColors.STRESSED_RED & 16777215 | (int)(stressAlpha * 255.0F) << 24;
   }

   protected void renderSafe(SpringBlockEntity be, float partialTicks, PoseStack ps, MultiBufferSource bufferSource, int light, int overlay) {
      super.renderSafe(be, partialTicks, ps, bufferSource, light, overlay);
      if (be.isController()) {
         SpringBlockEntity other = be.getPairedSpring();
         if (other != null) {
            BlockState state = be.getBlockState();
            SpringBlock.Size size = (SpringBlock.Size)state.getValue(SpringBlock.SIZE);
            String name = (size == SpringBlock.Size.MEDIUM ? "" : size.getSerializedName() + "_") + "spring";
            VertexConsumer buffer = bufferSource.getBuffer(SimRenderTypes.spring(Simulated.path("textures/block/spring/" + name + ".png")));
            ps.pushPose();
            Minecraft minecraft = Minecraft.getInstance();
            ClientSubLevelContainer container = SubLevelContainer.getContainer(minecraft.level);

            assert container != null;

            UUID otherSubLevelID = be.getPartnerSubLevelID();
            ClientSubLevel otherSubLevel = otherSubLevelID != null ? (ClientSubLevel)container.getSubLevel(otherSubLevelID) : null;
            ClientSubLevel subLevel = Sable.HELPER.getContainingClient(be);
            BlockPos blockPos = be.getBlockPos();
            Vector3dc center = be.getCenter();
            Vector3d otherCenter = other.getCenter();
            Direction facing = (Direction)state.getValue(SpringBlock.FACING);
            Direction otherFacing = (Direction)other.getBlockState().getValue(SpringBlock.FACING);
            Vector3dc normalA = JOMLConversion.atLowerCornerOf(facing.getNormal());
            Vector3d normalB = JOMLConversion.atLowerCornerOf(otherFacing.getNormal());
            ps.translate(center.x() - (double)blockPos.getX(), center.y() - (double)blockPos.getY(), center.z() - (double)blockPos.getZ());
            double PI2 = Math.PI / 2;
            double PI4 = Math.PI / 4;
            Pose3dc renderPose = subLevel != null ? subLevel.renderPose() : null;
            Pose3dc otherRenderPose = otherSubLevel != null ? otherSubLevel.renderPose() : null;
            if (otherRenderPose != null) {
               otherRenderPose.transformNormal(normalB);
               otherRenderPose.transformPosition(otherCenter);
            }

            if (renderPose != null) {
               renderPose.transformNormalInverse(normalB);
               renderPose.transformPositionInverse(otherCenter);
            }

            int color = getStressColor(be, partialTicks, otherCenter, center, minecraft);
            List<SpringRenderer.SplinePoint> splinePoints = this.generateSpline(
               JOMLConversion.ZERO, otherCenter.sub(center, new Vector3d()), normalA, normalB, center.distance(otherCenter) / 5.0 + 0.25
            );
            int totalPoints = splinePoints.size();
            Vector3d pointNormal = new Vector3d();
            Vector3d startUpDir = JOMLConversion.toJOML(this.getUpDirection(be, otherCenter.sub(center, new Vector3d())));
            pointNormal.set(splinePoints.getFirst().normal);
            Matrix3d matrix = new Matrix3d(startUpDir, pointNormal, startUpDir.cross(pointNormal, new Vector3d()));
            double totalSpringLength = 0.0;

            for (int i = 0; i < totalPoints - 1; i++) {
               SpringRenderer.SplinePoint point = splinePoints.get(i);
               SpringRenderer.SplinePoint nextPoint = splinePoints.get(i + 1);
               totalSpringLength += point.point.distance(nextPoint.point);
               matrix.rotateLocal(SimMathUtils.getQuaternionfFromVectorRotation(point.normal, nextPoint.normal));
            }

            Quaterniond orientation = new Quaterniond();
            Quaterniondc orientation1 = renderPose != null ? renderPose.orientation() : JOMLConversion.QUAT_IDENTITY;
            Quaterniondc orientation2 = otherRenderPose != null ? otherRenderPose.orientation() : JOMLConversion.QUAT_IDENTITY;
            Quaterniond blockOrientation1 = new Quaterniond(facing.getRotation());
            Quaterniond blockOrientation2 = new Quaterniond(otherFacing.getRotation());
            blockOrientation2.premul(orientation2).premul(orientation1.conjugate(new Quaterniond()));
            Quaterniond relativeBlockOrientation = new Quaterniond(blockOrientation1).div(blockOrientation2);
            orientation.mul(new Quaterniond(relativeBlockOrientation));
            orientation.mul(matrix.getNormalizedRotation(new Quaterniond()));
            if (Math.abs(OrientedBoundingBox3d.UP.dot(new Vector3d(orientation.x(), orientation.y(), orientation.z()))) < 1.0E-5) {
               orientation.rotateLocalX(Math.PI);
            }

            double d = OrientedBoundingBox3d.UP.dot(new Vector3d(orientation.x(), orientation.y(), orientation.z()));
            double deg = 2.0 * Math.atan2(-d, orientation.w());
            double twist = Math.floor((deg + (Math.PI / 4)) / (Math.PI / 2)) * (Math.PI / 2) - deg;
            float uvScale = (float)((be.getRenderLength(partialTicks) - 0.75) / totalSpringLength);
            double runningSpringLength = 0.0;
            matrix.set(startUpDir, pointNormal, startUpDir.cross(pointNormal, new Vector3d()));

            for (int i = 0; i < totalPoints - 1; i++) {
               SpringRenderer.SplinePoint point = splinePoints.get(i);
               SpringRenderer.SplinePoint nextPoint = splinePoints.get(i + 1);
               Vector3dc upDir = matrix.getColumn(0, new Vector3d());
               matrix.rotateLocal(SimMathUtils.getQuaternionfFromVectorRotation(point.normal, nextPoint.normal));
               matrix.rotateY(-twist / (double)(totalPoints - 1));
               Vector3dc nextUpDir = matrix.getColumn(0, new Vector3d());
               double length = point.point.distance(nextPoint.point);

               float width = switch (size) {
                  case SMALL -> 6.0F;
                  case MEDIUM -> 8.0F;
                  case LARGE -> 10.0F;
               };

               float textureWidth = switch (size) {
                  case SMALL -> 16.0F;
                  case MEDIUM -> 16.0F;
                  case LARGE -> 32.0F;
               };
               this.renderSegment(
                  ps,
                  point.normal,
                  nextPoint.normal,
                  upDir,
                  nextUpDir,
                  point.point,
                  nextPoint.point,
                  false,
                  (float)runningSpringLength * uvScale,
                  (float)(runningSpringLength + length) * uvScale,
                  light,
                  color,
                  buffer,
                  width,
                  textureWidth
               );
               this.renderSegment(
                  ps,
                  point.normal.negate(new Vector3d()),
                  nextPoint.normal.negate(new Vector3d()),
                  upDir.negate(new Vector3d()),
                  nextUpDir.negate(new Vector3d()),
                  point.point,
                  nextPoint.point,
                  true,
                  0.0F - (float)runningSpringLength * uvScale,
                  0.0F - (float)(runningSpringLength + length) * uvScale,
                  light,
                  color,
                  buffer,
                  width,
                  textureWidth
               );
               runningSpringLength += length;
            }

            ps.popPose();
         }
      }
   }

   private Vec3 getUpDirection(SpringBlockEntity be, Vector3dc directionToSpring) {
      Direction facing = (Direction)be.getBlockState().getValue(SpringBlock.FACING);
      Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
      double dot = directionToSpring.dot(normal.x, normal.y, normal.z);
      Vector3d dir = directionToSpring.sub(normal.x * dot, normal.y * dot, normal.z * dot, new Vector3d());
      if (dir.lengthSquared() < 1.0E-6) {
         return facing.getAxis().isHorizontal() ? new Vec3(0.0, 1.0, 0.0) : new Vec3(0.0, 0.0, -1.0);
      } else {
         return Vec3.atLowerCornerOf(Direction.getNearest(dir.x, dir.y, dir.z).getOpposite().getNormal());
      }
   }

   private List<SpringRenderer.SplinePoint> generateSpline(Vector3dc pointA, Vector3dc pointB, Vector3dc normalA, Vector3dc normalB, double controlPointLength) {
      List<SpringRenderer.SplinePoint> list = new ObjectArrayList();
      pointA.fma(controlPointLength, normalA, this.controlPointA);
      pointB.fma(controlPointLength, normalB, this.controlPointB);
      double len = pointA.distance(pointB);
      int initialPointCount = Mth.clamp(Mth.ceil(len), 5, 8);

      for (int i = 0; i <= initialPointCount; i++) {
         double t = (double)i / (double)initialPointCount;
         pointA.lerp(this.controlPointA, t, this.segmentALerp);
         this.controlPointA.lerp(this.controlPointB, t, this.segmentBLerp);
         this.controlPointB.lerp(pointB, t, this.segmentCLerp);
         Vector3d point = new Vector3d(this.segmentALerp.lerp(this.segmentBLerp, t).lerp(this.segmentBLerp.lerp(this.segmentCLerp, t), t));
         Vector3d normal = new Vector3d();
         if (list.isEmpty()) {
            normal.set(normalA);
         } else if (list.size() == initialPointCount) {
            normal.set(normalB).negate();
         } else {
            point.sub(list.get(list.size() - 1).point, normal).normalize();
         }

         list.add(new SpringRenderer.SplinePoint(point, normal));
      }

      return list;
   }

   private void renderSegment(
      PoseStack ms,
      Vector3dc startDirection,
      Vector3dc endDirection,
      Vector3dc inputStartUp,
      Vector3dc inputEndUp,
      Vector3dc startPos,
      Vector3dc endPos,
      boolean second,
      float uvStart,
      float uvEnd,
      int light,
      int color,
      VertexConsumer a,
      float width,
      float textureWidth
   ) {
      inputStartUp.cross(startDirection, this.startLeft).normalize();
      inputEndUp.cross(endDirection, this.endLeft).normalize();
      float texW = width / textureWidth;
      double scale = (double)width / 16.0 / 2.0;
      this.startLeft.mul(scale);
      inputStartUp.mul(scale, this.startUp);
      this.endLeft.mul(scale);
      inputEndUp.mul(scale, this.endUp);
      Vector3d startDown = this.startUp.negate(new Vector3d());
      Vector3d endDown = this.endUp.negate(new Vector3d());
      Vector3d startRight = this.startLeft.negate(new Vector3d());
      Vector3d endRight = this.endLeft.negate(new Vector3d());
      float uvScale = 16.0F / textureWidth;
      float uvXOffset = second ? width / textureWidth : 0.0F;
      this.vert(ms, a, startPos.add(this.startLeft, this.vertex).sub(this.startUp), color, 0.0F + uvXOffset, uvStart * uvScale, startDown, light);
      this.vert(ms, a, endPos.add(this.endLeft, this.vertex).sub(this.endUp), color, 0.0F + uvXOffset, uvEnd * uvScale, endDown, light);
      this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).sub(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, endDown, light);
      this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).sub(this.startUp), color, texW + uvXOffset, uvStart * uvScale, startDown, light);
      this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).add(this.startUp), color, 0.0F + uvXOffset, uvStart * uvScale, this.startUp, light);
      this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).add(this.endUp), color, 0.0F + uvXOffset, uvEnd * uvScale, this.endUp, light);
      this.vert(ms, a, endPos.add(this.endLeft, this.vertex).add(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, this.endUp, light);
      this.vert(ms, a, startPos.add(this.startLeft, this.vertex).add(this.startUp), color, texW + uvXOffset, uvStart * uvScale, this.startUp, light);
      this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).sub(this.startUp), color, 0.0F + uvXOffset, uvStart * uvScale, startRight, light);
      this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).sub(this.endUp), color, 0.0F + uvXOffset, uvEnd * uvScale, endRight, light);
      this.vert(ms, a, endPos.sub(this.endLeft, this.vertex).add(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, endRight, light);
      this.vert(ms, a, startPos.sub(this.startLeft, this.vertex).add(this.startUp), color, texW + uvXOffset, uvStart * uvScale, startRight, light);
      this.vert(ms, a, startPos.add(this.startLeft, this.vertex).add(this.startUp), color, 0.0F + uvXOffset, uvStart * uvScale, this.startLeft, light);
      this.vert(ms, a, endPos.add(this.endLeft, this.vertex).add(this.endUp), color, 0.0F + uvXOffset, uvEnd * uvScale, this.endLeft, light);
      this.vert(ms, a, endPos.add(this.endLeft, this.vertex).sub(this.endUp), color, texW + uvXOffset, uvEnd * uvScale, this.endLeft, light);
      this.vert(ms, a, startPos.add(this.startLeft, this.vertex).sub(this.startUp), color, texW + uvXOffset, uvStart * uvScale, this.startLeft, light);
   }

   private void vert(PoseStack ms, VertexConsumer a, Vector3dc pos, int color, float u1, float v1, Vector3dc normal, int light) {
      normal.normalize(this.normalizedNormal);
      a.addVertex(ms.last().pose(), (float)pos.x(), (float)pos.y(), (float)pos.z())
         .setColor(color)
         .setUv(u1, v1)
         .setLight(light)
         .setNormal(ms.last(), (float)this.normalizedNormal.x(), (float)this.normalizedNormal.y(), (float)this.normalizedNormal.z());
   }

   public boolean shouldRender(SpringBlockEntity blockEntity, Vec3 vec3) {
      return true;
   }

   public boolean shouldRenderOffScreen(SpringBlockEntity blockEntity) {
      return super.shouldRenderOffScreen(blockEntity);
   }

   static record SplinePoint(Vector3dc point, Vector3dc normal) {
   }
}
