package com.simibubi.create.foundation.data;

import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import java.util.function.Supplier;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder;

public class MetalBarsGen {
   public static <P extends IronBarsBlock> NonNullBiConsumer<DataGenContext<Block, P>, RegistrateBlockstateProvider> barsBlockState(
      String name, boolean specialEdge
   ) {
      return (c, p) -> {
         ModelFile post_ends = barsSubModel(p, name, "post_ends", specialEdge);
         ModelFile post = barsSubModel(p, name, "post", specialEdge);
         ModelFile cap = barsSubModel(p, name, "cap", specialEdge);
         ModelFile cap_alt = barsSubModel(p, name, "cap_alt", specialEdge);
         ModelFile side = barsSubModel(p, name, "side", specialEdge);
         ModelFile side_alt = barsSubModel(p, name, "side_alt", specialEdge);
         ((PartBuilder)((PartBuilder)((PartBuilder)((PartBuilder)((PartBuilder)((PartBuilder)((PartBuilder)((PartBuilder)((PartBuilder)((PartBuilder)p.getMultipartBuilder(
                                             (Block)c.get()
                                          )
                                          .part()
                                          .modelFile(post_ends)
                                          .addModel())
                                       .end()
                                       .part()
                                       .modelFile(post)
                                       .addModel())
                                    .condition(BlockStateProperties.NORTH, new Boolean[]{false})
                                    .condition(BlockStateProperties.EAST, new Boolean[]{false})
                                    .condition(BlockStateProperties.SOUTH, new Boolean[]{false})
                                    .condition(BlockStateProperties.WEST, new Boolean[]{false})
                                    .end()
                                    .part()
                                    .modelFile(cap)
                                    .addModel())
                                 .condition(BlockStateProperties.NORTH, new Boolean[]{true})
                                 .condition(BlockStateProperties.EAST, new Boolean[]{false})
                                 .condition(BlockStateProperties.SOUTH, new Boolean[]{false})
                                 .condition(BlockStateProperties.WEST, new Boolean[]{false})
                                 .end()
                                 .part()
                                 .modelFile(cap)
                                 .rotationY(90)
                                 .addModel())
                              .condition(BlockStateProperties.NORTH, new Boolean[]{false})
                              .condition(BlockStateProperties.EAST, new Boolean[]{true})
                              .condition(BlockStateProperties.SOUTH, new Boolean[]{false})
                              .condition(BlockStateProperties.WEST, new Boolean[]{false})
                              .end()
                              .part()
                              .modelFile(cap_alt)
                              .addModel())
                           .condition(BlockStateProperties.NORTH, new Boolean[]{false})
                           .condition(BlockStateProperties.EAST, new Boolean[]{false})
                           .condition(BlockStateProperties.SOUTH, new Boolean[]{true})
                           .condition(BlockStateProperties.WEST, new Boolean[]{false})
                           .end()
                           .part()
                           .modelFile(cap_alt)
                           .rotationY(90)
                           .addModel())
                        .condition(BlockStateProperties.NORTH, new Boolean[]{false})
                        .condition(BlockStateProperties.EAST, new Boolean[]{false})
                        .condition(BlockStateProperties.SOUTH, new Boolean[]{false})
                        .condition(BlockStateProperties.WEST, new Boolean[]{true})
                        .end()
                        .part()
                        .modelFile(side)
                        .addModel())
                     .condition(BlockStateProperties.NORTH, new Boolean[]{true})
                     .end()
                     .part()
                     .modelFile(side)
                     .rotationY(90)
                     .addModel())
                  .condition(BlockStateProperties.EAST, new Boolean[]{true})
                  .end()
                  .part()
                  .modelFile(side_alt)
                  .addModel())
               .condition(BlockStateProperties.SOUTH, new Boolean[]{true})
               .end()
               .part()
               .modelFile(side_alt)
               .rotationY(90)
               .addModel())
            .condition(BlockStateProperties.WEST, new Boolean[]{true})
            .end();
      };
   }

   private static ModelFile barsSubModel(RegistrateBlockstateProvider p, String name, String suffix, boolean specialEdge) {
      ResourceLocation barsTexture = p.modLoc("block/bars/" + name + "_bars");
      ResourceLocation edgeTexture = specialEdge ? p.modLoc("block/bars/" + name + "_bars_edge") : barsTexture;
      return ((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)p.models().withExistingParent(name + "_" + suffix, p.modLoc("block/bars/" + suffix)))
               .texture("bars", barsTexture))
            .texture("particle", barsTexture))
         .texture("edge", edgeTexture);
   }

   public static BlockEntry<IronBarsBlock> createBars(String name, boolean specialEdge, Supplier<DataIngredient> ingredient, MapColor color) {
      return ((BlockBuilder)((BlockBuilder)Create.registrate()
               .block(name + "_bars", IronBarsBlock::new)
               .addLayer(() -> RenderType::cutoutMipped)
               .initialProperties(() -> Blocks.IRON_BARS)
               .properties(p -> p.sound(SoundType.COPPER).mapColor(color))
               .tag(new TagKey[]{AllTags.AllBlockTags.WRENCH_PICKUP.tag})
               .tag(new TagKey[]{AllTags.AllBlockTags.FAN_TRANSPARENT.tag})
               .transform(TagGen.pickaxeOnly()))
            .blockstate(barsBlockState(name, specialEdge))
            .item()
            .model((c, p) -> {
               ResourceLocation barsTexture = p.modLoc("block/bars/" + name + "_bars");
               p.generated(c, new ResourceLocation[]{barsTexture});
            })
            .recipe((c, p) -> p.stonecutting(ingredient.get(), RecipeCategory.DECORATIONS, c::get, 4))
            .build())
         .register();
   }
}
