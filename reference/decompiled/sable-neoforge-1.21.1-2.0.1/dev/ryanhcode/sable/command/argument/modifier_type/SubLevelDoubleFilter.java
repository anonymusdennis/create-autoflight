package dev.ryanhcode.sable.command.argument.modifier_type;

import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifierType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SubLevelDoubleFilter implements SubLevelSelectorModifierType.Modifier {
   private final double value;
   private final SubLevelDoubleFilter.DoublePredicate valuePredicate;

   private SubLevelDoubleFilter(double value, SubLevelDoubleFilter.DoublePredicate valuePredicate) {
      this.value = value;
      this.valuePredicate = valuePredicate;
   }

   public static SubLevelDoubleFilter.Factory factory(SubLevelDoubleFilter.DoublePredicate valuePredicate) {
      return new SubLevelDoubleFilter.Factory(valuePredicate);
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
         if (this.valuePredicate.fromSublevel(subLevel, sourcePos, this.value)) {
            filtered.add(subLevel);
         }
      }

      return filtered;
   }

   @FunctionalInterface
   public interface DoublePredicate {
      boolean fromSublevel(ServerSubLevel var1, Vector3dc var2, double var3);
   }

   public static class Factory {
      private final SubLevelDoubleFilter.DoublePredicate doublePredicate;

      public Factory(SubLevelDoubleFilter.DoublePredicate doublePredicate) {
         this.doublePredicate = doublePredicate;
      }

      public SubLevelDoubleFilter create(double value) {
         return new SubLevelDoubleFilter(value, this.doublePredicate);
      }
   }
}
