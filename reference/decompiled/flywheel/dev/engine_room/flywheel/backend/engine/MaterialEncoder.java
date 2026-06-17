package dev.engine_room.flywheel.backend.engine;

import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.backend.MaterialShaderIndices;
import net.minecraft.util.Mth;

public final class MaterialEncoder {
   private static final int BLUR_LENGTH = 1;
   private static final int MIPMAP_LENGTH = 1;
   private static final int BACKFACE_CULLING_LENGTH = 1;
   private static final int POLYGON_OFFSET_LENGTH = 1;
   private static final int DEPTH_TEST_LENGTH = Mth.ceillog2(DepthTest.values().length);
   private static final int TRANSPARENCY_LENGTH = Mth.ceillog2(Transparency.values().length);
   private static final int WRITE_MASK_LENGTH = Mth.ceillog2(WriteMask.values().length);
   private static final int USE_OVERLAY_LENGTH = 1;
   private static final int USE_LIGHT_LENGTH = 1;
   private static final int CARDINAL_LIGHTING_MODE_LENGTH = Mth.ceillog2(CardinalLightingMode.values().length);
   private static final int AMBIENT_OCCLUSION_LENGTH = 1;
   private static final int BLUR_OFFSET = 0;
   private static final int MIPMAP_OFFSET = 1;
   private static final int BACKFACE_CULLING_OFFSET = 2;
   private static final int POLYGON_OFFSET_OFFSET = 3;
   private static final int DEPTH_TEST_OFFSET = 4;
   private static final int TRANSPARENCY_OFFSET = 4 + DEPTH_TEST_LENGTH;
   private static final int WRITE_MASK_OFFSET = TRANSPARENCY_OFFSET + TRANSPARENCY_LENGTH;
   private static final int USE_OVERLAY_OFFSET = WRITE_MASK_OFFSET + WRITE_MASK_LENGTH;
   private static final int USE_LIGHT_OFFSET = USE_OVERLAY_OFFSET + 1;
   private static final int CARDINAL_LIGHTING_MODE_OFFSET = USE_LIGHT_OFFSET + 1;
   private static final int AMBIENT_OCCLUSION_OFFSET = CARDINAL_LIGHTING_MODE_OFFSET + CARDINAL_LIGHTING_MODE_LENGTH;
   private static final int BLUR_MASK = bitMask(1, 0);
   private static final int MIPMAP_MASK = bitMask(1, 1);
   private static final int BACKFACE_CULLING_MASK = bitMask(1, 2);
   private static final int POLYGON_OFFSET_MASK = bitMask(1, 3);
   private static final int DEPTH_TEST_MASK = bitMask(DEPTH_TEST_LENGTH, 4);
   private static final int TRANSPARENCY_MASK = bitMask(TRANSPARENCY_LENGTH, TRANSPARENCY_OFFSET);
   private static final int WRITE_MASK_MASK = bitMask(WRITE_MASK_LENGTH, WRITE_MASK_OFFSET);
   private static final int USE_OVERLAY_MASK = bitMask(1, USE_OVERLAY_OFFSET);
   private static final int USE_LIGHT_MASK = bitMask(1, USE_LIGHT_OFFSET);
   private static final int CARDINAL_LIGHTING_MODE_MASK = bitMask(CARDINAL_LIGHTING_MODE_LENGTH, CARDINAL_LIGHTING_MODE_OFFSET);
   private static final int AMBIENT_OCCLUSION_MASK = bitMask(1, AMBIENT_OCCLUSION_OFFSET);

   private MaterialEncoder() {
   }

   private static int bitMask(int bitLength, int bitOffset) {
      return (1 << bitLength) - 1 << bitOffset;
   }

   public static int packUberShader(Material material) {
      int fog = MaterialShaderIndices.fogIndex(material.fog());
      int cutout = MaterialShaderIndices.cutoutIndex(material.cutout());
      return cutout & 65535 | (fog & 65535) << 16;
   }

   public static int packProperties(Material material) {
      int bits = 0;
      if (material.blur()) {
         bits |= BLUR_MASK;
      }

      if (material.mipmap()) {
         bits |= MIPMAP_MASK;
      }

      if (material.backfaceCulling()) {
         bits |= BACKFACE_CULLING_MASK;
      }

      if (material.polygonOffset()) {
         bits |= POLYGON_OFFSET_MASK;
      }

      bits |= material.depthTest().ordinal() << 4 & DEPTH_TEST_MASK;
      bits |= material.transparency().ordinal() << TRANSPARENCY_OFFSET & TRANSPARENCY_MASK;
      bits |= material.writeMask().ordinal() << WRITE_MASK_OFFSET & WRITE_MASK_MASK;
      if (material.useOverlay()) {
         bits |= USE_OVERLAY_MASK;
      }

      if (material.useLight()) {
         bits |= USE_LIGHT_MASK;
      }

      bits |= material.cardinalLightingMode().ordinal() << CARDINAL_LIGHTING_MODE_OFFSET & CARDINAL_LIGHTING_MODE_MASK;
      if (material.ambientOcclusion()) {
         bits |= AMBIENT_OCCLUSION_MASK;
      }

      return bits;
   }
}
