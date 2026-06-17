package net.createmod.ponder.api.element;

import net.createmod.catnip.data.Pair;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public interface WorldSectionElement extends AnimatedSceneElement {
   void mergeOnto(WorldSectionElement var1);

   void set(Selection var1);

   void add(Selection var1);

   void erase(Selection var1);

   void setCenterOfRotation(Vec3 var1);

   void stabilizeRotation(Vec3 var1);

   void selectBlock(BlockPos var1);

   void resetSelectedBlock();

   void queueRedraw();

   boolean isEmpty();

   void setEmpty();

   void setAnimatedRotation(Vec3 var1, boolean var2);

   Vec3 getAnimatedRotation();

   void setAnimatedOffset(Vec3 var1, boolean var2);

   Vec3 getAnimatedOffset();

   Pair<Vec3, BlockHitResult> rayTrace(PonderLevel var1, Vec3 var2, Vec3 var3);
}
