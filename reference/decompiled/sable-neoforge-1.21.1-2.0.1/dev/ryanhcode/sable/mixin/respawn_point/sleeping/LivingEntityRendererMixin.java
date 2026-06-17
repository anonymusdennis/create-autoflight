package dev.ryanhcode.sable.mixin.respawn_point.sleeping;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public class LivingEntityRendererMixin {
   @Inject(
      method = {"render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getBedOrientation()Lnet/minecraft/core/Direction;"
      )}
   )
   private void sable$setupRotations(
      LivingEntity livingEntity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i, CallbackInfo ci
   ) {
      if (livingEntity.getBedOrientation() != null) {
         Optional<BlockPos> sleepingPos = livingEntity.getSleepingPos();
         if (sleepingPos.isPresent()) {
            BlockPos blockPos = sleepingPos.get();
            if (Sable.HELPER.getContaining(livingEntity.level(), blockPos) instanceof ClientSubLevel clientSubLevel) {
               poseStack.mulPose(new Quaternionf(clientSubLevel.renderPose().orientation()));
            }
         }
      }
   }
}
