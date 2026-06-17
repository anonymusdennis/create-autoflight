package dev.ryanhcode.sable.api.physics.object.rope;

import dev.ryanhcode.sable.api.physics.object.ArbitraryPhysicsObject;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.util.Collection;
import net.minecraft.world.level.ChunkPos;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class RopePhysicsObject implements ArbitraryPhysicsObject {
   protected final ObjectList<Vector3d> points;
   protected final ObjectList<Vector3d> pointsView;
   protected final double collisionRadius;
   protected boolean active;
   protected RopeHandle handle;
   protected Vector3dc startAttachmentLocation = null;
   protected ServerSubLevel startAttachmentSubLevel = null;

   public RopePhysicsObject(Collection<Vector3d> points, double collisionRadius) {
      this.points = new ObjectArrayList(points);
      this.pointsView = ObjectLists.unmodifiable(this.points);
      this.collisionRadius = collisionRadius;
      this.active = false;
   }

   @Override
   public void getBoundingBox(BoundingBox3d dest) {
      Vector3d first = (Vector3d)this.points.getFirst();
      dest.set(first.x, first.y, first.z, first.x, first.y, first.z);
      ObjectListIterator var3 = this.points.iterator();

      while (var3.hasNext()) {
         Vector3d point = (Vector3d)var3.next();
         dest.expandTo(point.x - this.collisionRadius, point.y - this.collisionRadius, point.z - this.collisionRadius);
         dest.expandTo(point.x + this.collisionRadius, point.y + this.collisionRadius, point.z + this.collisionRadius);
      }
   }

   public double getCollisionRadius() {
      return this.collisionRadius;
   }

   public ObjectList<Vector3d> getPoints() {
      return this.pointsView;
   }

   public void updatePose() {
      this.handle.readPose(this.points);
   }

   public void setFirstSegmentLength(double length) {
      this.handle.setFirstSegmentLength(length);
   }

   public void removeFirstPoint() {
      this.points.removeFirst();
      if (this.isActive()) {
         this.handle.removeFirstPoint();
      }

      if (this.startAttachmentLocation != null) {
         this.setAttachment(RopeHandle.AttachmentPoint.START, this.startAttachmentLocation, this.startAttachmentSubLevel);
      }
   }

   public void addPoint(Vector3dc position) {
      this.points.addFirst(new Vector3d(position));
      if (this.isActive()) {
         this.handle.addPoint(position);
      }

      if (this.startAttachmentLocation != null) {
         this.setAttachment(RopeHandle.AttachmentPoint.START, this.startAttachmentLocation, this.startAttachmentSubLevel);
      }
   }

   public void setAttachment(RopeHandle.AttachmentPoint attachmentPoint, Vector3dc location, ServerSubLevel subLevel) {
      if (attachmentPoint == RopeHandle.AttachmentPoint.START) {
         this.startAttachmentSubLevel = subLevel;
         this.startAttachmentLocation = new Vector3d(location);
      }

      if (this.isActive()) {
         this.handle.setAttachment(attachmentPoint, location, subLevel);
      }
   }

   @Override
   public void onUnloaded(SubLevelHoldingChunkMap holdingChunkMap, ChunkPos chunkPos) {
      this.remove();
   }

   @Override
   public void onRemoved() {
      this.remove();
   }

   protected void remove() {
      this.active = false;
      this.handle.remove();
      this.handle = null;
   }

   @Override
   public void onAddition(SubLevelPhysicsSystem physicsSystem) {
      this.active = true;
      this.handle = physicsSystem.getPipeline().addRope(this);
   }

   @Override
   public void wakeUp() {
      if (this.isActive()) {
         this.handle.wakeUp();
      }
   }

   public boolean isActive() {
      return this.active;
   }
}
