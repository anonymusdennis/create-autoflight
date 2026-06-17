package com.simibubi.create.content.kinetics.gauge;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public class GaugeBlock extends DirectionalAxisKineticBlock implements IBE<GaugeBlockEntity> {
   public static final GaugeShaper GAUGE = GaugeShaper.make();
   protected GaugeBlock.Type type;

   public static GaugeBlock speed(Properties properties) {
      return new GaugeBlock(properties, GaugeBlock.Type.SPEED);
   }

   public static GaugeBlock stress(Properties properties) {
      return new GaugeBlock(properties, GaugeBlock.Type.STRESS);
   }

   protected GaugeBlock(Properties properties, GaugeBlock.Type type) {
      super(properties);
      this.type = type;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Level world = context.getLevel();
      Direction face = context.getClickedFace();
      BlockPos placedOnPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
      BlockState placedOnState = world.getBlockState(placedOnPos);
      Block block = placedOnState.getBlock();
      if (block instanceof IRotate && ((IRotate)block).hasShaftTowards(world, placedOnPos, placedOnState, face)) {
         BlockState toPlace = this.defaultBlockState();
         Direction horizontalFacing = context.getHorizontalDirection();
         Direction nearestLookingDirection = context.getNearestLookingDirection();
         boolean lookPositive = nearestLookingDirection.getAxisDirection() == AxisDirection.POSITIVE;
         if (face.getAxis() == Axis.X) {
            toPlace = (BlockState)((BlockState)toPlace.setValue(FACING, lookPositive ? Direction.NORTH : Direction.SOUTH))
               .setValue(AXIS_ALONG_FIRST_COORDINATE, true);
         } else if (face.getAxis() == Axis.Y) {
            toPlace = (BlockState)((BlockState)toPlace.setValue(FACING, horizontalFacing.getOpposite()))
               .setValue(AXIS_ALONG_FIRST_COORDINATE, horizontalFacing.getAxis() == Axis.X);
         } else {
            toPlace = (BlockState)((BlockState)toPlace.setValue(FACING, lookPositive ? Direction.WEST : Direction.EAST))
               .setValue(AXIS_ALONG_FIRST_COORDINATE, false);
         }

         return toPlace;
      } else {
         return super.getStateForPlacement(context);
      }
   }

   @Override
   protected Direction getFacingForPlacement(BlockPlaceContext context) {
      return context.getClickedFace();
   }

   @Override
   protected boolean getAxisAlignmentForPlacement(BlockPlaceContext context) {
      return context.getHorizontalDirection().getAxis() != Axis.X;
   }

   public boolean shouldRenderHeadOnFace(Level world, BlockPos pos, BlockState state, Direction face) {
      if (face.getAxis().isVertical()) {
         return false;
      } else if (face == ((Direction)state.getValue(FACING)).getOpposite()) {
         return false;
      } else if (face.getAxis() == this.getRotationAxis(state)) {
         return false;
      } else {
         return this.getRotationAxis(state) == Axis.Y && face != state.getValue(FACING)
            ? false
            : Block.shouldRenderFace(state, world, pos, face, pos.relative(face)) || world instanceof WrappedLevel;
      }
   }

   public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
      BlockEntity be = worldIn.getBlockEntity(pos);
      if (be != null && be instanceof GaugeBlockEntity gaugeBE) {
         if (gaugeBE.dialTarget != 0.0F) {
            int color = gaugeBE.color;

            for (Direction face : Iterate.directions) {
               if (this.shouldRenderHeadOnFace(worldIn, pos, stateIn, face)) {
                  Vector3f rgb = new Color(color).asVectorF();
                  Vec3 faceVec = Vec3.atLowerCornerOf(face.getNormal());
                  Direction positiveFacing = Direction.get(AxisDirection.POSITIVE, face.getAxis());
                  Vec3 positiveFaceVec = Vec3.atLowerCornerOf(positiveFacing.getNormal());
                  int particleCount = gaugeBE.dialTarget > 1.0F ? 4 : 1;
                  if (particleCount != 1 || !(rand.nextFloat() > 0.25F)) {
                     for (int i = 0; i < particleCount; i++) {
                        Vec3 mul = VecHelper.offsetRandomly(Vec3.ZERO, rand, 0.25F)
                           .multiply(new Vec3(1.0, 1.0, 1.0).subtract(positiveFaceVec))
                           .normalize()
                           .scale(0.3F);
                        Vec3 offset = VecHelper.getCenterOf(pos).add(faceVec.scale(0.55)).add(mul);
                        worldIn.addParticle(new DustParticleOptions(rgb, 1.0F), offset.x, offset.y, offset.z, mul.x, mul.y, mul.z);
                     }
                  }
               }
            }
         }
      }
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return GAUGE.get((Direction)state.getValue(FACING), (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE));
   }

   public boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
      return worldIn.getBlockEntity(pos) instanceof GaugeBlockEntity gaugeBlockEntity
         ? Mth.ceil(Mth.clamp(gaugeBlockEntity.dialTarget * 14.0F, 0.0F, 15.0F))
         : 0;
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @Override
   public Class<GaugeBlockEntity> getBlockEntityClass() {
      return GaugeBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends GaugeBlockEntity> getBlockEntityType() {
      return this.type == GaugeBlock.Type.SPEED
         ? (BlockEntityType)AllBlockEntityTypes.SPEEDOMETER.get()
         : (BlockEntityType)AllBlockEntityTypes.STRESSOMETER.get();
   }

   public static enum Type implements StringRepresentable {
      SPEED,
      STRESS;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}
