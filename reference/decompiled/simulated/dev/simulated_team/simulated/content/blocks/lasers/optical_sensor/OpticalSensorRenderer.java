package dev.simulated_team.simulated.content.blocks.lasers.optical_sensor;

import dev.simulated_team.simulated.content.blocks.lasers.AbstractLaserRenderer;
import dev.simulated_team.simulated.content.blocks.lasers.LaserBehaviour;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector4f;

public class OpticalSensorRenderer extends AbstractLaserRenderer<OpticalSensorBlockEntity> {
   public OpticalSensorRenderer(Context context) {
      super(context);
   }

   public Vector4f getColors(OpticalSensorBlockEntity blockEntity, float partialTicks) {
      Vector4f laserColor = new Vector4f(0.75F, 0.15F, 0.15F, 0.4F * blockEntity.getOpacity());
      if ((Boolean)blockEntity.getBlockState().getValue(OpticalSensorBlock.POWERED)) {
         laserColor.set(0.0F, 0.05F, 0.8F, 0.4F * blockEntity.getOpacity());
      }

      return laserColor;
   }

   @Override
   public float getLaserScale(LaserBehaviour laser) {
      return 0.378F;
   }

   @Override
   public HitResult getRenderedHitResult(LaserBehaviour laser) {
      return laser.getBlockHitResult();
   }
}
