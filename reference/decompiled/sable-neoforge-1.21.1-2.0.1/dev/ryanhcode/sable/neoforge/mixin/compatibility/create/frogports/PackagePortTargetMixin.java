package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget.ChainConveyorFrogportTarget;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ChainConveyorFrogportTarget.class})
public class PackagePortTargetMixin {
   @Shadow
   public float chainPos;
   @Shadow
   @Nullable
   public BlockPos connection;

   @Inject(
      method = {"export"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"
      )},
      cancellable = true
   )
   public void sable$testSublevelDistance(
      LevelAccessor level, BlockPos portPos, ItemStack box, boolean simulate, CallbackInfoReturnable<Boolean> cir, @Local ChainConveyorBlockEntity cbe
   ) {
      Vec3 targetPos = cbe.getPackagePosition(this.chainPos, this.connection);
      int maxRange = (Integer)AllConfigs.server().logistics.packagePortRange.get() + 2;
      if (Sable.HELPER
            .distanceSquaredWithSubLevels((Level)level, targetPos, (double)portPos.getX() + 0.5, (double)portPos.getY() + 0.5, (double)portPos.getZ() + 0.5)
         > (double)(maxRange * maxRange)) {
         cir.setReturnValue(false);
      }
   }
}
