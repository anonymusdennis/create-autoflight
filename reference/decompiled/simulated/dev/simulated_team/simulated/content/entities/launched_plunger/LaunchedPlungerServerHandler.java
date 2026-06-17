package dev.simulated_team.simulated.content.entities.launched_plunger;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class LaunchedPlungerServerHandler {
   private static final WorldAttached<Collection<LaunchedPlungerEntity>> LEVEL_PLUNGERS = new WorldAttached(x -> new ObjectOpenHashSet());

   public static void addLaunchedPlunger(Level level, LaunchedPlungerEntity toAdd) {
      ((Collection)LEVEL_PLUNGERS.get(level)).add(toAdd);
   }

   public static void removeLaunchedPlunger(Level level, LaunchedPlungerEntity toRemove) {
      Collection<LaunchedPlungerEntity> launchedPlungers = (Collection<LaunchedPlungerEntity>)LEVEL_PLUNGERS.get(level);
      launchedPlungers.remove(toRemove);
   }

   public static void removePlayerPlungers(Player player) {
      List<LaunchedPlungerEntity> plungersForRemoval = new ArrayList<>();

      for (LaunchedPlungerEntity launchedPlungerEntity : (Collection)LEVEL_PLUNGERS.get(player.level())) {
         if (launchedPlungerEntity.getOwner() == player) {
            plungersForRemoval.add(launchedPlungerEntity);
         }
      }

      for (LaunchedPlungerEntity launchedPlungerEntityx : plungersForRemoval) {
         launchedPlungerEntityx.discard();
         LaunchedPlungerEntity other = launchedPlungerEntityx.getOther();
         if (other != null) {
            other.discard();
         }
      }
   }

   public static void physicsTickAllPlungers(SubLevelPhysicsSystem physicsSystem, double timeStep) {
      ServerLevel level = physicsSystem.getLevel();

      for (LaunchedPlungerEntity launchedPlunger : (Collection)LEVEL_PLUNGERS.get(level)) {
         ServerSubLevel sublevel = (ServerSubLevel)Sable.HELPER.getContaining(launchedPlunger);
         if (sublevel != null) {
            launchedPlunger.physicsTick(sublevel, physicsSystem.getPhysicsHandle(sublevel), timeStep);
         }
      }
   }
}
