package dev.engine_room.flywheel.api.layout;

import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public non-sealed interface VectorElementType extends ElementType {
   ValueRepr repr();

   @Range(
      from = 2L,
      to = 4L
   )
   int size();
}
