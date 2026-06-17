package com.simibubi.create.content.redstone.smartObserver;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.content.redstone.FilteredDetectorFilterSlot;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.TankManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import java.util.List;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SmartObserverBlockEntity extends SmartBlockEntity implements Clearable {
   private static final int DEFAULT_DELAY = 6;
   private FilteringBehaviour filtering;
   private InvManipulationBehaviour observedInventory;
   private TankManipulationBehaviour observedTank;
   private VersionedInventoryTrackerBehaviour invVersionTracker;
   private boolean sustainSignal;
   public int turnOffTicks = 0;

   public SmartObserverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.setLazyTickRate(20);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.filtering = new FilteringBehaviour(this, new FilteredDetectorFilterSlot(false)).withCallback($ -> this.invVersionTracker.reset()));
      behaviours.add(this.invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
      CapManipulationBehaviourBase.InterfaceProvider towardBlockFacing = (w, p, s) -> new BlockFace(p, DirectedDirectionalBlock.getTargetDirection(s));
      behaviours.add(this.observedInventory = new InvManipulationBehaviour(this, towardBlockFacing).bypassSidedness());
      behaviours.add(this.observedTank = new TankManipulationBehaviour(this, towardBlockFacing).bypassSidedness());
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.isClientSide()) {
         BlockState state = this.getBlockState();
         if (this.turnOffTicks > 0) {
            this.turnOffTicks--;
            if (this.turnOffTicks == 0) {
               this.level.scheduleTick(this.worldPosition, state.getBlock(), 1);
            }
         }

         if (this.isActive()) {
            BlockPos targetPos = this.worldPosition.relative(SmartObserverBlock.getTargetDirection(state));
            Block block = this.level.getBlockState(targetPos).getBlock();
            if (!this.filtering.getFilter().isEmpty() && block.asItem() != null && this.filtering.test(new ItemStack(block))) {
               this.activate(3);
            } else {
               TransportedItemStackHandlerBehaviour behaviour = BlockEntityBehaviour.get(this.level, targetPos, TransportedItemStackHandlerBehaviour.TYPE);
               if (behaviour != null) {
                  behaviour.handleCenteredProcessingOnAllItems(0.45F, stack -> {
                     if (this.filtering.test(stack.stack) && this.turnOffTicks != 6) {
                        this.activate();
                        return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
                     } else {
                        return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
                     }
                  });
               } else {
                  FluidTransportBehaviour fluidBehaviour = BlockEntityBehaviour.get(this.level, targetPos, FluidTransportBehaviour.TYPE);
                  if (fluidBehaviour != null) {
                     for (Direction side : Iterate.directions) {
                        PipeConnection.Flow flow = fluidBehaviour.getFlow(side);
                        if (flow != null && flow.inbound && flow.complete && this.filtering.test(flow.fluid)) {
                           this.activate();
                           return;
                        }
                     }
                  } else if (this.level.getBlockEntity(targetPos) instanceof ChainConveyorBlockEntity ccbe) {
                     for (ChainConveyorPackage box : ccbe.getLoopingPackages()) {
                        if (this.filtering.test(box.item)) {
                           this.activate();
                           return;
                        }
                     }
                  } else {
                     if (this.observedInventory.hasInventory()) {
                        boolean skipInv = this.invVersionTracker.stillWaiting(this.observedInventory);
                        this.invVersionTracker.awaitNewVersion(this.observedInventory);
                        if (skipInv && this.sustainSignal) {
                           this.turnOffTicks = 6;
                        }

                        if (!skipInv) {
                           this.sustainSignal = false;
                           if (!this.observedInventory.simulate().extract().isEmpty()) {
                              this.sustainSignal = true;
                              this.activate();
                              return;
                           }
                        }
                     }

                     if (!this.observedTank.simulate().extractAny().isEmpty()) {
                        this.activate();
                     }
                  }
               }
            }
         }
      }
   }

   public void activate() {
      this.activate(6);
   }

   public void activate(int ticks) {
      BlockState state = this.getBlockState();
      this.turnOffTicks = ticks;
      if (!(Boolean)state.getValue(SmartObserverBlock.POWERED)) {
         this.level.setBlockAndUpdate(this.worldPosition, (BlockState)state.setValue(SmartObserverBlock.POWERED, true));
         this.level.updateNeighborsAt(this.worldPosition, state.getBlock());
      }
   }

   private boolean isActive() {
      return true;
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putInt("TurnOff", this.turnOffTicks);
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.turnOffTicks = compound.getInt("TurnOff");
   }

   public void clearContent() {
      this.filtering.setFilter(ItemStack.EMPTY);
   }
}
