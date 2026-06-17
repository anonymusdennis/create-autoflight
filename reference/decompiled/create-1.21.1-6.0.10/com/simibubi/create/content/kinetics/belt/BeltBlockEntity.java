package com.simibubi.create.content.kinetics.belt;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler;
import com.simibubi.create.content.kinetics.belt.transport.BeltTunnelInteractionHandler;
import com.simibubi.create.content.kinetics.belt.transport.ItemHandlerBeltSegment;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.items.IItemHandler;

public class BeltBlockEntity extends KineticBlockEntity implements Clearable {
   public Map<Entity, BeltMovementHandler.TransportedEntityInfo> passengers;
   public Optional<DyeColor> color;
   public int beltLength;
   public int index;
   public Direction lastInsert;
   public BeltBlockEntity.CasingType casing;
   public boolean covered;
   protected BlockPos controller = BlockPos.ZERO;
   protected BeltInventory inventory;
   protected IItemHandler itemHandler = null;
   public VersionedInventoryTrackerBehaviour invVersionTracker;
   public CompoundTag trackerUpdateTag;

   public BeltBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.casing = BeltBlockEntity.CasingType.NONE;
      this.color = Optional.empty();
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.BELT.get(), (be, context) -> {
         if (!BeltBlock.canTransportObjects(be.getBlockState())) {
            return null;
         } else {
            if (!be.isRemoved() && be.itemHandler == null) {
               be.initializeItemHandler();
            }

            return be.itemHandler;
         }
      });
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      behaviours.add(
         new DirectBeltInputBehaviour(this)
            .onlyInsertWhen(this::canInsertFrom)
            .setInsertionHandler(this::tryInsertingFromSide)
            .considerOccupiedWhen(this::isOccupied)
      );
      behaviours.add(new TransportedItemStackHandlerBehaviour(this, this::applyToAllItems).withStackPlacement(this::getWorldPositionOf));
      behaviours.add(this.invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
   }

   @Override
   public void tick() {
      if (this.beltLength == 0) {
         BeltBlock.initBelt(this.level, this.worldPosition);
      }

      super.tick();
      if (AllBlocks.BELT.has(this.level.getBlockState(this.worldPosition))) {
         this.initializeItemHandler();
         if (this.isController()) {
            this.invalidateRenderBoundingBox();
            this.getInventory().tick();
            if (this.getSpeed() != 0.0F) {
               if (this.passengers == null) {
                  this.passengers = new HashMap<>();
               }

               List<Entity> toRemove = new ArrayList<>();
               this.passengers.forEach((entity, info) -> {
                  boolean canBeTransported = BeltMovementHandler.canBeTransported(entity);
                  boolean leftTheBelt = info.getTicksSinceLastCollision() > (this.getBlockState().getValue(BeltBlock.SLOPE) != BeltSlope.HORIZONTAL ? 3 : 1);
                  if (canBeTransported && !leftTheBelt) {
                     info.tick();
                     BeltMovementHandler.transportEntity(this, entity, info);
                  } else {
                     toRemove.add(entity);
                  }
               });
               toRemove.forEach(this.passengers::remove);
            }
         }
      }
   }

   @Override
   public float calculateStressApplied() {
      return !this.isController() ? 0.0F : super.calculateStressApplied();
   }

   @Override
   public AABB createRenderBoundingBox() {
      return !this.isController() ? super.createRenderBoundingBox() : super.createRenderBoundingBox().inflate((double)(this.beltLength + 1));
   }

   protected void initializeItemHandler() {
      if (!this.level.isClientSide && this.itemHandler == null) {
         if (this.beltLength != 0 && this.controller != null) {
            if (this.level.isLoaded(this.controller)) {
               BlockEntity be = this.level.getBlockEntity(this.controller);
               if (be != null && be instanceof BeltBlockEntity) {
                  BeltInventory inventory = ((BeltBlockEntity)be).getInventory();
                  if (inventory != null) {
                     this.itemHandler = new ItemHandlerBeltSegment(inventory, this.index);
                     this.invalidateCapabilities();
                  }
               }
            }
         }
      }
   }

   public void clearContent() {
      if (this.inventory != null) {
         this.inventory.getTransportedItems().clear();
      }
   }

   @Override
   public void destroy() {
      super.destroy();
      if (this.isController()) {
         this.getInventory().ejectAll();
      }
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.invalidateCapabilities();
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      if (this.controller != null) {
         compound.put("Controller", NbtUtils.writeBlockPos(this.controller));
      }

      compound.putBoolean("IsController", this.isController());
      compound.putInt("Length", this.beltLength);
      compound.putInt("Index", this.index);
      NBTHelper.writeEnum(compound, "Casing", this.casing);
      compound.putBoolean("Covered", this.covered);
      this.color.ifPresent(dyeColor -> NBTHelper.writeEnum(compound, "Dye", dyeColor));
      if (this.isController()) {
         compound.put("Inventory", this.getInventory().write(registries));
      }

      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      if (compound.getBoolean("IsController")) {
         this.controller = this.worldPosition;
      }

      this.color = compound.contains("Dye") ? Optional.of((DyeColor)NBTHelper.readEnum(compound, "Dye", DyeColor.class)) : Optional.empty();
      if (!this.wasMoved) {
         if (!this.isController()) {
            this.controller = NBTHelper.readBlockPos(compound, "Controller");
         }

         this.trackerUpdateTag = compound;
         this.index = compound.getInt("Index");
         this.beltLength = compound.getInt("Length");
      }

      if (this.isController()) {
         this.getInventory().read(compound.getCompound("Inventory"), registries);
      }

      BeltBlockEntity.CasingType casingBefore = this.casing;
      boolean coverBefore = this.covered;
      this.casing = (BeltBlockEntity.CasingType)NBTHelper.readEnum(compound, "Casing", BeltBlockEntity.CasingType.class);
      this.covered = compound.getBoolean("Covered");
      if (clientPacket) {
         if (casingBefore != this.casing || coverBefore != this.covered) {
            if (!this.isVirtual()) {
               this.requestModelDataUpdate();
            }

            if (this.hasLevel()) {
               this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 16);
            }
         }
      }
   }

   @Override
   public void clearKineticInformation() {
      super.clearKineticInformation();
      this.beltLength = 0;
      this.index = 0;
      this.controller = null;
      this.trackerUpdateTag = new CompoundTag();
   }

   public boolean applyColor(DyeColor colorIn) {
      if (colorIn == null) {
         if (!this.color.isPresent()) {
            return false;
         }
      } else if (this.color.isPresent() && this.color.get() == colorIn) {
         return false;
      }

      if (this.level.isClientSide()) {
         return true;
      } else {
         for (BlockPos blockPos : BeltBlock.getBeltChain(this.level, this.getController())) {
            BeltBlockEntity belt = BeltHelper.getSegmentBE(this.level, blockPos);
            if (belt != null) {
               belt.color = Optional.ofNullable(colorIn);
               belt.setChanged();
               belt.sendData();
            }
         }

         return true;
      }
   }

   public BeltBlockEntity getControllerBE() {
      if (this.controller == null) {
         return null;
      } else if (!this.level.isLoaded(this.controller)) {
         return null;
      } else {
         BlockEntity be = this.level.getBlockEntity(this.controller);
         return be != null && be instanceof BeltBlockEntity ? (BeltBlockEntity)be : null;
      }
   }

   public void setController(BlockPos controller) {
      this.controller = controller;
   }

   public BlockPos getController() {
      return this.controller == null ? this.worldPosition : this.controller;
   }

   public boolean isController() {
      return this.controller != null
         && this.worldPosition.getX() == this.controller.getX()
         && this.worldPosition.getY() == this.controller.getY()
         && this.worldPosition.getZ() == this.controller.getZ();
   }

   public float getBeltMovementSpeed() {
      return this.getSpeed() / 480.0F;
   }

   public float getDirectionAwareBeltMovementSpeed() {
      int offset = this.getBeltFacing().getAxisDirection().getStep();
      if (this.getBeltFacing().getAxis() == Axis.X) {
         offset *= -1;
      }

      return this.getBeltMovementSpeed() * (float)offset;
   }

   public boolean hasPulley() {
      return !AllBlocks.BELT.has(this.getBlockState()) ? false : this.getBlockState().getValue(BeltBlock.PART) != BeltPart.MIDDLE;
   }

   protected boolean isLastBelt() {
      if (this.getSpeed() == 0.0F) {
         return false;
      } else {
         Direction direction = this.getBeltFacing();
         if (this.getBlockState().getValue(BeltBlock.SLOPE) == BeltSlope.VERTICAL) {
            return false;
         } else {
            BeltPart part = (BeltPart)this.getBlockState().getValue(BeltBlock.PART);
            if (part == BeltPart.MIDDLE) {
               return false;
            } else {
               boolean movingPositively = this.getSpeed() > 0.0F == (direction.getAxisDirection().getStep() == 1) ^ direction.getAxis() == Axis.X;
               return part == BeltPart.START ^ movingPositively;
            }
         }
      }
   }

   public Vec3i getMovementDirection(boolean firstHalf) {
      return this.getMovementDirection(firstHalf, false);
   }

   public Vec3i getBeltChainDirection() {
      return this.getMovementDirection(true, true);
   }

   protected Vec3i getMovementDirection(boolean firstHalf, boolean ignoreHalves) {
      if (this.getSpeed() == 0.0F) {
         return BlockPos.ZERO;
      } else {
         BlockState blockState = this.getBlockState();
         Direction beltFacing = (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
         BeltSlope slope = (BeltSlope)blockState.getValue(BeltBlock.SLOPE);
         BeltPart part = (BeltPart)blockState.getValue(BeltBlock.PART);
         Axis axis = beltFacing.getAxis();
         Direction movementFacing = Direction.get(axis == Axis.X ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE, axis);
         boolean notHorizontal = blockState.getValue(BeltBlock.SLOPE) != BeltSlope.HORIZONTAL;
         if (this.getSpeed() < 0.0F) {
            movementFacing = movementFacing.getOpposite();
         }

         Vec3i movement = movementFacing.getNormal();
         boolean slopeBeforeHalf = part == BeltPart.END == (beltFacing.getAxisDirection() == AxisDirection.POSITIVE);
         boolean onSlope = notHorizontal && (part == BeltPart.MIDDLE || slopeBeforeHalf == firstHalf || ignoreHalves);
         boolean movingUp = onSlope && slope == (movementFacing == beltFacing ? BeltSlope.UPWARD : BeltSlope.DOWNWARD);
         return !onSlope ? movement : new Vec3i(movement.getX(), movingUp ? 1 : -1, movement.getZ());
      }
   }

   public Direction getMovementFacing() {
      Axis axis = this.getBeltFacing().getAxis();
      return Direction.fromAxisAndDirection(axis, this.getBeltMovementSpeed() < 0.0F ^ axis == Axis.X ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE);
   }

   protected Direction getBeltFacing() {
      return (Direction)this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
   }

   public BeltInventory getInventory() {
      if (!this.isController()) {
         BeltBlockEntity controllerBE = this.getControllerBE();
         return controllerBE != null ? controllerBE.getInventory() : null;
      } else {
         if (this.inventory == null) {
            this.inventory = new BeltInventory(this);
         }

         return this.inventory;
      }
   }

   private void applyToAllItems(
      float maxDistanceFromCenter, Function<TransportedItemStack, TransportedItemStackHandlerBehaviour.TransportedResult> processFunction
   ) {
      BeltBlockEntity controller = this.getControllerBE();
      if (controller != null) {
         BeltInventory inventory = controller.getInventory();
         if (inventory != null) {
            inventory.applyToEachWithin((float)this.index + 0.5F, maxDistanceFromCenter, processFunction);
         }
      }
   }

   private Vec3 getWorldPositionOf(TransportedItemStack transported) {
      BeltBlockEntity controllerBE = this.getControllerBE();
      return controllerBE == null ? Vec3.ZERO : BeltHelper.getVectorForOffset(controllerBE, transported.beltPosition);
   }

   public void setCasingType(BeltBlockEntity.CasingType type) {
      if (this.casing != type) {
         BlockState blockState = this.getBlockState();
         boolean shouldBlockHaveCasing = type != BeltBlockEntity.CasingType.NONE;
         if (this.level.isClientSide) {
            this.casing = type;
            this.level.setBlock(this.worldPosition, (BlockState)blockState.setValue(BeltBlock.CASING, shouldBlockHaveCasing), 0);
            this.requestModelDataUpdate();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 16);
         } else {
            if (this.casing != BeltBlockEntity.CasingType.NONE) {
               this.level
                  .levelEvent(
                     2001,
                     this.worldPosition,
                     Block.getId(
                        this.casing == BeltBlockEntity.CasingType.ANDESITE
                           ? AllBlocks.ANDESITE_CASING.getDefaultState()
                           : AllBlocks.BRASS_CASING.getDefaultState()
                     )
                  );
            }

            if ((Boolean)blockState.getValue(BeltBlock.CASING) != shouldBlockHaveCasing) {
               KineticBlockEntity.switchToBlockState(this.level, this.worldPosition, (BlockState)blockState.setValue(BeltBlock.CASING, shouldBlockHaveCasing));
            }

            this.casing = type;
            this.setChanged();
            this.sendData();
         }
      }
   }

   private boolean canInsertFrom(Direction side) {
      if (this.getSpeed() == 0.0F) {
         return false;
      } else {
         BlockState state = this.getBlockState();
         return !state.hasProperty(BeltBlock.SLOPE)
               || state.getValue(BeltBlock.SLOPE) != BeltSlope.SIDEWAYS && state.getValue(BeltBlock.SLOPE) != BeltSlope.VERTICAL
            ? this.getMovementFacing() != side.getOpposite()
            : false;
      }
   }

   private boolean isOccupied(Direction side) {
      BeltBlockEntity nextBeltController = this.getControllerBE();
      if (nextBeltController == null) {
         return true;
      } else {
         BeltInventory nextInventory = nextBeltController.getInventory();
         if (nextInventory == null) {
            return true;
         } else if (this.getSpeed() == 0.0F) {
            return true;
         } else {
            return this.getMovementFacing() == side.getOpposite() ? true : !nextInventory.canInsertAtFromSide(this.index, side);
         }
      }
   }

   private ItemStack tryInsertingFromSide(TransportedItemStack transportedStack, Direction side, boolean simulate) {
      BeltBlockEntity nextBeltController = this.getControllerBE();
      ItemStack inserted = transportedStack.stack;
      ItemStack empty = ItemStack.EMPTY;
      if (!BeltBlock.canTransportObjects(this.getBlockState())) {
         return inserted;
      } else if (nextBeltController == null) {
         return inserted;
      } else {
         BeltInventory nextInventory = nextBeltController.getInventory();
         if (nextInventory == null) {
            return inserted;
         } else {
            if (this.level.getBlockEntity(this.worldPosition.above()) instanceof BrassTunnelBlockEntity tunnelBE && tunnelBE.hasDistributionBehaviour()) {
               if (!tunnelBE.getStackToDistribute().isEmpty()) {
                  return inserted;
               }

               if (!tunnelBE.testFlapFilter(side.getOpposite(), inserted)) {
                  return inserted;
               }

               if (!simulate) {
                  BeltTunnelInteractionHandler.flapTunnel(nextInventory, this.index, side.getOpposite(), true);
                  tunnelBE.setStackToDistribute(inserted, side.getOpposite());
               }

               return empty;
            }

            if (this.isOccupied(side)) {
               return inserted;
            } else if (simulate) {
               return empty;
            } else {
               transportedStack = transportedStack.copy();
               transportedStack.beltPosition = (float)this.index + 0.5F - Math.signum(this.getDirectionAwareBeltMovementSpeed()) / 16.0F;
               Direction movementFacing = this.getMovementFacing();
               if (!side.getAxis().isVertical()) {
                  if (movementFacing != side) {
                     transportedStack.sideOffset = (float)side.getAxisDirection().getStep() * 0.675F;
                     if (side.getAxis() == Axis.X) {
                        transportedStack.sideOffset *= -1.0F;
                     }
                  } else {
                     float extraOffset = transportedStack.prevBeltPosition != 0.0F
                           && BeltHelper.getSegmentBE(this.level, this.worldPosition.relative(movementFacing.getOpposite())) != null
                        ? 0.26F
                        : 0.0F;
                     transportedStack.beltPosition = this.getDirectionAwareBeltMovementSpeed() > 0.0F
                        ? (float)this.index - extraOffset
                        : (float)(this.index + 1) + extraOffset;
                  }
               }

               transportedStack.prevSideOffset = transportedStack.sideOffset;
               transportedStack.insertedAt = this.index;
               transportedStack.insertedFrom = side;
               transportedStack.prevBeltPosition = transportedStack.beltPosition;
               BeltTunnelInteractionHandler.flapTunnel(nextInventory, this.index, side.getOpposite(), true);
               nextInventory.addItem(transportedStack);
               nextBeltController.setChanged();
               nextBeltController.sendData();
               return empty;
            }
         }
      }
   }

   public ModelData getModelData() {
      return ModelData.builder().with(BeltModel.CASING_PROPERTY, this.casing).with(BeltModel.COVER_PROPERTY, this.covered).build();
   }

   @Override
   protected boolean canPropagateDiagonally(IRotate block, BlockState state) {
      return state.hasProperty(BeltBlock.SLOPE)
         && (state.getValue(BeltBlock.SLOPE) == BeltSlope.UPWARD || state.getValue(BeltBlock.SLOPE) == BeltSlope.DOWNWARD);
   }

   @Override
   public float propagateRotationTo(
      KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs
   ) {
      if (target instanceof BeltBlockEntity && !connectedViaAxes) {
         return this.getController().equals(((BeltBlockEntity)target).getController()) ? 1.0F : 0.0F;
      } else {
         return 0.0F;
      }
   }

   public void invalidateItemHandler() {
      this.invalidateCapabilities();
      this.itemHandler = null;
   }

   public boolean shouldRenderNormally() {
      if (this.level == null) {
         return this.isController();
      } else {
         BlockState state = this.getBlockState();
         return state != null && state.hasProperty(BeltBlock.PART) && state.getValue(BeltBlock.PART) == BeltPart.START;
      }
   }

   public void setCovered(boolean blockCoveringBelt) {
      if (blockCoveringBelt != this.covered) {
         this.covered = blockCoveringBelt;
         this.notifyUpdate();
      }
   }

   public static enum CasingType {
      NONE,
      ANDESITE,
      BRASS;
   }
}
