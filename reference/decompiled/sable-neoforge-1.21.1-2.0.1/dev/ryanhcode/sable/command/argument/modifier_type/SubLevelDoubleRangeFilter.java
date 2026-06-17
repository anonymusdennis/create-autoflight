package dev.ryanhcode.sable.command.argument.modifier_type;

import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifierType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.advancements.critereon.MinMaxBounds.Doubles;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SubLevelDoubleRangeFilter implements SubLevelSelectorModifierType.Modifier {
   private final Doubles range;
   private final SubLevelDoubleRangeFilter.DoubleGetter valueGetter;
   private final boolean squared;

   private SubLevelDoubleRangeFilter(Doubles range, SubLevelDoubleRangeFilter.DoubleGetter valueGetter, boolean squared) {
      this.range = range;
      this.valueGetter = valueGetter;
      this.squared = squared;
   }

   public static SubLevelDoubleRangeFilter.Factory linear(SubLevelDoubleRangeFilter.DoubleGetter valueGetter) {
      return new SubLevelDoubleRangeFilter.Factory(valueGetter, false);
   }

   public static SubLevelDoubleRangeFilter.Factory squared(SubLevelDoubleRangeFilter.DoubleGetter valueGetter) {
      return new SubLevelDoubleRangeFilter.Factory(valueGetter, true);
   }

   @Override
   public int getMaxResults() {
      return Integer.MAX_VALUE;
   }

   @Nullable
   @Override
   public List<ServerSubLevel> apply(List<ServerSubLevel> selected, Vector3d sourcePos) {
      List<ServerSubLevel> filtered = new ObjectArrayList();

      for (ServerSubLevel subLevel : selected) {
         double value = this.valueGetter.fromSublevel(subLevel, sourcePos);
         if (this.squared) {
            if (this.range.matchesSqr(value)) {
               filtered.add(subLevel);
            }
         } else if (this.range.matches(value)) {
            filtered.add(subLevel);
         }
      }

      return filtered;
   }

   @FunctionalInterface
   public interface DoubleGetter {
      double fromSublevel(ServerSubLevel var1, Vector3dc var2);
   }

   public static class Factory {
      private final SubLevelDoubleRangeFilter.DoubleGetter doubleGetter;
      private final boolean squared;

      public Factory(SubLevelDoubleRangeFilter.DoubleGetter doubleGetter, boolean squared) {
         this.doubleGetter = doubleGetter;
         this.squared = squared;
      }

      public SubLevelDoubleRangeFilter create(Doubles range) {
         return new SubLevelDoubleRangeFilter(range, this.doubleGetter, this.squared);
      }
   }
}
