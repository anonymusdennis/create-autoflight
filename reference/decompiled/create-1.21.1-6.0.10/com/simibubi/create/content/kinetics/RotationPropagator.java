package com.simibubi.create.content.kinetics;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalShaftHalvesBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveBlock;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.LinkedList;
import java.util.List;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RotationPropagator {
   private static final int MAX_FLICKER_SCORE = 128;

   private static float getRotationSpeedModifier(KineticBlockEntity from, KineticBlockEntity to) {
      BlockState stateFrom = from.getBlockState();
      BlockState stateTo = to.getBlockState();
      Block fromBlock = stateFrom.getBlock();
      Block toBlock = stateTo.getBlock();
      if (fromBlock instanceof IRotate definitionFrom && toBlock instanceof IRotate definitionTo) {
         BlockPos diff = to.getBlockPos().subtract(from.getBlockPos());
         Direction direction = Direction.getNearest((float)diff.getX(), (float)diff.getY(), (float)diff.getZ());
         Level world = from.getLevel();
         boolean alignedAxes = true;

         for (Axis axis : Axis.values()) {
            if (axis != direction.getAxis() && axis.choose(diff.getX(), diff.getY(), diff.getZ()) != 0) {
               alignedAxes = false;
            }
         }

         boolean connectedByAxis = alignedAxes
            && definitionFrom.hasShaftTowards(world, from.getBlockPos(), stateFrom, direction)
            && definitionTo.hasShaftTowards(world, to.getBlockPos(), stateTo, direction.getOpposite());
         boolean connectedByGears = ICogWheel.isSmallCog(stateFrom) && ICogWheel.isSmallCog(stateTo);
         float custom = from.propagateRotationTo(to, stateFrom, stateTo, diff, connectedByAxis, connectedByGears);
         if (custom != 0.0F) {
            return custom;
         }

         if (connectedByAxis) {
            float axisModifier = getAxisModifier(to, direction.getOpposite());
            if (axisModifier != 0.0F) {
               axisModifier = 1.0F / axisModifier;
            }

            return getAxisModifier(from, direction) * axisModifier;
         }

         if (fromBlock instanceof ChainDriveBlock && toBlock instanceof ChainDriveBlock) {
            boolean connected = ChainDriveBlock.areBlocksConnected(stateFrom, stateTo, direction);
            return connected ? ChainDriveBlock.getRotationSpeedModifier(from, to) : 0.0F;
         }

         if (isLargeToLargeGear(stateFrom, stateTo, diff)) {
            Axis sourceAxis = (Axis)stateFrom.getValue(BlockStateProperties.AXIS);
            Axis targetAxis = (Axis)stateTo.getValue(BlockStateProperties.AXIS);
            int sourceAxisDiff = sourceAxis.choose(diff.getX(), diff.getY(), diff.getZ());
            int targetAxisDiff = targetAxis.choose(diff.getX(), diff.getY(), diff.getZ());
            return sourceAxisDiff > 0 ^ targetAxisDiff > 0 ? -1.0F : 1.0F;
         }

         if (ICogWheel.isLargeCog(stateFrom) && ICogWheel.isSmallCog(stateTo) && isLargeToSmallCog(stateFrom, stateTo, definitionTo, diff)) {
            return -2.0F;
         }

         if (ICogWheel.isLargeCog(stateTo) && ICogWheel.isSmallCog(stateFrom) && isLargeToSmallCog(stateTo, stateFrom, definitionFrom, diff)) {
            return -0.5F;
         }

         if (connectedByGears) {
            if (diff.distManhattan(BlockPos.ZERO) != 1) {
               return 0.0F;
            }

            if (ICogWheel.isLargeCog(stateTo)) {
               return 0.0F;
            }

            if (direction.getAxis() == definitionFrom.getRotationAxis(stateFrom)) {
               return 0.0F;
            }

            if (definitionFrom.getRotationAxis(stateFrom) == definitionTo.getRotationAxis(stateTo)) {
               return -1.0F;
            }
         }

         return 0.0F;
      }

      return 0.0F;
   }

   private static float getConveyedSpeed(KineticBlockEntity from, KineticBlockEntity to) {
      BlockState stateFrom = from.getBlockState();
      BlockState stateTo = to.getBlockState();
      if (isLargeCogToSpeedController(stateFrom, stateTo, to.getBlockPos().subtract(from.getBlockPos()))) {
         return SpeedControllerBlockEntity.getConveyedSpeed(from, to, true);
      } else if (isLargeCogToSpeedController(stateTo, stateFrom, from.getBlockPos().subtract(to.getBlockPos()))) {
         return SpeedControllerBlockEntity.getConveyedSpeed(to, from, false);
      } else {
         float rotationSpeedModifier = getRotationSpeedModifier(from, to);
         return from.getTheoreticalSpeed() * rotationSpeedModifier;
      }
   }

   private static boolean isLargeToLargeGear(BlockState from, BlockState to, BlockPos diff) {
      if (ICogWheel.isLargeCog(from) && ICogWheel.isLargeCog(to)) {
         Axis fromAxis = (Axis)from.getValue(BlockStateProperties.AXIS);
         Axis toAxis = (Axis)to.getValue(BlockStateProperties.AXIS);
         if (fromAxis == toAxis) {
            return false;
         } else {
            for (Axis axis : Axis.values()) {
               int axisDiff = axis.choose(diff.getX(), diff.getY(), diff.getZ());
               if (axis != fromAxis && axis != toAxis) {
                  if (axisDiff != 0) {
                     return false;
                  }
               } else if (axisDiff == 0) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static float getAxisModifier(KineticBlockEntity be, Direction direction) {
      if ((be.hasSource() || be.isSource()) && be instanceof DirectionalShaftHalvesBlockEntity) {
         Direction source = ((DirectionalShaftHalvesBlockEntity)be).getSourceFacing();
         if (be instanceof GearboxBlockEntity) {
            return direction.getAxis() == source.getAxis()
               ? (direction == source ? 1.0F : -1.0F)
               : (direction.getAxisDirection() == source.getAxisDirection() ? -1.0F : 1.0F);
         } else {
            return be instanceof SplitShaftBlockEntity ? ((SplitShaftBlockEntity)be).getRotationSpeedModifier(direction) : 1.0F;
         }
      } else {
         return 1.0F;
      }
   }

   private static boolean isLargeToSmallCog(BlockState from, BlockState to, IRotate defTo, BlockPos diff) {
      Axis axisFrom = (Axis)from.getValue(BlockStateProperties.AXIS);
      if (axisFrom != defTo.getRotationAxis(to)) {
         return false;
      } else if (axisFrom.choose(diff.getX(), diff.getY(), diff.getZ()) != 0) {
         return false;
      } else {
         for (Axis axis : Axis.values()) {
            if (axis != axisFrom && Math.abs(axis.choose(diff.getX(), diff.getY(), diff.getZ())) != 1) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean isLargeCogToSpeedController(BlockState from, BlockState to, BlockPos diff) {
      if (!ICogWheel.isLargeCog(from) || !AllBlocks.ROTATION_SPEED_CONTROLLER.has(to)) {
         return false;
      } else if (!diff.equals(BlockPos.ZERO.below())) {
         return false;
      } else {
         Axis axis = (Axis)from.getValue(CogWheelBlock.AXIS);
         return axis.isVertical() ? false : to.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) != axis;
      }
   }

   public static void handleAdded(Level worldIn, BlockPos pos, KineticBlockEntity addedTE) {
      if (!worldIn.isClientSide) {
         if (worldIn.isLoaded(pos)) {
            propagateNewSource(addedTE);
         }
      }
   }

   private static void propagateNewSource(KineticBlockEntity currentTE) {
      BlockPos pos = currentTE.getBlockPos();
      Level world = currentTE.getLevel();

      for (KineticBlockEntity neighbourTE : getConnectedNeighbours(currentTE)) {
         float speedOfCurrent = currentTE.getTheoreticalSpeed();
         float speedOfNeighbour = neighbourTE.getTheoreticalSpeed();
         float newSpeed = getConveyedSpeed(currentTE, neighbourTE);
         float oppositeSpeed = getConveyedSpeed(neighbourTE, currentTE);
         if (newSpeed != 0.0F || oppositeSpeed != 0.0F) {
            boolean incompatible = Math.signum(newSpeed) != Math.signum(speedOfNeighbour) && newSpeed != 0.0F && speedOfNeighbour != 0.0F;
            boolean tooFast = Math.abs(newSpeed) > (float)((Integer)AllConfigs.server().kinetics.maxRotationSpeed.get()).intValue()
               || Math.abs(oppositeSpeed) > (float)((Integer)AllConfigs.server().kinetics.maxRotationSpeed.get()).intValue();
            boolean speedChangedTooOften = currentTE.getFlickerScore() > 128;
            if (tooFast || speedChangedTooOften) {
               world.destroyBlock(pos, true);
               return;
            }

            if (incompatible) {
               world.destroyBlock(pos, true);
               return;
            }

            if (Math.abs(oppositeSpeed) > Math.abs(speedOfCurrent)) {
               float prevSpeed = currentTE.getSpeed();
               currentTE.setSource(neighbourTE.getBlockPos());
               currentTE.setSpeed(getConveyedSpeed(neighbourTE, currentTE));
               currentTE.onSpeedChanged(prevSpeed);
               currentTE.sendData();
               propagateNewSource(currentTE);
               return;
            }

            if (!(Math.abs(newSpeed) >= Math.abs(speedOfNeighbour))) {
               if (!(Math.abs(neighbourTE.getTheoreticalSpeed() - newSpeed) <= 1.0E-4F)) {
                  float prevSpeed = neighbourTE.getSpeed();
                  neighbourTE.setSpeed(newSpeed);
                  neighbourTE.setSource(currentTE.getBlockPos());
                  neighbourTE.onSpeedChanged(prevSpeed);
                  neighbourTE.sendData();
                  propagateNewSource(neighbourTE);
               }
            } else if (currentTE.hasNetwork() && !currentTE.network.equals(neighbourTE.network)) {
               if (currentTE.hasSource() && currentTE.source.equals(neighbourTE.getBlockPos())) {
                  currentTE.removeSource();
               }

               float prevSpeed = neighbourTE.getSpeed();
               neighbourTE.setSource(currentTE.getBlockPos());
               neighbourTE.setSpeed(getConveyedSpeed(currentTE, neighbourTE));
               neighbourTE.onSpeedChanged(prevSpeed);
               neighbourTE.sendData();
               propagateNewSource(neighbourTE);
            } else {
               float epsilon = Math.abs(speedOfNeighbour) / 256.0F / 256.0F;
               if (Math.abs(newSpeed) > Math.abs(speedOfNeighbour) + epsilon) {
                  world.destroyBlock(pos, true);
               }
            }
         }
      }
   }

   public static void handleRemoved(Level worldIn, BlockPos pos, KineticBlockEntity removedBE) {
      if (!worldIn.isClientSide) {
         if (removedBE != null) {
            if (removedBE.getTheoreticalSpeed() != 0.0F) {
               for (BlockPos neighbourPos : getPotentialNeighbourLocations(removedBE)) {
                  BlockState neighbourState = worldIn.getBlockState(neighbourPos);
                  if (neighbourState.getBlock() instanceof IRotate) {
                     BlockEntity blockEntity = worldIn.getBlockEntity(neighbourPos);
                     if (blockEntity instanceof KineticBlockEntity) {
                        KineticBlockEntity neighbourBE = (KineticBlockEntity)blockEntity;
                        if (neighbourBE.hasSource() && neighbourBE.source.equals(pos)) {
                           propagateMissingSource(neighbourBE);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void propagateMissingSource(KineticBlockEntity updateTE) {
      Level world = updateTE.getLevel();
      List<KineticBlockEntity> potentialNewSources = new LinkedList<>();
      List<BlockPos> frontier = new LinkedList<>();
      frontier.add(updateTE.getBlockPos());
      BlockPos missingSource = updateTE.hasSource() ? updateTE.source : null;

      while (!frontier.isEmpty()) {
         BlockPos pos = frontier.remove(0);
         BlockEntity blockEntity = world.getBlockEntity(pos);
         if (blockEntity instanceof KineticBlockEntity) {
            KineticBlockEntity currentBE = (KineticBlockEntity)blockEntity;
            currentBE.removeSource();
            currentBE.sendData();

            for (KineticBlockEntity neighbourBE : getConnectedNeighbours(currentBE)) {
               if (!neighbourBE.getBlockPos().equals(missingSource) && neighbourBE.hasSource()) {
                  if (!neighbourBE.source.equals(pos)) {
                     potentialNewSources.add(neighbourBE);
                  } else {
                     if (neighbourBE.isSource()) {
                        potentialNewSources.add(neighbourBE);
                     }

                     frontier.add(neighbourBE.getBlockPos());
                  }
               }
            }
         }
      }

      for (KineticBlockEntity newSource : potentialNewSources) {
         if (newSource.hasSource() || newSource.isSource()) {
            propagateNewSource(newSource);
            return;
         }
      }
   }

   private static KineticBlockEntity findConnectedNeighbour(KineticBlockEntity currentTE, BlockPos neighbourPos) {
      BlockState neighbourState = currentTE.getLevel().getBlockState(neighbourPos);
      if (!(neighbourState.getBlock() instanceof IRotate)) {
         return null;
      } else if (!neighbourState.hasBlockEntity()) {
         return null;
      } else if (currentTE.getLevel().getBlockEntity(neighbourPos) instanceof KineticBlockEntity neighbourKBE) {
         if (!(neighbourKBE.getBlockState().getBlock() instanceof IRotate)) {
            return null;
         } else {
            return !isConnected(currentTE, neighbourKBE) && !isConnected(neighbourKBE, currentTE) ? null : neighbourKBE;
         }
      } else {
         return null;
      }
   }

   public static boolean isConnected(KineticBlockEntity from, KineticBlockEntity to) {
      BlockState stateFrom = from.getBlockState();
      BlockState stateTo = to.getBlockState();
      return isLargeCogToSpeedController(stateFrom, stateTo, to.getBlockPos().subtract(from.getBlockPos()))
         || getRotationSpeedModifier(from, to) != 0.0F
         || from.isCustomConnection(to, stateFrom, stateTo);
   }

   private static List<KineticBlockEntity> getConnectedNeighbours(KineticBlockEntity be) {
      List<KineticBlockEntity> neighbours = new LinkedList<>();

      for (BlockPos neighbourPos : getPotentialNeighbourLocations(be)) {
         KineticBlockEntity neighbourBE = findConnectedNeighbour(be, neighbourPos);
         if (neighbourBE != null) {
            neighbours.add(neighbourBE);
         }
      }

      return neighbours;
   }

   private static List<BlockPos> getPotentialNeighbourLocations(KineticBlockEntity be) {
      List<BlockPos> neighbours = new LinkedList<>();
      BlockPos blockPos = be.getBlockPos();
      Level level = be.getLevel();
      if (!level.isLoaded(blockPos)) {
         return neighbours;
      } else {
         for (Direction facing : Iterate.directions) {
            BlockPos relative = blockPos.relative(facing);
            if (level.isLoaded(relative)) {
               neighbours.add(relative);
            }
         }

         BlockState blockState = be.getBlockState();
         return blockState.getBlock() instanceof IRotate block ? be.addPropagationLocations(block, blockState, neighbours) : neighbours;
      }
   }
}
