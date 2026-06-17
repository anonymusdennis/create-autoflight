package com.simibubi.create.content.contraptions.elevator;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.IntAttached;
import net.createmod.catnip.data.WorldAttached;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class ElevatorColumn {
   public static WorldAttached<Map<ElevatorColumn.ColumnCoords, ElevatorColumn>> LOADED_COLUMNS = new WorldAttached($ -> new HashMap());
   protected LevelAccessor level;
   protected ElevatorColumn.ColumnCoords coords;
   protected List<Integer> contacts;
   protected int targetedYLevel;
   protected boolean isActive;
   protected boolean targetAvailable;
   public int namesListVersion;

   @Nullable
   public static ElevatorColumn get(LevelAccessor level, ElevatorColumn.ColumnCoords coords) {
      return (ElevatorColumn)((Map)LOADED_COLUMNS.get(level)).get(coords);
   }

   public static ElevatorColumn getOrCreate(LevelAccessor level, ElevatorColumn.ColumnCoords coords) {
      return ((Map)LOADED_COLUMNS.get(level)).computeIfAbsent(coords, c -> new ElevatorColumn(level, c));
   }

   public ElevatorColumn(LevelAccessor level, ElevatorColumn.ColumnCoords coords) {
      this.level = level;
      this.coords = coords;
      this.contacts = new ArrayList<>();
      this.targetAvailable = false;
   }

   public void markDirty() {
      for (BlockPos pos : this.getContacts()) {
         if (this.level.getBlockEntity(pos) instanceof ElevatorContactBlockEntity ecbe) {
            ecbe.setChanged();
         }
      }
   }

   public void floorReached(LevelAccessor level, String name) {
      this.getContacts().forEach(p -> {
         if (level.getBlockEntity(p) instanceof ElevatorContactBlockEntity ecbe) {
            ecbe.updateDisplayedFloor(name);
         }
      });
   }

   public List<IntAttached<Couple<String>>> compileNamesList() {
      return this.getContacts()
         .stream()
         .map(p -> this.level.getBlockEntity(p) instanceof ElevatorContactBlockEntity ecbe ? IntAttached.with(p.getY(), ecbe.getNames()) : null)
         .filter(Objects::nonNull)
         .toList();
   }

   public void namesChanged() {
      this.namesListVersion++;
   }

   public Collection<BlockPos> getContacts() {
      return this.contacts.stream().map(this::contactAt).toList();
   }

   public void gatherAll() {
      BlockPos.betweenClosedStream(this.contactAt(this.level.getMinBuildHeight()), this.contactAt(this.level.getMaxBuildHeight()))
         .filter(p -> this.coords.equals(ElevatorContactBlock.getColumnCoords(this.level, p)))
         .forEach(p -> this.level.setBlock(p, BlockHelper.copyProperties(this.level.getBlockState(p), AllBlocks.ELEVATOR_CONTACT.getDefaultState()), 3));
   }

   public BlockPos contactAt(int y) {
      return new BlockPos(this.coords.x, y, this.coords.z);
   }

   public void setActive(boolean isActive) {
      this.isActive = isActive;
      this.markDirty();
      this.checkEmpty();
   }

   public boolean isActive() {
      return this.isActive;
   }

   public void target(int yLevel) {
      this.targetedYLevel = yLevel;
      this.targetAvailable = true;
   }

   public boolean isTargetAvailable() {
      return this.targetAvailable;
   }

   public int getTargetedYLevel() {
      return this.targetedYLevel;
   }

   public void initNames(Level level) {
      Integer prevLevel = null;

      for (int i = 0; i < this.contacts.size(); i++) {
         Integer y = this.contacts.get(i);
         BlockPos pos = this.contactAt(y);
         BlockEntity currentLevel = level.getBlockEntity(pos);
         if (currentLevel instanceof ElevatorContactBlockEntity) {
            ElevatorContactBlockEntity ecbe = (ElevatorContactBlockEntity)currentLevel;
            Integer currentLevelx = null;
            if (!ecbe.shortName.isBlank()) {
               Integer tryValueOf = tryValueOf(ecbe.shortName);
               if (tryValueOf != null) {
                  currentLevelx = tryValueOf;
               }

               if (currentLevelx == null) {
                  continue;
               }
            }

            if (prevLevel != null) {
               currentLevelx = prevLevel + 1;
            }

            Integer nextLevel = null;

            for (int peekI = i + 1; peekI < this.contacts.size(); peekI++) {
               BlockPos peekPos = this.contactAt(this.contacts.get(peekI));
               BlockEntity tryValueOfx = level.getBlockEntity(peekPos);
               if (tryValueOfx instanceof ElevatorContactBlockEntity) {
                  ElevatorContactBlockEntity peekEcbe = (ElevatorContactBlockEntity)tryValueOfx;
                  Integer tryValueOfxx = tryValueOf(peekEcbe.shortName);
                  if (tryValueOfxx != null) {
                     if (currentLevelx != null && currentLevelx >= tryValueOfxx) {
                        peekEcbe.shortName = "";
                     } else {
                        nextLevel = tryValueOfxx;
                     }
                     break;
                  }
               }
            }

            if (currentLevelx == null) {
               currentLevelx = nextLevel != null ? nextLevel - 1 : 0;
            }

            ecbe.updateName(String.valueOf(currentLevelx), ecbe.longName);
            prevLevel = currentLevelx;
         }
      }
   }

   private static Integer tryValueOf(String floorName) {
      try {
         return Integer.valueOf(floorName, 10);
      } catch (NumberFormatException var2) {
         return null;
      }
   }

   public void add(BlockPos contactPos) {
      int coord = contactPos.getY();
      if (!this.contacts.contains(coord)) {
         int index = 0;

         while (index < this.contacts.size() && this.contacts.get(index) <= coord) {
            index++;
         }

         this.contacts.add(index, coord);
         this.namesChanged();
      }
   }

   public void remove(BlockPos contactPos) {
      this.contacts.remove(Integer.valueOf(contactPos.getY()));
      this.checkEmpty();
      this.namesChanged();
   }

   private void checkEmpty() {
      if (this.contacts.isEmpty() && !this.isActive()) {
         ((Map)LOADED_COLUMNS.get(this.level)).remove(this.coords);
      }
   }

   public static record ColumnCoords(int x, int z, Direction side) {
      public ElevatorColumn.ColumnCoords relative(BlockPos anchor) {
         return new ElevatorColumn.ColumnCoords(this.x + anchor.getX(), this.z + anchor.getZ(), this.side);
      }

      public CompoundTag write() {
         CompoundTag tag = new CompoundTag();
         tag.putInt("X", this.x);
         tag.putInt("Z", this.z);
         NBTHelper.writeEnum(tag, "Side", this.side);
         return tag;
      }

      public static ElevatorColumn.ColumnCoords read(CompoundTag tag) {
         int x = tag.getInt("X");
         int z = tag.getInt("Z");
         Direction side = (Direction)NBTHelper.readEnum(tag, "Side", Direction.class);
         return new ElevatorColumn.ColumnCoords(x, z, side);
      }
   }
}
