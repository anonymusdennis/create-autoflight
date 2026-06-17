package dev.simulated_team.simulated.content.blocks.lasers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.util.SableDistUtil;
import dev.simulated_team.simulated.index.SimRenderTypes;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public abstract class AbstractLaserRenderer<T extends AbstractLaserBlockEntity> extends SmartBlockEntityRenderer<T> {
   public AbstractLaserRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(T blockEntity, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(blockEntity, partialTicks, pose, buffer, light, overlay);
      LaserBehaviour laser = blockEntity.getAllBehaviours()
         .stream()
         .filter(behaviour -> behaviour instanceof LaserBehaviour)
         .map(behaviour -> (LaserBehaviour)behaviour)
         .findFirst()
         .orElse(null);
      if (laser != null && laser.shouldCast()) {
         Vector4f colors = this.getColors(blockEntity, partialTicks);
         if (colors.w > 0.0F) {
            pose.pushPose();
            this.transformPose(blockEntity, laser, pose);
            float distance = this.getLaserLength(laser);
            this.createLaser(colors, pose, buffer, laser.getRange(), distance);
            pose.popPose();
         }
      }
   }

   public abstract Vector4f getColors(T var1, float var2);

   public float getLaserLength(LaserBehaviour laser) {
      float laserRange = laser.getRange();
      HitResult hr = this.getRenderedHitResult(laser);
      Couple<Vec3> positions = laser.getLaserPositions().get();
      if (hr != null && !hr.getType().equals(Type.MISS)) {
         Vec3 hitPos = hr.getLocation();
         if (laser.getVirtualHitPos() != Vec3.ZERO) {
            hitPos = laser.getVirtualHitPos();
         }

         laserRange = (float)Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(SableDistUtil.getClientLevel(), (Position)positions.getFirst(), hitPos))
            - 0.1F;
      } else if (laser.getVirtualHitPos() != Vec3.ZERO) {
         Vec3 hitPos = laser.getVirtualHitPos();
         laserRange = (float)Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(SableDistUtil.getClientLevel(), (Position)positions.getFirst(), hitPos))
            - 0.1F;
      }

      return laserRange;
   }

   public abstract float getLaserScale(LaserBehaviour var1);

   public HitResult getRenderedHitResult(LaserBehaviour laser) {
      return laser.getClosestHitResult();
   }

   protected void transformPose(T blockEntity, LaserBehaviour laser, PoseStack pose) {
      Direction facing = blockEntity.getDirection();
      pose.translate(0.5, 0.5, 0.5);
      ((PoseTransformStack)TransformStack.of(pose).rotate(facing.getRotation()).rotateXDegrees(-90.0F)).translate(0.0, 0.0, 0.4375);
      float scale = this.getLaserScale(laser);
      pose.scale(scale, scale, 1.0F);
      pose.translate(-0.5, -0.5, 0.0);
   }

   protected void createLaser(Vector4f color, PoseStack pose, MultiBufferSource buffer, float maxLength, float length) {
      VertexConsumer builder;
      if (buffer instanceof SuperRenderTypeBuffer superRenderTypeBuffer) {
         builder = superRenderTypeBuffer.getLateBuffer(SimRenderTypes.laser());
      } else {
         builder = buffer.getBuffer(SimRenderTypes.laser());
      }

      float lengthFrac = length / maxLength;
      float offset = lengthFrac / 10.0F;
      float endU = 1.0F + 1.0F / length;
      float red = color.x();
      float blue = color.y();
      float green = color.z();
      float alpha = color.w();
      float endAlpha = alpha * (1.0F - lengthFrac);
      pose.pushPose();
      Quaternionf rotationQuat = Axis.ZN.rotationDegrees(90.0F);

      for (int i = 0; i < 4; i++) {
         Matrix4f matrix = pose.last().pose();
         builder.addVertex(matrix, 0.0F, 0.0F, 0.0F)
            .setColor(red, green, blue, alpha)
            .setUv(0.0F, endU)
            .setLight(15728880)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setNormal(0.0F, 1.0F, 0.0F);
         builder.addVertex(matrix, 1.0F, 0.0F, 0.0F)
            .setColor(red, green, blue, alpha)
            .setUv(0.0F, endU)
            .setLight(15728880)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setNormal(0.0F, 1.0F, 0.0F);
         builder.addVertex(matrix, 1.0F + offset, -offset, length + 0.5F)
            .setColor(red, green, blue, endAlpha)
            .setUv(endU, endU)
            .setLight(15728880)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setNormal(0.0F, 1.0F, 0.0F);
         builder.addVertex(matrix, -offset, -offset, length + 0.5F)
            .setColor(red, green, blue, endAlpha)
            .setUv(endU, endU)
            .setLight(15728880)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setNormal(0.0F, 1.0F, 0.0F);
         pose.translate(0.5, 0.5, 0.5);
         pose.mulPose(rotationQuat);
         pose.translate(-0.5, -0.5, -0.5);
      }

      pose.popPose();
   }

   public boolean shouldRenderOffScreen(@NotNull T blockEntity) {
      return true;
   }

   public int getViewDistance() {
      return 256;
   }
}
