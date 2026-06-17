package com.simibubi.create.compat.jei.category;

import com.simibubi.create.Create;
import com.simibubi.create.compat.jei.category.animations.AnimatedItemDrain;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.fluids.transfer.EmptyingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.item.ItemHelper;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.function.Consumer;
import javax.annotation.ParametersAreNonnullByDefault;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

@ParametersAreNonnullByDefault
public class ItemDrainCategory extends CreateRecipeCategory<EmptyingRecipe> {
   private final AnimatedItemDrain drain = new AnimatedItemDrain();

   public ItemDrainCategory(CreateRecipeCategory.Info<EmptyingRecipe> info) {
      super(info);
   }

   public static void consumeRecipes(Consumer<RecipeHolder<EmptyingRecipe>> consumer, IIngredientManager ingredientManager) {
      ObjectOpenCustomHashSet<ItemStack> emptiedItems = new ObjectOpenCustomHashSet(ItemStackLinkedSet.TYPE_AND_TAG);

      for (ItemStack stack : ingredientManager.getAllIngredients(VanillaTypes.ITEM_STACK)) {
         if (PotionFluidHandler.isPotionItem(stack)) {
            FluidStack fluidFromPotionItem = PotionFluidHandler.getFluidFromPotionItem(stack);
            Ingredient potion = Ingredient.of(new ItemStack[]{stack});
            ResourceLocation id = Create.asResource("potions");
            EmptyingRecipe recipe = new StandardProcessingRecipe.Builder<>(EmptyingRecipe::new, id)
               .withItemIngredients(new Ingredient[]{potion})
               .withFluidOutputs(new FluidStack[]{fluidFromPotionItem})
               .withSingleItemOutput(new ItemStack(Items.GLASS_BOTTLE))
               .build();
            consumer.accept(new RecipeHolder(id, recipe));
         } else {
            IFluidHandlerItem capability = (IFluidHandlerItem)stack.getCapability(FluidHandler.ITEM);
            if (capability != null) {
               ItemStack copy = stack.copy();
               capability = (IFluidHandlerItem)copy.getCapability(FluidHandler.ITEM);
               FluidStack extracted = capability.drain(1000, FluidAction.EXECUTE);
               ItemStack result = capability.getContainer();
               if (!extracted.isEmpty() && !result.isEmpty()) {
                  result = ItemHelper.sameItem(stack, result) ? stack : (ItemStack)emptiedItems.addOrGet(result);
                  Ingredient ingredient = Ingredient.of(new ItemStack[]{stack});
                  ResourceLocation itemName = RegisteredObjectsHelper.getKeyOrThrow(stack.getItem());
                  ResourceLocation fluidName = RegisteredObjectsHelper.getKeyOrThrow(extracted.getFluid());
                  ResourceLocation id = Create.asResource(
                     "empty_" + itemName.getNamespace() + "_" + itemName.getPath() + "_of_" + fluidName.getNamespace() + "_" + fluidName.getPath()
                  );
                  EmptyingRecipe recipe = new StandardProcessingRecipe.Builder<>(EmptyingRecipe::new, id)
                     .withItemIngredients(new Ingredient[]{ingredient})
                     .withFluidOutputs(new FluidStack[]{extracted})
                     .withSingleItemOutput(result)
                     .build();
                  consumer.accept(new RecipeHolder(id, recipe));
               }
            }
         }
      }
   }

   public void setRecipe(IRecipeLayoutBuilder builder, EmptyingRecipe recipe, IFocusGroup focuses) {
      builder.addSlot(RecipeIngredientRole.INPUT, 27, 8).setBackground(getRenderedSlot(), -1, -1).addIngredients((Ingredient)recipe.getIngredients().get(0));
      addFluidSlot(builder, 132, 8, recipe.getResultingFluid());
      builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 27).setBackground(getRenderedSlot(), -1, -1).addItemStack(getResultItem(recipe));
   }

   public void draw(EmptyingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
      AllGuiTextures.JEI_SHADOW.render(graphics, 62, 37);
      AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 73, 4);
      this.drain.withFluid(recipe.getResultingFluid()).draw(graphics, this.getBackground().getWidth() / 2 - 13, 40);
   }
}
