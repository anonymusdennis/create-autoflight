package dev.simulated_team.simulated.content.blocks.rope.rope_winch;

import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.impl.contraption.BlockMovementChecksImpl;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.simulated_team.simulated.content.blocks.rope.RopeHolderBlock;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.util.DirectionalAxisShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RopeWinchBlock
   extends DirectionalAxisKineticBlock
   implements IBE<RopeWinchBlockEntity>,
   RopeHolderBlock<RopeWinchBlockEntity>,
   BlockSubLevelAssemblyListener,
   BlockSubLevelCollisionShape {
   private static final DirectionalAxisShaper ROPE_WINCH = DirectionalAxisShaper.make(SimBlockShapes.ROPE_WINCH);
   private static final DirectionalAxisShaper PHYSICS_COLLIDER = DirectionalAxisShaper.make(SimBlockShapes.ROPE_CONNECTOR_COLLIDER);

   public RopeWinchBlock(Properties properties) {
      super(properties);
   }

   public BlockEntityType<? extends RopeWinchBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends RopeWinchBlockEntity>)SimBlockEntityTypes.ROPE_WINCH.get();
   }

   public Class<RopeWinchBlockEntity> getBlockEntityClass() {
      return RopeWinchBlockEntity.class;
   }

   protected Direction getFacingForPlacement(BlockPlaceContext context) {
      return context.getClickedFace();
   }

   protected boolean getAxisAlignmentForPlacement(BlockPlaceContext context) {
      return context.getHorizontalDirection().getAxis() != Axis.X;
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return ROPE_WINCH.get((Direction)state.getValue(FACING), (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE));
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      return !level.isClientSide() && stack.is(SimTags.Items.DESTROYS_ROPE)
         ? RopeHolderBlock.shearRope(this, level, pos, (ServerPlayer)player)
         : super.useItemOn(stack, state, level, pos, player, hand, hitResult);
   }

   public VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state) {
      return PHYSICS_COLLIDER.get((Direction)state.getValue(FACING), (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE));
   }

   static {
      BlockMovementChecksImpl.registerAttachedCheck((state, world, pos, direction) -> {
         BlockState relativeState = world.getBlockState(pos.relative(direction));
         if (state.getBlock() instanceof RopeWinchBlock && state.getValue(FACING) == direction.getOpposite()) {
            return CheckResult.SUCCESS;
         } else {
            return relativeState.getBlock() instanceof RopeWinchBlock && relativeState.getValue(FACING) == direction ? CheckResult.SUCCESS : CheckResult.PASS;
         }
      });
   }
}
