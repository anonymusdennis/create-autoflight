package net.createmod.ponder.api.element;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface MinecartElement extends AnimatedSceneElement {
   void setPositionOffset(Vec3 var1, boolean var2);

   void setRotation(float var1, boolean var2);

   Vec3 getPositionOffset();

   Vec3 getRotation();

   public interface MinecartConstructor {
      AbstractMinecart create(Level var1, double var2, double var4, double var6);
   }
}
