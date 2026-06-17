package dev.engine_room.flywheel.backend;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.engine_room.flywheel.backend.gl.GlTextureUnit;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.UnknownNullability;

public class NoiseTextures {
   public static final ResourceLocation NOISE_TEXTURE = ResourceUtil.rl("textures/flywheel/noise/blue.png");
   @UnknownNullability
   public static DynamicTexture BLUE_NOISE;

   public static void reload(ResourceManager manager) {
      if (BLUE_NOISE != null) {
         BLUE_NOISE.close();
         BLUE_NOISE = null;
      }

      Optional<Resource> optional = manager.getResource(NOISE_TEXTURE);
      if (!optional.isEmpty()) {
         try (InputStream is = optional.get().open()) {
            NativeImage image = NativeImage.read(Format.LUMINANCE, is);
            BLUE_NOISE = new DynamicTexture(image);
            GlTextureUnit.T0.makeActive();
            BLUE_NOISE.bind();
            BLUE_NOISE.setFilter(true, false);
            RenderSystem.texParameter(3553, 10242, 10497);
            RenderSystem.texParameter(3553, 10243, 10497);
            RenderSystem.bindTexture(0);
         } catch (IOException var7) {
         }
      }
   }
}
