package com.simibubi.create.content.fluids;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.VanillaFluidTargets;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class FluidPropagator {
   public static CreateAdvancement[] getSharedTriggers() {
      return new CreateAdvancement[]{AllAdvancements.WATER_SUPPLY, AllAdvancements.CROSS_STREAMS, AllAdvancements.HONEY_DRAIN};
   }

   public static void propagateChangedPipe(LevelAccessor world, BlockPos pipePos, BlockState pipeState) {
      List<Pair<Integer, BlockPos>> frontier = new ArrayList<>();
      Set<BlockPos> visited = new HashSet<>();
      Set<Pair<PumpBlockEntity, Direction>> discoveredPumps = new HashSet<>();
      frontier.add(Pair.of(0, pipePos));

      while (!frontier.isEmpty()) {
         Pair<Integer, BlockPos> pair = frontier.remove(0);
         BlockPos currentPos = (BlockPos)pair.getSecond();
         if (!visited.contains(currentPos)) {
            visited.add(currentPos);
            BlockState currentState = currentPos.equals(pipePos) ? pipeState : world.getBlockState(currentPos);
            FluidTransportBehaviour pipe = getPipe(world, currentPos);
            if (pipe != null) {
               pipe.wipePressure();

               for (Direction direction : getPipeConnections(currentState, pipe)) {
                  BlockPos target = currentPos.relative(direction);
                  if (!(world instanceof Level l) || l.isLoaded(target)) {
                     BlockEntity blockEntity = world.getBlockEntity(target);
                     BlockState targetState = world.getBlockState(target);
                     if (blockEntity instanceof PumpBlockEntity) {
                        if (targetState.getBlock() instanceof PumpBlock && ((Direction)targetState.getValue(PumpBlock.FACING)).getAxis() == direction.getAxis()
                           )
                         {
                           discoveredPumps.add(Pair.of((PumpBlockEntity)blockEntity, direction.getOpposite()));
                        }
                     } else if (!visited.contains(target)) {
                        FluidTransportBehaviour targetPipe = getPipe(world, target);
                        if (targetPipe != null) {
                           Integer distance = (Integer)pair.getFirst();
                           if ((distance < getPumpRange() || targetPipe.hasAnyPressure()) && targetPipe.canHaveFlowToward(targetState, direction.getOpposite())
                              )
                            {
                              frontier.add(Pair.of(distance + 1, target));
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      discoveredPumps.forEach(pairx -> ((PumpBlockEntity)pairx.getFirst()).updatePipesOnSide((Direction)pairx.getSecond()));
   }

   public static void resetAffectedFluidNetworks(Level world, BlockPos start, Direction side) {
      List<BlockPos> frontier = new ArrayList<>();
      Set<BlockPos> visited = new HashSet<>();
      frontier.add(start);

      while (!frontier.isEmpty()) {
         BlockPos pos = frontier.remove(0);
         if (!visited.contains(pos)) {
            visited.add(pos);
            FluidTransportBehaviour pipe = getPipe(world, pos);
            if (pipe != null) {
               for (Direction d : Iterate.directions) {
                  if (!pos.equals(start) || d == side) {
                     BlockPos target = pos.relative(d);
                     if (!visited.contains(target)) {
                        PipeConnection connection = pipe.getConnection(d);
                        if (connection != null && connection.hasFlow()) {
                           PipeConnection.Flow flow = connection.flow.get();
                           if (flow.inbound) {
                              connection.resetNetwork();
                              frontier.add(target);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static Direction validateNeighbourChange(BlockState state, Level world, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
      if (world.isClientSide) {
         return null;
      } else {
         otherBlock = world.getBlockState(neighborPos).getBlock();
         if (otherBlock instanceof FluidPipeBlock) {
            return null;
         } else if (otherBlock instanceof AxisPipeBlock) {
            return null;
         } else if (otherBlock instanceof PumpBlock) {
            return null;
         } else if (otherBlock instanceof LiquidBlock) {
            return null;
         } else if (getStraightPipeAxis(state) == null && !(state.getBlock() instanceof EncasedPipeBlock)) {
            return null;
         } else {
            for (Direction d : Iterate.directions) {
               if (pos.relative(d).equals(neighborPos)) {
                  return d;
               }
            }

            return null;
         }
      }
   }

   public static FluidTransportBehaviour getPipe(BlockGetter reader, BlockPos pos) {
      return BlockEntityBehaviour.get(reader, pos, FluidTransportBehaviour.TYPE);
   }

   public static boolean isOpenEnd(BlockGetter reader, BlockPos pos, Direction side) {
      BlockPos connectedPos = pos.relative(side);
      BlockState connectedState = reader.getBlockState(connectedPos);
      FluidTransportBehaviour pipe = getPipe(reader, connectedPos);
      if (pipe != null && pipe.canHaveFlowToward(connectedState, side.getOpposite())) {
         return false;
      } else if (PumpBlock.isPump(connectedState) && ((Direction)connectedState.getValue(PumpBlock.FACING)).getAxis() == side.getAxis()) {
         return false;
      } else if (VanillaFluidTargets.canProvideFluidWithoutCapability(connectedState)) {
         return true;
      } else if (BlockHelper.hasBlockSolidSide(connectedState, reader, connectedPos, side.getOpposite())
         && !AllTags.AllBlockTags.FAN_TRANSPARENT.matches(connectedState)) {
         return false;
      } else {
         return hasFluidCapability(reader, connectedPos, side.getOpposite())
            ? false
            : connectedState.canBeReplaced() && connectedState.getDestroySpeed(reader, connectedPos) != -1.0F
               || connectedState.hasProperty(BlockStateProperties.WATERLOGGED);
      }
   }

   public static List<Direction> getPipeConnections(BlockState state, FluidTransportBehaviour pipe) {
      List<Direction> list = new ArrayList<>();

      for (Direction d : Iterate.directions) {
         if (pipe.canHaveFlowToward(state, d)) {
            list.add(d);
         }
      }

      return list;
   }

   public static int getPumpRange() {
      return (Integer)AllConfigs.server().fluids.mechanicalPumpRange.get();
   }

   public static boolean hasFluidCapability(BlockGetter world, BlockPos pos, Direction side) {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      if (blockEntity != null && blockEntity.getLevel() != null) {
         IFluidHandler capability = (IFluidHandler)blockEntity.getLevel().getCapability(FluidHandler.BLOCK, blockEntity.getBlockPos(), side);
         return capability != null;
      } else {
         return false;
      }
   }

   @Nullable
   public static Axis getStraightPipeAxis(BlockState state) {
      if (state.getBlock() instanceof PumpBlock) {
         return ((Direction)state.getValue(PumpBlock.FACING)).getAxis();
      } else if (state.getBlock() instanceof AxisPipeBlock) {
         return (Axis)state.getValue(AxisPipeBlock.AXIS);
      } else if (!FluidPipeBlock.isPipe(state)) {
         return null;
      } else {
         Axis axisFound = null;
         int connections = 0;

         for (Axis axis : Iterate.axes) {
            Direction d1 = Direction.get(AxisDirection.NEGATIVE, axis);
            Direction d2 = Direction.get(AxisDirection.POSITIVE, axis);
            boolean openAt1 = FluidPipeBlock.isOpenAt(state, d1);
            boolean openAt2 = FluidPipeBlock.isOpenAt(state, d2);
            if (openAt1) {
               connections++;
            }

            if (openAt2) {
               connections++;
            }

            if (openAt1 && openAt2) {
               if (axisFound != null) {
                  return null;
               }

               axisFound = axis;
            }
         }

         return connections == 2 ? axisFound : null;
      }
   }
}
