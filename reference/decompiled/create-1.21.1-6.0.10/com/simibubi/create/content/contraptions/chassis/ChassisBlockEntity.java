package com.simibubi.create.content.contraptions.chassis;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllKeys;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.BulkScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ChassisBlockEntity extends SmartBlockEntity {
   ScrollValueBehaviour range;
   public int currentlySelectedRange;

   public ChassisBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      int max = (Integer)AllConfigs.server().kinetics.maxChassisRange.get();
      this.range = new ChassisBlockEntity.ChassisScrollValueBehaviour(
         CreateLang.translateDirect("contraptions.chassis.range"),
         this,
         new CenteredSideValueBoxTransform(),
         be -> ((ChassisBlockEntity)be).collectChassisGroup()
      );
      this.range.requiresWrench();
      this.range.between(1, max);
      this.range.withClientCallback(i -> CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> ChassisRangeDisplay.display(this)));
      this.range.setValue(max / 2);
      this.range.withFormatter(s -> String.valueOf(this.currentlySelectedRange));
      behaviours.add(this.range);
      this.currentlySelectedRange = this.range.getValue();
   }

   @Override
   public void initialize() {
      super.initialize();
      if (this.getBlockState().getBlock() instanceof RadialChassisBlock) {
         this.range.setLabel(CreateLang.translateDirect("contraptions.chassis.radius"));
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      if (clientPacket) {
         this.currentlySelectedRange = this.getRange();
      }
   }

   public int getRange() {
      return this.range.getValue();
   }

   public List<BlockPos> getIncludedBlockPositions(Direction forcedMovement, boolean visualize) {
      if (!(this.getBlockState().getBlock() instanceof AbstractChassisBlock)) {
         return Collections.emptyList();
      } else {
         return this.isRadial()
            ? this.getIncludedBlockPositionsRadial(forcedMovement, visualize)
            : this.getIncludedBlockPositionsLinear(forcedMovement, visualize);
      }
   }

   protected boolean isRadial() {
      return this.level.getBlockState(this.worldPosition).getBlock() instanceof RadialChassisBlock;
   }

   public List<ChassisBlockEntity> collectChassisGroup() {
      Queue<BlockPos> frontier = new LinkedList<>();
      List<ChassisBlockEntity> collected = new ArrayList<>();
      Set<BlockPos> visited = new HashSet<>();
      frontier.add(this.worldPosition);

      while (!frontier.isEmpty()) {
         BlockPos current = frontier.poll();
         if (!visited.contains(current)) {
            visited.add(current);
            if (this.level.getBlockEntity(current) instanceof ChassisBlockEntity chassis) {
               collected.add(chassis);
               visited.add(current);
               chassis.addAttachedChasses(frontier, visited);
            }
         }
      }

      return collected;
   }

   public boolean addAttachedChasses(Queue<BlockPos> frontier, Set<BlockPos> visited) {
      BlockState state = this.getBlockState();
      if (!(state.getBlock() instanceof AbstractChassisBlock)) {
         return false;
      } else {
         Axis axis = (Axis)state.getValue(AbstractChassisBlock.AXIS);
         if (this.isRadial()) {
            for (int offset : new int[]{-1, 1}) {
               Direction direction = Direction.get(AxisDirection.POSITIVE, axis);
               BlockPos currentPos = this.worldPosition.relative(direction, offset);
               if (!this.level.isLoaded(currentPos)) {
                  return false;
               }

               BlockState neighbourState = this.level.getBlockState(currentPos);
               if (AllBlocks.RADIAL_CHASSIS.has(neighbourState) && axis == neighbourState.getValue(BlockStateProperties.AXIS) && !visited.contains(currentPos)) {
                  frontier.add(currentPos);
               }
            }

            return true;
         } else {
            for (Direction offset : Iterate.directions) {
               BlockPos current = this.worldPosition.relative(offset);
               if (!visited.contains(current)) {
                  if (!this.level.isLoaded(current)) {
                     return false;
                  }

                  BlockState neighbourState = this.level.getBlockState(current);
                  if (LinearChassisBlock.isChassis(neighbourState)
                     && LinearChassisBlock.sameKind(state, neighbourState)
                     && neighbourState.getValue(LinearChassisBlock.AXIS) == axis) {
                     frontier.add(current);
                  }
               }
            }

            return true;
         }
      }
   }

   private List<BlockPos> getIncludedBlockPositionsLinear(Direction forcedMovement, boolean visualize) {
      List<BlockPos> positions = new ArrayList<>();
      BlockState state = this.getBlockState();
      AbstractChassisBlock block = (AbstractChassisBlock)state.getBlock();
      Axis axis = (Axis)state.getValue(AbstractChassisBlock.AXIS);
      Direction facing = Direction.get(AxisDirection.POSITIVE, axis);
      int chassisRange = visualize ? this.currentlySelectedRange : this.getRange();

      for (int offset : new int[]{1, -1}) {
         if (offset == -1) {
            facing = facing.getOpposite();
         }

         boolean sticky = (Boolean)state.getValue(block.getGlueableSide(state, facing));

         for (int i = 1; i <= chassisRange; i++) {
            BlockPos current = this.worldPosition.relative(facing, i);
            BlockState currentState = this.level.getBlockState(current);
            if (forcedMovement != facing && !sticky
               || !BlockMovementChecks.isMovementNecessary(currentState, this.level, current)
               || BlockMovementChecks.isBrittle(currentState)) {
               break;
            }

            positions.add(current);
            if (BlockMovementChecks.isNotSupportive(currentState, facing)) {
               break;
            }
         }
      }

      return positions;
   }

   private List<BlockPos> getIncludedBlockPositionsRadial(Direction forcedMovement, boolean visualize) {
      List<BlockPos> positions = new ArrayList<>();
      BlockState state = this.level.getBlockState(this.worldPosition);
      Axis axis = (Axis)state.getValue(AbstractChassisBlock.AXIS);
      AbstractChassisBlock block = (AbstractChassisBlock)state.getBlock();
      int chassisRange = visualize ? this.currentlySelectedRange : this.getRange();

      for (Direction facing : Iterate.directions) {
         if (facing.getAxis() != axis && (Boolean)state.getValue(block.getGlueableSide(state, facing))) {
            BlockPos startPos = this.worldPosition.relative(facing);
            List<BlockPos> localFrontier = new LinkedList<>();
            Set<BlockPos> localVisited = new HashSet<>();
            localFrontier.add(startPos);

            while (!localFrontier.isEmpty()) {
               BlockPos searchPos = localFrontier.remove(0);
               BlockState searchedState = this.level.getBlockState(searchPos);
               if (!localVisited.contains(searchPos)
                  && searchPos.closerThan(this.worldPosition, (double)((float)chassisRange + 0.5F))
                  && BlockMovementChecks.isMovementNecessary(searchedState, this.level, searchPos)
                  && !BlockMovementChecks.isBrittle(searchedState)) {
                  localVisited.add(searchPos);
                  if (!searchPos.equals(this.worldPosition)) {
                     positions.add(searchPos);
                  }

                  for (Direction offset : Iterate.directions) {
                     if (offset.getAxis() != axis
                        && (!searchPos.equals(this.worldPosition) || offset == facing)
                        && !BlockMovementChecks.isNotSupportive(searchedState, offset)) {
                        localFrontier.add(searchPos.relative(offset));
                     }
                  }
               }
            }
         }
      }

      return positions;
   }

   class ChassisScrollValueBehaviour extends BulkScrollValueBehaviour {
      public ChassisScrollValueBehaviour(
         Component label, SmartBlockEntity be, ValueBoxTransform slot, Function<SmartBlockEntity, List<? extends SmartBlockEntity>> groupGetter
      ) {
         super(label, be, slot, groupGetter);
      }

      @Override
      public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
         ImmutableList<Component> rows = ImmutableList.of(CreateLang.translateDirect("contraptions.chassis.distance"));
         ValueSettingsFormatter formatter = new ValueSettingsFormatter(vs -> new ValueSettingsBehaviour.ValueSettings(vs.row(), vs.value() + 1).format());
         return new ValueSettingsBoard(this.label, this.max - 1, 1, rows, formatter);
      }

      @OnlyIn(Dist.CLIENT)
      @Override
      public void newSettingHovered(ValueSettingsBehaviour.ValueSettings valueSetting) {
         if (ChassisBlockEntity.this.level.isClientSide) {
            if (!AllKeys.ctrlDown()) {
               ChassisBlockEntity.this.currentlySelectedRange = valueSetting.value() + 1;
            } else {
               for (SmartBlockEntity be : this.getBulk()) {
                  if (be instanceof ChassisBlockEntity cbe) {
                     cbe.currentlySelectedRange = valueSetting.value() + 1;
                  }
               }
            }

            ChassisRangeDisplay.display(ChassisBlockEntity.this);
         }
      }

      @Override
      public void setValueSettings(Player player, ValueSettingsBehaviour.ValueSettings vs, boolean ctrlHeld) {
         super.setValueSettings(player, new ValueSettingsBehaviour.ValueSettings(vs.row(), vs.value() + 1), ctrlHeld);
      }

      @Override
      public ValueSettingsBehaviour.ValueSettings getValueSettings() {
         ValueSettingsBehaviour.ValueSettings vs = super.getValueSettings();
         return new ValueSettingsBehaviour.ValueSettings(vs.row(), vs.value() - 1);
      }

      @Override
      public String getClipboardKey() {
         return "Chassis";
      }
   }
}
