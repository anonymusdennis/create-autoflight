package com.simibubi.create.content.redstone.thresholdSwitch;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
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
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;

public class ThresholdSwitchBlock extends DirectedDirectionalBlock implements IBE<ThresholdSwitchBlockEntity> {
   public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 5);

   public ThresholdSwitchBlock(Properties p_i48377_1_) {
      super(p_i48377_1_);
   }

   public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
      this.updateObservedInventory(state, worldIn, pos);
   }

   private void updateObservedInventory(BlockState state, LevelReader world, BlockPos pos) {
      this.withBlockEntityDo(world, pos, ThresholdSwitchBlockEntity::updateCurrentLevel);
   }

   public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return side != null && side.getOpposite() != getTargetDirection(state);
   }

   public boolean isSignalSource(BlockState state) {
      return true;
   }

   public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return side == getTargetDirection(blockState).getOpposite()
         ? 0
         : this.getBlockEntityOptional(blockAccess, pos).filter(ThresholdSwitchBlockEntity::isPowered).map($ -> 15).orElse(0);
   }

   public void tick(BlockState blockState, ServerLevel world, BlockPos pos, RandomSource random) {
      this.getBlockEntityOptional(world, pos).ifPresent(ThresholdSwitchBlockEntity::updatePowerAfterDelay);
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{LEVEL}));
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
   protected void displayScreen(ThresholdSwitchBlockEntity be, Player player) {
      if (player instanceof LocalPlayer) {
         ScreenOpener.open(new ThresholdSwitchScreen(be));
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = this.defaultBlockState();
      Direction preferredFacing = null;

      for (Direction face : context.getNearestLookingDirections()) {
         BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos().relative(face));
         if (be != null
            && (
               be.getLevel().getCapability(ItemHandler.BLOCK, be.getBlockPos(), null) != null
                  || be.getLevel().getCapability(FluidHandler.BLOCK, be.getBlockPos(), null) != null
            )) {
            preferredFacing = face;
            break;
         }
      }

      if (preferredFacing == null) {
         Direction facing = context.getNearestLookingDirection();
         preferredFacing = context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? facing : facing.getOpposite();
      }

      if (preferredFacing.getAxis() == Axis.Y) {
         state = (BlockState)state.setValue(TARGET, preferredFacing == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR);
         preferredFacing = context.getHorizontalDirection();
      }

      return (BlockState)state.setValue(FACING, preferredFacing);
   }

   @Override
   public Class<ThresholdSwitchBlockEntity> getBlockEntityClass() {
      return ThresholdSwitchBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends ThresholdSwitchBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ThresholdSwitchBlockEntity>)AllBlockEntityTypes.THRESHOLD_SWITCH.get();
   }
}
