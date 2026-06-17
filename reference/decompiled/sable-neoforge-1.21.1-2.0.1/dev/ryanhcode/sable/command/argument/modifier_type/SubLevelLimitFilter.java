package dev.ryanhcode.sable.command.argument.modifier_type;

import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifierType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class SubLevelLimitFilter implements SubLevelSelectorModifierType.Modifier {
   private final int limit;

   public SubLevelLimitFilter(int limit) {
      this.limit = limit;
   }

   @Override
   public int getMaxResults() {
      return this.limit;
   }

   @Nullable
   @Override
   public List<ServerSubLevel> apply(List<ServerSubLevel> selected, Vector3d sourcePos) {
      return (List<ServerSubLevel>)(selected.size() > this.limit ? new ObjectArrayList(selected.subList(0, this.limit)) : selected);
   }
}
