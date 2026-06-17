package com.simibubi.create.foundation.fluid;

import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.createmod.catnip.data.Pair;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

public class FluidHelper {
   public static boolean isWater(Fluid fluid) {
      return convertToStill(fluid) == Fluids.WATER;
   }

   public static boolean isLava(Fluid fluid) {
      return convertToStill(fluid) == Fluids.LAVA;
   }

   public static boolean isSame(FluidStack fluidStack, FluidStack fluidStack2) {
      return fluidStack.getFluid() == fluidStack2.getFluid();
   }

   public static boolean isSame(FluidStack fluidStack, Fluid fluid) {
      return fluidStack.getFluid() == fluid;
   }

   public static boolean isTag(Fluid fluid, TagKey<Fluid> tag) {
      return fluid.is(tag);
   }

   public static boolean isTag(FluidState fluid, TagKey<Fluid> tag) {
      return fluid.is(tag);
   }

   public static boolean isTag(FluidStack fluid, TagKey<Fluid> tag) {
      return isTag(fluid.getFluid(), tag);
   }

   public static SoundEvent getFillSound(FluidStack fluid) {
      SoundEvent soundevent = fluid.getFluid().getFluidType().getSound(fluid, SoundActions.BUCKET_FILL);
      if (soundevent == null) {
         soundevent = isTag(fluid, FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL;
      }

      return soundevent;
   }

   public static SoundEvent getEmptySound(FluidStack fluid) {
      SoundEvent soundevent = fluid.getFluid().getFluidType().getSound(fluid, SoundActions.BUCKET_EMPTY);
      if (soundevent == null) {
         soundevent = isTag(fluid, FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
      }

      return soundevent;
   }

   public static boolean hasBlockState(Fluid fluid) {
      BlockState blockState = fluid.defaultFluidState().createLegacyBlock();
      return blockState != null && blockState != Blocks.AIR.defaultBlockState();
   }

   public static FluidStack copyStackWithAmount(FluidStack fs, int amount) {
      if (amount <= 0) {
         return FluidStack.EMPTY;
      } else if (fs.isEmpty()) {
         return FluidStack.EMPTY;
      } else {
         FluidStack copy = fs.copy();
         copy.setAmount(amount);
         return copy;
      }
   }

   public static Fluid convertToFlowing(Fluid fluid) {
      if (fluid == Fluids.WATER) {
         return Fluids.FLOWING_WATER;
      } else if (fluid == Fluids.LAVA) {
         return Fluids.FLOWING_LAVA;
      } else {
         return fluid instanceof BaseFlowingFluid ? ((BaseFlowingFluid)fluid).getFlowing() : fluid;
      }
   }

   public static Fluid convertToStill(Fluid fluid) {
      if (fluid == Fluids.FLOWING_WATER) {
         return Fluids.WATER;
      } else if (fluid == Fluids.FLOWING_LAVA) {
         return Fluids.LAVA;
      } else {
         return fluid instanceof BaseFlowingFluid ? ((BaseFlowingFluid)fluid).getSource() : fluid;
      }
   }

   public static boolean tryEmptyItemIntoBE(Level worldIn, Player player, InteractionHand handIn, ItemStack heldItem, SmartBlockEntity be) {
      if (!GenericItemEmptying.canItemBeEmptied(worldIn, heldItem)) {
         return false;
      } else {
         Pair<FluidStack, ItemStack> emptyingResult = GenericItemEmptying.emptyItem(worldIn, heldItem, true);
         IFluidHandler capability = (IFluidHandler)worldIn.getCapability(FluidHandler.BLOCK, be.getBlockPos(), null);
         FluidStack fluidStack = (FluidStack)emptyingResult.getFirst();
         if (capability == null || fluidStack.getAmount() != capability.fill(fluidStack, FluidAction.SIMULATE)) {
            return false;
         } else if (worldIn.isClientSide) {
            return true;
         } else {
            ItemStack copyOfHeld = heldItem.copy();
            emptyingResult = GenericItemEmptying.emptyItem(worldIn, copyOfHeld, false);
            capability.fill(fluidStack, FluidAction.EXECUTE);
            if (!player.isCreative() && !(be instanceof CreativeFluidTankBlockEntity)) {
               if (copyOfHeld.isEmpty()) {
                  player.setItemInHand(handIn, (ItemStack)emptyingResult.getSecond());
               } else {
                  player.setItemInHand(handIn, copyOfHeld);
                  player.getInventory().placeItemBackInInventory((ItemStack)emptyingResult.getSecond());
               }
            }

            return true;
         }
      }
   }

   public static boolean tryFillItemFromBE(Level world, Player player, InteractionHand handIn, ItemStack heldItem, SmartBlockEntity be) {
      if (!GenericItemFilling.canItemBeFilled(world, heldItem)) {
         return false;
      } else {
         IFluidHandler capability = (IFluidHandler)world.getCapability(FluidHandler.BLOCK, be.getBlockPos(), null);
         if (capability == null) {
            return false;
         } else {
            for (int i = 0; i < capability.getTanks(); i++) {
               FluidStack fluid = capability.getFluidInTank(i);
               if (!fluid.isEmpty()) {
                  int requiredAmountForItem = GenericItemFilling.getRequiredAmountForItem(world, heldItem, fluid.copy());
                  if (requiredAmountForItem != -1 && requiredAmountForItem <= fluid.getAmount()) {
                     if (world.isClientSide) {
                        return true;
                     }

                     if (player.isCreative() || be instanceof CreativeFluidTankBlockEntity) {
                        heldItem = heldItem.copy();
                     }

                     ItemStack out = GenericItemFilling.fillItem(world, requiredAmountForItem, heldItem, fluid.copy());
                     FluidStack copy = fluid.copy();
                     copy.setAmount(requiredAmountForItem);
                     capability.drain(copy, FluidAction.EXECUTE);
                     if (!player.isCreative()) {
                        player.getInventory().placeItemBackInInventory(out);
                     }

                     be.notifyUpdate();
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   @Nullable
   public static FluidHelper.FluidExchange exchange(IFluidHandler fluidTank, IFluidHandlerItem fluidItem, FluidHelper.FluidExchange preferred, int maxAmount) {
      return exchange(fluidTank, fluidItem, preferred, true, maxAmount);
   }

   @Nullable
   public static FluidHelper.FluidExchange exchangeAll(IFluidHandler fluidTank, IFluidHandlerItem fluidItem, FluidHelper.FluidExchange preferred) {
      return exchange(fluidTank, fluidItem, preferred, false, Integer.MAX_VALUE);
   }

   @Nullable
   private static FluidHelper.FluidExchange exchange(
      IFluidHandler fluidTank, IFluidHandlerItem fluidItem, FluidHelper.FluidExchange preferred, boolean singleOp, int maxTransferAmountPerTank
   ) {
      FluidHelper.FluidExchange lockedExchange = null;

      for (int tankSlot = 0; tankSlot < fluidTank.getTanks(); tankSlot++) {
         for (int slot = 0; slot < fluidItem.getTanks(); slot++) {
            FluidStack fluidInTank = fluidTank.getFluidInTank(tankSlot);
            int tankCapacity = fluidTank.getTankCapacity(tankSlot) - fluidInTank.getAmount();
            boolean tankEmpty = fluidInTank.isEmpty();
            FluidStack fluidInItem = fluidItem.getFluidInTank(tankSlot);
            int itemCapacity = fluidItem.getTankCapacity(tankSlot) - fluidInItem.getAmount();
            boolean itemEmpty = fluidInItem.isEmpty();
            boolean undecided = lockedExchange == null;
            boolean canMoveToTank = (undecided || lockedExchange == FluidHelper.FluidExchange.ITEM_TO_TANK) && tankCapacity > 0;
            boolean canMoveToItem = (undecided || lockedExchange == FluidHelper.FluidExchange.TANK_TO_ITEM) && itemCapacity > 0;
            if (tankEmpty || itemEmpty || FluidStack.isSameFluidSameComponents(fluidInItem, fluidInTank)) {
               if ((tankEmpty || itemCapacity <= 0) && canMoveToTank || undecided && preferred == FluidHelper.FluidExchange.ITEM_TO_TANK) {
                  int amount = fluidTank.fill(fluidItem.drain(Math.min(maxTransferAmountPerTank, tankCapacity), FluidAction.EXECUTE), FluidAction.EXECUTE);
                  if (amount > 0) {
                     lockedExchange = FluidHelper.FluidExchange.ITEM_TO_TANK;
                     if (singleOp) {
                        return lockedExchange;
                     }
                     continue;
                  }
               }

               if ((itemEmpty || tankCapacity <= 0) && canMoveToItem || undecided && preferred == FluidHelper.FluidExchange.TANK_TO_ITEM) {
                  int amount = fluidItem.fill(fluidTank.drain(Math.min(maxTransferAmountPerTank, itemCapacity), FluidAction.EXECUTE), FluidAction.EXECUTE);
                  if (amount > 0) {
                     lockedExchange = FluidHelper.FluidExchange.TANK_TO_ITEM;
                     if (singleOp) {
                        return lockedExchange;
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   public static enum FluidExchange {
      ITEM_TO_TANK,
      TANK_TO_ITEM;
   }
}
