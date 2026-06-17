package dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite;

import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class AndesitePropellerBlock extends BasePropellerBlock {
   public AndesitePropellerBlock(Properties properties) {
      super(properties);
   }

   public BlockEntityType<? extends BasePropellerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends BasePropellerBlockEntity>)AeroBlockEntityTypes.ANDESITE_PROPELLER.get();
   }
}
