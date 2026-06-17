package dev.simulated_team.simulated.content.blocks.rope.rope_connector;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.impl.contraption.BlockMovementChecksImpl;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.simulated_team.simulated.content.blocks.rope.RopeHolderBlock;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
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
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RopeConnectorBlock
   extends AbstractDirectionalAxisBlock
   implements IBE<RopeConnectorBlockEntity>,
   RopeHolderBlock<RopeConnectorBlockEntity>,
   BlockSubLevelAssemblyListener,
   BlockSubLevelCollisionShape {
   public static final MapCodec<RopeConnectorBlock> CODEC = simpleCodec(RopeConnectorBlock::new);
   private static final DirectionalAxisShaper SHAPE = DirectionalAxisShaper.make(SimBlockShapes.ROPE_CONNECTOR);
   private static final DirectionalAxisShaper PHYSICS_COLLIDER = DirectionalAxisShaper.make(SimBlockShapes.ROPE_CONNECTOR_COLLIDER);

   public RopeConnectorBlock(Properties properties) {
      super(properties);
   }

   protected MapCodec<? extends DirectionalBlock> codec() {
      return CODEC;
   }

   public VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state) {
      return PHYSICS_COLLIDER.get((Direction)state.getValue(FACING), (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE));
   }

   public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, worldIn, pos, oldState, isMoving);
      if (!worldIn.isClientSide) {
         ;
      }
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      IBE.onRemove(pState, pLevel, pPos, pNewState);
   }

   public Class<RopeConnectorBlockEntity> getBlockEntityClass() {
      return RopeConnectorBlockEntity.class;
   }

   public BlockEntityType<? extends RopeConnectorBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends RopeConnectorBlockEntity>)SimBlockEntityTypes.ROPE_CONNECTOR.get();
   }

   @Override
   protected Direction getFacingForPlacement(BlockPlaceContext context) {
      return context.getClickedFace();
   }

   @Override
   protected boolean getAxisAlignmentForPlacement(BlockPlaceContext context) {
      return context.getHorizontalDirection().getAxis() != Axis.X;
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return SHAPE.get((Direction)state.getValue(FACING), (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE));
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      return !level.isClientSide() && stack.is(SimTags.Items.DESTROYS_ROPE)
         ? RopeHolderBlock.shearRope(this, level, pos, (ServerPlayer)player)
         : super.useItemOn(stack, state, level, pos, player, hand, hitResult);
   }

   static {
      BlockMovementChecksImpl.registerAttachedCheck(
         (state, world, pos, direction) -> {
            BlockState relativeState = world.getBlockState(pos.relative(direction));
            if (state.getBlock() instanceof RopeConnectorBlock && state.getValue(FACING) == direction.getOpposite()) {
               return CheckResult.SUCCESS;
            } else {
               return relativeState.getBlock() instanceof RopeConnectorBlock && relativeState.getValue(FACING) == direction
                  ? CheckResult.SUCCESS
                  : CheckResult.PASS;
            }
         }
      );
   }
}
