package com.simibubi.create.content.processing.basin;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidFX;
import com.simibubi.create.content.fluids.particle.FluidParticleData;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.IntAttached;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasinBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, Clearable {
   private boolean areFluidsMoving;
   LerpedFloat ingredientRotationSpeed;
   LerpedFloat ingredientRotation;
   public BasinInventory inputInventory = new BasinInventory(9, this);
   public SmartFluidTankBehaviour inputTank;
   protected SmartInventory outputInventory;
   protected SmartFluidTankBehaviour outputTank;
   private FilteringBehaviour filtering;
   private boolean contentsChanged;
   private Couple<SmartInventory> invs;
   private Couple<SmartFluidTankBehaviour> tanks;
   protected IItemHandlerModifiable itemCapability;
   protected IFluidHandler fluidCapability;
   List<Direction> disabledSpoutputs;
   Direction preferredSpoutput;
   protected List<ItemStack> spoutputBuffer;
   protected List<FluidStack> spoutputFluidBuffer;
   int recipeBackupCheck;
   public static final int OUTPUT_ANIMATION_TIME = 10;
   List<IntAttached<ItemStack>> visualizedOutputItems;
   List<IntAttached<FluidStack>> visualizedOutputFluids;
   @Nullable
   private BlazeBurnerBlock.HeatLevel cachedHeatLevel;

   public BasinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.inputInventory.whenContentsChanged($ -> this.contentsChanged = true);
      this.outputInventory = new BasinInventory(9, this).forbidInsertion().withMaxStackSize(64);
      this.areFluidsMoving = false;
      this.itemCapability = new CombinedInvWrapper(new IItemHandlerModifiable[]{this.inputInventory, this.outputInventory});
      this.contentsChanged = true;
      this.ingredientRotation = LerpedFloat.angular().startWithValue(0.0);
      this.ingredientRotationSpeed = LerpedFloat.linear().startWithValue(0.0);
      this.invs = Couple.create(this.inputInventory, this.outputInventory);
      this.tanks = Couple.create(this.inputTank, this.outputTank);
      this.visualizedOutputItems = Collections.synchronizedList(new ArrayList<>());
      this.visualizedOutputFluids = Collections.synchronizedList(new ArrayList<>());
      this.disabledSpoutputs = new ArrayList<>();
      this.preferredSpoutput = null;
      this.spoutputBuffer = new ArrayList<>();
      this.spoutputFluidBuffer = new ArrayList<>();
      this.recipeBackupCheck = 20;
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.BASIN.get(), (be, context) -> be.itemCapability);
      event.registerBlockEntity(FluidHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.BASIN.get(), (be, context) -> be.fluidCapability);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(new DirectBeltInputBehaviour(this));
      this.filtering = new FilteringBehaviour(this, new BasinBlockEntity.BasinValueBox()).withCallback(newFilter -> this.contentsChanged = true).forRecipes();
      behaviours.add(this.filtering);
      this.inputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 2, 1000, true).whenFluidUpdates(() -> this.contentsChanged = true);
      this.outputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 2, 1000, true)
         .whenFluidUpdates(() -> this.contentsChanged = true)
         .forbidInsertion();
      behaviours.add(this.inputTank);
      behaviours.add(this.outputTank);
      this.fluidCapability = new CombinedTankWrapper(this.outputTank.getCapability(), this.inputTank.getCapability());
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.inputInventory.deserializeNBT(registries, compound.getCompound("InputItems"));
      this.outputInventory.deserializeNBT(registries, compound.getCompound("OutputItems"));
      this.preferredSpoutput = null;
      if (compound.contains("PreferredSpoutput")) {
         this.preferredSpoutput = (Direction)NBTHelper.readEnum(compound, "PreferredSpoutput", Direction.class);
      }

      this.disabledSpoutputs.clear();
      ListTag disabledList = compound.getList("DisabledSpoutput", 8);
      disabledList.forEach(d -> this.disabledSpoutputs.add(Direction.valueOf(((StringTag)d).getAsString())));
      this.spoutputBuffer = NBTHelper.readItemList(compound.getList("Overflow", 10), registries);
      this.spoutputFluidBuffer = NBTHelper.readCompoundList(compound.getList("FluidOverflow", 10), tag -> FluidStack.parseOptional(registries, tag));
      if (clientPacket) {
         NBTHelper.iterateCompoundList(
            compound.getList("VisualizedItems", 10), c -> this.visualizedOutputItems.add(IntAttached.with(10, ItemStack.parseOptional(registries, c)))
         );
         NBTHelper.iterateCompoundList(
            compound.getList("VisualizedFluids", 10), c -> this.visualizedOutputFluids.add(IntAttached.with(10, FluidStack.parseOptional(registries, c)))
         );
      }
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.put("InputItems", this.inputInventory.serializeNBT(registries));
      compound.put("OutputItems", this.outputInventory.serializeNBT(registries));
      if (this.preferredSpoutput != null) {
         NBTHelper.writeEnum(compound, "PreferredSpoutput", this.preferredSpoutput);
      }

      ListTag disabledList = new ListTag();
      this.disabledSpoutputs.forEach(d -> disabledList.add(StringTag.valueOf(d.name())));
      compound.put("DisabledSpoutput", disabledList);
      compound.put("Overflow", NBTHelper.writeItemList(this.spoutputBuffer, registries));
      compound.put("FluidOverflow", NBTHelper.writeCompoundList(this.spoutputFluidBuffer, fs -> (CompoundTag)fs.saveOptional(registries)));
      if (clientPacket) {
         compound.put(
            "VisualizedItems", NBTHelper.writeCompoundList(this.visualizedOutputItems, ia -> (CompoundTag)((ItemStack)ia.getValue()).saveOptional(registries))
         );
         compound.put(
            "VisualizedFluids",
            NBTHelper.writeCompoundList(this.visualizedOutputFluids, ia -> (CompoundTag)((FluidStack)ia.getValue()).saveOptional(registries))
         );
         this.visualizedOutputItems.clear();
         this.visualizedOutputFluids.clear();
      }
   }

   public void clearContent() {
      this.spoutputBuffer.clear();
      this.inputInventory.clearContent();
      this.outputInventory.clearContent();
      this.filtering.setFilter(ItemStack.EMPTY);
   }

   @Override
   public void destroy() {
      super.destroy();
      ItemHelper.dropContents(this.level, this.worldPosition, this.inputInventory);
      ItemHelper.dropContents(this.level, this.worldPosition, this.outputInventory);
      this.spoutputBuffer.forEach(is -> Block.popResource(this.level, this.worldPosition, is));
   }

   @Override
   public void remove() {
      super.remove();
      this.onEmptied();
   }

   public void onEmptied() {
      this.getOperator().ifPresent(be -> be.basinRemoved = true);
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.invalidateCapabilities();
   }

   @Override
   public void notifyUpdate() {
      super.notifyUpdate();
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      if (!this.level.isClientSide) {
         this.updateSpoutput();
         if (this.recipeBackupCheck-- <= 0) {
            this.recipeBackupCheck = 20;
            if (!this.isEmpty()) {
               this.notifyChangeOfContents();
            }
         }
      } else if (!(this.level.getBlockEntity(this.worldPosition.above(2)) instanceof MechanicalMixerBlockEntity mixer)) {
         this.setAreFluidsMoving(false);
      } else {
         this.setAreFluidsMoving(mixer.running && mixer.runningTicks <= 20);
      }
   }

   public boolean isEmpty() {
      return this.inputInventory.isEmpty() && this.outputInventory.isEmpty() && this.inputTank.isEmpty() && this.outputTank.isEmpty();
   }

   public void onWrenched(Direction face) {
      BlockState blockState = this.getBlockState();
      Direction currentFacing = (Direction)blockState.getValue(BasinBlock.FACING);
      this.disabledSpoutputs.remove(face);
      if (currentFacing == face) {
         if (this.preferredSpoutput == face) {
            this.preferredSpoutput = null;
         }

         this.disabledSpoutputs.add(face);
      } else {
         this.preferredSpoutput = face;
      }

      this.updateSpoutput();
   }

   private void updateSpoutput() {
      BlockState blockState = this.getBlockState();
      Direction currentFacing = (Direction)blockState.getValue(BasinBlock.FACING);
      Direction newFacing = Direction.DOWN;

      for (Direction test : Iterate.horizontalDirections) {
         boolean canOutputTo = BasinBlock.canOutputTo(this.level, this.worldPosition, test);
         if (canOutputTo && !this.disabledSpoutputs.contains(test)) {
            newFacing = test;
         }
      }

      if (this.preferredSpoutput != null
         && BasinBlock.canOutputTo(this.level, this.worldPosition, this.preferredSpoutput)
         && this.preferredSpoutput != Direction.UP) {
         newFacing = this.preferredSpoutput;
      }

      if (newFacing != currentFacing) {
         this.level.setBlockAndUpdate(this.worldPosition, (BlockState)blockState.setValue(BasinBlock.FACING, newFacing));
         if (!newFacing.getAxis().isVertical()) {
            for (int slot = 0; slot < this.outputInventory.getSlots(); slot++) {
               ItemStack extractItem = this.outputInventory.extractItem(slot, 64, true);
               if (!extractItem.isEmpty() && this.acceptOutputs(ImmutableList.of(extractItem), Collections.emptyList(), true)) {
                  this.acceptOutputs(ImmutableList.of(this.outputInventory.extractItem(slot, 64, false)), Collections.emptyList(), false);
               }
            }

            IFluidHandler handler = this.outputTank.getCapability();

            for (int slotx = 0; slotx < handler.getTanks(); slotx++) {
               FluidStack fs = handler.getFluidInTank(slotx).copy();
               if (!fs.isEmpty() && this.acceptOutputs(Collections.emptyList(), ImmutableList.of(fs), true)) {
                  handler.drain(fs, FluidAction.EXECUTE);
                  this.acceptOutputs(Collections.emptyList(), ImmutableList.of(fs), false);
               }
            }

            this.notifyChangeOfContents();
            this.notifyUpdate();
         }
      }
   }

   @Override
   public void tick() {
      this.cachedHeatLevel = null;
      super.tick();
      if (this.level.isClientSide) {
         this.createFluidParticles();
         this.tickVisualizedOutputs();
         this.ingredientRotationSpeed.tickChaser();
         this.ingredientRotation.setValue((double)(this.ingredientRotation.getValue() + this.ingredientRotationSpeed.getValue()));
      }

      if ((!this.spoutputBuffer.isEmpty() || !this.spoutputFluidBuffer.isEmpty()) && !this.level.isClientSide) {
         this.tryClearingSpoutputOverflow();
      }

      if (this.contentsChanged) {
         this.contentsChanged = false;
         this.getOperator().ifPresent(bex -> bex.basinChecker.scheduleUpdate());

         for (Direction offset : Iterate.horizontalDirections) {
            BlockPos toUpdate = this.worldPosition.above().relative(offset);
            BlockState stateToUpdate = this.level.getBlockState(toUpdate);
            if (stateToUpdate.getBlock() instanceof BasinBlock && stateToUpdate.getValue(BasinBlock.FACING) == offset.getOpposite()) {
               BlockEntity be = this.level.getBlockEntity(toUpdate);
               if (be instanceof BasinBlockEntity) {
                  ((BasinBlockEntity)be).contentsChanged = true;
               }
            }
         }
      }
   }

   private void tryClearingSpoutputOverflow() {
      BlockState blockState = this.getBlockState();
      if (blockState.getBlock() instanceof BasinBlock) {
         Direction direction = (Direction)blockState.getValue(BasinBlock.FACING);
         BlockEntity be = this.level.getBlockEntity(this.worldPosition.below().relative(direction));
         FilteringBehaviour filter = null;
         InvManipulationBehaviour inserter = null;
         if (be != null) {
            filter = BlockEntityBehaviour.get(this.level, be.getBlockPos(), FilteringBehaviour.TYPE);
            inserter = BlockEntityBehaviour.get(this.level, be.getBlockPos(), InvManipulationBehaviour.TYPE);
         }

         if (filter != null && filter.isRecipeFilter()) {
            filter = null;
         }

         IItemHandler targetInv = be == null
            ? null
            : (IItemHandler)Optional.ofNullable((IItemHandler)this.level.getCapability(ItemHandler.BLOCK, be.getBlockPos(), direction.getOpposite()))
               .orElse(inserter == null ? null : inserter.getInventory());
         IFluidHandler targetTank = be == null ? null : (IFluidHandler)this.level.getCapability(FluidHandler.BLOCK, be.getBlockPos(), direction.getOpposite());
         boolean update = false;
         Iterator<ItemStack> iterator = this.spoutputBuffer.iterator();

         while (iterator.hasNext()) {
            ItemStack itemStack = iterator.next();
            if (direction == Direction.DOWN) {
               Block.popResource(this.level, this.worldPosition, itemStack);
               iterator.remove();
               update = true;
            } else {
               if (targetInv == null) {
                  break;
               }

               ItemStack remainder = ItemHandlerHelper.insertItemStacked(targetInv, itemStack, true);
               if (remainder.getCount() != itemStack.getCount() && (filter == null || filter.test(itemStack))) {
                  if (this.visualizedOutputItems.size() < 3) {
                     this.visualizedOutputItems.add(IntAttached.withZero(itemStack));
                  }

                  update = true;
                  remainder = ItemHandlerHelper.insertItemStacked(targetInv, itemStack.copy(), false);
                  if (remainder.isEmpty()) {
                     iterator.remove();
                  } else {
                     itemStack.setCount(remainder.getCount());
                  }
               }
            }
         }

         iterator = this.spoutputFluidBuffer.iterator();

         while (iterator.hasNext()) {
            FluidStack fluidStack = (FluidStack)iterator.next();
            if (direction == Direction.DOWN) {
               iterator.remove();
               update = true;
            } else {
               if (targetTank == null) {
                  break;
               }

               for (boolean simulate : Iterate.trueAndFalse) {
                  FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
                  int fill = targetTank instanceof SmartFluidTankBehaviour.InternalFluidHandler
                     ? ((SmartFluidTankBehaviour.InternalFluidHandler)targetTank).forceFill(fluidStack.copy(), action)
                     : targetTank.fill(fluidStack.copy(), action);
                  if (fill != fluidStack.getAmount()) {
                     break;
                  }

                  if (!simulate) {
                     update = true;
                     iterator.remove();
                     if (this.visualizedOutputFluids.size() < 3) {
                        this.visualizedOutputFluids.add(IntAttached.withZero(fluidStack));
                     }
                  }
               }
            }
         }

         if (update) {
            this.notifyChangeOfContents();
            this.sendData();
         }
      }
   }

   public float getTotalFluidUnits(float partialTicks) {
      int renderedFluids = 0;
      float totalUnits = 0.0F;

      for (SmartFluidTankBehaviour behaviour : this.getTanks()) {
         if (behaviour != null) {
            for (SmartFluidTankBehaviour.TankSegment tankSegment : behaviour.getTanks()) {
               if (!tankSegment.getRenderedFluid().isEmpty()) {
                  float units = tankSegment.getTotalUnits(partialTicks);
                  if (!(units < 1.0F)) {
                     totalUnits += units;
                     renderedFluids++;
                  }
               }
            }
         }
      }

      if (renderedFluids == 0) {
         return 0.0F;
      } else {
         return totalUnits < 1.0F ? 0.0F : totalUnits;
      }
   }

   private Optional<BasinOperatingBlockEntity> getOperator() {
      if (this.level == null) {
         return Optional.empty();
      } else {
         BlockEntity be = this.level.getBlockEntity(this.worldPosition.above(2));
         return be instanceof BasinOperatingBlockEntity ? Optional.of((BasinOperatingBlockEntity)be) : Optional.empty();
      }
   }

   public FilteringBehaviour getFilter() {
      return this.filtering;
   }

   public void notifyChangeOfContents() {
      this.contentsChanged = true;
   }

   public SmartInventory getInputInventory() {
      return this.inputInventory;
   }

   public SmartInventory getOutputInventory() {
      return this.outputInventory;
   }

   public boolean canContinueProcessing() {
      return this.spoutputBuffer.isEmpty() && this.spoutputFluidBuffer.isEmpty();
   }

   public boolean acceptOutputs(List<ItemStack> outputItems, List<FluidStack> outputFluids, boolean simulate) {
      this.outputInventory.allowInsertion();
      this.outputTank.allowInsertion();
      boolean acceptOutputsInner = this.acceptOutputsInner(outputItems, outputFluids, simulate);
      this.outputInventory.forbidInsertion();
      this.outputTank.forbidInsertion();
      return acceptOutputsInner;
   }

   private boolean acceptOutputsInner(List<ItemStack> outputItems, List<FluidStack> outputFluids, boolean simulate) {
      BlockState blockState = this.getBlockState();
      if (!(blockState.getBlock() instanceof BasinBlock)) {
         return false;
      } else {
         Direction direction = (Direction)blockState.getValue(BasinBlock.FACING);
         if (direction == Direction.DOWN) {
            IItemHandler targetInv = this.outputInventory;
            IFluidHandler targetTank = this.outputTank.getCapability();
            if (targetInv == null && !outputItems.isEmpty()) {
               return false;
            } else if (!this.acceptItemOutputsIntoBasin(outputItems, simulate, targetInv)) {
               return false;
            } else if (outputFluids.isEmpty()) {
               return true;
            } else {
               return targetTank == null ? false : this.acceptFluidOutputsIntoBasin(outputFluids, simulate, targetTank);
            }
         } else {
            BlockEntity be = this.level.getBlockEntity(this.worldPosition.below().relative(direction));
            InvManipulationBehaviour inserter = be == null ? null : BlockEntityBehaviour.get(this.level, be.getBlockPos(), InvManipulationBehaviour.TYPE);
            IItemHandler targetInv = be == null
               ? null
               : (IItemHandler)Optional.ofNullable((IItemHandler)this.level.getCapability(ItemHandler.BLOCK, be.getBlockPos(), direction.getOpposite()))
                  .orElse(inserter == null ? null : inserter.getInventory());
            IFluidHandler targetTank = be == null
               ? null
               : (IFluidHandler)this.level.getCapability(FluidHandler.BLOCK, be.getBlockPos(), direction.getOpposite());
            boolean externalTankNotPresent = targetTank == null;
            if (!outputItems.isEmpty() && targetInv == null) {
               return false;
            } else {
               if (!outputFluids.isEmpty() && externalTankNotPresent) {
                  targetTank = this.outputTank.getCapability();
                  if (targetTank == null) {
                     return false;
                  }

                  if (!this.acceptFluidOutputsIntoBasin(outputFluids, simulate, targetTank)) {
                     return false;
                  }
               }

               if (simulate) {
                  return true;
               } else {
                  for (ItemStack itemStack : outputItems) {
                     if (!itemStack.isEmpty()) {
                        this.spoutputBuffer.add(itemStack.copy());
                     }
                  }

                  if (!externalTankNotPresent) {
                     for (FluidStack fluidStack : outputFluids) {
                        this.spoutputFluidBuffer.add(fluidStack.copy());
                     }
                  }

                  return true;
               }
            }
         }
      }
   }

   private boolean acceptFluidOutputsIntoBasin(List<FluidStack> outputFluids, boolean simulate, IFluidHandler targetTank) {
      for (FluidStack fluidStack : outputFluids) {
         FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
         int fill = targetTank instanceof SmartFluidTankBehaviour.InternalFluidHandler
            ? ((SmartFluidTankBehaviour.InternalFluidHandler)targetTank).forceFill(fluidStack.copy(), action)
            : targetTank.fill(fluidStack.copy(), action);
         if (fill != fluidStack.getAmount()) {
            return false;
         }
      }

      return true;
   }

   private boolean acceptItemOutputsIntoBasin(List<ItemStack> outputItems, boolean simulate, IItemHandler targetInv) {
      for (ItemStack itemStack : outputItems) {
         if (!ItemHandlerHelper.insertItemStacked(targetInv, itemStack.copy(), simulate).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public void readOnlyItems(CompoundTag compound, Provider registries) {
      this.inputInventory.deserializeNBT(registries, compound.getCompound("InputItems"));
      this.outputInventory.deserializeNBT(registries, compound.getCompound("OutputItems"));
   }

   public static BlazeBurnerBlock.HeatLevel getHeatLevelOf(BlockState state) {
      if (state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)) {
         return (BlazeBurnerBlock.HeatLevel)state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
      } else {
         return AllTags.AllBlockTags.PASSIVE_BOILER_HEATERS.matches(state) && BlockHelper.isNotUnheated(state)
            ? BlazeBurnerBlock.HeatLevel.SMOULDERING
            : BlazeBurnerBlock.HeatLevel.NONE;
      }
   }

   public Couple<SmartFluidTankBehaviour> getTanks() {
      return this.tanks;
   }

   public Couple<SmartInventory> getInvs() {
      return this.invs;
   }

   private void tickVisualizedOutputs() {
      this.visualizedOutputFluids.forEach(IntAttached::decrement);
      this.visualizedOutputItems.forEach(IntAttached::decrement);
      this.visualizedOutputFluids.removeIf(IntAttached::isOrBelowZero);
      this.visualizedOutputItems.removeIf(IntAttached::isOrBelowZero);
   }

   private void createFluidParticles() {
      RandomSource r = this.level.random;
      if (!this.visualizedOutputFluids.isEmpty()) {
         this.createOutputFluidParticles(r);
      }

      if (this.areFluidsMoving || !(r.nextFloat() > 0.125F)) {
         int segments = 0;

         for (SmartFluidTankBehaviour behaviour : this.getTanks()) {
            if (behaviour != null) {
               for (SmartFluidTankBehaviour.TankSegment tankSegment : behaviour.getTanks()) {
                  if (!tankSegment.isEmpty(0.0F)) {
                     segments++;
                  }
               }
            }
         }

         if (segments >= 2) {
            float totalUnits = this.getTotalFluidUnits(0.0F);
            if (totalUnits != 0.0F) {
               float fluidLevel = Mth.clamp(totalUnits / 2000.0F, 0.0F, 1.0F);
               float rim = 0.125F;
               float space = 0.75F;
               float surface = (float)this.worldPosition.getY() + rim + space * fluidLevel + 0.03125F;
               if (this.areFluidsMoving) {
                  this.createMovingFluidParticles(surface, segments);
               } else {
                  for (SmartFluidTankBehaviour behaviourx : this.getTanks()) {
                     if (behaviourx != null) {
                        for (SmartFluidTankBehaviour.TankSegment tankSegmentx : behaviourx.getTanks()) {
                           if (!tankSegmentx.isEmpty(0.0F)) {
                              float x = (float)this.worldPosition.getX() + rim + space * r.nextFloat();
                              float z = (float)this.worldPosition.getZ() + rim + space * r.nextFloat();
                              this.level
                                 .addAlwaysVisibleParticle(
                                    new FluidParticleData(AllParticleTypes.BASIN_FLUID.get(), tankSegmentx.getRenderedFluid()),
                                    (double)x,
                                    (double)surface,
                                    (double)z,
                                    0.0,
                                    0.0,
                                    0.0
                                 );
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void createOutputFluidParticles(RandomSource r) {
      BlockState blockState = this.getBlockState();
      if (blockState.getBlock() instanceof BasinBlock) {
         Direction direction = (Direction)blockState.getValue(BasinBlock.FACING);
         if (direction != Direction.DOWN) {
            Vec3 directionVec = Vec3.atLowerCornerOf(direction.getNormal());
            Vec3 outVec = VecHelper.getCenterOf(this.worldPosition).add(directionVec.scale(0.65).subtract(0.0, 0.25, 0.0));
            Vec3 outMotion = directionVec.scale(0.0625).add(0.0, -0.0625, 0.0);

            for (int i = 0; i < 2; i++) {
               this.visualizedOutputFluids.forEach(ia -> {
                  FluidStack fluidStack = (FluidStack)ia.getValue();
                  ParticleOptions fluidParticle = FluidFX.getFluidParticle(fluidStack);
                  Vec3 m = VecHelper.offsetRandomly(outMotion, r, 0.0625F);
                  this.level.addAlwaysVisibleParticle(fluidParticle, outVec.x, outVec.y, outVec.z, m.x, m.y, m.z);
               });
            }
         }
      }
   }

   private void createMovingFluidParticles(float surface, int segments) {
      Vec3 pointer = new Vec3(1.0, 0.0, 0.0).scale(0.0625);
      float interval = 360.0F / (float)segments;
      Vec3 centerOf = VecHelper.getCenterOf(this.worldPosition);
      float intervalOffset = (float)(AnimationTickHolder.getTicks() * 18 % 360);
      int currentSegment = 0;

      for (SmartFluidTankBehaviour behaviour : this.getTanks()) {
         if (behaviour != null) {
            for (SmartFluidTankBehaviour.TankSegment tankSegment : behaviour.getTanks()) {
               if (!tankSegment.isEmpty(0.0F)) {
                  float angle = interval * (float)(1 + currentSegment) + intervalOffset;
                  Vec3 vec = centerOf.add(VecHelper.rotate(pointer, (double)angle, Axis.Y));
                  this.level
                     .addAlwaysVisibleParticle(
                        new FluidParticleData(AllParticleTypes.BASIN_FLUID.get(), tankSegment.getRenderedFluid()),
                        vec.x(),
                        (double)surface,
                        vec.z(),
                        1.0,
                        0.0,
                        0.0
                     );
                  currentSegment++;
               }
            }
         }
      }
   }

   public boolean areFluidsMoving() {
      return this.areFluidsMoving;
   }

   public boolean setAreFluidsMoving(boolean areFluidsMoving) {
      this.areFluidsMoving = areFluidsMoving;
      this.ingredientRotationSpeed.chase(areFluidsMoving ? 20.0 : 0.0, 0.1F, Chaser.EXP);
      return areFluidsMoving;
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      CreateLang.translate("gui.goggles.basin_contents").forGoggles(tooltip);
      if (this.itemCapability == null) {
         this.itemCapability = new ItemStackHandler();
      }

      if (this.fluidCapability == null) {
         this.fluidCapability = new FluidTank(0);
      }

      boolean isEmpty = true;

      for (int i = 0; i < this.itemCapability.getSlots(); i++) {
         ItemStack stackInSlot = this.itemCapability.getStackInSlot(i);
         if (!stackInSlot.isEmpty()) {
            CreateLang.text("")
               .add(Component.translatable(stackInSlot.getDescriptionId()).withStyle(ChatFormatting.GRAY))
               .add(CreateLang.text(" x" + stackInSlot.getCount()).style(ChatFormatting.GREEN))
               .forGoggles(tooltip, 1);
            isEmpty = false;
         }
      }

      LangBuilder mb = CreateLang.translate("generic.unit.millibuckets");

      for (int ix = 0; ix < this.fluidCapability.getTanks(); ix++) {
         FluidStack fluidStack = this.fluidCapability.getFluidInTank(ix);
         if (!fluidStack.isEmpty()) {
            CreateLang.text("")
               .add(
                  CreateLang.fluidName(fluidStack)
                     .add(CreateLang.text(" "))
                     .style(ChatFormatting.GRAY)
                     .add(CreateLang.number((double)fluidStack.getAmount()).add(mb).style(ChatFormatting.BLUE))
               )
               .forGoggles(tooltip, 1);
            isEmpty = false;
         }
      }

      if (isEmpty) {
         tooltip.remove(0);
      }

      return true;
   }

   @NotNull
   BlazeBurnerBlock.HeatLevel getHeatLevel() {
      if (this.cachedHeatLevel == null) {
         if (this.level == null) {
            return BlazeBurnerBlock.HeatLevel.NONE;
         }

         this.cachedHeatLevel = getHeatLevelOf(this.level.getBlockState(this.getBlockPos().below(1)));
      }

      return this.cachedHeatLevel;
   }

   static class BasinValueBox extends ValueBoxTransform.Sided {
      @Override
      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 12.0, 16.05);
      }

      @Override
      protected boolean isSideActive(BlockState state, Direction direction) {
         return direction.getAxis().isHorizontal();
      }
   }
}
