package net.createmod.ponder.api.registration;

import java.util.List;
import java.util.Set;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.resources.ResourceLocation;

public interface TagRegistryAccess {
   PonderTag getRegisteredTag(ResourceLocation var1);

   List<PonderTag> getListedTags();

   Set<PonderTag> getTags(ResourceLocation var1);

   Set<ResourceLocation> getItems(ResourceLocation var1);

   Set<ResourceLocation> getItems(PonderTag var1);
}
