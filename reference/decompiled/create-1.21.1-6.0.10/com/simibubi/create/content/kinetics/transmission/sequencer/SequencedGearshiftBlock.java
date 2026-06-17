package com.simibubi.create.content.kinetics.transmission.sequencer;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class SequencedGearshiftBlock extends HorizontalAxisKineticBlock implements IBE<SequencedGearshiftBlockEntity>, TransformableBlock {
   public static final BooleanProperty VERTICAL = BooleanProperty.create("vertical");
   public static final IntegerProperty STATE = IntegerProperty.create("state", 0, 5);

   public SequencedGearshiftBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{STATE, VERTICAL}));
   }

   public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide) {
         if (!level.getBlockTicks().willTickThisTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
         }
      }
   }

   public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource r) {
      boolean previouslyPowered = (Integer)state.getValue(STATE) != 0;
      boolean isPowered = worldIn.hasNeighborSignal(pos);
      this.withBlockEntityDo(worldIn, pos, sgte -> sgte.onRedstoneUpdate(isPowered, previouslyPowered));
   }

   @Override
   protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
      return false;
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return state.getValue(VERTICAL) ? face.getAxis().isVertical() : super.hasShaftTowards(world, pos, state, face);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (AllItems.WRENCH.isIn(stack)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         if (stack.getItem() instanceof BlockItem blockItem
            && blockItem.getBlock() instanceof KineticBlock
            && this.hasShaftTowards(level, pos, state, hitResult.getDirection())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }

         CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.withBlockEntityDo(level, pos, be -> this.displayScreen(be, player)));
         return ItemInteractionResult.SUCCESS;
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected void displayScreen(SequencedGearshiftBlockEntity be, Player player) {
      if (player instanceof LocalPlayer) {
         ScreenOpener.open(new SequencedGearshiftScreen(be));
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Axis preferredAxis = RotatedPillarKineticBlock.getPreferredAxis(context);
      return preferredAxis == null || context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
         ? this.withAxis(context.getNearestLookingDirection().getAxis(), context)
         : this.withAxis(preferredAxis, context);
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      BlockState newState = state;
      if (context.getClickedFace().getAxis() != Axis.Y && state.getValue(HORIZONTAL_AXIS) != context.getClickedFace().getAxis()) {
         newState = (BlockState)state.cycle(VERTICAL);
      }

      return super.onWrenched(newState, context);
   }

   private BlockState withAxis(Axis axis, BlockPlaceContext context) {
      BlockState state = (BlockState)this.defaultBlockState().setValue(VERTICAL, axis.isVertical());
      return axis.isVertical()
         ? (BlockState)state.setValue(HORIZONTAL_AXIS, context.getHorizontalDirection().getAxis())
         : (BlockState)state.setValue(HORIZONTAL_AXIS, axis);
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return state.getValue(VERTICAL) ? Axis.Y : super.getRotationAxis(state);
   }

   @Override
   public Class<SequencedGearshiftBlockEntity> getBlockEntityClass() {
      return SequencedGearshiftBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends SequencedGearshiftBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SequencedGearshiftBlockEntity>)AllBlockEntityTypes.SEQUENCED_GEARSHIFT.get();
   }

   public boolean hasAnalogOutputSignal(BlockState p_149740_1_) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
      return (Integer)state.getValue(STATE);
   }

   @Override
   public BlockState transform(BlockState state, StructureTransform transform) {
      if (transform.mirror != null) {
         state = this.mirror(state, transform.mirror);
      }

      if (transform.rotationAxis == Axis.Y) {
         return this.rotate(state, transform.rotation);
      } else {
         if (transform.rotation.ordinal() % 2 == 1) {
            if (transform.rotationAxis != state.getValue(HORIZONTAL_AXIS)) {
               return (BlockState)state.cycle(VERTICAL);
            }

            if ((Boolean)state.getValue(VERTICAL)) {
               return (BlockState)((BlockState)state.cycle(VERTICAL)).cycle(HORIZONTAL_AXIS);
            }
         }

         return state;
      }
   }
}
