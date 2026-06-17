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

public class SsboInstanceComponent extends InstanceAssemblerComponent {
   public SsboInstanceComponent(InstanceType<?> type) {
      super(type);
   }

   @Override
   public String name() {
      return ResourceUtil.rl("ssbo_instance_assembler").toString();
   }

   @Override
   protected void generateUnpacking(GlslBuilder builder) {
      GlslBlock fnBody = new GlslBlock();
      int uintCount = MoreMath.ceilingDiv(this.layout.byteSize(), 4);
      fnBody.add(GlslStmt.raw("uint base = index * " + uintCount + "u;"));

      for (int i = 0; i < uintCount; i++) {
         fnBody.add(GlslStmt.raw("uint u" + i + " = _flw_instances[base + " + i + "u];"));
      }

      ArrayList<GlslExpr> unpackArgs = new ArrayList<>();

      for (Layout.Element element : this.layout.elements()) {
         unpackArgs.add(this.unpackElement(element));
      }

      fnBody.ret(GlslExpr.call("FlwInstance", unpackArgs));
      builder._raw("layout(std430, binding = 1) restrict readonly buffer InstanceBuffer {\n    uint _flw_instances[];\n};");
      builder.blankLine();
      builder.function().signature(FnSignature.create().returnType("FlwInstance").name("_flw_unpackInstance").arg("uint", "index").build()).body(fnBody);
   }

   @Override
   protected GlslExpr access(int uintOffset) {
      return GlslExpr.variable("u" + uintOffset);
   }
}
