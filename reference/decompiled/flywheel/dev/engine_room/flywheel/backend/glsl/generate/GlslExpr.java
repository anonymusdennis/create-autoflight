package dev.engine_room.flywheel.backend.glsl.generate;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface GlslExpr {
   static GlslExpr.Variable variable(String name) {
      return new GlslExpr.Variable(name);
   }

   static GlslExpr.FunctionCall call(String functionName, GlslExpr... args) {
      return new GlslExpr.FunctionCall(functionName, List.of(args));
   }

   static GlslExpr.FunctionCall call(String functionName, Collection<? extends GlslExpr> args) {
      return new GlslExpr.FunctionCall(functionName, args);
   }

   static GlslExpr.FunctionCall0 call(String functionName) {
      return new GlslExpr.FunctionCall0(functionName);
   }

   static GlslExpr intLiteral(int expr) {
      return new GlslExpr.RawLiteral(Integer.toString(expr));
   }

   static GlslExpr uintLiteral(int expr) {
      return new GlslExpr.RawLiteral(Integer.toUnsignedString(expr) + "u");
   }

   static GlslExpr uintHexLiteral(int expr) {
      return new GlslExpr.RawLiteral("0x" + Integer.toHexString(expr) + "u");
   }

   static GlslExpr boolLiteral(boolean expr) {
      return new GlslExpr.RawLiteral(Boolean.toString(expr));
   }

   static GlslExpr floatLiteral(float expr) {
      return new GlslExpr.RawLiteral(Float.toString(expr));
   }

   default GlslExpr.FunctionCall callFunction(String name) {
      return new GlslExpr.FunctionCall(name, this);
   }

   default GlslExpr.FunctionCall cast(String name) {
      return new GlslExpr.FunctionCall(name, this);
   }

   default GlslExpr.Swizzle swizzle(String selection) {
      return new GlslExpr.Swizzle(this, selection);
   }

   default GlslExpr.Access access(String member) {
      return new GlslExpr.Access(this, member);
   }

   default GlslExpr transform(Function<GlslExpr, GlslExpr> f) {
      return f.apply(this);
   }

   default GlslExpr div(float v) {
      return new GlslExpr.Binary(this, floatLiteral(v), BinOp.DIVIDE);
   }

   default GlslExpr sub(int v) {
      return new GlslExpr.Binary(this, uintLiteral(v), BinOp.SUBTRACT);
   }

   default GlslExpr rsh(int by) {
      return (GlslExpr)(by == 0 ? this : new GlslExpr.Binary(this, uintLiteral(by), BinOp.RIGHT_SHIFT));
   }

   default GlslExpr and(int mask) {
      return new GlslExpr.Binary(this, uintHexLiteral(mask), BinOp.BITWISE_AND);
   }

   default GlslExpr xor(int mask) {
      return new GlslExpr.Binary(this, uintHexLiteral(mask), BinOp.BITWISE_XOR);
   }

   default GlslExpr clamp(float from, float to) {
      return new GlslExpr.Clamp(this, floatLiteral(from), floatLiteral(to));
   }

   String prettyPrint();

   public static record Access(GlslExpr target, String argName) implements GlslExpr {
      @Override
      public String prettyPrint() {
         return this.target.prettyPrint() + "." + this.argName;
      }
   }

   public static record Binary(GlslExpr lhs, GlslExpr rhs, BinOp op) implements GlslExpr {
      @Override
      public String prettyPrint() {
         return "(" + this.lhs.prettyPrint() + " " + this.op.op + " " + this.rhs.prettyPrint() + ")";
      }
   }

   public static record Clamp(GlslExpr value, GlslExpr from, GlslExpr to) implements GlslExpr {
      @Override
      public String prettyPrint() {
         return "clamp(" + this.value.prettyPrint() + ", " + this.from.prettyPrint() + ", " + this.to.prettyPrint() + ")";
      }
   }

   public static record FunctionCall(String name, Collection<? extends GlslExpr> args) implements GlslExpr {
      public FunctionCall(String name, GlslExpr target) {
         this(name, ImmutableList.of(target));
      }

      @Override
      public String prettyPrint() {
         String args = this.args.stream().map(GlslExpr::prettyPrint).collect(Collectors.joining(", "));
         return this.name + "(" + args + ")";
      }
   }

   public static record FunctionCall0(String name) implements GlslExpr {
      @Override
      public String prettyPrint() {
         return this.name + "()";
      }
   }

   public static record RawLiteral(String value) implements GlslExpr {
      @Override
      public String prettyPrint() {
         return this.value;
      }
   }

   public static record Swizzle(GlslExpr target, String selection) implements GlslExpr {
      @Override
      public String prettyPrint() {
         return this.target.prettyPrint() + "." + this.selection;
      }
   }

   public static record Variable(String name) implements GlslExpr {
      @Override
      public String prettyPrint() {
         return this.name;
      }
   }
}
