package dev.simulated_team.simulated.content.blocks.torsion_spring;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.api.IDirectionalAnalogOutput;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TorsionSpringBlock
   extends DirectionalKineticBlock
   implements IBE<TorsionSpringBlockEntity>,
   ExtraKinetics.ExtraKineticsBlock,
   IDirectionalAnalogOutput {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public TorsionSpringBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(POWERED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{POWERED}));
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getOpposite() == state.getValue(FACING);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
   }

   protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
      return SimBlockShapes.TORSION_SPRING.get((Direction)blockState.getValue(FACING));
   }

   protected void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos2, boolean bl) {
      super.neighborChanged(blockState, level, blockPos, block, blockPos2, bl);
      boolean signal = level.hasNeighborSignal(blockPos);
      if (signal != (Boolean)blockState.getValue(POWERED)) {
         level.setBlock(blockPos, (BlockState)blockState.setValue(POWERED, signal), 2);
         this.withBlockEntityDo(level, blockPos, TorsionSpringBlockEntity::onSignalChanged);
      }
   }

   protected boolean hasAnalogOutputSignal(BlockState blockState) {
      return ((Direction)blockState.getValue(FACING)).getAxis().isHorizontal();
   }

   @Override
   public int getAnalogOutputSignalFrom(BlockState blockState, Level level, BlockPos blockPos, Direction dir) {
      Direction facing = (Direction)blockState.getValue(FACING);
      TorsionSpringBlockEntity be = (TorsionSpringBlockEntity)this.getBlockEntity(level, blockPos);
      float frac = Mth.clamp(be.getAngle() / (float)be.angleInput.getValue(), -1.0F, 1.0F);
      if ((double)Math.abs(be.getAngle()) < 0.99) {
         return 0;
      } else {
         int value = (int)(
            (frac < 0.0F ? Math.floor((double)(frac * 15.0F)) : Math.ceil((double)(frac * 15.0F)))
               * (double)(facing.getStepX() != 1 && facing.getStepZ() != 1 ? 1 : -1)
         );
         if (facing.getClockWise() == dir && value > 0) {
            return value;
         } else {
            return facing.getCounterClockWise() == dir && value < 0 ? -value : 0;
         }
      }
   }

   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(FACING)).getAxis();
   }

   public Class<TorsionSpringBlockEntity> getBlockEntityClass() {
      return TorsionSpringBlockEntity.class;
   }

   public BlockEntityType<? extends TorsionSpringBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends TorsionSpringBlockEntity>)SimBlockEntityTypes.TORSION_SPRING.get();
   }

   @Override
   public IRotate getExtraKineticsRotationConfiguration() {
      return TorsionSpringBlockEntity.Output.CONFIG;
   }
}
