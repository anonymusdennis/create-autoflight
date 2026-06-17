package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.entity_falls_on_block;

import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SeatBlock.class})
public class SeatBlockMixin {
   @Redirect(
      method = {"updateEntityAfterFallOn"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"
      )
   )
   private BlockPos sable$updateEntityAfterFallOn(Entity instance) {
      return instance.getOnPos();
   }
}
