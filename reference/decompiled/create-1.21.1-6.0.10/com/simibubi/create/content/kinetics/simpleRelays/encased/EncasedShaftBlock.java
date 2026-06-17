package com.simibubi.create.content.kinetics.simpleRelays.encased;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class EncasedShaftBlock extends AbstractEncasedShaftBlock implements IBE<KineticBlockEntity>, SpecialBlockItemRequirement, EncasedBlock {
   private final Supplier<Block> casing;

   public EncasedShaftBlock(Properties properties, Supplier<Block> casing) {
      super(properties);
      this.casing = casing;
   }

   @Override
   public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
      if (context.getLevel().isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         context.getLevel().levelEvent(2001, context.getClickedPos(), Block.getId(state));
         KineticBlockEntity.switchToBlockState(
            context.getLevel(), context.getClickedPos(), (BlockState)AllBlocks.SHAFT.getDefaultState().setValue(AXIS, (Axis)state.getValue(AXIS))
         );
         return InteractionResult.SUCCESS;
      }
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      if (target instanceof BlockHitResult) {
         return ((BlockHitResult)target).getDirection().getAxis() == this.getRotationAxis(state)
            ? AllBlocks.SHAFT.asStack()
            : this.getCasing().asItem().getDefaultInstance();
      } else {
         return super.getCloneItemStack(state, target, level, pos, player);
      }
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
      return ItemRequirement.of(AllBlocks.SHAFT.getDefaultState(), be);
   }

   @Override
   public Class<KineticBlockEntity> getBlockEntityClass() {
      return KineticBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends KineticBlockEntity>)AllBlockEntityTypes.ENCASED_SHAFT.get();
   }

   @Override
   public Block getCasing() {
      return this.casing.get();
   }

   @Override
   public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player, InteractionHand hand, BlockHitResult ray) {
      KineticBlockEntity.switchToBlockState(
         level, pos, (BlockState)this.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, (Axis)state.getValue(RotatedPillarKineticBlock.AXIS))
      );
   }
}
