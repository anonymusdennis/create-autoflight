package dev.simulated_team.simulated.content.end_sea;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public record EndSeaPhysics(ResourceLocation dimension, Optional<Integer> priority, double startY, double depthGradient, double drag) {
   public static final Codec<EndSeaPhysics> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               ResourceLocation.CODEC.fieldOf("dimension").forGetter(EndSeaPhysics::dimension),
               Codec.optionalField("priority", Codec.INT, true).forGetter(EndSeaPhysics::priority),
               Codec.DOUBLE.fieldOf("start_y").forGetter(EndSeaPhysics::startY),
               Codec.DOUBLE.optionalFieldOf("depth_gradient", 1.0).forGetter(EndSeaPhysics::depthGradient),
               Codec.DOUBLE.optionalFieldOf("drag", 1.0).forGetter(EndSeaPhysics::drag)
            )
            .apply(instance, EndSeaPhysics::new)
   );
   public static final StreamCodec<ByteBuf, EndSeaPhysics> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

   public void physicsTick(double substepTimeStep, ServerLevel level) {
      BoundingBox3dc bounds = new BoundingBox3d(-3.0E7, -10000.0, -3.0E7, 3.0E7, 10000.0, 3.0E7);
      Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(level, bounds);
      SubLevelPhysicsSystem physicsSystem = SubLevelContainer.getContainer(level).physicsSystem();
      Vector3d tempLinearVelocity = new Vector3d();
      Vector3d tempAngularVelocity = new Vector3d();

      for (SubLevel subLevel : intersecting) {
         ServerSubLevel serverSubLevel = (ServerSubLevel)subLevel;
         Pose3d pose = subLevel.logicalPose();
         Vector3d pos = pose.position();
         double depth = (this.startY - pos.y) * this.depthGradient;
         if (!(depth < 0.0)) {
            MassData massTracker = serverSubLevel.getMassTracker();
            Vector3dc centerOfMass = massTracker.getCenterOfMass();
            if (centerOfMass != null) {
               RigidBodyHandle handle = physicsSystem.getPhysicsHandle(serverSubLevel);
               Vector3dc gravity = DimensionPhysicsData.getGravity(level, pos);
               QueuedForceGroup dragGroup = serverSubLevel.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.DRAG.get());
               QueuedForceGroup levitationGroup = serverSubLevel.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.LEVITATION.get());
               Vector3d linearDrag = handle.getLinearVelocity(tempLinearVelocity).mul(-substepTimeStep * 3.5 * this.drag);
               Vector3d angularDrag = handle.getAngularVelocity(tempAngularVelocity).mul(-substepTimeStep * 3.0 * this.drag);
               pose.transformNormalInverse(linearDrag).mul(massTracker.getMass());
               massTracker.getInertiaTensor().transform(pose.transformNormalInverse(angularDrag));
               dragGroup.recordPointForce(new Vector3d(centerOfMass), linearDrag);
               dragGroup.getForceTotal().applyLinearAndAngularImpulse(linearDrag, angularDrag);
               Vector3d levitationImpulse = pose.transformNormalInverse(
                  gravity.negate(new Vector3d()).mul(Math.signum(this.depthGradient) * depth * substepTimeStep * massTracker.getMass())
               );
               levitationGroup.applyAndRecordPointForce(centerOfMass, levitationImpulse);

               for (UUID uuid : serverSubLevel.getTrackingPlayers()) {
                  Player player = level.getPlayerByUUID(uuid);
                  if (player != null && Sable.HELPER.getTrackingSubLevel(player) == subLevel) {
                     SimAdvancements.CALL_OF_THE_VOID.awardTo(player);
                  }
               }
            }
         }
      }
   }
}
