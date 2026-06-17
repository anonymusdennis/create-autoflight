package com.simibubi.create.content.fluids.pump;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

public class PumpBlockEntity extends KineticBlockEntity {
   Couple<MutableBoolean> sidesToUpdate = Couple.create(MutableBoolean::new);
   boolean pressureUpdate;
   boolean scheduleFlip;

   public PumpBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      behaviours.add(new PumpBlockEntity.PumpFluidTransferBehaviour(this));
      this.registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.PUMP});
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.isClientSide || this.isVirtual()) {
         if (this.scheduleFlip) {
            this.level
               .setBlockAndUpdate(
                  this.worldPosition,
                  (BlockState)this.getBlockState().setValue(PumpBlock.FACING, ((Direction)this.getBlockState().getValue(PumpBlock.FACING)).getOpposite())
               );
            this.scheduleFlip = false;
         }

         this.sidesToUpdate.forEachWithContext((update, isFront) -> {
            if (!update.isFalse()) {
               update.setFalse();
               this.distributePressureTo(isFront ? this.getFront() : this.getFront().getOpposite());
            }
         });
      }
   }

   @Override
   public void onSpeedChanged(float previousSpeed) {
      super.onSpeedChanged(previousSpeed);
      if (Math.abs(previousSpeed) != Math.abs(this.getSpeed())) {
         if (this.speed != 0.0F) {
            this.award(AllAdvancements.PUMP);
         }

         if (!this.level.isClientSide || this.isVirtual()) {
            this.updatePressureChange();
         }
      }
   }

   public void updatePressureChange() {
      this.pressureUpdate = false;
      BlockPos frontPos = this.worldPosition.relative(this.getFront());
      BlockPos backPos = this.worldPosition.relative(this.getFront().getOpposite());
      FluidPropagator.propagateChangedPipe(this.level, frontPos, this.level.getBlockState(frontPos));
      FluidPropagator.propagateChangedPipe(this.level, backPos, this.level.getBlockState(backPos));
      FluidTransportBehaviour behaviour = this.getBehaviour(FluidTransportBehaviour.TYPE);
      if (behaviour != null) {
         behaviour.wipePressure();
      }

      this.sidesToUpdate.forEach(MutableBoolean::setTrue);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      if (compound.getBoolean("Reversed")) {
         this.scheduleFlip = true;
      }
   }

   protected void distributePressureTo(Direction side) {
      if (this.getSpeed() != 0.0F) {
         BlockFace start = new BlockFace(this.worldPosition, side);
         boolean pull = this.isPullingOnSide(this.isFront(side));
         Set<BlockFace> targets = new HashSet<>();
         Map<BlockPos, Pair<Integer, Map<Direction, Boolean>>> pipeGraph = new HashMap<>();
         if (!pull) {
            FluidPropagator.resetAffectedFluidNetworks(this.level, this.worldPosition, side.getOpposite());
         }

         if (!this.hasReachedValidEndpoint(this.level, start, pull)) {
            ((Map)pipeGraph.computeIfAbsent(this.worldPosition, $ -> Pair.of(0, new IdentityHashMap())).getSecond()).put(side, pull);
            ((Map)pipeGraph.computeIfAbsent(start.getConnectedPos(), $ -> Pair.of(1, new IdentityHashMap())).getSecond()).put(side.getOpposite(), !pull);
            List<Pair<Integer, BlockPos>> frontier = new ArrayList<>();
            Set<BlockPos> visited = new HashSet<>();
            int maxDistance = FluidPropagator.getPumpRange();
            frontier.add(Pair.of(1, start.getConnectedPos()));

            while (!frontier.isEmpty()) {
               Pair<Integer, BlockPos> entry = frontier.remove(0);
               int distance = (Integer)entry.getFirst();
               BlockPos currentPos = (BlockPos)entry.getSecond();
               if (this.level.isLoaded(currentPos) && !visited.contains(currentPos)) {
                  visited.add(currentPos);
                  BlockState currentState = this.level.getBlockState(currentPos);
                  FluidTransportBehaviour pipe = FluidPropagator.getPipe(this.level, currentPos);
                  if (pipe != null) {
                     for (Direction face : FluidPropagator.getPipeConnections(currentState, pipe)) {
                        BlockFace blockFace = new BlockFace(currentPos, face);
                        BlockPos connectedPos = blockFace.getConnectedPos();
                        if (this.level.isLoaded(connectedPos) && !blockFace.isEquivalent(start)) {
                           if (this.hasReachedValidEndpoint(this.level, blockFace, pull)) {
                              ((Map)pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap())).getSecond()).put(face, pull);
                              targets.add(blockFace);
                           } else {
                              FluidTransportBehaviour pipeBehaviour = FluidPropagator.getPipe(this.level, connectedPos);
                              if (pipeBehaviour != null
                                 && !(pipeBehaviour instanceof PumpBlockEntity.PumpFluidTransferBehaviour)
                                 && !visited.contains(connectedPos)) {
                                 if (distance + 1 >= maxDistance) {
                                    ((Map)pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap())).getSecond()).put(face, pull);
                                    targets.add(blockFace);
                                 } else {
                                    ((Map)pipeGraph.computeIfAbsent(currentPos, $ -> Pair.of(distance, new IdentityHashMap())).getSecond()).put(face, pull);
                                    ((Map)pipeGraph.computeIfAbsent(connectedPos, $ -> Pair.of(distance + 1, new IdentityHashMap())).getSecond())
                                       .put(face.getOpposite(), !pull);
                                    frontier.add(Pair.of(distance + 1, connectedPos));
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         Map<Integer, Set<BlockFace>> validFaces = new HashMap<>();
         this.searchForEndpointRecursively(pipeGraph, targets, validFaces, new BlockFace(start.getPos(), start.getOppositeFace()), pull);
         float pressure = Math.abs(this.getSpeed());

         for (Set<BlockFace> set : validFaces.values()) {
            int parallelBranches = Math.max(1, set.size() - 1);

            for (BlockFace facex : set) {
               BlockPos pipePos = facex.getPos();
               Direction pipeSide = facex.getFace();
               if (!pipePos.equals(this.worldPosition)) {
                  boolean inbound = (Boolean)((Map)pipeGraph.get(pipePos).getSecond()).get(pipeSide);
                  FluidTransportBehaviour pipeBehaviour = FluidPropagator.getPipe(this.level, pipePos);
                  if (pipeBehaviour != null) {
                     pipeBehaviour.addPressure(pipeSide, inbound, pressure / (float)parallelBranches);
                  }
               }
            }
         }
      }
   }

   protected boolean searchForEndpointRecursively(
      Map<BlockPos, Pair<Integer, Map<Direction, Boolean>>> pipeGraph,
      Set<BlockFace> targets,
      Map<Integer, Set<BlockFace>> validFaces,
      BlockFace currentFace,
      boolean pull
   ) {
      BlockPos currentPos = currentFace.getPos();
      if (!pipeGraph.containsKey(currentPos)) {
         return false;
      } else {
         Pair<Integer, Map<Direction, Boolean>> pair = pipeGraph.get(currentPos);
         int distance = (Integer)pair.getFirst();
         boolean atLeastOneBranchSuccessful = false;

         for (Direction nextFacing : Iterate.directions) {
            if (nextFacing != currentFace.getFace()) {
               Map<Direction, Boolean> map = (Map<Direction, Boolean>)pair.getSecond();
               if (map.containsKey(nextFacing)) {
                  BlockFace localTarget = new BlockFace(currentPos, nextFacing);
                  if (targets.contains(localTarget)) {
                     validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(localTarget);
                     atLeastOneBranchSuccessful = true;
                  } else if (map.get(nextFacing) == pull
                     && this.searchForEndpointRecursively(
                        pipeGraph, targets, validFaces, new BlockFace(currentPos.relative(nextFacing), nextFacing.getOpposite()), pull
                     )) {
                     validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(localTarget);
                     atLeastOneBranchSuccessful = true;
                  }
               }
            }
         }

         if (atLeastOneBranchSuccessful) {
            validFaces.computeIfAbsent(distance, $ -> new HashSet<>()).add(currentFace);
         }

         return atLeastOneBranchSuccessful;
      }
   }

   private boolean hasReachedValidEndpoint(LevelAccessor world, BlockFace blockFace, boolean pull) {
      BlockPos connectedPos = blockFace.getConnectedPos();
      BlockState connectedState = world.getBlockState(connectedPos);
      BlockEntity blockEntity = world.getBlockEntity(connectedPos);
      Direction face = blockFace.getFace();
      if (PumpBlock.isPump(connectedState)
         && ((Direction)connectedState.getValue(PumpBlock.FACING)).getAxis() == face.getAxis()
         && blockEntity instanceof PumpBlockEntity pumpBE) {
         return pumpBE.isPullingOnSide(pumpBE.isFront(blockFace.getOppositeFace())) != pull;
      } else {
         FluidTransportBehaviour pipe = FluidPropagator.getPipe(world, connectedPos);
         if (pipe != null && pipe.canHaveFlowToward(connectedState, blockFace.getOppositeFace())) {
            return false;
         } else {
            if (blockEntity != null) {
               IFluidHandler capability = (IFluidHandler)blockEntity.getLevel()
                  .getCapability(FluidHandler.BLOCK, blockEntity.getBlockPos(), face.getOpposite());
               if (capability != null) {
                  return true;
               }
            }

            return FluidPropagator.isOpenEnd(world, blockFace.getPos(), face);
         }
      }
   }

   public void updatePipesOnSide(Direction side) {
      if (this.isSideAccessible(side)) {
         this.updatePipeNetwork(this.isFront(side));
         this.getBehaviour(FluidTransportBehaviour.TYPE).wipePressure();
      }
   }

   protected boolean isFront(Direction side) {
      BlockState blockState = this.getBlockState();
      if (!(blockState.getBlock() instanceof PumpBlock)) {
         return false;
      } else {
         Direction front = (Direction)blockState.getValue(PumpBlock.FACING);
         return side == front;
      }
   }

   @Nullable
   protected Direction getFront() {
      BlockState blockState = this.getBlockState();
      return !(blockState.getBlock() instanceof PumpBlock) ? null : (Direction)blockState.getValue(PumpBlock.FACING);
   }

   protected void updatePipeNetwork(boolean front) {
      ((MutableBoolean)this.sidesToUpdate.get(front)).setTrue();
   }

   public boolean isSideAccessible(Direction side) {
      BlockState blockState = this.getBlockState();
      return !(blockState.getBlock() instanceof PumpBlock) ? false : ((Direction)blockState.getValue(PumpBlock.FACING)).getAxis() == side.getAxis();
   }

   public boolean isPullingOnSide(boolean front) {
      return !front;
   }

   class PumpFluidTransferBehaviour extends FluidTransportBehaviour {
      public PumpFluidTransferBehaviour(SmartBlockEntity be) {
         super(be);
      }

      @Override
      public void tick() {
         super.tick();

         for (Entry<Direction, PipeConnection> entry : this.interfaces.entrySet()) {
            boolean pull = PumpBlockEntity.this.isPullingOnSide(PumpBlockEntity.this.isFront(entry.getKey()));
            Couple<Float> pressure = entry.getValue().getPressure();
            pressure.set(pull, Math.abs(PumpBlockEntity.this.getSpeed()));
            pressure.set(!pull, 0.0F);
         }
      }

      @Override
      public boolean canHaveFlowToward(BlockState state, Direction direction) {
         return PumpBlockEntity.this.isSideAccessible(direction);
      }

      @Override
      public FluidTransportBehaviour.AttachmentTypes getRenderedRimAttachment(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
         FluidTransportBehaviour.AttachmentTypes attachment = super.getRenderedRimAttachment(world, pos, state, direction);
         return attachment == FluidTransportBehaviour.AttachmentTypes.RIM ? FluidTransportBehaviour.AttachmentTypes.NONE : attachment;
      }
   }
}
