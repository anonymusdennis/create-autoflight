package dev.simulated_team.simulated.mixin_interface.ponder;

import net.minecraft.world.phys.Vec3;

public interface PonderSceneExtension {
   float simulated$getBasePlateAnimationTimer(float var1);

   void simulated$toggleRenderBasePlateShadow();

   Vec3 simulated$getShadowOffset(float var1);

   void simulated$setShadowOffset(Vec3 var1);

   void simulated$setOldShadowOffset(Vec3 var1);

   void simulated$moveShadowOffset(Vec3 var1);

   void simulated$setScaleFactor(float var1);

   float simulated$getScale(float var1);

   void simulated$setYOffset(float var1);

   float simulated$getYOffset(float var1);
}
