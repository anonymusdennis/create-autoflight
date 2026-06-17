package dev.eriksonn.aeronautics.index;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.foundation.block.DyedBlockList;
import com.simibubi.create.foundation.block.connected.SimpleCTBehaviour;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.BlockStateGen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.foundation.data.TagGen;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.config.server.AeroStress;
import dev.eriksonn.aeronautics.content.blocks.hot_air.envelope.EnvelopeBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.envelope.EnvelopeEncasedShaftBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlock;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlock;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing.GyroscopicPropellerBearingBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller.SmartPropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.wooden.WoodenPropellerBlock;
import dev.eriksonn.aeronautics.content.components.Levitating;
import dev.eriksonn.aeronautics.data.AeroBlockStateGen;
import dev.ryanhcode.sable.index.SableTags;
import dev.simulated_team.simulated.data.SimBlockStateGen;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.sounds.SimLazySoundType;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.registrate.simulated_tab.CreativeTabItemTransforms.VisibilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootPool.Builder;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

public class AeroBlocks {
   private static final SimulatedRegistrate REGISTRATE = Aeronautics.getRegistrate();
   public static final BlockEntry<EnvelopeBlock> WHITE_ENVELOPE_BLOCK = ((BlockBuilder)((BlockBuilder)REGISTRATE.block(
               "white_envelope", p -> new EnvelopeBlock(p, DyeColor.WHITE)
            )
            .lang("Hot Air Envelope")
            .initialProperties(SharedProperties::wooden)
            .properties(p -> p.isValidSpawn(AeroBlocks::neverSpawn))
            .properties(
               p -> p.sound(
                     new SimLazySoundType(
                        1.0F,
                        1.0F,
                        AeroSoundEvents.ENVELOPE_BREAK::event,
                        () -> SoundEvents.WOOL_STEP,
                        AeroSoundEvents.ENVELOPE_PLACE::event,
                        AeroSoundEvents.ENVELOPE_HIT::event,
                        () -> SoundEvents.WOOL_FALL
                     )
                  )
            )
            .properties(p -> p.mapColor(DyeColor.WHITE))
            .blockstate(
               (c, p) -> p.simpleBlock((Block)c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/envelope_block/envelope_" + DyeColor.WHITE.getName())))
            )
            .recipe(
               (c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)c.get(), 4)
                     .pattern("WS")
                     .pattern("SW")
                     .define('W', DyeHelper.getWoolOfDye(DyeColor.WHITE))
                     .define('S', Items.STICK)
                     .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(ItemTags.WOOL))
                     .save(p)
            )
            .tag(new TagKey[]{AeroTags.BlockTags.ENVELOPE})
            .tag(new TagKey[]{BlockTags.MINEABLE_WITH_AXE})
            .transform(flammable(30, 60)))
         .item()
         .tag(new TagKey[]{AeroTags.ItemTags.ENVELOPE})
         .tag(new TagKey[]{AeroTags.ItemTags.SHAFTLESS_ENVELOPE})
         .build())
      .register();
   public static final DyedBlockList<EnvelopeBlock> DYED_ENVELOPE_BLOCKS = new DyedBlockList(
      color -> {
         String colorName = color.getSerializedName();
         return color == DyeColor.WHITE
            ? WHITE_ENVELOPE_BLOCK
            : ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(colorName + "_envelope", p -> new EnvelopeBlock(p, color))
                        .lang(RegistrateLangProvider.toEnglishName(color.getName()) + " Hot Air Envelope")
                        .initialProperties(SharedProperties::wooden)
                        .properties(p -> p.isValidSpawn(AeroBlocks::neverSpawn))
                        .properties(
                           p -> p.sound(
                                 new SimLazySoundType(
                                    1.0F,
                                    1.0F,
                                    AeroSoundEvents.ENVELOPE_BREAK::event,
                                    () -> SoundEvents.WOOL_STEP,
                                    AeroSoundEvents.ENVELOPE_PLACE::event,
                                    AeroSoundEvents.ENVELOPE_HIT::event,
                                    () -> SoundEvents.WOOL_FALL
                                 )
                              )
                        )
                        .properties(p -> p.mapColor(color))
                        .blockstate(
                           (c, p) -> p.simpleBlock((Block)c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/envelope_block/envelope_" + colorName)))
                        )
                        .recipe(
                           (c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)c.get(), 4)
                                 .pattern("WS")
                                 .pattern("SW")
                                 .define('W', DyeHelper.getWoolOfDye(color))
                                 .define('S', Items.STICK)
                                 .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(ItemTags.WOOL))
                                 .save(p)
                        )
                        .tag(new TagKey[]{AeroTags.BlockTags.ENVELOPE})
                        .tag(new TagKey[]{BlockTags.MINEABLE_WITH_AXE})
                        .transform(VisibilityType.SEARCH_ONLY.applyBlock()))
                     .transform(flammable(30, 60)))
                  .item()
                  .tag(new TagKey[]{AeroTags.ItemTags.ENVELOPE})
                  .tag(new TagKey[]{AeroTags.ItemTags.SHAFTLESS_ENVELOPE})
                  .build())
               .register();
      }
   );
   public static final DyedBlockList<EnvelopeEncasedShaftBlock> ENVELOPE_ENCASED_SHAFTS = new DyedBlockList(
      color -> {
         String colorName = color.getSerializedName();
         return ((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                              colorName + "_envelope_encased_shaft", p -> EnvelopeEncasedShaftBlock.withCanvas(p, color)
                           )
                           .initialProperties(SharedProperties::wooden)
                           .properties(p -> p.sound(SoundType.SCAFFOLDING))
                           .properties(Properties::noOcclusion)
                           .properties(
                              p -> p.sound(
                                    new SimLazySoundType(
                                       1.0F,
                                       1.0F,
                                       AeroSoundEvents.ENVELOPE_BREAK::event,
                                       () -> SoundEvents.WOOL_STEP,
                                       AeroSoundEvents.ENVELOPE_PLACE::event,
                                       AeroSoundEvents.ENVELOPE_HIT::event,
                                       () -> SoundEvents.WOOL_FALL
                                    )
                                 )
                           )
                           .properties(p -> p.mapColor(color))
                           .transform(b -> (BlockBuilder)b.transform(EncasingRegistry.addVariantTo(AllBlocks.SHAFT))))
                        .blockstate(
                           (c, p) -> BlockStateGen.axisBlock(
                                 c,
                                 p,
                                 blockState -> ((BlockModelBuilder)p.models()
                                          .withExistingParent(colorName + "_envelope_encased_shaft", p.modLoc("block/envelope_encased_shaft/block")))
                                       .texture("0", p.modLoc("block/envelope_block/envelope_" + colorName))
                              )
                        )
                        .loot(
                           (p, b) -> p.add(
                                 b,
                                 p.createSingleItemTable(DYED_ENVELOPE_BLOCKS.get(color))
                                    .withPool(
                                       (Builder)p.applyExplosionCondition(
                                          (ItemLike)AllBlocks.SHAFT.get(),
                                          LootPool.lootPool()
                                             .setRolls(ConstantValue.exactly(1.0F))
                                             .add(LootItem.lootTableItem((ItemLike)AllBlocks.SHAFT.get()))
                                       )
                                    )
                              )
                        )
                        .tag(new TagKey[]{AeroTags.BlockTags.ENVELOPE})
                        .transform(TagGen.axeOnly()))
                     .transform(EncasingRegistry.addVariantTo(AllBlocks.SHAFT)))
                  .transform(VisibilityType.INVISIBLE.applyBlock()))
               .item()
               .tag(new TagKey[]{AeroTags.ItemTags.ENVELOPE})
               .transform(
                  b -> (BlockBuilder)b.model(
                           SimBlockStateGen.coloredBlockItemModel("envelope_block/envelope_" + colorName, new String[]{"envelope_encased_shaft/item"})
                        )
                        .build()
               ))
            .register();
      }
   );
   public static final BlockEntry<HotAirBurnerBlock> HOT_AIR_BURNER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "adjustable_burner", HotAirBurnerBlock::new
               )
               .lang("Hot Air Burner")
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
               .properties(Properties::noOcclusion)
               .properties(p -> p.lightLevel(HotAirBurnerBlock::getLightPower))
               .blockstate(
                  (ctx, prov) -> BlockStateGen.simpleBlock(
                        ctx,
                        prov,
                        blockState -> prov.models()
                              .getExistingFile(
                                 prov.modLoc(
                                    "block/"
                                       + ctx.getName()
                                       + "/block_"
                                       + ((HotAirBurnerBlock.Variant)blockState.getValue(HotAirBurnerBlock.VARIANT)).getSerializedName()
                                 )
                              )
                     )
               )
               .transform(DisplaySource.displaySource(AeroDisplaySources.GAS_DISPLAY)))
            .transform(TagGen.pickaxeOnly()))
         .item()
         .transform(ModelGen.customItemModel()))
      .recipe(
         (c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)c.get(), 1)
               .pattern("S S")
               .pattern("SCS")
               .pattern("ARA")
               .define('S', (ItemLike)AllItems.IRON_SHEET.get())
               .define('A', (ItemLike)AllItems.ANDESITE_ALLOY.get())
               .define('C', AeroTags.ItemTags.BURNER_FIRE)
               .define('R', Items.REDSTONE)
               .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(Items.REDSTONE))
               .save(p)
      )
      .register();
   public static final BlockEntry<SteamVentBlock> STEAM_VENT = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("steam_vent", SteamVentBlock::new)
            .initialProperties(SharedProperties::stone)
            .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
            .properties(Properties::noOcclusion)
            .blockstate(
               (ctx, prov) -> prov.horizontalBlock(
                     (Block)ctx.get(),
                     blockState -> prov.models()
                           .getExistingFile(
                              prov.modLoc(
                                 "block/"
                                    + ctx.getName()
                                    + "/block_"
                                    + ((SteamVentBlock.Variant)blockState.getValue(SteamVentBlock.VARIANT)).getSerializedName()
                              )
                           )
                  )
            )
            .item()
            .transform(ModelGen.customItemModel()))
         .transform(DisplaySource.displaySource(AeroDisplaySources.GAS_DISPLAY)))
      .tag(new TagKey[]{BlockTags.MINEABLE_WITH_PICKAXE})
      .recipe(
         (c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)c.get(), 1)
               .pattern("G")
               .pattern("C")
               .define('G', AeroTags.ItemTags.GOLD_SHEET)
               .define('C', Blocks.COPPER_BLOCK)
               .unlockedBy("has_ingredient", RegistrateRecipeProvider.has(Items.COPPER_INGOT))
               .save(p)
      )
      .register();
   public static final BlockEntry<PropellerBearingBlock> PROPELLER_BEARING = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "propeller_bearing", PropellerBearingBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.sound(SoundType.COPPER))
               .properties(Properties::noOcclusion)
               .transform(AeroStress.setImpact(2.0)))
            .blockstate((ctx, prov) -> SimBlockStateGen.facingBlockstate(ctx, prov, "block/propeller_bearing/block"))
            .transform(TagGen.axeOrPickaxe()))
         .item()
         .transform(ModelGen.customItemModel()))
      .recipe(
         (c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)c.get(), 1)
               .pattern(" A ")
               .pattern(" S ")
               .pattern(" B ")
               .define('A', ItemTags.WOODEN_SLABS)
               .define('B', (ItemLike)AllBlocks.BRASS_CASING.get())
               .define('S', (ItemLike)AllItems.IRON_SHEET.get())
               .unlockedBy("has_ingredient", RegistrateRecipeProvider.has((ItemLike)AllBlocks.BRASS_CASING.get()))
               .save(p)
      )
      .register();
   public static final BlockEntry<GyroscopicPropellerBearingBlock> GYROSCOPIC_PROPELLER_BEARING = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "gyroscopic_propeller_bearing", GyroscopicPropellerBearingBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .properties(p -> p.sound(SoundType.COPPER))
               .transform(AeroStress.setImpact(2.0)))
            .properties(Properties::noOcclusion)
            .blockstate((ctx, prov) -> SimBlockStateGen.facingBlockstate(ctx, prov, "block/gyroscopic_propeller_bearing/block"))
            .transform(TagGen.axeOrPickaxe()))
         .item()
         .transform(ModelGen.customItemModel()))
      .recipe(
         (c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)c.get(), 1)
               .pattern(" A ")
               .pattern(" G ")
               .pattern(" B ")
               .define('A', ItemTags.WOODEN_SLABS)
               .define('B', (ItemLike)AllBlocks.BRASS_CASING.get())
               .define('G', (ItemLike)SimItems.GYRO_MECHANISM.get())
               .unlockedBy("has_ingredient", RegistrateRecipeProvider.has((ItemLike)AllBlocks.BRASS_CASING.get()))
               .save(p)
      )
      .register();
   public static final BlockEntry<SmartPropellerBlock> SMART_PROPELLER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "smart_propeller", SmartPropellerBlock::new
               )
               .initialProperties(SharedProperties::softMetal)
               .transform(TagGen.axeOrPickaxe()))
            .transform(AeroStress.setImpact(4.0)))
         .blockstate(
            (ctx, prov) -> prov.getVariantBuilder((Block)ctx.getEntry())
                  .forAllStates(
                     state -> ConfiguredModel.builder()
                           .modelFile(AssetLookup.partialBaseModel(ctx, prov, new String[0]))
                           .rotationY(state.getValue(BlockStateProperties.HORIZONTAL_AXIS) == Axis.X ? 90 : 0)
                           .rotationX(state.getValue(SmartPropellerBlock.CEILING) ? 180 : 0)
                           .build()
                  )
         )
         .item()
         .transform(ModelGen.customItemModel()))
      .recipe(
         (c, p) -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)c.get(), 2)
               .pattern("P")
               .pattern("G")
               .pattern("B")
               .define('P', AllItems.PROPELLER)
               .define('G', SimItems.GYRO_MECHANISM)
               .define('B', AllBlocks.BRASS_CASING)
               .unlockedBy("has_ingredient", RegistrateRecipeProvider.has((ItemLike)SimItems.GYRO_MECHANISM.get()))
               .save(p)
      )
      .register();
   public static final BlockEntry<AndesitePropellerBlock> ANDESITE_PROPELLER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "andesite_propeller", AndesitePropellerBlock::new
               )
               .initialProperties(SharedProperties::wooden)
               .transform(TagGen.axeOrPickaxe()))
            .properties(p -> p.sound(SoundType.WOOD))
            .transform(AeroStress.setImpact(4.0)))
         .blockstate(BlockStateGen.directionalBlockProvider(true))
         .item()
         .transform(ModelGen.customItemModel()))
      .recipe(
         (c, p) -> {
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, (ItemLike)c.get(), 1)
               .requires((ItemLike)AeroBlocks.WOODEN_PROPELLER.get())
               .unlockedBy("has_ingredient", RegistrateRecipeProvider.has((ItemLike)AllItems.PROPELLER.get()))
               .save(p, Aeronautics.path(c.getName() + "_from_andesite"));
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)c.get(), 1)
               .pattern("P")
               .pattern("C")
               .pattern("S")
               .define('P', AllItems.PROPELLER)
               .define('C', ItemTags.WOODEN_SLABS)
               .define('S', AllBlocks.SHAFT)
               .unlockedBy("has_ingredient", RegistrateRecipeProvider.has((ItemLike)AllItems.PROPELLER.get()))
               .save(p);
         }
      )
      .register();
   public static final BlockEntry<WoodenPropellerBlock> WOODEN_PROPELLER = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "wooden_propeller", WoodenPropellerBlock::new
               )
               .initialProperties(SharedProperties::wooden)
               .transform(TagGen.axeOrPickaxe()))
            .properties(p -> p.sound(SoundType.WOOD))
            .transform(AeroStress.setImpact(4.0)))
         .blockstate(BlockStateGen.directionalBlockProvider(true))
         .recipe(
            (c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, (ItemLike)c.get(), 1)
                  .requires((ItemLike)ANDESITE_PROPELLER.get())
                  .unlockedBy("has_ingredient", RegistrateRecipeProvider.has((ItemLike)AllItems.PROPELLER.get()))
                  .save(p, Aeronautics.path(c.getName() + "_from_andesite"))
         )
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<MountedPotatoCannonBlock> MOUNTED_POTATO_CANNON = ((BlockBuilder)((BlockBuilder)((BlockBuilder)REGISTRATE.block(
                  "mounted_potato_cannon", MountedPotatoCannonBlock::new
               )
               .initialProperties(SharedProperties::stone)
               .blockstate(AeroBlockStateGen::directionalPoweredAxisBlockstate)
               .properties(Properties::noOcclusion)
               .transform(AeroStress.setImpact(2.0)))
            .transform(TagGen.pickaxeOnly()))
         .item()
         .transform(ModelGen.customItemModel()))
      .register();
   public static final BlockEntry<Block> LEVITITE = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("levitite", Block::new)
            .properties(p -> p.lightLevel($ -> 10))
            .properties(Properties::noLootTable)
            .properties(p -> p.strength(7.0F, 20.0F))
            .properties(
               p -> p.sound(
                     new SimLazySoundType(
                        1.0F,
                        1.0F,
                        AeroSoundEvents.LEVITITE_BREAK::event,
                        () -> SoundEvents.AMETHYST_BLOCK_STEP,
                        AeroSoundEvents.LEVITITE_PLACE::event,
                        () -> SoundEvents.AMETHYST_BLOCK_HIT,
                        () -> SoundEvents.AMETHYST_BLOCK_FALL
                     )
                  )
            )
            .tag(new TagKey[]{BlockTags.MINEABLE_WITH_PICKAXE, AeroTags.BlockTags.LEVITITE})
            .onRegister(CreateRegistrate.connectedTextures(() -> new SimpleCTBehaviour(AeroSpriteShift.LEVITITE))))
         .tag(new TagKey[]{SableTags.ALWAYS_CHUNK_RENDERING})
         .item(BlockItem::new)
         .tag(new TagKey[]{AeroTags.ItemTags.LEVITITE})
         .properties(p -> p.component(AeroDataComponents.LEVITATING, Levitating.LEVITITE))
         .build())
      .register();
   public static final BlockEntry<Block> PEARLESCENT_LEVITITE = ((BlockBuilder)((BlockBuilder)REGISTRATE.block("pearlescent_levitite", Block::new)
            .properties(p -> p.lightLevel($ -> 10))
            .properties(Properties::noLootTable)
            .properties(p -> p.strength(7.0F, 20.0F))
            .properties(
               p -> p.sound(
                     new SimLazySoundType(
                        1.0F,
                        1.0F,
                        AeroSoundEvents.LEVITITE_BREAK::event,
                        () -> SoundEvents.AMETHYST_BLOCK_STEP,
                        AeroSoundEvents.LEVITITE_PLACE::event,
                        () -> SoundEvents.AMETHYST_BLOCK_HIT,
                        () -> SoundEvents.AMETHYST_BLOCK_FALL
                     )
                  )
            )
            .tag(new TagKey[]{BlockTags.MINEABLE_WITH_PICKAXE, AeroTags.BlockTags.LEVITITE})
            .onRegister(CreateRegistrate.connectedTextures(() -> new SimpleCTBehaviour(AeroSpriteShift.PEARLESCENT_LEVITITE))))
         .tag(new TagKey[]{SableTags.ALWAYS_CHUNK_RENDERING})
         .item(BlockItem::new)
         .tag(new TagKey[]{AeroTags.ItemTags.LEVITITE})
         .properties(p -> p.component(AeroDataComponents.LEVITATING, Levitating.PEARLESCENT_LEVITITE))
         .build())
      .register();

   private static Boolean neverSpawn(BlockState state, BlockGetter blockGetter, BlockPos pos, EntityType<?> entity) {
      return false;
   }

   private static <B extends Block, R> NonNullUnaryOperator<BlockBuilder<B, R>> flammable(int encouragement, int flamability) {
      return builder -> (BlockBuilder)builder.onRegisterAfter(
            Registries.BLOCK, block -> ((FireBlock)Blocks.FIRE).setFlammable(block, encouragement, flamability)
         );
   }

   public static void init() {
   }
}
