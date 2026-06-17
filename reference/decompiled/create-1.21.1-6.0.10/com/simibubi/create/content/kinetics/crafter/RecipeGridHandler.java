package com.simibubi.create.content.kinetics.crafter;

import com.google.common.base.Predicates;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.Pointing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.FireworkRocketRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

public class RecipeGridHandler {
   public static List<MechanicalCrafterBlockEntity> getAllCraftersOfChain(MechanicalCrafterBlockEntity root) {
      return getAllCraftersOfChainIf(root, Predicates.alwaysTrue());
   }

   public static List<MechanicalCrafterBlockEntity> getAllCraftersOfChainIf(MechanicalCrafterBlockEntity root, Predicate<MechanicalCrafterBlockEntity> test) {
      return getAllCraftersOfChainIf(root, test, false);
   }

   public static List<MechanicalCrafterBlockEntity> getAllCraftersOfChainIf(
      MechanicalCrafterBlockEntity root, Predicate<MechanicalCrafterBlockEntity> test, boolean poweredStart
   ) {
      List<MechanicalCrafterBlockEntity> crafters = new ArrayList<>();
      List<Pair<MechanicalCrafterBlockEntity, MechanicalCrafterBlockEntity>> frontier = new ArrayList<>();
      Set<MechanicalCrafterBlockEntity> visited = new HashSet<>();
      frontier.add(Pair.of(root, null));
      boolean empty = false;
      boolean allEmpty = true;

      while (!frontier.isEmpty()) {
         Pair<MechanicalCrafterBlockEntity, MechanicalCrafterBlockEntity> pair = frontier.remove(0);
         MechanicalCrafterBlockEntity current = (MechanicalCrafterBlockEntity)pair.getKey();
         MechanicalCrafterBlockEntity last = (MechanicalCrafterBlockEntity)pair.getValue();
         if (visited.contains(current)) {
            return null;
         }

         if (!test.test(current)) {
            empty = true;
         } else {
            allEmpty = false;
         }

         crafters.add(current);
         visited.add(current);
         MechanicalCrafterBlockEntity target = getTargetingCrafter(current);
         if (target != last && target != null) {
            frontier.add(Pair.of(target, current));
         }

         for (MechanicalCrafterBlockEntity preceding : getPrecedingCrafters(current)) {
            if (preceding != last) {
               frontier.add(Pair.of(preceding, current));
            }
         }
      }

      return (!empty || poweredStart) && !allEmpty ? crafters : null;
   }

