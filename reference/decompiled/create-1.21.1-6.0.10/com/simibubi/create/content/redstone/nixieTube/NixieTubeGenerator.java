package com.simibubi.create.content.redstone.nixieTube;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class NixieTubeGenerator extends SpecialBlockStateGen {
   @Override
   protected int getXRotation(BlockState state) {
      return ((DoubleFaceAttachedBlock.DoubleAttachFace)state.getValue(NixieTubeBlock.FACE)).xRot();
   }

   @Override
   protected int getYRotation(BlockState state) {
      DoubleFaceAttachedBlock.DoubleAttachFace face = (DoubleFaceAttachedBlock.DoubleAttachFace)state.getValue(NixieTubeBlock.FACE);
      return this.horizontalAngle((Direction)state.getValue(NixieTubeBlock.FACING))
         + (face != DoubleFaceAttachedBlock.DoubleAttachFace.WALL && face != DoubleFaceAttachedBlock.DoubleAttachFace.WALL_REVERSED ? 0 : 180);
   }

   @Override
   public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {
      return prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/nixie_tube/block"));
   }
}
