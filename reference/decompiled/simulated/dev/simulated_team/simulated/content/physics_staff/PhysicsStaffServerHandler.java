package dev.simulated_team.simulated.content.physics_staff;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.config.server.physics.SimPhysics;
import dev.simulated_team.simulated.network.packets.physics_staff.PhysicsStaffDragSessionsPacket;
import dev.simulated_team.simulated.network.packets.physics_staff.PhysicsStaffLocksPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class PhysicsStaffServerHandler extends SavedData {
   public static final String ID = "simulated_physics_staff_lock_data";
   private final Map<UUID, PhysicsStaffServerHandler.Lock> locks = new Object2ObjectOpenHashMap();
   private final Map<UUID, PhysicsStaffServerHandler.DragSession> draggingSessions = new Object2ObjectOpenHashMap();
   private ServerLevel level;
   private boolean draggingSessionsDirty = false;

   public PhysicsStaffServerHandler() {
      this(null);
   }

   public PhysicsStaffServerHandler(LevelAccessor level) {
      this.level = (ServerLevel)level;
   }

   public static void sendAllData(Player player) {
      MinecraftServer server = player.getServer();

      assert server != null;

      for (ServerLevel level : server.getAllLevels()) {
         PhysicsStaffServerHandler handler = get(level);
         VeilPacketManager.player((ServerPlayer)player)
            .sendPacket(new CustomPacketPayload[]{new PhysicsStaffLocksPacket(level.dimension(), handler.locks.keySet()), makeSessionsPacket(level, handler)});
      }
   }

   @NotNull
   private static PhysicsStaffDragSessionsPacket makeSessionsPacket(ServerLevel level, PhysicsStaffServerHandler handler) {
      List<Pair<UUID, Vector3d>> sessions = new ObjectArrayList(handler.draggingSessions.size());

      for (Entry<UUID, PhysicsStaffServerHandler.DragSession> entry : handler.draggingSessions.entrySet()) {
         sessions.add(Pair.of(entry.getKey(), entry.getValue().plotAnchor));
      }

      return new PhysicsStaffDragSessionsPacket(level.dimension(), sessions);
   }

   private static FixedConstraintHandle addConstraint(ServerSubLevelContainer container, ServerSubLevel subLevel) {
      SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
      PhysicsPipeline pipeline = physicsSystem.getPipeline();
      return (FixedConstraintHandle)pipeline.addConstraint(
         null,
         subLevel,
         new FixedConstraintConfiguration(subLevel.logicalPose().position(), subLevel.logicalPose().rotationPoint(), subLevel.logicalPose().orientation())
      );
   }

   private static PhysicsStaffServerHandler create(ServerLevel level, CompoundTag nbt, Provider registries) {
      PhysicsStaffServerHandler sd = new PhysicsStaffServerHandler(level);
      sd.loadLocks(nbt.getList("simulated_physics_staff_lock_data", 11));
      return sd;
   }

   public static PhysicsStaffServerHandler get(ServerLevel level) {
      PhysicsStaffServerHandler data = (PhysicsStaffServerHandler)level.getChunkSource()
         .getDataStorage()
         .computeIfAbsent(new Factory(PhysicsStaffServerHandler::new, (nbt, lookup) -> create(level, nbt, lookup), null), "simulated_physics_staff_lock_data");
      data.level = level;
      return data;
   }

   public void tick() {
      Iterator<Entry<UUID, PhysicsStaffServerHandler.DragSession>> iter = this.draggingSessions.entrySet().iterator();

      while (iter.hasNext()) {
         Entry<UUID, PhysicsStaffServerHandler.DragSession> entry = iter.next();
         PhysicsStaffServerHandler.DragSession session = entry.getValue();
         Player player = this.level.getPlayerByUUID(entry.getKey());
         if (player != null && PhysicsStaffItem.isHolding(player)) {
            session.tick();
            if (session.isMarkedForRemoval()) {
               session.onRemoved();
               iter.remove();
               this.markDraggingSessionsDirty();
            }
         } else {
            session.onRemoved();
            iter.remove();
            this.markDraggingSessionsDirty();
         }
      }

      if (this.draggingSessionsDirty) {
         this.sendDragSessionsToClients();
         this.draggingSessionsDirty = false;
      }
   }

   private void markDraggingSessionsDirty() {
      this.draggingSessionsDirty = true;
   }

   public void physicsTick(SubLevelPhysicsSystem physicsSystem) {
      for (PhysicsStaffServerHandler.DragSession session : this.draggingSessions.values()) {
         session.physicsTick(physicsSystem);
      }
   }

   public void toggleLock(UUID uuid) {
      ServerSubLevelContainer container = SubLevelContainer.getContainer(this.level);
      ServerSubLevel subLevel = (ServerSubLevel)container.getSubLevel(uuid);
      if (subLevel != null) {
         PhysicsStaffServerHandler.Lock existingLock = this.locks.get(uuid);
         if (existingLock != null) {
            this.locks.remove(uuid);
            existingLock.remove();
            this.setLocksDirty();
         } else {
            FixedConstraintHandle handle = addConstraint(container, subLevel);
            this.locks.put(uuid, new PhysicsStaffServerHandler.Lock(uuid, handle));
            this.setLocksDirty();
         }
      }
   }

   private void setLocksDirty() {
      this.setDirty(true);
      this.sendLocksToClients();
   }

   private void sendLocksToClients() {
      VeilPacketManager.all(this.level.getServer())
         .sendPacket(new CustomPacketPayload[]{new PhysicsStaffLocksPacket(this.level.dimension(), this.locks.keySet())});
   }

   private void sendDragSessionsToClients() {
      VeilPacketManager.all(this.level.getServer()).sendPacket(new CustomPacketPayload[]{makeSessionsPacket(this.level, this)});
   }

   @NotNull
   public CompoundTag save(CompoundTag tag, @NotNull Provider provider) {
      ListTag tags = new ListTag();
      this.saveLocks(tags);
      tag.put("simulated_physics_staff_lock_data", tags);
      return tag;
   }

   private void loadLocks(ListTag list) {
      for (Tag tag : list) {
         UUID uuid = NbtUtils.loadUUID(tag);
         this.locks.put(uuid, new PhysicsStaffServerHandler.Lock(uuid, null));
      }
   }

   private void saveLocks(ListTag list) {
      list.addAll(this.locks.keySet().stream().map(NbtUtils::createUUID).toList());
   }

   public boolean isLocked(SubLevel subLevel) {
      PhysicsStaffServerHandler.Lock lock = this.locks.get(subLevel.getUniqueId());
      return lock != null && lock.handle != null && lock.handle.isValid();
   }

   @Internal
   public void applyLockIfNeeded(SubLevel subLevel) {
      PhysicsStaffServerHandler.Lock lock = this.locks.get(subLevel.getUniqueId());
      if (lock != null && (lock.handle == null || !lock.handle.isValid())) {
         ServerSubLevelContainer container = SubLevelContainer.getContainer(this.level);

         assert container != null;

         FixedConstraintHandle handle = addConstraint(container, (ServerSubLevel)subLevel);
         this.locks.put(lock.subLevel(), new PhysicsStaffServerHandler.Lock(lock.subLevel(), handle));
      }
   }

   public void removeLock(SubLevel subLevel) {
      PhysicsStaffServerHandler.Lock removedLock = this.locks.remove(subLevel.getUniqueId());
      if (removedLock != null) {
         removedLock.remove();
         this.setLocksDirty();
      }
   }

   public void drag(UUID playerUUID, UUID subLevelUUID, Vector3dc globalAnchor, Vector3dc localAnchor, Quaterniondc orientation) {
      ServerSubLevelContainer container = SubLevelContainer.getContainer(this.level);
      SubLevel subLevel = container.getSubLevel(subLevelUUID);
      if (subLevel != null) {
         this.removeLock(subLevel);
         PhysicsStaffServerHandler.DragSession session = this.draggingSessions.get(playerUUID);
         if (session == null) {
            this.draggingSessions.put(playerUUID, session = new PhysicsStaffServerHandler.DragSession(playerUUID, (ServerSubLevel)subLevel));
            this.markDraggingSessionsDirty();
         }

         session.playerRelativeGoal.set(globalAnchor);
         session.plotAnchor.set(localAnchor);
         session.orientation.set(orientation);
      }
   }

   public void stopDragging(UUID playerUUID) {
      PhysicsStaffServerHandler.DragSession session = this.draggingSessions.remove(playerUUID);
      if (session != null) {
         this.markDraggingSessionsDirty();
         session.onRemoved();
      }
   }

   private static class DragSession {
      private final UUID playerUUID;
      private final Vector3d plotAnchor = new Vector3d();
      private final Vector3d playerRelativeGoal = new Vector3d();
      private final Vector3d localGoal = new Vector3d();
      private final Quaterniond orientation = new Quaterniond();
      private final ServerSubLevel subLevel;
      private boolean markedForRemoval = false;
      private PhysicsConstraintHandle constraint = null;

      private DragSession(UUID playerUUID, ServerSubLevel subLevel) {
         this.playerUUID = playerUUID;
         this.subLevel = subLevel;
      }

      private void tick() {
         if (this.subLevel.isRemoved()) {
            this.markForRemoval();
         }
      }

      private void physicsTick(SubLevelPhysicsSystem physicsSystem) {
         if (!this.subLevel.isRemoved()) {
            if (this.constraint != null) {
               this.constraint.remove();
               this.constraint = null;
            }

            this.attachConstraint(physicsSystem);
            Player player = this.subLevel.getLevel().getPlayerByUUID(this.playerUUID);
            SimPhysics config = SimConfigService.INSTANCE.server().physics;
            if (player != null && this.constraint != null) {
               float angularStiffness = config.physicsStaffAngularStiffness.getF();
               float angularDamping = config.physicsStaffAngularDamping.getF();
               float linearStiffness = config.physicsStaffLinearStiffness.getF();
               float linearDamping = config.physicsStaffLinearDamping.getF();

               for (ConstraintJointAxis angularAxis : ConstraintJointAxis.ANGULAR) {
                  this.constraint.setMotor(angularAxis, 0.0, (double)angularStiffness, (double)angularDamping, false, 0.0);
               }

               double partialTick = physicsSystem.getPartialPhysicsTick();
               double eyePosX = Mth.lerp(partialTick, player.xOld, player.getX());
               double eyePosY = Mth.lerp(partialTick, player.yOld, player.getY()) + (double)player.getEyeHeight();
               double eyePosZ = Mth.lerp(partialTick, player.zOld, player.getZ());
               this.localGoal.set(this.playerRelativeGoal).add(eyePosX, eyePosY, eyePosZ);
               this.orientation.transformInverse(this.localGoal);
               this.constraint.setMotor(ConstraintJointAxis.LINEAR_X, this.localGoal.x(), (double)linearStiffness, (double)linearDamping, false, 0.0);
               this.constraint.setMotor(ConstraintJointAxis.LINEAR_Y, this.localGoal.y(), (double)linearStiffness, (double)linearDamping, false, 0.0);
               this.constraint.setMotor(ConstraintJointAxis.LINEAR_Z, this.localGoal.z(), (double)linearStiffness, (double)linearDamping, false, 0.0);
            }
         }
      }

      private void attachConstraint(SubLevelPhysicsSystem physicsSystem) {
         PhysicsPipeline pipeline = physicsSystem.getPipeline();
         FreeConstraintConfiguration config = new FreeConstraintConfiguration(JOMLConversion.ZERO, this.plotAnchor, this.orientation);
         this.constraint = pipeline.addConstraint(null, this.subLevel, config);
      }

      public boolean isMarkedForRemoval() {
         return this.markedForRemoval;
      }

      public void markForRemoval() {
         this.markedForRemoval = true;
      }

      public void onRemoved() {
         if (this.constraint != null) {
            this.constraint.remove();
         }

         this.constraint = null;
      }
   }

   private static record Lock(@NotNull UUID subLevel, @Nullable PhysicsConstraintHandle handle) {
      private void remove() {
         if (this.handle != null) {
            this.handle.remove();
         }
      }
   }
}
