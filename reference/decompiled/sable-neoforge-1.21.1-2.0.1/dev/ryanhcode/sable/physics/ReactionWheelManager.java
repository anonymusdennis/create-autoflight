package dev.ryanhcode.sable.physics;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelReactionWheel;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class ReactionWheelManager {
   private static final Vector3d totalLocalAngularMomentum = new Vector3d();
   private static final Vector3d temp = new Vector3d();
   private final ServerSubLevel subLevel;
   private final Vector3d previousAngularMomentum = new Vector3d();
   private final ForceTotal forceTotal = new ForceTotal();

   public ReactionWheelManager(ServerSubLevel subLevel) {
      this.subLevel = subLevel;
   }

   public void physicsTick(RigidBodyHandle handle) {
      if (this.needsTicking()) {
         totalLocalAngularMomentum.zero();

         for (Entry<BlockPos, BlockEntitySubLevelReactionWheel> wheelEntry : this.subLevel.getPlot().getBlockEntityReactionWheelMap()) {
            BlockEntitySubLevelReactionWheel wheel = wheelEntry.getValue();
            BlockPos pos = wheelEntry.getKey();
            this.addWheelMomentumToLocalVector(pos, wheel, totalLocalAngularMomentum);
         }

         this.subLevel.logicalPose().orientation().transform(totalLocalAngularMomentum);
         Vector3d impulse = totalLocalAngularMomentum.sub(this.previousAngularMomentum, temp);
         this.subLevel.logicalPose().orientation().transformInverse(impulse);
         this.forceTotal.applyAngularImpulse(impulse);
         handle.applyForcesAndReset(this.forceTotal);
         this.previousAngularMomentum.set(totalLocalAngularMomentum);
      }
   }

   public boolean needsTicking() {
      return this.previousAngularMomentum.lengthSquared() > 0.0 || !this.subLevel.getPlot().getBlockEntityReactionWheels().isEmpty();
   }

   public void wheelChanged(BlockPos pos, BlockEntitySubLevelReactionWheel wheel, boolean add) {
      this.addWheelMomentumToLocalVector(pos, wheel, totalLocalAngularMomentum.zero());
      this.subLevel.logicalPose().orientation().transform(totalLocalAngularMomentum);
      if (add) {
         this.previousAngularMomentum.add(totalLocalAngularMomentum);
      } else {
         this.previousAngularMomentum.sub(totalLocalAngularMomentum);
      }
   }

   void addWheelMomentumToLocalVector(BlockPos pos, BlockEntitySubLevelReactionWheel wheel, Vector3d v) {
      wheel.sable$getAngularVelocity(temp.zero());
      Vec3 blockInertia = PhysicsBlockPropertyHelper.getInertia(this.subLevel.getLevel(), pos, wheel.getBlockState());
      if (blockInertia == null) {
         temp.mul(0.16666666666666666);
      } else {
         temp.mul(blockInertia.x, blockInertia.y, blockInertia.z);
      }

      v.fma(PhysicsBlockPropertyHelper.getMass(this.subLevel.getLevel(), pos, wheel.getBlockState()), temp);
   }
}
