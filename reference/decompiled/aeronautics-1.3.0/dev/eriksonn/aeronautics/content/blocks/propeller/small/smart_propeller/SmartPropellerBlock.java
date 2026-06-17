package dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlock;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.eriksonn.aeronautics.index.AeroBlockShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SmartPropellerBlock extends HorizontalAxisKineticBlock implements IBE<SmartPropellerBlockEntity> {
   public static final BooleanProperty REVERSED = BasePropellerBlock.REVERSED;
   public static final BooleanProperty CEILING = BooleanProperty.create("ceiling");

   public SmartPropellerBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.getStateDefinition().any()).setValue(REVERSED, false)).setValue(CEILING, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{REVERSED});
      builder.add(new Property[]{CEILING});
      super.createBlockStateDefinition(builder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = super.getStateForPlacement(context);
      Axis axis = (Axis)state.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);
      return (BlockState)((BlockState)state.setValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, axis == Axis.X ? Axis.Z : Axis.X))
         .setValue(CEILING, context.getClickedFace() == Direction.DOWN);
   }

   public Axis getRotationAxis(BlockState state) {
      return Axis.Y;
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return state.getValue(CEILING) ? face == Direction.UP : face == Direction.DOWN;
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      if (context.getClickedFace() == (state.getValue(CEILING) ? Direction.DOWN : Direction.UP)) {
         state = (BlockState)state.cycle(REVERSED);
         context.getLevel().setBlock(context.getClickedPos(), state, 3);
         IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
         return InteractionResult.SUCCESS;
      } else {
         return super.onWrenched(state, context);
      }
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return pState.getValue(CEILING)
         ? AeroBlockShapes.SMART_PROPELLER_CEILING.get((Axis)pState.getValue(HORIZONTAL_AXIS))
         : AeroBlockShapes.SMART_PROPELLER.get((Axis)pState.getValue(HORIZONTAL_AXIS));
   }

   public Class<SmartPropellerBlockEntity> getBlockEntityClass() {
      return SmartPropellerBlockEntity.class;
   }

   public BlockEntityType<? extends SmartPropellerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SmartPropellerBlockEntity>)AeroBlockEntityTypes.SMART_PROPELLER.get();
   }
}
