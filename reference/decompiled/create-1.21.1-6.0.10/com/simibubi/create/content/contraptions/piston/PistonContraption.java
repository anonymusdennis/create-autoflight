package com.simibubi.create.content.contraptions.piston;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.TranslatingContraption;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

public class PistonContraption extends TranslatingContraption {
   protected int extensionLength;
   protected int initialExtensionProgress;
   protected Direction orientation;
   private AABB pistonExtensionCollisionBox;
   private boolean retract;

   @Override
   public ContraptionType getType() {
      return (ContraptionType)AllContraptionTypes.PISTON.value();
   }

   public PistonContraption() {
   }

   public PistonContraption(Direction direction, boolean retract) {
      this.orientation = direction;
      this.retract = retract;
   }

   @Override
   public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
      if (!this.collectExtensions(world, pos, this.orientation)) {
         return false;
      } else {
         int count = this.blocks.size();
         if (!this.searchMovedStructure(world, this.anchor, this.retract ? this.orientation.getOpposite() : this.orientation)) {
            return false;
         } else {
            if (this.blocks.size() == count) {
               this.bounds = this.pistonExtensionCollisionBox;
            } else {
               this.bounds = this.bounds.minmax(this.pistonExtensionCollisionBox);
            }

            this.startMoving(world);
            return true;
         }
      }
   }

   private boolean collectExtensions(Level world, BlockPos pos, Direction direction) throws AssemblyException {
      List<StructureBlockInfo> poles = new ArrayList<>();
      BlockPos actualStart = pos;
      BlockState nextBlock = world.getBlockState(pos.relative(direction));
      int extensionsInFront = 0;
      BlockState blockState = world.getBlockState(pos);
      boolean sticky = MechanicalPistonBlock.isStickyPiston(blockState);
      if (!MechanicalPistonBlock.isPiston(blockState)) {
         return false;
      } else {
         if (blockState.getValue(MechanicalPistonBlock.STATE) == MechanicalPistonBlock.PistonState.EXTENDED) {
            while (
               PistonExtensionPoleBlock.PlacementHelper.get().matchesAxis(nextBlock, direction.getAxis())
                  || MechanicalPistonBlock.isPistonHead(nextBlock) && nextBlock.getValue(BlockStateProperties.FACING) == direction
            ) {
               actualStart = actualStart.relative(direction);
               poles.add(new StructureBlockInfo(actualStart, (BlockState)nextBlock.setValue(BlockStateProperties.FACING, direction), null));
               extensionsInFront++;
               if (MechanicalPistonBlock.isPistonHead(nextBlock)) {
                  break;
               }

               nextBlock = world.getBlockState(actualStart.relative(direction));
               if (extensionsInFront > MechanicalPistonBlock.maxAllowedPistonPoles()) {
                  throw AssemblyException.tooManyPistonPoles();
               }
            }
         }

         if (extensionsInFront == 0) {
            poles.add(
               new StructureBlockInfo(
                  pos,
                  (BlockState)((BlockState)AllBlocks.MECHANICAL_PISTON_HEAD.getDefaultState().setValue(BlockStateProperties.FACING, direction))
                     .setValue(BlockStateProperties.PISTON_TYPE, sticky ? PistonType.STICKY : PistonType.DEFAULT),
                  null
               )
            );
         } else {
            poles.add(
               new StructureBlockInfo(pos, (BlockState)AllBlocks.PISTON_EXTENSION_POLE.getDefaultState().setValue(BlockStateProperties.FACING, direction), null)
            );
         }

         BlockPos end = pos;
         nextBlock = world.getBlockState(pos.relative(direction.getOpposite()));
         int extensionsInBack = 0;

         while (PistonExtensionPoleBlock.PlacementHelper.get().matchesAxis(nextBlock, direction.getAxis())) {
            end = end.relative(direction.getOpposite());
            poles.add(new StructureBlockInfo(end, (BlockState)nextBlock.setValue(BlockStateProperties.FACING, direction), null));
            extensionsInBack++;
            nextBlock = world.getBlockState(end.relative(direction.getOpposite()));
            if (extensionsInFront + extensionsInBack > MechanicalPistonBlock.maxAllowedPistonPoles()) {
               throw AssemblyException.tooManyPistonPoles();
            }
         }

         this.anchor = pos.relative(direction, this.initialExtensionProgress + 1);
         this.extensionLength = extensionsInBack + extensionsInFront;
         this.initialExtensionProgress = extensionsInFront;
         this.pistonExtensionCollisionBox = new AABB(
               Vec3.atLowerCornerOf(BlockPos.ZERO.relative(direction, -1)), Vec3.atLowerCornerOf(BlockPos.ZERO.relative(direction, -this.extensionLength - 1))
            )
            .expandTowards(1.0, 1.0, 1.0);
         if (this.extensionLength == 0) {
            throw AssemblyException.noPistonPoles();
         } else {
            this.bounds = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

            for (StructureBlockInfo pole : poles) {
               BlockPos relPos = pole.pos().relative(direction, -extensionsInFront);
               BlockPos localPos = relPos.subtract(this.anchor);
               this.getBlocks().put(localPos, new StructureBlockInfo(localPos, pole.state(), null));
            }

            return true;
         }
      }
   }

   @Override
   protected boolean isAnchoringBlockAt(BlockPos pos) {
      return this.pistonExtensionCollisionBox.contains(VecHelper.getCenterOf(pos.subtract(this.anchor)));
   }

   @Override
   protected boolean addToInitialFrontier(Level world, BlockPos pos, Direction direction, Queue<BlockPos> frontier) throws AssemblyException {
      frontier.clear();
      boolean sticky = MechanicalPistonBlock.isStickyPiston(world.getBlockState(pos.relative(this.orientation, -1)));
      boolean retracting = direction != this.orientation;
      if (retracting && !sticky) {
         return true;
      } else {
         for (int offset = 0; offset <= AllConfigs.server().kinetics.maxChassisRange.get(); offset++) {
            if (offset == 1 && retracting) {
               return true;
            }

            BlockPos currentPos = pos.relative(this.orientation, offset + this.initialExtensionProgress);
            if (retracting && world.isOutsideBuildHeight(currentPos)) {
               return true;
            }

            if (!world.isLoaded(currentPos)) {
               throw AssemblyException.unloadedChunk(currentPos);
            }

            BlockState state = world.getBlockState(currentPos);
            if (!BlockMovementChecks.isMovementNecessary(state, world, currentPos)) {
               return true;
            }

            if (BlockMovementChecks.isBrittle(state) && !(state.getBlock() instanceof WoolCarpetBlock)) {
               return true;
            }

            if (MechanicalPistonBlock.isPistonHead(state) && state.getValue(BlockStateProperties.FACING) == direction.getOpposite()) {
               return true;
            }

            if (!BlockMovementChecks.isMovementAllowed(state, world, currentPos)) {
               if (retracting) {
                  return true;
               }

               throw AssemblyException.unmovableBlock(currentPos, state);
            }

            if (retracting && state.getPistonPushReaction() == PushReaction.PUSH_ONLY) {
               return true;
            }

            frontier.add(currentPos);
            if (BlockMovementChecks.isNotSupportive(state, this.orientation)) {
               return true;
            }
         }

         return true;
      }
   }

   @Override
   public void addBlock(Level level, BlockPos pos, Pair<StructureBlockInfo, BlockEntity> capture) {
      super.addBlock(level, pos.relative(this.orientation, -this.initialExtensionProgress), capture);
   }

   @Override
   public BlockPos toLocalPos(BlockPos globalPos) {
      return globalPos.subtract(this.anchor).relative(this.orientation, -this.initialExtensionProgress);
   }

   @Override
   protected boolean customBlockPlacement(LevelAccessor world, BlockPos pos, BlockState state) {
      BlockPos pistonPos = this.anchor.relative(this.orientation, -1);
      BlockState pistonState = world.getBlockState(pistonPos);
      BlockEntity be = world.getBlockEntity(pistonPos);
      if (pos.equals(pistonPos)) {
         if (be != null && !be.isRemoved()) {
            if (!MechanicalPistonBlock.isExtensionPole(state) && MechanicalPistonBlock.isPiston(pistonState)) {
               world.setBlock(pistonPos, (BlockState)pistonState.setValue(MechanicalPistonBlock.STATE, MechanicalPistonBlock.PistonState.RETRACTED), 19);
            }

            return true;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   protected boolean customBlockRemoval(LevelAccessor world, BlockPos pos, BlockState state) {
      BlockPos pistonPos = this.anchor.relative(this.orientation, -1);
      BlockState blockState = world.getBlockState(pos);
      if (pos.equals(pistonPos) && MechanicalPistonBlock.isPiston(blockState)) {
         world.setBlock(pos, (BlockState)blockState.setValue(MechanicalPistonBlock.STATE, MechanicalPistonBlock.PistonState.MOVING), 82);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void readNBT(Level world, CompoundTag nbt, boolean spawnData) {
      super.readNBT(world, nbt, spawnData);
      this.initialExtensionProgress = nbt.getInt("InitialLength");
      this.extensionLength = nbt.getInt("ExtensionLength");
      this.orientation = Direction.from3DDataValue(nbt.getInt("Orientation"));
   }

   @Override
   public CompoundTag writeNBT(Provider registries, boolean spawnPacket) {
      CompoundTag tag = super.writeNBT(registries, spawnPacket);
      tag.putInt("InitialLength", this.initialExtensionProgress);
      tag.putInt("ExtensionLength", this.extensionLength);
      tag.putInt("Orientation", this.orientation.get3DDataValue());
      return tag;
   }
}
