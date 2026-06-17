package dev.simulated_team.simulated.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.util.SimDebugThing;
import net.minecraft.commands.CommandSourceStack;
import org.joml.Vector3d;

public class SimDebugThingCommands {
   public static int start(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      int steps = (Integer)context.getArgument("steps", Integer.class);
      SimDebugThing.start(steps, ((CommandSourceStack)context.getSource()).getLevel());
      return 1;
   }

   public static int stop(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      SimDebugThing.stop();
      return 1;
   }

   public static int abort(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      SimDebugThing.abort();
      return 1;
   }

   public static int stopSublevels(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      if (((CommandSourceStack)context.getSource()).getLevel() instanceof SubLevelContainerHolder holder
         && holder.sable$getPlotContainer() instanceof ServerSubLevelContainer serverContainer) {
         SubLevelPhysicsSystem physicsSystem = serverContainer.physicsSystem();
         Vector3d angularVelocity = new Vector3d();
         Vector3d linearVelocity = new Vector3d();

         for (SubLevel sublevel : holder.sable$getPlotContainer().getAllSubLevels()) {
            if (sublevel instanceof ServerSubLevel serverSubLevel) {
               RigidBodyHandle rigidBodyHandle = physicsSystem.getPhysicsHandle(serverSubLevel);
               rigidBodyHandle.getAngularVelocity(angularVelocity).negate();
               rigidBodyHandle.getLinearVelocity(linearVelocity).negate();
               serverSubLevel.logicalPose().orientation().transformInverse(angularVelocity);
               serverSubLevel.logicalPose().orientation().transformInverse(linearVelocity);
               ForceTotal forceTotal = new ForceTotal();
               forceTotal.applyLinearImpulse(linearVelocity.mul(serverSubLevel.getMassTracker().getMass()));
               forceTotal.applyTorqueImpulse(serverSubLevel.getMassTracker().getInertiaTensor().transform(angularVelocity));
               rigidBodyHandle.applyForcesAndReset(forceTotal);
            }
         }
      }

      return 1;
   }
}
