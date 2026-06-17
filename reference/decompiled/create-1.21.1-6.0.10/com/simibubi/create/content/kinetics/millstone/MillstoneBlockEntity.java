package com.simibubi.create.content.kinetics.millstone;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.mixin.accessor.ItemStackHandlerAccessor;
import com.simibubi.create.foundation.sound.SoundScapes;
import java.util.List;
import java.util.Optional;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class MillstoneBlockEntity extends KineticBlockEntity implements Clearable {
   public ItemStackHandler inputInv = new ItemStackHandler(1);
   public ItemStackHandler outputInv = new ItemStackHandler(9);
   public IItemHandler capability = new MillstoneBlockEntity.MillstoneInventoryHandler();
   public int timer;
   private MillingRecipe lastRecipe;

   public MillstoneBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.MILLSTONE.get(), (be, context) -> be.capability);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(new DirectBeltInputBehaviour(this));
      super.addBehaviours(behaviours);
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.MILLSTONE});
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void tickAudio() {
      super.tickAudio();
      if (this.getSpeed() != 0.0F) {
         if (!this.inputInv.getStackInSlot(0).isEmpty()) {
            float pitch = Mth.clamp(Math.abs(this.getSpeed()) / 256.0F + 0.45F, 0.85F, 1.0F);
            SoundScapes.play(SoundScapes.AmbienceGroup.MILLING, this.worldPosition, pitch);
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.getSpeed() != 0.0F) {
         for (int i = 0; i < this.outputInv.getSlots(); i++) {
            if (this.outputInv.getStackInSlot(i).getCount() == this.outputInv.getSlotLimit(i)) {
               return;
            }
         }

         if (this.timer > 0) {
            this.timer = this.timer - this.getProcessingSpeed();
            if (this.level.isClientSide) {
               this.spawnParticles();
            } else {
               if (this.timer <= 0) {
                  this.process();
               }
            }
         } else if (!this.inputInv.getStackInSlot(0).isEmpty()) {
            RecipeWrapper inventoryIn = new RecipeWrapper(this.inputInv);
            if (this.lastRecipe != null && this.lastRecipe.matches(inventoryIn, this.level)) {
               this.timer = this.lastRecipe.getProcessingDuration();
               this.sendData();
            } else {
               Optional<RecipeHolder<MillingRecipe>> recipe = AllRecipeTypes.MILLING.find(inventoryIn, this.level);
               if (!recipe.isPresent()) {
                  this.timer = 100;
                  this.sendData();
               } else {
                  this.lastRecipe = (MillingRecipe)recipe.get().value();
                  this.timer = this.lastRecipe.getProcessingDuration();
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
      ((ItemStackHandlerAccessor)this.inputInv).create$getStacks().clear();
   }

   @Override
   public void destroy() {
      super.destroy();
      ItemHelper.dropContents(this.level, this.worldPosition, this.inputInv);
      ItemHelper.dropContents(this.level, this.worldPosition, this.outputInv);
   }

   private void process() {
      RecipeWrapper inventoryIn = new RecipeWrapper(this.inputInv);
      if (this.lastRecipe == null || !this.lastRecipe.matches(inventoryIn, this.level)) {
         Optional<RecipeHolder<MillingRecipe>> recipe = AllRecipeTypes.MILLING.find(inventoryIn, this.level);
         if (recipe.isEmpty()) {
            return;
         }

         this.lastRecipe = (MillingRecipe)recipe.get().value();
      }

      ItemStack stackInSlot = this.inputInv.getStackInSlot(0);
      ItemStack craftingRemainingItem = stackInSlot.getCraftingRemainingItem();
      stackInSlot.shrink(1);
      this.inputInv.setStackInSlot(0, stackInSlot);
      this.lastRecipe.rollResults(this.level.random).forEach(stack -> ItemHandlerHelper.insertItemStacked(this.outputInv, stack, false));
      if (!craftingRemainingItem.isEmpty()) {
         ItemHandlerHelper.insertItemStacked(this.outputInv, craftingRemainingItem, false);
      }

      this.award(AllAdvancements.MILLSTONE);
      this.sendData();
      this.setChanged();
   }

   public void spawnParticles() {
      ItemStack stackInSlot = this.inputInv.getStackInSlot(0);
      if (!stackInSlot.isEmpty()) {
         ItemParticleOption data = new ItemParticleOption(ParticleTypes.ITEM, stackInSlot);
         float angle = this.level.random.nextFloat() * 360.0F;
         Vec3 offset = new Vec3(0.0, 0.0, 0.5);
         offset = VecHelper.rotate(offset, (double)angle, Axis.Y);
         Vec3 target = VecHelper.rotate(offset, this.getSpeed() > 0.0F ? 25.0 : -25.0, Axis.Y);
         Vec3 center = offset.add(VecHelper.getCenterOf(this.worldPosition));
         target = VecHelper.offsetRandomly(target.subtract(offset), this.level.random, 0.0078125F);
         this.level.addParticle(data, center.x, center.y, center.z, target.x, target.y, target.z);
      }
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putInt("Timer", this.timer);
      compound.put("InputInventory", this.inputInv.serializeNBT(registries));
      compound.put("OutputInventory", this.outputInv.serializeNBT(registries));
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.timer = compound.getInt("Timer");
      this.inputInv.deserializeNBT(registries, compound.getCompound("InputInventory"));
      this.outputInv.deserializeNBT(registries, compound.getCompound("OutputInventory"));
      super.read(compound, registries, clientPacket);
   }

   public int getProcessingSpeed() {
      return Mth.clamp((int)Math.abs(this.getSpeed() / 16.0F), 1, 512);
   }

   private boolean canProcess(ItemStack stack) {
      ItemStackHandler tester = new ItemStackHandler(1);
      tester.setStackInSlot(0, stack);
      RecipeWrapper inventoryIn = new RecipeWrapper(tester);
      return this.lastRecipe != null && this.lastRecipe.matches(inventoryIn, this.level)
         ? true
         : AllRecipeTypes.MILLING.find(inventoryIn, this.level).isPresent();
   }

   private class MillstoneInventoryHandler extends CombinedInvWrapper {
      public MillstoneInventoryHandler() {
         super(new IItemHandlerModifiable[]{MillstoneBlockEntity.this.inputInv, MillstoneBlockEntity.this.outputInv});
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return MillstoneBlockEntity.this.outputInv == this.getHandlerFromIndex(this.getIndexForSlot(slot))
            ? false
            : MillstoneBlockEntity.this.canProcess(stack) && super.isItemValid(slot, stack);
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         if (MillstoneBlockEntity.this.outputInv == this.getHandlerFromIndex(this.getIndexForSlot(slot))) {
            return stack;
         } else {
            return !this.isItemValid(slot, stack) ? stack : super.insertItem(slot, stack, simulate);
         }
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return MillstoneBlockEntity.this.inputInv == this.getHandlerFromIndex(this.getIndexForSlot(slot))
            ? ItemStack.EMPTY
            : super.extractItem(slot, amount, simulate);
      }
   }
}
