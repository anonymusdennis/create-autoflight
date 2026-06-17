package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.display_link;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({DisplayLinkBlock.class})
public class DisplayLinkBlockMixin implements BlockSubLevelAssemblyListener {
   @Override
   public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
      if (originLevel.getBlockEntity(oldPos) instanceof DisplayLinkBlockEntity be
         && resultingLevel.getBlockEntity(newPos) instanceof DisplayLinkBlockEntity newBe) {
         newBe.target(be.getTargetPosition());
      }
   }
}
