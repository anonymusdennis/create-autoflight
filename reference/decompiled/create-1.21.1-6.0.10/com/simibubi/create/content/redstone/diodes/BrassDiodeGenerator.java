package com.simibubi.create.content.redstone.diodes;

import com.tterrag.registrate.providers.DataGenContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class BrassDiodeGenerator extends AbstractDiodeGenerator {
   @Override
   protected <T extends Block> List<ModelFile> createModels(DataGenContext<Block, T> ctx, BlockModelProvider prov) {
      List<ModelFile> models = new ArrayList<>(4);
      String name = ctx.getName();
      ResourceLocation template = this.existing(name);
      models.add(prov.getExistingFile(template));
      models.add(((BlockModelBuilder)prov.withExistingParent(name + "_powered", template)).texture("top", this.texture(ctx, "powered")));
      models.add(
         ((BlockModelBuilder)((BlockModelBuilder)prov.withExistingParent(name + "_powering", template)).texture("torch", this.poweredTorch()))
            .texture("top", this.texture(ctx, "powering"))
      );
      models.add(
         ((BlockModelBuilder)((BlockModelBuilder)prov.withExistingParent(name + "_powered_powering", template)).texture("torch", this.poweredTorch()))
            .texture("top", this.texture(ctx, "powered_powering"))
      );
      return models;
   }

   @Override
   protected int getModelIndex(BlockState state) {
      return (state.getValue(BrassDiodeBlock.POWERING) ^ state.getValue(BrassDiodeBlock.INVERTED) ? 2 : 0) + (state.getValue(BrassDiodeBlock.POWERED) ? 1 : 0);
   }
}
