package com.simibubi.create.content.kinetics.chainConveyor;

import com.google.common.cache.Cache;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.codecs.CatnipCodecs;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ChainConveyorBlockEntity extends KineticBlockEntity implements TransformableBlockEntity, Clearable {
   public Set<BlockPos> connections = new HashSet<>();
   public Map<BlockPos, ChainConveyorBlockEntity.ConnectionStats> connectionStats;
   public Map<BlockPos, ChainConveyorBlockEntity.ConnectedPort> loopPorts = new HashMap<>();
   public Map<BlockPos, ChainConveyorBlockEntity.ConnectedPort> travelPorts = new HashMap<>();
   public ChainConveyorRoutingTable routingTable = new ChainConveyorRoutingTable();
   List<ChainConveyorPackage> loopingPackages = new ArrayList<>();
   Map<BlockPos, List<ChainConveyorPackage>> travellingPackages = new HashMap<>();
   public boolean reversed;
   public boolean cancelDrops;
   public boolean checkInvalid = true;
   BlockPos chainDestroyedEffectToSend;

   public ChainConveyorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return new AABB(this.worldPosition).inflate(this.connections.isEmpty() ? 3.0 : 64.0);
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      this.updateChainShapes();
   }

   public boolean canAcceptMorePackages() {
      return this.loopingPackages.size() + this.travellingPackages.size() < (Integer)AllConfigs.server().logistics.chainConveyorCapacity.get();
   }

   public boolean canAcceptPackagesFor(@Nullable BlockPos connection) {
      return connection == null && !this.canAcceptMorePackages()
         ? false
         : connection == null
            || this.level.getBlockEntity(this.worldPosition.offset(connection)) instanceof ChainConveyorBlockEntity otherClbe
               && otherClbe.canAcceptMorePackages();
   }

   public boolean canAcceptMorePackagesFromOtherConveyor() {
      return this.loopingPackages.size() < (Integer)AllConfigs.server().logistics.chainConveyorCapacity.get();
   }

   @Override
   public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      return super.addToTooltip(tooltip, isPlayerSneaking);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.checkInvalid && !this.level.isClientSide()) {
         this.checkInvalid = false;
         this.removeInvalidConnections();
      }

      float serverSpeed = this.level.isClientSide() && !this.isVirtual() ? ServerSpeedProvider.get() : 1.0F;
      float speed = this.getSpeed() / 360.0F;
      float radius = 1.5F;
      float distancePerTick = Math.abs(speed);
      float degreesPerTick = speed / ((float) Math.PI * radius) * 360.0F;
      boolean reversedPreviously = this.reversed;
      this.prepareStats();
      if (this.level.isClientSide() && !VisualizationManager.supportsVisualization(this.level)) {
         this.tickBoxVisuals();
      }

      if (!this.level.isClientSide()) {
         this.routingTable.tick();
         if (this.routingTable.shouldAdvertise()) {
            for (BlockPos pos : this.connections) {
               if (this.level.getBlockEntity(this.worldPosition.offset(pos)) instanceof ChainConveyorBlockEntity clbe) {
                  this.routingTable.advertiseTo(pos, clbe.routingTable);
               }
            }

            this.routingTable.changed = false;
            this.routingTable.lastUpdate = 0;
         }
      }

      if (speed == 0.0F) {
         this.updateBoxWorldPositions();
      } else {
         if (reversedPreviously != this.reversed) {
            for (Entry<BlockPos, List<ChainConveyorPackage>> entry : this.travellingPackages.entrySet()) {
               BlockPos offset = entry.getKey();
               BlockEntity iterator = this.level.getBlockEntity(this.worldPosition.offset(offset));
               if (iterator instanceof ChainConveyorBlockEntity) {
                  ChainConveyorBlockEntity otherLift = (ChainConveyorBlockEntity)iterator;
                  Iterator<ChainConveyorPackage> iteratorx = entry.getValue().iterator();

                  while (iteratorx.hasNext()) {
                     ChainConveyorPackage box = iteratorx.next();
                     if (!box.justFlipped) {
                        box.justFlipped = true;
                        float length = (float)Vec3.atLowerCornerOf(offset).length() - 1.375F;
                        box.chainPosition = length - box.chainPosition;
                        otherLift.addTravellingPackage(box, offset.multiply(-1));
                        iteratorx.remove();
                     }
                  }
               }
            }

            this.notifyUpdate();
         }

         for (Entry<BlockPos, List<ChainConveyorPackage>> entryx : this.travellingPackages.entrySet()) {
            BlockPos target = entryx.getKey();
            ChainConveyorBlockEntity.ConnectionStats stats = this.connectionStats.get(target);
            if (stats != null) {
               Iterator<ChainConveyorPackage> iterator = entryx.getValue().iterator();

               label182:
               while (iterator.hasNext()) {
                  ChainConveyorPackage box = iterator.next();
                  box.justFlipped = false;
                  float prevChainPosition = box.chainPosition;
                  box.chainPosition += serverSpeed * distancePerTick;
                  box.chainPosition = Math.min(stats.chainLength, box.chainPosition);
                  float anticipatePosition = box.chainPosition;
                  anticipatePosition += serverSpeed * distancePerTick * 4.0F;
                  anticipatePosition = Math.min(stats.chainLength, anticipatePosition);
                  if (!this.level.isClientSide() || this.isVirtual()) {
                     for (Entry<BlockPos, ChainConveyorBlockEntity.ConnectedPort> portEntry : this.travelPorts.entrySet()) {
                        ChainConveyorBlockEntity.ConnectedPort port = portEntry.getValue();
                        float chainPosition = port.chainPosition();
                        if (!(prevChainPosition > chainPosition) && target.equals(port.connection)) {
                           boolean notAtPositionYet = box.chainPosition < chainPosition;
                           if ((!notAtPositionYet || !(anticipatePosition < chainPosition)) && PackageItem.matchAddress(box.item, port.filter())) {
                              if (notAtPositionYet) {
                                 this.notifyPortToAnticipate(portEntry.getKey());
                              } else if (this.exportToPort(box, portEntry.getKey())) {
                                 iterator.remove();
                                 this.notifyUpdate();
                                 continue label182;
                              }
                           }
                        }
                     }

                     if (!(box.chainPosition < stats.chainLength)
                        && this.level.getBlockEntity(this.worldPosition.offset(target)) instanceof ChainConveyorBlockEntity clbe) {
                        box.chainPosition = this.wrapAngle(stats.tangentAngle + 180.0F + (float)(70 * (this.reversed ? -1 : 1)));
                        clbe.addLoopingPackage(box);
                        iterator.remove();
                        this.notifyUpdate();
                     }
                  }
               }
            }
         }

         Iterator<ChainConveyorPackage> iterator = this.loopingPackages.iterator();

         label148:
         while (iterator.hasNext()) {
            ChainConveyorPackage box = iterator.next();
            box.justFlipped = false;
            float prevChainPosition = box.chainPosition;
            box.chainPosition += serverSpeed * degreesPerTick;
            box.chainPosition = this.wrapAngle(box.chainPosition);
            float anticipatePosition = box.chainPosition;
            anticipatePosition += serverSpeed * degreesPerTick * 4.0F;
            anticipatePosition = this.wrapAngle(anticipatePosition);
            if (!this.level.isClientSide()) {
               for (Entry<BlockPos, ChainConveyorBlockEntity.ConnectedPort> portEntryx : this.loopPorts.entrySet()) {
                  ChainConveyorBlockEntity.ConnectedPort port = portEntryx.getValue();
                  float offBranchAngle = port.chainPosition();
                  boolean notAtPositionYet = !this.loopThresholdCrossed(box.chainPosition, prevChainPosition, offBranchAngle);
                  if ((!notAtPositionYet || this.loopThresholdCrossed(anticipatePosition, prevChainPosition, offBranchAngle))
                     && PackageItem.matchAddress(box.item, port.filter())) {
                     if (notAtPositionYet) {
                        this.notifyPortToAnticipate(portEntryx.getKey());
                     } else if (this.exportToPort(box, portEntryx.getKey())) {
                        iterator.remove();
                        this.notifyUpdate();
                        continue label148;
                     }
                  }
               }

               for (BlockPos connection : this.connections) {
                  if (this.level.getBlockEntity(this.worldPosition.offset(connection)) instanceof ChainConveyorBlockEntity ccbe
                     && !ccbe.canAcceptMorePackagesFromOtherConveyor()) {
                     continue;
                  }

                  float offBranchAngle = this.connectionStats.get(connection).tangentAngle;
                  if (this.loopThresholdCrossed(box.chainPosition, prevChainPosition, offBranchAngle)
                     && this.routingTable.getExitFor(box.item).equals(connection)) {
                     box.chainPosition = 0.0F;
                     this.addTravellingPackage(box, connection);
                     iterator.remove();
                     break;
                  }
               }
            }
         }

         this.updateBoxWorldPositions();
      }
   }

   public void removeInvalidConnections() {
      boolean changed = false;
      Iterator<BlockPos> iterator = this.connections.iterator();

      while (iterator.hasNext()) {
         BlockPos next = iterator.next();
         BlockPos target = this.worldPosition.offset(next);
         if (this.level.isLoaded(target)) {
            BlockEntity var6 = this.level.getBlockEntity(target);
            if (var6 instanceof ChainConveyorBlockEntity) {
               ChainConveyorBlockEntity ccbe = (ChainConveyorBlockEntity)var6;
               if (ccbe.connections.contains(next.multiply(-1))) {
                  continue;
               }
            }

            iterator.remove();
            changed = true;
         }
      }

      if (changed) {
         this.notifyUpdate();
      }
   }

   public void notifyConnectedToValidate() {
      for (BlockPos blockPos : this.connections) {
         BlockPos target = this.worldPosition.offset(blockPos);
         if (this.level.isLoaded(target) && this.level.getBlockEntity(target) instanceof ChainConveyorBlockEntity ccbe) {
            ccbe.checkInvalid = true;
         }
      }
   }

   public void tickBoxVisuals() {
      for (ChainConveyorPackage box : this.loopingPackages) {
         this.tickBoxVisuals(box);
      }

      for (Entry<BlockPos, List<ChainConveyorPackage>> entry : this.travellingPackages.entrySet()) {
         for (ChainConveyorPackage box : entry.getValue()) {
            this.tickBoxVisuals(box);
         }
      }
   }

   public boolean loopThresholdCrossed(float chainPosition, float prevChainPosition, float offBranchAngle) {
      int sign1 = Mth.sign((double)AngleHelper.getShortestAngleDiff((double)offBranchAngle, (double)prevChainPosition));
      int sign2 = Mth.sign((double)AngleHelper.getShortestAngleDiff((double)offBranchAngle, (double)chainPosition));
      boolean notCrossed = sign1 >= sign2 && !this.reversed || sign1 <= sign2 && this.reversed;
      return !notCrossed;
   }

   private boolean exportToPort(ChainConveyorPackage box, BlockPos offset) {
      BlockPos globalPos = this.worldPosition.offset(offset);
      if (this.level.getBlockEntity(globalPos) instanceof FrogportBlockEntity ppbe) {
         if (ppbe.isAnimationInProgress()) {
            return false;
         } else if (ppbe.isBackedUp()) {
            return false;
         } else {
            ppbe.startAnimation(box.item, false);
            return true;
         }
      } else {
         return false;
      }
   }

   private void notifyPortToAnticipate(BlockPos offset) {
      if (this.level.getBlockEntity(this.worldPosition.offset(offset)) instanceof FrogportBlockEntity ppbe) {
         ppbe.sendAnticipate();
      }
   }

   public boolean addTravellingPackage(ChainConveyorPackage box, BlockPos connection) {
      if (!this.connections.contains(connection)) {
         return false;
      } else {
         this.travellingPackages.computeIfAbsent(connection, $ -> new ArrayList<>()).add(box);
         if (this.level.isClientSide) {
            return true;
         } else {
            this.notifyUpdate();
            return true;
         }
      }
   }

   @Override
   public void notifyUpdate() {
      this.level.blockEntityChanged(this.worldPosition);
      this.sendData();
   }

   public boolean addLoopingPackage(ChainConveyorPackage box) {
      this.loopingPackages.add(box);
      this.notifyUpdate();
      return true;
   }

   public void prepareStats() {
      float speed = this.getSpeed();
      if (this.reversed != speed < 0.0F && speed != 0.0F) {
         this.reversed = speed < 0.0F;
         this.connectionStats = null;
      }

      if (this.connectionStats == null) {
         this.connectionStats = new HashMap<>();
         this.connections.forEach(this::calculateConnectionStats);
      }
   }

   public void updateBoxWorldPositions() {
      this.prepareStats();

      for (Entry<BlockPos, List<ChainConveyorPackage>> entry : this.travellingPackages.entrySet()) {
         BlockPos target = entry.getKey();
         ChainConveyorBlockEntity.ConnectionStats stats = this.connectionStats.get(target);
         if (stats != null) {
            for (ChainConveyorPackage box : entry.getValue()) {
               box.worldPosition = this.getPackagePosition(box.chainPosition, target);
               if (this.level != null && this.level.isClientSide()) {
                  Vec3 diff = stats.end.subtract(stats.start).normalize();
                  box.yaw = Mth.wrapDegrees((float)Mth.atan2(diff.x, diff.z) * (180.0F / (float)Math.PI) - 90.0F);
               }
            }
         }
      }

      for (ChainConveyorPackage boxx : this.loopingPackages) {
         boxx.worldPosition = this.getPackagePosition(boxx.chainPosition, null);
         boxx.yaw = Mth.wrapDegrees(boxx.chainPosition);
         if (this.reversed) {
            boxx.yaw += 180.0F;
         }
      }
   }

   public Vec3 getPackagePosition(float chainPosition, @Nullable BlockPos travelTarget) {
      if (travelTarget == null) {
         return Vec3.atBottomCenterOf(this.worldPosition).add(VecHelper.rotate(new Vec3(0.0, 0.375, 0.875), (double)chainPosition, Axis.Y));
      } else {
         this.prepareStats();
         ChainConveyorBlockEntity.ConnectionStats stats = this.connectionStats.get(travelTarget);
         if (stats == null) {
            return Vec3.ZERO;
         } else {
            Vec3 diff = stats.end.subtract(stats.start).normalize();
            return stats.start.add(diff.scale((double)Math.min(stats.chainLength, chainPosition)));
         }
      }
   }

   private void tickBoxVisuals(ChainConveyorPackage box) {
      if (box.worldPosition != null) {
         ChainConveyorPackage.ChainConveyorPackagePhysicsData physicsData = box.physicsData(this.level);
         physicsData.setBE(this);
         if (physicsData.shouldTick() || this.isVirtual()) {
            physicsData.prevTargetPos = physicsData.targetPos;
            physicsData.prevPos = physicsData.pos;
            physicsData.prevYaw = physicsData.yaw;
            physicsData.flipped = this.reversed;
            if (physicsData.pos != null) {
               if (physicsData.pos.distanceToSqr(box.worldPosition) > 2.25) {
                  physicsData.pos = box.worldPosition.add(physicsData.pos.subtract(box.worldPosition).normalize().scale(1.5));
               }

               physicsData.motion = physicsData.motion.add(0.0, -0.25, 0.0).scale(0.75).add(box.worldPosition.subtract(physicsData.pos).scale(0.25));
               physicsData.pos = physicsData.pos.add(physicsData.motion);
            }

            physicsData.targetPos = box.worldPosition.subtract(0.0, 0.5625, 0.0);
            if (physicsData.pos == null) {
               physicsData.pos = physicsData.targetPos;
               physicsData.prevPos = physicsData.targetPos;
               physicsData.prevTargetPos = physicsData.targetPos;
            }

            physicsData.yaw = AngleHelper.angleLerp(0.25, (double)physicsData.yaw, (double)box.yaw);
         }
      }
   }

   private void calculateConnectionStats(BlockPos connection) {
      boolean reversed = this.getSpeed() < 0.0F;
      float offBranchDistance = 35.0F;
      float direction = (180.0F / (float)Math.PI) * (float)Mth.atan2((double)connection.getX(), (double)connection.getZ());
      float angle = this.wrapAngle(direction - offBranchDistance * (float)(reversed ? -1 : 1));
      float oppositeAngle = this.wrapAngle(angle + 180.0F + 2.0F * offBranchDistance * (float)(reversed ? -1 : 1));
      Vec3 start = Vec3.atBottomCenterOf(this.worldPosition).add(VecHelper.rotate(new Vec3(0.0, 0.0, 1.25), (double)angle, Axis.Y)).add(0.0, 0.375, 0.0);
      Vec3 end = Vec3.atBottomCenterOf(this.worldPosition.offset(connection))
         .add(VecHelper.rotate(new Vec3(0.0, 0.0, 1.25), (double)oppositeAngle, Axis.Y))
         .add(0.0, 0.375, 0.0);
      float length = (float)start.distanceTo(end);
      this.connectionStats.put(connection, new ChainConveyorBlockEntity.ConnectionStats(angle, length, start, end));
   }

   public boolean addConnectionTo(BlockPos target) {
      BlockPos localTarget = target.subtract(this.worldPosition);
      boolean added = this.connections.add(localTarget);
      if (added) {
         this.notifyUpdate();
         this.calculateConnectionStats(localTarget);
         this.updateChainShapes();
      }

      this.detachKinetics();
      this.updateSpeed = true;
      return added;
   }

   public void chainDestroyed(BlockPos target, boolean spawnDrops, boolean sendEffect) {
      int chainCount = getChainCost(target);
      if (sendEffect) {
         this.chainDestroyedEffectToSend = target;
         this.sendData();
      }

      if (spawnDrops) {
         if (!this.forPointsAlongChains(
            target, chainCount, vec -> this.level.addFreshEntity(new ItemEntity(this.level, vec.x, vec.y, vec.z, new ItemStack(Items.CHAIN)))
         )) {
            while (chainCount > 0) {
               Block.popResource(this.level, this.worldPosition, new ItemStack(Blocks.CHAIN.asItem(), Math.min(chainCount, 64)));
               chainCount -= 64;
            }
         }
      }
   }

   public boolean removeConnectionTo(BlockPos target) {
      BlockPos localTarget = target.subtract(this.worldPosition);
      if (!this.connections.contains(localTarget)) {
         return false;
      } else {
         this.detachKinetics();
         this.connections.remove(localTarget);
         this.connectionStats.remove(localTarget);
         List<ChainConveyorPackage> packages = this.travellingPackages.remove(localTarget);
         if (packages != null) {
            for (ChainConveyorPackage box : packages) {
               this.drop(box);
            }
         }

         this.notifyUpdate();
         this.updateChainShapes();
         this.updateSpeed = true;
         return true;
      }
   }

   private void updateChainShapes() {
      this.prepareStats();
      List<ChainConveyorShape> shapes = new ArrayList<>();
      shapes.add(new ChainConveyorShape.ChainConveyorBB(Vec3.atBottomCenterOf(BlockPos.ZERO)));

      for (BlockPos target : this.connections) {
         ChainConveyorBlockEntity.ConnectionStats stats = this.connectionStats.get(target);
         if (stats != null) {
            Vec3 localStart = stats.start.subtract(Vec3.atLowerCornerOf(this.worldPosition));
            Vec3 localEnd = stats.end.subtract(Vec3.atLowerCornerOf(this.worldPosition));
            shapes.add(new ChainConveyorShape.ChainConveyorOBB(target, localStart, localEnd));
         }
      }

      if (this.level != null && this.level.isClientSide()) {
         ((Cache)ChainConveyorInteractionHandler.loadedChains.get(this.level)).put(this.worldPosition, shapes);
      }
   }

   @Override
   public void remove() {
      super.remove();
      if (this.level != null && this.level.isClientSide()) {
         for (BlockPos blockPos : this.connections) {
            this.spawnDestroyParticles(blockPos);
         }
      }
   }

   private void spawnDestroyParticles(BlockPos blockPos) {
      this.forPointsAlongChains(
         blockPos,
         (int)Math.round(Vec3.atLowerCornerOf(blockPos).length() * 8.0),
         vec -> this.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CHAIN.defaultBlockState()), vec.x, vec.y, vec.z, 0.0, 0.0, 0.0)
      );
   }

   public void clearContent() {
      this.connections.clear();
      this.travellingPackages.clear();
      this.loopingPackages.clear();
   }

   @Override
   public void destroy() {
      super.destroy();

      for (BlockPos blockPos : this.connections) {
         this.chainDestroyed(blockPos, !this.cancelDrops, false);
         if (this.level.getBlockEntity(this.worldPosition.offset(blockPos)) instanceof ChainConveyorBlockEntity clbe) {
            clbe.removeConnectionTo(this.worldPosition);
         }
      }

      for (ChainConveyorPackage box : this.loopingPackages) {
         this.drop(box);
      }

      for (Entry<BlockPos, List<ChainConveyorPackage>> entry : this.travellingPackages.entrySet()) {
         for (ChainConveyorPackage box : entry.getValue()) {
            this.drop(box);
         }
      }
   }

   public boolean forPointsAlongChains(BlockPos connection, int positions, Consumer<Vec3> callback) {
      this.prepareStats();
      ChainConveyorBlockEntity.ConnectionStats stats = this.connectionStats.get(connection);
      if (stats == null) {
         return false;
      } else {
         Vec3 start = stats.start;
         Vec3 direction = stats.end.subtract(start);
         Vec3 origin = Vec3.atCenterOf(this.worldPosition);
         Vec3 normal = direction.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
         Vec3 offset = start.subtract(origin);
         Vec3 start2 = origin.add(offset.add(normal.scale(-2.0 * normal.dot(offset))));

         for (boolean firstChain : Iterate.trueAndFalse) {
            int steps = positions / 2;
            if (firstChain) {
               steps += positions % 2;
            }

            for (int i = 0; i < steps; i++) {
               callback.accept((firstChain ? start : start2).add(direction.scale((0.5 + (double)i) / (double)steps)));
            }
         }

         return true;
      }
   }

   @Override
   public void invalidate() {
      super.invalidate();
      if (this.level != null && this.level.isClientSide()) {
         ((Cache)ChainConveyorInteractionHandler.loadedChains.get(this.level)).invalidate(this.worldPosition);
      }
   }

   private void drop(ChainConveyorPackage box) {
      if (box.worldPosition != null) {
         this.level.addFreshEntity(PackageEntity.fromItemStack(this.level, box.worldPosition.subtract(0.0, 0.5, 0.0), box.item));
      }
   }

   @Override
   public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
      this.connections.forEach(p -> neighbours.add(this.worldPosition.offset(p)));
      return super.addPropagationLocations(block, state, neighbours);
   }

   @Override
   public float propagateRotationTo(
      KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs
   ) {
      if (this.connections.contains(target.getBlockPos().subtract(this.worldPosition))) {
         return !(target instanceof ChainConveyorBlockEntity) ? 0.0F : 1.0F;
      } else {
         return super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
      }
   }

   @Override
   public void writeSafe(CompoundTag tag, Provider registries) {
      super.writeSafe(tag, registries);
      tag.put("Connections", (Tag)CatnipCodecUtils.encode(CatnipCodecs.set(BlockPos.CODEC), registries, this.connections).orElseThrow());
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      if (clientPacket && this.chainDestroyedEffectToSend != null) {
         compound.put("DestroyEffect", NbtUtils.writeBlockPos(this.chainDestroyedEffectToSend));
         this.chainDestroyedEffectToSend = null;
      }

      compound.put("Connections", (Tag)CatnipCodecUtils.encode(CatnipCodecs.set(BlockPos.CODEC), registries, this.connections).orElseThrow());
      compound.put(
         "TravellingPackages",
         NBTHelper.writeCompoundList(
            this.travellingPackages.entrySet(),
            entry -> {
               CompoundTag compoundTag = new CompoundTag();
               compoundTag.put("Target", NbtUtils.writeBlockPos((BlockPos)entry.getKey()));
               compoundTag.put(
                  "Packages", NBTHelper.writeCompoundList((Iterable)entry.getValue(), p -> clientPacket ? p.writeToClient(registries) : p.write(registries))
               );
               return compoundTag;
            }
         )
      );
      compound.put("LoopingPackages", NBTHelper.writeCompoundList(this.loopingPackages, p -> clientPacket ? p.writeToClient(registries) : p.write(registries)));
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      if (clientPacket && compound.contains("DestroyEffect") && this.level != null) {
         this.spawnDestroyParticles(NBTHelper.readBlockPos(compound, "DestroyEffect"));
      }

      int sizeBefore = this.connections.size();
      this.connections.clear();
      CatnipCodecUtils.decode(CatnipCodecs.set(BlockPos.CODEC), registries, compound.get("Connections")).ifPresent(this.connections::addAll);
      this.travellingPackages.clear();
      NBTHelper.iterateCompoundList(
         compound.getList("TravellingPackages", 10),
         c -> this.travellingPackages
               .put(NBTHelper.readBlockPos(c, "Target"), NBTHelper.readCompoundList(c.getList("Packages", 10), t -> ChainConveyorPackage.read(t, registries)))
      );
      this.loopingPackages = NBTHelper.readCompoundList(compound.getList("LoopingPackages", 10), t -> ChainConveyorPackage.read(t, registries));
      this.connectionStats = null;
      this.updateBoxWorldPositions();
      this.updateChainShapes();
      if (this.connections.size() != sizeBefore && this.level != null && this.level.isClientSide) {
         this.invalidateRenderBoundingBox();
      }
   }

   public float wrapAngle(float angle) {
      angle %= 360.0F;
      if (angle < 0.0F) {
         angle += 360.0F;
      }

      return angle;
   }

   public static int getChainCost(BlockPos connection) {
      return (int)Math.max(Math.round(Vec3.atLowerCornerOf(connection).length() / 2.5), 1L);
   }

   public static boolean getChainsFromInventory(Player player, ItemStack chain, int cost, boolean simulate) {
      int found = 0;
      Inventory inv = player.getInventory();
      int size = inv.items.size();

      for (int j = 0; j <= size + 1; j++) {
         int i = j;
         boolean offhand = j == size + 1;
         if (j == size) {
            i = inv.selected;
         } else if (offhand) {
            i = 0;
         } else if (j == inv.selected) {
            continue;
         }

         ItemStack stackInSlot = (ItemStack)(offhand ? inv.offhand : inv.items).get(i);
         if (stackInSlot.is(chain.getItem()) && found < cost) {
            int count = stackInSlot.getCount();
            if (!simulate) {
               int remainingItems = count - Math.min(cost - found, count);
               ItemStack newItem = stackInSlot.copyWithCount(remainingItems);
               if (offhand) {
                  player.setItemInHand(InteractionHand.OFF_HAND, newItem);
               } else {
                  inv.setItem(i, newItem);
               }
            }

            found += count;
         }
      }

      return found >= cost;
   }

   public List<ChainConveyorPackage> getLoopingPackages() {
      return this.loopingPackages;
   }

   public Map<BlockPos, List<ChainConveyorPackage>> getTravellingPackages() {
      return this.travellingPackages;
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state) {
      return super.getRequiredItems(state);
   }

   @Override
   public void transform(BlockEntity be, StructureTransform transform) {
      if (this.connections != null && !this.connections.isEmpty()) {
         this.connections = new HashSet<>(this.connections.stream().map(transform::applyWithoutOffset).toList());
         HashMap<BlockPos, List<ChainConveyorPackage>> newMap = new HashMap<>();
         this.travellingPackages.entrySet().forEach(e -> newMap.put(transform.applyWithoutOffset(e.getKey()), e.getValue()));
         this.travellingPackages = newMap;
         this.connectionStats = null;
         this.notifyUpdate();
      }
   }

   public static record ConnectedPort(float chainPosition, @Nullable BlockPos connection, String filter) {
   }

   public static record ConnectionStats(float tangentAngle, float chainLength, Vec3 start, Vec3 end) {
   }
}
