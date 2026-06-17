package dev.simulated_team.simulated.index;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.SailBlock;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlock;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.contraptions.chassis.StickerBlock;
import com.simibubi.create.content.contraptions.gantry.GantryCarriageBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonHeadBlock;
import com.simibubi.create.content.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.content.contraptions.pulley.PulleyBlock;
import com.simibubi.create.content.contraptions.pulley.PulleyBlock.MagnetBlock;
import com.simibubi.create.content.contraptions.pulley.PulleyBlock.RopeBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import dev.simulated_team.simulated.content.blocks.symmetric_sail.SymmetricSailBlock;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import org.jetbrains.annotations.ApiStatus.Internal;

public class SimBlockMovementChecks {
   private static final List<BlockPos> TEMP_DEFAULT_POSITIONS = new ArrayList<>();
   private static final ObjectList<SimBlockMovementChecks.AdditionalBlocks> ADDITIONAL_BLOCK_REGISTRATIONS = new ObjectArrayList();
   private static final ObjectList<SimBlockMovementChecks.AttachedCheck> ATTACHED_CHECKS = new ObjectArrayList();

   private static CheckResult registerDefaultBlockAttachedTowards(BlockState state, Level world, BlockPos pos, Direction direction) {
      Block block = state.getBlock();
      BlockState relativeState = world.getBlockState(pos.relative(direction));
      Block relativeBlock = relativeState.getBlock();
      Objects.requireNonNull(block);

      return switch (block) {
         case SymmetricSailBlock ignored when relativeBlock instanceof SailBlock -> CheckResult.FAIL;
         case SailBlock ignoredx when relativeBlock instanceof SymmetricSailBlock -> CheckResult.FAIL;
         case SymmetricSailBlock ignoredxx -> direction.getAxis() == state.getValue(SymmetricSailBlock.AXIS) ? CheckResult.FAIL : CheckResult.SUCCESS;
         case SpringBlock ignoredxxx -> direction.getOpposite() == state.getValue(SpringBlock.FACING) ? CheckResult.SUCCESS : CheckResult.FAIL;
         default -> CheckResult.PASS;
      };
   }

