package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.sticker;

import com.simibubi.create.content.contraptions.chassis.StickerBlock;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.StickerBlockEntityExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({StickerBlock.class})
public class StickerBlockMixin implements BlockSubLevelAssemblyListener {
   @Override
   public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
      if (originLevel.getBlockEntity(oldPos) instanceof StickerBlockEntityExtension extension) {
         extension.sable$removeConstraint();
      }

      if (resultingLevel.getBlockEntity(newPos) instanceof StickerBlockEntityExtension extension) {
         extension.sable$removeConstraint();
      }
   }
}
