package dev.ryanhcode.sable.mixin.impact;

import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.physics.callback.ExplosiveBlockCallback;
import net.minecraft.world.level.block.TntBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({TntBlock.class})
public abstract class TntBlockMixin implements BlockWithSubLevelCollisionCallback {
   @Override
   public BlockSubLevelCollisionCallback sable$getCallback() {
      return ExplosiveBlockCallback.INSTANCE;
   }
}
