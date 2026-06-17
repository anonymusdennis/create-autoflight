package com.simibubi.create.content.contraptions.chassis;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSpecialTextures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

public class ChassisRangeDisplay {
   private static final int DISPLAY_TIME = 200;
   private static ChassisRangeDisplay.GroupEntry lastHoveredGroup = null;
   static Map<BlockPos, ChassisRangeDisplay.Entry> entries = new HashMap<>();
   static List<ChassisRangeDisplay.GroupEntry> groupEntries = new ArrayList<>();

   public static void tick() {
      Player player = Minecraft.getInstance().player;
      Level world = Minecraft.getInstance().level;
      boolean hasWrench = AllItems.WRENCH.isIn(player.getMainHandItem());
      Iterator<BlockPos> iterator = entries.keySet().iterator();

      while (iterator.hasNext()) {
         BlockPos pos = iterator.next();
         ChassisRangeDisplay.Entry entry = entries.get(pos);
         if (tickEntry(entry, hasWrench)) {
            iterator.remove();
         }

         Outliner.getInstance().keep(entry.getOutlineKey());
      }

      iterator = groupEntries.iterator();

      while (iterator.hasNext()) {
         ChassisRangeDisplay.GroupEntry group = (ChassisRangeDisplay.GroupEntry)iterator.next();
         if (tickEntry(group, hasWrench)) {
            iterator.remove();
            if (group == lastHoveredGroup) {
               lastHoveredGroup = null;
            }
         }

         Outliner.getInstance().keep(group.getOutlineKey());
      }

      if (hasWrench) {
         if (Minecraft.getInstance().hitResult instanceof BlockHitResult ray) {
            BlockPos var16 = ray.getBlockPos();
            BlockEntity blockEntity = world.getBlockEntity(var16);
            if (blockEntity != null && !blockEntity.isRemoved()) {
               if (blockEntity instanceof ChassisBlockEntity chassisBlockEntity) {
                  boolean ctrl = AllKeys.ctrlDown();
                  if (ctrl) {
                     ChassisRangeDisplay.GroupEntry existingGroupForPos = getExistingGroupForPos(var16);
                     if (existingGroupForPos != null) {
                        for (ChassisBlockEntity included : existingGroupForPos.includedBEs) {
                           entries.remove(included.getBlockPos());
                        }

                        existingGroupForPos.timer = 200;
                        return;
                     }
                  }

                  if (!entries.containsKey(var16) || ctrl) {
                     display(chassisBlockEntity);
                  } else if (!ctrl) {
                     entries.get(var16).timer = 200;
                  }
               }
            }
         }
      }
   }

   private static boolean tickEntry(ChassisRangeDisplay.Entry entry, boolean hasWrench) {
      ChassisBlockEntity chassisBlockEntity = entry.be;
      Level beWorld = chassisBlockEntity.getLevel();
      Level world = Minecraft.getInstance().level;
      if (chassisBlockEntity.isRemoved() || beWorld == null || beWorld != world || !world.isLoaded(chassisBlockEntity.getBlockPos())) {
         return true;
      } else if (!hasWrench && entry.timer > 20) {
         entry.timer = 20;
         return false;
      } else {
         entry.timer--;
         return entry.timer == 0;
      }
   }

   public static void display(ChassisBlockEntity chassis) {
      if (!AllKeys.ctrlDown()) {
         BlockPos pos = chassis.getBlockPos();
         ChassisRangeDisplay.GroupEntry entry = getExistingGroupForPos(pos);
         if (entry != null) {
            Outliner.getInstance().remove(entry.getOutlineKey());
         }

         groupEntries.clear();
         entries.clear();
         entries.put(pos, new ChassisRangeDisplay.Entry(chassis));
      } else {
         ChassisRangeDisplay.GroupEntry hoveredGroup = new ChassisRangeDisplay.GroupEntry(chassis);

         for (ChassisBlockEntity included : hoveredGroup.includedBEs) {
            Outliner.getInstance().remove(Pair.of(included.getBlockPos(), 1));
         }

         groupEntries.forEach(entry -> Outliner.getInstance().remove(entry.getOutlineKey()));
         groupEntries.clear();
         entries.clear();
         groupEntries.add(hoveredGroup);
      }
   }

   private static ChassisRangeDisplay.GroupEntry getExistingGroupForPos(BlockPos pos) {
      for (ChassisRangeDisplay.GroupEntry groupEntry : groupEntries) {
         for (ChassisBlockEntity chassis : groupEntry.includedBEs) {
            if (pos.equals(chassis.getBlockPos())) {
               return groupEntry;
            }
         }
      }

      return null;
   }

   private static class Entry {
      ChassisBlockEntity be;
      int timer;

      public Entry(ChassisBlockEntity be) {
         this.be = be;
         this.timer = 200;
         Outliner.getInstance()
            .showCluster(this.getOutlineKey(), this.createSelection(be))
            .colored(16777215)
            .disableLineNormals()
            .lineWidth(0.0625F)
            .withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED);
      }

      protected Object getOutlineKey() {
         return Pair.of(this.be.getBlockPos(), 1);
      }

      protected Set<BlockPos> createSelection(ChassisBlockEntity chassis) {
         Set<BlockPos> positions = new HashSet<>();
         List<BlockPos> includedBlockPositions = chassis.getIncludedBlockPositions(null, true);
         if (includedBlockPositions == null) {
            return Collections.emptySet();
         } else {
            positions.addAll(includedBlockPositions);
            return positions;
         }
      }
   }

   private static class GroupEntry extends ChassisRangeDisplay.Entry {
      List<ChassisBlockEntity> includedBEs;

      public GroupEntry(ChassisBlockEntity be) {
         super(be);
      }

      @Override
      protected Object getOutlineKey() {
         return this;
      }

      @Override
      protected Set<BlockPos> createSelection(ChassisBlockEntity chassis) {
         Set<BlockPos> list = new HashSet<>();
         this.includedBEs = this.be.collectChassisGroup();
         if (this.includedBEs == null) {
            return list;
         } else {
            for (ChassisBlockEntity chassisBlockEntity : this.includedBEs) {
               list.addAll(super.createSelection(chassisBlockEntity));
            }

            return list;
         }
      }
   }
}
