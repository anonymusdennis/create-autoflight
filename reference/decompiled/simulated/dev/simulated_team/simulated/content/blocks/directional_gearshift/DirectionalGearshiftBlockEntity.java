package dev.simulated_team.simulated.content.blocks.directional_gearshift;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DirectionalGearshiftBlockEntity extends SplitShaftBlockEntity {
   public DirectionalGearshiftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public float getRotationSpeedModifier(Direction face) {
      if (this.hasSource()) {
         if (face == this.getSourceFacing()) {
            return 1.0F;
         }

         boolean leftPowered = (Boolean)this.getBlockState().getValue(DirectionalGearshiftBlock.LEFT_POWERED);
         boolean rightPowered = (Boolean)this.getBlockState().getValue(DirectionalGearshiftBlock.RIGHT_POWERED);
         if (rightPowered && leftPowered) {
            return 0.0F;
         }

         if (leftPowered) {
            return 1.0F;
         }

         if (rightPowered) {
            return -1.0F;
         }
      }

      return 0.0F;
   }
}
