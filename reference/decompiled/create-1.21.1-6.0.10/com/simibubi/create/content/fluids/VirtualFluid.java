package com.simibubi.create.content.fluids;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties;

public class VirtualFluid extends BaseFlowingFluid {
   private final boolean source;

   public static VirtualFluid createSource(Properties properties) {
      return new VirtualFluid(properties, true);
   }

   public static VirtualFluid createFlowing(Properties properties) {
      return new VirtualFluid(properties, false);
   }

   public VirtualFluid(Properties properties, boolean source) {
      super(properties);
      this.source = source;
   }

   public Fluid getSource() {
      return (Fluid)(this.source ? this : super.getSource());
   }

   public Fluid getFlowing() {
      return (Fluid)(this.source ? super.getFlowing() : this);
   }

   public Item getBucket() {
      return Items.AIR;
   }

   protected BlockState createLegacyBlock(FluidState state) {
      return Blocks.AIR.defaultBlockState();
   }

   public boolean isSource(FluidState p_207193_1_) {
      return this.source;
   }

   public int getAmount(FluidState p_207192_1_) {
      return 0;
   }
}
