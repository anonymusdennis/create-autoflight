package com.simibubi.create.content.contraptions.elevator;

import com.simibubi.create.content.decoration.slidingDoor.DoorControlBehaviour;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ElevatorContactBlockEntity extends SmartBlockEntity {
   public DoorControlBehaviour doorControls;
   public ElevatorColumn.ColumnCoords columnCoords;
   public boolean activateBlock;
   public String shortName;
   public String longName;
   public String lastReportedCurrentFloor = "";
   private int yTargetFromNBT = Integer.MIN_VALUE;
   private boolean deferNameGenerator;

   public ElevatorContactBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.shortName = "";
      this.longName = "";
      this.deferNameGenerator = false;
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.doorControls = new DoorControlBehaviour(this));
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putString("ShortName", this.shortName);
      tag.putString("LongName", this.longName);
      if (this.lastReportedCurrentFloor != null) {
         tag.putString("LastReportedCurrentFloor", this.lastReportedCurrentFloor);
      }

      if (!clientPacket) {
         tag.putBoolean("Activate", this.activateBlock);
         if (this.columnCoords != null) {
            ElevatorColumn column = ElevatorColumn.get(this.level, this.columnCoords);
            if (column != null) {
               tag.putInt("ColumnTarget", column.getTargetedYLevel());
               if (column.isActive()) {
                  NBTHelper.putMarker(tag, "ColumnActive");
               }
            }
         }
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.shortName = tag.getString("ShortName");
      this.longName = tag.getString("LongName");
      if (tag.contains("LastReportedCurrentFloor")) {
         this.lastReportedCurrentFloor = tag.getString("LastReportedCurrentFloor");
      }

      if (!clientPacket) {
         this.activateBlock = tag.getBoolean("Activate");
         if (tag.contains("ColumnTarget")) {
            int target = tag.getInt("ColumnTarget");
            boolean active = tag.contains("ColumnActive");
            if (this.columnCoords == null) {
               this.yTargetFromNBT = target;
            } else {
               ElevatorColumn column = ElevatorColumn.getOrCreate(this.level, this.columnCoords);
               column.target(target);
               column.setActive(active);
            }
         }
      }
   }

   public void updateDisplayedFloor(String floor) {
      if (!floor.equals(this.lastReportedCurrentFloor)) {
         this.lastReportedCurrentFloor = floor;
         DisplayLinkBlock.notifyGatherers(this.level, this.worldPosition);
      }
   }

   @Override
   public void initialize() {
      super.initialize();
      if (!this.level.isClientSide()) {
         this.columnCoords = ElevatorContactBlock.getColumnCoords(this.level, this.worldPosition);
         if (this.columnCoords != null) {
            ElevatorColumn column = ElevatorColumn.getOrCreate(this.level, this.columnCoords);
            column.add(this.worldPosition);
            if (this.shortName.isBlank()) {
               this.deferNameGenerator = true;
            }

            if (this.yTargetFromNBT != Integer.MIN_VALUE) {
               column.target(this.yTargetFromNBT);
               this.yTargetFromNBT = Integer.MIN_VALUE;
            }
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.deferNameGenerator) {
         if (this.columnCoords != null) {
            ElevatorColumn.getOrCreate(this.level, this.columnCoords).initNames(this.level);
         }

         this.deferNameGenerator = false;
      }
   }

   @Override
   public void invalidate() {
      if (this.columnCoords != null) {
         ElevatorColumn column = ElevatorColumn.get(this.level, this.columnCoords);
         if (column != null) {
            column.remove(this.worldPosition);
         }
      }

      super.invalidate();
   }

   public void updateName(String shortName, String longName) {
      this.shortName = shortName;
      this.longName = longName;
      this.deferNameGenerator = false;
      this.notifyUpdate();
      ElevatorColumn column = ElevatorColumn.get(this.level, this.columnCoords);
      if (column != null) {
         column.namesChanged();
      }
   }

   public Couple<String> getNames() {
      return Couple.create(this.shortName, this.longName);
   }
}
