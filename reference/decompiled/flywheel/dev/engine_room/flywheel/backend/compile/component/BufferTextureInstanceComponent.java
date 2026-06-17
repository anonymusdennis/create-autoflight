package dev.engine_room.flywheel.backend.compile.component;

import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.Layout;
import dev.engine_room.flywheel.backend.glsl.generate.FnSignature;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBlock;
import dev.engine_room.flywheel.backend.glsl.generate.GlslBuilder;
import dev.engine_room.flywheel.backend.glsl.generate.GlslExpr;
import dev.engine_room.flywheel.backend.glsl.generate.GlslStmt;
import dev.engine_room.flywheel.lib.math.MoreMath;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import java.util.ArrayList;

public class BufferTextureInstanceComponent extends InstanceAssemblerComponent {
   private static final String[] SWIZZLE_SELECTORS = new String[]{"x", "y", "z", "w"};

   public BufferTextureInstanceComponent(InstanceType<?> type) {
      super(type);
   }

   @Override
   public String name() {
      return ResourceUtil.rl("buffer_texture_instance_assembler").toString();
   }

   @Override
   protected void generateUnpacking(GlslBuilder builder) {
      GlslBlock fnBody = new GlslBlock();
      int texels = MoreMath.ceilingDiv(this.layout.byteSize(), 16);
      fnBody.add(GlslStmt.raw("int base = index * " + texels + ";"));

      for (int i = 0; i < texels; i++) {
         fnBody.add(GlslStmt.raw("uvec4 u" + i + " = texelFetch(_flw_instances, base + " + i + ");"));
      }

      ArrayList<GlslExpr> unpackArgs = new ArrayList<>();

      for (Layout.Element element : this.layout.elements()) {
         unpackArgs.add(this.unpackElement(element));
      }

      fnBody.ret(GlslExpr.call("FlwInstance", unpackArgs));
      builder.uniform().type("usamplerBuffer").name("_flw_instances");
      builder.blankLine();
      builder.function().signature(FnSignature.create().returnType("FlwInstance").name("_flw_unpackInstance").arg("int", "index").build()).body(fnBody);
   }

   @Override
   protected GlslExpr access(int uintOffset) {
      return GlslExpr.variable("u" + (uintOffset >> 2)).swizzle(SWIZZLE_SELECTORS[uintOffset & 3]);
   }
}
