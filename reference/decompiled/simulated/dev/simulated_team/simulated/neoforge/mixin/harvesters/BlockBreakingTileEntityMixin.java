package dev.simulated_team.simulated.neoforge.mixin.harvesters;

import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.simulated_team.simulated.content.blocks.auger_shaft.BlockHarvester;
import dev.simulated_team.simulated.content.blocks.auger_shaft.auger_groups.AugerDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockBreakingKineticBlockEntity.class})
public abstract class BlockBreakingTileEntityMixin extends SmartBlockEntity implements BlockHarvester {
   @Unique
   AugerDistributor simulated$attachedGroup;

   public BlockBreakingTileEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public AugerDistributor simulated$getAssociatedDistributor() {
      return this.simulated$attachedGroup;
   }

   @Override
   public void simulated$setDistributor(AugerDistributor distributor) {
      this.simulated$attachedGroup = distributor;
   }

   @Inject(
      method = {"lambda$onBlockBroken$0"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void checkIfDepositable(Vec3 vec, ItemStack stack, CallbackInfo ci) {
      if (this.depositItemStack(this.worldPosition, stack).isEmpty()) {
         ci.cancel();
      }
   }
}
