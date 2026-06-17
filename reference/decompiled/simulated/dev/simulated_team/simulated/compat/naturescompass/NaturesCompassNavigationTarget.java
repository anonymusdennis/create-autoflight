package dev.simulated_team.simulated.compat.naturescompass;

import com.chaosthedude.naturescompass.NaturesCompass;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class NaturesCompassNavigationTarget implements NavigationTarget {
   @Nullable
   @Override
   public Vec3 getTarget(NavTableBlockEntity navBE, ItemStack self) {
      Integer x = (Integer)self.getComponents().get(NaturesCompass.FOUND_X);
      Integer z = (Integer)self.getComponents().get(NaturesCompass.FOUND_Z);
      if (x != null && z != null) {
         Vec3 pos = navBE.getProjectedSelfPos();
         return new Vec3((double)x.intValue(), pos.y(), (double)z.intValue());
      } else {
         return null;
      }
   }
}
