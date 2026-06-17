package com.simibubi.create.content.equipment.zapper;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class ZapperLog {
   private Level activeWorld;
   private List<List<StructureBlockInfo>> log = new LinkedList<>();

   public void record(Level world, List<BlockPos> positions) {
      if (world != this.activeWorld) {
         this.log.clear();
      }

      this.activeWorld = world;
      List<StructureBlockInfo> blocks = positions.stream().map(pos -> {
         BlockEntity blockEntity = world.getBlockEntity(pos);
         return new StructureBlockInfo(pos, world.getBlockState(pos), blockEntity == null ? null : blockEntity.saveWithFullMetadata(world.registryAccess()));
      }).collect(Collectors.toList());
      this.log.add(0, blocks);
   }

   public void undo() {
   }

   public void redo() {
   }
}
