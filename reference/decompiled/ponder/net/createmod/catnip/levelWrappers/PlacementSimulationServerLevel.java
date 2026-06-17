package net.createmod.catnip.levelWrappers;

import java.util.HashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class PlacementSimulationServerLevel extends WrappedServerLevel {
   public HashMap<BlockPos, BlockState> blocksAdded = new HashMap<>();

   public PlacementSimulationServerLevel(ServerLevel wrapped) {
      super(wrapped);
   }

   public void clear() {
      this.blocksAdded.clear();
   }

   public boolean setBlock(BlockPos pos, BlockState newState, int flags) {
      this.blocksAdded.put(pos.immutable(), newState);
      return true;
   }

   public boolean setBlockAndUpdate(BlockPos pos, BlockState state) {
      return this.setBlock(pos, state, 0);
   }

   public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> condition) {
      return condition.test(this.getBlockState(pos));
   }

   public boolean isLoaded(BlockPos pos) {
      return true;
   }

   public boolean isAreaLoaded(BlockPos center, int range) {
      return true;
   }

   public BlockState getBlockState(BlockPos pos) {
      return this.blocksAdded.containsKey(pos) ? this.blocksAdded.get(pos) : Blocks.AIR.defaultBlockState();
   }

   public FluidState getFluidState(BlockPos pos) {
      return this.getBlockState(pos).getFluidState();
   }
}
