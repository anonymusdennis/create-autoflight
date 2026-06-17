package com.simibubi.create.content.kinetics.chainDrive;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;

public class ChainDriveBlock extends RotatedPillarKineticBlock implements IBE<KineticBlockEntity>, TransformableBlock {
   public static final Property<ChainDriveBlock.Part> PART = EnumProperty.create("part", ChainDriveBlock.Part.class);
   public static final BooleanProperty CONNECTED_ALONG_FIRST_COORDINATE = DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;

   public ChainDriveBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(PART, ChainDriveBlock.Part.NONE));
   }

   public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   public PushReaction getPistonPushReaction(BlockState state) {
      return PushReaction.NORMAL;
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{PART, CONNECTED_ALONG_FIRST_COORDINATE}));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Axis placedAxis = context.getNearestLookingDirection().getAxis();
      Axis axis = context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? placedAxis : getPreferredAxis(context);
      if (axis == null) {
         axis = placedAxis;
      }

      BlockState state = (BlockState)this.defaultBlockState().setValue(AXIS, axis);

      for (Direction facing : Iterate.directions) {
         if (facing.getAxis() != axis) {
            BlockPos pos = context.getClickedPos();
            BlockPos offset = pos.relative(facing);
            state = this.updateShape(state, facing, context.getLevel().getBlockState(offset), context.getLevel(), pos, offset);
         }
      }

      return state;
   }

   public BlockState updateShape(BlockState stateIn, Direction face, BlockState neighbour, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
      ChainDriveBlock.Part part = (ChainDriveBlock.Part)stateIn.getValue(PART);
      Axis axis = (Axis)stateIn.getValue(AXIS);
      boolean connectionAlongFirst = (Boolean)stateIn.getValue(CONNECTED_ALONG_FIRST_COORDINATE);
      Axis connectionAxis = connectionAlongFirst ? (axis == Axis.X ? Axis.Y : Axis.X) : (axis == Axis.Z ? Axis.Y : Axis.Z);
      Axis faceAxis = face.getAxis();
      boolean facingAlongFirst = axis == Axis.X ? faceAxis.isVertical() : faceAxis == Axis.X;
      boolean positive = face.getAxisDirection() == AxisDirection.POSITIVE;
      if (axis == faceAxis) {
         return stateIn;
      } else if (!(neighbour.getBlock() instanceof ChainDriveBlock)) {
         if (facingAlongFirst != connectionAlongFirst || part == ChainDriveBlock.Part.NONE) {
            return stateIn;
         } else if (part == ChainDriveBlock.Part.MIDDLE) {
            return (BlockState)stateIn.setValue(PART, positive ? ChainDriveBlock.Part.END : ChainDriveBlock.Part.START);
         } else {
            return part == ChainDriveBlock.Part.START == positive ? (BlockState)stateIn.setValue(PART, ChainDriveBlock.Part.NONE) : stateIn;
         }
      } else {
         ChainDriveBlock.Part otherPart = (ChainDriveBlock.Part)neighbour.getValue(PART);
         Axis otherAxis = (Axis)neighbour.getValue(AXIS);
         boolean otherConnection = (Boolean)neighbour.getValue(CONNECTED_ALONG_FIRST_COORDINATE);
         Axis otherConnectionAxis = otherConnection ? (otherAxis == Axis.X ? Axis.Y : Axis.X) : (otherAxis == Axis.Z ? Axis.Y : Axis.Z);
         if (neighbour.getValue(AXIS) == faceAxis) {
            return stateIn;
         } else if (otherPart != ChainDriveBlock.Part.NONE && otherConnectionAxis != faceAxis) {
            return stateIn;
         } else {
            if (part == ChainDriveBlock.Part.NONE) {
               part = positive ? ChainDriveBlock.Part.START : ChainDriveBlock.Part.END;
               connectionAlongFirst = axis == Axis.X ? faceAxis.isVertical() : faceAxis == Axis.X;
            } else if (connectionAxis != faceAxis) {
               return stateIn;
            }

            if (part == ChainDriveBlock.Part.START != positive) {
               part = ChainDriveBlock.Part.MIDDLE;
            }

            return (BlockState)((BlockState)stateIn.setValue(PART, part)).setValue(CONNECTED_ALONG_FIRST_COORDINATE, connectionAlongFirst);
         }
      }
   }

   @Override
   public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
      return originalState.getValue(PART) == ChainDriveBlock.Part.NONE
         ? super.getRotatedBlockState(originalState, targetedFace)
         : super.getRotatedBlockState(originalState, Direction.get(AxisDirection.POSITIVE, getConnectionAxis(originalState)));
   }

   @Override
   public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
      Axis axis = (Axis)newState.getValue(AXIS);
      newState = (BlockState)this.defaultBlockState().setValue(AXIS, axis);
      if (newState.hasProperty(BlockStateProperties.POWERED)) {
         newState = (BlockState)newState.setValue(BlockStateProperties.POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
      }

      for (Direction facing : Iterate.directions) {
         if (facing.getAxis() != axis) {
            BlockPos pos = context.getClickedPos();
            BlockPos offset = pos.relative(facing);
            newState = this.updateShape(newState, facing, context.getLevel().getBlockState(offset), context.getLevel(), pos, offset);
         }
      }

      return newState;
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == state.getValue(AXIS);
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return (Axis)state.getValue(AXIS);
   }

   public static boolean areBlocksConnected(BlockState state, BlockState other, Direction facing) {
      ChainDriveBlock.Part part = (ChainDriveBlock.Part)state.getValue(PART);
      Axis connectionAxis = getConnectionAxis(state);
      Axis otherConnectionAxis = getConnectionAxis(other);
      if (otherConnectionAxis != connectionAxis) {
         return false;
      } else if (facing.getAxis() != connectionAxis) {
         return false;
      } else {
         return facing.getAxisDirection() != AxisDirection.POSITIVE || part != ChainDriveBlock.Part.MIDDLE && part != ChainDriveBlock.Part.START
            ? facing.getAxisDirection() == AxisDirection.NEGATIVE && (part == ChainDriveBlock.Part.MIDDLE || part == ChainDriveBlock.Part.END)
            : true;
      }
   }

   protected static Axis getConnectionAxis(BlockState state) {
      Axis axis = (Axis)state.getValue(AXIS);
      boolean connectionAlongFirst = (Boolean)state.getValue(CONNECTED_ALONG_FIRST_COORDINATE);
      return connectionAlongFirst ? (axis == Axis.X ? Axis.Y : Axis.X) : (axis == Axis.Z ? Axis.Y : Axis.Z);
   }

   public static float getRotationSpeedModifier(KineticBlockEntity from, KineticBlockEntity to) {
      float fromMod = 1.0F;
      float toMod = 1.0F;
      if (from instanceof ChainGearshiftBlockEntity) {
         fromMod = ((ChainGearshiftBlockEntity)from).getModifier();
      }

      if (to instanceof ChainGearshiftBlockEntity) {
         toMod = ((ChainGearshiftBlockEntity)to).getModifier();
      }

      return fromMod / toMod;
   }

   @Override
   public Class<KineticBlockEntity> getBlockEntityClass() {
      return KineticBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends KineticBlockEntity>)AllBlockEntityTypes.ENCASED_SHAFT.get();
   }

   @Override
   public BlockState rotate(BlockState state, Rotation rot) {
      return this.rotate(state, rot, Axis.Y);
   }

   protected BlockState rotate(BlockState pState, Rotation rot, Axis rotAxis) {
      Axis connectionAxis = getConnectionAxis(pState);
      Direction direction = Direction.fromAxisAndDirection(connectionAxis, AxisDirection.POSITIVE);
      Direction normal = Direction.fromAxisAndDirection((Axis)pState.getValue(AXIS), AxisDirection.POSITIVE);

      for (int i = 0; i < rot.ordinal(); i++) {
         direction = direction.getClockWise(rotAxis);
         normal = normal.getClockWise(rotAxis);
      }

      if (direction.getAxisDirection() == AxisDirection.NEGATIVE) {
         pState = this.reversePart(pState);
      }

      Axis newAxis = normal.getAxis();
      Axis newConnectingDirection = direction.getAxis();
      boolean alongFirst = newAxis == Axis.X && newConnectingDirection == Axis.Y || newAxis != Axis.X && newConnectingDirection == Axis.X;
      return (BlockState)((BlockState)pState.setValue(AXIS, newAxis)).setValue(CONNECTED_ALONG_FIRST_COORDINATE, alongFirst);
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      Axis connectionAxis = getConnectionAxis(pState);
      return pMirror.mirror(Direction.fromAxisAndDirection(connectionAxis, AxisDirection.POSITIVE)).getAxisDirection() == AxisDirection.POSITIVE
         ? pState
         : this.reversePart(pState);
   }

   protected BlockState reversePart(BlockState pState) {
      ChainDriveBlock.Part part = (ChainDriveBlock.Part)pState.getValue(PART);
      if (part == ChainDriveBlock.Part.START) {
         return (BlockState)pState.setValue(PART, ChainDriveBlock.Part.END);
      } else {
         return part == ChainDriveBlock.Part.END ? (BlockState)pState.setValue(PART, ChainDriveBlock.Part.START) : pState;
      }
   }

   @Override
   public BlockState transform(BlockState state, StructureTransform transform) {
      return this.rotate(this.mirror(state, transform.mirror), transform.rotation, transform.rotationAxis);
   }

   public static enum Part implements StringRepresentable {
      START,
      MIDDLE,
      END,
      NONE;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}