   private static synchronized Iterable<BlockPos> registerDefaultAdditionalBlocks(BlockState state, Level level, BlockPos pos, Set<BlockPos> visited) {
      TEMP_DEFAULT_POSITIONS.clear();
      Block block = state.getBlock();
      Objects.requireNonNull(block);
      switch (block) {
         case BeltBlock ignored:
            BlockPos nextPos = BeltBlock.nextSegmentPosition(state, pos, true);
            if (nextPos != null && !visited.contains(nextPos)) {
               TEMP_DEFAULT_POSITIONS.add(nextPos);
            }

            BlockPos prevPos = BeltBlock.nextSegmentPosition(state, pos, false);
            if (prevPos != null && !visited.contains(prevPos)) {
               TEMP_DEFAULT_POSITIONS.add(prevPos);
            }
            break;
         case PulleyBlock ignoredx:
            int limit = (Integer)AllConfigs.server().kinetics.maxRopeLength.get();
            BlockPos ropePos = pos;

            while (limit-- >= 0) {
               ropePos = ropePos.below();
               if (!level.isLoaded(ropePos)) {
                  return TEMP_DEFAULT_POSITIONS;
               }

               BlockState ropeState = level.getBlockState(ropePos);
               Block ropeBlock = ropeState.getBlock();
               if (!(ropeBlock instanceof RopeBlock) && !(ropeBlock instanceof MagnetBlock)) {
                  if (!visited.contains(ropePos)) {
                     TEMP_DEFAULT_POSITIONS.add(ropePos);
                  }

                  return TEMP_DEFAULT_POSITIONS;
               }

               if (!visited.contains(ropePos)) {
                  TEMP_DEFAULT_POSITIONS.add(ropePos);
               }
            }
            break;
         case WindmillBearingBlock ignoredxx:
            if (level.getBlockEntity(pos) instanceof WindmillBearingBlockEntity wwbe) {
               wwbe.disassembleForMovement();
            }

            BlockPos relativex = pos.relative((Direction)state.getValue(BearingBlock.FACING));
            if (!visited.contains(relativex)) {
               TEMP_DEFAULT_POSITIONS.add(relativex);
            }
            break;
         case BearingBlock ignoredxxx:
            BlockPos relative = pos.relative((Direction)state.getValue(BearingBlock.FACING));
            if (!visited.contains(relative)) {
               TEMP_DEFAULT_POSITIONS.add(relative);
            }
            break;
         case MechanicalPistonBlock ignoredxxxx:
            PistonState s = (PistonState)state.getValue(MechanicalPistonBlock.STATE);
            if (s != PistonState.MOVING) {
               Direction dir = (Direction)state.getValue(MechanicalPistonBlock.FACING);
               BlockPos reverseOffset = pos.relative(dir.getOpposite());
               if (!visited.contains(reverseOffset)) {
                  BlockState poleState = level.getBlockState(reverseOffset);
                  if (poleState.getBlock() instanceof PistonExtensionPoleBlock
                     && ((Direction)poleState.getValue(PistonExtensionPoleBlock.FACING)).getAxis() == dir.getAxis()) {
                     TEMP_DEFAULT_POSITIONS.add(reverseOffset);
                  }
               }

               if (s == PistonState.EXTENDED || MechanicalPistonBlock.isStickyPiston(state)) {
                  reverseOffset = pos.relative(dir);
                  if (!visited.contains(reverseOffset)) {
                     TEMP_DEFAULT_POSITIONS.add(reverseOffset);
                  }
               }
            }
            break;
         case PistonExtensionPoleBlock ignoredxxxxx:
            for (Direction dxx : Iterate.directionsInAxis(((Direction)state.getValue(PistonExtensionPoleBlock.FACING)).getAxis())) {
               BlockPos offsetx = pos.relative(dxx);
               if (!visited.contains(offsetx)) {
                  BlockState blockStatex = level.getBlockState(offsetx);
                  if (MechanicalPistonBlock.isExtensionPole(blockStatex)
                     && ((Direction)blockStatex.getValue(PistonExtensionPoleBlock.FACING)).getAxis() == dxx.getAxis()) {
                     TEMP_DEFAULT_POSITIONS.add(offsetx);
                  }

                  if (MechanicalPistonBlock.isPistonHead(blockStatex)
                     && ((Direction)blockStatex.getValue(MechanicalPistonHeadBlock.FACING)).getAxis() == dxx.getAxis()) {
                     TEMP_DEFAULT_POSITIONS.add(offsetx);
                  }

                  if (blockStatex.getBlock() instanceof MechanicalPistonBlock) {
                     Direction pistonFacing = (Direction)blockStatex.getValue(MechanicalPistonBlock.FACING);
                     if (pistonFacing == dxx || pistonFacing == dxx.getOpposite() && blockStatex.getValue(MechanicalPistonBlock.STATE) == PistonState.EXTENDED) {
                        TEMP_DEFAULT_POSITIONS.add(offsetx);
                     }
                  }
               }
            }
            break;
         case MechanicalPistonHeadBlock ignore:
            Direction direction = (Direction)state.getValue(MechanicalPistonHeadBlock.FACING);
            BlockPos offset = pos.relative(direction.getOpposite());
            if (!visited.contains(offset)) {
               BlockState blockState = level.getBlockState(offset);
               if (MechanicalPistonBlock.isExtensionPole(blockState)
                  && ((Direction)blockState.getValue(PistonExtensionPoleBlock.FACING)).getAxis() == direction.getAxis()) {
                  TEMP_DEFAULT_POSITIONS.add(offset);
               }

               if (blockState.getBlock() instanceof MechanicalPistonBlock) {
                  Direction pistonFacing = (Direction)blockState.getValue(MechanicalPistonBlock.FACING);
                  if (pistonFacing == direction && blockState.getValue(MechanicalPistonBlock.STATE) == PistonState.EXTENDED) {
                     TEMP_DEFAULT_POSITIONS.add(offset);
                  }
               }
            }

            if (state.getValue(MechanicalPistonHeadBlock.TYPE) == PistonType.STICKY) {
               BlockPos attached = pos.relative(direction);
               if (!visited.contains(attached)) {
                  TEMP_DEFAULT_POSITIONS.add(attached);
               }
            }
            break;
         case GantryCarriageBlock ignoredxxxxxx:
            BlockPos offsetx = pos.relative((Direction)state.getValue(GantryCarriageBlock.FACING));
            if (!visited.contains(offsetx)) {
               TEMP_DEFAULT_POSITIONS.add(offsetx);
            }

            Axis rotationAxis = ((IRotate)state.getBlock()).getRotationAxis(state);

            for (Direction dx : Iterate.directionsInAxis(rotationAxis)) {
               offsetx = pos.relative(dx);
               BlockState offsetState = level.getBlockState(offsetx);
               if (AllBlocks.GANTRY_SHAFT.has(offsetState)
                  && ((Direction)offsetState.getValue(GantryShaftBlock.FACING)).getAxis() == dx.getAxis()
                  && !visited.contains(offsetx)) {
                  TEMP_DEFAULT_POSITIONS.add(offsetx);
               }
            }
            break;
         case GantryShaftBlock ignoredxxxxxxx:
            for (Direction d : Iterate.directions) {
               BlockPos offsetx = pos.relative(d);
               if (!visited.contains(offsetx)) {
                  BlockState offsetState = level.getBlockState(offsetx);
                  Direction facing = (Direction)state.getValue(GantryShaftBlock.FACING);
                  if (d.getAxis() == facing.getAxis() && AllBlocks.GANTRY_SHAFT.has(offsetState) && offsetState.getValue(GantryShaftBlock.FACING) == facing) {
                     TEMP_DEFAULT_POSITIONS.add(offsetx);
                  } else if (AllBlocks.GANTRY_CARRIAGE.has(offsetState) && offsetState.getValue(GantryCarriageBlock.FACING) == d) {
                     TEMP_DEFAULT_POSITIONS.add(offsetx);
                  }
               }
            }
            break;
         case StickerBlock ignoredxxxxxxxx:
            if ((Boolean)state.getValue(StickerBlock.EXTENDED)) {
               Direction offset = (Direction)state.getValue(StickerBlock.FACING);
               BlockPos attached = pos.relative(offset);
               if (!visited.contains(attached) && !BlockMovementChecks.isNotSupportive(level.getBlockState(attached), offset.getOpposite())) {
                  TEMP_DEFAULT_POSITIONS.add(attached);
               }
            }
            break;
      }

      return TEMP_DEFAULT_POSITIONS;
   }

