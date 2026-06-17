package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.IHaveBigOutline;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.api.IDirectionalAnalogOutput;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimClickInteractions;
import dev.simulated_team.simulated.util.QuietUse;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SteeringWheelBlock
   extends HorizontalDirectionalBlock
   implements IBE<SteeringWheelBlockEntity>,
   ProperWaterloggedBlock,
   IRotate,
   IHaveBigOutline,
   QuietUse,
   IDirectionalAnalogOutput {
   public static final BooleanProperty ON_FLOOR = BooleanProperty.create("on_floor");
   public static final MapCodec<SteeringWheelBlock> CODEC = simpleCodec(SteeringWheelBlock::new);

   public SteeringWheelBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(WATERLOGGED, false)).setValue(ON_FLOOR, true));
   }

   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      boolean onFloor = (Boolean)state.getValue(ON_FLOOR);
      Direction facing = (Direction)state.getValue(FACING);
      if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player && player.isLocalPlayer()) {
         VoxelShape wheel = (onFloor ? SimBlockShapes.STEERING_WHEEL_FLOOR : SimBlockShapes.STEERING_WHEEL_CEILING).get(facing);
         VoxelShape mount = SimBlockShapes.STEERING_WHEEL_MOUNT.get(facing);
         return lookingAtWheel(player, pos, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), wheel, mount) ? wheel : mount;
      }

      return state.getValue(ON_FLOOR) ? SimBlockShapes.STEERING_WHEEL_FULL_FLOOR.get(facing) : SimBlockShapes.STEERING_WHEEL_FULL_CEILING.get(facing);
   }

   public static boolean lookingAtWheel(Player player, BlockPos pos, float pt, BlockState state) {
      boolean onFloor = (Boolean)state.getValue(ON_FLOOR);
      Direction facing = (Direction)state.getValue(FACING);
      VoxelShape wheel = (onFloor ? SimBlockShapes.STEERING_WHEEL_FLOOR : SimBlockShapes.STEERING_WHEEL_CEILING).get(facing);
      VoxelShape mount = SimBlockShapes.STEERING_WHEEL_MOUNT.get(facing);
      return lookingAtWheel(player, pos, pt, wheel, mount);
   }

   public static boolean lookingAtWheel(Player player, BlockPos pos, float pt, VoxelShape wheel, VoxelShape mount) {
      Vec3 from = player.getEyePosition(pt);
      Vec3 to = from.add(player.getViewVector(pt).scale(player.blockInteractionRange()));
      SubLevel subLevel = Sable.HELPER.getContaining(player.level(), pos);
      if (subLevel != null) {
         Pose3dc pose;
         if (subLevel instanceof ClientSubLevel clientSubLevel) {
            pose = clientSubLevel.renderPose(pt);
         } else {
            pose = subLevel.logicalPose();
         }

         from = pose.transformPositionInverse(from);
         to = pose.transformPositionInverse(to);
      }

      BlockHitResult wheelResult = wheel.clip(from, to, pos);
      BlockHitResult mountResult = mount.clip(from, to, pos);
      if (wheelResult == null || wheelResult.getType() == Type.MISS) {
         return false;
      } else {
         return mountResult != null && mountResult.getType() != Type.MISS
            ? wheelResult.getLocation().distanceTo(from) < mountResult.getLocation().distanceTo(from)
            : true;
      }
   }

   protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
      return state.getValue(ON_FLOOR)
         ? SimBlockShapes.STEERING_WHEEL_FULL_FLOOR.get((Direction)state.getValue(FACING))
         : SimBlockShapes.STEERING_WHEEL_FULL_CEILING.get((Direction)state.getValue(FACING));
   }

   @Deprecated
   public VoxelShape getCollisionShape(BlockState state, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SimBlockShapes.STEERING_WHEEL_MOUNT.get((Direction)state.getValue(FACING));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{WATERLOGGED}).add(new Property[]{FACING}).add(new Property[]{ON_FLOOR}));
   }

   @Nullable
   @Override
   public InteractionResult quietUse(Player player, InteractionHand hand, BlockPos pos, BlockState state) {
      return lookingAtWheel(player, pos, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), state)
         ? this.getBlockEntityOptional(player.level(), pos).map(be -> {
            if (!be.held && !be.isMaterialValid(player.getItemInHand(hand)) && !be.angleInput.testHit(this.getPlayerHitLocation())) {
               SimClickInteractions.STEERING_WHEEL_MANAGER.startHold(player.level(), player, pos);
               return InteractionResult.SUCCESS;
            } else {
               return null;
            }
         }).orElse(null)
         : null;
   }

   public Vec3 getPlayerHitLocation() {
      return Minecraft.getInstance().hitResult.getLocation();
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      return player.isShiftKeyDown()
         ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
         : this.onBlockEntityUseItemOn(level, pos, be -> be.applyMaterialIfValid(stack));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState defaultState = this.withWater(this.defaultBlockState(), context);

      boolean floor = switch (context.getClickedFace()) {
         case UP -> true;
         case DOWN -> false;
         default -> {
            Direction verticalLookDir = Arrays.stream(context.getNearestLookingDirections()).filter(d -> d.getAxis().isVertical()).findFirst().get();
            yield verticalLookDir == Direction.DOWN;
         }
      };
      Direction horizontalLookDir = Arrays.stream(context.getNearestLookingDirections()).filter(d -> d.getAxis().isHorizontal()).findFirst().get();
      return (BlockState)((BlockState)defaultState.setValue(FACING, horizontalLookDir.getOpposite())).setValue(ON_FLOOR, floor);
   }

   public BlockState updateShape(
      BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos
   ) {
      this.updateWater(pLevel, pState, pCurrentPos);
      return pState;
   }

   protected boolean hasAnalogOutputSignal(BlockState blockState) {
      return true;
   }

   @Override
   public int getAnalogOutputSignalFrom(BlockState blockState, Level level, BlockPos blockPos, Direction dir) {
      Direction facing = (Direction)blockState.getValue(FACING);
      SteeringWheelBlockEntity be = (SteeringWheelBlockEntity)this.getBlockEntity(level, blockPos);
      float frac = Mth.clamp(be.targetAngleToUpdate / (float)be.angleInput.getValue(), -1.0F, 1.0F);
      if (facing == dir) {
         return be.held ? 15 : 0;
      } else if ((double)Math.abs(be.getAngle()) < 0.99) {
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

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face == (state.getValue(ON_FLOOR) ? Direction.DOWN : Direction.UP);
   }

   public Axis getRotationAxis(BlockState state) {
      return Axis.Y;
   }

   public Class<SteeringWheelBlockEntity> getBlockEntityClass() {
      return SteeringWheelBlockEntity.class;
   }

   public BlockEntityType<? extends SteeringWheelBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SteeringWheelBlockEntity>)SimBlockEntityTypes.STEERING_WHEEL.get();
   }
}
