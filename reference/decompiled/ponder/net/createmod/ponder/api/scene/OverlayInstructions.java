package net.createmod.ponder.api.scene;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public interface OverlayInstructions {
   TextElementBuilder showText(int var1);

   TextElementBuilder showOutlineWithText(Selection var1, int var2);

   InputElementBuilder showControls(Vec3 var1, Pointing var2, int var3);

   void chaseBoundingBoxOutline(PonderPalette var1, Object var2, AABB var3, int var4);

   void showCenteredScrollInput(BlockPos var1, Direction var2, int var3);

   void showScrollInput(Vec3 var1, Direction var2, int var3);

   void showRepeaterScrollInput(BlockPos var1, int var2);

   void showFilterSlotInput(Vec3 var1, int var2);

   void showFilterSlotInput(Vec3 var1, Direction var2, int var3);

   void showLine(PonderPalette var1, Vec3 var2, Vec3 var3, int var4);

   void showBigLine(PonderPalette var1, Vec3 var2, Vec3 var3, int var4);

   void showOutline(PonderPalette var1, Object var2, Selection var3, int var4);
}
