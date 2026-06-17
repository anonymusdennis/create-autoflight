package dev.simulated_team.simulated.content.blocks.merging_glue;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class MergingGlueBlock extends DirectionalBlock implements IBE<MergingGlueBlockEntity> {
   public static final MapCodec<MergingGlueBlock> CODEC = simpleCodec(MergingGlueBlock::new);

   public MergingGlueBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
      return getConnectedDirection(state) == facing && !state.canSurvive(level, currentPos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
   }

   protected static Direction getConnectedDirection(BlockState state) {
      return ((Direction)state.getValue(FACING)).getOpposite();
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return canAttach(level, pos, getConnectedDirection(state));
   }

   public static boolean canAttach(LevelReader reader, BlockPos pos, Direction direction) {
      BlockPos blockpos = pos.relative(direction);
      return reader.getBlockState(blockpos).isFaceSturdy(reader, blockpos, direction.getOpposite());
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.empty();
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SimBlockShapes.MERGING_GlUE.get((Direction)state.getValue(FACING));
   }

   @NotNull
   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   @NotNull
   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FACING});
   }

   @NotNull
   protected MapCodec<? extends DirectionalBlock> codec() {
      return CODEC;
   }

   public Class<MergingGlueBlockEntity> getBlockEntityClass() {
      return MergingGlueBlockEntity.class;
   }

   public BlockEntityType<? extends MergingGlueBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends MergingGlueBlockEntity>)SimBlockEntityTypes.MERGING_GLUE.get();
   }
}
