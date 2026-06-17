package dev.engine_room.flywheel.api.layout;

import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public non-sealed interface MatrixElementType extends ElementType {
   FloatRepr repr();

   @Range(
      from = 2L,
      to = 4L
   )
   int rows();

   @Range(
      from = 2L,
      to = 4L
   )
   int columns();
}
