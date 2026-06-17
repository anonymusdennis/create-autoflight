package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.foundation.block.IBE;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimClickInteractions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class PhysicsAssemblerBlock
   extends FaceAttachedHorizontalDirectionalBlock
   implements IBE<PhysicsAssemblerBlockEntity>,
   IWrenchable,
   BlockSubLevelAssemblyListener {
   public static final MapCodec<PhysicsAssemblerBlock> CODEC = simpleCodec(PhysicsAssemblerBlock::new);

   public PhysicsAssemblerBlock(Properties properties) {
      super(properties);
   }

   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return canAttach(level, pos, getConnectedDirection(state).getOpposite());
   }

   public static boolean canAttach(LevelReader reader, BlockPos pos, Direction direction) {
      BlockPos blockpos = pos.relative(direction);
      return !reader.getBlockState(blockpos).getBlockSupportShape(reader, pos).getFaceShape(direction.getOpposite()).isEmpty();
   }

   protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   public Class<PhysicsAssemblerBlockEntity> getBlockEntityClass() {
      return PhysicsAssemblerBlockEntity.class;
   }

   public BlockEntityType<? extends PhysicsAssemblerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends PhysicsAssemblerBlockEntity>)SimBlockEntityTypes.PHYSICS_ASSEMBLER.get();
   }

   @NotNull
   public VoxelShape getShape(BlockState state, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
      Direction facing = (Direction)state.getValue(FACING);

      return switch ((AttachFace)state.getValue(FACE)) {
         case CEILING -> SimBlockShapes.PHYSICS_ASSEMBLER_CEILING_OUTLINE.get(facing);
         case FLOOR -> SimBlockShapes.PHYSICS_ASSEMBLER_OUTLINE.get(facing);
         default -> SimBlockShapes.PHYSICS_ASSEMBLER_WALL_OUTLINE.get(facing.getOpposite());
      };
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      Direction facing = (Direction)state.getValue(FACING);

      return switch ((AttachFace)state.getValue(FACE)) {
         case CEILING -> SimBlockShapes.PHYSICS_ASSEMBLER_CEILING_COLLISION.get(facing);
         case FLOOR -> SimBlockShapes.PHYSICS_ASSEMBLER_COLLISION.get(facing);
         default -> SimBlockShapes.PHYSICS_ASSEMBLER_WALL_COLLISION.get(facing.getOpposite());
      };
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (player instanceof DeployerFakePlayer) {
         if (!level.isClientSide) {
            this.withBlockEntityDo(level, pos, PhysicsAssemblerBlockEntity::assembleOrDisassemble);
         }

         return InteractionResult.SUCCESS;
      } else {
         return level.isClientSide && player.isLocalPlayer() ? this.onBlockEntityUse(level, pos, be -> {
            SimClickInteractions.PHYSICS_ASSEMBLER_MANAGER.startHold(level, player, pos);
            return InteractionResult.SUCCESS;
         }) : InteractionResult.CONSUME;
      }
   }

   public static Direction getStickyFacing(BlockState state) {
      return switch ((AttachFace)state.getValue(FACE)) {
         case CEILING -> Direction.UP;
         case FLOOR -> Direction.DOWN;
         case WALL -> ((Direction)state.getValue(FACING)).getOpposite();
         default -> throw new MatchException(null, null);
      };
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{FACING, FACE}));
   }

   public void afterMove(ServerLevel serverLevel, ServerLevel serverLevel1, BlockState blockState, BlockPos blockPos, BlockPos blockPos1) {
      if (serverLevel1.getBlockEntity(blockPos1) instanceof PhysicsAssemblerBlockEntity pabe) {
         pabe.setParent(serverLevel1);
      }
   }
}
