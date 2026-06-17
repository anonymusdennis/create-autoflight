package com.simibubi.create.content.fluids.drain;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class ItemDrainBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
   public static final int FILLING_TIME = 20;
   SmartFluidTankBehaviour internalTank;
   TransportedItemStack heldItem;
   protected int processingTicks;
   Map<Direction, ItemDrainItemHandler> itemHandlers = new IdentityHashMap<>();

   public ItemDrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);

      for (Direction d : Iterate.horizontalDirections) {
         this.itemHandlers.put(d, new ItemDrainItemHandler(this, d));
      }
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(
         ItemHandler.BLOCK,
         (BlockEntityType)AllBlockEntityTypes.ITEM_DRAIN.get(),
         (be, context) -> context != null && context.getAxis().isHorizontal() ? be.itemHandlers.get(context) : null
      );
      event.registerBlockEntity(
         FluidHandler.BLOCK,
         (BlockEntityType)AllBlockEntityTypes.ITEM_DRAIN.get(),
         (be, context) -> context != Direction.UP ? be.internalTank.getCapability() : null
      );
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(new DirectBeltInputBehaviour(this).allowingBeltFunnels().setInsertionHandler(this::tryInsertingFromSide));
      behaviours.add(this.internalTank = SmartFluidTankBehaviour.single(this, 1500).allowExtraction().forbidInsertion());
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.DRAIN, AllAdvancements.CHAINED_DRAIN});
   }

   private ItemStack tryInsertingFromSide(TransportedItemStack transportedStack, Direction side, boolean simulate) {
      ItemStack inserted = transportedStack.stack;
      ItemStack returned = ItemStack.EMPTY;
      if (!this.getHeldItemStack().isEmpty()) {
         return inserted;
      } else {
         if (inserted.getCount() > 1 && GenericItemEmptying.canItemBeEmptied(this.level, inserted)) {
            returned = inserted.copyWithCount(inserted.getCount() - 1);
            inserted = inserted.copyWithCount(1);
         }

         if (simulate) {
            return returned;
         } else {
            transportedStack = transportedStack.copy();
            transportedStack.stack = inserted.copy();
            transportedStack.beltPosition = side.getAxis().isVertical() ? 0.5F : 0.0F;
            transportedStack.prevSideOffset = transportedStack.sideOffset;
            transportedStack.prevBeltPosition = transportedStack.beltPosition;
            this.setHeldItem(transportedStack, side);
            this.setChanged();
            this.sendData();
            return returned;
         }
      }
   }

   public ItemStack getHeldItemStack() {
      return this.heldItem == null ? ItemStack.EMPTY : this.heldItem.stack;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.heldItem == null) {
         this.processingTicks = 0;
      } else {
         boolean onClient = this.level.isClientSide && !this.isVirtual();
         if (this.processingTicks > 0) {
            this.heldItem.prevBeltPosition = 0.5F;
            boolean wasAtBeginning = this.processingTicks == 20;
            if (!onClient || this.processingTicks < 20) {
               this.processingTicks--;
            }

            if (!this.continueProcessing()) {
               this.processingTicks = 0;
               this.notifyUpdate();
            } else {
               if (wasAtBeginning != (this.processingTicks == 20)) {
                  this.sendData();
               }
            }
         } else {
            this.heldItem.prevBeltPosition = this.heldItem.beltPosition;
            this.heldItem.prevSideOffset = this.heldItem.sideOffset;
            this.heldItem.beltPosition = this.heldItem.beltPosition + this.itemMovementPerTick();
            if (this.heldItem.beltPosition > 1.0F) {
               this.heldItem.beltPosition = 1.0F;
               if (!onClient) {
                  Direction side = this.heldItem.insertedFrom;
                  ItemStack tryExportingToBeltFunnel = this.getBehaviour(DirectBeltInputBehaviour.TYPE)
                     .tryExportingToBeltFunnel(this.heldItem.stack, side.getOpposite(), false);
                  if (tryExportingToBeltFunnel != null) {
                     if (tryExportingToBeltFunnel.getCount() != this.heldItem.stack.getCount()) {
                        if (tryExportingToBeltFunnel.isEmpty()) {
                           this.heldItem = null;
                        } else {
                           this.heldItem.stack = tryExportingToBeltFunnel;
                        }

                        this.notifyUpdate();
                        return;
                     }

                     if (!tryExportingToBeltFunnel.isEmpty()) {
                        return;
                     }
                  }

                  BlockPos nextPosition = this.worldPosition.relative(side);
                  DirectBeltInputBehaviour directBeltInputBehaviour = BlockEntityBehaviour.get(this.level, nextPosition, DirectBeltInputBehaviour.TYPE);
                  if (directBeltInputBehaviour == null) {
                     if (!BlockHelper.hasBlockSolidSide(this.level.getBlockState(nextPosition), this.level, nextPosition, side.getOpposite())) {
                        ItemStack ejected = this.heldItem.stack;
                        Vec3 outPos = VecHelper.getCenterOf(this.worldPosition).add(Vec3.atLowerCornerOf(side.getNormal()).scale(0.75));
                        float movementSpeed = this.itemMovementPerTick();
                        Vec3 outMotion = Vec3.atLowerCornerOf(side.getNormal()).scale((double)movementSpeed).add(0.0, 0.125, 0.0);
                        outPos.add(outMotion.normalize());
                        ItemEntity entity = new ItemEntity(this.level, outPos.x, outPos.y + 0.375, outPos.z, ejected);
                        entity.setDeltaMovement(outMotion);
                        entity.setDefaultPickUpDelay();
                        entity.hurtMarked = true;
                        this.level.addFreshEntity(entity);
                        this.heldItem = null;
                        this.notifyUpdate();
                     }
                  } else if (directBeltInputBehaviour.canInsertFromSide(side)) {
                     ItemStack returned = directBeltInputBehaviour.handleInsertion(this.heldItem.copy(), side, false);
                     if (returned.isEmpty()) {
                        if (this.level.getBlockEntity(nextPosition) instanceof ItemDrainBlockEntity) {
                           this.award(AllAdvancements.CHAINED_DRAIN);
                        }

                        this.heldItem = null;
                        this.notifyUpdate();
                     } else if (returned.getCount() != this.heldItem.stack.getCount()) {
                        this.heldItem.stack = returned;
                        this.notifyUpdate();
                     }
                  }
               }
            } else {
               if (this.heldItem.prevBeltPosition < 0.5F && this.heldItem.beltPosition >= 0.5F) {
                  if (!GenericItemEmptying.canItemBeEmptied(this.level, this.heldItem.stack)) {
                     return;
                  }

                  this.heldItem.beltPosition = 0.5F;
                  if (onClient) {
                     return;
                  }

                  this.processingTicks = 20;
                  this.sendData();
               }
            }
         }
      }
   }

   protected boolean continueProcessing() {
      if (this.level.isClientSide && !this.isVirtual()) {
         return true;
      } else if (this.processingTicks < 5) {
         return true;
      } else if (!GenericItemEmptying.canItemBeEmptied(this.level, this.heldItem.stack)) {
         return false;
      } else {
         Pair<FluidStack, ItemStack> emptyItem = GenericItemEmptying.emptyItem(this.level, this.heldItem.stack, true);
         FluidStack fluidFromItem = (FluidStack)emptyItem.getFirst();
         if (this.processingTicks > 5) {
            this.internalTank.allowInsertion();
            if (this.internalTank.getPrimaryHandler().fill(fluidFromItem, FluidAction.SIMULATE) != fluidFromItem.getAmount()) {
               this.internalTank.forbidInsertion();
               this.processingTicks = 20;
               return true;
            } else {
               this.internalTank.forbidInsertion();
               return true;
            }
         } else {
            emptyItem = GenericItemEmptying.emptyItem(this.level, this.heldItem.stack.copy(), false);
            this.award(AllAdvancements.DRAIN);
            ItemStack out = (ItemStack)emptyItem.getSecond();
            if (!out.isEmpty()) {
               this.heldItem.stack = out;
            } else {
               this.heldItem = null;
            }

            this.internalTank.allowInsertion();
            this.internalTank.getPrimaryHandler().fill(fluidFromItem, FluidAction.EXECUTE);
            this.internalTank.forbidInsertion();
            this.notifyUpdate();
            return true;
         }
      }
   }

   private float itemMovementPerTick() {
      return 0.125F;
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.invalidateCapabilities();
   }

   public void setHeldItem(TransportedItemStack heldItem, Direction insertedFrom) {
      this.heldItem = heldItem;
      this.heldItem.insertedFrom = insertedFrom;
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putInt("ProcessingTicks", this.processingTicks);
      if (this.heldItem != null) {
         compound.put("HeldItem", this.heldItem.serializeNBT(registries));
      }

      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.heldItem = null;
      this.processingTicks = compound.getInt("ProcessingTicks");
      if (compound.contains("HeldItem")) {
         this.heldItem = TransportedItemStack.read(compound.getCompound("HeldItem"), registries);
      }

      super.read(compound, registries, clientPacket);
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      return this.containedFluidTooltip(tooltip, isPlayerSneaking, (IFluidHandler)this.level.getCapability(FluidHandler.BLOCK, this.worldPosition, null));
   }
}
