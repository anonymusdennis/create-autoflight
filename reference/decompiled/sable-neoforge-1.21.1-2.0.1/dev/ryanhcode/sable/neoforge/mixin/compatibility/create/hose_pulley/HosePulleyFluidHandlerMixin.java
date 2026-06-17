package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.hose_pulley;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({HosePulleyFluidHandler.class})
public abstract class HosePulleyFluidHandlerMixin {
   @Shadow
   private FluidDrainingBehaviour drainer;
   @Shadow
   private Supplier<BlockPos> rootPosGetter;
   @Unique
   private BlockPos sable$lastValidPos = null;

   @Inject(
      method = {"drainInternal"},
      at = {@At("HEAD")}
   )
   public void sable$updateLastValidPos(int maxDrain, FluidStack resource, FluidAction action, CallbackInfoReturnable<FluidStack> cir) {
      ActiveSableCompanion helper = Sable.HELPER;
      Level level = this.drainer.getWorld();
      float distance = 1.5F;
      this.sable$lastValidPos = helper.runIncludingSubLevels(
         level, this.rootPosGetter.get().getCenter(), true, helper.getContaining(level, this.drainer.getPos()), (sublevel, pos) -> {
            if (sable$hasFluid(level, pos)) {
               return this.sable$lastValidPos != null && !(this.sable$lastValidPos.distSqr(pos) > 2.25) ? this.sable$lastValidPos : pos;
            } else {
               return null;
            }
         }
      );
   }

   @WrapOperation(
      method = {"drainInternal"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;getDrainableFluid(Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/fluids/FluidStack;"
      )}
   )
   public FluidStack sable$modifyGetDrainableFluid(FluidDrainingBehaviour instance, BlockPos rootPos, Operation<FluidStack> original) {
      return this.sable$lastValidPos != null
         ? (FluidStack)original.call(new Object[]{instance, this.sable$lastValidPos})
         : (FluidStack)original.call(new Object[]{instance, rootPos});
   }

   @WrapOperation(
      method = {"drainInternal"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;pullNext(Lnet/minecraft/core/BlockPos;Z)Z"
      )}
   )
   public boolean sable$modifyPullNext(FluidDrainingBehaviour instance, BlockPos root, boolean simulate, Operation<Boolean> original) {
      return this.sable$lastValidPos != null
         ? (Boolean)original.call(new Object[]{instance, this.sable$lastValidPos, simulate})
         : (Boolean)original.call(new Object[]{instance, root, simulate});
   }

   @WrapOperation(
      method = {"getFluidInTank"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;getDrainableFluid(Lnet/minecraft/core/BlockPos;)Lnet/neoforged/neoforge/fluids/FluidStack;"
      )}
   )
   public FluidStack sable$modifyGetFluidInTank(FluidDrainingBehaviour instance, BlockPos rootPos, Operation<FluidStack> original) {
      return this.sable$lastValidPos != null
         ? (FluidStack)original.call(new Object[]{instance, this.sable$lastValidPos})
         : (FluidStack)original.call(new Object[]{instance, rootPos});
   }

   @Unique
   private static boolean sable$hasFluid(Level level, BlockPos pos) {
      return !level.getFluidState(pos).isEmpty();
   }
}
