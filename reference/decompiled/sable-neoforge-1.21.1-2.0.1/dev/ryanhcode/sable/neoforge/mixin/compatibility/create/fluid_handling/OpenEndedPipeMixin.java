package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.fluid_handling;

import com.simibubi.create.content.fluids.OpenEndedPipe;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({OpenEndedPipe.class})
public abstract class OpenEndedPipeMixin {
   @Shadow
   private BlockPos outputPos;
   @Shadow
   private Level world;
   @Unique
   private BlockPos sable$plotOutputPos;

   @Shadow
   public abstract BlockPos getPos();

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")},
      remap = false
   )
   private void sable$saveCurrentPos(BlockFace face, CallbackInfo ci) {
      this.sable$plotOutputPos = this.outputPos;
   }

   @Redirect(
      method = {"*"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
      )
   )
   private BlockState sable$getBlockstateInclSublevels(Level level, BlockPos pos) {
      this.outputPos = this.sable$plotOutputPos;
      ActiveSableCompanion helper = Sable.HELPER;
      Vec3 checkPos = Vec3.atCenterOf(this.sable$plotOutputPos);
      BlockState gatheredState = helper.runIncludingSubLevels(level, checkPos, true, helper.getContaining(level, checkPos), this::sable$gatherState);
      if (gatheredState == null) {
         this.outputPos = this.sable$plotOutputPos;
         gatheredState = level.getBlockState(this.sable$plotOutputPos);
      }

      return gatheredState;
   }

   @Unique
   private BlockState sable$gatherState(SubLevel level, BlockPos b) {
      BlockState checkedState = this.world.getBlockState(b);
      if (!checkedState.isAir()) {
         this.outputPos = b;
         return checkedState;
      } else {
         return null;
      }
   }

   @Redirect(
      method = {"provideFluidToSpace"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
         ordinal = 1
      )
   )
   private boolean sable$preventInWorldPlace(Level instance, BlockPos pPos, BlockState pNesubleveltate, int pFlags) {
      return instance.setBlock(this.sable$plotOutputPos, pNesubleveltate, 3);
   }
}
