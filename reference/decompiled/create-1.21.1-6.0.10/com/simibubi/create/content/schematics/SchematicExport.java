package com.simibubi.create.content.schematics;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.FilesHelper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SchematicExport {
   @Nullable
   public static SchematicExport.SchematicExportResult saveSchematic(Path dir, String fileName, boolean overwrite, Level level, BlockPos first, BlockPos second) {
      BoundingBox bb = BoundingBox.fromCorners(first, second);
      BlockPos origin = new BlockPos(bb.minX(), bb.minY(), bb.minZ());
      BlockPos bounds = new BlockPos(bb.getXSpan(), bb.getYSpan(), bb.getZSpan());
      StructureTemplate structure = new StructureTemplate();
      structure.fillFromWorld(level, origin, bounds, true, Blocks.AIR);
      CompoundTag data = structure.save(new CompoundTag());
      SchematicAndQuillItem.replaceStructureVoidWithAir(data);
      SchematicAndQuillItem.clampGlueBoxes(level, new AABB(Vec3.atLowerCornerOf(origin), Vec3.atLowerCornerOf(origin.offset(bounds))), data);
      if (fileName.isEmpty()) {
         fileName = CreateLang.translateDirect("schematicAndQuill.fallbackName").getString();
      }

      if (!overwrite) {
         fileName = FilesHelper.findFirstValidFilename(fileName, dir, "nbt");
      }

      if (!fileName.endsWith(".nbt")) {
         fileName = fileName + ".nbt";
      }

      Path file = dir.resolve(fileName).toAbsolutePath();

      try {
         Files.createDirectories(dir);
         boolean overwritten = Files.deleteIfExists(file);

         try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE)) {
            NbtIo.writeCompressed(data, out);
         }

         return new SchematicExport.SchematicExportResult(file, dir, fileName, overwritten, origin, bounds);
      } catch (IOException var18) {
         Create.LOGGER.error("An error occurred while saving schematic [" + fileName + "]", var18);
         return null;
      }
   }

   public static record SchematicExportResult(Path file, Path dir, String fileName, boolean overwritten, BlockPos origin, BlockPos bounds) {
   }
}
