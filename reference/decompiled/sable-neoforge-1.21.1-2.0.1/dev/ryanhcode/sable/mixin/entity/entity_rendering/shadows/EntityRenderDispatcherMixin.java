package dev.ryanhcode.sable.mixin.entity.entity_rendering.shadows;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_rendering.shadows.SubLevelEntityShadowRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderDispatcher.class})
public abstract class EntityRenderDispatcherMixin {
   @Inject(
      method = {"renderShadow"},
      at = {@At("TAIL")}
   )
   private static void sable$renderShadowsOnSubLevels(
      PoseStack poseStack,
      MultiBufferSource multiBufferSource,
      Entity entity,
      float f,
      float g,
      LevelReader levelReader,
      float shadowRadius,
      CallbackInfo ci,
      @Local(ordinal = 0) Pose pose,
      @Local(ordinal = 0) VertexConsumer vertexConsumer
   ) {
      SubLevelEntityShadowRenderer.renderEntityShadowOnSubLevels(entity, f, g, shadowRadius, vertexConsumer, pose);
   }
}
