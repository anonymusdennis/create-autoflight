package dev.engine_room.flywheel.backend.glsl.generate;

import dev.engine_room.flywheel.lib.util.StringUtil;
import java.util.function.Consumer;

public class GlslFn implements GlslBuilder.Declaration {
   private FnSignature signature;
   private GlslBlock body = new GlslBlock();

   public GlslFn signature(FnSignature signature) {
      this.signature = signature;
      return this;
   }

   public GlslFn body(GlslBlock block) {
      this.body = block;
      return this;
   }

   public GlslFn body(Consumer<GlslBlock> f) {
      f.accept(this.body);
      return this;
   }

   @Override
   public String prettyPrint() {
      return "%s {\n%s\n}".formatted(this.signature.fullDeclaration(), StringUtil.indent(this.body.prettyPrint(), 4));
   }
}
