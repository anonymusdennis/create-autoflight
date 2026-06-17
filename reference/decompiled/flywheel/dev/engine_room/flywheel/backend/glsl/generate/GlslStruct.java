package dev.engine_room.flywheel.backend.glsl.generate;

import com.mojang.datafixers.util.Pair;
import dev.engine_room.flywheel.lib.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GlslStruct implements GlslBuilder.Declaration {
   private String name;
   private final List<Pair<String, String>> fields = new ArrayList<>();

   public GlslStruct name(String name) {
      this.name = name;
      return this;
   }

   public GlslStruct addField(String type, String name) {
      this.fields.add(Pair.of(type, name));
      return this;
   }

   @Override
   public String prettyPrint() {
      return "struct %s {\n%s\n};".formatted(this.name, StringUtil.indent(this.buildFields(), 4));
   }

   private String buildFields() {
      return this.fields.stream().map(p -> (String)p.getFirst() + " " + (String)p.getSecond() + ";").collect(Collectors.joining("\n"));
   }
}
