package dev.engine_room.flywheel.backend.compile.component;

import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.Layout;
import dev.engine_room.flywheel.backend.compile.LayoutInterpreter;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import dev.engine_room.flywheel.backend.glsl.generate.GlslStruct;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import java.util.Collection;
import java.util.Collections;

public class InstanceStructComponent implements SourceComponent {
   private static final String STRUCT_NAME = "FlwInstance";
   private final Layout layout;

   public InstanceStructComponent(InstanceType<?> type) {
      this.layout = type.layout();
   }

   @Override
   public String name() {
      return ResourceUtil.rl("instance_struct").toString();
   }

   @Override
   public Collection<? extends SourceComponent> included() {
      return Collections.emptyList();
   }

   @Override
   public String source() {
      GlslBuilder builder = new GlslBuilder();
      GlslStruct instance = builder.struct();
      instance.name("FlwInstance");

      for (Layout.Element element : this.layout.elements()) {
         instance.addField(LayoutInterpreter.typeName(element.type()), element.name());
      }

      builder.blankLine();
      return builder.build();
   }
}
