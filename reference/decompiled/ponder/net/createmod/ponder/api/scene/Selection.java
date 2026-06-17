package net.createmod.ponder.api.scene;

import java.util.function.Predicate;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface Selection extends Iterable<BlockPos>, Predicate<BlockPos> {
   Selection add(Selection var1);

   Selection substract(Selection var1);

   Selection copy();

   Vec3 getCenter();

   Outline.OutlineParams makeOutline(Outliner var1, Object var2);

   default Outline.OutlineParams makeOutline(Outliner outliner) {
      return this.makeOutline(outliner, this);
   }
}
