package dev.eriksonn.aeronautics.neoforge.content.fluids;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tterrag.registrate.builders.FluidBuilder.FluidTypeFactory;
import java.util.function.Supplier;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidType.Properties;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public abstract class AeroFluidType extends FluidType implements IClientFluidTypeExtensions {
   private Vector3f fogColor;
   private Supplier<Float> fogDistance;
   private final ResourceLocation stillTexture;
   private final ResourceLocation flowingTexture;

   public AeroFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
      super(properties);
      this.stillTexture = stillTexture;
      this.flowingTexture = flowingTexture;
   }

   public static FluidTypeFactory create(int fogColor, Supplier<Float> fogDistance, AeroFluidType.Factory factory) {
      return (p, s, f) -> {
         AeroFluidType fluidType = factory.create(p, s, f);
         fluidType.fogColor = new Color(fogColor, false).asVectorF();
         fluidType.fogDistance = fogDistance;
         return fluidType;
      };
   }

   @NotNull
   public ResourceLocation getStillTexture() {
      return this.stillTexture;
   }

   @NotNull
   public ResourceLocation getFlowingTexture() {
      return this.flowingTexture;
   }

   public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
      Vector3f customFogColor = this.getCustomFogColor();
      return customFogColor == null ? fluidFogColor : customFogColor;
   }

   public void modifyFogRender(Camera camera, FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
      super.modifyFogRender(camera, mode, renderDistance, partialTick, nearDistance, farDistance, shape);
      float modifier = this.getFogDistanceModifier();
      float baseWaterFog = 96.0F;
      if (modifier != 1.0F) {
         RenderSystem.setShaderFogShape(FogShape.CYLINDER);
         RenderSystem.setShaderFogStart(-8.0F);
         RenderSystem.setShaderFogEnd(baseWaterFog * modifier);
      }
   }

   public Vector3f getCustomFogColor() {
      return this.fogColor;
   }

   public float getFogDistanceModifier() {
      return this.fogDistance.get();
   }

   public interface Factory {
      AeroFluidType create(Properties var1, ResourceLocation var2, ResourceLocation var3);
   }
}
