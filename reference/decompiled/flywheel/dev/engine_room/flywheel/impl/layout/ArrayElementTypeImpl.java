package dev.engine_room.flywheel.impl.layout;

import dev.engine_room.flywheel.api.layout.ArrayElementType;
import dev.engine_room.flywheel.api.layout.ElementType;
import org.jetbrains.annotations.Range;

record ArrayElementTypeImpl(ElementType innerType, @Range(from = 1L,to = 256L) int length, int byteSize, int byteAlignment) implements ArrayElementType {
   static ArrayElementTypeImpl create(ElementType innerType, @Range(from = 1L,to = 256L) int length) {
      if (length >= 1 && length <= 256) {
         return new ArrayElementTypeImpl(innerType, length, innerType.byteSize() * length, innerType.byteAlignment());
      } else {
         throw new IllegalArgumentException("Array element length must be in range [1, 256]!");
      }
   }
}
