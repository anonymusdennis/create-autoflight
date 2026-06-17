package com.simibubi.create.foundation.blockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import org.jetbrains.annotations.Nullable;

public interface IMultiBlockEntityContainer {
   BlockPos getController();

   <T extends BlockEntity & IMultiBlockEntityContainer> T getControllerBE();

   boolean isController();

   void setController(BlockPos var1);

   void removeController(boolean var1);

   BlockPos getLastKnownPos();

   void preventConnectivityUpdate();

   void notifyMultiUpdated();

   default void setExtraData(@Nullable Object data) {
   }

   @Nullable
   default Object getExtraData() {
      return null;
   }

   default Object modifyExtraData(Object data) {
      return data;
   }

   Axis getMainConnectionAxis();

   default Axis getMainAxisOf(BlockEntity be) {
      BlockState state = be.getBlockState();
      Axis axis;
      if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
         axis = (Axis)state.getValue(BlockStateProperties.HORIZONTAL_AXIS);
      } else if (state.hasProperty(BlockStateProperties.FACING)) {
         axis = ((Direction)state.getValue(BlockStateProperties.FACING)).getAxis();
      } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
         axis = ((Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING)).getAxis();
      } else {
         axis = Axis.Y;
      }

      return axis;
   }

   int getMaxLength(Axis var1, int var2);

   int getMaxWidth();

   int getHeight();

   void setHeight(int var1);

   int getWidth();

   void setWidth(int var1);

   public interface Fluid extends IMultiBlockEntityContainer {
      default boolean hasTank() {
         return false;
      }

      default int getTankSize(int tank) {
         return 0;
      }

      default void setTankSize(int tank, int blocks) {
      }

      default IFluidTank getTank(int tank) {
         return null;
      }

      default FluidStack getFluid(int tank) {
         return FluidStack.EMPTY;
      }
   }

   public interface Inventory extends IMultiBlockEntityContainer {
      default boolean hasInventory() {
         return false;
      }
   }
}
