package dev.simulated_team.simulated.content.physics_staff;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.server.level.ServerLevel;

public class PhysicsStaffSubLevelObserver implements SubLevelObserver {
   private final ServerLevel level;

   public PhysicsStaffSubLevelObserver(ServerLevel level) {
      this.level = level;
   }

   public void tick(SubLevelContainer subLevels) {
      this.getPhysicsHandler().tick();
   }

   public void onSubLevelAdded(SubLevel subLevel) {
      this.getPhysicsHandler().applyLockIfNeeded(subLevel);
   }

   public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
      if (reason == SubLevelRemovalReason.REMOVED) {
         this.getPhysicsHandler().removeLock(subLevel);
      }
   }

   public PhysicsStaffServerHandler getPhysicsHandler() {
      return PhysicsStaffServerHandler.get(this.level);
   }
}
