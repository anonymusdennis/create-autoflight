package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.display_link;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.LinkWithBulbBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({DisplayLinkBlockEntity.class})
public abstract class DisplayLinkBlockEntityMixin extends LinkWithBulbBlockEntity {
   private DisplayLinkBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Inject(
      method = {"getTargetPosition"},
      at = {@At("TAIL")},
      cancellable = true
   )
   public void sable$accountForSubLevels(CallbackInfoReturnable<BlockPos> cir) {
      BlockPos target = (BlockPos)cir.getReturnValue();
      int range = (Integer)AllConfigs.server().logistics.displayLinkRange.get();
      BlockPos pos = this.getBlockPos();
      if (Sable.HELPER
            .distanceSquaredWithSubLevels(
               this.level,
               (double)pos.getX() + 0.5,
               (double)pos.getY() + 0.5,
               (double)pos.getZ() + 0.5,
               (double)target.getX() + 0.5,
               (double)target.getY() + 0.5,
               (double)target.getZ() + 0.5
            )
         >= (double)(range * range)) {
         cir.setReturnValue(BlockPos.ZERO);
      }
   }
}
