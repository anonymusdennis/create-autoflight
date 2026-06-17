package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.block_breakers;

import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.block_breakers.SubLevelBlockBreakingUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({BlockBreakingKineticBlockEntity.class})
public abstract class BlockBreakingKineticBlockEntityMixin extends BlockEntity {
   public BlockBreakingKineticBlockEntityMixin(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
      super(pType, pPos, pBlockState);
   }

   @Shadow
   public abstract boolean canBreak(BlockState var1, float var2);

   @Redirect(
      remap = false,
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/kinetics/base/BlockBreakingKineticBlockEntity;getBreakingPos()Lnet/minecraft/core/BlockPos;"
      )
   )
   private BlockPos sable$preGetBlockToBreak(BlockBreakingKineticBlockEntity be) {
      assert this.level != null;

      BlockPos breakingPos = this.getBlockPos().relative((Direction)this.getBlockState().getValue(BlockStateProperties.FACING));
      BlockState originalStateToBreak = this.level.getBlockState(breakingPos);
      return !this.canBreak(originalStateToBreak, originalStateToBreak.getDestroySpeed(this.level, breakingPos))
         ? SubLevelBlockBreakingUtility.findBreakingPos(
            (pos, state) -> this.canBreak(state, state.getDestroySpeed(this.level, pos)),
            Sable.HELPER.getContaining(this),
            this.getLevel(),
            Vec3.atLowerCornerOf(((Direction)this.getBlockState().getValue(SawBlock.FACING)).getNormal()),
            this.getBlockPos().getCenter(),
            breakingPos
         )
         : breakingPos;
   }
}
