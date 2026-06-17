package com.simibubi.create.foundation.block;

import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.providers.loot.RegistrateBlockLootTables;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.core.Holder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WeatheringCopperFullBlock;
import net.minecraft.world.level.block.WeatheringCopperSlabBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.apache.commons.lang3.ArrayUtils;

public class CopperBlockSet {
   protected static final WeatherState[] WEATHER_STATES = WeatherState.values();
   protected static final int WEATHER_STATE_COUNT = WEATHER_STATES.length;
   protected static final Map<WeatherState, Supplier<Block>> BASE_BLOCKS = new EnumMap<>(WeatherState.class);
   public static final CopperBlockSet.Variant<?>[] DEFAULT_VARIANTS = new CopperBlockSet.Variant[]{
      CopperBlockSet.BlockVariant.INSTANCE, CopperBlockSet.SlabVariant.INSTANCE, CopperBlockSet.StairVariant.INSTANCE
   };
   protected final String name;
   protected final String generalDirectory;
   protected final CopperBlockSet.Variant<?>[] variants;
   protected final Map<CopperBlockSet.Variant<?>, BlockEntry<?>[]> entries = new HashMap<>();
   protected final NonNullBiConsumer<DataGenContext<Block, ?>, RegistrateRecipeProvider> mainBlockRecipe;
   protected final String endTextureName;
   protected final NonNullBiConsumer<WeatherState, Block> onRegister;

   public CopperBlockSet(AbstractRegistrate<?> registrate, String name, String endTextureName, CopperBlockSet.Variant<?>[] variants) {
      this(registrate, name, endTextureName, variants, NonNullBiConsumer.noop(), "copper/", NonNullBiConsumer.noop());
   }

   public CopperBlockSet(AbstractRegistrate<?> registrate, String name, String endTextureName, CopperBlockSet.Variant<?>[] variants, String generalDirectory) {
      this(registrate, name, endTextureName, variants, NonNullBiConsumer.noop(), generalDirectory, NonNullBiConsumer.noop());
   }

   public CopperBlockSet(
      AbstractRegistrate<?> registrate,
      String name,
      String endTextureName,
      CopperBlockSet.Variant<?>[] variants,
      NonNullBiConsumer<DataGenContext<Block, ?>, RegistrateRecipeProvider> mainBlockRecipe
   ) {
      this(registrate, name, endTextureName, variants, mainBlockRecipe, "copper/", NonNullBiConsumer.noop());
   }

   public CopperBlockSet(
      AbstractRegistrate<?> registrate,
      String name,
      String endTextureName,
      CopperBlockSet.Variant<?>[] variants,
      NonNullBiConsumer<DataGenContext<Block, ?>, RegistrateRecipeProvider> mainBlockRecipe,
      NonNullBiConsumer<WeatherState, Block> onRegister
   ) {
      this(registrate, name, endTextureName, variants, mainBlockRecipe, "copper/", onRegister);
   }

   public CopperBlockSet(
      AbstractRegistrate<?> registrate,
      String name,
      String endTextureName,
      CopperBlockSet.Variant<?>[] variants,
      NonNullBiConsumer<DataGenContext<Block, ?>, RegistrateRecipeProvider> mainBlockRecipe,
      String generalDirectory,
      NonNullBiConsumer<WeatherState, Block> onRegister
   ) {
      this.name = name;
      this.generalDirectory = generalDirectory;
      this.endTextureName = endTextureName;
      this.variants = variants;
      this.mainBlockRecipe = mainBlockRecipe;
      this.onRegister = onRegister;

      for (boolean waxed : Iterate.falseAndTrue) {
         for (CopperBlockSet.Variant<?> variant : this.variants) {
            BlockEntry<?>[] entries = waxed ? this.entries.get(variant) : new BlockEntry[WEATHER_STATE_COUNT * 2];

            for (WeatherState state : WEATHER_STATES) {
               int index = this.getIndex(state, waxed);
               BlockEntry<?> entry = this.createEntry(registrate, variant, state, waxed);
               entries[index] = entry;
               if (waxed) {
                  CopperRegistries.addWaxable((Holder<Block>)entries[this.getIndex(state, false)], (Holder<Block>)entry);
               } else if (state != WeatherState.UNAFFECTED) {
                  CopperRegistries.addWeathering((Holder<Block>)entries[this.getIndex(WEATHER_STATES[state.ordinal() - 1], false)], (Holder<Block>)entry);
               }
            }

            if (!waxed) {
               this.entries.put(variant, entries);
            }
         }
      }
   }

