package dev.engine_room.flywheel.impl.mixin.visualmanage;

import dev.engine_room.flywheel.api.visualization.VisualManager;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
abstract class LevelRendererMixin {
   @Shadow
   private ClientLevel level;

   @Inject(
      method = {"setBlockDirty(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V"},
      at = {@At("TAIL")}
   )
   private void flywheel$checkUpdate(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
      VisualizationManager manager = VisualizationManager.get(this.level);
      if (manager != null) {
         BlockEntity blockEntity = this.level.getBlockEntity(pos);
         if (blockEntity != null) {
            VisualManager<BlockEntity> blockEntities = manager.blockEntities();
            if (oldState != newState) {
               blockEntities.queueRemove(blockEntity);
               blockEntities.queueAdd(blockEntity);
            } else {
               blockEntities.queueUpdate(blockEntity);
            }
         }
      }
   }
}
