package com.simibubi.create.content.trains.signal;

import com.google.common.base.Objects;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SignalBoundary extends TrackEdgePoint {
   public Couple<Map<BlockPos, Boolean>> blockEntities = Couple.create(HashMap::new);
   public Couple<SignalBlock.SignalType> types;
   public Couple<UUID> groups;
   public Couple<Boolean> sidesToUpdate;
   public Couple<SignalBlockEntity.SignalState> cachedStates;
   private Couple<Map<UUID, Boolean>> chainedSignals = Couple.create(null, null);

   public SignalBoundary() {
      this.groups = Couple.create(null, null);
      this.sidesToUpdate = Couple.create(true, true);
      this.types = Couple.create(() -> SignalBlock.SignalType.ENTRY_SIGNAL);
      this.cachedStates = Couple.create(() -> SignalBlockEntity.SignalState.INVALID);
   }

   public void setGroup(boolean primary, UUID groupId) {
      UUID previous = (UUID)this.groups.get(primary);
      this.groups.set(primary, groupId);
      UUID opposite = (UUID)this.groups.get(!primary);
      Map<UUID, SignalEdgeGroup> signalEdgeGroups = Create.RAILWAYS.signalEdgeGroups;
      if (opposite != null && signalEdgeGroups.containsKey(opposite)) {
         SignalEdgeGroup oppositeGroup = signalEdgeGroups.get(opposite);
         if (previous != null) {
            oppositeGroup.removeAdjacent(previous);
         }

         if (groupId != null) {
            oppositeGroup.putAdjacent(groupId);
         }
      }

      if (groupId != null && signalEdgeGroups.containsKey(groupId)) {
         SignalEdgeGroup group = signalEdgeGroups.get(groupId);
         if (opposite != null) {
            group.putAdjacent(opposite);
         }
      }
   }

   public void setGroupAndUpdate(TrackNode side, UUID groupId) {
      boolean primary = this.isPrimary(side);
      this.setGroup(primary, groupId);
      this.sidesToUpdate.set(primary, false);
      this.chainedSignals.set(primary, null);
   }

   @Override
   public boolean canMerge() {
      return true;
   }

   @Override
   public void invalidate(LevelAccessor level) {
      this.blockEntities.forEach(s -> s.keySet().forEach(p -> this.invalidateAt(level, p)));
      this.groups.forEach(uuid -> {
         if (Create.RAILWAYS.signalEdgeGroups.remove(uuid) != null) {
            Create.RAILWAYS.sync.edgeGroupRemoved(uuid);
         }
      });
   }

   @Override
   public boolean canCoexistWith(EdgePointType<?> otherType, boolean front) {
      return otherType == this.getType();
   }

   @Override
   public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
      Map<BlockPos, Boolean> blockEntitiesOnSide = (Map<BlockPos, Boolean>)this.blockEntities.get(front);
      if (blockEntitiesOnSide.isEmpty()) {
         blockEntity.getBlockState().getOptionalValue(SignalBlock.TYPE).ifPresent(type -> this.types.set(front, type));
      }

      BlockPos var10001;
      boolean var10002;
      label15: {
         var10001 = blockEntity.getBlockPos();
         if (blockEntity instanceof SignalBlockEntity ste && ste.getReportedPower()) {
            var10002 = true;
            break label15;
         }

         var10002 = false;
      }

      blockEntitiesOnSide.put(var10001, var10002);
   }

   public void updateBlockEntityPower(SignalBlockEntity blockEntity) {
      for (boolean front : Iterate.trueAndFalse) {
         ((Map)this.blockEntities.get(front)).computeIfPresent(blockEntity.getBlockPos(), (p, c) -> blockEntity.getReportedPower());
      }
   }

   @Override
   public void blockEntityRemoved(BlockPos blockEntityPos, boolean front) {
      this.blockEntities.forEach(s -> s.remove(blockEntityPos));
      if (this.blockEntities.both(Map::isEmpty)) {
         this.removeFromAllGraphs();
      }
   }

   @Override
   public void onRemoved(TrackGraph graph) {
      super.onRemoved(graph);
      SignalPropagator.onSignalRemoved(graph, this);
   }

   public void queueUpdate(TrackNode side) {
      this.sidesToUpdate.set(this.isPrimary(side), true);
   }

   public UUID getGroup(TrackNode side) {
      return (UUID)this.groups.get(this.isPrimary(side));
   }

   @Override
   public boolean canNavigateVia(TrackNode side) {
      return !((Map)this.blockEntities.get(this.isPrimary(side))).isEmpty();
   }

   public SignalBlockEntity.OverlayState getOverlayFor(BlockPos blockEntity) {
      for (boolean first : Iterate.trueAndFalse) {
         Map<BlockPos, Boolean> set = (Map<BlockPos, Boolean>)this.blockEntities.get(first);
         Iterator var7 = set.keySet().iterator();
         if (var7.hasNext()) {
            BlockPos blockPos = (BlockPos)var7.next();
            if (blockPos.equals(blockEntity)) {
               return ((Map)this.blockEntities.get(!first)).isEmpty() ? SignalBlockEntity.OverlayState.RENDER : SignalBlockEntity.OverlayState.DUAL;
            }

            return SignalBlockEntity.OverlayState.SKIP;
         }
      }

      return SignalBlockEntity.OverlayState.SKIP;
   }

   public SignalBlock.SignalType getTypeFor(BlockPos blockEntity) {
      return (SignalBlock.SignalType)this.types.get(((Map)this.blockEntities.getFirst()).containsKey(blockEntity));
   }

   public SignalBlockEntity.SignalState getStateFor(BlockPos blockEntity) {
      for (boolean first : Iterate.trueAndFalse) {
         Map<BlockPos, Boolean> set = (Map<BlockPos, Boolean>)this.blockEntities.get(first);
         if (set.containsKey(blockEntity)) {
            return (SignalBlockEntity.SignalState)this.cachedStates.get(first);
         }
      }

      return SignalBlockEntity.SignalState.INVALID;
   }

   @Override
   public void tick(TrackGraph graph, boolean preTrains) {
      super.tick(graph, preTrains);
      if (!preTrains) {
         this.tickState(graph);
      } else {
         for (boolean front : Iterate.trueAndFalse) {
            if ((Boolean)this.sidesToUpdate.get(front)) {
               this.sidesToUpdate.set(front, false);
               SignalPropagator.propagateSignalGroup(graph, this, front);
               this.chainedSignals.set(front, null);
            }
         }
      }
   }

   private void tickState(TrackGraph graph) {
      for (boolean current : Iterate.trueAndFalse) {
         Map<BlockPos, Boolean> set = (Map<BlockPos, Boolean>)this.blockEntities.get(current);
         if (!set.isEmpty()) {
            boolean forcedRed = this.isForcedRed(current);
            UUID group = (UUID)this.groups.get(current);
            if (Objects.equal(group, this.groups.get(!current))) {
               this.cachedStates.set(current, SignalBlockEntity.SignalState.INVALID);
            } else {
               Map<UUID, SignalEdgeGroup> signalEdgeGroups = Create.RAILWAYS.signalEdgeGroups;
               SignalEdgeGroup signalEdgeGroup = signalEdgeGroups.get(group);
               if (signalEdgeGroup == null) {
                  this.cachedStates.set(current, SignalBlockEntity.SignalState.INVALID);
               } else {
                  boolean occupiedUnlessBySelf = forcedRed || signalEdgeGroup.isOccupiedUnless(this);
                  this.cachedStates.set(current, occupiedUnlessBySelf ? SignalBlockEntity.SignalState.RED : this.resolveSignalChain(graph, current));
               }
            }
         }
      }
   }

   public boolean isForcedRed(TrackNode side) {
      return this.isForcedRed(this.isPrimary(side));
   }

   public boolean isForcedRed(boolean primary) {
      for (Boolean b : ((Map)this.blockEntities.get(primary)).values()) {
         if (b) {
            return true;
         }
      }

      return false;
   }

   private SignalBlockEntity.SignalState resolveSignalChain(TrackGraph graph, boolean side) {
      if (this.types.get(side) != SignalBlock.SignalType.CROSS_SIGNAL) {
         return SignalBlockEntity.SignalState.GREEN;
      } else {
         if (this.chainedSignals.get(side) == null) {
            this.chainedSignals.set(side, SignalPropagator.collectChainedSignals(graph, this, side));
         }

         boolean allPathsFree = true;
         boolean noPathsFree = true;
         boolean invalid = false;

         for (Entry<UUID, Boolean> entry : ((Map)this.chainedSignals.get(side)).entrySet()) {
            UUID uuid = entry.getKey();
            boolean sideOfOther = entry.getValue();
            SignalBoundary otherSignal = graph.getPoint(EdgePointType.SIGNAL, uuid);
            if (otherSignal == null) {
               invalid = true;
               break;
            }

            if (!((Map)otherSignal.blockEntities.get(sideOfOther)).isEmpty()) {
               SignalBlockEntity.SignalState otherState = (SignalBlockEntity.SignalState)otherSignal.cachedStates.get(sideOfOther);
               allPathsFree &= otherState == SignalBlockEntity.SignalState.GREEN || otherState == SignalBlockEntity.SignalState.INVALID;
               noPathsFree &= otherState == SignalBlockEntity.SignalState.RED;
            }
         }

         if (invalid) {
            this.chainedSignals.set(side, null);
            return SignalBlockEntity.SignalState.INVALID;
         } else if (allPathsFree) {
            return SignalBlockEntity.SignalState.GREEN;
         } else {
            return noPathsFree ? SignalBlockEntity.SignalState.RED : SignalBlockEntity.SignalState.YELLOW;
         }
      }
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean migration, DimensionPalette dimensions) {
      super.read(nbt, registries, migration, dimensions);
      if (!migration) {
         this.blockEntities = Couple.create(HashMap::new);
         this.groups = Couple.create(null, null);

         for (int i = 1; i <= 2; i++) {
            if (nbt.contains("Tiles" + i)) {
               boolean first = i == 1;
               NBTHelper.iterateCompoundList(
                  nbt.getList("Tiles" + i, 10), c -> ((Map)this.blockEntities.get(first)).put(NBTHelper.readBlockPos(c, "Pos"), c.getBoolean("Power"))
               );
            }
         }

         for (int ix = 1; ix <= 2; ix++) {
            if (nbt.contains("Group" + ix)) {
               this.groups.set(ix == 1, nbt.getUUID("Group" + ix));
            }
         }

         for (int ixx = 1; ixx <= 2; ixx++) {
            this.sidesToUpdate.set(ixx == 1, nbt.contains("Update" + ixx));
         }

         for (int ixx = 1; ixx <= 2; ixx++) {
            this.types.set(ixx == 1, (SignalBlock.SignalType)NBTHelper.readEnum(nbt, "Type" + ixx, SignalBlock.SignalType.class));
         }

         for (int ixx = 1; ixx <= 2; ixx++) {
            this.cachedStates.set(ixx == 1, (SignalBlockEntity.SignalState)NBTHelper.readEnum(nbt, "State" + ixx, SignalBlockEntity.SignalState.class));
         }
      }
   }

   @Override
   public void read(FriendlyByteBuf buffer, DimensionPalette dimensions) {
      super.read(buffer, dimensions);

      for (int i = 1; i <= 2; i++) {
         if (buffer.readBoolean()) {
            this.groups.set(i == 1, buffer.readUUID());
         }
      }
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, DimensionPalette dimensions) {
      super.write(nbt, registries, dimensions);

      for (int i = 1; i <= 2; i++) {
         if (!((Map)this.blockEntities.get(i == 1)).isEmpty()) {
            nbt.put("Tiles" + i, NBTHelper.writeCompoundList(((Map)this.blockEntities.get(i == 1)).entrySet(), e -> {
               CompoundTag c = new CompoundTag();
               c.put("Pos", NbtUtils.writeBlockPos((BlockPos)e.getKey()));
               c.putBoolean("Power", (Boolean)e.getValue());
               return c;
            }));
         }
      }

      for (int ix = 1; ix <= 2; ix++) {
         if (this.groups.get(ix == 1) != null) {
            nbt.putUUID("Group" + ix, (UUID)this.groups.get(ix == 1));
         }
      }

      for (int ixx = 1; ixx <= 2; ixx++) {
         if ((Boolean)this.sidesToUpdate.get(ixx == 1)) {
            nbt.putBoolean("Update" + ixx, true);
         }
      }

      for (int ixxx = 1; ixxx <= 2; ixxx++) {
         NBTHelper.writeEnum(nbt, "Type" + ixxx, (SignalBlock.SignalType)this.types.get(ixxx == 1));
      }

      for (int ixxx = 1; ixxx <= 2; ixxx++) {
         NBTHelper.writeEnum(nbt, "State" + ixxx, (SignalBlockEntity.SignalState)this.cachedStates.get(ixxx == 1));
      }
   }

   @Override
   public void write(FriendlyByteBuf buffer, DimensionPalette dimensions) {
      super.write(buffer, dimensions);

      for (int i = 1; i <= 2; i++) {
         boolean hasGroup = this.groups.get(i == 1) != null;
         buffer.writeBoolean(hasGroup);
         if (hasGroup) {
            buffer.writeUUID((UUID)this.groups.get(i == 1));
         }
      }
   }

   public void cycleSignalType(BlockPos pos) {
      this.types
         .set(
            ((Map)this.blockEntities.getFirst()).containsKey(pos),
            SignalBlock.SignalType.values()[(this.getTypeFor(pos).ordinal() + 1) % SignalBlock.SignalType.values().length]
         );
   }
}
