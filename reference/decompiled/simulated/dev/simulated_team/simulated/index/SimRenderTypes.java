package dev.simulated_team.simulated.index;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.simulated_team.simulated.Simulated;
import foundry.veil.api.client.render.VeilRenderBridge;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

public final class SimRenderTypes extends RenderType {
   private static final RenderType STAFF_OVERLAY = create(
      "simulated:staff_overlay/staff_overlay",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLE_STRIP,
      1536,
      false,
      true,
      CompositeState.builder()
         .setShaderState(VeilRenderBridge.shaderState(Simulated.path("staff_overlay/staff_overlay")))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
         .setCullState(CULL)
         .createCompositeState(false)
   );
   private static final RenderType LASER = create(
      "simulated:laser",
      DefaultVertexFormat.POSITION_TEX_COLOR,
      Mode.QUADS,
      1536,
      false,
      true,
      CompositeState.builder()
         .setShaderState(VeilRenderBridge.shaderState(Simulated.path("laser/laser")))
         .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
         .setCullState(NO_CULL)
         .createCompositeState(false)
   );
   private static final RenderType LENS = create(
      "simulated:laser_pointer_lens",
      DefaultVertexFormat.BLOCK,
      Mode.QUADS,
      1536,
      true,
      true,
      CompositeState.builder()
         .setLightmapState(LIGHTMAP)
         .setShaderState(RENDERTYPE_CUTOUT_SHADER)
         .setTextureState(BLOCK_SHEET_MIPPED)
         .setShaderState(VeilRenderBridge.shaderState(Simulated.path("laser_pointer/lens")))
         .createCompositeState(true)
   );
   private static final VertexFormat SPRING_FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Stress", VertexFormatElement.COLOR)
      .add("UV0", VertexFormatElement.UV0)
      .add("UV2", VertexFormatElement.UV2)
      .add("Normal", VertexFormatElement.NORMAL)
      .padding(1)
      .build();
   private static final RenderType LOCK = create(
      "simulated:lock",
      DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
      Mode.QUADS,
      1536,
      true,
      false,
      CompositeState.builder()
         .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
         .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
         .setCullState(RenderStateShard.NO_CULL)
         .setTextureState(new TextureStateShard(Simulated.path("textures/gui/lock.png"), false, false))
         .createCompositeState(true)
   );
   private static final RenderType ROPE = create(
      "simulated:rope",
      DefaultVertexFormat.BLOCK,
      Mode.QUADS,
      1536,
      true,
      false,
      CompositeState.builder()
         .setShaderState(VeilRenderBridge.shaderState(Simulated.path("rope/rope")))
         .setTextureState(new TextureStateShard(Simulated.path("textures/block/rope_particle.png"), false, false))
         .setLightmapState(LIGHTMAP)
         .setCullState(CULL)
         .createCompositeState(false)
   );
   private static final Function<ResourceLocation, RenderType> SPRING = Util.memoize(
      texture -> {
         CompositeState state = CompositeState.builder()
            .setShaderState(VeilRenderBridge.shaderState(Simulated.path("spring/spring")))
            .setTextureState(new TextureStateShard(texture, false, false))
            .setTransparencyState(NO_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true);
         return create("spring", SPRING_FORMAT, Mode.QUADS, 1536, true, false, state);
      }
   );

   private SimRenderTypes(
      String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
   }

   public static RenderType staffOverlay() {
      return STAFF_OVERLAY;
   }

   public static RenderType laser() {
      return LASER;
   }

   public static RenderType lens() {
      return LENS;
   }

   public static RenderType lock() {
      return LOCK;
   }

   public static RenderType rope() {
      return ROPE;
   }

   public static RenderType itemGlowingSolid(boolean shadersActive) {
      return shadersActive ? Sheets.solidBlockSheet() : RenderTypes.itemGlowingSolid();
   }

   public static RenderType itemGlowingTranslucent(boolean shadersActive) {
      return shadersActive ? Sheets.translucentCullBlockSheet() : RenderTypes.itemGlowingTranslucent();
   }

   public static RenderType spring(ResourceLocation texture) {
      return SPRING.apply(texture);
   }
}
