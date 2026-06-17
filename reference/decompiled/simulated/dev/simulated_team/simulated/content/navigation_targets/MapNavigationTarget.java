package dev.simulated_team.simulated.content.navigation_targets;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapDecorations.Entry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MapNavigationTarget implements NavigationTarget {
   @Nullable
   @Override
   public Vec3 getTarget(NavTableBlockEntity navBE, ItemStack self) {
      Level level = navBE.getLevel();
      Vec3 pos = navBE.getProjectedSelfPos();
      return getNearestDecorationPos(level, pos, self);
   }

   private static Vec3 getNearestDecorationPos(Level level, Vec3 pos, ItemStack stack) {
      MapDecorations decorations = (MapDecorations)stack.getComponents().get(DataComponents.MAP_DECORATIONS);
      MapId mapId = (MapId)stack.getComponents().get(DataComponents.MAP_ID);
      if (decorations != null && mapId != null) {
         double closestDist = Double.POSITIVE_INFINITY;
         Vec3 closestPos = null;

         for (Entry decoration : decorations.decorations().values()) {
            if (decoration.type().is(SimTags.Misc.NAV_TABLE_FINDABLE)) {
               double dist = pos.distanceToSqr(decoration.x(), pos.y(), decoration.z());
               if (dist < closestDist) {
                  closestPos = new Vec3(decoration.x(), pos.y(), decoration.z());
                  closestDist = dist;
               }
            }
         }

         MapItemSavedData mapData = level.getMapData(mapId);
         if (mapData != null) {
            for (MapBanner banner : mapData.getBanners()) {
               Vec3 bannerPos = banner.pos().getCenter();
               double dist = pos.distanceToSqr(bannerPos.x(), pos.y(), bannerPos.z());
               if (dist < closestDist) {
                  closestPos = bannerPos;
                  closestDist = dist;
               }
            }
         }

         return closestPos;
      } else {
         return null;
      }
   }
}
