package com.simibubi.create.content.contraptions;

import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

public class StructureTransform {
   public static final StreamCodec<ByteBuf, StructureTransform> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      i -> i.offset,
      ByteBufCodecs.INT,
      i -> i.angle,
      CatnipStreamCodecBuilders.nullable(CatnipStreamCodecs.AXIS),
      i -> i.rotationAxis,
      CatnipStreamCodecBuilders.nullable(CatnipStreamCodecs.ROTATION),
      i -> i.rotation,
      CatnipStreamCodecBuilders.nullable(CatnipStreamCodecs.MIRROR),
      i -> i.mirror,
      StructureTransform::new
   );
   public Axis rotationAxis;
   public BlockPos offset;
   public int angle;
   public Rotation rotation;
   public Mirror mirror;

   private StructureTransform(BlockPos offset, int angle, Axis axis, Rotation rotation, Mirror mirror) {
      this.offset = offset;
      this.angle = angle;
      this.rotationAxis = axis;
      this.rotation = rotation;
      this.mirror = mirror;
   }

   public StructureTransform(BlockPos offset, Axis axis, Rotation rotation, Mirror mirror) {
      this(offset, rotation == Rotation.NONE ? 0 : (4 - rotation.ordinal()) * 90, axis, rotation, mirror);
   }

   public StructureTransform(BlockPos offset, float xRotation, float yRotation, float zRotation) {
      this.offset = offset;
      if (xRotation != 0.0F) {
         this.rotationAxis = Axis.X;
         this.angle = Math.round(xRotation / 90.0F) * 90;
      }

      if (yRotation != 0.0F) {
         this.rotationAxis = Axis.Y;
         this.angle = Math.round(yRotation / 90.0F) * 90;
      }

      if (zRotation != 0.0F) {
         this.rotationAxis = Axis.Z;
         this.angle = Math.round(zRotation / 90.0F) * 90;
      }

      this.angle %= 360;
      if (this.angle < -90) {
         this.angle += 360;
      }

      this.rotation = Rotation.NONE;
      if (this.angle == -90 || this.angle == 270) {
         this.rotation = Rotation.CLOCKWISE_90;
      }

      if (this.angle == 90) {
         this.rotation = Rotation.COUNTERCLOCKWISE_90;
      }

      if (this.angle == 180) {
         this.rotation = Rotation.CLOCKWISE_180;
      }

      this.mirror = Mirror.NONE;
   }

   public Vec3 applyWithoutOffsetUncentered(Vec3 localVec) {
      Vec3 vec = localVec;
      if (this.mirror != null) {
         vec = VecHelper.mirror(localVec, this.mirror);
      }

      if (this.rotationAxis != null) {
         vec = VecHelper.rotate(vec, (double)this.angle, this.rotationAxis);
      }

      return vec;
   }

   public Vec3 applyWithoutOffset(Vec3 localVec) {
      Vec3 vec = localVec;
      if (this.mirror != null) {
         vec = VecHelper.mirrorCentered(localVec, this.mirror);
      }

      if (this.rotationAxis != null) {
         vec = VecHelper.rotateCentered(vec, (double)this.angle, this.rotationAxis);
      }

      return vec;
   }

   public Vec3 unapplyWithoutOffset(Vec3 globalVec) {
      Vec3 vec = globalVec;
      if (this.rotationAxis != null) {
         vec = VecHelper.rotateCentered(globalVec, (double)(-this.angle), this.rotationAxis);
      }

      if (this.mirror != null) {
         vec = VecHelper.mirrorCentered(vec, this.mirror);
      }

      return vec;
   }

   public Vec3 apply(Vec3 localVec) {
      return this.applyWithoutOffset(localVec).add(Vec3.atLowerCornerOf(this.offset));
   }

   public BlockPos applyWithoutOffset(BlockPos localPos) {
      return BlockPos.containing(this.applyWithoutOffset(VecHelper.getCenterOf(localPos)));
   }

   public BlockPos apply(BlockPos localPos) {
      return this.applyWithoutOffset(localPos).offset(this.offset);
   }

   public BlockPos unapply(BlockPos globalPos) {
      return this.unapplyWithoutOffset(globalPos.subtract(this.offset));
   }

   public BlockPos unapplyWithoutOffset(BlockPos globalPos) {
      return BlockPos.containing(this.unapplyWithoutOffset(VecHelper.getCenterOf(globalPos)));
   }

   public void apply(BlockEntity be) {
      MovedBlockTransformerRegistries.BlockEntityTransformer transformer = MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.get(be.getType());
      if (transformer != null) {
         transformer.transform(be, this);
      } else if (be instanceof TransformableBlockEntity itbe) {
         itbe.transform(be, this);
      }
   }

   public BlockState apply(BlockState state) {
      Block block = state.getBlock();
      MovedBlockTransformerRegistries.BlockTransformer transformer = MovedBlockTransformerRegistries.BLOCK_TRANSFORMERS.get(block);
      if (transformer != null) {
         return transformer.transform(state, this);
      } else if (block instanceof TransformableBlock transformable) {
         return transformable.transform(state, this);
      } else {
         if (this.mirror != null) {
            state = state.mirror(this.mirror);
         }

         if (this.rotationAxis == Axis.Y) {
            if (block instanceof BellBlock) {
               if (state.getValue(BlockStateProperties.BELL_ATTACHMENT) == BellAttachType.DOUBLE_WALL) {
                  state = (BlockState)state.setValue(BlockStateProperties.BELL_ATTACHMENT, BellAttachType.SINGLE_WALL);
               }

               return (BlockState)state.setValue(BellBlock.FACING, this.rotation.rotate((Direction)state.getValue(BellBlock.FACING)));
            } else {
               return state.rotate(this.rotation);
            }
         } else if (block instanceof FaceAttachedHorizontalDirectionalBlock) {
            DirectionProperty facingProperty = FaceAttachedHorizontalDirectionalBlock.FACING;
            EnumProperty<AttachFace> faceProperty = FaceAttachedHorizontalDirectionalBlock.FACE;
            Direction stateFacing = (Direction)state.getValue(facingProperty);
            AttachFace stateFace = (AttachFace)state.getValue(faceProperty);
            boolean z = this.rotationAxis == Axis.Z;
            Direction forcedAxis = z ? Direction.WEST : Direction.SOUTH;
            if (stateFacing.getAxis() == this.rotationAxis && stateFace == AttachFace.WALL) {
               return state;
            } else {
               for (int i = 0; i < this.rotation.ordinal(); i++) {
                  stateFace = (AttachFace)state.getValue(faceProperty);
                  stateFacing = (Direction)state.getValue(facingProperty);
                  boolean b = state.getValue(faceProperty) == AttachFace.CEILING;
                  state = (BlockState)state.setValue(facingProperty, b ? forcedAxis : forcedAxis.getOpposite());
                  if (stateFace != AttachFace.WALL) {
                     state = (BlockState)state.setValue(faceProperty, AttachFace.WALL);
                  } else if (stateFacing.getAxisDirection() == (z ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE)) {
                     state = (BlockState)state.setValue(faceProperty, AttachFace.FLOOR);
                  } else {
                     state = (BlockState)state.setValue(faceProperty, AttachFace.CEILING);
                  }
               }

               return state;
            }
         } else {
            boolean halfTurn = this.rotation == Rotation.CLOCKWISE_180;
            if (block instanceof StairBlock) {
               return this.transformStairs(state, halfTurn);
            } else {
               if (state.hasProperty(BlockStateProperties.FACING)) {
                  state = (BlockState)state.setValue(BlockStateProperties.FACING, this.rotateFacing((Direction)state.getValue(BlockStateProperties.FACING)));
               } else if (state.hasProperty(BlockStateProperties.AXIS)) {
                  state = (BlockState)state.setValue(BlockStateProperties.AXIS, this.rotateAxis((Axis)state.getValue(BlockStateProperties.AXIS)));
               } else if (halfTurn) {
                  if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                     Direction stateFacing = (Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                     if (stateFacing.getAxis() == this.rotationAxis) {
                        return state;
                     }
                  }

                  state = state.rotate(this.rotation);
                  if (state.hasProperty(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE) != SlabType.DOUBLE) {
                     state = (BlockState)state.setValue(SlabBlock.TYPE, state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM ? SlabType.TOP : SlabType.BOTTOM);
                  }
               }

               return state;
            }
         }
      }
   }

   protected BlockState transformStairs(BlockState state, boolean halfTurn) {
      if (((Direction)state.getValue(StairBlock.FACING)).getAxis() != this.rotationAxis) {
         for (int i = 0; i < this.rotation.ordinal(); i++) {
            Direction direction = (Direction)state.getValue(StairBlock.FACING);
            Half half = (Half)state.getValue(StairBlock.HALF);
            if (direction.getAxisDirection() == AxisDirection.POSITIVE ^ half == Half.BOTTOM ^ direction.getAxis() == Axis.Z) {
               state = (BlockState)state.cycle(StairBlock.HALF);
            } else {
               state = (BlockState)state.setValue(StairBlock.FACING, direction.getOpposite());
            }
         }
      } else if (halfTurn) {
         state = (BlockState)state.cycle(StairBlock.HALF);
      }

      return state;
   }

   public Direction mirrorFacing(Direction facing) {
      return this.mirror != null ? this.mirror.mirror(facing) : facing;
   }

   public Axis rotateAxis(Axis axis) {
      Direction facing = Direction.get(AxisDirection.POSITIVE, axis);
      return this.rotateFacing(facing).getAxis();
   }

   public Direction rotateFacing(Direction facing) {
      for (int i = 0; i < this.rotation.ordinal(); i++) {
         facing = facing.getClockWise(this.rotationAxis);
      }

      return facing;
   }
}
