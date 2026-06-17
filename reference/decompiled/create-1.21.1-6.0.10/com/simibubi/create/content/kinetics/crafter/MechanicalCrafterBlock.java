package com.simibubi.create.content.kinetics.crafter;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class MechanicalCrafterBlock extends HorizontalKineticBlock implements IBE<MechanicalCrafterBlockEntity>, ICogWheel {
   public static final EnumProperty<Pointing> POINTING = EnumProperty.create("pointing", Pointing.class);

   public MechanicalCrafterBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(POINTING, Pointing.UP));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{POINTING}));
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(HORIZONTAL_FACING)).getAxis();
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction face = context.getClickedFace();
      BlockPos placedOnPos = context.getClickedPos().relative(face.getOpposite());
      BlockState blockState = context.getLevel().getBlockState(placedOnPos);
      if (blockState.getBlock() != this || context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
         BlockState stateForPlacement = super.getStateForPlacement(context);
         Direction direction = (Direction)stateForPlacement.getValue(HORIZONTAL_FACING);
         if (direction != face) {
            stateForPlacement = (BlockState)stateForPlacement.setValue(POINTING, pointingFromFacing(face, direction));
         }

         return stateForPlacement;
      } else {
         Direction otherFacing = (Direction)blockState.getValue(HORIZONTAL_FACING);
         Pointing pointing = pointingFromFacing(face, otherFacing);
         return (BlockState)((BlockState)this.defaultBlockState().setValue(HORIZONTAL_FACING, otherFacing)).setValue(POINTING, pointing);
      }
   }

   @Override
   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.getBlock() == newState.getBlock() && getTargetDirection(state) != getTargetDirection(newState)) {
         MechanicalCrafterBlockEntity crafter = CrafterHelper.getCrafter(worldIn, pos);
         if (crafter != null) {
            crafter.blockChanged();
         }
      }

      if (state.hasBlockEntity() && !state.is(newState.getBlock())) {
         MechanicalCrafterBlockEntity crafter = CrafterHelper.getCrafter(worldIn, pos);
         if (crafter != null) {
            if (crafter.covered) {
               Block.popResource(worldIn, pos, AllItems.CRAFTER_SLOT_COVER.asStack());
            }

            if (!isMoving) {
               crafter.ejectWholeGrid();
            }
         }

         for (Direction direction : Iterate.directions) {
            if (direction.getAxis() != ((Direction)state.getValue(HORIZONTAL_FACING)).getAxis()) {
               BlockPos otherPos = pos.relative(direction);
               ConnectedInputHandler.ConnectedInput thisInput = CrafterHelper.getInput(worldIn, pos);
               ConnectedInputHandler.ConnectedInput otherInput = CrafterHelper.getInput(worldIn, otherPos);
               if (thisInput != null && otherInput != null && pos.offset((Vec3i)thisInput.data.get(0)).equals(otherPos.offset((Vec3i)otherInput.data.get(0)))) {
                  ConnectedInputHandler.toggleConnection(worldIn, pos, otherPos);
               }
            }
         }
      }

      super.onRemove(state, worldIn, pos, newState, isMoving);
   }

   public static Pointing pointingFromFacing(Direction pointingFace, Direction blockFacing) {
      boolean positive = blockFacing.getAxisDirection() == AxisDirection.POSITIVE;
      Pointing pointing = pointingFace == Direction.DOWN ? Pointing.UP : Pointing.DOWN;
      if (pointingFace == Direction.EAST) {
         pointing = positive ? Pointing.LEFT : Pointing.RIGHT;
      }

      if (pointingFace == Direction.WEST) {
         pointing = positive ? Pointing.RIGHT : Pointing.LEFT;
      }

      if (pointingFace == Direction.NORTH) {
         pointing = positive ? Pointing.LEFT : Pointing.RIGHT;
      }

      if (pointingFace == Direction.SOUTH) {
         pointing = positive ? Pointing.RIGHT : Pointing.LEFT;
      }

      return pointing;
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      if (context.getClickedFace() == state.getValue(HORIZONTAL_FACING)) {
         if (!context.getLevel().isClientSide) {
            KineticBlockEntity.switchToBlockState(context.getLevel(), context.getClickedPos(), (BlockState)state.cycle(POINTING));
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (level.getBlockEntity(pos) instanceof MechanicalCrafterBlockEntity crafter) {
         if (AllBlocks.MECHANICAL_ARM.isIn(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else {
            boolean isHand = stack.isEmpty() && hand == InteractionHand.MAIN_HAND;
            boolean wrenched = AllItems.WRENCH.isIn(stack);
            if (hitResult.getDirection() == state.getValue(HORIZONTAL_FACING)) {
               if (crafter.phase != MechanicalCrafterBlockEntity.Phase.IDLE && !wrenched) {
                  crafter.ejectWholeGrid();
                  return ItemInteractionResult.SUCCESS;
               } else if (crafter.phase != MechanicalCrafterBlockEntity.Phase.IDLE || isHand || wrenched) {
                  ItemStack inSlot = crafter.getInventory().getItem(0);
                  if (inSlot.isEmpty()) {
                     if (!crafter.covered || wrenched) {
                        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                     } else if (level.isClientSide) {
                        return ItemInteractionResult.SUCCESS;
                     } else {
                        crafter.covered = false;
                        crafter.setChanged();
                        crafter.sendData();
                        if (!player.isCreative()) {
                           player.getInventory().placeItemBackInInventory(AllItems.CRAFTER_SLOT_COVER.asStack());
                        }

                        return ItemInteractionResult.SUCCESS;
                     }
                  } else if (!isHand && !ItemStack.isSameItemSameComponents(stack, inSlot)) {
                     return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                  } else if (level.isClientSide) {
                     return ItemInteractionResult.SUCCESS;
                  } else {
                     player.getInventory().placeItemBackInInventory(inSlot);
                     crafter.getInventory().setStackInSlot(0, ItemStack.EMPTY);
                     return ItemInteractionResult.SUCCESS;
                  }
               } else if (level.isClientSide) {
                  return ItemInteractionResult.SUCCESS;
               } else if (!AllItems.CRAFTER_SLOT_COVER.isIn(stack)) {
                  IItemHandler capability = (IItemHandler)level.getCapability(ItemHandler.BLOCK, crafter.getBlockPos(), null);
                  if (capability == null) {
                     return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                  } else {
                     ItemStack remainder = ItemHandlerHelper.insertItem(capability, stack.copy(), false);
                     if (remainder.getCount() != stack.getCount()) {
                        player.setItemInHand(hand, remainder);
                     }

                     return ItemInteractionResult.SUCCESS;
                  }
               } else if (crafter.covered) {
                  return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
               } else if (!crafter.inventory.isEmpty()) {
                  return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
               } else {
                  crafter.covered = true;
                  crafter.setChanged();
                  crafter.sendData();
                  if (!player.isCreative()) {
                     stack.shrink(1);
                  }

                  return ItemInteractionResult.SUCCESS;
               }
            } else {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      InvManipulationBehaviour behaviour = BlockEntityBehaviour.get(worldIn, pos, InvManipulationBehaviour.TYPE);
      if (behaviour != null) {
         behaviour.onNeighborChanged(fromPos);
      }
   }

   @Override
   public float getParticleTargetRadius() {
      return 0.85F;
   }

   @Override
   public float getParticleInitialRadius() {
      return 0.75F;
   }

   public static Direction getTargetDirection(BlockState state) {
      if (!AllBlocks.MECHANICAL_CRAFTER.has(state)) {
         return Direction.UP;
      } else {
         Direction facing = (Direction)state.getValue(HORIZONTAL_FACING);
         Pointing point = (Pointing)state.getValue(POINTING);
         Vec3 targetVec = new Vec3(0.0, 1.0, 0.0);
         targetVec = VecHelper.rotate(targetVec, (double)(-point.getXRotation()), Axis.Z);
         targetVec = VecHelper.rotate(targetVec, (double)AngleHelper.horizontalAngle(facing), Axis.Y);
         return Direction.getNearest(targetVec.x, targetVec.y, targetVec.z);
      }
   }

   public static boolean isValidTarget(Level world, BlockPos targetPos, BlockState crafterState) {
      BlockState targetState = world.getBlockState(targetPos);
      if (!world.isLoaded(targetPos)) {
         return false;
      } else if (!AllBlocks.MECHANICAL_CRAFTER.has(targetState)) {
         return false;
      } else {
         return crafterState.getValue(HORIZONTAL_FACING) != targetState.getValue(HORIZONTAL_FACING)
            ? false
            : Math.abs(((Pointing)crafterState.getValue(POINTING)).getXRotation() - ((Pointing)targetState.getValue(POINTING)).getXRotation()) != 180;
      }
   }

   @Override
   public Class<MechanicalCrafterBlockEntity> getBlockEntityClass() {
      return MechanicalCrafterBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends MechanicalCrafterBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends MechanicalCrafterBlockEntity>)AllBlockEntityTypes.MECHANICAL_CRAFTER.get();
   }
}
