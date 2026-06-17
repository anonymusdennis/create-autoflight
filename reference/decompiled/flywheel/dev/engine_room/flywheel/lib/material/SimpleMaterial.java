package dev.engine_room.flywheel.lib.material;

import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.CutoutShader;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.FogShader;
import dev.engine_room.flywheel.api.material.LightShader;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.MaterialShaders;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public class SimpleMaterial implements Material {
   protected final MaterialShaders shaders;
   protected final FogShader fog;
   protected final CutoutShader cutout;
   protected final LightShader light;
   protected final ResourceLocation texture;
   protected final boolean blur;
   protected final boolean mipmap;
   protected final boolean backfaceCulling;
   protected final boolean polygonOffset;
   protected final DepthTest depthTest;
   protected final Transparency transparency;
   protected final WriteMask writeMask;
   protected final boolean useOverlay;
   protected final boolean useLight;
   protected final CardinalLightingMode cardinalLightingMode;
   protected final boolean ambientOcclusion;

   protected SimpleMaterial(SimpleMaterial.Builder builder) {
      this.shaders = builder.shaders();
      this.fog = builder.fog();
      this.cutout = builder.cutout();
      this.light = builder.light();
      this.texture = builder.texture();
      this.blur = builder.blur();
      this.mipmap = builder.mipmap();
      this.backfaceCulling = builder.backfaceCulling();
      this.polygonOffset = builder.polygonOffset();
      this.depthTest = builder.depthTest();
      this.transparency = builder.transparency();
      this.writeMask = builder.writeMask();
      this.useOverlay = builder.useOverlay();
      this.useLight = builder.useLight();
      this.cardinalLightingMode = builder.cardinalLightingMode();
      this.ambientOcclusion = builder.ambientOcclusion();
   }

   public static SimpleMaterial.Builder builder() {
      return new SimpleMaterial.Builder();
   }

   public static SimpleMaterial.Builder builderOf(Material material) {
      return new SimpleMaterial.Builder(material);
   }

   @Override
   public MaterialShaders shaders() {
      return this.shaders;
   }

   @Override
   public FogShader fog() {
      return this.fog;
   }

   @Override
   public CutoutShader cutout() {
      return this.cutout;
   }

   @Override
   public LightShader light() {
      return this.light;
   }

   @Override
   public ResourceLocation texture() {
      return this.texture;
   }

   @Override
   public boolean blur() {
      return this.blur;
   }

   @Override
   public boolean mipmap() {
      return this.mipmap;
   }

   @Override
   public boolean backfaceCulling() {
      return this.backfaceCulling;
   }

   @Override
   public boolean polygonOffset() {
      return this.polygonOffset;
   }

   @Override
   public DepthTest depthTest() {
      return this.depthTest;
   }

   @Override
   public Transparency transparency() {
      return this.transparency;
   }

   @Override
   public WriteMask writeMask() {
      return this.writeMask;
   }

   @Override
   public boolean useOverlay() {
      return this.useOverlay;
   }

   @Override
   public boolean useLight() {
      return this.useLight;
   }

   @Override
   public CardinalLightingMode cardinalLightingMode() {
      return this.cardinalLightingMode;
   }

   @Override
   public boolean ambientOcclusion() {
      return this.ambientOcclusion;
   }

   public static class Builder implements Material {
      protected MaterialShaders shaders;
      protected FogShader fog;
      protected CutoutShader cutout;
      protected LightShader light;
      protected ResourceLocation texture;
      protected boolean blur;
      protected boolean mipmap;
      protected boolean backfaceCulling;
      protected boolean polygonOffset;
      protected DepthTest depthTest;
      protected Transparency transparency;
      protected WriteMask writeMask;
      protected boolean useOverlay;
      protected boolean useLight;
      protected CardinalLightingMode cardinalLightingMode;
      protected boolean ambientOcclusion;

      public Builder() {
         this.shaders = StandardMaterialShaders.DEFAULT;
         this.fog = FogShaders.LINEAR;
         this.cutout = CutoutShaders.OFF;
         this.light = LightShaders.SMOOTH_WHEN_EMBEDDED;
         this.texture = InventoryMenu.BLOCK_ATLAS;
         this.blur = false;
         this.mipmap = true;
         this.backfaceCulling = true;
         this.polygonOffset = false;
         this.depthTest = DepthTest.LEQUAL;
         this.transparency = Transparency.OPAQUE;
         this.writeMask = WriteMask.COLOR_DEPTH;
         this.useOverlay = true;
         this.useLight = true;
         this.cardinalLightingMode = CardinalLightingMode.ENTITY;
         this.ambientOcclusion = true;
      }

      public Builder(Material material) {
         this.copyFrom(material);
      }

      public SimpleMaterial.Builder copyFrom(Material material) {
         this.shaders = material.shaders();
         this.fog = material.fog();
         this.cutout = material.cutout();
         this.light = material.light();
         this.texture = material.texture();
         this.blur = material.blur();
         this.mipmap = material.mipmap();
         this.backfaceCulling = material.backfaceCulling();
         this.polygonOffset = material.polygonOffset();
         this.depthTest = material.depthTest();
         this.transparency = material.transparency();
         this.writeMask = material.writeMask();
         this.useOverlay = material.useOverlay();
         this.useLight = material.useLight();
         this.cardinalLightingMode = material.cardinalLightingMode();
         this.ambientOcclusion = material.ambientOcclusion();
         return this;
      }

      public SimpleMaterial.Builder shaders(MaterialShaders value) {
         this.shaders = value;
         return this;
      }

      public SimpleMaterial.Builder fog(FogShader value) {
         this.fog = value;
         return this;
      }

      public SimpleMaterial.Builder cutout(CutoutShader value) {
         this.cutout = value;
         return this;
      }

      public SimpleMaterial.Builder light(LightShader value) {
         this.light = value;
         return this;
      }

      public SimpleMaterial.Builder texture(ResourceLocation value) {
         this.texture = value;
         return this;
      }

      public SimpleMaterial.Builder blur(boolean value) {
         this.blur = value;
         return this;
      }

      public SimpleMaterial.Builder mipmap(boolean value) {
         this.mipmap = value;
         return this;
      }

      public SimpleMaterial.Builder backfaceCulling(boolean value) {
         this.backfaceCulling = value;
         return this;
      }

      public SimpleMaterial.Builder polygonOffset(boolean value) {
         this.polygonOffset = value;
         return this;
      }

      public SimpleMaterial.Builder depthTest(DepthTest value) {
         this.depthTest = value;
         return this;
      }

      public SimpleMaterial.Builder transparency(Transparency value) {
         this.transparency = value;
         return this;
      }

      public SimpleMaterial.Builder writeMask(WriteMask value) {
         this.writeMask = value;
         return this;
      }

      public SimpleMaterial.Builder useOverlay(boolean value) {
         this.useOverlay = value;
         return this;
      }

      public SimpleMaterial.Builder useLight(boolean value) {
         this.useLight = value;
         return this;
      }

      @Deprecated(
         forRemoval = true
      )
      public SimpleMaterial.Builder diffuse(boolean value) {
         return this.cardinalLightingMode(value ? CardinalLightingMode.ENTITY : CardinalLightingMode.OFF);
      }

      public SimpleMaterial.Builder cardinalLightingMode(CardinalLightingMode value) {
         this.cardinalLightingMode = value;
         return this;
      }

      public SimpleMaterial.Builder ambientOcclusion(boolean ambientOcclusion) {
         this.ambientOcclusion = ambientOcclusion;
         return this;
      }

      @Override
      public MaterialShaders shaders() {
         return this.shaders;
      }

      @Override
      public FogShader fog() {
         return this.fog;
      }

      @Override
      public CutoutShader cutout() {
         return this.cutout;
      }

      @Override
      public LightShader light() {
         return this.light;
      }

      @Override
      public ResourceLocation texture() {
         return this.texture;
      }

      @Override
      public boolean blur() {
         return this.blur;
      }

      @Override
      public boolean mipmap() {
         return this.mipmap;
      }

      @Override
      public boolean backfaceCulling() {
         return this.backfaceCulling;
      }

      @Override
      public boolean polygonOffset() {
         return this.polygonOffset;
      }

      @Override
      public DepthTest depthTest() {
         return this.depthTest;
      }

      @Override
      public Transparency transparency() {
         return this.transparency;
      }

      @Override
      public WriteMask writeMask() {
         return this.writeMask;
      }

      @Override
      public boolean useOverlay() {
         return this.useOverlay;
      }

      @Override
      public boolean useLight() {
         return this.useLight;
      }

      @Override
      public CardinalLightingMode cardinalLightingMode() {
         return this.cardinalLightingMode;
      }

      @Override
      public boolean ambientOcclusion() {
         return this.ambientOcclusion;
      }

      public SimpleMaterial build() {
         return new SimpleMaterial(this);
      }
   }
}
