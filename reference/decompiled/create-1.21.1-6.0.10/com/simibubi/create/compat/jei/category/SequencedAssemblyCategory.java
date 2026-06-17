package com.simibubi.create.compat.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;
import java.lang.invoke.StringConcatFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.ParametersAreNonnullByDefault;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.NotNull;

@ParametersAreNonnullByDefault
public class SequencedAssemblyCategory extends CreateRecipeCategory<SequencedAssemblyRecipe> {
   Map<ResourceLocation, SequencedAssemblySubCategory> subCategories = new HashMap<>();
   final String[] romans = new String[]{"I", "II", "III", "IV", "V", "VI", "-"};

   public SequencedAssemblyCategory(CreateRecipeCategory.Info<SequencedAssemblyRecipe> info) {
      super(info);
   }

   public void setRecipe(IRecipeLayoutBuilder builder, SequencedAssemblyRecipe recipe, IFocusGroup focuses) {
      boolean noRandomOutput = recipe.getOutputChance() == 1.0F;
      int xOffset = noRandomOutput ? 0 : -7;
      builder.addSlot(RecipeIngredientRole.INPUT, 27 + xOffset, 91)
         .setBackground(getRenderedSlot(), -1, -1)
         .addItemStacks(List.of(recipe.getIngredient().getItems()));
      ((IRecipeSlotBuilder)builder.addSlot(RecipeIngredientRole.OUTPUT, 132 + xOffset, 91)
            .setBackground(getRenderedSlot(recipe.getOutputChance()), -1, -1)
            .addItemStack(getResultItem(recipe)))
         .addTooltipCallback((recipeSlotView, tooltip) -> {
            if (!noRandomOutput) {
               float chance = recipe.getOutputChance();
               tooltip.add(1, this.chanceComponent(chance));
            }
         });
      int width = 0;
      int margin = 3;

      for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence()) {
         width += this.getSubCategory(sequencedRecipe).getWidth() + margin;
      }

      width -= margin;
      int x = width / -2 + this.getBackground().getWidth() / 2;

