package dev.simulated_team.simulated.util.assembly;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.content.contraptions.chassis.ChassisBlockEntity;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonHeadBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.index.SimBlockMovementChecks;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.service.SimAssemblyService;
import dev.simulated_team.simulated.service.SimConfigService;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.UniqueLinkedList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;

public class SimAssemblyContraption {
   private static final BlockPos[] DIRECTION_OFFSETS = new BlockPos[]{
      new BlockPos(1, 0, 0),
      new BlockPos(-1, 0, 0),
      new BlockPos(0, 1, 0),
      new BlockPos(0, -1, 0),
      new BlockPos(0, 0, 1),
      new BlockPos(0, 0, -1),
      new BlockPos(1, 1, 0),
      new BlockPos(-1, -1, 0),
      new BlockPos(1, -1, 0),
      new BlockPos(-1, 1, 0),
      new BlockPos(1, 0, 1),
      new BlockPos(-1, 0, -1),
      new BlockPos(1, 0, -1),
      new BlockPos(-1, 0, 1),
      new BlockPos(0, 1, 1),
      new BlockPos(0, -1, -1),
      new BlockPos(0, -1, 1),
      new BlockPos(0, 1, -1)
   };
   public final BlockPos anchor;
   public final boolean ignoreEnclosingGlue;
   private final ObjectOpenHashSet<BlockPos> blocks = new ObjectOpenHashSet(4096);
   private final ObjectOpenHashSet<SuperGlueEntity> glueCache = new ObjectOpenHashSet();
   private final ObjectOpenHashSet<HoneyGlueEntity> honeyGlueCache = new ObjectOpenHashSet();

   public SimAssemblyContraption(BlockPos anchor, boolean ignoreEnclosingGlue) {
      this.anchor = anchor;
      this.ignoreEnclosingGlue = ignoreEnclosingGlue;
   }

   public boolean checkAndCacheGlue(LevelAccessor level, BlockPos blockPos, BlockPos offsetDir) {
      BlockPos targetPos = blockPos.offset(offsetDir);
      boolean inHoneyGlue = false;
      boolean containedByAnyHoneyGlue = false;
      ObjectIterator honeyGlueRange = this.honeyGlueCache.iterator();

      while (honeyGlueRange.hasNext()) {
         HoneyGlueEntity honeyGlueEntity = (HoneyGlueEntity)honeyGlueRange.next();
         boolean firstContained = honeyGlueEntity.contains(blockPos);
         boolean targetContained = honeyGlueEntity.contains(targetPos);
         containedByAnyHoneyGlue |= firstContained;
         containedByAnyHoneyGlue |= targetContained;
         if (firstContained && targetContained) {
            inHoneyGlue = true;
         }
      }

      if (containedByAnyHoneyGlue) {
         int honeyGlueRangex = (Integer)SimConfigService.INSTANCE.server().assembly.honeyGlueRange.get();

         for (HoneyGlueEntity honeyGlueEntity : level.getEntitiesOfClass(
            HoneyGlueEntity.class, SuperGlueEntity.span(blockPos, targetPos).inflate((double)honeyGlueRangex)
         )) {
            if ((this.anchor == null || !this.ignoreEnclosingGlue || !honeyGlueEntity.contains(this.anchor))
               && honeyGlueEntity.contains(blockPos)
               && honeyGlueEntity.contains(targetPos)) {
               this.honeyGlueCache.add(honeyGlueEntity);
               inHoneyGlue = true;
            }
         }
      }

      honeyGlueRange = this.glueCache.iterator();

      while (honeyGlueRange.hasNext()) {
         SuperGlueEntity glueEntity = (SuperGlueEntity)honeyGlueRange.next();
         if (glueEntity.contains(blockPos) && glueEntity.contains(targetPos)) {
            return true;
         }
      }

      for (SuperGlueEntity glueEntity : level.getEntitiesOfClass(SuperGlueEntity.class, SuperGlueEntity.span(blockPos, targetPos).inflate(16.0))) {
         if (glueEntity.contains(blockPos) && glueEntity.contains(targetPos)) {
            this.glueCache.add(glueEntity);
            return true;
         }
      }

      return inHoneyGlue;
   }

