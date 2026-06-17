package dev.engine_room.flywheel.backend.glsl.generate;

public interface GlslStmt {
   GlslStmt BREAK = () -> "break;";
   GlslStmt CONTINUE = () -> "continue;";
   GlslStmt RETURN = () -> "return;";

   static GlslStmt eval(GlslExpr expr) {
      return new GlslStmt.Eval(expr);
   }

   static GlslStmt ret(GlslExpr value) {
      return new GlslStmt.Return(value);
   }

   static GlslStmt raw(String s) {
      return new GlslStmt.Raw(s);
   }

   String prettyPrint();

   public static record Eval(GlslExpr expr) implements GlslStmt {
      @Override
      public String prettyPrint() {
         return this.expr.prettyPrint() + ";";
      }
   }

   public static record Raw(String glsl) implements GlslStmt {
      @Override
      public String prettyPrint() {
         return this.glsl;
      }
   }

   public static record Return(GlslExpr expr) implements GlslStmt {
      @Override
      public String prettyPrint() {
         return "return " + this.expr.prettyPrint() + ";";
      }
   }
}
