package dev.engine_room.flywheel.backend.glsl.generate;

import com.mojang.datafixers.util.Pair;
import dev.engine_room.flywheel.lib.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GlslUniformBlock implements GlslBuilder.Declaration {
   private String qualifier;
   private String name;
   private final List<Pair<String, String>> members = new ArrayList<>();

   public GlslUniformBlock layout(String qualifier) {
      this.qualifier = qualifier;
      return this;
   }

   public GlslUniformBlock name(String name) {
      this.name = name;
      return this;
   }

   public GlslUniformBlock member(String typeName, String variableName) {
      this.members.add(Pair.of(typeName, variableName));
      return this;
   }

   @Override
   public String prettyPrint() {
      return "layout(%s) uniform %s {\n%s\n};".formatted(this.qualifier, this.name, StringUtil.indent(this.formatMembers(), 4));
   }

   private String formatMembers() {
      return this.members.stream().map(p -> (String)p.getFirst() + " " + (String)p.getSecond() + ";").collect(Collectors.joining("\n"));
   }
}
