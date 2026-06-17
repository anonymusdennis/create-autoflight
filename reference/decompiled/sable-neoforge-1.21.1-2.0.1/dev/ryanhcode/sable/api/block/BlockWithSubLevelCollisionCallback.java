package dev.ryanhcode.sable.api.block;

import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.callback.FragileBlockCallback;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockWithSubLevelCollisionCallback {
   static BlockSubLevelCollisionCallback sable$getCallback(BlockState state) {
      if (state.getBlock() instanceof BlockWithSubLevelCollisionCallback blockCollisionCallback) {
         return blockCollisionCallback.sable$getCallback();
      } else {
         return ((BlockStateExtension)state)
               .<Boolean>sable$getProperty((PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Boolean>)PhysicsBlockPropertyTypes.FRAGILE.get())
            ? FragileBlockCallback.INSTANCE
            : null;
      }
   }

   static boolean hasCallback(BlockState state) {
      return sable$getCallback(state) != null;
   }

   BlockSubLevelCollisionCallback sable$getCallback();
}
