package dev.ryanhcode.sable.api.physics;

import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.object.box.BoxHandle;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.config.PhysicsConfigData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface PhysicsPipeline {
   void init(Vector3dc var1, double var2);

   void dispose();

   void prePhysicsTicks();

   void physicsTick(double var1);

   void postPhysicsTicks();

   void tick();

   void add(ServerSubLevel var1, Pose3dc var2);

   void remove(ServerSubLevel var1);

   void add(KinematicContraption var1);

   void remove(KinematicContraption var1);

   @OverrideOnly
   Pose3d readPose(ServerSubLevel var1, Pose3d var2);

   @OverrideOnly
   RopeHandle addRope(RopePhysicsObject var1);

   BoxHandle addBox(BoxPhysicsObject var1);

   void handleChunkSectionAddition(LevelChunkSection var1, int var2, int var3, int var4, boolean var5);

   void handleChunkSectionRemoval(int var1, int var2, int var3);

   void handleBlockChange(SectionPos var1, LevelChunkSection var2, int var3, int var4, int var5, BlockState var6, BlockState var7);

   default void onStatsChanged(@NotNull ServerSubLevel serverSubLevel) {
   }

   void teleport(PhysicsPipelineBody var1, Vector3dc var2, Quaterniondc var3);

   void applyImpulse(PhysicsPipelineBody var1, Vector3dc var2, Vector3dc var3);

   void applyLinearAndAngularImpulse(PhysicsPipelineBody var1, Vector3dc var2, Vector3dc var3, boolean var4);

   default void addLinearAndAngularVelocity(PhysicsPipelineBody body, Vector3dc linearVelocity, Vector3dc angularVelocity) {
   }

   default void resetVelocity(PhysicsPipelineBody body) {
      this.addLinearAndAngularVelocity(body, this.getLinearVelocity(body, new Vector3d()).negate(), this.getAngularVelocity(body, new Vector3d()).negate());
   }

   default Vector3d getLinearVelocity(PhysicsPipelineBody body, Vector3d dest) {
      return dest.zero();
   }

   default Vector3d getAngularVelocity(PhysicsPipelineBody body, Vector3d dest) {
      return dest.zero();
   }

   void wakeUp(PhysicsPipelineBody var1);

   @Nullable
   @Contract("null, null, _ -> fail")
   default <T extends PhysicsConstraintHandle> T addConstraint(
      @Nullable PhysicsPipelineBody bodyA, @Nullable PhysicsPipelineBody bodyB, @NotNull PhysicsConstraintConfiguration<T> configuration
   ) {
      if (bodyA == null && bodyB == null) {
         throw new IllegalArgumentException("Cannot add a constraint between the static world and static world");
      } else if (bodyA == bodyB) {
         throw new IllegalArgumentException("Cannot add a constraint between a body and itself");
      } else {
         return null;
      }
   }

   @OverrideOnly
   default void updateConfigFrom(PhysicsConfigData data) {
   }

   int getNextRuntimeID();
}