   protected <T extends Block> BlockEntry<?> createEntry(AbstractRegistrate<?> registrate, CopperBlockSet.Variant<T> variant, WeatherState state, boolean waxed) {
      String name = "";
      if (waxed) {
         name = name + "waxed_";
      }

      name = name + getWeatherStatePrefix(state);
      name = name + this.name;
      String suffix = variant.getSuffix();
      if (!suffix.equals("")) {
         name = Lang.nonPluralId(name);
      }

      name = name + suffix;
      Supplier<Block> baseBlock = BASE_BLOCKS.get(state);
      BlockBuilder<T, ?> builder = ((BlockBuilder)((BlockBuilder)registrate.block(name, variant.getFactory(this, state, waxed))
               .initialProperties(() -> baseBlock.get())
               .loot((lt, block) -> variant.generateLootTable(lt, (T)block, this, state, waxed))
               .blockstate((ctx, prov) -> variant.generateBlockState(ctx, prov, this, state, waxed))
               .transform(TagGen.pickaxeOnly()))
            .onRegister(block -> this.onRegister.accept(state, block)))
         .tag(new TagKey[]{BlockTags.NEEDS_STONE_TOOL})
         .simpleItem();
      if (variant == CopperBlockSet.BlockVariant.INSTANCE && state == WeatherState.UNAFFECTED && !waxed) {
         builder.recipe(this.mainBlockRecipe::accept);
      } else {
         builder.recipe(
            (ctx, prov) -> {
               if (waxed) {
                  Block unwaxed = (Block)this.get(variant, state, false).get();
                  ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, (ItemLike)ctx.get())
                     .requires(unwaxed)
                     .requires(Items.HONEYCOMB)
                     .unlockedBy("has_unwaxed", RegistrateRecipeProvider.has(unwaxed))
                     .save(
                        prov,
                        ResourceLocation.fromNamespaceAndPath(
                           ctx.getId().getNamespace(), "crafting/" + this.generalDirectory + ctx.getName() + "_from_honeycomb"
                        )
                     );
               }

               variant.generateRecipes(this.get(CopperBlockSet.BlockVariant.INSTANCE, state, waxed), ctx, prov);
            }
         );
      }

      if (variant == CopperBlockSet.StairVariant.INSTANCE) {
         builder.tag(new TagKey[]{BlockTags.STAIRS});
      }

      if (variant == CopperBlockSet.SlabVariant.INSTANCE) {
         builder.tag(new TagKey[]{BlockTags.SLABS});
      }

