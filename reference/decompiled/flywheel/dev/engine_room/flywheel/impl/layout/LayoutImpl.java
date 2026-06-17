package dev.engine_room.flywheel.impl.layout;

import dev.engine_room.flywheel.api.layout.ElementType;
import dev.engine_room.flywheel.api.layout.Layout;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Unmodifiable;

final class LayoutImpl implements Layout {
   @Unmodifiable
   private final List<Layout.Element> elements;
   @Unmodifiable
   private final Map<String, Layout.Element> map;
   private final int byteSize;
   private final int byteAlignment;

   LayoutImpl(@Unmodifiable List<Layout.Element> elements, int byteSize, int byteAlignment) {
      this.elements = elements;
      Object2ObjectOpenHashMap<String, Layout.Element> map = new Object2ObjectOpenHashMap();

      for (Layout.Element element : this.elements) {
         map.put(element.name(), element);
      }

      map.trim();
      this.map = Collections.unmodifiableMap(map);
      this.byteSize = byteSize;
      this.byteAlignment = byteAlignment;
   }

   @Unmodifiable
   @Override
   public List<Layout.Element> elements() {
      return this.elements;
   }

   @Unmodifiable
   @Override
   public Map<String, Layout.Element> asMap() {
      return this.map;
   }

   @Override
   public int byteSize() {
      return this.byteSize;
   }

   @Override
   public int byteAlignment() {
      return this.byteAlignment;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      return 31 * result + this.elements.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         LayoutImpl other = (LayoutImpl)obj;
         return this.elements.equals(other.elements);
      }
   }

   static record ElementImpl(String name, ElementType type, int byteOffset, int paddedByteSize, int paddingByteSize) implements Layout.Element {
   }
}
