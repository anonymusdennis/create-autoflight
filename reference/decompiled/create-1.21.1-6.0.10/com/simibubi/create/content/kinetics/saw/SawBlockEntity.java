package com.simibubi.create.content.kinetics.saw;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.recipe.RecipeConditions;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import com.simibubi.create.foundation.utility.AbstractBlockBreakQueue;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SawBlockEntity extends BlockBreakingKineticBlockEntity implements Clearable {
   private static final Object cuttingRecipesKey = new Object();
   public static final Supplier<RecipeType<?>> woodcuttingRecipeType = Suppliers.memoize(
      () -> (RecipeType)BuiltInRegistries.RECIPE_TYPE.get(ResourceLocation.fromNamespaceAndPath("druidcraft", "woodcutting"))
   );
   public ProcessingInventory inventory = new ProcessingInventory(this::start).withSlotLimit(!(Boolean)AllConfigs.server().recipes.bulkCutting.get());
   private int recipeIndex;
   private FilteringBehaviour filtering;
   private ItemStack playEvent;

   public SawBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.inventory.remainingTime = -1.0F;
      this.recipeIndex = 0;
      this.playEvent = ItemStack.EMPTY;
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(
         ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.SAW.get(), (be, context) -> context != Direction.DOWN ? be.inventory : null
      );
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.filtering = new FilteringBehaviour(this, new SawFilterSlot()).forRecipes();
      behaviours.add(this.filtering);
      behaviours.add(new DirectBeltInputBehaviour(this).allowingBeltFunnelsWhen(this::canProcess));
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.SAW_PROCESSING});
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.put("Inventory", this.inventory.serializeNBT(registries));
      compound.putInt("RecipeIndex", this.recipeIndex);
      super.write(compound, registries, clientPacket);
      if (clientPacket && !this.playEvent.isEmpty()) {
         compound.put("PlayEvent", this.playEvent.saveOptional(registries));
         this.playEvent = ItemStack.EMPTY;
      }
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
      this.recipeIndex = compound.getInt("RecipeIndex");
      if (compound.contains("PlayEvent")) {
         this.playEvent = ItemStack.parseOptional(registries, compound.getCompound("PlayEvent"));
      }
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return new AABB(this.getBlockPos()).inflate(0.125);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void tickAudio() {
      super.tickAudio();
      if (this.getSpeed() != 0.0F) {
         if (!this.playEvent.isEmpty()) {
            boolean isWood = false;
            Item item = this.playEvent.getItem();
            if (item instanceof BlockItem) {
               Block block = ((BlockItem)item).getBlock();
               isWood = block.getSoundType(block.defaultBlockState(), this.level, this.worldPosition, null) == SoundType.WOOD;
            }

            this.spawnEventParticles(this.playEvent);
            this.playEvent = ItemStack.EMPTY;
            if (!isWood) {
               AllSoundEvents.SAW_ACTIVATE_STONE.playAt(this.level, this.worldPosition, 3.0F, 1.0F, true);
            } else {
               AllSoundEvents.SAW_ACTIVATE_WOOD.playAt(this.level, this.worldPosition, 3.0F, 1.0F, true);
            }
         }
      }
   }

   @Override
   public void tick() {
      if (this.shouldRun() && this.ticksUntilNextProgress < 0) {
         this.destroyNextTick();
      }

      super.tick();
      if (this.canProcess()) {
         if (this.getSpeed() != 0.0F) {
            if (this.inventory.remainingTime == -1.0F) {
               if (!this.inventory.isEmpty() && !this.inventory.appliedRecipe) {
                  this.start(this.inventory.getStackInSlot(0));
               }
            } else {
               float processingSpeed = Mth.clamp(Math.abs(this.getSpeed()) / 24.0F, 1.0F, 128.0F);
               this.inventory.remainingTime -= processingSpeed;
               if (this.inventory.remainingTime > 0.0F) {
                  this.spawnParticles(this.inventory.getStackInSlot(0));
               }

               if (!(this.inventory.remainingTime < 5.0F) || this.inventory.appliedRecipe) {
                  Vec3 itemMovement = this.getItemMovementVec();
                  Direction itemMovementFacing = Direction.getNearest(itemMovement.x, itemMovement.y, itemMovement.z);
                  if (!(this.inventory.remainingTime > 0.0F)) {
                     this.inventory.remainingTime = 0.0F;

                     for (int slot = 0; slot < this.inventory.getSlots(); slot++) {
                        ItemStack stack = this.inventory.getStackInSlot(slot);
                        if (!stack.isEmpty()) {
                           ItemStack tryExportingToBeltFunnel = this.getBehaviour(DirectBeltInputBehaviour.TYPE)
                              .tryExportingToBeltFunnel(stack, itemMovementFacing.getOpposite(), false);
                           if (tryExportingToBeltFunnel != null) {
                              if (tryExportingToBeltFunnel.getCount() != stack.getCount()) {
                                 this.inventory.setStackInSlot(slot, tryExportingToBeltFunnel);
                                 this.notifyUpdate();
                                 return;
                              }

                              if (!tryExportingToBeltFunnel.isEmpty()) {
                                 return;
                              }
                           }
                        }
                     }

                     BlockPos nextPos = this.worldPosition.offset(BlockPos.containing(itemMovement));
                     DirectBeltInputBehaviour behaviour = BlockEntityBehaviour.get(this.level, nextPos, DirectBeltInputBehaviour.TYPE);
                     if (behaviour != null) {
                        boolean changed = false;
                        if (behaviour.canInsertFromSide(itemMovementFacing)) {
                           if (!this.level.isClientSide || this.isVirtual()) {
                              for (int slotx = 0; slotx < this.inventory.getSlots(); slotx++) {
                                 ItemStack stack = this.inventory.getStackInSlot(slotx);
                                 if (!stack.isEmpty()) {
                                    ItemStack remainder = behaviour.handleInsertion(stack, itemMovementFacing, false);
                                    if (!ItemStack.matches(remainder, stack)) {
                                       this.inventory.setStackInSlot(slotx, remainder);
                                       changed = true;
                                    }
                                 }
                              }

                              if (changed) {
                                 this.setChanged();
                                 this.sendData();
                              }
                           }
                        }
                     } else {
                        Vec3 outPos = VecHelper.getCenterOf(this.worldPosition).add(itemMovement.scale(0.5).add(0.0, 0.5, 0.0));
                        Vec3 outMotion = itemMovement.scale(0.0625).add(0.0, 0.125, 0.0);

                        for (int slotxx = 0; slotxx < this.inventory.getSlots(); slotxx++) {
                           ItemStack stack = this.inventory.getStackInSlot(slotxx);
                           if (!stack.isEmpty()) {
                              ItemEntity entityIn = new ItemEntity(this.level, outPos.x, outPos.y, outPos.z, stack);
                              entityIn.setDeltaMovement(outMotion);
                              this.level.addFreshEntity(entityIn);
                           }
                        }

                        this.inventory.clear();
                        this.level.updateNeighbourForOutputSignal(this.worldPosition, this.getBlockState().getBlock());
                        this.inventory.remainingTime = -1.0F;
                        this.sendData();
                     }
                  }
               } else if (!this.level.isClientSide || this.isVirtual()) {
                  this.playEvent = this.inventory.getStackInSlot(0);
                  this.applyRecipe();
                  this.inventory.appliedRecipe = true;
                  this.inventory.recipeDuration = 20.0F;
                  this.inventory.remainingTime = 20.0F;
                  this.sendData();
               }
            }
         }
      }
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.invalidateCapabilities();
   }

   public void clearContent() {
      this.inventory.clear();
      this.filtering.setFilter(ItemStack.EMPTY);
   }

   @Override
   public void destroy() {
      super.destroy();
      ItemHelper.dropContents(this.level, this.worldPosition, this.inventory);
   }

   protected void spawnEventParticles(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         ParticleOptions particleData = null;
         if (stack.getItem() instanceof BlockItem) {
            particleData = new BlockParticleOption(ParticleTypes.BLOCK, ((BlockItem)stack.getItem()).getBlock().defaultBlockState());
         } else {
            particleData = new ItemParticleOption(ParticleTypes.ITEM, stack);
         }

         RandomSource r = this.level.random;
         Vec3 v = VecHelper.getCenterOf(this.worldPosition).add(0.0, 0.3125, 0.0);

         for (int i = 0; i < 10; i++) {
            Vec3 m = VecHelper.offsetRandomly(new Vec3(0.0, 0.25, 0.0), r, 0.125F);
            this.level.addParticle(particleData, v.x, v.y, v.z, m.x, m.y, m.y);
         }
      }
   }

   protected void spawnParticles(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         ParticleOptions particleData = null;
         float speed = 1.0F;
         if (stack.getItem() instanceof BlockItem) {
            particleData = new BlockParticleOption(ParticleTypes.BLOCK, ((BlockItem)stack.getItem()).getBlock().defaultBlockState());
         } else {
            particleData = new ItemParticleOption(ParticleTypes.ITEM, stack);
            speed = 0.125F;
         }

         RandomSource r = this.level.random;
         Vec3 vec = this.getItemMovementVec();
         Vec3 pos = VecHelper.getCenterOf(this.worldPosition);
         float offset = this.inventory.recipeDuration != 0.0F ? this.inventory.remainingTime / this.inventory.recipeDuration : 0.0F;
         offset /= 2.0F;
         if (this.inventory.appliedRecipe) {
            offset -= 0.5F;
         }

         this.level
            .addParticle(
               particleData,
               pos.x() + -vec.x * (double)offset,
               pos.y() + 0.45F,
               pos.z() + -vec.z * (double)offset,
               -vec.x * (double)speed,
               (double)(r.nextFloat() * speed),
               -vec.z * (double)speed
            );
      }
   }

   public Vec3 getItemMovementVec() {
      boolean alongX = !(Boolean)this.getBlockState().getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE);
      int offset = this.getSpeed() < 0.0F ? -1 : 1;
      return new Vec3((double)(offset * (alongX ? 1 : 0)), 0.0, (double)(offset * (alongX ? 0 : -1)));
   }

   private void applyRecipe() {
      ItemStack input = this.inventory.getStackInSlot(0);
      List<ItemStack> list = new ArrayList<>();
      if (PackageItem.isPackage(input)) {
         this.inventory.clear();
         ItemStackHandler results = PackageItem.getContents(input);

         for (int i = 0; i < results.getSlots(); i++) {
            ItemStack stack = results.getStackInSlot(i);
            if (!stack.isEmpty()) {
               ItemHelper.addToList(stack, list);
            }
         }

         for (int slot = 0; slot < list.size() && slot + 1 < this.inventory.getSlots(); slot++) {
            this.inventory.setStackInSlot(slot + 1, list.get(slot));
         }
      } else {
         List<RecipeHolder<? extends Recipe<?>>> recipes = this.getRecipes();
         if (!recipes.isEmpty()) {
            if (this.recipeIndex >= recipes.size()) {
               this.recipeIndex = 0;
            }

            Recipe<?> recipe = recipes.get(this.recipeIndex).value();
            int rolls = input.getCount();
            this.inventory.clear();

            for (int roll = 0; roll < rolls; roll++) {
               List<ItemStack> results = new LinkedList<>();
               if (recipe instanceof CuttingRecipe) {
                  results = ((CuttingRecipe)recipe).rollResults(this.level.random);
               } else if (recipe instanceof StonecutterRecipe || recipe.getType() == woodcuttingRecipeType.get()) {
                  results.add(recipe.getResultItem(this.level.registryAccess()).copy());
               }

               for (ItemStack stack : results) {
                  ItemHelper.addToList(stack, list);
               }

               if (input.hasCraftingRemainingItem()) {
                  ItemHelper.addToList(input.getCraftingRemainingItem(), list);
               }
            }

            for (int slot = 0; slot < list.size() && slot + 1 < this.inventory.getSlots(); slot++) {
               this.inventory.setStackInSlot(slot + 1, list.get(slot));
            }

            this.award(AllAdvancements.SAW_PROCESSING);
         }
      }
   }

   private List<RecipeHolder<? extends Recipe<?>>> getRecipes() {
      Optional<RecipeHolder<CuttingRecipe>> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(
         this.level, this.inventory.getStackInSlot(0), AllRecipeTypes.CUTTING.getType(), CuttingRecipe.class
      );
      if (assemblyRecipe.isPresent() && this.filtering.test(((CuttingRecipe)assemblyRecipe.get().value()).getResultItem(this.level.registryAccess()))) {
         return ImmutableList.of(assemblyRecipe.get());
      } else {
         Predicate<RecipeHolder<? extends Recipe<?>>> types = RecipeConditions.isOfType(
            AllRecipeTypes.CUTTING.getType(), AllConfigs.server().recipes.allowStonecuttingOnSaw.get() ? RecipeType.STONECUTTING : null
         );
         List<RecipeHolder<? extends Recipe<?>>> startedSearch = RecipeFinder.get(cuttingRecipesKey, this.level, types);
         return startedSearch.stream()
            .filter(RecipeConditions.outputMatchesFilter(this.filtering))
            .filter(RecipeConditions.firstIngredientMatches(this.inventory.getStackInSlot(0)))
            .filter(r -> !AllRecipeTypes.shouldIgnoreInAutomation((RecipeHolder<?>)r))
            .collect(Collectors.toList());
      }
   }

   public void insertItem(ItemEntity entity) {
      if (this.canProcess()) {
         if (this.inventory.isEmpty()) {
            if (entity.isAlive()) {
               if (!this.level.isClientSide) {
                  this.inventory.clear();
                  ItemStack remainder = this.inventory.insertItem(0, entity.getItem().copy(), false);
                  if (remainder.isEmpty()) {
                     entity.discard();
                  } else {
                     entity.setItem(remainder);
                  }
               }
            }
         }
      }
   }

   public void start(ItemStack inserted) {
      if (this.canProcess()) {
         if (!this.inventory.isEmpty()) {
            if (!this.level.isClientSide || this.isVirtual()) {
               List<RecipeHolder<? extends Recipe<?>>> recipes = this.getRecipes();
               boolean valid = !recipes.isEmpty();
               int time = 50;
               if (recipes.isEmpty()) {
                  this.inventory.remainingTime = this.inventory.recipeDuration = 10.0F;
                  this.inventory.appliedRecipe = false;
                  this.sendData();
               } else {
                  if (valid) {
                     this.recipeIndex++;
                     if (this.recipeIndex >= recipes.size()) {
                        this.recipeIndex = 0;
                     }
                  }

                  Recipe<?> recipe = recipes.get(this.recipeIndex).value();
                  if (recipe instanceof CuttingRecipe) {
                     time = ((CuttingRecipe)recipe).getProcessingDuration();
                  }

                  this.inventory.remainingTime = (float)(time * Math.max(1, inserted.getCount() / 5));
                  this.inventory.recipeDuration = this.inventory.remainingTime;
                  this.inventory.appliedRecipe = false;
                  this.sendData();
               }
            }
         }
      }
   }

   protected boolean canProcess() {
      return this.getBlockState().getValue(SawBlock.FACING) == Direction.UP;
   }

   @Override
   protected boolean shouldRun() {
      return ((Direction)this.getBlockState().getValue(SawBlock.FACING)).getAxis().isHorizontal();
   }

   @Override
   protected BlockPos getBreakingPos() {
      return this.getBlockPos().relative((Direction)this.getBlockState().getValue(SawBlock.FACING));
   }

   @Override
   public void onBlockBroken(BlockState stateToBreak) {
      Optional<AbstractBlockBreakQueue> dynamicTree = TreeCutter.findDynamicTree(stateToBreak.getBlock(), this.breakingPos);
      if (dynamicTree.isPresent()) {
         dynamicTree.get().destroyBlocks(this.level, null, this::dropItemFromCutTree);
      } else {
         super.onBlockBroken(stateToBreak);
         TreeCutter.findTree(this.level, this.breakingPos, stateToBreak).destroyBlocks(this.level, null, this::dropItemFromCutTree);
      }
   }

   public void dropItemFromCutTree(BlockPos pos, ItemStack stack) {
      float distance = (float)Math.sqrt(pos.distSqr(this.breakingPos));
      Vec3 dropPos = VecHelper.getCenterOf(pos);
      ItemEntity entity = new ItemEntity(this.level, dropPos.x, dropPos.y, dropPos.z, stack);
      entity.setDeltaMovement(Vec3.atLowerCornerOf(this.breakingPos.subtract(this.worldPosition)).scale((double)(distance / 20.0F)));
      this.level.addFreshEntity(entity);
   }

   @Override
   public boolean canBreak(BlockState stateToBreak, float blockHardness) {
      boolean sawable = isSawable(stateToBreak);
      return super.canBreak(stateToBreak, blockHardness) && sawable;
   }

   public static boolean isSawable(BlockState stateToBreak) {
      if (stateToBreak.is(BlockTags.SAPLINGS)) {
         return false;
      } else if (TreeCutter.isLog(stateToBreak) || stateToBreak.is(BlockTags.LEAVES)) {
         return true;
      } else if (TreeCutter.isRoot(stateToBreak)) {
         return true;
      } else {
         Block block = stateToBreak.getBlock();
         if (block instanceof BambooStalkBlock) {
            return true;
         } else if (block.equals(Blocks.PUMPKIN) || block.equals(Blocks.MELON)) {
            return true;
         } else if (block instanceof CactusBlock) {
            return true;
         } else if (block instanceof SugarCaneBlock) {
            return true;
         } else if (block instanceof KelpPlantBlock) {
            return true;
         } else if (block instanceof KelpBlock) {
            return true;
         } else {
            return block instanceof ChorusPlantBlock ? true : TreeCutter.canDynamicTreeCutFrom(block);
         }
      }
   }
}
