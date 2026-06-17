package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.IMergeableBE;
import com.simibubi.create.foundation.blockEntity.RemoveBlockEntityPacket;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

public class TrackBlockEntity extends SmartBlockEntity implements TransformableBlockEntity, IMergeableBE {
   Map<BlockPos, BezierConnection> connections = new HashMap<>();
   boolean cancelDrops;
   public Pair<ResourceKey<Level>, BlockPos> boundLocation;
   public TrackBlockEntityTilt tilt;

   public TrackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.setLazyTickRate(100);
      this.tilt = new TrackBlockEntityTilt(this);
   }

   public Map<BlockPos, BezierConnection> getConnections() {
      return this.connections;
   }

   @Override
   public void initialize() {
      super.initialize();
      if (!this.level.isClientSide && this.hasInteractableConnections()) {
         this.registerToCurveInteraction();
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.tilt.undoSmoothing();
   }

   @Override
   public void lazyTick() {
      for (BezierConnection connection : this.connections.values()) {
         if (connection.isPrimary()) {
            this.manageFakeTracksAlong(connection, false);
         }
      }
   }

   public void validateConnections() {
      Set<BlockPos> invalid = new HashSet<>();

      for (Entry<BlockPos, BezierConnection> entry : this.connections.entrySet()) {
         BlockPos key = entry.getKey();
         BezierConnection bc = entry.getValue();
         if (key.equals(bc.getKey()) && this.worldPosition.equals(bc.bePositions.getFirst())) {
            BlockState blockState = this.level.getBlockState(key);
            if (blockState.getBlock() instanceof ITrackBlock trackBlock && !(Boolean)blockState.getValue(TrackBlock.HAS_BE)) {
               for (Vec3 v : trackBlock.getTrackAxes(this.level, key, blockState)) {
                  Vec3 bcEndAxis = (Vec3)bc.axes.getSecond();
                  if (v.distanceTo(bcEndAxis) < 9.765625E-4 || v.distanceTo(bcEndAxis.scale(-1.0)) < 9.765625E-4) {
                     this.level.setBlock(key, (BlockState)blockState.setValue(TrackBlock.HAS_BE, true), 3);
                  }
               }
            }

            BlockEntity blockEntity = this.level.getBlockEntity(key);
            if (!(blockEntity instanceof TrackBlockEntity trackBE) || blockEntity.isRemoved()) {
               invalid.add(key);
               continue;
            }

            if (!trackBE.connections.containsKey(this.worldPosition)) {
               trackBE.addConnection(bc.secondary());
               trackBE.tilt.tryApplySmoothing();
            }
         } else {
            invalid.add(key);
         }
      }

      for (BlockPos blockPos : invalid) {
         this.removeConnection(blockPos);
      }
   }

   public void addConnection(BezierConnection connection) {
      if (!this.connections.containsKey(connection.getKey()) || !connection.equalsSansMaterial(this.connections.get(connection.getKey()))) {
         this.connections.put(connection.getKey(), connection);
         this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 1);
         this.notifyUpdate();
         if (connection.isPrimary()) {
            this.manageFakeTracksAlong(connection, false);
         }
      }
   }

   public void removeConnection(BlockPos target) {
      if (this.isTilted()) {
         this.tilt.captureSmoothingHandles();
      }

      BezierConnection removed = this.connections.remove(target);
      this.notifyUpdate();
      if (removed != null) {
         this.manageFakeTracksAlong(removed, true);
      }

      if (this.connections.isEmpty() && !this.getBlockState().getOptionalValue(TrackBlock.SHAPE).orElse(TrackShape.NONE).isPortal()) {
         BlockState blockState = this.level.getBlockState(this.worldPosition);
         if (blockState.hasProperty(TrackBlock.HAS_BE)) {
            this.level.setBlockAndUpdate(this.worldPosition, (BlockState)blockState.setValue(TrackBlock.HAS_BE, false));
         }

         if (this.level instanceof ServerLevel serverLevel) {
            CatnipServices.NETWORK.sendToClientsTrackingChunk(serverLevel, new ChunkPos(this.worldPosition), new RemoveBlockEntityPacket(this.worldPosition));
         }
      }
   }

   public void removeInboundConnections(boolean dropAndDiscard) {
      for (BezierConnection bezierConnection : this.connections.values()) {
         if (!(this.level.getBlockEntity(bezierConnection.getKey()) instanceof TrackBlockEntity tbe)) {
            return;
         }

         tbe.removeConnection((BlockPos)bezierConnection.bePositions.getFirst());
         if (dropAndDiscard) {
            if (!this.cancelDrops) {
               bezierConnection.spawnItems(this.level);
            }

            bezierConnection.spawnDestroyParticles(this.level);
         }
      }

      if (dropAndDiscard && this.level instanceof ServerLevel serverLevel) {
         CatnipServices.NETWORK.sendToClientsTrackingChunk(serverLevel, new ChunkPos(this.worldPosition), new RemoveBlockEntityPacket(this.worldPosition));
      }
   }

   public void bind(ResourceKey<Level> boundDimension, BlockPos boundLocation) {
      this.boundLocation = Pair.of(boundDimension, boundLocation);
      this.setChanged();
   }

   public boolean isTilted() {
      return this.tilt.smoothingAngle.isPresent();
   }

   @Override
   public void writeSafe(CompoundTag tag, Provider registries) {
      super.writeSafe(tag, registries);
      this.writeTurns(tag, true);
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      this.writeTurns(tag, false);
      if (this.isTilted()) {
         tag.putDouble("Smoothing", this.tilt.smoothingAngle.get());
      }

      if (this.boundLocation != null) {
         tag.put("BoundLocation", NbtUtils.writeBlockPos((BlockPos)this.boundLocation.getSecond()));
         tag.putString("BoundDimension", ((ResourceKey)this.boundLocation.getFirst()).location().toString());
      }
   }

   private void writeTurns(CompoundTag tag, boolean restored) {
      ListTag listTag = new ListTag();

      for (BezierConnection bezierConnection : this.connections.values()) {
         listTag.add((restored ? this.tilt.restoreToOriginalCurve(bezierConnection.clone()) : bezierConnection).write(this.worldPosition));
      }

      tag.put("Connections", listTag);
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.connections.clear();

      for (Tag t : tag.getList("Connections", 10)) {
         if (!(t instanceof CompoundTag)) {
            return;
         }

         BezierConnection connection = new BezierConnection((CompoundTag)t, this.worldPosition);
         this.connections.put(connection.getKey(), connection);
      }

      boolean smoothingPreviously = this.tilt.smoothingAngle.isPresent();
      this.tilt.smoothingAngle = Optional.ofNullable(tag.contains("Smoothing") ? tag.getDouble("Smoothing") : null);
      if (smoothingPreviously != this.tilt.smoothingAngle.isPresent() && clientPacket) {
         this.requestModelDataUpdate();
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 16);
      }

      CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> VisualizationHelper.queueUpdate(this));
      if (this.hasInteractableConnections()) {
         this.registerToCurveInteraction();
      } else {
         this.removeFromCurveInteraction();
      }

      if (tag.contains("BoundLocation")) {
         this.boundLocation = Pair.of(
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("BoundDimension"))), NBTHelper.readBlockPos(tag, "BoundLocation")
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public AABB getRenderBoundingBox() {
      return AABB.INFINITE;
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   @Override
   public void accept(BlockEntity other) {
      if (other instanceof TrackBlockEntity track) {
         this.connections.putAll(track.connections);
      }

      this.validateConnections();
      this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 1);
   }

   public boolean hasInteractableConnections() {
      for (BezierConnection connection : this.connections.values()) {
         if (connection.isPrimary()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void transform(BlockEntity be, StructureTransform transform) {
      Map<BlockPos, BezierConnection> restoredConnections = new HashMap<>();

      for (Entry<BlockPos, BezierConnection> entry : this.connections.entrySet()) {
         restoredConnections.put(entry.getKey(), this.tilt.restoreToOriginalCurve(this.tilt.restoreToOriginalCurve(entry.getValue().secondary()).secondary()));
      }

      this.connections = restoredConnections;
      this.tilt.smoothingAngle = Optional.empty();
      if (transform.rotationAxis == Axis.Y) {
         Map<BlockPos, BezierConnection> transformedConnections = new HashMap<>();

         for (Entry<BlockPos, BezierConnection> entry : this.connections.entrySet()) {
            BezierConnection newConnection = entry.getValue();
            newConnection.normals.replace(transform::applyWithoutOffsetUncentered);
            newConnection.axes.replace(transform::applyWithoutOffsetUncentered);
            BlockPos diff = ((BlockPos)newConnection.bePositions.getSecond()).subtract((Vec3i)newConnection.bePositions.getFirst());
            newConnection.bePositions
               .setSecond(
                  BlockPos.containing(
                     Vec3.atCenterOf((Vec3i)newConnection.bePositions.getFirst()).add(transform.applyWithoutOffsetUncentered(Vec3.atLowerCornerOf(diff)))
                  )
               );
            Vec3 beVec = Vec3.atLowerCornerOf(this.worldPosition);
            Vec3 teCenterVec = beVec.add(0.5, 0.5, 0.5);
            Vec3 start = (Vec3)newConnection.starts.getFirst();
            Vec3 startToBE = start.subtract(teCenterVec);
            Vec3 endToStart = ((Vec3)newConnection.starts.getSecond()).subtract(start);
            startToBE = transform.applyWithoutOffsetUncentered(startToBE).add(teCenterVec);
            endToStart = transform.applyWithoutOffsetUncentered(endToStart).add(startToBE);
            newConnection.starts.setFirst(new TrackNodeLocation(startToBE).getLocation());
            newConnection.starts.setSecond(new TrackNodeLocation(endToStart).getLocation());
            BlockPos newTarget = newConnection.getKey();
            transformedConnections.put(newTarget, newConnection);
         }

         this.connections = transformedConnections;
      }
   }

   @Override
   public void invalidate() {
      super.invalidate();
      if (this.level.isClientSide) {
         this.removeFromCurveInteraction();
      }
   }

   @Override
   public void remove() {
      super.remove();

      for (BezierConnection connection : this.connections.values()) {
         this.manageFakeTracksAlong(connection, true);
      }

      if (this.boundLocation != null && this.level instanceof ServerLevel) {
         ServerLevel otherLevel = this.level.getServer().getLevel((ResourceKey)this.boundLocation.getFirst());
         if (otherLevel == null) {
            return;
         }

         if (AllTags.AllBlockTags.TRACKS.matches(otherLevel.getBlockState((BlockPos)this.boundLocation.getSecond()))) {
            otherLevel.destroyBlock((BlockPos)this.boundLocation.getSecond(), false);
         }
      }
   }

   private void registerToCurveInteraction() {
      CatnipServices.PLATFORM.executeOnClientOnly(() -> this::registerToCurveInteractionUnsafe);
   }

   private void removeFromCurveInteraction() {
      CatnipServices.PLATFORM.executeOnClientOnly(() -> this::removeFromCurveInteractionUnsafe);
   }

   public ModelData getModelData() {
      return !this.isTilted()
         ? super.getModelData()
         : ModelData.builder().with(TrackBlockEntityTilt.ASCENDING_PROPERTY, this.tilt.smoothingAngle.get()).build();
   }

   @OnlyIn(Dist.CLIENT)
   private void registerToCurveInteractionUnsafe() {
      ((Map)TrackBlockOutline.TRACKS_WITH_TURNS.get(this.level)).put(this.worldPosition, this);
   }

   @OnlyIn(Dist.CLIENT)
   private void removeFromCurveInteractionUnsafe() {
      ((Map)TrackBlockOutline.TRACKS_WITH_TURNS.get(this.level)).remove(this.worldPosition);
   }

   public void manageFakeTracksAlong(BezierConnection bc, boolean remove) {
      Map<Pair<Integer, Integer>, Double> yLevels = bc.rasterise();

      for (Entry<Pair<Integer, Integer>, Double> entry : yLevels.entrySet()) {
         double yValue = entry.getValue();
         int floor = Mth.floor(yValue);
         BlockPos targetPos = new BlockPos((Integer)entry.getKey().getFirst(), floor, (Integer)entry.getKey().getSecond());
         targetPos = targetPos.offset((Vec3i)bc.bePositions.getFirst()).above(1);
         BlockState stateAtPos = this.level.getBlockState(targetPos);
         boolean present = AllBlocks.FAKE_TRACK.has(stateAtPos);
         if (remove) {
            if (present) {
               this.level.removeBlock(targetPos, false);
            }
         } else {
            FluidState fluidState = stateAtPos.getFluidState();
            if (fluidState.isEmpty() || fluidState.isSourceOfType(Fluids.WATER)) {
               if (!present && stateAtPos.canBeReplaced()) {
                  this.level.setBlock(targetPos, ProperWaterloggedBlock.withWater(this.level, AllBlocks.FAKE_TRACK.getDefaultState(), targetPos), 3);
               }

               FakeTrackBlock.keepAlive(this.level, targetPos);
            }
         }
      }
   }
}
