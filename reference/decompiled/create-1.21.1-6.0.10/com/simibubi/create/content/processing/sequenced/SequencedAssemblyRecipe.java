package com.simibubi.create.content.processing.sequenced;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class SequencedAssemblyRecipe implements Recipe<RecipeWrapper> {
   protected SequencedAssemblyRecipeSerializer serializer;
   protected Ingredient ingredient;
   protected List<SequencedRecipe<?>> sequence;
   protected int loops;
   protected ProcessingOutput transitionalItem;
   public final List<ProcessingOutput> resultPool;

   public SequencedAssemblyRecipe(SequencedAssemblyRecipeSerializer serializer) {
      this.serializer = serializer;
      this.sequence = new ArrayList<>();
      this.resultPool = new ArrayList<>();
      this.loops = 5;
   }

   public static <I extends RecipeInput, R extends ProcessingRecipe<I, ?>> Optional<RecipeHolder<R>> getRecipe(
      Level world, I inv, RecipeType<R> type, Class<R> recipeClass
   ) {
      return getRecipe(world, inv, type, recipeClass, r -> ((ProcessingRecipe)r.value()).matches(inv, world));
   }

   public static <I extends RecipeInput, R extends ProcessingRecipe<I, ?>> Optional<RecipeHolder<R>> getRecipe(
      Level world, I inv, RecipeType<R> type, Class<R> recipeClass, Predicate<? super RecipeHolder<R>> recipeFilter
   ) {
      List<RecipeHolder<R>> list = getRecipes(world, inv.getItem(0), type, recipeClass, recipeFilter);
      return !list.isEmpty() ? Optional.of(list.getFirst()) : Optional.empty();
   }

   public static <R extends ProcessingRecipe<?, ?>> Optional<RecipeHolder<R>> getRecipe(Level level, ItemStack item, RecipeType<R> type, Class<R> recipeClass) {
      for (RecipeHolder<SequencedAssemblyRecipe> sequencedAssemblyRecipe : level.getRecipeManager()
         .getAllRecipesFor(AllRecipeTypes.SEQUENCED_ASSEMBLY.getType())) {
         if (((SequencedAssemblyRecipe)sequencedAssemblyRecipe.value()).appliesTo(sequencedAssemblyRecipe.id(), item)) {
            SequencedRecipe<?> nextRecipe = ((SequencedAssemblyRecipe)sequencedAssemblyRecipe.value()).getNextRecipe(item);
            ProcessingRecipe<?, ?> recipe = nextRecipe.getRecipe();
            if (recipe.getType() == type && recipeClass.isInstance(recipe)) {
               recipe.enforceNextResult(
                  () -> ((SequencedAssemblyRecipe)sequencedAssemblyRecipe.value()).advance(sequencedAssemblyRecipe.id(), item, level.random)
               );
               return Optional.of(new RecipeHolder(sequencedAssemblyRecipe.id(), recipeClass.cast(recipe)));
            }
         }
      }

      return Optional.empty();
   }

   public static <R extends ProcessingRecipe<?, ?>> List<RecipeHolder<R>> getRecipes(
      Level level, ItemStack item, RecipeType<R> type, Class<R> recipeClass, Predicate<? super RecipeHolder<R>> recipeFilter
   ) {
      List<RecipeHolder<SequencedAssemblyRecipe>> all = level.getRecipeManager().getAllRecipesFor(AllRecipeTypes.SEQUENCED_ASSEMBLY.getType());
      List<RecipeHolder<R>> result = new ArrayList<>();

      for (RecipeHolder<SequencedAssemblyRecipe> holder : all) {
         if (((SequencedAssemblyRecipe)holder.value()).appliesTo(holder.id(), item)) {
            ProcessingRecipe<?, ?> recipe = ((SequencedAssemblyRecipe)holder.value()).getNextRecipe(item).getRecipe();
            if (recipe.getType() == type && recipeClass.isInstance(recipe)) {
               recipe.enforceNextResult(() -> ((SequencedAssemblyRecipe)holder.value()).advance(holder.id(), item, level.random));
               R castedRecipe = (R)recipeClass.cast(recipe);
               RecipeHolder<R> h = new RecipeHolder(holder.id(), castedRecipe);
               if (recipeFilter.test(h)) {
                  result.add(h);
               }
            }
         }
      }

      return result;
   }

   private ItemStack advance(ResourceLocation id, ItemStack input, RandomSource random) {
      int step = this.getStep(input);
      if ((step + 1) / this.sequence.size() >= this.loops) {
         return this.rollResult(random);
      } else {
         ItemStack advancedItem = this.getTransitionalItem().copyWithCount(1);
         SequencedAssemblyRecipe.SequencedAssembly sequencedAssembly = new SequencedAssemblyRecipe.SequencedAssembly(
            id, step + 1, ((float)step + 1.0F) / (float)(this.sequence.size() * this.loops)
         );
         advancedItem.set(AllDataComponents.SEQUENCED_ASSEMBLY, sequencedAssembly);
         return advancedItem;
      }
   }

   public int getLoops() {
      return this.loops;
   }

   private ItemStack rollResult(RandomSource random) {
      float totalWeight = 0.0F;

      for (ProcessingOutput entry : this.resultPool) {
         totalWeight += entry.getChance();
      }

      float number = random.nextFloat() * totalWeight;

      for (ProcessingOutput entry : this.resultPool) {
         number -= entry.getChance();
         if (number < 0.0F) {
            return entry.getStack().copy();
         }
      }

      return ItemStack.EMPTY;
   }

   private boolean appliesTo(ResourceLocation id, ItemStack input) {
      return !input.has(AllDataComponents.SEQUENCED_ASSEMBLY)
         ? this.ingredient.test(input)
         : this.getTransitionalItem().getItem() == input.getItem()
            && ((SequencedAssemblyRecipe.SequencedAssembly)input.get(AllDataComponents.SEQUENCED_ASSEMBLY)).id().equals(id);
   }

   private SequencedRecipe<?> getNextRecipe(ItemStack input) {
      return this.sequence.get(this.getStep(input) % this.sequence.size());
   }

   private int getStep(ItemStack input) {
      return !input.has(AllDataComponents.SEQUENCED_ASSEMBLY)
         ? 0
         : ((SequencedAssemblyRecipe.SequencedAssembly)input.get(AllDataComponents.SEQUENCED_ASSEMBLY)).step();
   }

   public boolean matches(RecipeWrapper inv, Level level) {
      return false;
   }

   public ItemStack assemble(RecipeWrapper input, Provider registries) {
      return ItemStack.EMPTY;
   }

   public boolean canCraftInDimensions(int width, int height) {
      return false;
   }

   public ItemStack getResultItem(Provider registries) {
      return this.resultPool.getFirst().getStack();
   }

   public float getOutputChance() {
      float totalWeight = 0.0F;

      for (ProcessingOutput entry : this.resultPool) {
         totalWeight += entry.getChance();
      }

      return this.resultPool.getFirst().getChance() / totalWeight;
   }

   public RecipeSerializer<?> getSerializer() {
      return this.serializer;
   }

   public boolean isSpecial() {
      return true;
   }

   public RecipeType<?> getType() {
      return AllRecipeTypes.SEQUENCED_ASSEMBLY.getType();
   }

   @OnlyIn(Dist.CLIENT)
   public static void addToTooltip(ItemTooltipEvent event) {
      ItemStack stack = event.getItemStack();
      if (stack.has(AllDataComponents.SEQUENCED_ASSEMBLY)) {
         SequencedAssemblyRecipe.SequencedAssembly sequencedAssembly = (SequencedAssemblyRecipe.SequencedAssembly)stack.get(
            AllDataComponents.SEQUENCED_ASSEMBLY
         );
         Optional<RecipeHolder<? extends Recipe<?>>> optionalRecipe = Minecraft.getInstance().level.getRecipeManager().byKey(sequencedAssembly.id());
         if (!optionalRecipe.isEmpty()) {
            if (optionalRecipe.get().value() instanceof SequencedAssemblyRecipe sequencedAssemblyRecipe) {
               int length = sequencedAssemblyRecipe.sequence.size();
               int step = sequencedAssemblyRecipe.getStep(stack);
               int total = length * sequencedAssemblyRecipe.loops;
               List<Component> tooltip = event.getToolTip();
               tooltip.add(CommonComponents.EMPTY);
               tooltip.add(CreateLang.translateDirect("recipe.sequenced_assembly").withStyle(ChatFormatting.GRAY));
               tooltip.add(CreateLang.translateDirect("recipe.assembly.progress", step, total).withStyle(ChatFormatting.DARK_GRAY));
               int remaining = total - step;

               for (int i = 0; i < length && i < remaining; i++) {
                  SequencedRecipe<?> sequencedRecipe = sequencedAssemblyRecipe.sequence.get((i + step) % length);
                  Component textComponent = sequencedRecipe.getAsAssemblyRecipe().getDescriptionForAssembly();
                  if (i == 0) {
                     tooltip.add(CreateLang.translateDirect("recipe.assembly.next", textComponent).withStyle(ChatFormatting.AQUA));
                  } else {
                     tooltip.add(Component.literal("-> ").append(textComponent).withStyle(ChatFormatting.DARK_AQUA));
                  }
               }
            }
         }
      }
   }

   public Ingredient getIngredient() {
      return this.ingredient;
   }

   public List<SequencedRecipe<?>> getSequence() {
      return this.sequence;
   }

   public ItemStack getTransitionalItem() {
      return this.transitionalItem.getStack();
   }

   public static record SequencedAssembly(ResourceLocation id, int step, float progress) {
      public static final Codec<SequencedAssemblyRecipe.SequencedAssembly> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  ResourceLocation.CODEC.fieldOf("id").forGetter(SequencedAssemblyRecipe.SequencedAssembly::id),
                  Codec.INT.fieldOf("step").forGetter(SequencedAssemblyRecipe.SequencedAssembly::step),
                  Codec.FLOAT.fieldOf("progress").forGetter(SequencedAssemblyRecipe.SequencedAssembly::progress)
               )
               .apply(i, SequencedAssemblyRecipe.SequencedAssembly::new)
      );
      public static final StreamCodec<ByteBuf, SequencedAssemblyRecipe.SequencedAssembly> STREAM_CODEC = StreamCodec.composite(
         ResourceLocation.STREAM_CODEC,
         SequencedAssemblyRecipe.SequencedAssembly::id,
         ByteBufCodecs.INT,
         SequencedAssemblyRecipe.SequencedAssembly::step,
         ByteBufCodecs.FLOAT,
         SequencedAssemblyRecipe.SequencedAssembly::progress,
         SequencedAssemblyRecipe.SequencedAssembly::new
      );
   }
}
