package dev.engine_room.flywheel.backend.glsl.generate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GlslBlock {
   private final List<GlslStmt> body = new ArrayList<>();

   public static GlslBlock create() {
      return new GlslBlock();
   }

   public GlslBlock add(GlslStmt stmt) {
      this.body.add(stmt);
      return this;
   }

   public GlslBlock eval(GlslExpr expr) {
      return this.add(GlslStmt.eval(expr));
   }

   public GlslBlock ret(GlslExpr call) {
      this.add(GlslStmt.ret(call));
      return this;
   }

   public GlslBlock breakStmt() {
      this.add(GlslStmt.BREAK);
      return this;
   }

   public String prettyPrint() {
      return this.body.stream().map(GlslStmt::prettyPrint).collect(Collectors.joining("\n"));
   }

   public void raw(String s) {
      this.add(GlslStmt.raw(s));
   }
}
