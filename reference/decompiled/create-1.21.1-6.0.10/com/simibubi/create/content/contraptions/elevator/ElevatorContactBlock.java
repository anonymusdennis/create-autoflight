package com.simibubi.create.content.contraptions.elevator;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.redstone.contact.RedstoneContactBlock;
import com.simibubi.create.content.redstone.diodes.BrassDiodeBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.Optional;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ElevatorContactBlock extends WrenchableDirectionalBlock implements IBE<ElevatorContactBlockEntity>, SpecialBlockItemRequirement {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty CALLING = BooleanProperty.create("calling");
   public static final BooleanProperty POWERING = BrassDiodeBlock.POWERING;
   public static final MapCodec<ElevatorContactBlock> CODEC = simpleCodec(ElevatorContactBlock::new);

   public ElevatorContactBlock(Properties pProperties) {
      super(pProperties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(CALLING, false)).setValue(POWERING, false))
               .setValue(POWERED, false))
            .setValue(FACING, Direction.SOUTH)
      );
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{CALLING, POWERING, POWERED}));
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      InteractionResult onWrenched = super.onWrenched(state, context);
      if (onWrenched != InteractionResult.SUCCESS) {
         return onWrenched;
      } else {
         Level level = context.getLevel();
         if (level.isClientSide()) {
            return onWrenched;
         } else {
            BlockPos pos = context.getClickedPos();
            state = level.getBlockState(pos);
            Direction facing = (Direction)state.getValue(RedstoneContactBlock.FACING);
            if (facing.getAxis() != Axis.Y && ElevatorColumn.get(level, new ElevatorColumn.ColumnCoords(pos.getX(), pos.getZ(), facing)) != null) {
               return onWrenched;
            } else {
               level.setBlockAndUpdate(pos, BlockHelper.copyProperties(state, AllBlocks.REDSTONE_CONTACT.getDefaultState()));
               return onWrenched;
            }
         }
      }
   }

   @Nullable
   public static ElevatorColumn.ColumnCoords getColumnCoords(LevelAccessor level, BlockPos pos) {
      BlockState blockState = level.getBlockState(pos);
      if (!AllBlocks.ELEVATOR_CONTACT.has(blockState) && !AllBlocks.REDSTONE_CONTACT.has(blockState)) {
         return null;
      } else {
         Direction facing = (Direction)blockState.getValue(FACING);
         return new ElevatorColumn.ColumnCoords(pos.getX(), pos.getZ(), facing);
      }
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      if (!pLevel.isClientSide) {
         boolean isPowered = (Boolean)pState.getValue(POWERED);
         if (isPowered != pLevel.hasNeighborSignal(pPos)) {
            pLevel.setBlock(pPos, (BlockState)pState.cycle(POWERED), 2);
            if (!isPowered) {
               if (!(Boolean)pState.getValue(CALLING)) {
                  ElevatorColumn elevatorColumn = ElevatorColumn.getOrCreate(pLevel, getColumnCoords(pLevel, pPos));
                  this.callToContactAndUpdate(elevatorColumn, pState, pLevel, pPos, true);
               }
            }
         }
      }
   }

   public void callToContactAndUpdate(ElevatorColumn elevatorColumn, BlockState pState, Level pLevel, BlockPos pPos, boolean powered) {
      pLevel.setBlock(pPos, (BlockState)pState.cycle(CALLING), 2);

      for (BlockPos otherPos : elevatorColumn.getContacts()) {
         if (!otherPos.equals(pPos)) {
            BlockState otherState = pLevel.getBlockState(otherPos);
            if (AllBlocks.ELEVATOR_CONTACT.has(otherState)) {
               pLevel.setBlock(otherPos, (BlockState)otherState.setValue(CALLING, false), 18);
               this.scheduleActivation(pLevel, otherPos);
            }
         }
      }

      if (powered) {
         pState = (BlockState)pState.setValue(POWERED, true);
      }

      pLevel.setBlock(pPos, (BlockState)pState.setValue(CALLING, true), 2);
      pLevel.updateNeighborsAt(pPos, this);
      elevatorColumn.target(pPos.getY());
      elevatorColumn.markDirty();
   }

   public void scheduleActivation(LevelAccessor pLevel, BlockPos pPos) {
      if (!pLevel.getBlockTicks().hasScheduledTick(pPos, this)) {
         pLevel.scheduleTick(pPos, this, 1);
      }
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRand) {
      boolean wasPowering = (Boolean)pState.getValue(POWERING);
      Optional<ElevatorContactBlockEntity> optionalBE = this.getBlockEntityOptional(pLevel, pPos);
      boolean shouldBePowering = optionalBE.<Boolean>map(be -> {
         boolean activateBlock = be.activateBlock;
         be.activateBlock = false;
         be.setChanged();
         return activateBlock;
      }).orElse(false);
      shouldBePowering |= RedstoneContactBlock.hasValidContact(pLevel, pPos, (Direction)pState.getValue(FACING));
      if (wasPowering || shouldBePowering) {
         pLevel.setBlock(pPos, (BlockState)pState.setValue(POWERING, shouldBePowering), 18);
      }

      pLevel.updateNeighborsAt(pPos, this);
   }

   public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
      if (facing != stateIn.getValue(FACING)) {
         return stateIn;
      } else {
         boolean hasValidContact = RedstoneContactBlock.hasValidContact(worldIn, currentPos, facing);
         if ((Boolean)stateIn.getValue(POWERING) != hasValidContact) {
            this.scheduleActivation(worldIn, currentPos);
         }

         return stateIn;
      }
   }

   public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   public boolean isSignalSource(BlockState state) {
      return (Boolean)state.getValue(POWERING);
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      return AllBlocks.REDSTONE_CONTACT.asStack();
   }

   public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) {
      return side == null ? true : state.getValue(FACING) != side.getOpposite();
   }

   public int getSignal(BlockState state, BlockGetter blockAccess, BlockPos pos, Direction side) {
      if (side == null) {
         return 0;
      } else {
         BlockState toState = blockAccess.getBlockState(pos.relative(side.getOpposite()));
         if (toState.is(this)) {
            return 0;
         } else {
            return state.getValue(POWERING) ? 15 : 0;
         }
      }
   }

   @Override
   public Class<ElevatorContactBlockEntity> getBlockEntityClass() {
      return ElevatorContactBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends ElevatorContactBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ElevatorContactBlockEntity>)AllBlockEntityTypes.ELEVATOR_CONTACT.get();
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
      return ItemRequirement.of(AllBlocks.REDSTONE_CONTACT.getDefaultState(), be);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (player != null && AllItems.WRENCH.isIn(stack)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.withBlockEntityDo(level, pos, be -> this.displayScreen(be, player)));
         return ItemInteractionResult.SUCCESS;
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected void displayScreen(ElevatorContactBlockEntity be, Player player) {
      if (player instanceof LocalPlayer) {
         ScreenOpener.open(new ElevatorContactScreen(be.getBlockPos(), be.shortName, be.longName, be.doorControls.mode));
      }
   }

   public static int getLight(BlockState state) {
      return state.getValue(POWERING) ? 10 : 0;
   }

   @NotNull
   @Override
   protected MapCodec<? extends DirectionalBlock> codec() {
      return CODEC;
   }
}
