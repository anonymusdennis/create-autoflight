package com.simibubi.create.content.redstone.link;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

public class RedstoneLinkBlockEntity extends SmartBlockEntity {
   private boolean receivedSignalChanged;
   private int receivedSignal;
   private int transmittedSignal;
   private LinkBehaviour link;
   private boolean transmitter;
   public FactoryPanelSupportBehaviour panelSupport;

   public RedstoneLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(
         this.panelSupport = new FactoryPanelSupportBehaviour(
            this,
            () -> this.link != null && this.link.isListening(),
            () -> this.receivedSignal > 0,
            () -> ((RedstoneLinkBlock)AllBlocks.REDSTONE_LINK.get()).updateTransmittedSignal(this.getBlockState(), this.level, this.worldPosition)
         )
      );
   }

   @Override
   public void addBehavioursDeferred(List<BlockEntityBehaviour> behaviours) {
      this.createLink();
      behaviours.add(this.link);
   }

   protected void createLink() {
      Pair<ValueBoxTransform, ValueBoxTransform> slots = ValueBoxTransform.Dual.makeSlots(RedstoneLinkFrequencySlot::new);
      this.link = this.transmitter ? LinkBehaviour.transmitter(this, slots, this::getSignal) : LinkBehaviour.receiver(this, slots, this::setSignal);
   }

   public int getSignal() {
      return this.transmittedSignal;
   }

   public void setSignal(int power) {
      if (this.receivedSignal != power) {
         this.receivedSignalChanged = true;
      }

      this.receivedSignal = power;
   }

   public void transmit(int strength) {
      this.transmittedSignal = strength;
      if (this.link != null) {
         this.link.notifySignalChange();
      }
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putBoolean("Transmitter", this.transmitter);
      compound.putInt("Receive", this.getReceivedSignal());
      compound.putBoolean("ReceivedChanged", this.receivedSignalChanged);
      compound.putInt("Transmit", this.transmittedSignal);
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.transmitter = compound.getBoolean("Transmitter");
      super.read(compound, registries, clientPacket);
      this.receivedSignal = compound.getInt("Receive");
      this.receivedSignalChanged = compound.getBoolean("ReceivedChanged");
      if (this.level == null || this.level.isClientSide || !this.link.newPosition) {
         this.transmittedSignal = compound.getInt("Transmit");
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.isTransmitterBlock() != this.transmitter) {
         this.transmitter = this.isTransmitterBlock();
         LinkBehaviour prevlink = this.link;
         this.removeBehaviour(LinkBehaviour.TYPE);
         this.createLink();
         this.link.copyItemsFrom(prevlink);
         this.attachBehaviourLate(this.link);
      }

      if (!this.transmitter) {
         if (!this.level.isClientSide) {
            BlockState blockState = this.getBlockState();
            if (AllBlocks.REDSTONE_LINK.has(blockState)) {
               if (this.getReceivedSignal() > 0 != (Boolean)blockState.getValue(RedstoneLinkBlock.POWERED)) {
                  this.receivedSignalChanged = true;
                  this.level.setBlockAndUpdate(this.worldPosition, (BlockState)blockState.cycle(RedstoneLinkBlock.POWERED));
               }

               if (this.receivedSignalChanged) {
                  this.updateSelfAndAttached(blockState);
               }
            }
         }
      }
   }

   @Override
   public void remove() {
      super.remove();
      this.updateSelfAndAttached(this.getBlockState());
   }

   public void updateSelfAndAttached(BlockState blockState) {
      Direction attachedFace = ((Direction)blockState.getValue(RedstoneLinkBlock.FACING)).getOpposite();
      BlockPos attachedPos = this.worldPosition.relative(attachedFace);
      this.level.blockUpdated(this.worldPosition, this.level.getBlockState(this.worldPosition).getBlock());
      this.level.blockUpdated(attachedPos, this.level.getBlockState(attachedPos).getBlock());
      this.receivedSignalChanged = false;
      this.panelSupport.notifyPanels();
   }

   protected Boolean isTransmitterBlock() {
      return !(Boolean)this.getBlockState().getValue(RedstoneLinkBlock.RECEIVER);
   }

   public int getReceivedSignal() {
      return this.receivedSignal;
   }
}
