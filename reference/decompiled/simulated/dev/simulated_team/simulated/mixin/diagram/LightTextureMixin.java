package dev.simulated_team.simulated.mixin.diagram;

import com.mojang.blaze3d.platform.NativeImage;
import dev.simulated_team.simulated.mixin_interface.diagram.LightTextureExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin({LightTexture.class})
public abstract class LightTextureMixin implements LightTextureExtension {
   @Shadow
   private boolean updateLightTexture;
   @Shadow
   @Final
   private DynamicTexture lightTexture;
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private NativeImage lightPixels;

   @Shadow
   protected static void clampColor(Vector3f color) {
   }

   @Shadow
   protected abstract float notGamma(float var1);

   @Unique
   private static float simulated$getBrightness(int lightLevel) {
      float f = (float)lightLevel / 15.0F;
      return f / (4.0F - 3.0F * f);
   }

   @Override
   public void simulated$makeDiagramLightTexture(float brightnessMultiplier) {
      Vector3f color = new Vector3f();

      for (int x = 0; x < 16; x++) {
         for (int y = 0; y < 16; y++) {
            float brightness = simulated$getBrightness(y) * 0.6F + 0.15F;
            float brightnessG = brightness * ((brightness * 0.6F + 0.4F) * 0.6F + 0.4F);
            float brightnessB = brightness * (brightness * brightness * 0.6F + 0.4F);
            color.set(brightness, brightnessG, brightnessB);
            color.lerp(new Vector3f(0.99F, 1.12F, 1.0F), 0.25F);
            clampColor(color);
            float gamma = 0.55F;
            Vector3f notGamma = new Vector3f(this.notGamma(color.x), this.notGamma(color.y), this.notGamma(color.z));
            color.lerp(notGamma, Math.max(0.0F, 0.55F));
            color.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
            clampColor(color);
            color.mul(255.0F);
            color.mul(brightnessMultiplier);
            int r = (int)color.x();
            int g = (int)color.y();
            int b = (int)color.z();
            this.lightPixels.setPixelRGBA(y, x, 0xFF000000 | b << 16 | g << 8 | r);
         }
      }

      this.updateLightTexture = true;
      this.lightTexture.upload();
   }
}