      return builder.register();
   }

   protected int getIndex(WeatherState state, boolean waxed) {
      return state.ordinal() + (waxed ? WEATHER_STATE_COUNT : 0);
   }

   public String getName() {
      return this.name;
   }

   public String getEndTextureName() {
      return this.endTextureName;
   }

   public CopperBlockSet.Variant<?>[] getVariants() {
      return this.variants;
   }

   public boolean hasVariant(CopperBlockSet.Variant<?> variant) {
      return ArrayUtils.contains(this.variants, variant);
   }

   public BlockEntry<?> get(CopperBlockSet.Variant<?> variant, WeatherState state, boolean waxed) {
      BlockEntry<?>[] entries = this.entries.get(variant);
      return entries != null ? entries[this.getIndex(state, waxed)] : null;
   }

   public BlockEntry<?> getStandard() {
      return this.get(CopperBlockSet.BlockVariant.INSTANCE, WeatherState.UNAFFECTED, false);
   }

   public static String getWeatherStatePrefix(WeatherState state) {
      return state != WeatherState.UNAFFECTED ? state.name().toLowerCase(Locale.ROOT) + "_" : "";
   }

   static {
      BASE_BLOCKS.put(WeatherState.UNAFFECTED, () -> Blocks.COPPER_BLOCK);
      BASE_BLOCKS.put(WeatherState.EXPOSED, () -> Blocks.EXPOSED_COPPER);
      BASE_BLOCKS.put(WeatherState.WEATHERED, () -> Blocks.WEATHERED_COPPER);
      BASE_BLOCKS.put(WeatherState.OXIDIZED, () -> Blocks.OXIDIZED_COPPER);
   }

   public static class BlockVariant implements CopperBlockSet.Variant<Block> {
      public static final CopperBlockSet.BlockVariant INSTANCE = new CopperBlockSet.BlockVariant();

      protected BlockVariant() {
      }

      @Override
      public String getSuffix() {
         return "";
      }

      @Override
      public NonNullFunction<Properties, Block> getFactory(CopperBlockSet blocks, WeatherState state, boolean waxed) {
         return waxed ? Block::new : p -> new WeatheringCopperFullBlock(state, p);
      }

      @Override
      public void generateBlockState(
         DataGenContext<Block, Block> ctx, RegistrateBlockstateProvider prov, CopperBlockSet blocks, WeatherState state, boolean waxed
      ) {
         Block block = (Block)ctx.get();
         String path = RegisteredObjectsHelper.getKeyOrThrow(block).getPath();
         String baseLoc = "block/" + blocks.generalDirectory + CopperBlockSet.getWeatherStatePrefix(state);
         ResourceLocation texture = prov.modLoc(baseLoc + blocks.getName());
         if (Objects.equals(blocks.getName(), blocks.getEndTextureName())) {
            prov.simpleBlock(block, prov.models().cubeAll(path, texture));
         } else {
            ResourceLocation endTexture = prov.modLoc(baseLoc + blocks.getEndTextureName());
            prov.simpleBlock(block, prov.models().cubeColumn(path, texture, endTexture));
         }
      }

      @Override
      public void generateRecipes(BlockEntry<?> blockVariant, DataGenContext<Block, Block> ctx, RegistrateRecipeProvider prov) {
      }
   }

   public static class SlabVariant implements CopperBlockSet.Variant<SlabBlock> {
      public static final CopperBlockSet.SlabVariant INSTANCE = new CopperBlockSet.SlabVariant();

      protected SlabVariant() {
      }

      @Override
      public String getSuffix() {
         return "_slab";
      }

      @Override
      public NonNullFunction<Properties, SlabBlock> getFactory(CopperBlockSet blocks, WeatherState state, boolean waxed) {
         return waxed ? SlabBlock::new : p -> new WeatheringCopperSlabBlock(state, p);
      }

      public void generateLootTable(RegistrateBlockLootTables lootTable, SlabBlock block, CopperBlockSet blocks, WeatherState state, boolean waxed) {
         lootTable.add(block, lootTable.createSlabItemTable(block));
      }

      @Override
      public void generateBlockState(
         DataGenContext<Block, SlabBlock> ctx, RegistrateBlockstateProvider prov, CopperBlockSet blocks, WeatherState state, boolean waxed
      ) {
         ResourceLocation fullModel = prov.modLoc("block/" + CopperBlockSet.getWeatherStatePrefix(state) + blocks.getName());
         String baseLoc = "block/" + blocks.generalDirectory + CopperBlockSet.getWeatherStatePrefix(state);
         ResourceLocation texture = prov.modLoc(baseLoc + blocks.getName());
         ResourceLocation endTexture = prov.modLoc(baseLoc + blocks.getEndTextureName());
         prov.slabBlock((SlabBlock)ctx.get(), fullModel, texture, endTexture, endTexture);
      }

      @Override
      public void generateRecipes(BlockEntry<?> blockVariant, DataGenContext<Block, SlabBlock> ctx, RegistrateRecipeProvider prov) {
         prov.slab(DataIngredient.items((Block)blockVariant.get(), new Block[0]), RecipeCategory.BUILDING_BLOCKS, ctx::get, null, true);
      }
   }

   public static class StairVariant implements CopperBlockSet.Variant<StairBlock> {
      public static final CopperBlockSet.StairVariant INSTANCE = new CopperBlockSet.StairVariant(CopperBlockSet.BlockVariant.INSTANCE);
      protected final CopperBlockSet.Variant<?> parent;

      protected StairVariant(CopperBlockSet.Variant<?> parent) {
         this.parent = parent;
      }

      @Override
      public String getSuffix() {
         return "_stairs";
      }

      @Override
      public NonNullFunction<Properties, StairBlock> getFactory(CopperBlockSet blocks, WeatherState state, boolean waxed) {
         if (!blocks.hasVariant(this.parent)) {
            throw new IllegalStateException("Cannot add StairVariant '" + this + "' without parent Variant '" + this.parent.toString() + "'!");
         } else {
            return waxed ? CreateCopperStairBlock::new : p -> new CreateWeatheringCopperStairBlock(state, p);
         }
      }

      @Override
      public void generateBlockState(
         DataGenContext<Block, StairBlock> ctx, RegistrateBlockstateProvider prov, CopperBlockSet blocks, WeatherState state, boolean waxed
      ) {
         String baseLoc = "block/" + blocks.generalDirectory + CopperBlockSet.getWeatherStatePrefix(state);
         ResourceLocation texture = prov.modLoc(baseLoc + blocks.getName());
         ResourceLocation endTexture = prov.modLoc(baseLoc + blocks.getEndTextureName());
         prov.stairsBlock((StairBlock)ctx.get(), texture, endTexture, endTexture);
      }

      @Override
      public void generateRecipes(BlockEntry<?> blockVariant, DataGenContext<Block, StairBlock> ctx, RegistrateRecipeProvider prov) {
         prov.stairs(DataIngredient.items((Block)blockVariant.get(), new Block[0]), RecipeCategory.BUILDING_BLOCKS, ctx::get, null, true);
      }
   }

   public interface Variant<T extends Block> {
      String getSuffix();

      NonNullFunction<Properties, T> getFactory(CopperBlockSet var1, WeatherState var2, boolean var3);

      default void generateLootTable(RegistrateBlockLootTables lootTable, T block, CopperBlockSet blocks, WeatherState state, boolean waxed) {
         lootTable.dropSelf(block);
      }

      void generateRecipes(BlockEntry<?> var1, DataGenContext<Block, T> var2, RegistrateRecipeProvider var3);

      void generateBlockState(DataGenContext<Block, T> var1, RegistrateBlockstateProvider var2, CopperBlockSet var3, WeatherState var4, boolean var5);
   }
}
