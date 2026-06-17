package com.simibubi.create.content.trains.track;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class TrackBlockStateGenerator extends SpecialBlockStateGen {
   @Override
   protected int getXRotation(BlockState state) {
      return 0;
   }

   @Override
   protected int getYRotation(BlockState state) {
      return ((TrackShape)state.getValue(TrackBlock.SHAPE)).getModelRotation();
   }

   @Override
   public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {
      TrackShape value = (TrackShape)state.getValue(TrackBlock.SHAPE);
      return value == TrackShape.NONE
         ? prov.models().getExistingFile(prov.mcLoc("block/air"))
         : prov.models().getExistingFile(Create.asResource("block/track/" + value.getModel()));
   }
}
