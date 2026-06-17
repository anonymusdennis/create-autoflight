package dev.engine_room.flywheel.impl.registry;

import dev.engine_room.flywheel.api.registry.IdRegistry;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceCollection;
import it.unimi.dsi.fastutil.objects.ReferenceCollections;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public class IdRegistryImpl<T> implements IdRegistry<T> {
   private static final ObjectList<IdRegistryImpl<?>> ALL = new ObjectArrayList();
   private final Object2ReferenceMap<ResourceLocation, T> map = Object2ReferenceMaps.synchronize(new Object2ReferenceOpenHashMap());
   private final Reference2ObjectMap<T, ResourceLocation> reverseMap = Reference2ObjectMaps.synchronize(new Reference2ObjectOpenHashMap());
   private final ObjectSet<ResourceLocation> keysView = ObjectSets.unmodifiable(this.map.keySet());
   private final ReferenceCollection<T> valuesView = ReferenceCollections.unmodifiable(this.map.values());
   private boolean frozen;

   public IdRegistryImpl() {
      ALL.add(this);
   }

   @Override
   public void register(ResourceLocation id, T object) {
      if (this.frozen) {
         throw new IllegalStateException("Cannot register to frozen registry!");
      } else {
         T oldValue = (T)this.map.put(id, object);
         if (oldValue != null) {
            throw new IllegalArgumentException("Cannot override registration for ID '" + id + "'!");
         } else {
            ResourceLocation oldId = (ResourceLocation)this.reverseMap.put(object, id);
            if (oldId != null) {
               throw new IllegalArgumentException("Cannot override ID '" + id + "' with registration for ID '" + oldId + "'!");
            }
         }
      }
   }

   @Override
   public <S extends T> S registerAndGet(ResourceLocation id, S object) {
      this.register(id, (T)object);
      return object;
   }

   @Nullable
   @Override
   public T get(ResourceLocation id) {
      return (T)this.map.get(id);
   }

   @Nullable
   @Override
   public ResourceLocation getId(T object) {
      return (ResourceLocation)this.reverseMap.get(object);
   }

   @Override
   public T getOrThrow(ResourceLocation id) {
      T object = this.get(id);
      if (object == null) {
         throw new IllegalArgumentException("Could not find object for ID '" + id + "'!");
      } else {
         return object;
      }
   }

   @Override
   public ResourceLocation getIdOrThrow(T object) {
      ResourceLocation id = this.getId(object);
      if (id == null) {
         throw new IllegalArgumentException("Could not find ID for object!");
      } else {
         return id;
      }
   }

   @UnmodifiableView
   @Override
   public Set<ResourceLocation> getAllIds() {
      return this.keysView;
   }

   @UnmodifiableView
   @Override
   public Collection<T> getAll() {
      return this.valuesView;
   }

   @Override
   public boolean isFrozen() {
      return this.frozen;
   }

   @Override
   public Iterator<T> iterator() {
      return this.getAll().iterator();
   }

   private void freeze() {
      this.frozen = true;
   }

   public static void freezeAll() {
      ObjectListIterator var0 = ALL.iterator();

      while (var0.hasNext()) {
         IdRegistryImpl<?> registry = (IdRegistryImpl<?>)var0.next();
         registry.freeze();
      }
   }
}
