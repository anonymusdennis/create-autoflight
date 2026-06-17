package net.createmod.ponder.api.registration;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.resources.ResourceLocation;

public interface SceneRegistryAccess {
   boolean doScenesExistForId(ResourceLocation var1);

   Collection<Entry<ResourceLocation, StoryBoardEntry>> getRegisteredEntries();

   List<PonderScene> compile(ResourceLocation var1);

   List<PonderScene> compile(Collection<StoryBoardEntry> var1);
}
