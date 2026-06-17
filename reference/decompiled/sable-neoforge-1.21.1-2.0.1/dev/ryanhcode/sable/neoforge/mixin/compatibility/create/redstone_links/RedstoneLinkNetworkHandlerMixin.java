package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.redstone_links;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({RedstoneLinkNetworkHandler.class})
public class RedstoneLinkNetworkHandlerMixin {
   @Redirect(
      method = {"updateNetworkOf"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/redstone/link/RedstoneLinkNetworkHandler;withinRange(Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;Lcom/simibubi/create/content/redstone/link/IRedstoneLinkable;)Z"
      ),
      remap = false
   )
   private boolean sable$projectComparisons(IRedstoneLinkable from, IRedstoneLinkable to, @Local(argsOnly = true) LevelAccessor levelAccessor) {
      Level level = (Level)levelAccessor;
      if (from == to) {
         return true;
      } else {
         Vector3d fromPos = JOMLConversion.atCenterOf(from.getLocation());
         Vector3d toPos = JOMLConversion.atCenterOf(to.getLocation());
         ActiveSableCompanion helper = Sable.HELPER;
         SubLevel fromSublevel = helper.getContaining(level, fromPos);
         if (fromSublevel != null) {
            fromSublevel.logicalPose().transformPosition(fromPos);
         }

         SubLevel toSublevel = helper.getContaining(level, toPos);
         if (toSublevel != null) {
            toSublevel.logicalPose().transformPosition(toPos);
         }

         int linkRange = (Integer)AllConfigs.server().logistics.linkRange.get();
         return fromPos.distanceSquared(toPos) < (double)(linkRange * linkRange);
      }
   }
}
