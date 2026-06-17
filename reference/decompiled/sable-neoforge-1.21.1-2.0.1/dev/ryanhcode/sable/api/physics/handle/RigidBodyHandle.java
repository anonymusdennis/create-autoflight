package dev.ryanhcode.sable.api.physics.handle;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class RigidBodyHandle {
   private final PhysicsPipelineBody body;
   private final SubLevelPhysicsSystem physicsSystem;

   @Contract("_,_ -> _")
   @Nullable
   public static RigidBodyHandle of(ServerLevel level, PhysicsPipelineBody body) {
      ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container == null) {
         return null;
      } else {
         SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
         return new RigidBodyHandle(body, physicsSystem);
      }
   }

   @Contract("_ -> new")
   @Nullable
   public static RigidBodyHandle of(ServerSubLevel subLevel) {
      ServerLevel level = subLevel.getLevel();
      ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container == null) {
         return null;
      } else {
         SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
         return physicsSystem.getPhysicsHandle(subLevel);
      }
   }

   @Internal
   public RigidBodyHandle(PhysicsPipelineBody body, SubLevelPhysicsSystem physicsSystem) {
      this.body = body;
      this.physicsSystem = physicsSystem;
   }

   public void applyImpulseAtPoint(Vector3dc position, Vector3dc force) {
      this.physicsSystem.getPipeline().applyImpulse(this.body, position, force);
   }

   public void applyImpulseAtPoint(Vec3 position, Vec3 force) {
      this.physicsSystem.getPipeline().applyImpulse(this.body, JOMLConversion.toJOML(position), JOMLConversion.toJOML(force));
   }

   public void applyLinearAndAngularImpulse(Vector3dc impulse, Vector3dc torque) {
      this.applyLinearAndAngularImpulse(impulse, torque, true);
   }

   public void applyLinearAndAngularImpulse(Vector3dc impulse, Vector3dc torque, boolean wakeUp) {
      this.physicsSystem.getPipeline().applyLinearAndAngularImpulse(this.body, impulse, torque, wakeUp);
   }

   public void applyLinearImpulse(Vector3dc impulse) {
      this.applyLinearAndAngularImpulse(impulse, JOMLConversion.ZERO);
   }

   public void applyAngularImpulse(Vector3dc impulse) {
      this.applyLinearAndAngularImpulse(JOMLConversion.ZERO, impulse);
   }

   public void applyTorqueImpulse(Vector3dc torque) {
      this.applyAngularImpulse(torque);
   }

   @Deprecated
   public Vector3dc getLinearVelocity() {
      return this.physicsSystem.getPipeline().getLinearVelocity(this.body, new Vector3d());
   }

   @Deprecated
   public Vector3dc getAngularVelocity() {
      return this.physicsSystem.getPipeline().getAngularVelocity(this.body, new Vector3d());
   }

   public Vector3d getLinearVelocity(Vector3d dest) {
      return this.physicsSystem.getPipeline().getLinearVelocity(this.body, dest);
   }

   public Vector3d getAngularVelocity(Vector3d dest) {
      return this.physicsSystem.getPipeline().getAngularVelocity(this.body, dest);
   }

   public void applyForcesAndReset(ForceTotal forceTotal) {
      forceTotal.applyForces(this);
   }

   public void addLinearAndAngularVelocity(Vector3dc linearVelocity, Vector3dc angularVelocity) {
      this.physicsSystem.getPipeline().addLinearAndAngularVelocity(this.body, linearVelocity, angularVelocity);
   }

   public void teleport(Vector3dc position, Quaterniondc orientation) {
      this.physicsSystem.getPipeline().teleport(this.body, position, orientation);
   }

   public boolean isValid() {
      return !this.body.isRemoved();
   }
}
