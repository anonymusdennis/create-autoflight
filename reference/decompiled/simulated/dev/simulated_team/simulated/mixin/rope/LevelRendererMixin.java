package dev.simulated_team.simulated.mixin.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ZiplineClientManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
   @Inject(
      method = {"renderHitOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$cancelBlockHitOutline(
      PoseStack poseStack, VertexConsumer consumer, Entity entity, double camX, double camY, double camZ, BlockPos pos, BlockState state, CallbackInfo ci
   ) {
      if (ZiplineClientManager.hoveringRope != null) {
         ci.cancel();
      }
   }
}
