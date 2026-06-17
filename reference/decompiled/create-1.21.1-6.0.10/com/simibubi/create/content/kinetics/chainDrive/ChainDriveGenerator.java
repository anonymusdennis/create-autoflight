package com.simibubi.create.content.kinetics.chainDrive;

import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import java.util.function.BiFunction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class ChainDriveGenerator extends SpecialBlockStateGen {
   private BiFunction<BlockState, String, ModelFile> modelFunc;

   public ChainDriveGenerator(BiFunction<BlockState, String, ModelFile> modelFunc) {
      this.modelFunc = modelFunc;
   }

   @Override
   protected int getXRotation(BlockState state) {
      ChainDriveBlock.Part part = (ChainDriveBlock.Part)state.getValue(ChainDriveBlock.PART);
      boolean connectedAlongFirst = (Boolean)state.getValue(ChainDriveBlock.CONNECTED_ALONG_FIRST_COORDINATE);
      Axis axis = (Axis)state.getValue(ChainDriveBlock.AXIS);
      if (part == ChainDriveBlock.Part.NONE) {
         return axis == Axis.Y ? 90 : 0;
      } else if (axis == Axis.X) {
         return (connectedAlongFirst ? 90 : 0) + (part == ChainDriveBlock.Part.START ? 180 : 0);
      } else if (axis == Axis.Z) {
         return connectedAlongFirst ? 0 : (part == ChainDriveBlock.Part.START ? 270 : 90);
      } else {
         return 0;
      }
   }

   @Override
   protected int getYRotation(BlockState state) {
      ChainDriveBlock.Part part = (ChainDriveBlock.Part)state.getValue(ChainDriveBlock.PART);
      boolean connectedAlongFirst = (Boolean)state.getValue(ChainDriveBlock.CONNECTED_ALONG_FIRST_COORDINATE);
      Axis axis = (Axis)state.getValue(ChainDriveBlock.AXIS);
      if (part == ChainDriveBlock.Part.NONE) {
         return axis == Axis.X ? 90 : 0;
      } else if (axis == Axis.Z) {
         return connectedAlongFirst && part == ChainDriveBlock.Part.END ? 270 : 90;
      } else {
         boolean flip = part == ChainDriveBlock.Part.END && !connectedAlongFirst || part == ChainDriveBlock.Part.START && connectedAlongFirst;
         return axis == Axis.Y ? (connectedAlongFirst ? 90 : 0) + (flip ? 180 : 0) : 0;
      }
   }

   @Override
   public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {
      return this.modelFunc.apply(state, this.getModelSuffix(state));
   }

   protected String getModelSuffix(BlockState state) {
      ChainDriveBlock.Part part = (ChainDriveBlock.Part)state.getValue(ChainDriveBlock.PART);
      Axis axis = (Axis)state.getValue(ChainDriveBlock.AXIS);
      if (part == ChainDriveBlock.Part.NONE) {
         return "single";
      } else {
         String orientation = axis == Axis.Y ? "vertical" : "horizontal";
         String section = part == ChainDriveBlock.Part.MIDDLE ? "middle" : "end";
         return section + "_" + orientation;
      }
   }
}