   public boolean searchMovedStructure(Level level, BlockPos pos) throws AssemblyException {
      addInitialHoneyGlue(level, this, this.anchor, pos, this.ignoreEnclosingGlue);
      Queue<BlockPos> frontier = new UniqueLinkedList();
      Set<BlockPos> visited = new HashSet<>();
      Set<BlockPos> immutableVisited = Collections.unmodifiableSet(visited);
      if (!BlockMovementChecks.isBrittle(level.getBlockState(pos))) {
         frontier.add(pos);
      }

      int maxBlocksMoved = (Integer)SimConfigService.INSTANCE.server().assembly.maxBlocksMoved.get();

      for (int limit = maxBlocksMoved; limit > 0; limit--) {
         if (frontier.isEmpty()) {
            return true;
         }

         if (!this.moveBlock(level, frontier, visited, immutableVisited)) {
            return false;
         }
      }

      throw SimAssemblyException.structureTooLarge();
   }

   protected static void addInitialHoneyGlue(Level level, SimAssemblyContraption contraption, BlockPos anchor, BlockPos pos, boolean ignoreEnclosingGlue) {
      int honeyGlueRange = (Integer)SimConfigService.INSTANCE.server().assembly.honeyGlueRange.get();

      for (HoneyGlueEntity honeyGlueEntity : level.getEntitiesOfClass(HoneyGlueEntity.class, SuperGlueEntity.span(pos, pos).inflate((double)honeyGlueRange))) {
         if (anchor != null
            ? (!ignoreEnclosingGlue || !honeyGlueEntity.contains(anchor)) && (honeyGlueEntity.contains(pos) || honeyGlueEntity.contains(anchor))
            : honeyGlueEntity.contains(pos)) {
            contraption.honeyGlueCache.add(honeyGlueEntity);
         }
      }
   }

