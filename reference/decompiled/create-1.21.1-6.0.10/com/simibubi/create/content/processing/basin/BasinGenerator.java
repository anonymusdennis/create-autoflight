package com.simibubi.create.content.processing.basin;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class BasinGenerator extends SpecialBlockStateGen {
   @Override
   protected int getXRotation(BlockState state) {
      return 0;
   }

   @Override
   protected int getYRotation(BlockState state) {
      return this.horizontalAngle((Direction)state.getValue(BasinBlock.FACING));
   }

   @Override
   public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {
      return ((Direction)state.getValue(BasinBlock.FACING)).getAxis().isVertical()
         ? AssetLookup.partialBaseModel(ctx, prov)
         : AssetLookup.partialBaseModel(ctx, prov, "directional");
   }
}
