package net.createmod.ponder.foundation.registration;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.registration.TagRegistryAccess;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public class PonderTagRegistry implements TagRegistryAccess {
   private final PonderLocalization localization;
   private final Multimap<ResourceLocation, ResourceLocation> componentTagMap;
   private final Map<ResourceLocation, PonderTag> registeredTags;
   private final List<PonderTag> listedTags;
   private final PonderTag MISSING = new PonderTag(
      Ponder.asResource("not_registered"), null, Items.BARRIER.getDefaultInstance(), Items.BARRIER.getDefaultInstance()
   );
   private boolean allowRegistration = true;

   public PonderTagRegistry(PonderLocalization localization) {
      this.localization = localization;
      this.componentTagMap = LinkedHashMultimap.create();
      this.registeredTags = new HashMap<>();
      this.listedTags = new ArrayList<>();
   }

   public void clearRegistry() {
      this.componentTagMap.clear();
      this.listedTags.clear();
      this.allowRegistration = true;
   }

   public void registerTag(PonderTag tag) {
      if (!this.allowRegistration) {
         throw new IllegalStateException("Registration Phase has already ended!");
      } else {
         this.registeredTags.put(tag.getId(), tag);
      }
   }

   public void listTag(PonderTag tag) {
      if (!this.allowRegistration) {
         throw new IllegalStateException("Registration Phase has already ended!");
      } else {
         this.listedTags.add(tag);
      }
   }

   public void addTagToComponent(ResourceLocation tag, ResourceLocation item) {
      if (!this.allowRegistration) {
         throw new IllegalStateException("Registration Phase has already ended!");
      } else {
         synchronized (this.componentTagMap) {
            this.componentTagMap.put(item, tag);
         }
      }
   }

   @Override
   public PonderTag getRegisteredTag(ResourceLocation tagLocation) {
      return this.registeredTags.getOrDefault(tagLocation, this.MISSING);
   }

   @Override
   public List<PonderTag> getListedTags() {
      return this.listedTags;
   }

   @Override
   public Set<PonderTag> getTags(ResourceLocation item) {
      return this.componentTagMap.get(item).stream().map(this::getRegisteredTag).collect(Collectors.toUnmodifiableSet());
   }

   @Override
   public Set<ResourceLocation> getItems(ResourceLocation tag) {
      return this.componentTagMap
         .entries()
         .stream()
         .filter(e -> ((ResourceLocation)e.getValue()).equals(tag))
         .map(Entry::getKey)
         .collect(ImmutableSet.toImmutableSet());
   }

   @Override
   public Set<ResourceLocation> getItems(PonderTag tag) {
      return this.getItems(tag.getId());
   }
}
