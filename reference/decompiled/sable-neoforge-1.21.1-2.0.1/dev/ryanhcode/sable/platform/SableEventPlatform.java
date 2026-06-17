package dev.ryanhcode.sable.platform;

import dev.ryanhcode.sable.api.event.SablePostPhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SablePrePhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SableSubLevelContainerReadyEvent;

public interface SableEventPlatform {
   SableEventPlatform INSTANCE = SablePlatformUtil.load(SableEventPlatform.class);

   void onSubLevelContainerReady(SableSubLevelContainerReadyEvent var1);

   void onPhysicsTick(SablePrePhysicsTickEvent var1);

   void onPostPhysicsTick(SablePostPhysicsTickEvent var1);
}
