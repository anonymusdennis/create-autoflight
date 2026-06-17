package com.simibubi.create.foundation.data.recipe;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.content.decoration.palettes.AllPaletteStoneTypes;
import com.simibubi.create.content.equipment.toolbox.ToolboxDyeingRecipe;
import com.simibubi.create.foundation.mixin.accessor.MappedRegistryAccessor;
import com.simibubi.create.foundation.recipe.ItemCopyingRecipe;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemPredicate.Builder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.AbstractCookingRecipe.Factory;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import org.jetbrains.annotations.Nullable;

public final class CreateStandardRecipeGen extends BaseRecipeProvider {
   final List<BaseRecipeProvider.GeneratedRecipe> all = new ArrayList<>();
   private CreateStandardRecipeGen.Marker MATERIALS = this.enterFolder("materials");
   BaseRecipeProvider.GeneratedRecipe RAW_ZINC = this.create(AllItems.RAW_ZINC)
      .returns(9)
      .unlockedBy(AllBlocks.RAW_ZINC_BLOCK::get)
      .viaShapeless(b -> b.requires((ItemLike)AllBlocks.RAW_ZINC_BLOCK.get()));
   BaseRecipeProvider.GeneratedRecipe RAW_ZINC_BLOCK = this.create(AllBlocks.RAW_ZINC_BLOCK)
      .unlockedBy(AllItems.RAW_ZINC::get)
      .viaShaped(b -> b.define('C', (ItemLike)AllItems.RAW_ZINC.get()).pattern("CCC").pattern("CCC").pattern("CCC"));
   BaseRecipeProvider.GeneratedRecipe COPPER_NUGGET = this.create(AllItems.COPPER_NUGGET)
      .returns(9)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.copper()));
   BaseRecipeProvider.GeneratedRecipe COPPER_INGOT = this.create((Supplier<ItemLike>)(() -> Items.COPPER_INGOT))
      .unlockedByTag(CreateRecipeProvider.I::copperNugget)
      .viaShaped(b -> b.define('C', CreateRecipeProvider.I.copperNugget()).pattern("CCC").pattern("CCC").pattern("CCC"));
   BaseRecipeProvider.GeneratedRecipe ANDESITE_ALLOY_FROM_BLOCK = this.create(AllItems.ANDESITE_ALLOY)
      .withSuffix("_from_block")
      .returns(9)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShapeless(b -> b.requires((ItemLike)AllBlocks.ANDESITE_ALLOY_BLOCK.get()));
   BaseRecipeProvider.GeneratedRecipe ANDESITE_ALLOY_BLOCK = this.create(AllBlocks.ANDESITE_ALLOY_BLOCK)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('C', CreateRecipeProvider.I.andesiteAlloy()).pattern("CCC").pattern("CCC").pattern("CCC"));
   BaseRecipeProvider.GeneratedRecipe EXPERIENCE_FROM_BLOCK = this.create(AllItems.EXP_NUGGET)
      .withSuffix("_from_block")
      .returns(9)
      .unlockedBy(AllItems.EXP_NUGGET::get)
      .viaShapeless(b -> b.requires((ItemLike)AllBlocks.EXPERIENCE_BLOCK.get()));
   BaseRecipeProvider.GeneratedRecipe EXPERIENCE_BLOCK = this.create(AllBlocks.EXPERIENCE_BLOCK)
      .unlockedBy(AllItems.EXP_NUGGET::get)
      .viaShaped(b -> b.define('C', (ItemLike)AllItems.EXP_NUGGET.get()).pattern("CCC").pattern("CCC").pattern("CCC"));
   BaseRecipeProvider.GeneratedRecipe CARDBOARD_BLOCK = this.create(AllBlocks.CARDBOARD_BLOCK)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(b -> b.define('C', CreateRecipeProvider.I.cardboard()).pattern("CC").pattern("CC"));
   BaseRecipeProvider.GeneratedRecipe BOUND_CARDBOARD_BLOCK = this.create(AllBlocks.BOUND_CARDBOARD_BLOCK)
      .returns(1)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShapeless(b -> b.requires((ItemLike)AllBlocks.CARDBOARD_BLOCK.get()).requires(net.neoforged.neoforge.common.Tags.Items.STRINGS));
   BaseRecipeProvider.GeneratedRecipe CARDBOARD_FROM_BLOCK = this.create(AllItems.CARDBOARD)
      .withSuffix("_from_block")
      .returns(4)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShapeless(b -> b.requires((ItemLike)AllBlocks.CARDBOARD_BLOCK.get()));
   BaseRecipeProvider.GeneratedRecipe CARDBOARD_FROM_BOUND_BLOCK = this.create(AllItems.CARDBOARD)
      .withSuffix("_from_bound_block")
      .returns(4)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShapeless(b -> b.requires((ItemLike)AllBlocks.BOUND_CARDBOARD_BLOCK.get()));
   BaseRecipeProvider.GeneratedRecipe BRASS_COMPACTING = this.metalCompacting(
      ImmutableList.of(AllItems.BRASS_NUGGET, AllItems.BRASS_INGOT, AllBlocks.BRASS_BLOCK),
      ImmutableList.of(CreateRecipeProvider.I::brassNugget, CreateRecipeProvider.I::brass, CreateRecipeProvider.I::brassBlock)
   );
   BaseRecipeProvider.GeneratedRecipe ZINC_COMPACTING = this.metalCompacting(
      ImmutableList.of(AllItems.ZINC_NUGGET, AllItems.ZINC_INGOT, AllBlocks.ZINC_BLOCK),
      ImmutableList.of(CreateRecipeProvider.I::zincNugget, CreateRecipeProvider.I::zinc, CreateRecipeProvider.I::zincBlock)
   );
   BaseRecipeProvider.GeneratedRecipe ROSE_QUARTZ_CYCLE = this.conversionCycle(ImmutableList.of(AllBlocks.ROSE_QUARTZ_TILES, AllBlocks.SMALL_ROSE_QUARTZ_TILES));
   BaseRecipeProvider.GeneratedRecipe ANDESITE_ALLOY = this.create(AllItems.ANDESITE_ALLOY)
      .unlockedByTag(CreateRecipeProvider.I::iron)
      .viaShaped(b -> b.define('A', Blocks.ANDESITE).define('B', net.neoforged.neoforge.common.Tags.Items.NUGGETS_IRON).pattern("BA").pattern("AB"));
   BaseRecipeProvider.GeneratedRecipe ANDESITE_ALLOY_FROM_ZINC = this.create(AllItems.ANDESITE_ALLOY)
      .withSuffix("_from_zinc")
      .unlockedByTag(CreateRecipeProvider.I::zinc)
      .viaShaped(b -> b.define('A', Blocks.ANDESITE).define('B', CreateRecipeProvider.I.zincNugget()).pattern("BA").pattern("AB"));
   BaseRecipeProvider.GeneratedRecipe ELECTRON_TUBE = this.create(AllItems.ELECTRON_TUBE)
      .unlockedBy(AllItems.ROSE_QUARTZ::get)
      .viaShaped(b -> b.define('L', (ItemLike)AllItems.POLISHED_ROSE_QUARTZ.get()).define('N', CreateRecipeProvider.I.ironSheet()).pattern("L").pattern("N"));
   BaseRecipeProvider.GeneratedRecipe TRANSMITTER = this.create(AllItems.TRANSMITTER)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShaped(
         b -> b.define('L', CreateRecipeProvider.I.copperSheet())
               .define('N', Items.LIGHTNING_ROD)
               .define('R', CreateRecipeProvider.I.redstone())
               .pattern(" N ")
               .pattern("LLL")
               .pattern(" R ")
      );
   BaseRecipeProvider.GeneratedRecipe ROSE_QUARTZ = this.create(AllItems.ROSE_QUARTZ)
      .unlockedBy(() -> Items.REDSTONE)
      .viaShapeless(b -> b.requires(net.neoforged.neoforge.common.Tags.Items.GEMS_QUARTZ).requires(Ingredient.of(CreateRecipeProvider.I.redstone()), 8));
   BaseRecipeProvider.GeneratedRecipe SAND_PAPER = this.create(AllItems.SAND_PAPER)
      .unlockedBy(() -> Items.PAPER)
      .viaShapeless(b -> b.requires(Items.PAPER).requires(net.neoforged.neoforge.common.Tags.Items.SANDS_COLORLESS));
   BaseRecipeProvider.GeneratedRecipe RED_SAND_PAPER = this.create(AllItems.RED_SAND_PAPER)
      .unlockedBy(() -> Items.PAPER)
      .viaShapeless(b -> b.requires(Items.PAPER).requires(net.neoforged.neoforge.common.Tags.Items.SANDS_RED));
   private CreateStandardRecipeGen.Marker CURIOSITIES = this.enterFolder("curiosities");
   BaseRecipeProvider.GeneratedRecipe TOOLBOX = this.create(AllBlocks.TOOLBOXES.get(DyeColor.BROWN))
      .unlockedByTag(CreateRecipeProvider.I::goldSheet)
      .viaShaped(
         b -> b.define('S', CreateRecipeProvider.I.goldSheet())
               .define('C', CreateRecipeProvider.I.cog())
               .define('W', net.neoforged.neoforge.common.Tags.Items.CHESTS_WOODEN)
               .define('L', net.neoforged.neoforge.common.Tags.Items.LEATHERS)
               .pattern(" C ")
               .pattern("SWS")
               .pattern(" L ")
      );
   BaseRecipeProvider.GeneratedRecipe TOOLBOX_DYEING = this.createSpecial(ToolboxDyeingRecipe::new, "crafting", "toolbox_dyeing");
   BaseRecipeProvider.GeneratedRecipe ITEM_COPYING = this.createSpecial(ItemCopyingRecipe::new, "crafting", "item_copying");
   BaseRecipeProvider.GeneratedRecipe MINECART_COUPLING = this.create(AllItems.MINECART_COUPLING)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('E', CreateRecipeProvider.I.andesiteAlloy())
               .define('O', CreateRecipeProvider.I.ironSheet())
               .pattern("  E")
               .pattern(" O ")
               .pattern("E  ")
      );
   BaseRecipeProvider.GeneratedRecipe PECULIAR_BELL = this.create(AllBlocks.PECULIAR_BELL)
      .unlockedByTag(CreateRecipeProvider.I::brass)
      .viaShaped(b -> b.define('I', CreateRecipeProvider.I.brassBlock()).define('P', CreateRecipeProvider.I.brassSheet()).pattern("I").pattern("P"));
   BaseRecipeProvider.GeneratedRecipe CAKE = this.create((Supplier<ItemLike>)(() -> Items.CAKE))
      .unlockedByTag(() -> net.neoforged.neoforge.common.Tags.Items.FOODS_DOUGH)
      .viaShaped(
         b -> b.define('E', net.neoforged.neoforge.common.Tags.Items.EGGS)
               .define('S', Items.SUGAR)
               .define('P', net.neoforged.neoforge.common.Tags.Items.FOODS_DOUGH)
               .define('M', () -> Items.MILK_BUCKET)
               .pattern(" M ")
               .pattern("SES")
               .pattern(" P ")
      );
   private CreateStandardRecipeGen.Marker KINETICS = this.enterFolder("kinetics");
   BaseRecipeProvider.GeneratedRecipe BASIN = this.create(AllBlocks.BASIN)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('A', CreateRecipeProvider.I.andesiteAlloy()).pattern("A A").pattern("AAA"));
   BaseRecipeProvider.GeneratedRecipe GOGGLES = this.create(AllItems.GOGGLES)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('G', net.neoforged.neoforge.common.Tags.Items.GLASS_BLOCKS)
               .define('P', CreateRecipeProvider.I.goldSheet())
               .define('S', net.neoforged.neoforge.common.Tags.Items.STRINGS)
               .pattern(" S ")
               .pattern("GPG")
      );
   BaseRecipeProvider.GeneratedRecipe WRENCH = this.create(AllItems.WRENCH)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('G', CreateRecipeProvider.I.goldSheet())
               .define('P', CreateRecipeProvider.I.cog())
               .define('S', net.neoforged.neoforge.common.Tags.Items.RODS_WOODEN)
               .pattern("GG")
               .pattern("GP")
               .pattern(" S")
      );
   BaseRecipeProvider.GeneratedRecipe FILTER = this.create(AllItems.FILTER)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('S', ItemTags.WOOL).define('A', net.neoforged.neoforge.common.Tags.Items.NUGGETS_IRON).pattern("ASA"));
   BaseRecipeProvider.GeneratedRecipe ATTRIBUTE_FILTER = this.create(AllItems.ATTRIBUTE_FILTER)
      .unlockedByTag(CreateRecipeProvider.I::brass)
      .viaShaped(b -> b.define('S', ItemTags.WOOL).define('A', CreateRecipeProvider.I.brassNugget()).pattern("ASA"));
   BaseRecipeProvider.GeneratedRecipe PACKAGE_FILTER = this.create(AllItems.PACKAGE_FILTER)
      .unlockedByTag(CreateRecipeProvider.I::zinc)
      .viaShaped(b -> b.define('S', ItemTags.WOOL).define('A', CreateRecipeProvider.I.zincNugget()).pattern("ASA"));
   BaseRecipeProvider.GeneratedRecipe BRASS_HAND = this.create(AllItems.BRASS_HAND)
      .unlockedByTag(CreateRecipeProvider.I::brass)
      .viaShaped(
         b -> b.define('A', CreateRecipeProvider.I.andesiteAlloy())
               .define('B', CreateRecipeProvider.I.brassSheet())
               .pattern(" A ")
               .pattern("BBB")
               .pattern(" B ")
      );
   BaseRecipeProvider.GeneratedRecipe SUPER_GLUE = this.create(AllItems.SUPER_GLUE)
      .unlockedByTag(CreateRecipeProvider.I::ironSheet)
      .viaShaped(
         b -> b.define('A', net.neoforged.neoforge.common.Tags.Items.SLIMEBALLS)
               .define('S', CreateRecipeProvider.I.ironSheet())
               .define('N', net.neoforged.neoforge.common.Tags.Items.NUGGETS_IRON)
               .pattern("AS")
               .pattern("NA")
      );
   BaseRecipeProvider.GeneratedRecipe CRAFTER_SLOT_COVER = this.create(AllItems.CRAFTER_SLOT_COVER)
      .unlockedBy(AllBlocks.MECHANICAL_CRAFTER::get)
      .viaShaped(b -> b.define('A', CreateRecipeProvider.I.brassNugget()).pattern("AAA"));
   BaseRecipeProvider.GeneratedRecipe COGWHEEL = this.create(AllBlocks.COGWHEEL)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.shaft()).requires(CreateRecipeProvider.I.planks()));
   BaseRecipeProvider.GeneratedRecipe LARGE_COGWHEEL = this.create(AllBlocks.LARGE_COGWHEEL)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.shaft()).requires(CreateRecipeProvider.I.planks()).requires(CreateRecipeProvider.I.planks()));
   BaseRecipeProvider.GeneratedRecipe LARGE_COGWHEEL_FROM_LITTLE = this.create(AllBlocks.LARGE_COGWHEEL)
      .withSuffix("_from_little")
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.cog()).requires(CreateRecipeProvider.I.planks()));
   BaseRecipeProvider.GeneratedRecipe WATER_WHEEL = this.create(AllBlocks.WATER_WHEEL)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('S', CreateRecipeProvider.I.planks()).define('C', CreateRecipeProvider.I.shaft()).pattern("SSS").pattern("SCS").pattern("SSS"));
   BaseRecipeProvider.GeneratedRecipe LARGE_WATER_WHEEL = this.create(AllBlocks.LARGE_WATER_WHEEL)
      .unlockedBy(AllBlocks.WATER_WHEEL::get)
      .viaShaped(
         b -> b.define('S', CreateRecipeProvider.I.planks()).define('C', (ItemLike)AllBlocks.WATER_WHEEL.get()).pattern("SSS").pattern("SCS").pattern("SSS")
      );
   BaseRecipeProvider.GeneratedRecipe SHAFT = this.create(AllBlocks.SHAFT)
      .returns(8)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('A', CreateRecipeProvider.I.andesiteAlloy()).pattern("A").pattern("A"));
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_PRESS = this.create(AllBlocks.MECHANICAL_PRESS)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('S', CreateRecipeProvider.I.shaft())
               .define('I', net.neoforged.neoforge.common.Tags.Items.STORAGE_BLOCKS_IRON)
               .pattern("S")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe MILLSTONE = this.create(AllBlocks.MILLSTONE)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.cog())
               .define('S', CreateRecipeProvider.I.andesiteCasing())
               .define('I', CreateRecipeProvider.I.stone())
               .pattern("C")
               .pattern("S")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_PISTON = this.create(AllBlocks.MECHANICAL_PISTON)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(
         b -> b.define('B', ItemTags.WOODEN_SLABS)
               .define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('I', (ItemLike)AllBlocks.PISTON_EXTENSION_POLE.get())
               .pattern("B")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe STICKY_MECHANICAL_PISTON = this.create(AllBlocks.STICKY_MECHANICAL_PISTON)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('S', net.neoforged.neoforge.common.Tags.Items.SLIMEBALLS)
               .define('P', (ItemLike)AllBlocks.MECHANICAL_PISTON.get())
               .pattern("S")
               .pattern("P")
      );
   BaseRecipeProvider.GeneratedRecipe TURNTABLE = this.create(AllBlocks.TURNTABLE)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('S', CreateRecipeProvider.I.shaft()).define('P', ItemTags.WOODEN_SLABS).pattern("P").pattern("S"));
   BaseRecipeProvider.GeneratedRecipe PISTON_EXTENSION_POLE = this.create(AllBlocks.PISTON_EXTENSION_POLE)
      .returns(8)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('A', CreateRecipeProvider.I.andesiteAlloy()).define('P', ItemTags.PLANKS).pattern("P").pattern("A").pattern("P"));
   BaseRecipeProvider.GeneratedRecipe GANTRY_PINION = this.create(AllBlocks.GANTRY_CARRIAGE)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(
         b -> b.define('B', ItemTags.WOODEN_SLABS)
               .define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('I', CreateRecipeProvider.I.cog())
               .pattern("B")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe GANTRY_SHAFT = this.create(AllBlocks.GANTRY_SHAFT)
      .returns(8)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('A', CreateRecipeProvider.I.andesiteAlloy()).define('R', CreateRecipeProvider.I.redstone()).pattern("A").pattern("R").pattern("A")
      );
   BaseRecipeProvider.GeneratedRecipe PLACARD = this.create(AllBlocks.PLACARD)
      .returns(1)
      .unlockedByTag(() -> CreateRecipeProvider.I.brass())
      .viaShapeless(b -> b.requires(Items.ITEM_FRAME).requires(CreateRecipeProvider.I.brassSheet()));
   BaseRecipeProvider.GeneratedRecipe TRAIN_DOOR = this.create(AllBlocks.TRAIN_DOOR)
      .returns(1)
      .unlockedBy(() -> CreateRecipeProvider.I.railwayCasing())
      .viaShapeless(b -> b.requires(ItemTags.WOODEN_DOORS).requires(CreateRecipeProvider.I.railwayCasing()));
   BaseRecipeProvider.GeneratedRecipe ANDESITE_DOOR = this.create(AllBlocks.ANDESITE_DOOR)
      .returns(1)
      .unlockedBy(() -> CreateRecipeProvider.I.andesiteCasing())
      .viaShapeless(b -> b.requires(ItemTags.WOODEN_DOORS).requires(CreateRecipeProvider.I.andesiteCasing()));
   BaseRecipeProvider.GeneratedRecipe BRASS_DOOR = this.create(AllBlocks.BRASS_DOOR)
      .returns(1)
      .unlockedBy(() -> CreateRecipeProvider.I.brassCasing())
      .viaShapeless(b -> b.requires(ItemTags.WOODEN_DOORS).requires(CreateRecipeProvider.I.brassCasing()));
   BaseRecipeProvider.GeneratedRecipe COPPER_DOOR = this.create(AllBlocks.COPPER_DOOR)
      .returns(1)
      .unlockedBy(() -> CreateRecipeProvider.I.copperCasing())
      .viaShapeless(b -> b.requires(ItemTags.WOODEN_DOORS).requires(CreateRecipeProvider.I.copperCasing()));
   BaseRecipeProvider.GeneratedRecipe TRAIN_TRAPDOOR = this.create(AllBlocks.TRAIN_TRAPDOOR)
      .returns(1)
      .unlockedBy(() -> CreateRecipeProvider.I.railwayCasing())
      .viaShapeless(b -> b.requires(ItemTags.WOODEN_TRAPDOORS).requires(CreateRecipeProvider.I.railwayCasing()));
   BaseRecipeProvider.GeneratedRecipe FRAMED_GLASS_DOOR = this.create(AllBlocks.FRAMED_GLASS_DOOR)
      .returns(1)
      .unlockedBy(AllPaletteBlocks.FRAMED_GLASS::get)
      .viaShapeless(b -> b.requires(ItemTags.WOODEN_DOORS).requires((ItemLike)AllPaletteBlocks.FRAMED_GLASS.get()));
   BaseRecipeProvider.GeneratedRecipe FRAMED_GLASS_TRAPDOOR = this.create(AllBlocks.FRAMED_GLASS_TRAPDOOR)
      .returns(1)
      .unlockedBy(AllPaletteBlocks.FRAMED_GLASS::get)
      .viaShapeless(b -> b.requires(ItemTags.WOODEN_TRAPDOORS).requires((ItemLike)AllPaletteBlocks.FRAMED_GLASS.get()));
   BaseRecipeProvider.GeneratedRecipe ANALOG_LEVER = this.create(AllBlocks.ANALOG_LEVER)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('S', CreateRecipeProvider.I.andesiteCasing())
               .define('P', net.neoforged.neoforge.common.Tags.Items.RODS_WOODEN)
               .pattern("P")
               .pattern("S")
      );
   BaseRecipeProvider.GeneratedRecipe ROSE_QUARTZ_LAMP = this.create(AllBlocks.ROSE_QUARTZ_LAMP)
      .unlockedByTag(CreateRecipeProvider.I::zinc)
      .viaShapeless(
         b -> b.requires((ItemLike)AllItems.POLISHED_ROSE_QUARTZ.get()).requires(CreateRecipeProvider.I.redstone()).requires(CreateRecipeProvider.I.zinc())
      );
   BaseRecipeProvider.GeneratedRecipe BELT_CONNECTOR = this.create(AllItems.BELT_CONNECTOR)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('D', Items.DRIED_KELP).pattern("DDD").pattern("DDD"));
   BaseRecipeProvider.GeneratedRecipe ADJUSTABLE_PULLEY = this.create(AllBlocks.ADJUSTABLE_CHAIN_GEARSHIFT)
      .unlockedBy(CreateRecipeProvider.I::electronTube)
      .viaShapeless(b -> b.requires((ItemLike)AllBlocks.ENCASED_CHAIN_DRIVE.get()).requires(CreateRecipeProvider.I.electronTube()));
   BaseRecipeProvider.GeneratedRecipe CART_ASSEMBLER = this.create(AllBlocks.CART_ASSEMBLER)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('L', ItemTags.LOGS)
               .define('R', CreateRecipeProvider.I.redstone())
               .define('C', CreateRecipeProvider.I.andesiteAlloy())
               .pattern("CRC")
               .pattern("L L")
      );
   BaseRecipeProvider.GeneratedRecipe CONTROLLER_RAIL = this.create(AllBlocks.CONTROLLER_RAIL)
      .returns(6)
      .unlockedBy(() -> Items.POWERED_RAIL)
      .viaShaped(
         b -> b.define('A', CreateRecipeProvider.I.gold())
               .define('E', CreateRecipeProvider.I.electronTube())
               .define('S', net.neoforged.neoforge.common.Tags.Items.RODS_WOODEN)
               .pattern("A A")
               .pattern("ASA")
               .pattern("AEA")
      );
   BaseRecipeProvider.GeneratedRecipe HAND_CRANK = this.create(AllBlocks.HAND_CRANK)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('A', CreateRecipeProvider.I.andesiteAlloy()).define('C', ItemTags.PLANKS).pattern("CCC").pattern("  A"));
   BaseRecipeProvider.GeneratedRecipe COPPER_VALVE_HANDLE = this.create(AllBlocks.COPPER_VALVE_HANDLE)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShaped(b -> b.define('S', CreateRecipeProvider.I.andesiteAlloy()).define('C', CreateRecipeProvider.I.copperSheet()).pattern("CCC").pattern(" S "));
   BaseRecipeProvider.GeneratedRecipe COPPER_VALVE_HANDLE_FROM_OTHER_HANDLES = this.create(AllBlocks.COPPER_VALVE_HANDLE)
      .withSuffix("_from_others")
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShapeless(b -> b.requires(AllTags.AllItemTags.VALVE_HANDLES.tag));
   BaseRecipeProvider.GeneratedRecipe NOZZLE = this.create(AllBlocks.NOZZLE)
      .unlockedBy(AllBlocks.ENCASED_FAN::get)
      .viaShaped(b -> b.define('S', CreateRecipeProvider.I.andesiteAlloy()).define('C', ItemTags.WOOL).pattern(" S ").pattern(" C ").pattern("SSS"));
   BaseRecipeProvider.GeneratedRecipe PROPELLER = this.create(AllItems.PROPELLER)
      .unlockedByTag(CreateRecipeProvider.I::ironSheet)
      .viaShaped(
         b -> b.define('S', CreateRecipeProvider.I.ironSheet())
               .define('C', CreateRecipeProvider.I.andesiteAlloy())
               .pattern(" S ")
               .pattern("SCS")
               .pattern(" S ")
      );
   BaseRecipeProvider.GeneratedRecipe WHISK = this.create(AllItems.WHISK)
      .unlockedByTag(CreateRecipeProvider.I::ironSheet)
      .viaShaped(
         b -> b.define('S', CreateRecipeProvider.I.ironSheet())
               .define('C', CreateRecipeProvider.I.andesiteAlloy())
               .pattern(" C ")
               .pattern("SCS")
               .pattern("SSS")
      );
   BaseRecipeProvider.GeneratedRecipe ENCASED_FAN = this.create(AllBlocks.ENCASED_FAN)
      .unlockedByTag(CreateRecipeProvider.I::ironSheet)
      .viaShaped(
         b -> b.define('A', CreateRecipeProvider.I.andesiteCasing())
               .define('S', CreateRecipeProvider.I.shaft())
               .define('P', (ItemLike)AllItems.PROPELLER.get())
               .pattern("S")
               .pattern("A")
               .pattern("P")
      );
   BaseRecipeProvider.GeneratedRecipe CUCKOO_CLOCK = this.create(AllBlocks.CUCKOO_CLOCK)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('S', ItemTags.PLANKS)
               .define('A', Items.CLOCK)
               .define('C', CreateRecipeProvider.I.andesiteCasing())
               .pattern("S")
               .pattern("C")
               .pattern("A")
      );
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_CRAFTER = this.create(AllBlocks.MECHANICAL_CRAFTER)
      .returns(3)
      .unlockedBy(CreateRecipeProvider.I::brassCasing)
      .viaShaped(
         b -> b.define('B', CreateRecipeProvider.I.electronTube())
               .define('R', Blocks.CRAFTING_TABLE)
               .define('C', CreateRecipeProvider.I.brassCasing())
               .pattern("B")
               .pattern("C")
               .pattern("R")
      );
   BaseRecipeProvider.GeneratedRecipe WINDMILL_BEARING = this.create(AllBlocks.WINDMILL_BEARING)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('B', ItemTags.WOODEN_SLABS)
               .define('C', CreateRecipeProvider.I.stone())
               .define('I', CreateRecipeProvider.I.shaft())
               .pattern("B")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_BEARING = this.create(AllBlocks.MECHANICAL_BEARING)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(
         b -> b.define('B', ItemTags.WOODEN_SLABS)
               .define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('I', CreateRecipeProvider.I.shaft())
               .pattern("B")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe CLOCKWORK_BEARING = this.create(AllBlocks.CLOCKWORK_BEARING)
      .unlockedBy(CreateRecipeProvider.I::brassCasing)
      .viaShaped(
         b -> b.define('S', CreateRecipeProvider.I.electronTube())
               .define('B', CreateRecipeProvider.I.woodSlab())
               .define('C', CreateRecipeProvider.I.brassCasing())
               .pattern("B")
               .pattern("C")
               .pattern("S")
      );
   BaseRecipeProvider.GeneratedRecipe WOODEN_BRACKET = this.create(AllBlocks.WOODEN_BRACKET)
      .returns(4)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('S', net.neoforged.neoforge.common.Tags.Items.RODS_WOODEN)
               .define('P', CreateRecipeProvider.I.planks())
               .define('C', CreateRecipeProvider.I.andesiteAlloy())
               .pattern("SSS")
               .pattern("PCP")
      );
   BaseRecipeProvider.GeneratedRecipe METAL_BRACKET = this.create(AllBlocks.METAL_BRACKET)
      .returns(4)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('S', net.neoforged.neoforge.common.Tags.Items.NUGGETS_IRON)
               .define('P', CreateRecipeProvider.I.iron())
               .define('C', CreateRecipeProvider.I.andesiteAlloy())
               .pattern("SSS")
               .pattern("PCP")
      );
   BaseRecipeProvider.GeneratedRecipe METAL_GIRDER = this.create(AllBlocks.METAL_GIRDER)
      .returns(8)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('P', CreateRecipeProvider.I.ironSheet()).define('C', CreateRecipeProvider.I.andesiteAlloy()).pattern("PPP").pattern("CCC"));
   BaseRecipeProvider.GeneratedRecipe DISPLAY_BOARD = this.create(AllBlocks.DISPLAY_BOARD)
      .returns(2)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('A', CreateRecipeProvider.I.electronTube()).define('P', CreateRecipeProvider.I.andesiteAlloy()).pattern("PAP"));
   BaseRecipeProvider.GeneratedRecipe STEAM_WHISTLE = this.create(AllBlocks.STEAM_WHISTLE)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShaped(b -> b.define('P', CreateRecipeProvider.I.goldSheet()).define('C', CreateRecipeProvider.I.copper()).pattern("P").pattern("C"));
   BaseRecipeProvider.GeneratedRecipe STEAM_ENGINE = this.create(AllBlocks.STEAM_ENGINE)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShaped(
         b -> b.define('P', CreateRecipeProvider.I.goldSheet())
               .define('C', CreateRecipeProvider.I.copperBlock())
               .define('A', CreateRecipeProvider.I.andesiteAlloy())
               .pattern("P")
               .pattern("A")
               .pattern("C")
      );
   BaseRecipeProvider.GeneratedRecipe FLUID_PIPE = this.create(AllBlocks.FLUID_PIPE)
      .returns(4)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShaped(b -> b.define('S', CreateRecipeProvider.I.copperSheet()).define('C', CreateRecipeProvider.I.copper()).pattern("SCS"));
   BaseRecipeProvider.GeneratedRecipe FLUID_PIPE_2 = this.create(AllBlocks.FLUID_PIPE)
      .withSuffix("_vertical")
      .returns(4)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShaped(b -> b.define('S', CreateRecipeProvider.I.copperSheet()).define('C', CreateRecipeProvider.I.copper()).pattern("S").pattern("C").pattern("S"));
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_PUMP = this.create(AllBlocks.MECHANICAL_PUMP)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.cog()).requires((ItemLike)AllBlocks.FLUID_PIPE.get()));
   BaseRecipeProvider.GeneratedRecipe SMART_FLUID_PIPE = this.create(AllBlocks.SMART_FLUID_PIPE)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShaped(
         b -> b.define('P', CreateRecipeProvider.I.electronTube())
               .define('S', (ItemLike)AllBlocks.FLUID_PIPE.get())
               .define('I', CreateRecipeProvider.I.brassSheet())
               .pattern("I")
               .pattern("S")
               .pattern("P")
      );
   BaseRecipeProvider.GeneratedRecipe FLUID_VALVE = this.create(AllBlocks.FLUID_VALVE)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.ironSheet()).requires((ItemLike)AllBlocks.FLUID_PIPE.get()));
   BaseRecipeProvider.GeneratedRecipe SPOUT = this.create(AllBlocks.SPOUT)
      .unlockedBy(CreateRecipeProvider.I::copperCasing)
      .viaShaped(b -> b.define('T', CreateRecipeProvider.I.copperCasing()).define('P', Items.DRIED_KELP).pattern("T").pattern("P"));
   BaseRecipeProvider.GeneratedRecipe ITEM_DRAIN = this.create(AllBlocks.ITEM_DRAIN)
      .unlockedBy(CreateRecipeProvider.I::copperCasing)
      .viaShaped(b -> b.define('P', Blocks.IRON_BARS).define('S', CreateRecipeProvider.I.copperCasing()).pattern("P").pattern("S"));
   BaseRecipeProvider.GeneratedRecipe FLUID_TANK = this.create(AllBlocks.FLUID_TANK)
      .unlockedByTag(() -> net.neoforged.neoforge.common.Tags.Items.BARRELS_WOODEN)
      .viaShaped(
         b -> b.define('B', CreateRecipeProvider.I.copperSheet())
               .define('C', net.neoforged.neoforge.common.Tags.Items.BARRELS_WOODEN)
               .pattern("B")
               .pattern("C")
               .pattern("B")
      );
   BaseRecipeProvider.GeneratedRecipe ITEM_VAULT = this.create(AllBlocks.ITEM_VAULT)
      .unlockedByTag(() -> net.neoforged.neoforge.common.Tags.Items.BARRELS_WOODEN)
      .viaShaped(
         b -> b.define('B', CreateRecipeProvider.I.ironSheet())
               .define('C', net.neoforged.neoforge.common.Tags.Items.BARRELS_WOODEN)
               .pattern("B")
               .pattern("C")
               .pattern("B")
      );
   BaseRecipeProvider.GeneratedRecipe TRAIN_SIGNAL = this.create(AllBlocks.TRACK_SIGNAL)
      .unlockedBy(CreateRecipeProvider.I::railwayCasing)
      .returns(4)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.railwayCasing()).requires(CreateRecipeProvider.I.electronTube()));
   BaseRecipeProvider.GeneratedRecipe TRAIN_OBSERVER = this.create(AllBlocks.TRACK_OBSERVER)
      .unlockedBy(CreateRecipeProvider.I::railwayCasing)
      .returns(2)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.railwayCasing()).requires(ItemTags.WOODEN_PRESSURE_PLATES));
   BaseRecipeProvider.GeneratedRecipe TRAIN_OBSERVER_2 = this.create(AllBlocks.TRACK_OBSERVER)
      .withSuffix("_from_other_plates")
      .unlockedBy(CreateRecipeProvider.I::railwayCasing)
      .returns(2)
      .viaShapeless(
         b -> b.requires(CreateRecipeProvider.I.railwayCasing())
               .requires(
                  Ingredient.of(
                     new ItemLike[]{
                        Items.STONE_PRESSURE_PLATE,
                        Items.POLISHED_BLACKSTONE_PRESSURE_PLATE,
                        Items.HEAVY_WEIGHTED_PRESSURE_PLATE,
                        Items.LIGHT_WEIGHTED_PRESSURE_PLATE
                     }
                  )
               )
      );
   BaseRecipeProvider.GeneratedRecipe TRAIN_SCHEDULE = this.create(AllItems.SCHEDULE)
      .unlockedByTag(CreateRecipeProvider.I::sturdySheet)
      .returns(4)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.sturdySheet()).requires(Items.PAPER));
   BaseRecipeProvider.GeneratedRecipe TRAIN_STATION = this.create(AllBlocks.TRACK_STATION)
      .unlockedBy(CreateRecipeProvider.I::railwayCasing)
      .returns(2)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.railwayCasing()).requires(Items.COMPASS));
   BaseRecipeProvider.GeneratedRecipe TRAIN_CONTROLS = this.create(AllBlocks.TRAIN_CONTROLS)
      .unlockedBy(CreateRecipeProvider.I::railwayCasing)
      .viaShaped(
         b -> b.define('I', CreateRecipeProvider.I.precisionMechanism())
               .define('B', Items.LEVER)
               .define('C', CreateRecipeProvider.I.railwayCasing())
               .pattern("B")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe DEPLOYER = this.create(AllBlocks.DEPLOYER)
      .unlockedBy(CreateRecipeProvider.I::electronTube)
      .viaShaped(
         b -> b.define('I', (ItemLike)AllItems.BRASS_HAND.get())
               .define('B', CreateRecipeProvider.I.electronTube())
               .define('C', CreateRecipeProvider.I.andesiteCasing())
               .pattern("B")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe PORTABLE_STORAGE_INTERFACE = this.create(AllBlocks.PORTABLE_STORAGE_INTERFACE)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.andesiteCasing()).requires((ItemLike)AllBlocks.CHUTE.get()));
   BaseRecipeProvider.GeneratedRecipe PORTABLE_FLUID_INTERFACE = this.create(AllBlocks.PORTABLE_FLUID_INTERFACE)
      .unlockedBy(CreateRecipeProvider.I::copperCasing)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.copperCasing()).requires((ItemLike)AllBlocks.CHUTE.get()));
   BaseRecipeProvider.GeneratedRecipe ROPE_PULLEY = this.create(AllBlocks.ROPE_PULLEY)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('B', CreateRecipeProvider.I.andesiteCasing())
               .define('C', ItemTags.WOOL)
               .define('I', CreateRecipeProvider.I.ironSheet())
               .pattern("B")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe HOSE_PULLEY = this.create(AllBlocks.HOSE_PULLEY)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShaped(
         b -> b.define('B', CreateRecipeProvider.I.copperCasing())
               .define('C', Items.DRIED_KELP_BLOCK)
               .define('I', CreateRecipeProvider.I.copperSheet())
               .pattern("B")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe ELEVATOR_PULLEY = this.create(AllBlocks.ELEVATOR_PULLEY)
      .unlockedByTag(CreateRecipeProvider.I::brass)
      .viaShaped(
         b -> b.define('B', CreateRecipeProvider.I.brassCasing())
               .define('C', Items.DRIED_KELP_BLOCK)
               .define('I', CreateRecipeProvider.I.ironSheet())
               .pattern("B")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe CONTRAPTION_CONTROLS = this.create(AllBlocks.CONTRAPTION_CONTROLS)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('B', ItemTags.BUTTONS)
               .define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('I', CreateRecipeProvider.I.electronTube())
               .pattern("B")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe EMPTY_BLAZE_BURNER = this.create(AllItems.EMPTY_BLAZE_BURNER)
      .unlockedByTag(CreateRecipeProvider.I::iron)
      .viaShaped(
         b -> b.define('A', net.neoforged.neoforge.common.Tags.Items.NETHERRACKS)
               .define('I', CreateRecipeProvider.I.ironSheet())
               .pattern(" I ")
               .pattern("IAI")
               .pattern(" I ")
      );
   BaseRecipeProvider.GeneratedRecipe CHUTE = this.create(AllBlocks.CHUTE)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .returns(4)
      .viaShaped(b -> b.define('A', CreateRecipeProvider.I.ironSheet()).define('I', CreateRecipeProvider.I.iron()).pattern("A").pattern("I").pattern("A"));
   BaseRecipeProvider.GeneratedRecipe SMART_CHUTE = this.create(AllBlocks.SMART_CHUTE)
      .unlockedBy(AllBlocks.CHUTE::get)
      .viaShaped(
         b -> b.define('P', CreateRecipeProvider.I.electronTube())
               .define('S', (ItemLike)AllBlocks.CHUTE.get())
               .define('I', CreateRecipeProvider.I.brassSheet())
               .pattern("I")
               .pattern("S")
               .pattern("P")
      );
   BaseRecipeProvider.GeneratedRecipe DEPOT = this.create(AllBlocks.DEPOT)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.andesiteAlloy()).requires(CreateRecipeProvider.I.andesiteCasing()));
   BaseRecipeProvider.GeneratedRecipe WEIGHTED_EJECTOR = this.create(AllBlocks.WEIGHTED_EJECTOR)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(
         b -> b.define('A', CreateRecipeProvider.I.goldSheet())
               .define('D', (ItemLike)AllBlocks.DEPOT.get())
               .define('I', CreateRecipeProvider.I.cog())
               .pattern("A")
               .pattern("D")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_ARM = this.create(AllBlocks.MECHANICAL_ARM::get)
      .unlockedBy(CreateRecipeProvider.I::brassCasing)
      .returns(1)
      .viaShaped(
         b -> b.define('L', CreateRecipeProvider.I.brassSheet())
               .define('I', CreateRecipeProvider.I.precisionMechanism())
               .define('A', CreateRecipeProvider.I.andesiteAlloy())
               .define('C', CreateRecipeProvider.I.brassCasing())
               .pattern("LLA")
               .pattern("L  ")
               .pattern("IC ")
      );
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_MIXER = this.create(AllBlocks.MECHANICAL_MIXER)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('S', CreateRecipeProvider.I.cog())
               .define('I', (ItemLike)AllItems.WHISK.get())
               .pattern("S")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe CLUTCH = this.create(AllBlocks.CLUTCH)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShapeless(
         b -> b.requires(CreateRecipeProvider.I.andesiteCasing()).requires(CreateRecipeProvider.I.shaft()).requires(CreateRecipeProvider.I.redstone())
      );
   BaseRecipeProvider.GeneratedRecipe GEARSHIFT = this.create(AllBlocks.GEARSHIFT)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.andesiteCasing()).requires(CreateRecipeProvider.I.cog()).requires(CreateRecipeProvider.I.redstone()));
   BaseRecipeProvider.GeneratedRecipe SAIL = this.create(AllBlocks.SAIL)
      .returns(2)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('W', ItemTags.WOOL)
               .define('S', net.neoforged.neoforge.common.Tags.Items.RODS_WOODEN)
               .define('A', CreateRecipeProvider.I.andesiteAlloy())
               .pattern("WS")
               .pattern("SA")
      );
   BaseRecipeProvider.GeneratedRecipe SAIL_CYCLE = this.conversionCycle(ImmutableList.of(AllBlocks.SAIL_FRAME, AllBlocks.SAIL));
   BaseRecipeProvider.GeneratedRecipe RADIAL_CHASIS = this.create(AllBlocks.RADIAL_CHASSIS)
      .returns(3)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('P', CreateRecipeProvider.I.andesiteAlloy()).define('L', ItemTags.LOGS).pattern(" L ").pattern("PLP").pattern(" L "));
   BaseRecipeProvider.GeneratedRecipe LINEAR_CHASIS = this.create(AllBlocks.LINEAR_CHASSIS)
      .returns(3)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('P', CreateRecipeProvider.I.andesiteAlloy()).define('L', ItemTags.LOGS).pattern(" P ").pattern("LLL").pattern(" P "));
   BaseRecipeProvider.GeneratedRecipe LINEAR_CHASSIS_CYCLE = this.conversionCycle(
      ImmutableList.of(AllBlocks.LINEAR_CHASSIS, AllBlocks.SECONDARY_LINEAR_CHASSIS)
   );
   BaseRecipeProvider.GeneratedRecipe STICKER = this.create(AllBlocks.STICKER)
      .returns(1)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('I', CreateRecipeProvider.I.andesiteAlloy())
               .define('C', net.neoforged.neoforge.common.Tags.Items.COBBLESTONES)
               .define('R', CreateRecipeProvider.I.redstone())
               .define('S', net.neoforged.neoforge.common.Tags.Items.SLIMEBALLS)
               .pattern("ISI")
               .pattern("CRC")
      );
   BaseRecipeProvider.GeneratedRecipe MINECART = this.create((Supplier<ItemLike>)(() -> Items.MINECART))
      .withSuffix("_from_contraption_cart")
      .unlockedBy(AllBlocks.CART_ASSEMBLER::get)
      .viaShapeless(b -> b.requires((ItemLike)AllItems.MINECART_CONTRAPTION.get()));
   BaseRecipeProvider.GeneratedRecipe FURNACE_MINECART = this.create((Supplier<ItemLike>)(() -> Items.FURNACE_MINECART))
      .withSuffix("_from_contraption_cart")
      .unlockedBy(AllBlocks.CART_ASSEMBLER::get)
      .viaShapeless(b -> b.requires((ItemLike)AllItems.FURNACE_MINECART_CONTRAPTION.get()));
   BaseRecipeProvider.GeneratedRecipe GEARBOX = this.create(AllBlocks.GEARBOX)
      .unlockedBy(CreateRecipeProvider.I::cog)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.cog()).define('B', CreateRecipeProvider.I.andesiteCasing()).pattern(" C ").pattern("CBC").pattern(" C ")
      );
   BaseRecipeProvider.GeneratedRecipe VERTICAL_GEARBOX = this.create(AllItems.VERTICAL_GEARBOX)
      .unlockedBy(CreateRecipeProvider.I::cog)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.cog()).define('B', CreateRecipeProvider.I.andesiteCasing()).pattern("C C").pattern(" B ").pattern("C C")
      );
   BaseRecipeProvider.GeneratedRecipe GEARBOX_CYCLE = this.conversionCycle(ImmutableList.of(AllBlocks.GEARBOX, AllItems.VERTICAL_GEARBOX));
   BaseRecipeProvider.GeneratedRecipe MYSTERIOUS_CUCKOO_CLOCK = this.create(AllBlocks.MYSTERIOUS_CUCKOO_CLOCK)
      .unlockedBy(AllBlocks.CUCKOO_CLOCK::get)
      .viaShaped(
         b -> b.define('C', net.neoforged.neoforge.common.Tags.Items.GUNPOWDERS)
               .define('B', (ItemLike)AllBlocks.CUCKOO_CLOCK.get())
               .pattern(" C ")
               .pattern("CBC")
               .pattern(" C ")
      );
   BaseRecipeProvider.GeneratedRecipe ENCASED_CHAIN_DRIVE = this.create(AllBlocks.ENCASED_CHAIN_DRIVE)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShapeless(
         b -> b.requires(CreateRecipeProvider.I.andesiteCasing())
               .requires(CreateRecipeProvider.I.ironNugget())
               .requires(CreateRecipeProvider.I.ironNugget())
               .requires(CreateRecipeProvider.I.ironNugget())
      );
   BaseRecipeProvider.GeneratedRecipe ENCASED_CHAIN_DRIVE_ZINC = this.create(AllBlocks.ENCASED_CHAIN_DRIVE)
      .withSuffix("_from_zinc")
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShapeless(
         b -> b.requires(CreateRecipeProvider.I.andesiteCasing())
               .requires(CreateRecipeProvider.I.zincNugget())
               .requires(CreateRecipeProvider.I.zincNugget())
               .requires(CreateRecipeProvider.I.zincNugget())
      );
   BaseRecipeProvider.GeneratedRecipe FLYWHEEL = this.create(AllBlocks.FLYWHEEL)
      .unlockedByTag(CreateRecipeProvider.I::brass)
      .viaShaped(b -> b.define('C', CreateRecipeProvider.I.brass()).define('A', CreateRecipeProvider.I.shaft()).pattern("CCC").pattern("CAC").pattern("CCC"));
   BaseRecipeProvider.GeneratedRecipe SPEEDOMETER = this.create(AllBlocks.SPEEDOMETER)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('C', Items.COMPASS).define('A', CreateRecipeProvider.I.andesiteCasing()).pattern("C").pattern("A"));
   BaseRecipeProvider.GeneratedRecipe GAUGE_CYCLE = this.conversionCycle(ImmutableList.of(AllBlocks.SPEEDOMETER, AllBlocks.STRESSOMETER));
   BaseRecipeProvider.GeneratedRecipe ROTATION_SPEED_CONTROLLER = this.create(AllBlocks.ROTATION_SPEED_CONTROLLER)
      .unlockedBy(CreateRecipeProvider.I::brassCasing)
      .viaShaped(b -> b.define('B', CreateRecipeProvider.I.precisionMechanism()).define('C', CreateRecipeProvider.I.brassCasing()).pattern("B").pattern("C"));
   BaseRecipeProvider.GeneratedRecipe NIXIE_TUBE = this.create(AllBlocks.ORANGE_NIXIE_TUBE)
      .returns(4)
      .unlockedBy(CreateRecipeProvider.I::brassCasing)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.electronTube()).requires(CreateRecipeProvider.I.electronTube()));
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_SAW = this.create(AllBlocks.MECHANICAL_SAW)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('A', CreateRecipeProvider.I.ironSheet())
               .define('I', CreateRecipeProvider.I.iron())
               .pattern(" A ")
               .pattern("AIA")
               .pattern(" C ")
      );
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_HARVESTER = this.create(AllBlocks.MECHANICAL_HARVESTER)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('A', CreateRecipeProvider.I.andesiteAlloy())
               .define('I', CreateRecipeProvider.I.ironSheet())
               .pattern("AIA")
               .pattern("AIA")
               .pattern(" C ")
      );
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_PLOUGH = this.create(AllBlocks.MECHANICAL_PLOUGH)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('A', CreateRecipeProvider.I.andesiteAlloy())
               .define('I', CreateRecipeProvider.I.ironSheet())
               .pattern("III")
               .pattern("AAA")
               .pattern(" C ")
      );
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_ROLLER = this.create(AllBlocks.MECHANICAL_ROLLER)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('A', CreateRecipeProvider.I.electronTube())
               .define('I', (ItemLike)AllBlocks.CRUSHING_WHEEL.get())
               .pattern("A")
               .pattern("C")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe MECHANICAL_DRILL = this.create(AllBlocks.MECHANICAL_DRILL)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('A', CreateRecipeProvider.I.andesiteAlloy())
               .define('I', CreateRecipeProvider.I.iron())
               .pattern(" A ")
               .pattern("AIA")
               .pattern(" C ")
      );
   BaseRecipeProvider.GeneratedRecipe CHAIN_CONVEYOR = this.create(AllBlocks.CHAIN_CONVEYOR)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .returns(2)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.andesiteCasing())
               .define('A', CreateRecipeProvider.I.largeCog())
               .pattern(" C ")
               .pattern("CAC")
               .pattern(" C ")
      );
   BaseRecipeProvider.GeneratedRecipe SEQUENCED_GEARSHIFT = this.create(AllBlocks.SEQUENCED_GEARSHIFT)
      .unlockedBy(CreateRecipeProvider.I::brassCasing)
      .viaShapeless(
         b -> b.requires(CreateRecipeProvider.I.brassCasing()).requires(CreateRecipeProvider.I.cog()).requires(CreateRecipeProvider.I.electronTube())
      );
   private CreateStandardRecipeGen.Marker LOGISTICS = this.enterFolder("logistics");
   BaseRecipeProvider.GeneratedRecipe REDSTONE_CONTACT = this.create(AllBlocks.REDSTONE_CONTACT)
      .returns(2)
      .unlockedBy(CreateRecipeProvider.I::brassCasing)
      .viaShaped(
         b -> b.define('W', CreateRecipeProvider.I.redstone())
               .define('C', Blocks.COBBLESTONE)
               .define('S', CreateRecipeProvider.I.ironSheet())
               .pattern(" S ")
               .pattern("CWC")
               .pattern("CCC")
      );
   BaseRecipeProvider.GeneratedRecipe ANDESITE_FUNNEL = this.create(AllBlocks.ANDESITE_FUNNEL)
      .returns(2)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('A', CreateRecipeProvider.I.andesiteAlloy()).define('K', Items.DRIED_KELP).pattern("A").pattern("K"));
   BaseRecipeProvider.GeneratedRecipe BRASS_FUNNEL = this.create(AllBlocks.BRASS_FUNNEL)
      .returns(2)
      .unlockedByTag(CreateRecipeProvider.I::brass)
      .viaShaped(
         b -> b.define('A', CreateRecipeProvider.I.brass())
               .define('K', Items.DRIED_KELP)
               .define('E', CreateRecipeProvider.I.electronTube())
               .pattern("E")
               .pattern("A")
               .pattern("K")
      );
   BaseRecipeProvider.GeneratedRecipe ANDESITE_TUNNEL = this.create(AllBlocks.ANDESITE_TUNNEL)
      .returns(2)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(b -> b.define('A', CreateRecipeProvider.I.andesiteAlloy()).define('K', Items.DRIED_KELP).pattern("AA").pattern("KK"));
   BaseRecipeProvider.GeneratedRecipe BRASS_TUNNEL = this.create(AllBlocks.BRASS_TUNNEL)
      .returns(2)
      .unlockedByTag(CreateRecipeProvider.I::brass)
      .viaShaped(
         b -> b.define('A', CreateRecipeProvider.I.brass())
               .define('K', Items.DRIED_KELP)
               .define('E', CreateRecipeProvider.I.electronTube())
               .pattern("E ")
               .pattern("AA")
               .pattern("KK")
      );
   BaseRecipeProvider.GeneratedRecipe SMART_OBSERVER = this.create(AllBlocks.SMART_OBSERVER)
      .unlockedBy(CreateRecipeProvider.I::brassCasing)
      .viaShaped(
         b -> b.define('B', CreateRecipeProvider.I.brassCasing())
               .define('R', CreateRecipeProvider.I.electronTube())
               .define('I', Blocks.OBSERVER)
               .pattern("R")
               .pattern("B")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe THRESHOLD_SWITCH = this.create(AllBlocks.THRESHOLD_SWITCH)
      .unlockedBy(CreateRecipeProvider.I::brassCasing)
      .viaShaped(
         b -> b.define('B', CreateRecipeProvider.I.brassCasing())
               .define('R', CreateRecipeProvider.I.electronTube())
               .define('I', Blocks.COMPARATOR)
               .pattern("R")
               .pattern("B")
               .pattern("I")
      );
   BaseRecipeProvider.GeneratedRecipe PULSE_EXTENDER = this.create(AllBlocks.PULSE_EXTENDER)
      .unlockedByTag(CreateRecipeProvider.I::redstone)
      .viaShaped(
         b -> b.define('T', Blocks.REDSTONE_TORCH)
               .define('C', CreateRecipeProvider.I.brassSheet())
               .define('R', CreateRecipeProvider.I.redstone())
               .define('S', CreateRecipeProvider.I.stone())
               .pattern("  T")
               .pattern("RCT")
               .pattern("SSS")
      );
   BaseRecipeProvider.GeneratedRecipe PULSE_REPEATER = this.create(AllBlocks.PULSE_REPEATER)
      .unlockedByTag(CreateRecipeProvider.I::redstone)
      .viaShaped(
         b -> b.define('T', Blocks.REDSTONE_TORCH)
               .define('C', CreateRecipeProvider.I.brassSheet())
               .define('R', CreateRecipeProvider.I.redstone())
               .define('S', CreateRecipeProvider.I.stone())
               .pattern("RCT")
               .pattern("SSS")
      );
   BaseRecipeProvider.GeneratedRecipe PULSE_TIMER = this.create(AllBlocks.PULSE_TIMER)
      .unlockedByTag(CreateRecipeProvider.I::redstone)
      .viaShaped(
         b -> b.define('T', Blocks.REDSTONE_TORCH)
               .define('C', CreateRecipeProvider.I.brassSheet())
               .define('R', Items.AMETHYST_SHARD)
               .define('S', CreateRecipeProvider.I.stone())
               .pattern("RCT")
               .pattern("SSS")
      );
   BaseRecipeProvider.GeneratedRecipe POWERED_TOGGLE_LATCH = this.create(AllBlocks.POWERED_TOGGLE_LATCH)
      .unlockedByTag(CreateRecipeProvider.I::redstone)
      .viaShaped(
         b -> b.define('T', Blocks.REDSTONE_TORCH)
               .define('C', Blocks.LEVER)
               .define('S', CreateRecipeProvider.I.stone())
               .pattern(" T ")
               .pattern(" C ")
               .pattern("SSS")
      );
   BaseRecipeProvider.GeneratedRecipe POWERED_LATCH = this.create(AllBlocks.POWERED_LATCH)
      .unlockedByTag(CreateRecipeProvider.I::redstone)
      .viaShaped(
         b -> b.define('T', Blocks.REDSTONE_TORCH)
               .define('C', Blocks.LEVER)
               .define('R', CreateRecipeProvider.I.redstone())
               .define('S', CreateRecipeProvider.I.stone())
               .pattern(" T ")
               .pattern("RCR")
               .pattern("SSS")
      );
   BaseRecipeProvider.GeneratedRecipe REDSTONE_LINK = this.create(AllBlocks.REDSTONE_LINK)
      .returns(2)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShaped(b -> b.define('C', AllItems.TRANSMITTER).define('S', CreateRecipeProvider.I.andesiteCasing()).pattern("C").pattern("S"));
   BaseRecipeProvider.GeneratedRecipe ITEM_HATCH = this.create(AllBlocks.ITEM_HATCH)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.andesiteAlloy()).requires(Items.IRON_TRAPDOOR));
   BaseRecipeProvider.GeneratedRecipe PACKAGER = this.create(AllBlocks.PACKAGER)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.iron())
               .define('A', AllBlocks.CARDBOARD_BLOCK)
               .define('R', CreateRecipeProvider.I.redstone())
               .pattern(" C ")
               .pattern("CAC")
               .pattern("RCR")
      );
   BaseRecipeProvider.GeneratedRecipe PACKAGER_CYCLE = this.conversionCycle(ImmutableList.of(AllBlocks.PACKAGER, AllBlocks.REPACKAGER));
   BaseRecipeProvider.GeneratedRecipe PACKAGE_FROGPORT = this.create(AllBlocks.PACKAGE_FROGPORT)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.andesiteAlloy())
               .define('B', net.neoforged.neoforge.common.Tags.Items.SLIMEBALLS)
               .define('A', CreateRecipeProvider.I.vault())
               .pattern("B")
               .pattern("A")
               .pattern("C")
      );
   BaseRecipeProvider.GeneratedRecipe STOCK_LINK = this.create(AllBlocks.STOCK_LINK)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(b -> b.define('C', (ItemLike)AllItems.TRANSMITTER.get()).define('B', CreateRecipeProvider.I.vault()).pattern("C").pattern("B"));
   BaseRecipeProvider.GeneratedRecipe STOCK_TICKER = this.create(AllBlocks.STOCK_TICKER)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(
         b -> b.define('C', net.neoforged.neoforge.common.Tags.Items.GLASS_BLOCKS)
               .define('B', CreateRecipeProvider.I.gold())
               .define('A', CreateRecipeProvider.I.stockLink())
               .pattern("C")
               .pattern("A")
               .pattern("B")
      );
   BaseRecipeProvider.GeneratedRecipe REDSTONE_REQUESTER = this.create(AllBlocks.REDSTONE_REQUESTER)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(
         b -> b.define('C', CreateRecipeProvider.I.redstone())
               .define('B', CreateRecipeProvider.I.iron())
               .define('A', CreateRecipeProvider.I.stockLink())
               .pattern("C")
               .pattern("A")
               .pattern("B")
      );
   BaseRecipeProvider.GeneratedRecipe FACTORY_GAUGE = this.create(AllBlocks.FACTORY_GAUGE)
      .unlockedBy(CreateRecipeProvider.I::stockLink)
      .returns(2)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.stockLink()).requires(CreateRecipeProvider.I.precisionMechanism()));
   BaseRecipeProvider.GeneratedRecipe DESK_BELL = this.create(AllBlocks.DESK_BELL)
      .unlockedBy(CreateRecipeProvider.I::andesiteCasing)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.andesiteCasing()).requires(CreateRecipeProvider.I.goldSheet()));
   BaseRecipeProvider.GeneratedRecipe LOGISTICS_LINK_CLEAR = this.clearData(AllBlocks.STOCK_LINK);
   BaseRecipeProvider.GeneratedRecipe STOCK_TICKER_CLEAR = this.clearData(AllBlocks.STOCK_TICKER);
   BaseRecipeProvider.GeneratedRecipe REDSTONE_REQUESTER_CLEAR = this.clearData(AllBlocks.REDSTONE_REQUESTER);
   BaseRecipeProvider.GeneratedRecipe FACTORY_PANEL_CLEAR = this.clearData(AllBlocks.FACTORY_GAUGE);
   BaseRecipeProvider.GeneratedRecipe DISPLAY_LINK = this.create(AllBlocks.DISPLAY_LINK)
      .unlockedBy(CreateRecipeProvider.I::brassCasing)
      .viaShaped(b -> b.define('C', (ItemLike)AllItems.TRANSMITTER.get()).define('S', CreateRecipeProvider.I.brassCasing()).pattern("C").pattern("S"));
   private CreateStandardRecipeGen.Marker SCHEMATICS = this.enterFolder("schematics");
   BaseRecipeProvider.GeneratedRecipe SCHEMATIC_TABLE = this.create(AllBlocks.SCHEMATIC_TABLE)
      .unlockedBy(AllItems.EMPTY_SCHEMATIC::get)
      .viaShaped(b -> b.define('W', ItemTags.WOODEN_SLABS).define('S', Blocks.SMOOTH_STONE).pattern("WWW").pattern(" S ").pattern(" S "));
   BaseRecipeProvider.GeneratedRecipe SCHEMATICANNON = this.create(AllBlocks.SCHEMATICANNON)
      .unlockedBy(AllItems.EMPTY_SCHEMATIC::get)
      .viaShaped(
         b -> b.define('L', ItemTags.LOGS)
               .define('D', Blocks.DISPENSER)
               .define('S', Blocks.SMOOTH_STONE)
               .define('I', Blocks.IRON_BLOCK)
               .pattern(" I ")
               .pattern("LIL")
               .pattern("SDS")
      );
   BaseRecipeProvider.GeneratedRecipe EMPTY_SCHEMATIC = this.create(AllItems.EMPTY_SCHEMATIC)
      .unlockedBy(() -> Items.PAPER)
      .viaShapeless(b -> b.requires(Items.PAPER).requires(net.neoforged.neoforge.common.Tags.Items.DYES_LIGHT_BLUE));
   BaseRecipeProvider.GeneratedRecipe SCHEMATIC_AND_QUILL = this.create(AllItems.SCHEMATIC_AND_QUILL)
      .unlockedBy(() -> Items.PAPER)
      .viaShapeless(b -> b.requires((ItemLike)AllItems.EMPTY_SCHEMATIC.get()).requires(net.neoforged.neoforge.common.Tags.Items.FEATHERS));
   private CreateStandardRecipeGen.Marker PALETTES = this.enterFolder("palettes");
   BaseRecipeProvider.GeneratedRecipe SCORCHIA = this.create(AllPaletteStoneTypes.SCORCHIA.getBaseBlock()::get)
      .returns(8)
      .unlockedBy(AllPaletteStoneTypes.SCORIA.getBaseBlock()::get)
      .viaShaped(
         b -> b.define('#', (ItemLike)AllPaletteStoneTypes.SCORIA.getBaseBlock().get())
               .define('D', net.neoforged.neoforge.common.Tags.Items.DYES_BLACK)
               .pattern("###")
               .pattern("#D#")
               .pattern("###")
      );
   private CreateStandardRecipeGen.Marker APPLIANCES = this.enterFolder("appliances");
   BaseRecipeProvider.GeneratedRecipe DOUGH = this.create(AllItems.DOUGH)
      .unlockedByTag(CreateRecipeProvider.I::wheatFlour)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.wheatFlour()).requires(Items.WATER_BUCKET));
   BaseRecipeProvider.GeneratedRecipe CHAIN_FROM_ZINC = this.create((Supplier<ItemLike>)(() -> Items.CHAIN))
      .withSuffix("_from_zinc")
      .unlockedByTag(CreateRecipeProvider.I::zinc)
      .viaShaped(b -> b.define('C', CreateRecipeProvider.I.zinc()).define('S', CreateRecipeProvider.I.zincNugget()).pattern("S").pattern("C").pattern("S"));
   BaseRecipeProvider.GeneratedRecipe CLIPBOARD = this.create(AllBlocks.CLIPBOARD)
      .unlockedBy(CreateRecipeProvider.I::andesiteAlloy)
      .viaShaped(
         b -> b.define('G', CreateRecipeProvider.I.planks())
               .define('P', Items.PAPER)
               .define('A', CreateRecipeProvider.I.andesiteAlloy())
               .pattern("A")
               .pattern("P")
               .pattern("G")
      );
   BaseRecipeProvider.GeneratedRecipe CLIPBOARD_CLEAR = this.clearData(AllBlocks.CLIPBOARD);
   BaseRecipeProvider.GeneratedRecipe SCHEDULE_CLEAR = this.clearData(AllItems.SCHEDULE);
   BaseRecipeProvider.GeneratedRecipe FILTER_CLEAR = this.clearData(AllItems.FILTER);
   BaseRecipeProvider.GeneratedRecipe ATTRIBUTE_FILTER_CLEAR = this.clearData(AllItems.ATTRIBUTE_FILTER);
   BaseRecipeProvider.GeneratedRecipe PACKAGE_FILTER_CLEAR = this.clearData(AllItems.PACKAGE_FILTER);
   BaseRecipeProvider.GeneratedRecipe CARDBOARD_SWORD = this.create(AllItems.CARDBOARD_SWORD)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(
         b -> b.define('P', CreateRecipeProvider.I.cardboard())
               .define('S', net.neoforged.neoforge.common.Tags.Items.RODS_WOODEN)
               .pattern("P")
               .pattern("P")
               .pattern("S")
      );
   BaseRecipeProvider.GeneratedRecipe CARDBOARD_HELMET = this.create(AllItems.CARDBOARD_HELMET)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(b -> b.define('P', CreateRecipeProvider.I.cardboard()).pattern("PPP").pattern("P P"));
   BaseRecipeProvider.GeneratedRecipe CARDBOARD_CHESTPLATE = this.create(AllItems.CARDBOARD_CHESTPLATE)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(b -> b.define('P', CreateRecipeProvider.I.cardboard()).pattern("P P").pattern("PPP").pattern("PPP"));
   BaseRecipeProvider.GeneratedRecipe CARDBOARD_LEGGINGS = this.create(AllItems.CARDBOARD_LEGGINGS)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(b -> b.define('P', CreateRecipeProvider.I.cardboard()).pattern("PPP").pattern("P P").pattern("P P"));
   BaseRecipeProvider.GeneratedRecipe CARDBOARD_BOOTS = this.create(AllItems.CARDBOARD_BOOTS)
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(b -> b.define('P', CreateRecipeProvider.I.cardboard()).pattern("P P").pattern("P P"));
   BaseRecipeProvider.GeneratedRecipe DIVING_HELMET = this.create(AllItems.COPPER_DIVING_HELMET)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShaped(
         b -> b.define('G', net.neoforged.neoforge.common.Tags.Items.GLASS_BLOCKS).define('P', CreateRecipeProvider.I.copper()).pattern("PPP").pattern("PGP")
      );
   BaseRecipeProvider.GeneratedRecipe COPPER_BACKTANK = this.create(AllItems.COPPER_BACKTANK)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShaped(
         b -> b.define('G', CreateRecipeProvider.I.shaft())
               .define('A', CreateRecipeProvider.I.andesiteAlloy())
               .define('B', CreateRecipeProvider.I.copperBlock())
               .define('P', CreateRecipeProvider.I.copper())
               .pattern("AGA")
               .pattern("PBP")
               .pattern(" P ")
      );
   BaseRecipeProvider.GeneratedRecipe DIVING_BOOTS = this.create(AllItems.COPPER_DIVING_BOOTS)
      .unlockedByTag(CreateRecipeProvider.I::copper)
      .viaShaped(
         b -> b.define('G', CreateRecipeProvider.I.andesiteAlloy()).define('P', CreateRecipeProvider.I.copper()).pattern("P P").pattern("P P").pattern("G G")
      );
   BaseRecipeProvider.GeneratedRecipe LINKED_CONTROLLER = this.create(AllItems.LINKED_CONTROLLER)
      .unlockedBy(AllBlocks.REDSTONE_LINK::get)
      .viaShaped(b -> b.define('S', ItemTags.WOODEN_BUTTONS).define('P', (ItemLike)AllBlocks.REDSTONE_LINK.get()).pattern("SSS").pattern(" P ").pattern("SSS"));
   BaseRecipeProvider.GeneratedRecipe CRAFTING_BLUEPRINT = this.create(AllItems.CRAFTING_BLUEPRINT)
      .unlockedBy(() -> Items.CRAFTING_TABLE)
      .viaShapeless(b -> b.requires(Items.PAINTING).requires(Items.CRAFTING_TABLE));
   BaseRecipeProvider.GeneratedRecipe SLIME_BALL = this.create((Supplier<ItemLike>)(() -> Items.SLIME_BALL))
      .unlockedBy(AllItems.DOUGH::get)
      .viaShapeless(b -> b.requires((ItemLike)AllItems.DOUGH.get()).requires(net.neoforged.neoforge.common.Tags.Items.DYES_LIME));
   BaseRecipeProvider.GeneratedRecipe BOOK = this.create((Supplier<ItemLike>)(() -> Items.BOOK))
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShapeless(b -> b.requires(CreateRecipeProvider.I.cardboard()).requires(Items.PAPER).requires(Items.PAPER).requires(Items.PAPER));
   BaseRecipeProvider.GeneratedRecipe NAME_TAG = this.create((Supplier<ItemLike>)(() -> Items.NAME_TAG))
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShapeless(
         b -> b.requires(net.neoforged.neoforge.common.Tags.Items.DYES_BLACK)
               .requires(net.neoforged.neoforge.common.Tags.Items.STRINGS)
               .requires(CreateRecipeProvider.I.cardboard())
      );
   BaseRecipeProvider.GeneratedRecipe ITEM_FRAME = this.create((Supplier<ItemLike>)(() -> Items.ITEM_FRAME))
      .unlockedBy(CreateRecipeProvider.I::cardboard)
      .viaShaped(
         b -> b.define('S', net.neoforged.neoforge.common.Tags.Items.RODS_WOODEN)
               .define('P', CreateRecipeProvider.I.cardboard())
               .pattern("SSS")
               .pattern("SPS")
               .pattern("SSS")
      );
   BaseRecipeProvider.GeneratedRecipe TREE_FERTILIZER = this.create(AllItems.TREE_FERTILIZER)
      .returns(2)
      .unlockedBy(() -> Items.BONE_MEAL)
      .viaShapeless(
         b -> b.requires(Ingredient.of(ItemTags.SMALL_FLOWERS), 2)
               .requires(Ingredient.of(new ItemLike[]{Items.HORN_CORAL, Items.BRAIN_CORAL, Items.TUBE_CORAL, Items.BUBBLE_CORAL, Items.FIRE_CORAL}))
               .requires(Items.BONE_MEAL)
      );
   BaseRecipeProvider.GeneratedRecipe NETHERITE_DIVING_HELMET = this.create(AllItems.NETHERITE_DIVING_HELMET)
      .viaNetheriteSmithing(AllItems.COPPER_DIVING_HELMET::get, CreateRecipeProvider.I::netherite);
   BaseRecipeProvider.GeneratedRecipe NETHERITE_BACKTANK = this.create(AllItems.NETHERITE_BACKTANK)
      .viaNetheriteSmithing(AllItems.COPPER_BACKTANK::get, CreateRecipeProvider.I::netherite);
   BaseRecipeProvider.GeneratedRecipe NETHERITE_DIVING_BOOTS = this.create(AllItems.NETHERITE_DIVING_BOOTS)
      .viaNetheriteSmithing(AllItems.COPPER_DIVING_BOOTS::get, CreateRecipeProvider.I::netherite);
   BaseRecipeProvider.GeneratedRecipe NETHERITE_DIVING_HELMET_2 = this.create(AllItems.NETHERITE_DIVING_HELMET)
      .withSuffix("_from_netherite")
      .viaNetheriteSmithing(() -> Items.NETHERITE_HELMET, () -> Ingredient.of(new ItemLike[]{(ItemLike)AllItems.COPPER_DIVING_HELMET.get()}));
   BaseRecipeProvider.GeneratedRecipe NETHERITE_BACKTANK_2 = this.create(AllItems.NETHERITE_BACKTANK)
      .withSuffix("_from_netherite")
      .viaNetheriteSmithing(() -> Items.NETHERITE_CHESTPLATE, () -> Ingredient.of(new ItemLike[]{(ItemLike)AllItems.COPPER_BACKTANK.get()}));
   BaseRecipeProvider.GeneratedRecipe NETHERITE_DIVING_BOOTS_2 = this.create(AllItems.NETHERITE_DIVING_BOOTS)
      .withSuffix("_from_netherite")
      .viaNetheriteSmithing(() -> Items.NETHERITE_BOOTS, () -> Ingredient.of(new ItemLike[]{(ItemLike)AllItems.COPPER_DIVING_BOOTS.get()}));
   private CreateStandardRecipeGen.Marker COOKING = this.enterFolder("/");
   BaseRecipeProvider.GeneratedRecipe DOUGH_TO_BREAD = this.create((Supplier<ItemLike>)(() -> Items.BREAD)).viaCooking(AllItems.DOUGH::get).inSmoker();
   BaseRecipeProvider.GeneratedRecipe SOUL_SAND = this.create(AllPaletteStoneTypes.SCORIA.getBaseBlock()::get).viaCooking(() -> Blocks.SOUL_SAND).inFurnace();
   BaseRecipeProvider.GeneratedRecipe FRAMED_GLASS = this.recycleGlass(AllPaletteBlocks.FRAMED_GLASS);
   BaseRecipeProvider.GeneratedRecipe TILED_GLASS = this.recycleGlass(AllPaletteBlocks.TILED_GLASS);
   BaseRecipeProvider.GeneratedRecipe VERTICAL_FRAMED_GLASS = this.recycleGlass(AllPaletteBlocks.VERTICAL_FRAMED_GLASS);
   BaseRecipeProvider.GeneratedRecipe HORIZONTAL_FRAMED_GLASS = this.recycleGlass(AllPaletteBlocks.HORIZONTAL_FRAMED_GLASS);
   BaseRecipeProvider.GeneratedRecipe FRAMED_GLASS_PANE = this.recycleGlassPane(AllPaletteBlocks.FRAMED_GLASS_PANE);
   BaseRecipeProvider.GeneratedRecipe TILED_GLASS_PANE = this.recycleGlassPane(AllPaletteBlocks.TILED_GLASS_PANE);
   BaseRecipeProvider.GeneratedRecipe VERTICAL_FRAMED_GLASS_PANE = this.recycleGlassPane(AllPaletteBlocks.VERTICAL_FRAMED_GLASS_PANE);
   BaseRecipeProvider.GeneratedRecipe HORIZONTAL_FRAMED_GLASS_PANE = this.recycleGlassPane(AllPaletteBlocks.HORIZONTAL_FRAMED_GLASS_PANE);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_IRON = this.blastCrushedMetal(() -> Items.IRON_INGOT, AllItems.CRUSHED_IRON::get);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_GOLD = this.blastCrushedMetal(() -> Items.GOLD_INGOT, AllItems.CRUSHED_GOLD::get);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_COPPER = this.blastCrushedMetal(() -> Items.COPPER_INGOT, AllItems.CRUSHED_COPPER::get);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_ZINC = this.blastCrushedMetal(AllItems.ZINC_INGOT::get, AllItems.CRUSHED_ZINC::get);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_OSMIUM = this.blastModdedCrushedMetal(AllItems.CRUSHED_OSMIUM, CommonMetal.OSMIUM);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_PLATINUM = this.blastModdedCrushedMetal(AllItems.CRUSHED_PLATINUM, CommonMetal.PLATINUM);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_SILVER = this.blastModdedCrushedMetal(AllItems.CRUSHED_SILVER, CommonMetal.SILVER);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_TIN = this.blastModdedCrushedMetal(AllItems.CRUSHED_TIN, CommonMetal.TIN);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_LEAD = this.blastModdedCrushedMetal(AllItems.CRUSHED_LEAD, CommonMetal.LEAD);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_QUICKSILVER = this.blastModdedCrushedMetal(AllItems.CRUSHED_QUICKSILVER, CommonMetal.QUICKSILVER);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_BAUXITE = this.blastModdedCrushedMetal(AllItems.CRUSHED_BAUXITE, CommonMetal.ALUMINUM);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_URANIUM = this.blastModdedCrushedMetal(AllItems.CRUSHED_URANIUM, CommonMetal.URANIUM);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_NICKEL = this.blastModdedCrushedMetal(AllItems.CRUSHED_NICKEL, CommonMetal.NICKEL);
   BaseRecipeProvider.GeneratedRecipe ZINC_ORE = this.create(AllItems.ZINC_INGOT::get)
      .withSuffix("_from_ore")
      .viaCookingTag(() -> CommonMetal.ZINC.ores.items())
      .rewardXP(1.0F)
      .inBlastFurnace();
   BaseRecipeProvider.GeneratedRecipe RAW_ZINC_ORE = this.create(AllItems.ZINC_INGOT::get)
      .withSuffix("_from_raw_ore")
      .viaCookingTag(() -> CommonMetal.ZINC.rawOres)
      .rewardXP(0.7F)
      .inBlastFurnace();
   BaseRecipeProvider.GeneratedRecipe UA_TREE_FERTILIZER = this.create(AllItems.TREE_FERTILIZER::get)
      .returns(2)
      .unlockedBy(() -> Items.BONE_MEAL)
      .whenModLoaded(Mods.UA.getId())
      .viaShapeless(b -> b.requires(Ingredient.of(ItemTags.SMALL_FLOWERS), 2).requires(AllTags.AllItemTags.UA_CORAL.tag).requires(Items.BONE_MEAL));
   String currentFolder = "";

   CreateStandardRecipeGen.Marker enterFolder(String folder) {
      this.currentFolder = folder;
      return new CreateStandardRecipeGen.Marker();
   }

   CreateStandardRecipeGen.GeneratedRecipeBuilder create(Supplier<ItemLike> result) {
      return new CreateStandardRecipeGen.GeneratedRecipeBuilder(this.currentFolder, result);
   }

   CreateStandardRecipeGen.GeneratedRecipeBuilder create(ResourceLocation result) {
      return new CreateStandardRecipeGen.GeneratedRecipeBuilder(this.currentFolder, result);
   }

   CreateStandardRecipeGen.GeneratedRecipeBuilder create(ItemProviderEntry<? extends ItemLike, ? extends ItemLike> result) {
      return this.create(result::get);
   }

   BaseRecipeProvider.GeneratedRecipe createSpecial(Function<CraftingBookCategory, Recipe<?>> builder, String recipeType, String path) {
      ResourceLocation location = Create.asResource(recipeType + "/" + this.currentFolder + "/" + path);
      return this.register(consumer -> {
         SpecialRecipeBuilder b = SpecialRecipeBuilder.special(builder);
         b.save(consumer, location.toString());
      });
   }

   BaseRecipeProvider.GeneratedRecipe blastCrushedMetal(Supplier<? extends ItemLike> result, Supplier<? extends ItemLike> ingredient) {
      return this.create(result::get).withSuffix("_from_crushed").viaCooking(ingredient).rewardXP(0.1F).inBlastFurnace();
   }

   BaseRecipeProvider.GeneratedRecipe blastModdedCrushedMetal(ItemEntry<? extends Item> ingredient, CommonMetal metal) {
      for (Mods mod : metal.mods) {
         String metalName = metal.getName(mod);
         ResourceLocation ingot = mod.ingotOf(metalName);
         String modId = mod.getId();
         this.create(ingot).withSuffix("_compat_" + modId).whenModLoaded(modId).viaCooking(ingredient::get).rewardXP(0.1F).inBlastFurnace();
      }

      return null;
   }

   BaseRecipeProvider.GeneratedRecipe recycleGlass(BlockEntry<? extends Block> ingredient) {
      return this.create((Supplier<ItemLike>)(() -> Blocks.GLASS))
         .withSuffix("_from_" + ingredient.getId().getPath())
         .viaCooking(ingredient::get)
         .forDuration(50)
         .inFurnace();
   }

   BaseRecipeProvider.GeneratedRecipe recycleGlassPane(BlockEntry<? extends Block> ingredient) {
      return this.create((Supplier<ItemLike>)(() -> Blocks.GLASS_PANE))
         .withSuffix("_from_" + ingredient.getId().getPath())
         .viaCooking(ingredient::get)
         .forDuration(50)
         .inFurnace();
   }

   BaseRecipeProvider.GeneratedRecipe metalCompacting(
      List<ItemProviderEntry<? extends ItemLike, ? extends ItemLike>> variants, List<Supplier<TagKey<Item>>> ingredients
   ) {
      BaseRecipeProvider.GeneratedRecipe result = null;

      for (int i = 0; i + 1 < variants.size(); i++) {
         ItemProviderEntry<? extends ItemLike, ? extends ItemLike> currentEntry = variants.get(i);
         ItemProviderEntry<? extends ItemLike, ? extends ItemLike> nextEntry = variants.get(i + 1);
         Supplier<TagKey<Item>> currentIngredient = ingredients.get(i);
         Supplier<TagKey<Item>> nextIngredient = ingredients.get(i + 1);
         result = this.create(nextEntry)
            .withSuffix("_from_compacting")
            .unlockedBy(currentEntry::get)
            .viaShaped(b -> b.pattern("###").pattern("###").pattern("###").define('#', (TagKey)currentIngredient.get()));
         result = this.create(currentEntry)
            .returns(9)
            .withSuffix("_from_decompacting")
            .unlockedBy(nextEntry::get)
            .viaShapeless(b -> b.requires((TagKey)nextIngredient.get()));
      }

      return result;
   }

   BaseRecipeProvider.GeneratedRecipe conversionCycle(List<ItemProviderEntry<? extends ItemLike, ? extends ItemLike>> cycle) {
      BaseRecipeProvider.GeneratedRecipe result = null;

      for (int i = 0; i < cycle.size(); i++) {
         ItemProviderEntry<? extends ItemLike, ? extends ItemLike> currentEntry = cycle.get(i);
         ItemProviderEntry<? extends ItemLike, ? extends ItemLike> nextEntry = cycle.get((i + 1) % cycle.size());
         result = this.create(nextEntry)
            .withSuffix("_from_conversion")
            .unlockedBy(currentEntry::get)
            .viaShapeless(b -> b.requires((ItemLike)currentEntry.get()));
      }

      return result;
   }

   BaseRecipeProvider.GeneratedRecipe clearData(ItemProviderEntry<? extends ItemLike, ? extends ItemLike> item) {
      return this.create(item).withSuffix("_clear").unlockedBy(item::get).viaShapeless(b -> b.requires((ItemLike)item.get()));
   }

   @Override
   public void buildRecipes(RecipeOutput output) {
      this.all.forEach(c -> c.register(output));
      Create.LOGGER.info("{} registered {} recipe{}", new Object[]{this.getName(), this.all.size(), this.all.size() == 1 ? "" : "s"});
   }

   @Override
   protected BaseRecipeProvider.GeneratedRecipe register(BaseRecipeProvider.GeneratedRecipe recipe) {
      this.all.add(recipe);
      return recipe;
   }

   public String getName() {
      return "Create's Standard Recipes";
   }

   public CreateStandardRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }

   class GeneratedRecipeBuilder {
      private String path;
      private String suffix;
      private Supplier<? extends ItemLike> result;
      private ResourceLocation compatDatagenOutput;
      List<ICondition> recipeConditions;
      private Supplier<ItemPredicate> unlockedBy;
      private int amount;

      private GeneratedRecipeBuilder(String path) {
         this.path = path;
         this.recipeConditions = new ArrayList<>();
         this.suffix = "";
         this.amount = 1;
      }

      public GeneratedRecipeBuilder(String path, Supplier<? extends ItemLike> result) {
         this(path);
         this.result = result;
      }

      public GeneratedRecipeBuilder(String path, ResourceLocation result) {
         this(path);
         this.compatDatagenOutput = result;
      }

      CreateStandardRecipeGen.GeneratedRecipeBuilder returns(int amount) {
         this.amount = amount;
         return this;
      }

      CreateStandardRecipeGen.GeneratedRecipeBuilder unlockedBy(Supplier<? extends ItemLike> item) {
         this.unlockedBy = () -> Builder.item().of(new ItemLike[]{(ItemLike)item.get()}).build();
         return this;
      }

      CreateStandardRecipeGen.GeneratedRecipeBuilder unlockedByTag(Supplier<TagKey<Item>> tag) {
         this.unlockedBy = () -> Builder.item().of((TagKey)tag.get()).build();
         return this;
      }

      CreateStandardRecipeGen.GeneratedRecipeBuilder whenModLoaded(String modid) {
         return this.withCondition(new ModLoadedCondition(modid));
      }

      CreateStandardRecipeGen.GeneratedRecipeBuilder whenModMissing(String modid) {
         return this.withCondition(new NotCondition(new ModLoadedCondition(modid)));
      }

      CreateStandardRecipeGen.GeneratedRecipeBuilder withCondition(ICondition condition) {
         this.recipeConditions.add(condition);
         return this;
      }

      CreateStandardRecipeGen.GeneratedRecipeBuilder withSuffix(String suffix) {
         this.suffix = suffix;
         return this;
      }

      BaseRecipeProvider.GeneratedRecipe viaShaped(UnaryOperator<ShapedRecipeBuilder> builder) {
         return CreateStandardRecipeGen.this.register(consumer -> {
            ShapedRecipeBuilder b = builder.apply(ShapedRecipeBuilder.shaped(RecipeCategory.MISC, (ItemLike)this.result.get(), this.amount));
            if (this.unlockedBy != null) {
               b.unlockedBy("has_item", CreateStandardRecipeGen.inventoryTrigger(new ItemPredicate[]{(ItemPredicate)this.unlockedBy.get()}));
            }

            b.save(consumer, this.createLocation("crafting"));
         });
      }

      BaseRecipeProvider.GeneratedRecipe viaShapeless(UnaryOperator<ShapelessRecipeBuilder> builder) {
         return CreateStandardRecipeGen.this.register(recipeOutput -> {
            ShapelessRecipeBuilder b = builder.apply(ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, (ItemLike)this.result.get(), this.amount));
            if (this.unlockedBy != null) {
               b.unlockedBy("has_item", CreateStandardRecipeGen.inventoryTrigger(new ItemPredicate[]{(ItemPredicate)this.unlockedBy.get()}));
            }

            RecipeOutput conditionalOutput = recipeOutput.withConditions(this.recipeConditions.toArray(new ICondition[0]));
            b.save(conditionalOutput, this.createLocation("crafting"));
         });
      }

      BaseRecipeProvider.GeneratedRecipe viaNetheriteSmithing(Supplier<? extends Item> base, Supplier<Ingredient> upgradeMaterial) {
         return CreateStandardRecipeGen.this.register(
            consumer -> {
               SmithingTransformRecipeBuilder b = SmithingTransformRecipeBuilder.smithing(
                  Ingredient.of(new ItemLike[]{Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE}),
                  Ingredient.of(new ItemLike[]{(ItemLike)base.get()}),
                  (Ingredient)upgradeMaterial.get(),
                  RecipeCategory.COMBAT,
                  ((ItemLike)this.result.get()).asItem()
               );
               b.unlocks(
                  "has_item", CreateStandardRecipeGen.inventoryTrigger(new ItemPredicate[]{Builder.item().of(new ItemLike[]{(ItemLike)base.get()}).build()})
               );
               b.save(consumer, this.createLocation("crafting"));
            }
         );
      }

      private ResourceLocation createSimpleLocation(String recipeType) {
         return Create.asResource(recipeType + "/" + this.getRegistryName().getPath() + this.suffix);
      }

      private ResourceLocation createLocation(String recipeType) {
         return Create.asResource(recipeType + "/" + this.path + "/" + this.getRegistryName().getPath() + this.suffix);
      }

      private ResourceLocation getRegistryName() {
         return this.compatDatagenOutput == null ? RegisteredObjectsHelper.getKeyOrThrow(((ItemLike)this.result.get()).asItem()) : this.compatDatagenOutput;
      }

      CreateStandardRecipeGen.GeneratedRecipeBuilder.GeneratedCookingRecipeBuilder viaCooking(Supplier<? extends ItemLike> item) {
         return this.unlockedBy(item).viaCookingIngredient(() -> Ingredient.of(new ItemLike[]{(ItemLike)item.get()}));
      }

      CreateStandardRecipeGen.GeneratedRecipeBuilder.GeneratedCookingRecipeBuilder viaCookingTag(Supplier<TagKey<Item>> tag) {
         return this.unlockedByTag(tag).viaCookingIngredient(() -> Ingredient.of((TagKey)tag.get()));
      }

      CreateStandardRecipeGen.GeneratedRecipeBuilder.GeneratedCookingRecipeBuilder viaCookingIngredient(Supplier<Ingredient> ingredient) {
         return new CreateStandardRecipeGen.GeneratedRecipeBuilder.GeneratedCookingRecipeBuilder(ingredient);
      }

      class GeneratedCookingRecipeBuilder {
         private Supplier<Ingredient> ingredient;
         private float exp;
         private int cookingTime;

         GeneratedCookingRecipeBuilder(Supplier<Ingredient> ingredient) {
            this.ingredient = ingredient;
            this.cookingTime = 200;
            this.exp = 0.0F;
         }

         CreateStandardRecipeGen.GeneratedRecipeBuilder.GeneratedCookingRecipeBuilder forDuration(int duration) {
            this.cookingTime = duration;
            return this;
         }

         CreateStandardRecipeGen.GeneratedRecipeBuilder.GeneratedCookingRecipeBuilder rewardXP(float xp) {
            this.exp = xp;
            return this;
         }

         BaseRecipeProvider.GeneratedRecipe inFurnace() {
            return this.inFurnace(b -> b);
         }

         BaseRecipeProvider.GeneratedRecipe inFurnace(UnaryOperator<SimpleCookingRecipeBuilder> builder) {
            return this.create(RecipeSerializer.SMELTING_RECIPE, builder, SmeltingRecipe::new, 1.0F);
         }

         BaseRecipeProvider.GeneratedRecipe inSmoker() {
            return this.inSmoker(b -> b);
         }

         BaseRecipeProvider.GeneratedRecipe inSmoker(UnaryOperator<SimpleCookingRecipeBuilder> builder) {
            this.create(RecipeSerializer.SMELTING_RECIPE, builder, SmeltingRecipe::new, 1.0F);
            this.create(RecipeSerializer.CAMPFIRE_COOKING_RECIPE, builder, CampfireCookingRecipe::new, 3.0F);
            return this.create(RecipeSerializer.SMOKING_RECIPE, builder, SmokingRecipe::new, 0.5F);
         }

         BaseRecipeProvider.GeneratedRecipe inBlastFurnace() {
            return this.inBlastFurnace(b -> b);
         }

         BaseRecipeProvider.GeneratedRecipe inBlastFurnace(UnaryOperator<SimpleCookingRecipeBuilder> builder) {
            this.create(RecipeSerializer.SMELTING_RECIPE, builder, SmeltingRecipe::new, 1.0F);
            return this.create(RecipeSerializer.BLASTING_RECIPE, builder, BlastingRecipe::new, 0.5F);
         }

         private <T extends AbstractCookingRecipe> BaseRecipeProvider.GeneratedRecipe create(
            RecipeSerializer<T> serializer, UnaryOperator<SimpleCookingRecipeBuilder> builder, Factory<T> factory, float cookingTimeModifier
         ) {
            return CreateStandardRecipeGen.this.register(
               recipeOutput -> {
                  boolean isOtherMod = GeneratedRecipeBuilder.this.compatDatagenOutput != null;
                  SimpleCookingRecipeBuilder b = builder.apply(
                     SimpleCookingRecipeBuilder.generic(
                        (Ingredient)this.ingredient.get(),
                        RecipeCategory.MISC,
                        (ItemLike)(isOtherMod ? Items.DIRT : (ItemLike)GeneratedRecipeBuilder.this.result.get()),
                        this.exp,
                        (int)((float)this.cookingTime * cookingTimeModifier),
                        serializer,
                        factory
                     )
                  );
                  if (GeneratedRecipeBuilder.this.unlockedBy != null) {
                     b.unlockedBy(
                        "has_item", CreateStandardRecipeGen.inventoryTrigger(new ItemPredicate[]{(ItemPredicate)GeneratedRecipeBuilder.this.unlockedBy.get()})
                     );
                  }

                  RecipeOutput conditionalOutput = recipeOutput.withConditions(GeneratedRecipeBuilder.this.recipeConditions.toArray(new ICondition[0]));
                  b.save(
                     (RecipeOutput)(isOtherMod
                        ? new CreateStandardRecipeGen.ModdedCookingRecipeOutput(conditionalOutput, GeneratedRecipeBuilder.this.compatDatagenOutput)
                        : conditionalOutput),
                     GeneratedRecipeBuilder.this.createSimpleLocation(RegisteredObjectsHelper.getKeyOrThrow(serializer).getPath())
                  );
               }
            );
         }
      }
   }

   static class Marker {
   }

   @ParametersAreNonnullByDefault
   @MethodsReturnNonnullByDefault
   private static record ModdedCookingRecipeOutput(RecipeOutput wrapped, ResourceLocation outputOverride) implements RecipeOutput {
      public net.minecraft.advancements.Advancement.Builder advancement() {
         return this.wrapped.advancement();
      }

      public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
         this.wrapped.accept(id, new CreateStandardRecipeGen.ModdedCookingRecipeOutputShim(recipe, this.outputOverride), advancement, conditions);
      }
   }

   @ParametersAreNonnullByDefault
   @MethodsReturnNonnullByDefault
   private static class ModdedCookingRecipeOutputShim implements Recipe<RecipeInput> {
      private static final Map<RecipeType<?>, CreateStandardRecipeGen.ModdedCookingRecipeOutputShim.Serializer> serializers = new ConcurrentHashMap<>();
      private final Recipe<?> wrapped;
      private final ResourceLocation overrideID;

      private ModdedCookingRecipeOutputShim(Recipe<?> wrapped, ResourceLocation overrideID) {
         this.wrapped = wrapped;
         this.overrideID = overrideID;
      }

      public boolean matches(RecipeInput recipeInput, Level level) {
         throw new AssertionError("Only for datagen output");
      }

      public ItemStack assemble(RecipeInput input, Provider registries) {
         throw new AssertionError("Only for datagen output");
      }

      public boolean canCraftInDimensions(int pWidth, int pHeight) {
         throw new AssertionError("Only for datagen output");
      }

      public ItemStack getResultItem(Provider registries) {
         throw new AssertionError("Only for datagen output");
      }

      public RecipeSerializer<?> getSerializer() {
         return (RecipeSerializer<?>)serializers.computeIfAbsent(
            this.getType(), t -> CreateStandardRecipeGen.ModdedCookingRecipeOutputShim.Serializer.create(this.wrapped)
         );
      }

      public RecipeType<?> getType() {
         return this.wrapped.getType();
      }

      private static record FakeItemStack(ResourceLocation id) {
         public static Codec<CreateStandardRecipeGen.ModdedCookingRecipeOutputShim.FakeItemStack> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(ResourceLocation.CODEC.fieldOf("id").forGetter(CreateStandardRecipeGen.ModdedCookingRecipeOutputShim.FakeItemStack::id))
                  .apply(instance, CreateStandardRecipeGen.ModdedCookingRecipeOutputShim.FakeItemStack::new)
         );
      }

      private static record Serializer(MapCodec<Recipe<?>> wrappedCodec) implements RecipeSerializer<CreateStandardRecipeGen.ModdedCookingRecipeOutputShim> {
         private static CreateStandardRecipeGen.ModdedCookingRecipeOutputShim.Serializer create(Recipe<?> wrapped) {
            RecipeSerializer<?> wrappedSerializer = wrapped.getSerializer();
            CreateStandardRecipeGen.ModdedCookingRecipeOutputShim.Serializer serializer = new CreateStandardRecipeGen.ModdedCookingRecipeOutputShim.Serializer(
               wrappedSerializer.codec()
            );
            if (BuiltInRegistries.RECIPE_SERIALIZER instanceof MappedRegistryAccessor<?> mra) {
               int wrappedId = mra.getToId().getOrDefault(wrappedSerializer, -1);
               ResourceKey<RecipeSerializer<?>> wrappedKey = mra.getByValue().get(wrappedSerializer).key();
               mra.getToId().put(serializer, wrappedId);
               ((Map<CreateStandardRecipeGen.ModdedCookingRecipeOutputShim.Serializer, Reference<?>>)mra.getByValue())
                  .put(serializer, Reference.createStandAlone(null, wrappedKey));
               return serializer;
            } else {
               throw new AssertionError(
                  "ModdedCookingRecipeOutputShim will not be able to serialize without injecting into a registry. Expected BuiltInRegistries.RECIPE_SERIALIZER to be of class MappedRegistry, is of class "
                     + BuiltInRegistries.RECIPE_SERIALIZER.getClass()
               );
            }
         }

         public MapCodec<CreateStandardRecipeGen.ModdedCookingRecipeOutputShim> codec() {
            return RecordCodecBuilder.mapCodec(
               instance -> instance.group(
                        this.wrappedCodec.forGetter(i -> i.wrapped),
                        CreateStandardRecipeGen.ModdedCookingRecipeOutputShim.FakeItemStack.CODEC
                           .fieldOf("result")
                           .forGetter(i -> new CreateStandardRecipeGen.ModdedCookingRecipeOutputShim.FakeItemStack(i.overrideID))
                     )
                     .apply(instance, (wrappedRecipe, fakeItemStack) -> {
                        throw new AssertionError("Only for datagen output");
                     })
            );
         }

         public StreamCodec<RegistryFriendlyByteBuf, CreateStandardRecipeGen.ModdedCookingRecipeOutputShim> streamCodec() {
            throw new AssertionError("Only for datagen output");
         }
      }
   }
}
