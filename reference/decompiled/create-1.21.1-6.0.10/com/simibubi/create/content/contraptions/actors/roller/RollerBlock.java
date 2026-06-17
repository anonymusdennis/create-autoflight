package com.simibubi.create.content.contraptions.actors.roller;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.actors.AttachedActorBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.PoleHelper;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class RollerBlock extends AttachedActorBlock implements IBE<RollerBlockEntity> {
   private static final int placementHelperId = PlacementHelpers.register(new RollerBlock.PlacementHelper());
   public static final MapCodec<RollerBlock> CODEC = simpleCodec(RollerBlock::new);

   public RollerBlock(Properties p_i48377_1_) {
      super(p_i48377_1_);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return this.withWater((BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()), context);
   }

   @Override
   public Class<RollerBlockEntity> getBlockEntityClass() {
      return RollerBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends RollerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends RollerBlockEntity>)AllBlockEntityTypes.MECHANICAL_ROLLER.get();
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return Shapes.block();
   }

   @Override
   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      return true;
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
      this.withBlockEntityDo(pLevel, pPos, RollerBlockEntity::searchForSharedValues);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
      if (!player.isShiftKeyDown() && player.mayBuild() && placementHelper.matchesItem(stack)) {
         placementHelper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult);
         return ItemInteractionResult.SUCCESS;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @NotNull
   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   private static class PlacementHelper extends PoleHelper<Direction> {
      public PlacementHelper() {
         super(
            AllBlocks.MECHANICAL_ROLLER::has,
            state -> ((Direction)state.getValue(HorizontalDirectionalBlock.FACING)).getClockWise().getAxis(),
            HorizontalDirectionalBlock.FACING
         );
      }

      public Predicate<ItemStack> getItemPredicate() {
         return AllBlocks.MECHANICAL_ROLLER::isIn;
      }
   }
}
