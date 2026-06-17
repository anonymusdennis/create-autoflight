package dev.engine_room.flywheel.api.layout;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface Layout {
   int MAX_ELEMENT_NAME_LENGTH = 896;

   @Unmodifiable
   List<Layout.Element> elements();

   @Unmodifiable
   Map<String, Layout.Element> asMap();

   int byteSize();

   int byteAlignment();

   @NonExtendable
   public interface Element {
      String name();

      ElementType type();

      int byteOffset();

      int paddedByteSize();

      int paddingByteSize();
   }
}
