package com.simibubi.create.content.kinetics.gearbox;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.phys.HitResult;

public class GearboxBlock extends RotatedPillarKineticBlock implements IBE<GearboxBlockEntity> {
   public GearboxBlock(Properties properties) {
      super(properties);
   }

   public PushReaction getPistonPushReaction(BlockState state) {
      return PushReaction.PUSH_ONLY;
   }

   public List<ItemStack> getDrops(BlockState state, Builder builder) {
      return ((Axis)state.getValue(AXIS)).isVertical()
         ? super.getDrops(state, builder)
         : Arrays.asList(new ItemStack((ItemLike)AllItems.VERTICAL_GEARBOX.get()));
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      return ((Axis)state.getValue(AXIS)).isVertical()
         ? super.getCloneItemStack(state, target, level, pos, player)
         : new ItemStack((ItemLike)AllItems.VERTICAL_GEARBOX.get());
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(AXIS, Axis.Y);
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() != state.getValue(AXIS);
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return (Axis)state.getValue(AXIS);
   }

   @Override
   public Class<GearboxBlockEntity> getBlockEntityClass() {
      return GearboxBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends GearboxBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends GearboxBlockEntity>)AllBlockEntityTypes.GEARBOX.get();
   }
}
