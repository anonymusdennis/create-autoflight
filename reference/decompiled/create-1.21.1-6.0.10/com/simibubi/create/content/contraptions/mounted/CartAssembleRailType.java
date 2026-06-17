package com.simibubi.create.content.contraptions.mounted;

import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;
import java.util.function.Supplier;
import net.createmod.catnip.lang.Lang;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public enum CartAssembleRailType implements StringRepresentable {
   REGULAR(Blocks.RAIL),
   POWERED_RAIL(Blocks.POWERED_RAIL),
   DETECTOR_RAIL(Blocks.DETECTOR_RAIL),
   ACTIVATOR_RAIL(Blocks.ACTIVATOR_RAIL),
   CONTROLLER_RAIL(AllBlocks.CONTROLLER_RAIL);

   private final Supplier<Block> railBlockSupplier;
   private final Supplier<Item> railItemSupplier;

   private CartAssembleRailType(Block block) {
      this.railBlockSupplier = () -> block;
      this.railItemSupplier = block::asItem;
   }

   private CartAssembleRailType(BlockEntry<?> block) {
      this.railBlockSupplier = block::get;
      this.railItemSupplier = () -> ((Block)block.get()).asItem();
   }

   public Block getBlock() {
      return this.railBlockSupplier.get();
   }

   public Item getItem() {
      return this.railItemSupplier.get();
   }

   public boolean matches(BlockState rail) {
      return rail.getBlock() == this.railBlockSupplier.get();
   }

   public String getSerializedName() {
      return Lang.asId(this.name());
   }
}