   protected boolean moveBlock(Level world, Queue<BlockPos> frontier, Set<BlockPos> visited, Set<BlockPos> immutableVisitedView) throws AssemblyException {
      BlockPos pos = frontier.poll();
      if (pos == null) {
         return false;
      } else {
         visited.add(pos);
         if (world.isOutsideBuildHeight(pos)) {
            return true;
         } else if (!world.isLoaded(pos)) {
            throw AssemblyException.unloadedChunk(pos);
         } else if (this.isAnchoringBlockAt(pos)) {
            return true;
         } else {
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
               return true;
            } else if (!this.movementAllowed(state, world, pos)) {
               throw AssemblyException.unmovableBlock(pos, state);
            } else if (state.getBlock() instanceof AbstractChassisBlock && !this.moveChassis(world, pos, null, frontier, visited)) {
               return false;
            } else {
               if (SimBlocks.SWIVEL_BEARING.has(state)) {
                  this.moveSwivelBearing(world, pos, frontier, visited, state);
               }

               if (world.getBlockEntity(pos) instanceof ChainConveyorBlockEntity ccbe) {
                  ccbe.notifyConnectedToValidate();
               }

               if (state.hasProperty(ChestBlock.TYPE) && state.hasProperty(ChestBlock.FACING) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                  Direction offset = ChestBlock.getConnectedDirection(state);
                  BlockPos attached = pos.relative(offset);
                  if (!visited.contains(attached)) {
                     frontier.add(attached);
                  }
               }

               if (state.getBlock() instanceof AbstractBogeyBlock<?> bogey) {
                  for (Direction d : bogey.getStickySurfaces(world, pos, state)) {
                     if (!visited.contains(pos.relative(d))) {
                        frontier.add(pos.relative(d));
                     }
                  }
               }

               BlockPos posDown = pos.below();
               BlockState stateBelow = world.getBlockState(posDown);
               if (!visited.contains(posDown) && AllBlocks.CART_ASSEMBLER.has(stateBelow)) {
                  frontier.add(posDown);
               }

               SimBlockMovementChecks.addAdditionalBlocks(state, world, pos, frontier, immutableVisitedView);

               for (BlockPos offsetDirection : DIRECTION_OFFSETS) {
                  int absTotal = Math.abs(offsetDirection.getX()) + Math.abs(offsetDirection.getY()) + Math.abs(offsetDirection.getZ());
                  Direction offsetDirectionNullable = absTotal == 1
                     ? Direction.fromDelta(offsetDirection.getX(), offsetDirection.getY(), offsetDirection.getZ())
                     : null;
                  BlockPos offsetPos = pos.offset(offsetDirection);
                  BlockState blockState = world.getBlockState(offsetPos);
                  if (!this.isAnchoringBlockAt(offsetPos) && this.movementAllowed(blockState, world, offsetPos)) {
                     boolean wasVisited = visited.contains(offsetPos);
                     boolean faceHasGlue = this.checkAndCacheGlue(world, pos, offsetDirection);
                     boolean blockAttachedTowardsFace = offsetDirectionNullable != null
                        && BlockMovementChecks.isBlockAttachedTowards(blockState, world, offsetPos, offsetDirectionNullable.getOpposite());
                     blockAttachedTowardsFace |= SimBlockMovementChecks.checkIsBlockAttachedTowards(blockState, world, offsetPos, offsetDirection.multiply(-1));
                     boolean brittle = BlockMovementChecks.isBrittle(blockState);
                     boolean canStick = !brittle
                        && SimAssemblyService.INSTANCE.canStickTo(state, blockState)
                        && SimAssemblyService.INSTANCE.canStickTo(blockState, state);
                     if (canStick) {
                        if (state.getPistonPushReaction() == PushReaction.PUSH_ONLY || blockState.getPistonPushReaction() == PushReaction.PUSH_ONLY) {
                           canStick = false;
                        }

                        if (offsetDirectionNullable != null) {
                           if (BlockMovementChecks.isNotSupportive(state, offsetDirectionNullable)) {
                              canStick = false;
                           }

                           if (BlockMovementChecks.isNotSupportive(blockState, offsetDirectionNullable.getOpposite())) {
                              canStick = false;
                           }
                        }
                     }

                     if (!wasVisited && (canStick || blockAttachedTowardsFace || faceHasGlue)) {
                        frontier.add(offsetPos);
                     }
                  }
               }

               this.blocks.add(pos);
               if (this.blocks.size() <= (Integer)SimConfigService.INSTANCE.server().assembly.maxBlocksMoved.get()) {
                  return true;
               } else {
                  throw SimAssemblyException.structureTooLarge();
               }
            }
         }
      }
   }

   private void moveSwivelBearing(Level level, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
      Direction facing = (Direction)state.getValue(SwivelBearingBlock.FACING);
      BlockPos attachPos = pos.relative(facing);
      addInitialHoneyGlue(level, this, pos, attachPos, true);
      frontier.add(attachPos);
   }

   protected void movePistonHead(Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
      Direction direction = (Direction)state.getValue(MechanicalPistonHeadBlock.FACING);
      BlockPos offset = pos.relative(direction.getOpposite());
      if (!visited.contains(offset)) {
         BlockState blockState = world.getBlockState(offset);
         if (MechanicalPistonBlock.isExtensionPole(blockState)
            && ((Direction)blockState.getValue(PistonExtensionPoleBlock.FACING)).getAxis() == direction.getAxis()) {
            frontier.add(offset);
         }

         if (blockState.getBlock() instanceof MechanicalPistonBlock) {
            Direction pistonFacing = (Direction)blockState.getValue(MechanicalPistonBlock.FACING);
            if (pistonFacing == direction && blockState.getValue(MechanicalPistonBlock.STATE) == PistonState.EXTENDED) {
               frontier.add(offset);
            }
         }
      }

      if (state.getValue(MechanicalPistonHeadBlock.TYPE) == PistonType.STICKY) {
         BlockPos attached = pos.relative(direction);
         if (!visited.contains(attached)) {
            frontier.add(attached);
         }
      }
   }

   protected void movePistonPole(Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
      for (Direction d : Iterate.directionsInAxis(((Direction)state.getValue(PistonExtensionPoleBlock.FACING)).getAxis())) {
         BlockPos offset = pos.relative(d);
         if (!visited.contains(offset)) {
            BlockState blockState = world.getBlockState(offset);
            if (MechanicalPistonBlock.isExtensionPole(blockState) && ((Direction)blockState.getValue(PistonExtensionPoleBlock.FACING)).getAxis() == d.getAxis()
               )
             {
               frontier.add(offset);
            }

            if (MechanicalPistonBlock.isPistonHead(blockState) && ((Direction)blockState.getValue(MechanicalPistonHeadBlock.FACING)).getAxis() == d.getAxis()) {
               frontier.add(offset);
            }

            if (blockState.getBlock() instanceof MechanicalPistonBlock) {
               Direction pistonFacing = (Direction)blockState.getValue(MechanicalPistonBlock.FACING);
               if (pistonFacing == d || pistonFacing == d.getOpposite() && blockState.getValue(MechanicalPistonBlock.STATE) == PistonState.EXTENDED) {
                  frontier.add(offset);
               }
            }
         }
      }
   }

   protected void moveGantryPinion(Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
      BlockPos offset = pos.relative((Direction)state.getValue(GantryCarriageBlock.FACING));
      if (!visited.contains(offset)) {
         frontier.add(offset);
      }

      Axis rotationAxis = ((IRotate)state.getBlock()).getRotationAxis(state);

      for (Direction d : Iterate.directionsInAxis(rotationAxis)) {
         offset = pos.relative(d);
         BlockState offsetState = world.getBlockState(offset);
         if (AllBlocks.GANTRY_SHAFT.has(offsetState)
            && ((Direction)offsetState.getValue(GantryShaftBlock.FACING)).getAxis() == d.getAxis()
            && !visited.contains(offset)) {
            frontier.add(offset);
         }
      }
   }

   protected void moveGantryShaft(Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) {
      for (Direction d : Iterate.directions) {
         BlockPos offset = pos.relative(d);
         if (!visited.contains(offset)) {
            BlockState offsetState = world.getBlockState(offset);
            Direction facing = (Direction)state.getValue(GantryShaftBlock.FACING);
            if (d.getAxis() == facing.getAxis() && AllBlocks.GANTRY_SHAFT.has(offsetState) && offsetState.getValue(GantryShaftBlock.FACING) == facing) {
               frontier.add(offset);
            } else if (AllBlocks.GANTRY_CARRIAGE.has(offsetState) && offsetState.getValue(GantryCarriageBlock.FACING) == d) {
               frontier.add(offset);
            }
         }
      }
   }

   private boolean moveMechanicalPiston(Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited, BlockState state) throws AssemblyException {
      Direction direction = (Direction)state.getValue(MechanicalPistonBlock.FACING);
      PistonState pistonState = (PistonState)state.getValue(MechanicalPistonBlock.STATE);
      if (pistonState == PistonState.MOVING) {
         return false;
      } else {
         BlockPos offset = pos.relative(direction.getOpposite());
         if (!visited.contains(offset)) {
            BlockState poleState = world.getBlockState(offset);
            if (AllBlocks.PISTON_EXTENSION_POLE.has(poleState)
               && ((Direction)poleState.getValue(PistonExtensionPoleBlock.FACING)).getAxis() == direction.getAxis()) {
               frontier.add(offset);
            }
         }

         if (pistonState == PistonState.EXTENDED || MechanicalPistonBlock.isStickyPiston(state)) {
            offset = pos.relative(direction);
            if (!visited.contains(offset)) {
               frontier.add(offset);
            }
         }

         return true;
      }
   }

   private boolean moveChassis(Level world, BlockPos pos, Direction movementDirection, Queue<BlockPos> frontier, Set<BlockPos> visited) {
      if (world.getBlockEntity(pos) instanceof ChassisBlockEntity chassis) {
         chassis.addAttachedChasses(frontier, visited);
         List<BlockPos> includedBlockPositions = chassis.getIncludedBlockPositions(movementDirection, false);
         if (includedBlockPositions == null) {
            return false;
         } else {
            for (BlockPos blockPos : includedBlockPositions) {
               if (!visited.contains(blockPos)) {
                  frontier.add(blockPos);
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   protected boolean movementAllowed(BlockState state, Level world, BlockPos pos) {
      return state.getDestroySpeed(world, pos) != -1.0F && !state.is(SimTags.Blocks.NON_MOVABLE);
   }

   protected boolean isAnchoringBlockAt(BlockPos pos) {
      return pos.equals(this.anchor);
   }

   public Collection<SuperGlueEntity> getGlues() {
      return this.glueCache;
   }

   public Collection<HoneyGlueEntity> getHoneyGlues() {
      return this.honeyGlueCache;
   }

   public Collection<BlockPos> getBlocks() {
      return this.blocks;
   }
}
