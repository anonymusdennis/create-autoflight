package dev.ryanhcode.sable.api.block;

import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public interface BlockEntitySubLevelReactionWheel {
   void sable$getAngularVelocity(Vector3d var1);

   BlockState getBlockState();
}
