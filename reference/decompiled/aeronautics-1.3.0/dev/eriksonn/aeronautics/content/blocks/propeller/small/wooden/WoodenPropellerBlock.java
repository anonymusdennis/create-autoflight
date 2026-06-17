package dev.eriksonn.aeronautics.content.blocks.propeller.small.wooden;

import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class WoodenPropellerBlock extends BasePropellerBlock {
   public WoodenPropellerBlock(Properties properties) {
      super(properties);
   }

   public BlockEntityType<? extends BasePropellerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends BasePropellerBlockEntity>)AeroBlockEntityTypes.WOODEN_PROPELLER.get();
   }
}
