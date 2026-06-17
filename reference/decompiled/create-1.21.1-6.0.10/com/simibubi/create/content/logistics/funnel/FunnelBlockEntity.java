package com.simibubi.create.content.logistics.funnel;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.redstone.smartObserver.SmartObserverBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryTrackerBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import java.lang.ref.WeakReference;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.BlockFace;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class FunnelBlockEntity extends SmartBlockEntity implements IHaveHoveringInformation, Clearable {
   private FilteringBehaviour filtering;
   private InvManipulationBehaviour invManipulation;
   private VersionedInventoryTrackerBehaviour invVersionTracker;
   private int extractionCooldown = 0;
   private WeakReference<Entity> lastObserved;
   LerpedFloat flap = this.createChasingFlap();
   static final AABB coreBB = new AABB(BlockPos.ZERO);

   public FunnelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   FunnelBlockEntity.Mode determineCurrentMode() {
      BlockState state = this.getBlockState();
      if (!FunnelBlock.isFunnel(state)) {
         return FunnelBlockEntity.Mode.INVALID;
      } else if (state.getOptionalValue(BlockStateProperties.POWERED).orElse(false)) {
         return FunnelBlockEntity.Mode.PAUSED;
      } else if (state.getBlock() instanceof BeltFunnelBlock) {
         BeltFunnelBlock.Shape shape = (BeltFunnelBlock.Shape)state.getValue(BeltFunnelBlock.SHAPE);
         if (shape == BeltFunnelBlock.Shape.PULLING) {
            return FunnelBlockEntity.Mode.TAKING_FROM_BELT;
         } else if (shape == BeltFunnelBlock.Shape.PUSHING) {
            return FunnelBlockEntity.Mode.PUSHING_TO_BELT;
         } else {
            BeltBlockEntity belt = BeltHelper.getSegmentBE(this.level, this.worldPosition.below());
            if (belt != null) {
               return belt.getMovementFacing() == state.getValue(BeltFunnelBlock.HORIZONTAL_FACING)
                  ? FunnelBlockEntity.Mode.PUSHING_TO_BELT
                  : FunnelBlockEntity.Mode.TAKING_FROM_BELT;
            } else {
               return FunnelBlockEntity.Mode.INVALID;
            }
         }
      } else if (state.getBlock() instanceof FunnelBlock) {
         return state.getValue(FunnelBlock.EXTRACTING) ? FunnelBlockEntity.Mode.EXTRACT : FunnelBlockEntity.Mode.COLLECT;
      } else {
         return FunnelBlockEntity.Mode.INVALID;
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.flap.tickChaser();
      FunnelBlockEntity.Mode mode = this.determineCurrentMode();
      if (!this.level.isClientSide) {
         if (mode == FunnelBlockEntity.Mode.PAUSED) {
            this.extractionCooldown = 0;
         }

         if (mode != FunnelBlockEntity.Mode.TAKING_FROM_BELT) {
            if (this.extractionCooldown > 0) {
               this.extractionCooldown--;
            } else {
               if (mode == FunnelBlockEntity.Mode.PUSHING_TO_BELT) {
                  this.activateExtractingBeltFunnel();
               }

               if (mode == FunnelBlockEntity.Mode.EXTRACT) {
                  this.activateExtractor();
               }
            }
         }
      }
   }

   private void activateExtractor() {
      if (!this.invVersionTracker.stillWaiting(this.invManipulation)) {
         BlockState blockState = this.getBlockState();
         Direction facing = AbstractFunnelBlock.getFunnelFacing(blockState);
         if (facing != null) {
            Entity lastEntity = this.lastObserved != null ? this.lastObserved.get() : null;
            if (lastEntity != null && lastEntity.isAlive()) {
               AABB area = this.getEntityOverflowScanningArea();
               if (lastEntity.getBoundingBox().intersects(area)) {
                  return;
               }

               this.lastObserved = null;
            }

            int amountToExtract = this.getAmountToExtract();
            ItemHelper.ExtractionCountMode mode = this.getModeToExtract();
            ItemStack stack = this.invManipulation.simulate().extract(mode, amountToExtract);
            if (stack.isEmpty()) {
               this.invVersionTracker.awaitNewVersion(this.invManipulation);
            } else {
               AABB area = this.getEntityOverflowScanningArea();

               for (Entity entity : this.level.getEntities(null, area)) {
                  if (entity instanceof ItemEntity || entity instanceof PackageEntity) {
                     this.lastObserved = new WeakReference<>(entity);
                     return;
                  }
               }

               stack = this.invManipulation.extract(mode, amountToExtract);
               if (!stack.isEmpty()) {
                  this.flap(false);
                  this.onTransfer(stack);
                  Vec3 outputPos = VecHelper.getCenterOf(this.worldPosition);
                  boolean vertical = facing.getAxis().isVertical();
                  boolean up = facing == Direction.UP;
                  outputPos = outputPos.add(Vec3.atLowerCornerOf(facing.getNormal()).scale(vertical ? (up ? 0.15F : 0.5) : 0.25));
                  if (!vertical) {
                     outputPos = outputPos.subtract(0.0, 0.45F, 0.0);
                  }

                  Vec3 motion = Vec3.ZERO;
                  if (up) {
                     motion = new Vec3(0.0, 0.25, 0.0);
                  }

                  ItemEntity item = new ItemEntity(this.level, outputPos.x, outputPos.y, outputPos.z, stack.copy());
                  item.setDefaultPickUpDelay();
                  item.setDeltaMovement(motion);
                  this.level.addFreshEntity(item);
                  this.lastObserved = new WeakReference<>(item);
                  this.startCooldown();
               }
            }
         }
      }
   }

   private AABB getEntityOverflowScanningArea() {
      Direction facing = AbstractFunnelBlock.getFunnelFacing(this.getBlockState());
      AABB bb = coreBB.move(this.worldPosition);
      return facing != null && facing != Direction.UP ? bb.expandTowards(0.0, -1.0, 0.0) : bb;
   }

   private void activateExtractingBeltFunnel() {
      if (!this.invVersionTracker.stillWaiting(this.invManipulation)) {
         BlockState blockState = this.getBlockState();
         Direction facing = (Direction)blockState.getValue(BeltFunnelBlock.HORIZONTAL_FACING);
         DirectBeltInputBehaviour inputBehaviour = BlockEntityBehaviour.get(this.level, this.worldPosition.below(), DirectBeltInputBehaviour.TYPE);
         if (inputBehaviour != null) {
            if (inputBehaviour.canInsertFromSide(facing)) {
               if (!inputBehaviour.isOccupied(facing)) {
                  int amountToExtract = this.getAmountToExtract();
                  ItemHelper.ExtractionCountMode mode = this.getModeToExtract();
                  MutableBoolean deniedByInsertion = new MutableBoolean(false);
                  ItemStack stack = this.invManipulation.extract(mode, amountToExtract, s -> {
                     ItemStack handleInsertion = inputBehaviour.handleInsertion(s, facing, true);
                     if (handleInsertion.isEmpty()) {
                        return true;
                     } else {
                        deniedByInsertion.setTrue();
                        return false;
                     }
                  });
                  if (stack.isEmpty()) {
                     if (deniedByInsertion.isFalse()) {
                        this.invVersionTracker.awaitNewVersion(this.invManipulation.getInventory());
                     }
                  } else {
                     this.flap(false);
                     this.onTransfer(stack);
                     inputBehaviour.handleInsertion(stack, facing, false);
                     this.startCooldown();
                  }
               }
            }
         }
      }
   }

   public int getAmountToExtract() {
      if (!this.supportsAmountOnFilter()) {
         return 64;
      } else {
         int amountToExtract = this.invManipulation.getAmountFromFilter();
         if (!this.filtering.isActive()) {
            amountToExtract = 1;
         }

         return amountToExtract;
      }
   }

   public ItemHelper.ExtractionCountMode getModeToExtract() {
      return this.supportsAmountOnFilter() && this.filtering.isActive() ? this.invManipulation.getModeFromFilter() : ItemHelper.ExtractionCountMode.UPTO;
   }

   private int startCooldown() {
      return this.extractionCooldown = (Integer)AllConfigs.server().logistics.defaultExtractionTimer.get();
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.invManipulation = new InvManipulationBehaviour(this, (w, p, s) -> new BlockFace(p, AbstractFunnelBlock.getFunnelFacing(s).getOpposite()));
      behaviours.add(this.invManipulation);
      behaviours.add(this.invVersionTracker = new VersionedInventoryTrackerBehaviour(this));
      this.filtering = new FilteringBehaviour(this, new FunnelFilterSlotPositioning());
      this.filtering.showCountWhen(this::supportsAmountOnFilter);
      this.filtering.onlyActiveWhen(this::supportsFiltering);
      this.filtering.withCallback($ -> this.invVersionTracker.reset());
      behaviours.add(this.filtering);
      behaviours.add(new DirectBeltInputBehaviour(this).onlyInsertWhen(this::supportsDirectBeltInput).setInsertionHandler(this::handleDirectBeltInput));
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.FUNNEL});
   }

   private boolean supportsAmountOnFilter() {
      BlockState blockState = this.getBlockState();
      boolean beltFunnelsupportsAmount = false;
      if (blockState.getBlock() instanceof BeltFunnelBlock) {
         BeltFunnelBlock.Shape shape = (BeltFunnelBlock.Shape)blockState.getValue(BeltFunnelBlock.SHAPE);
         if (shape == BeltFunnelBlock.Shape.PUSHING) {
            beltFunnelsupportsAmount = true;
         } else {
            beltFunnelsupportsAmount = BeltHelper.getSegmentBE(this.level, this.worldPosition.below()) != null;
         }
      }

      boolean extractor = blockState.getBlock() instanceof FunnelBlock && (Boolean)blockState.getValue(FunnelBlock.EXTRACTING);
      return beltFunnelsupportsAmount || extractor;
   }

   private boolean supportsDirectBeltInput(Direction side) {
      BlockState blockState = this.getBlockState();
      if (blockState == null) {
         return false;
      } else if (!(blockState.getBlock() instanceof FunnelBlock)) {
         return false;
      } else {
         return blockState.getValue(FunnelBlock.EXTRACTING) ? false : FunnelBlock.getFunnelFacing(blockState) == Direction.UP;
      }
   }

   private boolean supportsFiltering() {
      BlockState blockState = this.getBlockState();
      return AllBlocks.BRASS_BELT_FUNNEL.has(blockState) || AllBlocks.BRASS_FUNNEL.has(blockState);
   }

   private ItemStack handleDirectBeltInput(TransportedItemStack stack, Direction side, boolean simulate) {
      ItemStack inserted = stack.stack;
      if (!this.filtering.test(inserted)) {
         return inserted;
      } else if (this.determineCurrentMode() == FunnelBlockEntity.Mode.PAUSED) {
         return inserted;
      } else {
         if (simulate) {
            this.invManipulation.simulate();
         }

         if (!simulate) {
            this.onTransfer(inserted);
         }

         return this.invManipulation.insert(inserted);
      }
   }

   public void flap(boolean inward) {
      if (!this.level.isClientSide && this.level instanceof ServerLevel serverLevel) {
         CatnipServices.NETWORK.sendToClientsTrackingChunk(serverLevel, new ChunkPos(this.worldPosition), new FunnelFlapPacket(this, inward));
      } else {
         this.flap.setValue(inward ? -1.0 : 1.0);
         AllSoundEvents.FUNNEL_FLAP.playAt(this.level, this.worldPosition, 1.0F, 1.0F, true);
      }
   }

   public boolean hasFlap() {
      BlockState blockState = this.getBlockState();
      return AbstractFunnelBlock.getFunnelFacing(blockState).getAxis().isHorizontal();
   }

   public float getFlapOffset() {
      BlockState blockState = this.getBlockState();
      if (!(blockState.getBlock() instanceof BeltFunnelBlock)) {
         return -0.0625F;
      } else {
         return switch ((BeltFunnelBlock.Shape)blockState.getValue(BeltFunnelBlock.SHAPE)) {
            case EXTENDED -> 0.5F;
            case PULLING, PUSHING -> -0.125F;
            default -> 0.0F;
         };
      }
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putInt("TransferCooldown", this.extractionCooldown);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.extractionCooldown = compound.getInt("TransferCooldown");
      if (clientPacket) {
         CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> VisualizationHelper.queueUpdate(this));
      }
   }

   public void clearContent() {
      this.filtering.setFilter(ItemStack.EMPTY);
   }

   public void onTransfer(ItemStack stack) {
      ((SmartObserverBlock)AllBlocks.SMART_OBSERVER.get()).onFunnelTransfer(this.level, this.worldPosition, stack);
      this.award(AllAdvancements.FUNNEL);
   }

   private LerpedFloat createChasingFlap() {
      return LerpedFloat.linear().startWithValue(0.25).chase(0.0, 0.05F, Chaser.EXP);
   }

   static enum Mode {
      INVALID,
      PAUSED,
      COLLECT,
      PUSHING_TO_BELT,
      TAKING_FROM_BELT,
      EXTRACT;
   }
}
