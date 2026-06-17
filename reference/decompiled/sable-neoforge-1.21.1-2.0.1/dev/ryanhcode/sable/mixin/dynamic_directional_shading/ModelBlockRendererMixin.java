package dev.ryanhcode.sable.mixin.dynamic_directional_shading;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.mixinterface.dynamic_directional_shading.ModelBlockRendererCacheExtension;
import dev.ryanhcode.sable.render.dynamic_shade.SableDynamicDirectionalShading;
import dev.ryanhcode.sable.render.dynamic_shade.SubLevelVertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer.Cache;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({ModelBlockRenderer.class})
public class ModelBlockRendererMixin {
   @Shadow
   @Final
   public static ThreadLocal<Cache> CACHE;

   @ModifyVariable(
      method = {"putQuadData"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private VertexConsumer sable$modifyConsumer(VertexConsumer value) {
      return (VertexConsumer)(SableDynamicDirectionalShading.isEnabled() && ((ModelBlockRendererCacheExtension)CACHE.get()).sable$getOnSubLevel()
         ? new SubLevelVertexConsumer(value)
         : value);
   }

   @WrapOperation(
      method = {"renderModelFaceFlat"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/BlockAndTintGetter;getShade(Lnet/minecraft/core/Direction;Z)F"
      )}
   )
   public float getShade(BlockAndTintGetter instance, Direction direction, boolean cull, Operation<Float> original) {
      boolean onSubLevel = SableDynamicDirectionalShading.isEnabled()
         && ((ModelBlockRendererCacheExtension)ModelBlockRenderer.CACHE.get()).sable$getOnSubLevel();
      return onSubLevel ? 1.0F : (Float)original.call(new Object[]{instance, direction, cull});
   }
}
