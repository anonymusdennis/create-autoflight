package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimClickInteractions;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class ThrottleLeverBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<ThrottleLeverBlockEntity>, IWrenchable, CommonRedstoneBlock {
   public static MapCodec<ThrottleLeverBlock> CODEC = simpleCodec(ThrottleLeverBlock::new);
   public static BooleanProperty INVERTED = BooleanProperty.create("inverted");

   public ThrottleLeverBlock(Properties builder) {
      super(builder);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(INVERTED, false));
   }

   private static void addParticles(BlockState state, LevelAccessor level, BlockPos pos, float alpha) {
      Direction direction = ((Direction)state.getValue(FACING)).getOpposite();
      Direction direction1 = getConnectedDirection(state).getOpposite();
      double d0 = (double)pos.getX() + 0.5 + 0.1 * (double)direction.getStepX() + 0.2 * (double)direction1.getStepX();
      double d1 = (double)pos.getY() + 0.5 + 0.1 * (double)direction.getStepY() + 0.2 * (double)direction1.getStepY();
      double d2 = (double)pos.getZ() + 0.5 + 0.1 * (double)direction.getStepZ() + 0.2 * (double)direction1.getStepZ();
      level.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), alpha), d0, d1, d2, 0.0, 0.0, 0.0);
   }

   static void updateNeighbors(BlockState state, Level world, BlockPos pos) {
      world.updateNeighborsAt(pos, state.getBlock());
      world.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), state.getBlock());
   }

   protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (player.getMainHandItem().is(AllItems.WRENCH)) {
         return InteractionResult.PASS;
      } else if (level.isClientSide && player.isLocalPlayer()) {
         addParticles(state, level, pos, 1.0F);
         return this.onBlockEntityUse(level, pos, be -> {
            if (!SimClickInteractions.THROTTLE_LEVER_MANAGER.isActive()) {
               SimClickInteractions.THROTTLE_LEVER_MANAGER.startHold(level, player, pos);
               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.FAIL;
            }
         });
      } else {
         return InteractionResult.CONSUME;
      }
   }

   @Override
   public boolean commonConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
      return direction != null;
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Level level = context.getLevel();
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockPos pos = context.getClickedPos();
         int signal = this.getSignal(state, level, pos, context.getClickedFace());
         addParticles(state, level, pos, 1.0F);
         level.setBlock(pos, (BlockState)state.cycle(INVERTED), 2);
         this.withBlockEntityDo(level, pos, be -> be.setSignal(state.getValue(INVERTED) ? 15 - signal : signal));
         return InteractionResult.SUCCESS;
      }
   }

   public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return this.getBlockEntityOptional(blockAccess, pos).map(al -> al.state).orElse(0);
   }

   public boolean isSignalSource(BlockState state) {
      return true;
   }

   public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return getConnectedDirection(blockState) == side ? this.getSignal(blockState, blockAccess, pos, side) : 0;
   }

   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      this.withBlockEntityDo(pLevel, pPos, be -> {
         if (be.state != 0 && pRandom.nextFloat() < 0.25F) {
            addParticles(pState, pLevel, pPos, 0.5F);
         }
      });
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!isMoving && state.getBlock() != newState.getBlock()) {
         this.withBlockEntityDo(level, pos, be -> {
            if (be.state != 0) {
               updateNeighbors(state, level, pos);
            }

            level.removeBlockEntity(pos);
         });
      }
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(FACE) != AttachFace.WALL && ((Direction)state.getValue(FACING)).getAxis() == Axis.X
         ? SimBlockShapes.THROTTLE_LEVER_SWAP.get(getConnectedDirection(state))
         : SimBlockShapes.THROTTLE_LEVER.get(getConnectedDirection(state));
   }

   public VoxelShape getHandleShape(BlockState state) {
      return state.getValue(FACE) != AttachFace.WALL && ((Direction)state.getValue(FACING)).getAxis() == Axis.X
         ? SimBlockShapes.THROTTLE_LEVER_HANDLE_SWAP.get(getConnectedDirection(state))
         : SimBlockShapes.THROTTLE_LEVER_HANDLE.get(getConnectedDirection(state));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{FACING, FACE, INVERTED}));
   }

   public Class<ThrottleLeverBlockEntity> getBlockEntityClass() {
      return ThrottleLeverBlockEntity.class;
   }

   public BlockEntityType<? extends ThrottleLeverBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ThrottleLeverBlockEntity>)SimBlockEntityTypes.THROTTLE_LEVER.get();
   }

   protected boolean isPathfindable(BlockState blockState, PathComputationType pathComputationType) {
      return false;
   }
}
