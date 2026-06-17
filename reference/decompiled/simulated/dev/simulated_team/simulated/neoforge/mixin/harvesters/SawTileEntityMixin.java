package dev.simulated_team.simulated.neoforge.mixin.harvesters;

import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.content.blocks.auger_shaft.BlockHarvester;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SawBlockEntity.class})
public abstract class SawTileEntityMixin extends SmartBlockEntity {
   @Unique
   SawBlockEntity simulated$self = (SawBlockEntity)this;

   public SawTileEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Inject(
      method = {"dropItemFromCutTree"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void checkDeposit(BlockPos pos, ItemStack stack, CallbackInfo ci) {
      if (((BlockHarvester)this.simulated$self).depositItemStack(this.worldPosition, stack).isEmpty()) {
         ci.cancel();
      }
   }
}
