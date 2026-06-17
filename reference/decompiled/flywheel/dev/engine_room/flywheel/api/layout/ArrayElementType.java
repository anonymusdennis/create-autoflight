package dev.engine_room.flywheel.api.layout;

import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public non-sealed interface ArrayElementType extends ElementType {
   ElementType innerType();

   @Range(
      from = 1L,
      to = 256L
   )
   int length();
}
