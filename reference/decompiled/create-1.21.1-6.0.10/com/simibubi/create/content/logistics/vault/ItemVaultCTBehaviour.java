package com.simibubi.create.content.logistics.vault;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ItemVaultCTBehaviour extends ConnectedTextureBehaviour.Base {
   @Override
   public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
      Axis vaultBlockAxis = ItemVaultBlock.getVaultBlockAxis(state);
      boolean small = !ItemVaultBlock.isLarge(state);
      if (vaultBlockAxis == null) {
         return null;
      } else if (direction.getAxis() == vaultBlockAxis) {
         return (CTSpriteShiftEntry)AllSpriteShifts.VAULT_FRONT.get(small);
      } else if (direction == Direction.UP) {
         return (CTSpriteShiftEntry)AllSpriteShifts.VAULT_TOP.get(small);
      } else {
         return direction == Direction.DOWN
            ? (CTSpriteShiftEntry)AllSpriteShifts.VAULT_BOTTOM.get(small)
            : (CTSpriteShiftEntry)AllSpriteShifts.VAULT_SIDE.get(small);
      }
   }

   @Override
   protected Direction getUpDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
      Axis vaultBlockAxis = ItemVaultBlock.getVaultBlockAxis(state);
      boolean alongX = vaultBlockAxis == Axis.X;
      if (face.getAxis().isVertical() && alongX) {
         return super.getUpDirection(reader, pos, state, face).getClockWise();
      } else {
         return face.getAxis() != vaultBlockAxis && !face.getAxis().isVertical()
            ? Direction.fromAxisAndDirection(vaultBlockAxis, alongX ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE)
            : super.getUpDirection(reader, pos, state, face);
      }
   }

   @Override
   protected Direction getRightDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
      Axis vaultBlockAxis = ItemVaultBlock.getVaultBlockAxis(state);
      if (face.getAxis().isVertical() && vaultBlockAxis == Axis.X) {
         return super.getRightDirection(reader, pos, state, face).getClockWise();
      } else {
         return face.getAxis() != vaultBlockAxis && !face.getAxis().isVertical()
            ? Direction.fromAxisAndDirection(Axis.Y, face.getAxisDirection())
            : super.getRightDirection(reader, pos, state, face);
      }
   }

   @Override
   public boolean buildContextForOccludedDirections() {
      return super.buildContextForOccludedDirections();
   }

   @Override
   public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
      return state == other && ConnectivityHandler.isConnected(reader, pos, otherPos);
   }
}
