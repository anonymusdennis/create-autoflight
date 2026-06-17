package dev.ryanhcode.sable.physics.impl.none;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.object.box.BoxHandle;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

public class StaticPhysicsPipeline implements PhysicsPipeline {
   @Override
   public void init(Vector3dc gravity, double universalDrag) {
   }

   @Override
   public void dispose() {
   }

   @Override
   public void prePhysicsTicks() {
   }

   @Override
   public void physicsTick(double timeStep) {
   }

   @Override
   public void postPhysicsTicks() {
   }

   @Override
   public void tick() {
   }

   @Override
   public void add(ServerSubLevel subLevel, Pose3dc pose) {
   }

   @Override
   public void remove(ServerSubLevel subLevel) {
   }

   @Override
   public void add(KinematicContraption contraption) {
   }

   @Override
   public void remove(KinematicContraption contraption) {
   }

   @Override
   public Pose3d readPose(ServerSubLevel subLevel, Pose3d dest) {
      return dest.set(subLevel.logicalPose());
   }

   @Override
   public RopeHandle addRope(RopePhysicsObject rope) {
      return null;
   }

   @Override
   public BoxHandle addBox(BoxPhysicsObject boxPhysicsObject) {
      return null;
   }

   @Override
   public void handleChunkSectionAddition(LevelChunkSection chunk, int x, int y, int z, boolean uploadDataIfGlobal) {
   }

   @Override
   public void handleChunkSectionRemoval(int x, int y, int z) {
   }

   @Override
   public void handleBlockChange(SectionPos sectionPos, LevelChunkSection chunk, int x, int y, int z, BlockState oldState, BlockState newState) {
   }

   @Override
   public void teleport(PhysicsPipelineBody body, Vector3dc position, Quaterniondc orientation) {
      if (body instanceof ServerSubLevel subLevel) {
         subLevel.logicalPose().position().set(position);
         subLevel.logicalPose().orientation().set(orientation);
      }
   }

   @Override
   public void applyImpulse(PhysicsPipelineBody body, Vector3dc position, Vector3dc force) {
   }

   @Override
   public void applyLinearAndAngularImpulse(PhysicsPipelineBody body, Vector3dc position, Vector3dc torque, boolean wakeUp) {
   }

   @Override
   public void wakeUp(PhysicsPipelineBody body) {
   }

   @Override
   public int getNextRuntimeID() {
      return 0;
   }
}
