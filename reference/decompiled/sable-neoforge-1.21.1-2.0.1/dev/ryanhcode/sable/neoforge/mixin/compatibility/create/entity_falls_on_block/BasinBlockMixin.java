package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.entity_falls_on_block;

import com.simibubi.create.content.processing.basin.BasinBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({BasinBlock.class})
public class BasinBlockMixin {
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
