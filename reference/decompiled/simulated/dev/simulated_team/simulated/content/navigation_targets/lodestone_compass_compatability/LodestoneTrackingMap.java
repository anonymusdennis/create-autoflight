package dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability;

import dev.engine_room.flywheel.lib.util.LevelAttached;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import dev.ryanhcode.sable.sublevel.tracking_points.TrackingPoint;
import dev.simulated_team.simulated.network.packets.lodestone_compass.UpdateClientLodestonePositionPacket;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.lang.ref.WeakReference;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class LodestoneTrackingMap extends SavedData {
   public static final String FILE_ID = "simulated_lodestone_tracker";
   private static final LevelAttached<LodestoneTrackingMap> LODESTONE_MAP = new LevelAttached(
      level -> level instanceof ServerLevel sl
            ? (LodestoneTrackingMap)sl.getDataStorage()
               .computeIfAbsent(new Factory(() -> new LodestoneTrackingMap(sl), (tag, prov) -> load(sl, tag), null), "simulated_lodestone_tracker")
            : null
   );
   private static final Vector3dc ZERO = new Vector3d();
   private static final Vector3d DUMMY = new Vector3d();
   private static final MutableBlockPos DUMMY_POS = new MutableBlockPos();
   private final ObjectOpenHashSet<LodestoneInformation> lodestoneInformationSet = new ObjectOpenHashSet();
   private final WeakReference<ServerLevel> associatedLevel;

   public static LodestoneTrackingMap getOrLoad(Level level) {
      return level.isClientSide ? null : (LodestoneTrackingMap)LODESTONE_MAP.get(level);
   }

   private static LodestoneTrackingMap load(ServerLevel level, CompoundTag tag) {
      LodestoneTrackingMap lodestoneMap = new LodestoneTrackingMap(level);

      for (Tag infoInner : tag.getList("TrackerInformation", 10)) {
         lodestoneMap.lodestoneInformationSet.add(LodestoneInformation.loadFromCompound((CompoundTag)infoInner));
      }

      return lodestoneMap;
   }

   public LodestoneTrackingMap(ServerLevel level) {
      this.associatedLevel = new WeakReference<>(level);
   }

   @NotNull
   public CompoundTag save(@NotNull CompoundTag compoundTag, @NotNull Provider provider) {
      ListTag lodestoneInformationList = new ListTag();
      ObjectIterator var4 = this.lodestoneInformationSet.iterator();

      while (var4.hasNext()) {
         LodestoneInformation info = (LodestoneInformation)var4.next();
         lodestoneInformationList.add(info.saveAsCompound());
      }

      compoundTag.put("TrackerInformation", lodestoneInformationList);
      return compoundTag;
   }

   public void tick() {
      if (!this.checkLevel()) {
         ServerLevel serverLevel = this.associatedLevel.get();
         SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad(serverLevel);
         int before = this.lodestoneInformationSet.size();
         this.checkLoadedLodestonePositions(data, serverLevel);
         if (before != this.lodestoneInformationSet.size()) {
            this.setDirty();
         }

         this.updateProjectedPositions(data, serverLevel);
      }
   }

   private void checkLoadedLodestonePositions(SubLevelTrackingPointSavedData data, ServerLevel serverLevel) {
      ObjectIterator<LodestoneInformation> lodestoneIter = this.lodestoneInformationSet.iterator();

      while (lodestoneIter.hasNext()) {
         LodestoneInformation info = (LodestoneInformation)lodestoneIter.next();
         TrackingPoint point = data.getTrackingPoint(info.id());
         if (point == null) {
            lodestoneIter.remove();
         } else {
            Vector3d lodestonePoint = point.point();
            if ((!point.inSubLevel() || Sable.HELPER.getContaining(serverLevel, point.point()) != null)
               && serverLevel.isLoaded(DUMMY_POS.set(lodestonePoint.x, lodestonePoint.y, lodestonePoint.z))) {
               BlockState state = serverLevel.getBlockState(DUMMY_POS);
               if (!state.is(Blocks.LODESTONE)) {
                  data.removeTrackingPoint(info.id());
                  lodestoneIter.remove();
               }
            }
         }
      }
   }

   private void updateProjectedPositions(SubLevelTrackingPointSavedData data, ServerLevel serverLevel) {
      ObjectIterator var3 = this.lodestoneInformationSet.iterator();

      while (var3.hasNext()) {
         LodestoneInformation info = (LodestoneInformation)var3.next();
         TrackingPoint point = data.getTrackingPoint(info.id());
         if (point != null) {
            if (point.inSubLevel()) {
               SubLevel existing = Sable.HELPER.getContaining(serverLevel, point.point());
               if (existing != null) {
                  existing.logicalPose().transformPosition(point.point(), info.projectedPos());
               } else if (point.subLevelID() != null) {
                  SubLevelHoldingChunkMap holdingMap = ServerSubLevelContainer.getContainer(serverLevel).getHoldingChunkMap();
                  HoldingSubLevel holdingSubLevel = holdingMap.getHoldingSubLevel(point.subLevelID());
                  if (holdingSubLevel != null) {
                     holdingSubLevel.data().pose().transformPosition(point.point(), info.projectedPos());
                  } else if (info.projectedPos().equals(ZERO)) {
                     GlobalSavedSubLevelPointer lastPointer = point.lastSavedSubLevelPointer();
                     if (lastPointer != null) {
                        SubLevelData subLevelData = holdingMap.getStorage().attemptLoadSubLevel(lastPointer.chunkPos(), lastPointer.local());
                        if (subLevelData != null) {
                           subLevelData.pose().transformPosition(point.point(), info.projectedPos());
                        }
                     }
                  }
               }
            } else {
               info.projectedPos().set(point.point());
            }
         }
      }
   }

   public LodestoneInformation getInformation(UUID id) {
      ObjectIterator var2 = this.lodestoneInformationSet.iterator();

      while (var2.hasNext()) {
         LodestoneInformation info = (LodestoneInformation)var2.next();
         if (info.id().equals(id)) {
            return info;
         }
      }

      return null;
   }

   @Nullable
   public UUID addOrGetLodestoneTrackingPoint(BlockPos pos) {
      if (this.checkLevel()) {
         return null;
      } else {
         ServerLevel sl = this.associatedLevel.get();
         SubLevelTrackingPointSavedData savedData = SubLevelTrackingPointSavedData.getOrLoad(sl);
         UUID lodestoneID = null;
         ObjectIterator var5 = this.lodestoneInformationSet.iterator();

         while (var5.hasNext()) {
            LodestoneInformation info = (LodestoneInformation)var5.next();
            TrackingPoint point = savedData.getTrackingPoint(info.id());
            if (point != null && point.point().sub((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, DUMMY).length() <= 0.1) {
               lodestoneID = info.id();
               break;
            }
         }

         return lodestoneID != null ? lodestoneID : this.generateTrackingPoint(pos, sl, savedData);
      }
   }

   @NotNull
   private UUID generateTrackingPoint(BlockPos pos, ServerLevel sl, SubLevelTrackingPointSavedData savedData) {
      LodestoneInformation newInfo = new LodestoneInformation(UUID.randomUUID(), new Vector3d());
      ServerSubLevel containing = (ServerSubLevel)Sable.HELPER.getContaining(sl, pos);
      boolean sublevelExists = containing != null;
      TrackingPoint trackingPoint = new TrackingPoint(
         sublevelExists,
         sublevelExists ? containing.getUniqueId() : null,
         sublevelExists ? containing.getLastSerializationPointer() : null,
         new Vector3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5),
         null
      );
      savedData.setTrackingPoint(newInfo.id(), trackingPoint);
      this.lodestoneInformationSet.add(newInfo);
      this.setDirty();
      return newInfo.id();
   }

   private boolean checkLevel() {
      return this.associatedLevel.get() == null;
   }

   public void sendUpdateForPlayer(UUID trackerID, ServerPlayer sp) {
      ObjectIterator var3 = this.lodestoneInformationSet.iterator();

      while (var3.hasNext()) {
         LodestoneInformation info = (LodestoneInformation)var3.next();
         if (info.id().equals(trackerID)) {
            VeilPacketManager.player(sp).sendPacket(new CustomPacketPayload[]{new UpdateClientLodestonePositionPacket(info.id(), info.projectedPos())});
            break;
         }
      }
   }
}
