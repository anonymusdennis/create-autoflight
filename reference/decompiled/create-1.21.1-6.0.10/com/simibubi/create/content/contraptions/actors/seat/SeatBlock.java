package com.simibubi.create.content.contraptions.actors.seat;

import com.google.common.base.Optional;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SeatBlock extends Block implements ProperWaterloggedBlock {
   protected final DyeColor color;

   public SeatBlock(Properties properties, DyeColor color) {
      super(properties);
      this.color = color;
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(WATERLOGGED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{WATERLOGGED}));
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.withWater(super.getStateForPlacement(pContext), pContext);
   }

   public BlockState updateShape(
      BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos
   ) {
      this.updateWater(pLevel, pState, pCurrentPos);
      return pState;
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   public void fallOn(Level p_152426_, BlockState p_152427_, BlockPos p_152428_, Entity p_152429_, float p_152430_) {
      super.fallOn(p_152426_, p_152427_, p_152428_, p_152429_, p_152430_ * 0.5F);
   }

   public void updateEntityAfterFallOn(BlockGetter reader, Entity entity) {
      BlockPos pos = entity.blockPosition();
      if (!(entity instanceof Player) && entity instanceof LivingEntity && canBePickedUp(entity) && !isSeatOccupied(entity.level(), pos)) {
         if (reader.getBlockState(pos).getBlock() == this) {
            sitDown(entity.level(), pos, entity);
         }
      } else if (entity.isSuppressingBounce()) {
         super.updateEntityAfterFallOn(reader, entity);
      } else {
         Vec3 vec3 = entity.getDeltaMovement();
         if (vec3.y < 0.0) {
            double d0 = entity instanceof LivingEntity ? 1.0 : 0.8;
            entity.setDeltaMovement(vec3.x, -vec3.y * 0.66F * d0, vec3.z);
         }
      }
   }

   public PathType getBlockPathType(BlockState state, BlockGetter world, BlockPos pos, @Nullable Mob entity) {
      return PathType.RAIL;
   }

   public VoxelShape getShape(BlockState p_220053_1_, BlockGetter p_220053_2_, BlockPos p_220053_3_, CollisionContext p_220053_4_) {
      return AllShapes.SEAT;
   }

   public VoxelShape getCollisionShape(BlockState p_220071_1_, BlockGetter p_220071_2_, BlockPos p_220071_3_, CollisionContext ctx) {
      if (ctx instanceof EntityCollisionContext ecc && ecc.getEntity() instanceof Player player) {
         return AllShapes.SEAT_COLLISION_PLAYERS;
      }

      return AllShapes.SEAT_COLLISION;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.isShiftKeyDown() && !(player instanceof FakePlayer)) {
         DyeColor color = DyeColor.getColor(stack);
         if (color == null || color == this.color) {
            List<SeatEntity> seats = level.getEntitiesOfClass(SeatEntity.class, new AABB(pos));
            if (!seats.isEmpty()) {
               SeatEntity seatEntity = seats.get(0);
               List<Entity> passengers = seatEntity.getPassengers();
               if (!passengers.isEmpty() && passengers.get(0) instanceof Player) {
                  return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
               } else {
                  if (!level.isClientSide) {
                     seatEntity.ejectPassengers();
                     player.startRiding(seatEntity);
                  }

                  return ItemInteractionResult.SUCCESS;
               }
            } else if (level.isClientSide) {
               return ItemInteractionResult.SUCCESS;
            } else {
               sitDown(level, pos, (Entity)getLeashed(level, player).or(player));
               return ItemInteractionResult.SUCCESS;
            }
         } else if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
         } else {
            BlockState newState = BlockHelper.copyProperties(state, AllBlocks.SEATS.get(color).getDefaultState());
            level.setBlockAndUpdate(pos, newState);
            return ItemInteractionResult.SUCCESS;
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public static boolean isSeatOccupied(Level world, BlockPos pos) {
      return !world.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).isEmpty();
   }

   public static Optional<Entity> getLeashed(Level level, Player player) {
      for (Entity e : player.level().getEntities((Entity)null, player.getBoundingBox().inflate(10.0), ex -> true)) {
         if (e instanceof Mob mob && mob.getLeashHolder() == player && canBePickedUp(e)) {
            return Optional.of(mob);
         }
      }

      return Optional.absent();
   }

   public static boolean canBePickedUp(Entity passenger) {
      if (passenger instanceof Shulker) {
         return false;
      } else if (passenger instanceof Player) {
         return false;
      } else if (AllTags.AllEntityTags.IGNORE_SEAT.matches(passenger)) {
         return false;
      } else {
         return !AllConfigs.server().logistics.seatHostileMobs.get() && !passenger.getType().getCategory().isFriendly()
            ? false
            : passenger instanceof LivingEntity;
      }
   }

   public static void sitDown(Level level, BlockPos pos, Entity entity) {
      if (!level.isClientSide) {
         SeatEntity seat = new SeatEntity(level);
         seat.setPos((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5);
         level.addFreshEntity(seat);
         entity.startRiding(seat, true);
         if (entity instanceof TamableAnimal ta) {
            ta.setInSittingPose(true);
         }
      }
   }

   public DyeColor getColor() {
      return this.color;
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }
}
