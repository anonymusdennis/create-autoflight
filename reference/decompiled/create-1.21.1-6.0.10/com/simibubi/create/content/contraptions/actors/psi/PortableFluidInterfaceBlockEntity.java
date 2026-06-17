package com.simibubi.create.content.contraptions.actors.psi;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class PortableFluidInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity {
   protected IFluidHandler capability = this.createEmptyHandler();

   public PortableFluidInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(FluidHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.PORTABLE_FLUID_INTERFACE.get(), (be, context) -> be.capability);
   }

   @Override
   public void startTransferringTo(Contraption contraption, float distance) {
      this.capability = new PortableFluidInterfaceBlockEntity.InterfaceFluidHandler(contraption.getStorage().getFluids());
      this.invalidateCapability();
      super.startTransferringTo(contraption, distance);
   }

   @Override
   protected void invalidateCapability() {
      this.invalidateCapabilities();
   }

   @Override
   protected void stopTransferring() {
      this.capability = this.createEmptyHandler();
      this.invalidateCapability();
      super.stopTransferring();
   }

   private IFluidHandler createEmptyHandler() {
      return new PortableFluidInterfaceBlockEntity.InterfaceFluidHandler(new FluidTank(0));
   }

   public class InterfaceFluidHandler implements IFluidHandler {
      private IFluidHandler wrapped;

      public InterfaceFluidHandler(IFluidHandler wrapped) {
         this.wrapped = wrapped;
      }

      public int getTanks() {
         return this.wrapped.getTanks();
      }

      public FluidStack getFluidInTank(int tank) {
         return this.wrapped.getFluidInTank(tank);
      }

      public int getTankCapacity(int tank) {
         return this.wrapped.getTankCapacity(tank);
      }

      public boolean isFluidValid(int tank, FluidStack stack) {
         return this.wrapped.isFluidValid(tank, stack);
      }

      public int fill(FluidStack resource, FluidAction action) {
         if (!PortableFluidInterfaceBlockEntity.this.isConnected()) {
            return 0;
         } else {
            int fill = this.wrapped.fill(resource, action);
            if (fill > 0 && action.execute()) {
               this.keepAlive();
            }

            return fill;
         }
      }

      public FluidStack drain(FluidStack resource, FluidAction action) {
         if (!PortableFluidInterfaceBlockEntity.this.canTransfer()) {
            return FluidStack.EMPTY;
         } else {
            FluidStack drain = this.wrapped.drain(resource, action);
            if (!drain.isEmpty() && action.execute()) {
               this.keepAlive();
            }

            return drain;
         }
      }

      public FluidStack drain(int maxDrain, FluidAction action) {
         if (!PortableFluidInterfaceBlockEntity.this.canTransfer()) {
            return FluidStack.EMPTY;
         } else {
            FluidStack drain = this.wrapped.drain(maxDrain, action);
            if (!drain.isEmpty() && action.execute()) {
               this.keepAlive();
            }

            return drain;
         }
      }

      public void keepAlive() {
         PortableFluidInterfaceBlockEntity.this.onContentTransferred();
      }
   }
}
