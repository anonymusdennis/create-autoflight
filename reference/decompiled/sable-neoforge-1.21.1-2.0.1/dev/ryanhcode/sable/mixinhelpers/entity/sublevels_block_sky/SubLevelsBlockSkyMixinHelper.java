package dev.ryanhcode.sable.mixinhelpers.entity.sublevels_block_sky;

import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SubLevelsBlockSkyMixinHelper {
   @Internal
   public static boolean checkSkyWithSublevels(Level level, BlockPos pos) {
      Vec3 start = Vec3.atBottomCenterOf(pos);
      ClipContext context = new ClipContext(
         start, new Vec3(start.x, (double)level.getMaxBuildHeight(), start.z), Block.COLLIDER, Fluid.ANY, CollisionContext.empty()
      );
      ((ClipContextExtension)context).sable$setIgnoreMainLevel(true);
      return level.clip(context).getType() != Type.MISS;
   }
}
