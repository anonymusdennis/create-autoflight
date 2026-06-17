package com.simibubi.create.content.redstone.diodes;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.ModelFile.ExistingModelFile;

public abstract class AbstractDiodeGenerator extends SpecialBlockStateGen {
   private List<ModelFile> models;

   public static <I extends BlockItem> void diodeItemModel(DataGenContext<Item, I> c, RegistrateItemModelProvider p) {
      String name = c.getName();
      String path = "block/diodes/";
      ItemModelBuilder builder = (ItemModelBuilder)p.withExistingParent(name, p.modLoc(path + name));
      builder.texture("top", path + name + "/item");
   }

   @Override
   protected final int getXRotation(BlockState state) {
      return 0;
   }

   @Override
   protected final int getYRotation(BlockState state) {
      return this.horizontalAngle((Direction)state.getValue(AbstractDiodeBlock.FACING));
   }

   protected abstract <T extends Block> List<ModelFile> createModels(DataGenContext<Block, T> var1, BlockModelProvider var2);

   protected abstract int getModelIndex(BlockState var1);

   @Override
   public final <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, BlockState state) {
      if (this.models == null) {
         this.models = this.createModels(ctx, prov.models());
      }

      return this.models.get(this.getModelIndex(state));
   }

   protected ExistingModelFile existingModel(BlockModelProvider prov, String name) {
      return prov.getExistingFile(this.existing(name));
   }

   protected ResourceLocation existing(String name) {
      return Create.asResource("block/diodes/" + name);
   }

   protected <T extends Block> ResourceLocation texture(DataGenContext<Block, T> ctx, String name) {
      return Create.asResource("block/diodes/" + ctx.getName() + "/" + name);
   }

   protected ResourceLocation poweredTorch() {
      return ResourceLocation.withDefaultNamespace("block/redstone_torch");
   }
}
