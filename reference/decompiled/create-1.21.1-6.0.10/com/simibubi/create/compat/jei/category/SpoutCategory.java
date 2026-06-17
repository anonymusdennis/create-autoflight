package com.simibubi.create.compat.jei.category;

import com.simibubi.create.Create;
import com.simibubi.create.compat.jei.category.animations.AnimatedSpout;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.item.ItemHelper;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import javax.annotation.ParametersAreNonnullByDefault;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

@ParametersAreNonnullByDefault
public class SpoutCategory extends CreateRecipeCategory<FillingRecipe> {
   private final AnimatedSpout spout = new AnimatedSpout();

   public SpoutCategory(CreateRecipeCategory.Info<FillingRecipe> info) {
      super(info);
   }

   public static void consumeRecipes(Consumer<RecipeHolder<FillingRecipe>> consumer, IIngredientManager ingredientManager) {
      Collection<FluidStack> fluidStacks = ingredientManager.getAllIngredients(NeoForgeTypes.FLUID_STACK);

      for (ItemStack stack : ingredientManager.getAllIngredients(VanillaTypes.ITEM_STACK)) {
         if (PotionFluidHandler.isPotionItem(stack)) {
            FluidStack fluidFromPotionItem = PotionFluidHandler.getFluidFromPotionItem(stack);
            Ingredient bottle = Ingredient.of(new ItemLike[]{Items.GLASS_BOTTLE});
            ResourceLocation id = Create.asResource("potions");
            SizedFluidIngredient fluidIngredient = new SizedFluidIngredient(
               DataComponentFluidIngredient.of(false, fluidFromPotionItem), fluidFromPotionItem.getAmount()
            );
            FillingRecipe recipe = new StandardProcessingRecipe.Builder<>(FillingRecipe::new, id)
               .withItemIngredients(new Ingredient[]{bottle})
               .withFluidIngredients(new SizedFluidIngredient[]{fluidIngredient})
               .withSingleItemOutput(stack)
               .build();
            consumer.accept(new RecipeHolder(id, recipe));
         } else {
            IFluidHandlerItem capability = (IFluidHandlerItem)stack.getCapability(FluidHandler.ITEM);
            if (capability != null) {
               int numTanks = capability.getTanks();
               FluidStack existingFluid = numTanks == 1 ? capability.getFluidInTank(0) : FluidStack.EMPTY;

               for (FluidStack fluidStack : fluidStacks) {
                  if (numTanks != 1 || existingFluid.isEmpty() || FluidStack.isSameFluidSameComponents(existingFluid, fluidStack)) {
                     ItemStack copy = stack.copy();
                     IFluidHandlerItem fhi = (IFluidHandlerItem)copy.getCapability(FluidHandler.ITEM);
                     if (fhi != null && GenericItemFilling.isFluidHandlerValid(copy, fhi)) {
                        FluidStack fluidCopy = fluidStack.copy();
                        fluidCopy.setAmount(1000);
                        fhi.fill(fluidCopy, FluidAction.EXECUTE);
                        ItemStack container = fhi.getContainer();
                        if (!ItemHelper.sameItem(container, copy) && !container.isEmpty()) {
                           Ingredient bucket = Ingredient.of(new ItemStack[]{stack});
                           ResourceLocation itemName = RegisteredObjectsHelper.getKeyOrThrow(stack.getItem());
                           ResourceLocation fluidName = RegisteredObjectsHelper.getKeyOrThrow(fluidCopy.getFluid());
                           ResourceLocation id = Create.asResource(
                              "fill_" + itemName.getNamespace() + "_" + itemName.getPath() + "_with_" + fluidName.getNamespace() + "_" + fluidName.getPath()
                           );
                           SizedFluidIngredient fluidIngredient = new SizedFluidIngredient(
                              DataComponentFluidIngredient.of(false, fluidCopy), fluidCopy.getAmount()
                           );
                           FillingRecipe recipe = new StandardProcessingRecipe.Builder<>(FillingRecipe::new, id)
                              .withItemIngredients(new Ingredient[]{bucket})
                              .withFluidIngredients(new SizedFluidIngredient[]{fluidIngredient})
                              .withSingleItemOutput(container)
                              .build();
                           consumer.accept(new RecipeHolder(id, recipe));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void setRecipe(IRecipeLayoutBuilder builder, FillingRecipe recipe, IFocusGroup focuses) {
      builder.addSlot(RecipeIngredientRole.INPUT, 27, 51).setBackground(getRenderedSlot(), -1, -1).addIngredients((Ingredient)recipe.getIngredients().get(0));
      addFluidSlot(builder, 27, 32, recipe.getRequiredFluid());
      builder.addSlot(RecipeIngredientRole.OUTPUT, 132, 51).setBackground(getRenderedSlot(), -1, -1).addItemStack(getResultItem(recipe));
   }

   public void draw(FillingRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
      AllGuiTextures.JEI_SHADOW.render(graphics, 62, 57);
      AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 126, 29);
      this.spout.withFluids(Arrays.asList(recipe.getRequiredFluid().getFluids())).draw(graphics, this.getBackground().getWidth() / 2 - 13, 22);
   }
}
