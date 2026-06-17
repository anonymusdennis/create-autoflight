package dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite;

import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class AndesitePropellerBlockEntity extends BasePropellerBlockEntity {
   public AndesitePropellerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   @Override
   public double getConfigThrust() {
      return (Double)AeroConfig.server().physics.andesitePropellerThrust.get();
   }

   @Override
   public double getConfigAirflow() {
      return (Double)AeroConfig.server().physics.andesitePropellerAirflow.get();
   }

   @Override
   public float getRadius() {
      return 1.0F;
   }

   @Override
   public float getOffset() {
      return 0.1875F;
   }
}
