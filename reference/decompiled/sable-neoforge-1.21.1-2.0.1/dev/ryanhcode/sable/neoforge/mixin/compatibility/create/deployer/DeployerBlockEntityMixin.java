package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.deployer;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import javax.annotation.Nullable;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({DeployerBlockEntity.class})
public abstract class DeployerBlockEntityMixin extends SmartBlockEntity {
   public DeployerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Redirect(
      method = {"start"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/lang/Math;min(DD)D"
      )
   )
   private double sable$deployerMin(double a, double b, @Local(ordinal = 1) Vec3 rayOrigin, @Local(ordinal = 0) BlockHitResult result) {
      return Math.min(Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(this.level, result.getLocation(), rayOrigin)), b);
   }

   @ModifyArg(
      method = {"activate"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/kinetics/deployer/DeployerHandler;activate(Lcom/simibubi/create/content/kinetics/deployer/DeployerFakePlayer;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/Vec3;Lcom/simibubi/create/content/kinetics/deployer/DeployerBlockEntity$Mode;)V"
      ),
      index = 2,
      remap = false
   )
   private BlockPos sable$checkPositions(BlockPos pos) {
      Vec3 centerPos = Vec3.atCenterOf(pos);
      ActiveSableCompanion helper = Sable.HELPER;
      BlockPos gatheredPos = helper.runIncludingSubLevels(this.getLevel(), centerPos, true, helper.getContaining(this), this::sable$getState);
      return gatheredPos != null ? gatheredPos : pos;
   }

   @Unique
   @Nullable
   private BlockPos sable$getState(SubLevel subLevel, BlockPos pos) {
      Level level = this.getLevel();

      assert level != null;

      BlockState state = level.getBlockState(pos);
      if (!state.isAir()) {
         return pos;
      } else {
         for (Direction direction : Iterate.directions) {
            if (!level.getBlockState(pos.relative(direction)).isAir()) {
               return pos;
            }
         }

         return null;
      }
   }
}
