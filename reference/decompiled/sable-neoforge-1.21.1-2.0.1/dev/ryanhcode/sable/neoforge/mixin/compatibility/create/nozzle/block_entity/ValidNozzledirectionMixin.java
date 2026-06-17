package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.nozzle.block_entity;

import com.simibubi.create.content.kinetics.fan.NozzleBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.NozzleBlockEntityExtension;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({NozzleBlockEntity.class})
public abstract class ValidNozzledirectionMixin extends SmartBlockEntity implements NozzleBlockEntityExtension {
   @Unique
   private final EnumSet<Direction> sable$validDirections = EnumSet.noneOf(Direction.class);

   public ValidNozzledirectionMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public EnumSet<Direction> sable$getValidDirections() {
      return this.sable$validDirections;
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void sable$updateValidDirections(CallbackInfo ci) {
      this.sable$validDirections.clear();
      if (this.getLevel() != null) {
         for (Direction value : Direction.values()) {
            BlockState state = this.getLevel().getBlockState(this.getBlockPos().relative(value));
            if (state.canBeReplaced()) {
               this.sable$validDirections.add(value);
            }
         }
      }
   }
}
