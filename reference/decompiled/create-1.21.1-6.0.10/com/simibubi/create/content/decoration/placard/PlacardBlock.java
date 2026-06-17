package com.simibubi.create.content.decoration.placard;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class PlacardBlock
   extends FaceAttachedHorizontalDirectionalBlock
   implements ProperWaterloggedBlock,
   IBE<PlacardBlockEntity>,
   SpecialBlockItemRequirement,
   IWrenchable {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final MapCodec<PlacardBlock> CODEC = simpleCodec(PlacardBlock::new);

   public PlacardBlock(Properties p_53182_) {
      super(p_53182_);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(WATERLOGGED, false)).setValue(POWERED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{FACE, FACING, WATERLOGGED, POWERED}));
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return canAttachLenient(pLevel, pPos, getConnectedDirection(pState).getOpposite());
   }

   public static boolean canAttachLenient(LevelReader pReader, BlockPos pPos, Direction pDirection) {
      BlockPos blockpos = pPos.relative(pDirection);
      return !pReader.getBlockState(blockpos).getCollisionShape(pReader, blockpos).isEmpty();
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState stateForPlacement = super.getStateForPlacement(pContext);
      if (stateForPlacement == null) {
         return null;
      } else {
         if (stateForPlacement.getValue(FACE) == AttachFace.FLOOR) {
            stateForPlacement = (BlockState)stateForPlacement.setValue(FACING, ((Direction)stateForPlacement.getValue(FACING)).getOpposite());
         }

         return this.withWater(stateForPlacement, pContext);
      }
   }

   public boolean isSignalSource(BlockState pState) {
      return true;
   }

   public int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return pBlockState.getValue(POWERED) ? 15 : 0;
   }

   public int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return pBlockState.getValue(POWERED) && getConnectedDirection(pBlockState) == pSide ? 15 : 0;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.PLACARD.get(getConnectedDirection(pState));
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      this.updateWater(pLevel, pState, pCurrentPos);
      return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (player.isShiftKeyDown()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (level.isClientSide) {
         return ItemInteractionResult.SUCCESS;
      } else {
         ItemStack inHand = player.getItemInHand(hand);
         return this.onBlockEntityUseItemOn(
            level,
            pos,
            pte -> {
               ItemStack inBlock = pte.getHeldItem();
               if (player.mayBuild() && !inHand.isEmpty() && inBlock.isEmpty()) {
                  level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                  pte.setHeldItem(inHand.copyWithCount(1));
                  if (!player.isCreative()) {
                     inHand.shrink(1);
                     if (inHand.isEmpty()) {
                        player.setItemInHand(hand, ItemStack.EMPTY);
                     }
                  }

                  return ItemInteractionResult.SUCCESS;
               } else if (inBlock.isEmpty()) {
                  return ItemInteractionResult.FAIL;
               } else if (inHand.isEmpty()) {
                  return ItemInteractionResult.FAIL;
               } else if ((Boolean)state.getValue(POWERED)) {
                  return ItemInteractionResult.FAIL;
               } else {
                  boolean test = inBlock.getItem() instanceof FilterItem
                     ? FilterItemStack.of(inBlock).test(level, inHand)
                     : ItemStack.isSameItemSameComponents(inHand, inBlock);
                  if (!test) {
                     AllSoundEvents.DENY.play(level, null, pos, 1.0F, 1.0F);
                     return ItemInteractionResult.SUCCESS;
                  } else {
                     AllSoundEvents.CONFIRM.play(level, null, pos, 1.0F, 1.0F);
                     level.setBlock(pos, (BlockState)state.setValue(POWERED, true), 3);
                     updateNeighbours(state, level, pos);
                     pte.poweredTicks = 19;
                     pte.notifyUpdate();
                     return ItemInteractionResult.SUCCESS;
                  }
               }
            }
         );
      }
   }

   public static Direction connectedDirection(BlockState state) {
      return getConnectedDirection(state);
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      boolean blockChanged = !pState.is(pNewState.getBlock());
      if (!pIsMoving && blockChanged && (Boolean)pState.getValue(POWERED)) {
         updateNeighbours(pState, pLevel, pPos);
      }

      if (pState.hasBlockEntity() && (blockChanged || !pNewState.hasBlockEntity())) {
         if (!pIsMoving) {
            this.withBlockEntityDo(pLevel, pPos, be -> Block.popResource(pLevel, pPos, be.getHeldItem()));
         }

         pLevel.removeBlockEntity(pPos);
      }
   }

   public static void updateNeighbours(BlockState pState, Level pLevel, BlockPos pPos) {
      pLevel.updateNeighborsAt(pPos, pState.getBlock());
      pLevel.updateNeighborsAt(pPos.relative(getConnectedDirection(pState).getOpposite()), pState.getBlock());
   }

   public void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
      if (!pLevel.isClientSide) {
         this.withBlockEntityDo(pLevel, pPos, pte -> {
            ItemStack heldItem = pte.getHeldItem();
            if (!heldItem.isEmpty()) {
               pPlayer.getInventory().placeItemBackInInventory(heldItem);
               pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
               pte.setHeldItem(ItemStack.EMPTY);
            }
         });
      }
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
      ItemStack placardStack = AllBlocks.PLACARD.asStack();
      if (be instanceof PlacardBlockEntity pbe) {
         ItemStack heldItem = pbe.getHeldItem();
         if (!heldItem.isEmpty()) {
            return new ItemRequirement(
               List.of(
                  new ItemRequirement.StackRequirement(placardStack, ItemRequirement.ItemUseType.CONSUME),
                  new ItemRequirement.StrictNbtStackRequirement(heldItem, ItemRequirement.ItemUseType.CONSUME)
               )
            );
         }
      }

      return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, placardStack);
   }

   @Override
   public Class<PlacardBlockEntity> getBlockEntityClass() {
      return PlacardBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends PlacardBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends PlacardBlockEntity>)AllBlockEntityTypes.PLACARD.get();
   }

   @NotNull
   protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
