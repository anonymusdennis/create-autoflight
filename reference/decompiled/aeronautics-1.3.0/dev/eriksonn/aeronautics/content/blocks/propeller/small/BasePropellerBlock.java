package dev.eriksonn.aeronautics.content.blocks.propeller.small;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.eriksonn.aeronautics.index.AeroBlockShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BasePropellerBlock extends DirectionalKineticBlock implements IBE<BasePropellerBlockEntity> {
   public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");

   public BasePropellerBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.getStateDefinition().any()).setValue(REVERSED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{REVERSED});
      super.createBlockStateDefinition(builder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction preferredFacing = this.getPreferredFacing(context);
      if (preferredFacing == null) {
         preferredFacing = context.getClickedFace().getOpposite();
      }

      return (BlockState)this.defaultBlockState()
         .setValue(FACING, context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? preferredFacing : preferredFacing.getOpposite());
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AeroBlockShapes.PROPELLER.get((Direction)pState.getValue(FACING));
   }

   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(FACING)).getAxis();
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face == ((Direction)state.getValue(FACING)).getOpposite();
   }

   public Class<BasePropellerBlockEntity> getBlockEntityClass() {
      return BasePropellerBlockEntity.class;
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Vec3 diff = context.getClickLocation().subtract(context.getClickedPos().getCenter());
      Direction facing = (Direction)state.getValue(FACING);
      Vec3i normal = facing.getNormal();
      if (context.getClickedFace() != facing && !(diff.dot(new Vec3((double)normal.getX(), (double)normal.getY(), (double)normal.getZ())) > 0.0)) {
         return super.onWrenched(state, context);
      } else {
         state = (BlockState)state.cycle(REVERSED);
         context.getLevel().setBlock(context.getClickedPos(), state, 3);
         IWrenchable.playRotateSound(context.getLevel(), context.getClickedPos());
         return InteractionResult.SUCCESS;
      }
   }
}
