package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.hose_pulley;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({HosePulleyBlockEntity.class})
public abstract class HosePulleyBlockEntityMixin extends SmartBlockEntity {
   public HosePulleyBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @WrapOperation(
      method = {"lazyTick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
      )}
   )
   public BlockState sable$checkForCollisions1(Level instance, BlockPos blockPos, Operation<BlockState> original) {
      return this.sable$getBlockState(instance, blockPos, original, true);
   }

   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
      )}
   )
   public BlockState sable$checkForCollisions2(Level instance, BlockPos blockPos, Operation<BlockState> original) {
      return this.sable$getBlockState(instance, blockPos, original, false);
   }

   @WrapOperation(
      method = {"onSpeedChanged"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
      )}
   )
   public BlockState sable$checkForCollisions3(Level instance, BlockPos blockPos, Operation<BlockState> original) {
      return this.sable$getBlockState(instance, blockPos, original, false);
   }

   @Unique
   private BlockState sable$getBlockState(Level level, BlockPos blockPos, Operation<BlockState> original, boolean inverseReplaceCheck) {
      ActiveSableCompanion helper = Sable.HELPER;
      BlockState gatheredState = helper.runIncludingSubLevels(
         level, blockPos.getCenter(), true, helper.getContaining(level, this.getBlockPos()), (sublevel, pos) -> {
            BlockState innerState = (BlockState)original.call(new Object[]{level, pos});
            return inverseReplaceCheck ^ innerState.canBeReplaced() ? innerState : null;
         }
      );
      return gatheredState != null ? gatheredState : (BlockState)original.call(new Object[]{level, blockPos});
   }
}
