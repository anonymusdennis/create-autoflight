package dev.ryanhcode.sable.neoforge.platform;

import dev.ryanhcode.sable.api.event.SablePostPhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SablePrePhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SableEventPlatformImpl implements SableEventPlatform {
   @Override
   public void onSubLevelContainerReady(SableSubLevelContainerReadyEvent event) {
      NeoForge.EVENT_BUS.addListener(forgeEvent -> event.onSubLevelContainerReady(forgeEvent.getLevel(), forgeEvent.getContainer()));
   }

   @Override
   public void onPhysicsTick(SablePrePhysicsTickEvent event) {
      NeoForge.EVENT_BUS.addListener(forgeEvent -> event.prePhysicsTick(forgeEvent.getPhysicsSystem(), forgeEvent.getTimeStep()));
   }

   @Override
   public void onPostPhysicsTick(SablePostPhysicsTickEvent event) {
      NeoForge.EVENT_BUS.addListener(forgeEvent -> event.postPhysicsTick(forgeEvent.getPhysicsSystem(), forgeEvent.getTimeStep()));
   }
}