      for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence()) {
         SequencedAssemblySubCategory subCategory = this.getSubCategory(sequencedRecipe);
         subCategory.setRecipe(builder, sequencedRecipe, focuses, x);
         x += subCategory.getWidth() + margin;
      }

      for (int i = 1; i < recipe.getLoops(); i++) {
         for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence()) {
            NonNullList<Ingredient> sequencedIngredients = sequencedRecipe.getRecipe().getIngredients();

            for (Ingredient ingredient : sequencedIngredients.subList(1, sequencedIngredients.size())) {
               builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredients(ingredient);
            }

            for (SizedFluidIngredient fluidIngredient : sequencedRecipe.getRecipe().getFluidIngredients()) {
               builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                  .addIngredients(NeoForgeTypes.FLUID_STACK, Arrays.asList(fluidIngredient.getFluids()));
            }
         }
      }
   }

   private SequencedAssemblySubCategory getSubCategory(SequencedRecipe<?> sequencedRecipe) {
      return this.subCategories
         .computeIfAbsent(
            RegisteredObjectsHelper.getKeyOrThrow(sequencedRecipe.getRecipe().getSerializer()),
            rl -> sequencedRecipe.getAsAssemblyRecipe().getJEISubCategory().get().get()
         );
   }

   public void draw(SequencedAssemblyRecipe recipe, IRecipeSlotsView iRecipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
      Font font = Minecraft.getInstance().font;
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.pushPose();
      matrixStack.translate(0.0F, 15.0F, 0.0F);
      boolean singleOutput = recipe.getOutputChance() == 1.0F;
      int xOffset = singleOutput ? 0 : -7;
      AllGuiTextures.JEI_LONG_ARROW.render(graphics, 52 + xOffset, 79);
      if (!singleOutput) {
         AllGuiTextures.JEI_CHANCE_SLOT.render(graphics, 150 + xOffset, 75);
         Component component = Component.literal("?").withStyle(ChatFormatting.BOLD);
         graphics.drawString(font, component, font.width(component) / -2 + 8 + 150 + xOffset, 80, 15724527);
      }

      if (recipe.getLoops() > 1) {
         matrixStack.pushPose();
         matrixStack.translate(15.0F, 9.0F, 0.0F);
         AllIcons.I_SEQ_REPEAT.render(graphics, 50 + xOffset, 75);
         Component repeat = Component.literal("x" + recipe.getLoops());
         graphics.drawString(font, repeat, 66 + xOffset, 80, 8947848, false);
         matrixStack.popPose();
      }

      matrixStack.popPose();
      int width = 0;
      int margin = 3;

      for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence()) {
         width += this.getSubCategory(sequencedRecipe).getWidth() + margin;
      }

      width -= margin;
      matrixStack.translate((float)(width / -2 + this.getBackground().getWidth() / 2), 0.0F, 0.0F);
      matrixStack.pushPose();
      List<SequencedRecipe<?>> sequence = recipe.getSequence();

      for (int i = 0; i < sequence.size(); i++) {
         SequencedRecipe<?> sequencedRecipe = sequence.get(i);
         SequencedAssemblySubCategory subCategory = this.getSubCategory(sequencedRecipe);
         int subWidth = subCategory.getWidth();
         MutableComponent component = Component.literal(
            StringConcatFactory.makeConcatWithConstants<"makeConcatWithConstants","\u0001">(this.romans[Math.min(i, 6)])
         );
         graphics.drawString(font, component, font.width(component) / -2 + subWidth / 2, 2, 8947848, false);
         subCategory.draw(sequencedRecipe, graphics, mouseX, mouseY, i);
         matrixStack.translate((float)(subWidth + margin), 0.0F, 0.0F);
      }

      matrixStack.popPose();
      matrixStack.popPose();
   }

   @NotNull
   public List<Component> getTooltipStrings(SequencedAssemblyRecipe recipe, IRecipeSlotsView iRecipeSlotsView, double mouseX, double mouseY) {
      List<Component> tooltip = new ArrayList<>();
      MutableComponent junk = CreateLang.translateDirect("recipe.assembly.junk");
      boolean singleOutput = recipe.getOutputChance() == 1.0F;
      boolean willRepeat = recipe.getLoops() > 1;
      int xOffset = -7;
      int minX = 150 + xOffset;
      int maxX = minX + 18;
      int minY = 90;
      int maxY = minY + 18;
      if (!singleOutput && mouseX >= (double)minX && mouseX < (double)maxX && mouseY >= (double)minY && mouseY < (double)maxY) {
         float chance = recipe.getOutputChance();
         tooltip.add(junk);
         tooltip.add(this.chanceComponent(1.0F - chance));
         return tooltip;
      } else {
         minX = 55 + xOffset;
         maxX = minX + 65;
         int var27 = 92;
         maxY = var27 + 24;
         if (willRepeat && mouseX >= (double)minX && mouseX < (double)maxX && mouseY >= (double)var27 && mouseY < (double)maxY) {
            tooltip.add(CreateLang.translateDirect("recipe.assembly.repeat", recipe.getLoops()));
            return tooltip;
         } else {
            if (mouseY > 5.0 && mouseY < 84.0) {
               int width = 0;
               int margin = 3;

               for (SequencedRecipe<?> sequencedRecipe : recipe.getSequence()) {
                  width += this.getSubCategory(sequencedRecipe).getWidth() + margin;
               }

               width -= margin;
               xOffset = width / 2 + this.getBackground().getWidth() / -2;
               double relativeX = mouseX + (double)xOffset;
               List<SequencedRecipe<?>> sequence = recipe.getSequence();

               for (int i = 0; i < sequence.size(); i++) {
                  SequencedRecipe<?> sequencedRecipe = sequence.get(i);
                  SequencedAssemblySubCategory subCategory = this.getSubCategory(sequencedRecipe);
                  if (relativeX >= 0.0 && relativeX < (double)subCategory.getWidth()) {
                     tooltip.add(CreateLang.translateDirect("recipe.assembly.step", i + 1));
                     tooltip.add(sequencedRecipe.getAsAssemblyRecipe().getDescriptionForAssembly().plainCopy().withStyle(ChatFormatting.DARK_GREEN));
                     return tooltip;
                  }

                  relativeX -= (double)(subCategory.getWidth() + margin);
               }
            }

            return tooltip;
         }
      }
   }

   protected MutableComponent chanceComponent(float chance) {
      String number = (double)chance < 0.01 ? "<1" : ((double)chance > 0.99 ? ">99" : String.valueOf(Math.round(chance * 100.0F)));
      return CreateLang.translateDirect("recipe.processing.chance", number).withStyle(ChatFormatting.GOLD);
   }
}
