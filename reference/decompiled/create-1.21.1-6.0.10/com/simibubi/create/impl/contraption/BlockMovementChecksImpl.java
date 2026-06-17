package com.simibubi.create.impl.contraption;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.ContraptionMovementSetting;
import com.simibubi.create.content.contraptions.actors.AttachedActorBlock;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterBlock;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlock;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlock;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlock;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.SailBlock;
import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.content.contraptions.chassis.StickerBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.content.contraptions.pulley.PulleyBlock;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleExtenderBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.fan.NozzleBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.vault.ItemVaultBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.station.StationBlock;
import com.simibubi.create.content.trains.track.ITrackBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.Tags.Blocks;

public class BlockMovementChecksImpl {
   private static final List<BlockMovementChecks.MovementNecessaryCheck> MOVEMENT_NECESSARY_CHECKS = new ArrayList<>();
   private static final List<BlockMovementChecks.MovementAllowedCheck> MOVEMENT_ALLOWED_CHECKS = new ArrayList<>();
   private static final List<BlockMovementChecks.BrittleCheck> BRITTLE_CHECKS = new ArrayList<>();
   private static final List<BlockMovementChecks.AttachedCheck> ATTACHED_CHECKS = new ArrayList<>();
   private static final List<BlockMovementChecks.NotSupportiveCheck> NOT_SUPPORTIVE_CHECKS = new ArrayList<>();

   public static synchronized void registerMovementNecessaryCheck(BlockMovementChecks.MovementNecessaryCheck check) {
      MOVEMENT_NECESSARY_CHECKS.add(0, check);
   }

   public static synchronized void registerMovementAllowedCheck(BlockMovementChecks.MovementAllowedCheck check) {
      MOVEMENT_ALLOWED_CHECKS.add(0, check);
   }

   public static synchronized void registerBrittleCheck(BlockMovementChecks.BrittleCheck check) {
      BRITTLE_CHECKS.add(0, check);
   }

   public static synchronized void registerAttachedCheck(BlockMovementChecks.AttachedCheck check) {
      ATTACHED_CHECKS.add(0, check);
   }

   public static synchronized void registerNotSupportiveCheck(BlockMovementChecks.NotSupportiveCheck check) {
      NOT_SUPPORTIVE_CHECKS.add(0, check);
   }

   public static boolean isMovementNecessary(BlockState state, Level world, BlockPos pos) {
      for (BlockMovementChecks.MovementNecessaryCheck check : MOVEMENT_NECESSARY_CHECKS) {
         BlockMovementChecks.CheckResult result = check.isMovementNecessary(state, world, pos);
         if (result != BlockMovementChecks.CheckResult.PASS) {
            return result.toBoolean();
         }
      }

      return isMovementNecessaryFallback(state, world, pos);
   }

   public static boolean isMovementAllowed(BlockState state, Level world, BlockPos pos) {
      for (BlockMovementChecks.MovementAllowedCheck check : MOVEMENT_ALLOWED_CHECKS) {
         BlockMovementChecks.CheckResult result = check.isMovementAllowed(state, world, pos);
         if (result != BlockMovementChecks.CheckResult.PASS) {
            return result.toBoolean();
         }
      }

      return isMovementAllowedFallback(state, world, pos);
   }

   public static boolean isBrittle(BlockState state) {
      for (BlockMovementChecks.BrittleCheck check : BRITTLE_CHECKS) {
         BlockMovementChecks.CheckResult result = check.isBrittle(state);
         if (result != BlockMovementChecks.CheckResult.PASS) {
            return result.toBoolean();
         }
      }

      return isBrittleFallback(state);
   }

   public static boolean isBlockAttachedTowards(BlockState state, Level world, BlockPos pos, Direction direction) {
      for (BlockMovementChecks.AttachedCheck check : ATTACHED_CHECKS) {
         BlockMovementChecks.CheckResult result = check.isBlockAttachedTowards(state, world, pos, direction);
         if (result != BlockMovementChecks.CheckResult.PASS) {
            return result.toBoolean();
         }
      }

      return isBlockAttachedTowardsFallback(state, world, pos, direction);
   }

   public static boolean isNotSupportive(BlockState state, Direction facing) {
      for (BlockMovementChecks.NotSupportiveCheck check : NOT_SUPPORTIVE_CHECKS) {
         BlockMovementChecks.CheckResult result = check.isNotSupportive(state, facing);
         if (result != BlockMovementChecks.CheckResult.PASS) {
            return result.toBoolean();
         }
      }

      return isNotSupportiveFallback(state, facing);
   }

   private static boolean isMovementNecessaryFallback(BlockState state, Level world, BlockPos pos) {
      if (BlockMovementChecks.isBrittle(state)) {
         return true;
      } else if (AllTags.AllBlockTags.MOVABLE_EMPTY_COLLIDER.matches(state)) {
         return true;
      } else {
         return state.getCollisionShape(world, pos).isEmpty() ? false : !state.canBeReplaced();
      }
   }

