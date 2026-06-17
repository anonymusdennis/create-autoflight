package net.createmod.ponder.api.element;

import net.minecraft.world.phys.Vec3;

public interface ParrotElement extends AnimatedSceneElement {
   void setPositionOffset(Vec3 var1, boolean var2);

   void setRotation(Vec3 var1, boolean var2);

   Vec3 getPositionOffset();

   Vec3 getRotation();

   void setPose(ParrotPose var1);
}
