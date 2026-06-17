package dev.engine_room.flywheel.impl.layout;

import dev.engine_room.flywheel.api.layout.ValueRepr;
import dev.engine_room.flywheel.api.layout.VectorElementType;
import org.jetbrains.annotations.Range;

record VectorElementTypeImpl(ValueRepr repr, @Range(from = 2L,to = 4L) int size, int byteSize, int byteAlignment) implements VectorElementType {
   static VectorElementTypeImpl create(ValueRepr repr, @Range(from = 2L,to = 4L) int size) {
      if (size >= 2 && size <= 4) {
         return new VectorElementTypeImpl(repr, size, repr.byteSize() * size, repr.byteSize());
      } else {
         throw new IllegalArgumentException("Vector element size must be in range [2, 4]!");
      }
   }
}
