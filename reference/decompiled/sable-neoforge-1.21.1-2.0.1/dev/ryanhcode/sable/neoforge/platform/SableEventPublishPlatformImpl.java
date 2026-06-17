package dev.ryanhcode.sable.neoforge.platform;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.neoforge.event.ForgeSableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.platform.SableEventPublishPlatform;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SableEventPublishPlatformImpl implements SableEventPublishPlatform {
   @Override
   public void onSubLevelContainerReady(Level level, SubLevelContainer container) {
      NeoForge.EVENT_BUS.post(new ForgeSableSubLevelContainerReadyEvent(level, container));
   }

   @Override
   public void prePhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
      NeoForge.EVENT_BUS.post(new ForgeSablePrePhysicsTickEvent(physicsSystem, timeStep));
   }

   @Override
   public void postPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
      NeoForge.EVENT_BUS.post(new ForgeSablePostPhysicsTickEvent(physicsSystem, timeStep));
   }
}
