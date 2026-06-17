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

public class PoweredLatchGenerator extends AbstractDiodeGenerator {
   @Override
   protected <T extends Block> List<ModelFile> createModels(DataGenContext<Block, T> ctx, BlockModelProvider prov) {
      List<ModelFile> models = new ArrayList<>(2);
      String name = ctx.getName();
      ResourceLocation off = this.existing("latch_off");
      ResourceLocation on = this.existing("latch_on");
      models.add(((BlockModelBuilder)prov.withExistingParent(name, off)).texture("top", this.texture(ctx, "idle")));
      models.add(((BlockModelBuilder)prov.withExistingParent(name + "_powered", on)).texture("top", this.texture(ctx, "powering")));
      return models;
   }

   @Override
   protected int getModelIndex(BlockState state) {
      return state.getValue(PoweredLatchBlock.POWERING) ? 1 : 0;
   }
}
