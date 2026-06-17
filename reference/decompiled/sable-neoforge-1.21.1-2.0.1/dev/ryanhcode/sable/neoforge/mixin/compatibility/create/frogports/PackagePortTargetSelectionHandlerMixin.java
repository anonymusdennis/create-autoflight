package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.frogports;

import com.simibubi.create.content.logistics.packagePort.PackagePortTargetSelectionHandler;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({PackagePortTargetSelectionHandler.class})
public class PackagePortTargetSelectionHandlerMixin {
   @Shadow
   public static boolean isPostbox;

   @Overwrite
   public static String validateDiff(Vec3 nonProjectedTarget, BlockPos placedPos) {
      ActiveSableCompanion helper = Sable.HELPER;
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      Level level = player.level();
      Vector3d target = helper.projectOutOfSubLevel(level, JOMLConversion.toJOML(nonProjectedTarget));
      SubLevel frogSubLevel = helper.getContaining(level, placedPos);
      if (frogSubLevel != null) {
         frogSubLevel.logicalPose().transformPositionInverse(target);
      }

      Vector3d localDiff = target.sub((double)placedPos.getX() + 0.5, (double)placedPos.getY(), (double)placedPos.getZ() + 0.5);
      if (localDiff.y < 0.0 && !isPostbox) {
         return "package_port.cannot_reach_down";
      } else {
         double packagePortRange = (double)((Integer)AllConfigs.server().logistics.packagePortRange.get()).intValue();
         return localDiff.lengthSquared() > packagePortRange * packagePortRange ? "package_port.too_far" : null;
      }
   }
}
