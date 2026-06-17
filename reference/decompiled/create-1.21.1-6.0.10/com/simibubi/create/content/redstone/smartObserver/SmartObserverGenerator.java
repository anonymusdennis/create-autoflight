package com.simibubi.create.content.redstone.smartObserver;

import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchBlock;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class SmartObserverGenerator extends SpecialBlockStateGen {
   @Override
   protected int getXRotation(BlockState state) {
      return switch ((AttachFace)state.getValue(SmartObserverBlock.TARGET)) {
         case CEILING -> -90;
         case WALL -> 0;
         case FLOOR -> 90;
         default -> throw new MatchException(null, null);
      };
   }

   @Override
   protected int getYRotation(BlockState state) {
      return this.horizontalAngle((Direction)state.getValue(ThresholdSwitchBlock.FACING)) + 180;
   }

   @Override
   public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {
      return AssetLookup.forPowered(ctx, prov).apply(state);
   }
}
