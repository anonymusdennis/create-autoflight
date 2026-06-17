package com.simibubi.create.content.redstone.thresholdSwitch;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class ThresholdSwitchGenerator extends SpecialBlockStateGen {
   @Override
   protected int getXRotation(BlockState state) {
      return 0;
   }

   @Override
   protected int getYRotation(BlockState state) {
      return this.horizontalAngle((Direction)state.getValue(ThresholdSwitchBlock.FACING)) + 180;
   }

   @Override
   public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {
      int level = (Integer)state.getValue(ThresholdSwitchBlock.LEVEL);
      String path = "block/threshold_switch/block_" + Lang.asId(((AttachFace)state.getValue(ThresholdSwitchBlock.TARGET)).name());
      return ((BlockModelBuilder)prov.models().withExistingParent(path + "_" + level, Create.asResource(path)))
         .texture("level", Create.asResource("block/threshold_switch/level_" + level));
   }
}