   public static MechanicalCrafterBlockEntity getTargetingCrafter(MechanicalCrafterBlockEntity crafter) {
      BlockState state = crafter.getBlockState();
      if (!isCrafter(state)) {
         return null;
      } else {
         BlockPos targetPos = crafter.getBlockPos().relative(MechanicalCrafterBlock.getTargetDirection(state));
         MechanicalCrafterBlockEntity targetBE = CrafterHelper.getCrafter(crafter.getLevel(), targetPos);
         if (targetBE == null) {
            return null;
         } else {
            BlockState targetState = targetBE.getBlockState();
            if (!isCrafter(targetState)) {
               return null;
            } else {
               return state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING) != targetState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)
                  ? null
                  : targetBE;
            }
         }
      }
   }

   public static List<MechanicalCrafterBlockEntity> getPrecedingCrafters(MechanicalCrafterBlockEntity crafter) {
      BlockPos pos = crafter.getBlockPos();
      Level world = crafter.getLevel();
      List<MechanicalCrafterBlockEntity> crafters = new ArrayList<>();
      BlockState blockState = crafter.getBlockState();
      if (!isCrafter(blockState)) {
         return crafters;
      } else {
         Direction blockFacing = (Direction)blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
         Direction blockPointing = MechanicalCrafterBlock.getTargetDirection(blockState);

         for (Direction facing : Iterate.directions) {
            if (blockFacing.getAxis() != facing.getAxis() && blockPointing != facing) {
               BlockPos neighbourPos = pos.relative(facing);
               BlockState neighbourState = world.getBlockState(neighbourPos);
               if (isCrafter(neighbourState)
                  && MechanicalCrafterBlock.getTargetDirection(neighbourState) == facing.getOpposite()
                  && blockFacing == neighbourState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)) {
                  MechanicalCrafterBlockEntity be = CrafterHelper.getCrafter(world, neighbourPos);
                  if (be != null) {
                     crafters.add(be);
                  }
               }
            }
         }

         return crafters;
      }
   }

   private static boolean isCrafter(BlockState state) {
      return AllBlocks.MECHANICAL_CRAFTER.has(state);
   }

   public static ItemStack tryToApplyRecipe(Level world, RecipeGridHandler.GroupedItems items) {
      items.calcStats();
      CraftingInput craftingInput = MechanicalCraftingInput.of(items);
      ItemStack result = null;
      RegistryAccess registryAccess = world.registryAccess();
      if ((Boolean)AllConfigs.server().recipes.allowRegularCraftingInCrafter.get()) {
         result = world.getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, craftingInput, world)
            .filter(r -> isRecipeAllowed((RecipeHolder<CraftingRecipe>)r, craftingInput))
            .map(r -> ((CraftingRecipe)r.value()).assemble(craftingInput, registryAccess))
            .orElse(null);
      }

      if (result == null) {
         result = AllRecipeTypes.MECHANICAL_CRAFTING.find(craftingInput, world).map(r -> r.value().assemble(craftingInput, registryAccess)).orElse(null);
      }

      return result;
   }

   public static boolean isRecipeAllowed(RecipeHolder<CraftingRecipe> recipe, CraftingInput craftingInput) {
      if (recipe.value() instanceof FireworkRocketRecipe) {
         int numItems = 0;

         for (int i = 0; i < craftingInput.size(); i++) {
            if (!craftingInput.getItem(i).isEmpty()) {
               numItems++;
            }
         }

         if (numItems > (Integer)AllConfigs.server().recipes.maxFireworkIngredientsInCrafter.get()) {
            return false;
         }
      }

      return !AllRecipeTypes.shouldIgnoreInAutomation(recipe);
   }

   public static class GroupedItems {
      Map<Pair<Integer, Integer>, ItemStack> grid = new HashMap<>();
      int minX;
      int minY;
      int maxX;
      int maxY;
      int width;
      int height;
      boolean statsReady;

      public GroupedItems() {
      }

      public GroupedItems(ItemStack stack) {
         this.grid.put(Pair.of(0, 0), stack);
      }

      public void mergeOnto(RecipeGridHandler.GroupedItems other, Pointing pointing) {
         int xOffset = pointing == Pointing.LEFT ? 1 : (pointing == Pointing.RIGHT ? -1 : 0);
         int yOffset = pointing == Pointing.DOWN ? 1 : (pointing == Pointing.UP ? -1 : 0);
         this.grid.forEach((pair, stack) -> other.grid.put(Pair.of((Integer)pair.getKey() + xOffset, (Integer)pair.getValue() + yOffset), stack));
         other.statsReady = false;
      }

      public void write(CompoundTag nbt, Provider registries) {
         ListTag gridNBT = new ListTag();
         this.grid.forEach((pair, stack) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", (Integer)pair.getKey());
            entry.putInt("y", (Integer)pair.getValue());
            entry.put("item", stack.saveOptional(registries));
            gridNBT.add(entry);
         });
         nbt.put("Grid", gridNBT);
      }

      public static RecipeGridHandler.GroupedItems read(CompoundTag nbt, Provider registries) {
         RecipeGridHandler.GroupedItems items = new RecipeGridHandler.GroupedItems();
         ListTag gridNBT = nbt.getList("Grid", 10);
         gridNBT.forEach(inbt -> {
            CompoundTag entry = (CompoundTag)inbt;
            int x = entry.getInt("x");
            int y = entry.getInt("y");
            ItemStack stack = ItemStack.parseOptional(registries, entry.getCompound("item"));
            items.grid.put(Pair.of(x, y), stack);
         });
         return items;
      }

      public void calcStats() {
         if (!this.statsReady) {
            this.statsReady = true;
            this.minX = 0;
            this.minY = 0;
            this.maxX = 0;
            this.maxY = 0;

            for (Pair<Integer, Integer> pair : this.grid.keySet()) {
               int x = (Integer)pair.getKey();
               int y = (Integer)pair.getValue();
               this.minX = Math.min(this.minX, x);
               this.minY = Math.min(this.minY, y);
               this.maxX = Math.max(this.maxX, x);
               this.maxY = Math.max(this.maxY, y);
            }

            this.width = this.maxX - this.minX + 1;
            this.height = this.maxY - this.minY + 1;
         }
      }

      public boolean onlyEmptyItems() {
         for (ItemStack stack : this.grid.values()) {
            if (!stack.isEmpty()) {
               return false;
            }
         }

         return true;
      }
   }
}
