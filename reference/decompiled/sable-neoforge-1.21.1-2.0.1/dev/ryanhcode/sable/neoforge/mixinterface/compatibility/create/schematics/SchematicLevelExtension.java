package dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.schematics;

import java.util.List;
import java.util.UUID;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public interface SchematicLevelExtension {
   List<SchematicLevelExtension.SchematicSubLevel> sable$getSubLevels();

   public static record SchematicSubLevel(UUID uuid, Vector3d position, Quaterniond orientation, SchematicLevel level) {
   }
}
