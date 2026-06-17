package com.simibubi.create.content.logistics.packager.repackager;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.logistics.packager.PackagerBlock;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;

public class RepackagerBlock extends PackagerBlock {
   public RepackagerBlock(Properties properties) {
      super(properties);
   }

   @Override
   public BlockEntityType<? extends PackagerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends PackagerBlockEntity>)AllBlockEntityTypes.REPACKAGER.get();
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, POWERED});
   }
}
