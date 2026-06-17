package dev.simulated_team.simulated.content.blocks.analog_transmission;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import dev.simulated_team.simulated.util.placement_helpers.CogwheelPlacementExtension;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

public class AnalogTransmissionBlock extends RotatedPillarKineticBlock implements IBE<AnalogTransmissionBlockEntity>, ExtraKinetics.ExtraKineticsBlock {
   public static final int placementHelperId = PlacementHelpers.register(
      new CogwheelPlacementExtension(i -> i.getItem() instanceof CogwheelBlockItem, SimBlocks.ANALOG_TRANSMISSION::has)
   );
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public AnalogTransmissionBlock(Properties properties) {
      super(properties);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      ItemStack heldItem = player.getItemInHand(interactionHand);
      IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
      return helper.matchesItem(heldItem)
         ? helper.getOffset(player, level, blockState, blockPos, blockHitResult)
            .placeInWorld(level, (BlockItem)heldItem.getItem(), player, interactionHand, blockHitResult)
         : super.useItemOn(itemStack, blockState, level, blockPos, player, interactionHand, blockHitResult);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{POWERED}));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
   }

   @Override
   public IRotate getExtraKineticsRotationConfiguration() {
      return AnalogTransmissionBlockEntity.AnalogTransmissionCogwheel.EXTRA_COGWHEEL_CONFIG;
   }

   public Axis getRotationAxis(BlockState state) {
      return (Axis)state.getValue(AXIS);
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == state.getValue(AXIS);
   }

   public Class<AnalogTransmissionBlockEntity> getBlockEntityClass() {
      return AnalogTransmissionBlockEntity.class;
   }

   public BlockEntityType<? extends AnalogTransmissionBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends AnalogTransmissionBlockEntity>)SimBlockEntityTypes.SIMPLE_BE.get();
   }
}
