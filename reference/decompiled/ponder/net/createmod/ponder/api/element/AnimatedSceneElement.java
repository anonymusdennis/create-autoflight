package net.createmod.ponder.api.element;

import net.minecraft.world.phys.Vec3;

public interface AnimatedSceneElement extends PonderSceneElement {
   void forceApplyFade(float var1);

   void setFade(float var1);

   void setFadeVec(Vec3 var1);
}
