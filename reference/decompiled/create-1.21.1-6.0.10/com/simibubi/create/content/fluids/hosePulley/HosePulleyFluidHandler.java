package com.simibubi.create.content.fluids.hosePulley;

import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

public class HosePulleyFluidHandler implements IFluidHandler {
   private SmartFluidTank internalTank;
   private FluidFillingBehaviour filler;
   private FluidDrainingBehaviour drainer;
   private Supplier<BlockPos> rootPosGetter;
   private Supplier<Boolean> predicate;

   public int fill(FluidStack resource, FluidAction action) {
      if (!this.internalTank.isEmpty() && !FluidStack.isSameFluidSameComponents(resource, this.internalTank.getFluid())) {
         return 0;
      } else if (!resource.isEmpty() && FluidHelper.hasBlockState(resource.getFluid())) {
         int diff = resource.getAmount();
         int totalAmountAfterFill = diff + this.internalTank.getFluidAmount();
         FluidStack remaining = resource.copy();
         boolean deposited = false;
         if (this.predicate.get() && totalAmountAfterFill >= 1000 && this.filler.tryDeposit(resource.getFluid(), this.rootPosGetter.get(), action.simulate())) {
            this.drainer.counterpartActed();
            remaining.shrink(1000);
            diff -= 1000;
            deposited = true;
         }

         if (action.simulate()) {
            return diff <= 0 ? resource.getAmount() : this.internalTank.fill(remaining, action);
         } else if (diff <= 0) {
            this.internalTank.drain(-diff, FluidAction.EXECUTE);
            return resource.getAmount();
         } else {
            return this.internalTank.fill(remaining, action) + (deposited ? 1000 : 0);
         }
      } else {
         return 0;
      }
   }

   public FluidStack getFluidInTank(int tank) {
      return this.internalTank.isEmpty() ? this.drainer.getDrainableFluid(this.rootPosGetter.get()) : this.internalTank.getFluidInTank(tank);
   }

   public FluidStack drain(FluidStack resource, FluidAction action) {
      return this.drainInternal(resource.getAmount(), resource, action);
   }

   public FluidStack drain(int maxDrain, FluidAction action) {
      return this.drainInternal(maxDrain, null, action);
   }

   private FluidStack drainInternal(int maxDrain, @Nullable FluidStack resource, FluidAction action) {
      if (resource != null && !this.internalTank.isEmpty() && !FluidStack.isSameFluidSameComponents(resource, this.internalTank.getFluid())) {
         return FluidStack.EMPTY;
      } else if (this.internalTank.getFluidAmount() >= 1000) {
         return this.internalTank.drain(maxDrain, action);
      } else {
         BlockPos pos = this.rootPosGetter.get();
         FluidStack returned = this.drainer.getDrainableFluid(pos);
         if (this.predicate.get() && this.drainer.pullNext(pos, action.simulate())) {
            this.filler.counterpartActed();
            FluidStack leftover = returned.copy();
            int available = 1000 + this.internalTank.getFluidAmount();
            if ((this.internalTank.isEmpty() || FluidStack.isSameFluidSameComponents(this.internalTank.getFluid(), returned)) && !returned.isEmpty()) {
               if (resource != null && !FluidStack.isSameFluidSameComponents(returned, resource)) {
                  return FluidStack.EMPTY;
               } else {
                  int drained = Math.min(maxDrain, available);
                  returned.setAmount(drained);
                  leftover.setAmount(available - drained);
                  if (action.execute() && !leftover.isEmpty()) {
                     this.internalTank.setFluid(leftover);
                  }

                  return returned;
               }
            } else {
               return this.internalTank.drain(maxDrain, action);
            }
         } else {
            return this.internalTank.drain(maxDrain, action);
         }
      }
   }

   public HosePulleyFluidHandler(
      SmartFluidTank internalTank, FluidFillingBehaviour filler, FluidDrainingBehaviour drainer, Supplier<BlockPos> rootPosGetter, Supplier<Boolean> predicate
   ) {
      this.internalTank = internalTank;
      this.filler = filler;
      this.drainer = drainer;
      this.rootPosGetter = rootPosGetter;
      this.predicate = predicate;
   }

   public int getTanks() {
      return this.internalTank.getTanks();
   }

   public int getTankCapacity(int tank) {
      return this.internalTank.getTankCapacity(tank);
   }

   public boolean isFluidValid(int tank, FluidStack stack) {
      return this.internalTank.isFluidValid(tank, stack);
   }

   public SmartFluidTank getInternalTank() {
      return this.internalTank;
   }
}
