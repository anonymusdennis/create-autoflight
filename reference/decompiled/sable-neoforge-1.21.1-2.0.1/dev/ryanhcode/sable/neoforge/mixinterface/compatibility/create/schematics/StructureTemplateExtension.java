package dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.schematics;

import java.util.List;
import java.util.UUID;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public interface StructureTemplateExtension {
   List<StructureTemplateExtension.SubLevelTemplate> sable$getSubLevels();

   public static record SubLevelTemplate(UUID uuid, Vector3d position, Quaterniond orientation, StructureTemplate template) {
   }
}
