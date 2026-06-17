package com.simibubi.create.content.redstone.nixieTube;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import java.util.function.BiConsumer;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NixieTubeBlock
   extends DoubleFaceAttachedBlock
   implements IBE<NixieTubeBlockEntity>,
   IWrenchable,
   SimpleWaterloggedBlock,
   SpecialBlockItemRequirement {
   protected final DyeColor color;

   public NixieTubeBlock(Properties properties, DyeColor color) {
      super(properties);
      this.color = color;
      this.registerDefaultState(
         (BlockState)((BlockState)this.defaultBlockState().setValue(FACE, DoubleFaceAttachedBlock.DoubleAttachFace.FLOOR))
            .setValue(BlockStateProperties.WATERLOGGED, false)
      );
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (player.isShiftKeyDown()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         NixieTubeBlockEntity nixie = this.getBlockEntity(level, pos);
         if (nixie == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (isInComputerControlledRow(level, pos)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (stack.isEmpty()) {
            if (nixie.reactsToRedstone()) {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            } else {
               nixie.clearCustomText();
               this.updateDisplayedRedstoneValue(state, level, pos);
               return ItemInteractionResult.SUCCESS;
            }
         } else {
            boolean display = stack.getItem() == Items.NAME_TAG && stack.has(DataComponents.CUSTOM_NAME) || AllBlocks.CLIPBOARD.isIn(stack);
            DyeColor dye = DyeColor.getColor(stack);
            if (!display && dye == null) {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            } else {
               Component component = (Component)stack.getOrDefault(DataComponents.CUSTOM_NAME, Component.empty());
               if (AllBlocks.CLIPBOARD.isIn(stack)) {
                  List<ClipboardEntry> entries = ClipboardEntry.getLastViewedEntries(stack);
                  if (!entries.isEmpty()) {
                     component = entries.getFirst().text;
                  }
               }

               if (level.isClientSide) {
                  return ItemInteractionResult.SUCCESS;
               } else {
                  String tagUsed = Serializer.toJson(component, level.registryAccess());
                  walkNixies(level, pos, true, (currentPos, rowPosition) -> {
                     if (display) {
                        this.withBlockEntityDo(level, currentPos, be -> be.displayCustomText(tagUsed, rowPosition));
                     }

                     if (dye != null) {
                        level.setBlockAndUpdate(currentPos, withColor(state, dye));
                     }
                  });
                  return ItemInteractionResult.SUCCESS;
               }
            }
         }
      }
   }

   public static Direction getLeftNixieDirection(@NotNull BlockState state) {
      Direction left = ((Direction)state.getValue(FACING)).getOpposite();
      if (state.getValue(FACE) == DoubleFaceAttachedBlock.DoubleAttachFace.WALL) {
         left = Direction.UP;
      }

      if (state.getValue(FACE) == DoubleFaceAttachedBlock.DoubleAttachFace.WALL_REVERSED) {
         left = Direction.DOWN;
      }

      return left;
   }

   public static Direction getRightNixieDirection(@NotNull BlockState state) {
      return getLeftNixieDirection(state).getOpposite();
   }

   public static boolean isInComputerControlledRow(@NotNull LevelAccessor world, @NotNull BlockPos pos) {
      return Mods.COMPUTERCRAFT.isLoaded() && !walkNixies(world, pos, false, null);
   }

   public static boolean walkNixies(
      @NotNull LevelAccessor world, @NotNull BlockPos start, boolean allowComputerControlled, @Nullable BiConsumer<BlockPos, Integer> callback
   ) {
      BlockState state = world.getBlockState(start);
      if (!(state.getBlock() instanceof NixieTubeBlock)) {
         return false;
      } else {
         if (!Mods.COMPUTERCRAFT.isLoaded()) {
            allowComputerControlled = true;
         }

         BlockPos currentPos = start;
         Direction left = getLeftNixieDirection(state);
         Direction right = left.getOpposite();

         while (true) {
            BlockPos nextPos = currentPos.relative(left);
            if (!areNixieBlocksEqual(world.getBlockState(nextPos), state)) {
               if (!allowComputerControlled) {
                  if (world.getBlockEntity(start) instanceof NixieTubeBlockEntity ntbe && ntbe.computerBehaviour.hasAttachedComputer()) {
                     return false;
                  }

                  currentPos = start;

                  while (true) {
                     BlockPos nextPosx = currentPos.relative(right);
                     if (!areNixieBlocksEqual(world.getBlockState(nextPosx), state)) {
                        currentPos = currentPos;
                        break;
                     }

                     if (world.getBlockEntity(nextPosx) instanceof NixieTubeBlockEntity ntbe && ntbe.computerBehaviour.hasAttachedComputer()) {
                        return false;
                     }

                     currentPos = nextPosx;
                  }
               }

               int index = 0;

               while (true) {
                  if (callback != null) {
                     callback.accept(currentPos, index);
                  }

                  BlockPos nextPosxx = currentPos.relative(right);
                  if (!areNixieBlocksEqual(world.getBlockState(nextPosxx), state)) {
                     return true;
                  }

                  currentPos = nextPosxx;
                  index++;
               }
            }

            if (!allowComputerControlled && world.getBlockEntity(nextPos) instanceof NixieTubeBlockEntity ntbe && ntbe.computerBehaviour.hasAttachedComputer()) {
               return false;
            }

            currentPos = nextPos;
         }
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{FACE, FACING, BlockStateProperties.WATERLOGGED}));
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!(newState.getBlock() instanceof NixieTubeBlock)) {
         world.removeBlockEntity(pos);
         if (Mods.COMPUTERCRAFT.isLoaded()) {
            Direction left = getLeftNixieDirection(state);
            BlockPos leftPos = pos.relative(left);
            if (areNixieBlocksEqual(world.getBlockState(leftPos), state)) {
               boolean leftRowComputerControlled = isInComputerControlledRow(world, leftPos);
               walkNixies(world, leftPos, true, leftRowComputerControlled ? (currentPos, rowPosition) -> {
                  if (world.getBlockEntity(currentPos) instanceof NixieTubeBlockEntity ntbe) {
                     ntbe.displayEmptyText(rowPosition);
                  }
               } : (currentPos, rowPosition) -> {
                  if (world.getBlockEntity(currentPos) instanceof NixieTubeBlockEntity ntbe) {
                     updateDisplayedRedstoneValue(ntbe, state, true);
                  }
               });
            }

            Direction right = left.getOpposite();
            BlockPos rightPos = pos.relative(right);
            if (areNixieBlocksEqual(world.getBlockState(rightPos), state)) {
               boolean rightRowComputerControlled = isInComputerControlledRow(world, rightPos);
               walkNixies(world, rightPos, true, rightRowComputerControlled ? (currentPos, rowPosition) -> {
                  if (world.getBlockEntity(currentPos) instanceof NixieTubeBlockEntity ntbe) {
                     ntbe.displayEmptyText(rowPosition);
                  }
               } : (currentPos, rowPosition) -> {
                  if (world.getBlockEntity(currentPos) instanceof NixieTubeBlockEntity ntbe) {
                     updateDisplayedRedstoneValue(ntbe, state, true);
                  }
               });
            }
         }
      }
   }

   public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return AllBlocks.ORANGE_NIXIE_TUBE.asStack();
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
      return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, ((NixieTubeBlock)AllBlocks.ORANGE_NIXIE_TUBE.get()).asItem());
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      Direction facing = (Direction)pState.getValue(FACING);

      return switch ((DoubleFaceAttachedBlock.DoubleAttachFace)pState.getValue(FACE)) {
         case CEILING -> AllShapes.NIXIE_TUBE_CEILING.get(facing.getClockWise().getAxis());
         case FLOOR -> AllShapes.NIXIE_TUBE.get(facing.getClockWise().getAxis());
         default -> AllShapes.NIXIE_TUBE_WALL.get(facing);
      };
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      return this.color != DyeColor.ORANGE
         ? ((NixieTubeBlock)AllBlocks.ORANGE_NIXIE_TUBE.get()).getCloneItemStack(state, target, level, pos, player)
         : super.getCloneItemStack(state, target, level, pos, player);
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor world, BlockPos pos, BlockPos neighbourPos) {
      if ((Boolean)state.getValue(BlockStateProperties.WATERLOGGED)) {
         world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
      }

      return state;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = super.getStateForPlacement(context);
      if (state == null) {
         return null;
      } else {
         if (state.getValue(FACE) != DoubleFaceAttachedBlock.DoubleAttachFace.WALL
            && state.getValue(FACE) != DoubleFaceAttachedBlock.DoubleAttachFace.WALL_REVERSED) {
            state = (BlockState)state.setValue(FACING, ((Direction)state.getValue(FACING)).getClockWise());
         }

         return (BlockState)state.setValue(
            BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER
         );
      }
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide) {
         if (!level.getBlockTicks().willTickThisTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
         }
      }
   }

   public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource r) {
      this.updateDisplayedRedstoneValue(state, worldIn, pos);
   }

   public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
      if (state.getBlock() != oldState.getBlock() && !isMoving && !(oldState.getBlock() instanceof NixieTubeBlock)) {
         if (Mods.COMPUTERCRAFT.isLoaded() && isInComputerControlledRow(worldIn, pos)) {
            walkNixies(worldIn, pos, true, (currentPos, rowPosition) -> {
               if (worldIn.getBlockEntity(currentPos) instanceof NixieTubeBlockEntity ntbe) {
                  ntbe.displayEmptyText(rowPosition);
               }
            });
         } else {
            this.updateDisplayedRedstoneValue(state, worldIn, pos);
         }
      }
   }

   public static void updateDisplayedRedstoneValue(NixieTubeBlockEntity be, BlockState state, boolean force) {
      if (be.getLevel() != null && !be.getLevel().isClientSide) {
         if (be.reactsToRedstone() || force) {
            be.updateRedstoneStrength(getPower(be.getLevel(), state, be.getBlockPos()));
         }
      }
   }

   private void updateDisplayedRedstoneValue(BlockState state, Level level, BlockPos pos) {
      if (!level.isClientSide) {
         this.withBlockEntityDo(level, pos, be -> updateDisplayedRedstoneValue(be, state, false));
      }
   }

   static boolean isValidBlock(BlockGetter world, BlockPos pos, boolean above) {
      BlockState state = world.getBlockState(pos.above(above ? 1 : -1));
      return !state.getShape(world, pos).isEmpty();
   }

   private static int getPower(Level level, BlockState state, BlockPos pos) {
      int power = 0;

      for (Direction direction : Iterate.directions) {
         power = Math.max(level.getSignal(pos.relative(direction), direction), power);
      }

      for (Direction direction : Iterate.directions) {
         if (((Direction)state.getValue(FACING)).getOpposite() != direction) {
            power = Math.max(level.getSignal(pos.relative(direction), Direction.UP), power);
         }
      }

      return power;
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return side != null;
   }

   @Override
   public Class<NixieTubeBlockEntity> getBlockEntityClass() {
      return NixieTubeBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends NixieTubeBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends NixieTubeBlockEntity>)AllBlockEntityTypes.NIXIE_TUBE.get();
   }

   public DyeColor getColor() {
      return this.color;
   }

   public static boolean areNixieBlocksEqual(BlockState blockState, BlockState otherState) {
      if (!(blockState.getBlock() instanceof NixieTubeBlock)) {
         return false;
      } else {
         return !(otherState.getBlock() instanceof NixieTubeBlock) ? false : withColor(blockState, DyeColor.WHITE) == withColor(otherState, DyeColor.WHITE);
      }
   }

   public static BlockState withColor(BlockState state, DyeColor color) {
      return (BlockState)((BlockState)((BlockState)(color == DyeColor.ORANGE ? AllBlocks.ORANGE_NIXIE_TUBE : AllBlocks.NIXIE_TUBES.get(color))
               .getDefaultState()
               .setValue(FACING, (Direction)state.getValue(FACING)))
            .setValue(BlockStateProperties.WATERLOGGED, (Boolean)state.getValue(BlockStateProperties.WATERLOGGED)))
         .setValue(FACE, (DoubleFaceAttachedBlock.DoubleAttachFace)state.getValue(FACE));
   }

   public static DyeColor colorOf(BlockState blockState) {
      return blockState.getBlock() instanceof NixieTubeBlock ? ((NixieTubeBlock)blockState.getBlock()).color : DyeColor.ORANGE;
   }

   public static Direction getFacing(BlockState sideState) {
      return getConnectedDirection(sideState);
   }
}
