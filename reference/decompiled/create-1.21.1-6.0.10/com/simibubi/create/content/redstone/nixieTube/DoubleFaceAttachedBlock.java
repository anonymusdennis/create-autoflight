package com.simibubi.create.content.redstone.nixieTube;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DoubleFaceAttachedBlock extends HorizontalDirectionalBlock {
   public static final MapCodec<DoubleFaceAttachedBlock> CODEC = simpleCodec(DoubleFaceAttachedBlock::new);
   public static final EnumProperty<DoubleFaceAttachedBlock.DoubleAttachFace> FACE = EnumProperty.create(
      "double_face", DoubleFaceAttachedBlock.DoubleAttachFace.class
   );

   public DoubleFaceAttachedBlock(Properties p_53182_) {
      super(p_53182_);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      for (Direction direction : pContext.getNearestLookingDirections()) {
         BlockState blockstate;
         if (direction.getAxis() == Axis.Y) {
            blockstate = (BlockState)((BlockState)this.defaultBlockState()
                  .setValue(FACE, direction == Direction.UP ? DoubleFaceAttachedBlock.DoubleAttachFace.CEILING : DoubleFaceAttachedBlock.DoubleAttachFace.FLOOR))
               .setValue(FACING, pContext.getHorizontalDirection());
         } else {
            Vec3 n = Vec3.atLowerCornerOf(direction.getClockWise().getNormal());
            DoubleFaceAttachedBlock.DoubleAttachFace face = DoubleFaceAttachedBlock.DoubleAttachFace.WALL;
            if (pContext.getPlayer() != null) {
               Vec3 lookAngle = pContext.getPlayer().getLookAngle();
               if (lookAngle.dot(n) < 0.0) {
                  face = DoubleFaceAttachedBlock.DoubleAttachFace.WALL_REVERSED;
               }
            }

            blockstate = (BlockState)((BlockState)this.defaultBlockState().setValue(FACE, face)).setValue(FACING, direction.getOpposite());
         }

         if (blockstate.canSurvive(pContext.getLevel(), pContext.getClickedPos())) {
            return blockstate;
         }
      }

      return null;
   }

   protected static Direction getConnectedDirection(BlockState pState) {
      switch ((DoubleFaceAttachedBlock.DoubleAttachFace)pState.getValue(FACE)) {
         case FLOOR:
            return Direction.UP;
         case CEILING:
            return Direction.DOWN;
         default:
            return (Direction)pState.getValue(FACING);
      }
   }

   @NotNull
   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   public static enum DoubleAttachFace implements StringRepresentable {
      FLOOR("floor"),
      WALL("wall"),
      WALL_REVERSED("wall_reversed"),
      CEILING("ceiling");

      private final String name;

      private DoubleAttachFace(String p_61311_) {
         this.name = p_61311_;
      }

      public String getSerializedName() {
         return this.name;
      }

      public int xRot() {
         return this == FLOOR ? 0 : (this == CEILING ? 180 : 90);
      }
   }
}
