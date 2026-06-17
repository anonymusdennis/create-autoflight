package dev.engine_room.flywheel.impl.extension;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public interface LevelExtension {
   Iterable<Entity> flywheel$getAllLoadedEntities();

   static Iterable<Entity> getAllLoadedEntities(Level level) {
      return ((LevelExtension)level).flywheel$getAllLoadedEntities();
   }
}
