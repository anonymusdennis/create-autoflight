package dev.simulated_team.simulated.content.blocks.lasers.laser_pointer;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.content.blocks.lasers.AbstractLaserRenderer;
import dev.simulated_team.simulated.content.blocks.lasers.LaserBehaviour;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimRenderTypes;
import dev.simulated_team.simulated.util.SimColors;
import java.awt.Color;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import org.joml.Vector3d;
import org.joml.Vector4f;

public class LaserPointerRenderer extends AbstractLaserRenderer<LaserPointerBlockEntity> {
   public LaserPointerRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(LaserPointerBlockEntity blockEntity, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
      Vector4f colors = this.getColors(blockEntity, partialTicks);
      boolean isDarkerThanDark = colors.x == 0.0F && colors.y == 0.0F && colors.z == 0.0F;
      SuperByteBuffer superBuffer;
      if (blockEntity.shouldCast() && !isDarkerThanDark) {
         superBuffer = CachedBuffers.partial(SimPartialModels.LASER_POINTER_LENS_ON, blockEntity.getBlockState());
      } else {
         superBuffer = CachedBuffers.partial(SimPartialModels.LASER_POINTER_LENS_OFF, blockEntity.getBlockState());
      }

      superBuffer.translate(0.5, 0.5, 0.5);
      superBuffer.rotateToFace((Direction)blockEntity.getBlockState().getValue(LaserPointerBlock.FACING));
      superBuffer.translate(-0.5, -0.5, -0.5);
      if (blockEntity.shouldCast()) {
         superBuffer.light(15728880);
      } else {
         superBuffer.light(light);
      }

      superBuffer.disableDiffuse();
      superBuffer.color((int)(colors.x * 255.0F), (int)(colors.z * 255.0F), (int)(colors.y * 255.0F), 255);
      superBuffer.renderInto(pose, buffer.getBuffer(SimRenderTypes.lens()));
      if (!isDarkerThanDark) {
         super.renderSafe(blockEntity, partialTicks, pose, buffer, light, overlay);
      }
   }

   @Override
   public float getLaserScale(LaserBehaviour laser) {
      return 0.48F;
   }

   public Vector4f getColors(LaserPointerBlockEntity blockEntity, float partialTicks) {
      Color c = new Color(blockEntity.laserColor);
      if (blockEntity.isRainbow()) {
         Vector3d baseLCh = SimColors.LabToLCh(SimColors.toOklab(c));
         float t;
         if (blockEntity.isVirtual()) {
            t = (float)((double)(Util.getMillis() % 5000L * 2L) * Math.PI / 5000.0);
         } else {
            long timeOff = blockEntity.getLevel().getGameTime();
            t = (float)((double)(((float)(timeOff % 100L) + partialTicks) * 2.0F) * Math.PI / 100.0);
         }

         c = SimColors.LChOklab(0.8F, 0.3F, (float)((double)t + baseLCh.z()));
      }

      return new Vector4f((float)c.getRed() / 255.0F, (float)c.getBlue() / 255.0F, (float)c.getGreen() / 255.0F, (float)blockEntity.getPower() / 60.0F);
   }
}
