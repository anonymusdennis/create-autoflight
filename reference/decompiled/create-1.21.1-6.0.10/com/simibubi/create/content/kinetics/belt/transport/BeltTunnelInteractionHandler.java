package com.simibubi.create.content.kinetics.belt.transport;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlockEntity;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.source.AccumulatedItemCountDisplaySource;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BeltTunnelInteractionHandler {
   public static boolean flapTunnelsAndCheckIfStuck(BeltInventory beltInventory, TransportedItemStack current, float nextOffset) {
      int currentSegment = (int)current.beltPosition;
      int upcomingSegment = (int)nextOffset;
      Direction movementFacing = beltInventory.belt.getMovementFacing();
      if (!beltInventory.beltMovementPositive && nextOffset == 0.0F) {
         upcomingSegment = -1;
      }

      if (currentSegment == upcomingSegment) {
         return false;
      } else if (stuckAtTunnel(beltInventory, upcomingSegment, current.stack, movementFacing)) {
         current.beltPosition = (float)currentSegment + (beltInventory.beltMovementPositive ? 0.99F : 0.01F);
         return true;
      } else {
         Level world = beltInventory.belt.getLevel();
         boolean onServer = !world.isClientSide || beltInventory.belt.isVirtual();
         boolean removed = false;
         BeltTunnelBlockEntity nextTunnel = getTunnelOnSegment(beltInventory, upcomingSegment);
         int transferred = current.stack.getCount();
         if (nextTunnel instanceof BrassTunnelBlockEntity brassTunnel) {
            if (brassTunnel.hasDistributionBehaviour()) {
               if (!brassTunnel.canTakeItems()) {
                  return true;
               }

               if (onServer) {
                  brassTunnel.setStackToDistribute(current.stack, movementFacing.getOpposite());
                  current.stack = ItemStack.EMPTY;
                  beltInventory.belt.notifyUpdate();
               }

               removed = true;
            }
         } else if (nextTunnel != null) {
            BlockState blockState = nextTunnel.getBlockState();
            if (current.stack.getCount() > 1
               && AllBlocks.ANDESITE_TUNNEL.has(blockState)
               && BeltTunnelBlock.isJunction(blockState)
               && movementFacing.getAxis() == blockState.getValue(BeltTunnelBlock.HORIZONTAL_AXIS)) {
               for (Direction d : Iterate.horizontalDirections) {
                  if (d.getAxis() != blockState.getValue(BeltTunnelBlock.HORIZONTAL_AXIS) && nextTunnel.flaps.containsKey(d)) {
                     BlockPos outpos = nextTunnel.getBlockPos().below().relative(d);
                     if (!world.isLoaded(outpos)) {
                        return true;
                     }

                     DirectBeltInputBehaviour behaviour = BlockEntityBehaviour.get(world, outpos, DirectBeltInputBehaviour.TYPE);
                     if (behaviour != null && behaviour.canInsertFromSide(d)) {
                        ItemStack toinsert = current.stack.copyWithCount(1);
                        if (!behaviour.handleInsertion(toinsert, d, false).isEmpty()) {
                           return true;
                        }

                        if (onServer) {
                           flapTunnel(beltInventory, upcomingSegment, d, false);
                        }

                        current.stack.shrink(1);
                        beltInventory.belt.notifyUpdate();
                        if (current.stack.getCount() <= 1) {
                           break;
                        }
                     }
                  }
               }
            }
         }

         if (onServer) {
            flapTunnel(beltInventory, currentSegment, movementFacing, false);
            flapTunnel(beltInventory, upcomingSegment, movementFacing.getOpposite(), true);
            if (nextTunnel != null) {
               DisplayLinkBlock.sendToGatherers(
                  world, nextTunnel.getBlockPos(), (dgte, b) -> b.itemReceived(dgte, transferred), AccumulatedItemCountDisplaySource.class
               );
            }
         }

         return removed;
      }
   }

   public static boolean stuckAtTunnel(BeltInventory beltInventory, int offset, ItemStack stack, Direction movementDirection) {
      BeltBlockEntity belt = beltInventory.belt;
      BlockPos pos = BeltHelper.getPositionForOffset(belt, offset).above();
      if (!(belt.getLevel().getBlockState(pos).getBlock() instanceof BrassTunnelBlock)) {
         return false;
      } else {
         BlockEntity be = belt.getLevel().getBlockEntity(pos);
         return be != null && be instanceof BrassTunnelBlockEntity tunnel ? !tunnel.canInsert(movementDirection.getOpposite(), stack) : false;
      }
   }

   public static void flapTunnel(BeltInventory beltInventory, int offset, Direction side, boolean inward) {
      BeltTunnelBlockEntity be = getTunnelOnSegment(beltInventory, offset);
      if (be != null) {
         be.flap(side, inward);
      }
   }

   protected static BeltTunnelBlockEntity getTunnelOnSegment(BeltInventory beltInventory, int offset) {
      BeltBlockEntity belt = beltInventory.belt;
      return belt.getBlockState().getValue(BeltBlock.SLOPE) != BeltSlope.HORIZONTAL
         ? null
         : getTunnelOnPosition(belt.getLevel(), BeltHelper.getPositionForOffset(belt, offset));
   }

   public static BeltTunnelBlockEntity getTunnelOnPosition(Level world, BlockPos pos) {
      pos = pos.above();
      if (!(world.getBlockState(pos).getBlock() instanceof BeltTunnelBlock)) {
         return null;
      } else {
         BlockEntity be = world.getBlockEntity(pos);
         return be != null && be instanceof BeltTunnelBlockEntity ? (BeltTunnelBlockEntity)be : null;
      }
   }
}
