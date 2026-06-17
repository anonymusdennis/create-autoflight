package com.simibubi.create.content.decoration.girder;

import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder;

public class GirderBlockStateGenerator {
   public static void blockStateWithShaft(DataGenContext<Block, GirderEncasedShaftBlock> c, RegistrateBlockstateProvider p) {
      MultiPartBlockStateBuilder builder = p.getMultipartBuilder((Block)c.get());
      ((PartBuilder)builder.part().modelFile(AssetLookup.partialBaseModel(c, p)).rotationY(0).addModel())
         .condition(GirderEncasedShaftBlock.HORIZONTAL_AXIS, new Axis[]{Axis.Z})
         .end();
      ((PartBuilder)builder.part().modelFile(AssetLookup.partialBaseModel(c, p)).rotationY(90).addModel())
         .condition(GirderEncasedShaftBlock.HORIZONTAL_AXIS, new Axis[]{Axis.X})
         .end();
      ((PartBuilder)builder.part().modelFile(AssetLookup.partialBaseModel(c, p, "top")).addModel())
         .condition(GirderEncasedShaftBlock.TOP, new Boolean[]{true})
         .end();
      ((PartBuilder)builder.part().modelFile(AssetLookup.partialBaseModel(c, p, "bottom")).addModel())
         .condition(GirderEncasedShaftBlock.BOTTOM, new Boolean[]{true})
         .end();
   }

   public static void blockState(DataGenContext<Block, GirderBlock> c, RegistrateBlockstateProvider p) {
      MultiPartBlockStateBuilder builder = p.getMultipartBuilder((Block)c.get());
      ((PartBuilder)builder.part().modelFile(AssetLookup.partialBaseModel(c, p, "pole")).addModel())
         .condition(GirderBlock.X, new Boolean[]{false})
         .condition(GirderBlock.Z, new Boolean[]{false})
         .end();
      ((PartBuilder)builder.part().modelFile(AssetLookup.partialBaseModel(c, p, "x")).addModel()).condition(GirderBlock.X, new Boolean[]{true}).end();
      ((PartBuilder)builder.part().modelFile(AssetLookup.partialBaseModel(c, p, "z")).addModel()).condition(GirderBlock.Z, new Boolean[]{true}).end();

      for (boolean x : Iterate.trueAndFalse) {
         ((PartBuilder)((PartBuilder)builder.part().modelFile(AssetLookup.partialBaseModel(c, p, "top")).addModel())
               .condition(GirderBlock.TOP, new Boolean[]{true})
               .condition(GirderBlock.X, new Boolean[]{x})
               .condition(GirderBlock.Z, new Boolean[]{!x})
               .end()
               .part()
               .modelFile(AssetLookup.partialBaseModel(c, p, "bottom"))
               .addModel())
            .condition(GirderBlock.BOTTOM, new Boolean[]{true})
            .condition(GirderBlock.X, new Boolean[]{x})
            .condition(GirderBlock.Z, new Boolean[]{!x})
            .end();
      }

      ((PartBuilder)builder.part().modelFile(AssetLookup.partialBaseModel(c, p, "cross")).addModel())
         .condition(GirderBlock.X, new Boolean[]{true})
         .condition(GirderBlock.Z, new Boolean[]{true})
         .end();
   }
}
