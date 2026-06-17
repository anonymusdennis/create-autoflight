package dev.engine_room.flywheel.backend.glsl.generate;

import com.mojang.datafixers.util.Pair;
import dev.engine_room.flywheel.lib.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public class GlslSwitch implements GlslStmt {
   private final GlslExpr on;
   private final List<Pair<GlslExpr, GlslBlock>> cases = new ArrayList<>();
   @Nullable
   private GlslBlock defaultCase = null;

   private GlslSwitch(GlslExpr on) {
      this.on = on;
   }

   public static GlslSwitch on(GlslExpr on) {
      return new GlslSwitch(on);
   }

   public void intCase(int expr, GlslBlock block) {
      this.cases.add(Pair.of(GlslExpr.intLiteral(expr), block));
   }

   public void uintCase(int expr, GlslBlock block) {
      this.cases.add(Pair.of(GlslExpr.uintLiteral(expr), block));
   }

   public void defaultCase(GlslBlock block) {
      this.defaultCase = block;
   }

   @Override
   public String prettyPrint() {
      return "switch (%s) {\n%s\n}".formatted(this.on.prettyPrint(), this.formatCases());
   }

   private String formatCases() {
      String cases = this.cases.stream().map(GlslSwitch::prettyPrintCase).collect(Collectors.joining("\n"));
      if (this.defaultCase != null) {
         cases = cases + "\ndefault:\n" + StringUtil.indent(this.defaultCase.prettyPrint(), 4);
      }

      return cases;
   }

   private static String prettyPrintCase(Pair<GlslExpr, GlslBlock> p) {
      String variant = ((GlslExpr)p.getFirst()).prettyPrint();
      String block = ((GlslBlock)p.getSecond()).prettyPrint();
      return "case %s:\n%s".formatted(variant, StringUtil.indent(block, 4));
   }
}
