package dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4d;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

public class SubLevelEntityShadowRenderer {
   public static final double INFLATION = 1.01;
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final Vector3d CENTER = new Vector3d();
   private static final Vector3d ENTITY_RELATIVE_CENTER = new Vector3d();
   private static final Vector3d NORMAL = new Vector3d();
   private static final Vector3d LOCAL_POS = new Vector3d();
   private static final Vector3d ENTITY_LOCAL_POS = new Vector3d();
   private static final Vector3f RENDER_POSITION = new Vector3f();
   private static final BoundingBox3d BOUNDS = new BoundingBox3d();
   private static final MutableBlockPos TEMP = new MutableBlockPos();
   private static final Vector3d[] CORNERS = new Vector3d[]{new Vector3d(), new Vector3d(), new Vector3d(), new Vector3d()};
   private static final Vector3d[] REVERSE_CORNERS = new Vector3d[]{CORNERS[3], CORNERS[2], CORNERS[1], CORNERS[0]};

   public static void renderEntityShadowOnSubLevels(Entity entity, float f, float partialTick, float shadowRadius, VertexConsumer vertexConsumer, Pose pose) {
      Quaterniondc customOrientation = EntitySubLevelUtil.getCustomEntityOrientation(entity, partialTick);
      Vec3 entityOrigin = entity.getPosition(partialTick);
      Vec3 entityFeet = entityOrigin;
      Vector3dc upDir = OrientedBoundingBox3d.UP;
      Vec3 eyePos = entity.getEyePosition(partialTick);
      if (customOrientation != null) {
         entityFeet = eyePos.subtract(JOMLConversion.toMojang(customOrientation.transform(new Vector3d(0.0, (double)entity.getEyeHeight(), 0.0))));
         upDir = customOrientation.transform(new Vector3d(upDir));
      }

      Level level = entity.level();
      float shadowHeight = Math.min(f / 0.5F, shadowRadius) * 3.0F;
      BoundingBox3d bounds = new BoundingBox3d(
         entityFeet.x - (double)shadowRadius,
         entityFeet.y - (double)shadowHeight,
         entityFeet.z - (double)shadowRadius,
         entityFeet.x + (double)shadowRadius,
         entityFeet.y + 0.2,
         entityFeet.z + (double)shadowRadius
      );
      BoundingBox3d localBounds = new BoundingBox3d();
      if (customOrientation != null) {
         bounds.transform(
            new Matrix4d().translate(entityFeet.x, entityFeet.y, entityFeet.z).rotate(customOrientation).translate(-entityFeet.x, -entityFeet.y, -entityFeet.z),
            bounds
         );
      }

      for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, bounds)) {
         Pose3dc renderPose = ((ClientSubLevel)subLevel).renderPose();
         bounds.transformInverse(renderPose, localBounds);

         for (BlockPos subLevelBlockPos : BlockPos.betweenClosed(
            Mth.floor(localBounds.minX),
            Mth.floor(localBounds.minY),
            Mth.floor(localBounds.minZ),
            Mth.floor(localBounds.maxX),
            Mth.floor(localBounds.maxY),
            Mth.floor(localBounds.maxZ)
         )) {
            BlockState blockState = level.getBlockState(subLevelBlockPos);
            if (blockState.getRenderShape() != RenderShape.INVISIBLE
               && level.getMaxLocalRawBrightness(entity.blockPosition()) > 3
               && blockState.isCollisionShapeFullBlock(level, subLevelBlockPos)) {
               VoxelShape voxelShape = blockState.getShape(level, subLevelBlockPos);
               if (!voxelShape.isEmpty()) {
                  float light = LightTexture.getBrightness(level.dimensionType(), level.getMaxLocalRawBrightness(entity.blockPosition()));
                  BoundingBox3d shapeBounds = BOUNDS.set(voxelShape.bounds())
                     .move((double)subLevelBlockPos.getX(), (double)subLevelBlockPos.getY(), (double)subLevelBlockPos.getZ(), BOUNDS);
                  Vector3d center = shapeBounds.center(CENTER);
                  double centerX = center.x;
                  double centerY = center.y;
                  double centerZ = center.z;
                  renderPose.transformPosition(center);

                  for (Direction direction : DIRECTIONS) {
                     BlockPos offset = TEMP.setWithOffset(subLevelBlockPos, direction);
                     BlockState offsetState = level.getBlockState(offset);
                     if ((offsetState.getRenderShape() == RenderShape.INVISIBLE || !offsetState.isCollisionShapeFullBlock(level, offset))
                        && !(renderPose.transformNormal(JOMLConversion.atLowerCornerOf(direction.getNormal(), NORMAL)).dot(upDir) < 0.6)
                        && !(center.sub(entityFeet.x, entityFeet.y, entityFeet.z, ENTITY_RELATIVE_CENTER).dot(NORMAL) >= 0.0)) {
                        double xHalfExtent = (shapeBounds.maxX - shapeBounds.minX) / 2.0;
                        double zHalfExtent = (shapeBounds.maxZ - shapeBounds.minZ) / 2.0;
                        double yHalfExtent = (shapeBounds.maxY - shapeBounds.minY) / 2.0;
                        if (direction.getAxis() == Axis.Y) {
                           double yStep = (double)direction.getStepY() * 1.01;
                           CORNERS[0].set(centerX - xHalfExtent, centerY + yStep * yHalfExtent, centerZ + zHalfExtent);
                           CORNERS[1].set(centerX + xHalfExtent, centerY + yStep * yHalfExtent, centerZ + zHalfExtent);
                           CORNERS[2].set(centerX + xHalfExtent, centerY + yStep * yHalfExtent, centerZ - zHalfExtent);
                           CORNERS[3].set(centerX - xHalfExtent, centerY + yStep * yHalfExtent, centerZ - zHalfExtent);
                        } else if (direction.getAxis() == Axis.X) {
                           double xStep = (double)direction.getStepX() * 1.01;
                           CORNERS[0].set(centerX + xStep * xHalfExtent, centerY + yHalfExtent, centerZ + zHalfExtent);
                           CORNERS[1].set(centerX + xStep * xHalfExtent, centerY - yHalfExtent, centerZ + zHalfExtent);
                           CORNERS[2].set(centerX + xStep * xHalfExtent, centerY - yHalfExtent, centerZ - zHalfExtent);
                           CORNERS[3].set(centerX + xStep * xHalfExtent, centerY + yHalfExtent, centerZ - zHalfExtent);
                        } else if (direction.getAxis() == Axis.Z) {
                           double zStep = (double)direction.getStepZ() * 1.01;
                           CORNERS[0].set(centerX + xHalfExtent, centerY + yHalfExtent, centerZ + zStep * zHalfExtent);
                           CORNERS[1].set(centerX - xHalfExtent, centerY + yHalfExtent, centerZ + zStep * zHalfExtent);
                           CORNERS[2].set(centerX - xHalfExtent, centerY - yHalfExtent, centerZ + zStep * zHalfExtent);
                           CORNERS[3].set(centerX + xHalfExtent, centerY - yHalfExtent, centerZ + zStep * zHalfExtent);
                        }
                        Vector3dc[] corners = switch (direction.getAxisDirection()) {
                           case POSITIVE -> CORNERS;
                           case NEGATIVE -> REVERSE_CORNERS;
                           default -> throw new MatchException(null, null);
                        };

                        for (Vector3dc corner : corners) {
                           renderPose.transformPosition(corner, LOCAL_POS).sub(entityFeet.x, entityFeet.y, entityFeet.z);
                           Vector3d entityLocalPos = ENTITY_LOCAL_POS.set(LOCAL_POS);
                           if (customOrientation != null) {
                              customOrientation.transformInverse(entityLocalPos);
                           }

                           double yDiff = entityLocalPos.y;
                           int alpha = Mth.floor((float)Math.max(0.0, (double)((f - (float)(-yDiff) * 0.5F) * 0.5F * light)) * 255.0F);
                           LOCAL_POS.add(entityFeet.x - entityOrigin.x, entityFeet.y - entityOrigin.y, entityFeet.z - entityOrigin.z);
                           shadowVertex(
                              pose,
                              vertexConsumer,
                              alpha << 24 | 16777215,
                              (float)LOCAL_POS.x,
                              (float)LOCAL_POS.y,
                              (float)LOCAL_POS.z,
                              (float)((entityLocalPos.x + (double)shadowRadius) / (double)(shadowRadius * 2.0F)),
                              (float)((entityLocalPos.z + (double)shadowRadius) / (double)(shadowRadius * 2.0F))
                           );
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void shadowVertex(Pose pose, VertexConsumer vertexConsumer, int i, float f, float g, float h, float j, float k) {
      Vector3f vector3f = pose.pose().transformPosition(f, g, h, RENDER_POSITION);
      vertexConsumer.addVertex(vector3f.x(), vector3f.y(), vector3f.z(), i, j, k, OverlayTexture.NO_OVERLAY, 15728880, 0.0F, 1.0F, 0.0F);
   }
}
