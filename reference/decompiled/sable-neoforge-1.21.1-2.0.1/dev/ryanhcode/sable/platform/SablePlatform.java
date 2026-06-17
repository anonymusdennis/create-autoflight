package dev.ryanhcode.sable.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface SablePlatform {
   SablePlatform INSTANCE = SablePlatformUtil.load(SablePlatform.class);

   boolean isWrappedLevel(@Nullable Level var1);

   boolean isBlockstateLadder(BlockState var1, Level var2, BlockPos var3, LivingEntity var4);
}
