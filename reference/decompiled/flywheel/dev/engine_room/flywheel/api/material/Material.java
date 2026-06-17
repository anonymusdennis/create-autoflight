package dev.engine_room.flywheel.api.material;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface Material {
   MaterialShaders shaders();

   FogShader fog();

   CutoutShader cutout();

   LightShader light();

   ResourceLocation texture();

   boolean blur();

   boolean mipmap();

   boolean backfaceCulling();

   boolean polygonOffset();

   DepthTest depthTest();

   Transparency transparency();

   WriteMask writeMask();

   boolean useOverlay();

   boolean useLight();

   CardinalLightingMode cardinalLightingMode();

   default boolean ambientOcclusion() {
      return true;
   }

   default boolean equals(@Nullable Material other) {
      if (this == other) {
         return true;
      } else {
         return other == null
            ? false
            : this.blur() == other.blur()
               && this.mipmap() == other.mipmap()
               && this.backfaceCulling() == other.backfaceCulling()
               && this.polygonOffset() == other.polygonOffset()
               && this.depthTest() == other.depthTest()
               && this.transparency() == other.transparency()
               && this.writeMask() == other.writeMask()
               && this.useOverlay() == other.useOverlay()
               && this.useLight() == other.useLight()
               && this.cardinalLightingMode() == other.cardinalLightingMode()
               && this.ambientOcclusion() == other.ambientOcclusion()
               && this.shaders().fragmentSource().equals(other.shaders().fragmentSource())
               && this.shaders().vertexSource().equals(other.shaders().vertexSource())
               && this.fog().source().equals(other.fog().source())
               && this.cutout().source().equals(other.cutout().source())
               && this.light().source().equals(other.light().source())
               && this.texture().equals(other.texture());
      }
   }
}
