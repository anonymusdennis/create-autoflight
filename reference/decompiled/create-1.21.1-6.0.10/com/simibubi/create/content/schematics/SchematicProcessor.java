package com.simibubi.create.content.schematics;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllStructureProcessorTypes;
import java.util.Optional;
import net.createmod.catnip.nbt.NBTProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import org.jetbrains.annotations.Nullable;

public class SchematicProcessor extends StructureProcessor {
   public static final SchematicProcessor INSTANCE = new SchematicProcessor();
   public static final MapCodec<SchematicProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

   private SchematicProcessor() {
   }

   @Nullable
   public StructureBlockInfo process(
      LevelReader world,
      BlockPos pos,
      BlockPos anotherPos,
      StructureBlockInfo rawInfo,
      StructureBlockInfo info,
      StructurePlaceSettings settings,
      @Nullable StructureTemplate template
   ) {
      if (info.nbt() != null && info.state().hasBlockEntity()) {
         BlockEntity be = ((EntityBlock)info.state().getBlock()).newBlockEntity(info.pos(), info.state());
         if (be != null) {
            CompoundTag nbt = NBTProcessors.process(info.state(), be, info.nbt(), false);
            if (nbt != info.nbt()) {
               return new StructureBlockInfo(info.pos(), info.state(), nbt);
            }
         }
      }

      return info;
   }

   @Nullable
   public StructureEntityInfo processEntity(
      LevelReader world, BlockPos pos, StructureEntityInfo rawInfo, StructureEntityInfo info, StructurePlaceSettings settings, StructureTemplate template
   ) {
      return EntityType.by(info.nbt).flatMap(type -> {
         if (world instanceof Level) {
            Entity e = type.create((Level)world);
            if (e != null && !e.onlyOpCanSetNbt()) {
               return Optional.of(info);
            }
         }

         return Optional.empty();
      }).orElse(null);
   }

   protected StructureProcessorType<?> getType() {
      return (StructureProcessorType<?>)AllStructureProcessorTypes.SCHEMATIC.get();
   }
}
