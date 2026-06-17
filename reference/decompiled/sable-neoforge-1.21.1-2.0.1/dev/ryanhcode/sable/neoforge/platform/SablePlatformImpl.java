package dev.ryanhcode.sable.neoforge.platform;

import dev.ryanhcode.sable.platform.SablePlatform;
import net.createmod.catnip.levelWrappers.WrappedServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SablePlatformImpl implements SablePlatform {
   @Override
   public boolean isWrappedLevel(@Nullable Level level) {
      return FMLLoader.getLoadingModList().getModFileById("create") != null ? level instanceof WrappedServerLevel : false;
   }

   @Override
   public boolean isBlockstateLadder(BlockState state, Level level, BlockPos pos, LivingEntity entity) {
      return CommonHooks.isLivingOnLadder(state, level, pos, entity).isPresent();
   }
}
