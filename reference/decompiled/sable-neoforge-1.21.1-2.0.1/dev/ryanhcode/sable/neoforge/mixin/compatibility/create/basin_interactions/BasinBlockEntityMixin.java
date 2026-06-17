package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.basin_interactions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BasinBlockEntity.class})
public class BasinBlockEntityMixin extends BlockEntity {
   @Shadow
   @Nullable
   private HeatLevel cachedHeatLevel;

   public BasinBlockEntityMixin(BlockEntityType<?> arg, BlockPos arg2, BlockState arg3) {
      super(arg, arg2, arg3);
   }

   @Inject(
      method = {"getHeatLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;getHeatLevelOf(Lnet/minecraft/world/level/block/state/BlockState;)Lcom/simibubi/create/content/processing/burner/BlazeBurnerBlock$HeatLevel;",
         shift = Shift.AFTER
      )},
      cancellable = true
   )
   private void sable$accountForSubLevels(CallbackInfoReturnable<HeatLevel> cir) {
      if (this.cachedHeatLevel == null || this.cachedHeatLevel == HeatLevel.NONE) {
         Level level = this.getLevel();
         BlockPos originalPos = this.getBlockPos().below();
         ActiveSableCompanion helper = Sable.HELPER;
         HeatLevel heatLevel = helper.runIncludingSubLevels(
            level, originalPos.getCenter(), false, helper.getContaining(level, originalPos), (subLevel, pos) -> {
               HeatLevel internalHeat = BasinBlockEntity.getHeatLevelOf(level.getBlockState(pos));
               return internalHeat != HeatLevel.NONE ? internalHeat : null;
            }
         );
         if (heatLevel != null) {
            cir.setReturnValue(heatLevel);
         }
      }
   }

   @WrapOperation(
      method = {"*"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"
      )}
   )
   public BlockEntity sable$accountForSubLevels(Level level, BlockPos pos, Operation<BlockEntity> original) {
      ActiveSableCompanion helper = Sable.HELPER;
      return helper.runIncludingSubLevels(
         level,
         pos.getCenter(),
         true,
         helper.getContaining(level, pos),
         (subLevel, internalPos) -> (BlockEntity)original.call(new Object[]{level, internalPos})
      );
   }
}
