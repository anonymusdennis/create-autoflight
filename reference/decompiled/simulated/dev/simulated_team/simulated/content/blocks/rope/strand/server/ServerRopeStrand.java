package dev.simulated_team.simulated.content.blocks.rope.strand.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle.AttachmentPoint;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import foundry.veil.api.util.CodecUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class ServerRopeStrand extends RopePhysicsObject {
   public static final Codec<ServerRopeStrand> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(ServerRopeStrand::getUUID),
               CodecUtil.VECTOR3D_CODEC.listOf().fieldOf("points").forGetter(strand -> List.copyOf(strand.getPoints())),
               RopeAttachment.CODEC.listOf().fieldOf("attachments").forGetter(strand -> List.copyOf(strand.attachments.values())),
               Codec.DOUBLE.fieldOf("extension_goal").forGetter(strand -> strand.extensionGoal)
            )
            .apply(instance, (uuid, points, attachments, extensionGoal) -> {
               ObjectArrayList<Vector3d> newPoints = new ObjectArrayList();

               for (Vector3dc point : points) {
                  newPoints.add(new Vector3d(point));
               }

               ServerRopeStrand strand = new ServerRopeStrand(uuid, newPoints);

               for (RopeAttachment attachment : attachments) {
                  strand.attachments.put(attachment.point(), attachment);
               }

               strand.extensionGoal = extensionGoal;
               strand.lastExtensionGoal = extensionGoal;
               return strand;
            })
   );
   public static final double SEGMENT_LENGTH = 1.0;
   private final UUID uuid;
   private final Map<RopeAttachmentPoint, RopeAttachment> attachments = new Object2ObjectOpenHashMap();
   private final List<Vector3d> lastNetworkedPoints = new ObjectArrayList();
   private int lastPointCount = 0;
   @Nullable
   private PhysicsConstraintHandle constraint = null;
   private double lastExtensionGoal = 1.0;
   private double extensionGoal = 1.0;
   private double lastFirstSegmentExtension = 1.0;
   private boolean attachmentsDirty;
   public boolean networkingStopped;
   private final Set<UUID> trackingPlayers = new ObjectOpenHashSet();

   public ServerRopeStrand(UUID uuid, Collection<Vector3d> points) {
      super(points, 0.125);
      this.uuid = uuid;
   }

   public boolean needsSync() {
      if (this.lastNetworkedPoints.size() != this.points.size()) {
         return true;
      } else {
         double threshold = Mth.square(0.00625);

         for (int i = 0; i < this.points.size(); i++) {
            if (((Vector3d)this.points.get(i)).distanceSquared((Vector3dc)this.lastNetworkedPoints.get(i)) > threshold) {
               return true;
            }
         }

         return false;
      }
   }

   public void justSynced() {
      this.lastNetworkedPoints.clear();
      ObjectListIterator var1 = this.points.iterator();

      while (var1.hasNext()) {
         Vector3d point = (Vector3d)var1.next();
         this.lastNetworkedPoints.add(new Vector3d(point));
      }
   }

   public void updateFirstSegmentExtension(double extensionGoal) {
      this.lastExtensionGoal = this.extensionGoal;
      this.extensionGoal = extensionGoal;
   }

   public Set<UUID> getTrackingPlayers() {
      return this.trackingPlayers;
   }

   public UUID getUUID() {
      return this.uuid;
   }

   public boolean isOwnerLoaded(ServerLevel level) {
      ServerSubLevelContainer container = SubLevelContainer.getContainer(level);

      assert container != null;

      RopeAttachment attachment = this.attachments.get(RopeAttachmentPoint.START);
      UUID subLevelID = attachment.subLevelID();
      if (subLevelID != null && container.getSubLevel(subLevelID) == null) {
         return false;
      } else {
         BlockPos blockPos = attachment.blockAttachment();
         return PhysicsChunkTicketManager.isChunkLoadedEnough(level, blockPos.getX() >> 4, blockPos.getZ() >> 4);
      }
   }

   public boolean areAttachmentsLoaded(ServerLevel level) {
      ServerSubLevelContainer container = SubLevelContainer.getContainer(level);

      assert container != null;

      for (RopeAttachment attachment : this.attachments.values()) {
         UUID subLevelID = attachment.subLevelID();
         if (subLevelID != null) {
            return container.getSubLevel(subLevelID) != null;
         }

         BlockPos blockPos = attachment.blockAttachment();
         if (!PhysicsChunkTicketManager.isChunkLoadedEnough(level, blockPos.getX() >> 4, blockPos.getZ() >> 4)) {
            return false;
         }
      }

      return true;
   }

   public double getCurrentExtension() {
      double totalExtension = 0.0;

      for (int i = 0; i < this.points.size() - 1; i++) {
         Vector3d a = (Vector3d)this.points.get(i);
         Vector3d b = (Vector3d)this.points.get(i + 1);
         totalExtension += a.distance(b);
      }

      return totalExtension;
   }

   public void addAttachment(ServerLevel level, RopeAttachmentPoint point, RopeAttachment ropeAttachment) {
      this.attachments.put(point, ropeAttachment);
      this.removeConstraints();
      if (this.isActive()) {
         this.applyAttachment(ropeAttachment, level);
      }
   }

   public void removeConstraints() {
      if (this.constraint != null) {
         this.constraint.remove();
      }

      this.constraint = null;
   }

   public void reattachConstraints(ServerLevel level) {
      this.removeConstraints();
      ServerSubLevelContainer container = SubLevelContainer.getContainer(level);

      assert container != null;

      RopeAttachment start = this.attachments.get(RopeAttachmentPoint.START);
      RopeAttachment end = this.attachments.get(RopeAttachmentPoint.END);
      if (start != null && end != null) {
         UUID idA = start.subLevelID();
         UUID idB = end.subLevelID();
         ServerSubLevel subLevelA = idA != null ? (ServerSubLevel)container.getSubLevel(idA) : null;
         ServerSubLevel subLevelB = idB != null ? (ServerSubLevel)container.getSubLevel(idB) : null;
         if (subLevelA != subLevelB) {
            SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
            PhysicsPipeline pipeline = physicsSystem.getPipeline();
            FreeConstraintConfiguration config = new FreeConstraintConfiguration(
               JOMLConversion.toJOML(start.blockAttachment().getCenter()), JOMLConversion.toJOML(end.blockAttachment().getCenter()), new Quaterniond()
            );
            this.constraint = pipeline.addConstraint(subLevelA, subLevelB, config);

            for (ConstraintJointAxis angularAxis : ConstraintJointAxis.ANGULAR) {
               this.constraint.setMotor(angularAxis, 0.0, 0.0, 1.3, false, 0.0);
            }

            for (ConstraintJointAxis linearAxis : ConstraintJointAxis.LINEAR) {
               this.constraint.setMotor(linearAxis, 0.0, 0.0, 0.25, false, 0.0);
            }
         }
      }
   }

   public Iterable<RopeAttachment> getAttachments() {
      return this.attachments.values();
   }

   public RopeAttachment getAttachment(RopeAttachmentPoint point) {
      return this.attachments.get(point);
   }

   public void onRemoved() {
      super.onRemoved();
      this.removeConstraints();
   }

   public void onUnloaded(SubLevelHoldingChunkMap holdingChunkMap, ChunkPos chunkPos) {
      super.onUnloaded(holdingChunkMap, chunkPos);
      this.removeConstraints();
   }

   public void onAddition(SubLevelPhysicsSystem physicsSystem) {
      super.onAddition(physicsSystem);
      this.setFirstSegmentLength(this.extensionGoal);
      ServerLevel level = physicsSystem.getLevel();

      for (RopeAttachment attachment : this.attachments.values()) {
         this.applyAttachment(attachment, level);
      }
   }

   private void applyAttachment(RopeAttachment attachment, ServerLevel level) {
      RopeAttachmentPoint point = attachment.point();
      AttachmentPoint sableAttachmentPoint = point == RopeAttachmentPoint.END ? AttachmentPoint.END : AttachmentPoint.START;
      BlockPos blockAttachment = attachment.blockAttachment();
      if (level.getBlockEntity(blockAttachment) instanceof SmartBlockEntity smartBlockEntity) {
         RopeStrandHolderBehavior ropeHolder = (RopeStrandHolderBehavior)smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
         if (ropeHolder != null) {
            Vector3d attachmentPoint = JOMLConversion.toJOML(ropeHolder.getAttachmentPoint());
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            SubLevel subLevel = attachment.subLevelID() != null ? Objects.requireNonNull(container.getSubLevel(attachment.subLevelID())) : null;
            this.setAttachment(sableAttachmentPoint, attachmentPoint, (ServerSubLevel)subLevel);
         }
      }
   }

   public double getExtension() {
      return this.extensionGoal;
   }

   public void prePhysicsTick(SubLevelPhysicsSystem physicsSystem, ServerLevel level, double timeStep) {
      if (this.constraint == null || !this.constraint.isValid()) {
         this.reattachConstraints(physicsSystem.getLevel());
      }

      if (this.points.size() != this.lastPointCount) {
         this.lastExtensionGoal = this.extensionGoal;
         this.lastPointCount = this.points.size();
      }

      double extension = Mth.lerp(physicsSystem.getPartialPhysicsTick(), this.lastExtensionGoal, this.extensionGoal);
      if (!Mth.equal(extension, this.lastFirstSegmentExtension)) {
         this.setFirstSegmentLength(extension);
         this.lastFirstSegmentExtension = extension;
      }

      if (this.attachmentsDirty) {
         for (RopeAttachment attachment : this.attachments.values()) {
            this.applyAttachment(attachment, level);
         }

         this.attachmentsDirty = false;
      }
   }

   public void removeFirstPoint() {
      super.removeFirstPoint();
      this.attachmentsDirty = true;
   }

   public void addPoint(Vector3dc position) {
      super.addPoint(position);
      this.attachmentsDirty = true;
   }
}
