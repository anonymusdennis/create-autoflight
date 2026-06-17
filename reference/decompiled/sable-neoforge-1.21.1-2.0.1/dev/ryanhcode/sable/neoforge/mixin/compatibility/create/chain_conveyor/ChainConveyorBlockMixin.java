package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.chain_conveyor;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlock;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({ChainConveyorBlock.class})
public class ChainConveyorBlockMixin implements BlockSubLevelAssemblyListener {
   @Override
   public void beforeMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
      if (originLevel.getBlockEntity(oldPos) instanceof ChainConveyorBlockEntity be) {
         be.notifyConnectedToValidate();
      }
   }

   @Override
   public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
      if (resultingLevel.getBlockEntity(newPos) instanceof ChainConveyorBlockEntity be) {
         be.checkInvalid = true;
      }
   }
}
