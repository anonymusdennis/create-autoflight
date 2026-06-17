package com.simibubi.create.content.fluids;

import com.simibubi.create.foundation.ICapabilityProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.lang.ref.WeakReference;
import java.util.function.Predicate;
import net.createmod.catnip.math.BlockFace;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

public abstract class FlowSource {
   private static final ICapabilityProvider<IFluidHandler> EMPTY = null;
   BlockFace location;

   public FlowSource(BlockFace location) {
      this.location = location;
   }

   public FluidStack provideFluid(Predicate<FluidStack> extractionPredicate) {
      ICapabilityProvider<IFluidHandler> tankCache = this.provideHandler();
      if (tankCache == null) {
         return FluidStack.EMPTY;
      } else {
         IFluidHandler tank = tankCache.getCapability();
         if (tank == null) {
            return FluidStack.EMPTY;
         } else {
            FluidStack immediateFluid = tank.drain(1, FluidAction.SIMULATE);
            if (extractionPredicate.test(immediateFluid)) {
               return immediateFluid;
            } else {
               for (int i = 0; i < tank.getTanks(); i++) {
                  FluidStack contained = tank.getFluidInTank(i);
                  if (!contained.isEmpty() && extractionPredicate.test(contained)) {
                     FluidStack toExtract = contained.copy();
                     toExtract.setAmount(1);
                     return tank.drain(toExtract, FluidAction.SIMULATE);
                  }
               }

               return FluidStack.EMPTY;
            }
         }
      }
   }

   public void keepAlive() {
   }

   public abstract boolean isEndpoint();

   public void manageSource(Level world, BlockEntity networkBE) {
   }

   public void whileFlowPresent(Level world, boolean pulling) {
   }

   @Nullable
   public ICapabilityProvider<IFluidHandler> provideHandler() {
      return EMPTY;
   }

   public static class Blocked extends FlowSource {
      public Blocked(BlockFace location) {
         super(location);
      }

      @Override
      public boolean isEndpoint() {
         return false;
      }
   }

   public static class FluidHandler extends FlowSource {
      @Nullable
      ICapabilityProvider<IFluidHandler> fluidHandlerCache = FlowSource.EMPTY;

      public FluidHandler(BlockFace location) {
         super(location);
      }

      @Override
      public void manageSource(Level level, BlockEntity networkBE) {
         if (this.fluidHandlerCache == null) {
            BlockEntity blockEntity = level.getBlockEntity(this.location.getConnectedPos());
            if (blockEntity != null) {
               if (level instanceof ServerLevel serverLevel) {
                  this.fluidHandlerCache = ICapabilityProvider.of(
                     invalidate -> BlockCapabilityCache.create(
                           net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                           serverLevel,
                           blockEntity.getBlockPos(),
                           this.location.getOppositeFace(),
                           () -> !networkBE.isRemoved(),
                           () -> {
                              this.fluidHandlerCache = FlowSource.EMPTY;
                              invalidate.run();
                           }
                        )
                  );
               } else if (level instanceof PonderLevel) {
                  this.fluidHandlerCache = ICapabilityProvider.of(
                     () -> (IFluidHandler)level.getCapability(
                           net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, blockEntity.getBlockPos(), this.location.getOppositeFace()
                        )
                  );
               }
            }
         }
      }

      @Nullable
      @Override
      public ICapabilityProvider<IFluidHandler> provideHandler() {
         return this.fluidHandlerCache;
      }

      @Override
      public boolean isEndpoint() {
         return true;
      }
   }

   public static class OtherPipe extends FlowSource {
      WeakReference<FluidTransportBehaviour> cached;

      public OtherPipe(BlockFace location) {
         super(location);
      }

      @Override
      public void manageSource(Level world, BlockEntity networkBE) {
         if (this.cached == null || this.cached.get() == null || this.cached.get().blockEntity.isRemoved()) {
            this.cached = null;
            FluidTransportBehaviour fluidTransportBehaviour = BlockEntityBehaviour.get(world, this.location.getConnectedPos(), FluidTransportBehaviour.TYPE);
            if (fluidTransportBehaviour != null) {
               this.cached = new WeakReference<>(fluidTransportBehaviour);
            }
         }
      }

      @Override
      public FluidStack provideFluid(Predicate<FluidStack> extractionPredicate) {
         if (this.cached != null && this.cached.get() != null) {
            FluidTransportBehaviour behaviour = this.cached.get();
            FluidStack providedOutwardFluid = behaviour.getProvidedOutwardFluid(this.location.getOppositeFace());
            return extractionPredicate.test(providedOutwardFluid) ? providedOutwardFluid : FluidStack.EMPTY;
         } else {
            return FluidStack.EMPTY;
         }
      }

      @Override
      public boolean isEndpoint() {
         return false;
      }
   }
}
