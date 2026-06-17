package com.simibubi.create.content.fluids;

import com.simibubi.create.AllFluids;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import com.simibubi.create.content.fluids.pipes.VanillaFluidTargets;
import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.mixin.accessor.FlowingFluidAccessor;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public class OpenEndedPipe extends FlowSource {
   private Level world;
   private BlockPos pos;
   private AABB aoe;
   private OpenEndedPipe.OpenEndFluidHandler fluidHandler;
   private BlockPos outputPos;
   private boolean wasPulling;
   private final ICapabilityProvider<IFluidHandler> fluidHandlerProvider = ICapabilityProvider.of(() -> this.fluidHandler);

   public OpenEndedPipe(BlockFace face) {
      super(face);
      this.fluidHandler = new OpenEndedPipe.OpenEndFluidHandler();
      this.outputPos = face.getConnectedPos();
      this.pos = face.getPos();
      this.aoe = new AABB(this.outputPos).expandTowards(0.0, -1.0, 0.0);
      if (face.getFace() == Direction.DOWN) {
         this.aoe = this.aoe.expandTowards(0.0, -1.0, 0.0);
      }
   }

   public Level getWorld() {
      return this.world;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockPos getOutputPos() {
      return this.outputPos;
   }

   public AABB getAOE() {
      return this.aoe;
   }

   @Override
   public void manageSource(Level world, BlockEntity networkBE) {
      this.world = world;
   }

   @Nullable
   @Override
   public ICapabilityProvider<IFluidHandler> provideHandler() {
      return this.fluidHandlerProvider;
   }

   @Override
   public boolean isEndpoint() {
      return true;
   }

   public CompoundTag serializeNBT(Provider registries) {
      CompoundTag compound = new CompoundTag();
      this.fluidHandler.writeToNBT(registries, compound);
      compound.putBoolean("Pulling", this.wasPulling);
      compound.put("Location", this.location.serializeNBT());
      return compound;
   }

   public static OpenEndedPipe fromNBT(CompoundTag compound, Provider registries, BlockPos blockEntityPos) {
      BlockFace fromNBT = BlockFace.fromNBT(compound.getCompound("Location"));
      OpenEndedPipe oep = new OpenEndedPipe(new BlockFace(blockEntityPos, fromNBT.getFace()));
      oep.fluidHandler.readFromNBT(registries, compound);
      oep.wasPulling = compound.getBoolean("Pulling");
      return oep;
   }

   private FluidStack removeFluidFromSpace(boolean simulate) {
      FluidStack empty = FluidStack.EMPTY;
      if (this.world == null) {
         return empty;
      } else if (!this.world.isLoaded(this.outputPos)) {
         return empty;
      } else {
         BlockState state = this.world.getBlockState(this.outputPos);
         FluidState fluidState = state.getFluidState();
         boolean waterlog = state.hasProperty(BlockStateProperties.WATERLOGGED);
         FluidStack drainBlock = VanillaFluidTargets.drainBlock(this.world, this.outputPos, state, simulate);
         if (!drainBlock.isEmpty()) {
            if (!simulate && state.hasProperty(BlockStateProperties.LEVEL_HONEY) && AllFluids.HONEY.is(drainBlock.getFluid())) {
               AdvancementBehaviour.tryAward(this.world, this.pos, AllAdvancements.HONEY_DRAIN);
            }

            return drainBlock;
         } else if (!waterlog && !state.canBeReplaced()) {
            return empty;
         } else if (!fluidState.isEmpty() && fluidState.isSource()) {
            FluidStack stack = new FluidStack(fluidState.getType(), 1000);
            if (simulate) {
               return stack;
            } else {
               if (FluidHelper.isWater(stack.getFluid())) {
                  AdvancementBehaviour.tryAward(this.world, this.pos, AllAdvancements.WATER_SUPPLY);
               }

               if (waterlog) {
                  this.world.setBlock(this.outputPos, (BlockState)state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
                  this.world.scheduleTick(this.outputPos, Fluids.WATER, 1);
               } else {
                  BlockState newState = (BlockState)fluidState.createLegacyBlock().setValue(LiquidBlock.LEVEL, 14);
                  FluidState newFluidState = newState.getFluidState();
                  if (newFluidState.getType() instanceof FlowingFluidAccessor flowing) {
                     FluidState potentiallyFilled = flowing.create$getNewLiquid(this.world, this.outputPos, newState);
                     if (potentiallyFilled.equals(fluidState)) {
                        return stack;
                     }
                  }

                  this.world.setBlock(this.outputPos, newState, 3);
               }

               return stack;
            }
         } else {
            return empty;
         }
      }
   }

   private boolean provideFluidToSpace(FluidStack fluid, boolean simulate) {
      if (this.world == null) {
         return false;
      } else if (!this.world.isLoaded(this.outputPos)) {
         return false;
      } else {
         BlockState state = this.world.getBlockState(this.outputPos);
         FluidState fluidState = state.getFluidState();
         boolean waterlog = state.hasProperty(BlockStateProperties.WATERLOGGED);
         if (!waterlog && !state.canBeReplaced()) {
            return false;
         } else if (fluid.isEmpty()) {
            return false;
         } else if (!(fluid.getFluid() instanceof FlowingFluid)) {
            return false;
         } else if (!FluidHelper.hasBlockState(fluid.getFluid())) {
            return true;
         } else if (!fluidState.isEmpty() && FluidHelper.convertToStill(fluidState.getType()) != fluid.getFluid()) {
            FluidReactions.handlePipeSpillCollision(this.world, this.outputPos, fluid.getFluid(), fluidState);
            return false;
         } else if (fluidState.isSource()) {
            return false;
         } else if (waterlog && fluid.getFluid() != Fluids.WATER) {
            return false;
         } else if (simulate) {
            return true;
         } else if (!(Boolean)AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get()) {
            return true;
         } else if (this.world.dimensionType().ultraWarm() && FluidHelper.isTag(fluid, FluidTags.WATER)) {
            int i = this.outputPos.getX();
            int j = this.outputPos.getY();
            int k = this.outputPos.getZ();
            this.world
               .playSound(
                  null,
                  (double)i,
                  (double)j,
                  (double)k,
                  SoundEvents.FIRE_EXTINGUISH,
                  SoundSource.BLOCKS,
                  0.5F,
                  2.6F + (this.world.random.nextFloat() - this.world.random.nextFloat()) * 0.8F
               );
            return true;
         } else if (waterlog) {
            this.world.setBlock(this.outputPos, (BlockState)state.setValue(BlockStateProperties.WATERLOGGED, true), 3);
            this.world.scheduleTick(this.outputPos, Fluids.WATER, 1);
            return true;
         } else {
            this.world.setBlock(this.outputPos, fluid.getFluid().defaultFluidState().createLegacyBlock(), 3);
            return true;
         }
      }
   }

   private class OpenEndFluidHandler extends FluidTank {
      public OpenEndFluidHandler() {
         super(1000);
      }

      public int fill(FluidStack resource, FluidAction action) {
         if (OpenEndedPipe.this.world == null) {
            return 0;
         } else if (!OpenEndedPipe.this.world.isLoaded(OpenEndedPipe.this.outputPos)) {
            return 0;
         } else if (resource.isEmpty()) {
            return 0;
         } else if (!OpenEndedPipe.this.provideFluidToSpace(resource, true)) {
            return 0;
         } else {
            FluidStack containedFluidStack = this.getFluid();
            boolean hasBlockState = FluidHelper.hasBlockState(containedFluidStack.getFluid());
            if (!containedFluidStack.isEmpty() && !FluidStack.isSameFluidSameComponents(containedFluidStack, resource)) {
               this.setFluid(FluidStack.EMPTY);
            }

            if (OpenEndedPipe.this.wasPulling) {
               OpenEndedPipe.this.wasPulling = false;
            }

            OpenPipeEffectHandler effectHandler = OpenPipeEffectHandler.REGISTRY.get(resource.getFluid());
            if (effectHandler != null && !hasBlockState) {
               resource = FluidHelper.copyStackWithAmount(resource, 1);
            }

            int fill = super.fill(resource, action);
            if (action.simulate()) {
               return fill;
            } else {
               if (effectHandler != null && !resource.isEmpty()) {
                  FluidStack exposed = hasBlockState ? resource.copy() : resource;
                  effectHandler.apply(OpenEndedPipe.this.world, OpenEndedPipe.this.aoe, exposed);
               }

               if ((this.getFluidAmount() == 1000 || !hasBlockState) && OpenEndedPipe.this.provideFluidToSpace(containedFluidStack, false)) {
                  this.setFluid(FluidStack.EMPTY);
               }

               return fill;
            }
         }
      }

      public FluidStack drain(FluidStack resource, FluidAction action) {
         return this.drainInner(resource.getAmount(), resource, action);
      }

      public FluidStack drain(int maxDrain, FluidAction action) {
         return this.drainInner(maxDrain, null, action);
      }

      private FluidStack drainInner(int amount, @Nullable FluidStack filter, FluidAction action) {
         FluidStack empty = FluidStack.EMPTY;
         boolean filterPresent = filter != null;
         if (OpenEndedPipe.this.world == null) {
            return empty;
         } else if (!OpenEndedPipe.this.world.isLoaded(OpenEndedPipe.this.outputPos)) {
            return empty;
         } else if (amount == 0) {
            return empty;
         } else {
            if (amount > 1000) {
               amount = 1000;
               if (filterPresent) {
                  filter = FluidHelper.copyStackWithAmount(filter, amount);
               }
            }

            if (!OpenEndedPipe.this.wasPulling) {
               OpenEndedPipe.this.wasPulling = true;
            }

            FluidStack drainedFromInternal = filterPresent ? super.drain(filter, action) : super.drain(amount, action);
            if (!drainedFromInternal.isEmpty()) {
               return drainedFromInternal;
            } else {
               FluidStack drainedFromWorld = OpenEndedPipe.this.removeFluidFromSpace(action.simulate());
               if (drainedFromWorld.isEmpty()) {
                  return FluidStack.EMPTY;
               } else if (filterPresent && !FluidStack.isSameFluidSameComponents(drainedFromWorld, filter)) {
                  return FluidStack.EMPTY;
               } else {
                  int remainder = drainedFromWorld.getAmount() - amount;
                  drainedFromWorld.setAmount(amount);
                  if (!action.simulate() && remainder > 0) {
                     if (!this.getFluid().isEmpty() && !FluidStack.isSameFluidSameComponents(this.getFluid(), drainedFromWorld)) {
                        this.setFluid(FluidStack.EMPTY);
                     }

                     super.fill(FluidHelper.copyStackWithAmount(drainedFromWorld, remainder), FluidAction.EXECUTE);
                  }

                  return drainedFromWorld;
               }
            }
         }
      }
   }
}