   private static boolean isMovementAllowedFallback(BlockState state, Level world, BlockPos pos) {
      Block block = state.getBlock();
      if (block instanceof AbstractChassisBlock) {
         return true;
      } else if (state.getDestroySpeed(world, pos) == -1.0F) {
         return false;
      } else if (state.is(Blocks.RELOCATION_NOT_SUPPORTED)) {
         return false;
      } else if (AllTags.AllBlockTags.NON_MOVABLE.matches(state)) {
         return false;
      } else if (ContraptionMovementSetting.get(state) == ContraptionMovementSetting.UNMOVABLE) {
         return false;
      } else if (block instanceof MechanicalPistonBlock && state.getValue(MechanicalPistonBlock.STATE) != MechanicalPistonBlock.PistonState.MOVING) {
         return true;
      } else {
         if (block instanceof MechanicalBearingBlock) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof MechanicalBearingBlockEntity) {
               return !((MechanicalBearingBlockEntity)be).isRunning();
            }
         }

         if (block instanceof ClockworkBearingBlock) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof ClockworkBearingBlockEntity) {
               return !((ClockworkBearingBlockEntity)be).isRunning();
            }
         }

         if (block instanceof PulleyBlock && world.getBlockEntity(pos) instanceof PulleyBlockEntity pulley) {
            return !pulley.running;
         } else if (AllBlocks.BELT.has(state)) {
            return true;
         } else if (state.getBlock() instanceof GrindstoneBlock) {
            return true;
         } else if (state.getBlock() instanceof ITrackBlock) {
            return false;
         } else {
            return state.getBlock() instanceof StationBlock ? false : state.getPistonPushReaction() != PushReaction.BLOCK;
         }
      }
   }

   private static boolean isBrittleFallback(BlockState state) {
      Block block = state.getBlock();
      if (state.hasProperty(BlockStateProperties.HANGING)) {
         return true;
      } else if (block instanceof LadderBlock) {
         return true;
      } else if (block instanceof BaseTorchBlock) {
         return true;
      } else if (block instanceof SignBlock) {
         return true;
      } else if (block instanceof BasePressurePlateBlock) {
         return true;
      } else if (block instanceof FaceAttachedHorizontalDirectionalBlock && !(block instanceof GrindstoneBlock) && !(block instanceof PackagerLinkBlock)) {
         return true;
      } else if (block instanceof CartAssemblerBlock) {
         return false;
      } else if (block instanceof BaseRailBlock) {
         return true;
      } else if (block instanceof DiodeBlock) {
         return true;
      } else if (block instanceof RedStoneWireBlock) {
         return true;
      } else if (block instanceof WoolCarpetBlock) {
         return true;
      } else if (block instanceof WhistleBlock) {
         return true;
      } else if (block instanceof WhistleExtenderBlock) {
         return true;
      } else {
         return block instanceof BeltFunnelBlock ? true : AllTags.AllBlockTags.BRITTLE.matches(state);
      }
   }

   private static boolean isBlockAttachedTowardsFallback(BlockState state, Level world, BlockPos pos, Direction direction) {
      Block block = state.getBlock();
      if (block instanceof LadderBlock) {
         return state.getValue(LadderBlock.FACING) == direction.getOpposite();
      } else if (block instanceof WallTorchBlock) {
         return state.getValue(WallTorchBlock.FACING) == direction.getOpposite();
      } else if (block instanceof WallSignBlock) {
         return state.getValue(WallSignBlock.FACING) == direction.getOpposite();
      } else if (block instanceof StandingSignBlock) {
         return direction == Direction.DOWN;
      } else if (block instanceof BasePressurePlateBlock) {
         return direction == Direction.DOWN;
      } else if (block instanceof DoorBlock) {
         return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && direction == Direction.UP ? true : direction == Direction.DOWN;
      } else if (block instanceof BedBlock) {
         Direction facing = (Direction)state.getValue(BedBlock.FACING);
         if (state.getValue(BedBlock.PART) == BedPart.HEAD) {
            facing = facing.getOpposite();
         }

         return direction == facing;
      } else if (block instanceof RedstoneLinkBlock) {
         return direction.getOpposite() == state.getValue(RedstoneLinkBlock.FACING);
      } else if (block instanceof FlowerPotBlock) {
         return direction == Direction.DOWN;
      } else if (block instanceof DiodeBlock) {
         return direction == Direction.DOWN;
      } else if (block instanceof RedStoneWireBlock) {
         return direction == Direction.DOWN;
      } else if (block instanceof WoolCarpetBlock) {
         return direction == Direction.DOWN;
      } else if (block instanceof RedstoneWallTorchBlock) {
         return state.getValue(RedstoneWallTorchBlock.FACING) == direction.getOpposite();
      } else if (block instanceof BaseTorchBlock) {
         return direction == Direction.DOWN;
      } else {
         if (block instanceof FaceAttachedHorizontalDirectionalBlock) {
            AttachFace attachFace = (AttachFace)state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
            if (attachFace == AttachFace.CEILING) {
               return direction == Direction.UP;
            }

            if (attachFace == AttachFace.FLOOR) {
               return direction == Direction.DOWN;
            }

            if (attachFace == AttachFace.WALL) {
               return direction.getOpposite() == state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);
            }
         }

         if (state.hasProperty(BlockStateProperties.HANGING)) {
            return direction == (state.getValue(BlockStateProperties.HANGING) ? Direction.UP : Direction.DOWN);
         } else if (block instanceof BaseRailBlock) {
            return direction == Direction.DOWN;
         } else if (block instanceof AttachedActorBlock) {
            return direction == ((Direction)state.getValue(HarvesterBlock.FACING)).getOpposite();
         } else if (block instanceof HandCrankBlock) {
            return direction == ((Direction)state.getValue(HandCrankBlock.FACING)).getOpposite();
         } else if (block instanceof NozzleBlock) {
            return direction == ((Direction)state.getValue(NozzleBlock.FACING)).getOpposite();
         } else if (block instanceof BellBlock) {
            BellAttachType attachment = (BellAttachType)state.getValue(BlockStateProperties.BELL_ATTACHMENT);
            if (attachment == BellAttachType.FLOOR) {
               return direction == Direction.DOWN;
            } else {
               return attachment == BellAttachType.CEILING ? direction == Direction.UP : direction == state.getValue(HorizontalDirectionalBlock.FACING);
            }
         } else if (state.getBlock() instanceof SailBlock) {
            return direction.getAxis() != ((Direction)state.getValue(SailBlock.FACING)).getAxis();
         } else if (state.getBlock() instanceof FluidTankBlock) {
            return ConnectivityHandler.isConnected(world, pos, pos.relative(direction));
         } else if (state.getBlock() instanceof ItemVaultBlock) {
            return ConnectivityHandler.isConnected(world, pos, pos.relative(direction));
         } else if (AllBlocks.STICKER.has(state) && (Boolean)state.getValue(StickerBlock.EXTENDED)) {
            return direction == state.getValue(StickerBlock.FACING)
               && !BlockMovementChecks.isNotSupportive(world.getBlockState(pos.relative(direction)), direction.getOpposite());
         } else if (block instanceof AbstractBogeyBlock<?> bogey) {
            return bogey.getStickySurfaces(world, pos, state).contains(direction);
         } else if (block instanceof WhistleBlock) {
            return direction == (state.getValue(WhistleBlock.WALL) ? (Direction)state.getValue(WhistleBlock.FACING) : Direction.DOWN);
         } else {
            return block instanceof WhistleExtenderBlock ? direction == Direction.DOWN : false;
         }
      }
   }

   private static boolean isNotSupportiveFallback(BlockState state, Direction facing) {
      if (AllBlocks.MECHANICAL_DRILL.has(state)) {
         return state.getValue(BlockStateProperties.FACING) == facing;
      } else if (AllBlocks.MECHANICAL_BEARING.has(state)) {
         return state.getValue(BlockStateProperties.FACING) == facing;
      } else if (AllBlocks.CART_ASSEMBLER.has(state)) {
         return facing == Direction.DOWN;
      } else if (AllBlocks.MECHANICAL_SAW.has(state)) {
         return state.getValue(BlockStateProperties.FACING) == facing;
      } else if (AllBlocks.PORTABLE_STORAGE_INTERFACE.has(state)) {
         return state.getValue(PortableStorageInterfaceBlock.FACING) == facing;
      } else if (state.getBlock() instanceof AttachedActorBlock && !AllBlocks.MECHANICAL_ROLLER.has(state)) {
         return state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing;
      } else if (AllBlocks.ROPE_PULLEY.has(state)) {
         return facing == Direction.DOWN;
      } else if (state.getBlock() instanceof WoolCarpetBlock) {
         return facing == Direction.UP;
      } else if (state.getBlock() instanceof SailBlock) {
         return facing.getAxis() == ((Direction)state.getValue(SailBlock.FACING)).getAxis();
      } else if (AllBlocks.PISTON_EXTENSION_POLE.has(state)) {
         return facing.getAxis() != ((Direction)state.getValue(BlockStateProperties.FACING)).getAxis();
      } else if (AllBlocks.MECHANICAL_PISTON_HEAD.has(state)) {
         return facing.getAxis() != ((Direction)state.getValue(BlockStateProperties.FACING)).getAxis();
      } else if (AllBlocks.STICKER.has(state) && !(Boolean)state.getValue(StickerBlock.EXTENDED)) {
         return facing == state.getValue(StickerBlock.FACING);
      } else {
         return state.getBlock() instanceof SlidingDoorBlock ? false : BlockMovementChecks.isBrittle(state);
      }
   }
}