   public static void addAdditionalBlocks(BlockState state, Level world, BlockPos pos, Queue<BlockPos> frontier, Set<BlockPos> visited) {
      ObjectListIterator var5 = ADDITIONAL_BLOCK_REGISTRATIONS.iterator();

      while (var5.hasNext()) {
         SimBlockMovementChecks.AdditionalBlocks additional = (SimBlockMovementChecks.AdditionalBlocks)var5.next();
         additional.addAdditionalBlocks(state, world, pos, visited).forEach(frontier::add);
      }
   }

   public static boolean checkIsBlockAttachedTowards(BlockState state, Level world, BlockPos pos, BlockPos direction) {
      ObjectListIterator var4 = ATTACHED_CHECKS.iterator();

      while (var4.hasNext()) {
         SimBlockMovementChecks.AttachedCheck check = (SimBlockMovementChecks.AttachedCheck)var4.next();
         CheckResult result = check.isBlockAttachedTowards(state, world, pos, direction);
         if (result != CheckResult.PASS) {
            return result.toBoolean();
         }
      }

      return false;
   }

   @Internal
   public static void register() {
      BlockMovementChecks.registerAttachedCheck(SimBlockMovementChecks::registerDefaultBlockAttachedTowards);
      registerAdditionalBlocks(SimBlockMovementChecks::registerDefaultAdditionalBlocks);
   }

   public static synchronized void registerAttachedCheck(SimBlockMovementChecks.AttachedCheck check) {
      ATTACHED_CHECKS.addFirst(check);
   }

   public static synchronized void registerAdditionalBlocks(SimBlockMovementChecks.AdditionalBlocks additionalBlocks) {
      ADDITIONAL_BLOCK_REGISTRATIONS.addFirst(additionalBlocks);
   }

   @FunctionalInterface
   public interface AdditionalBlocks {
      Iterable<BlockPos> addAdditionalBlocks(BlockState var1, Level var2, BlockPos var3, Set<BlockPos> var4);
   }

   @FunctionalInterface
   public interface AttachedCheck {
      CheckResult isBlockAttachedTowards(BlockState var1, Level var2, BlockPos var3, BlockPos var4);
   }
}
