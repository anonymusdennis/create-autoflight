package dev.engine_room.flywheel.backend.glsl.generate;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public record FnSignature(String returnType, String name, ImmutableList<Pair<String, String>> args) {
   public static FnSignature.Builder create() {
      return new FnSignature.Builder();
   }

   public static FnSignature of(String returnType, String name) {
      return create().returnType(returnType).name(name).build();
   }

   public static FnSignature ofVoid(String name) {
      return new FnSignature("void", name, ImmutableList.of());
   }

   public Collection<? extends GlslExpr> createArgExpressions() {
      return this.args.stream().<String>map(Pair::getSecond).map(GlslExpr::variable).collect(Collectors.toList());
   }

   public boolean isVoid() {
      return "void".equals(this.returnType);
   }

   public String fullDeclaration() {
      return this.returnType
         + " "
         + this.name
         + "("
         + this.args.stream().map(p -> (String)p.getFirst() + " " + (String)p.getSecond()).collect(Collectors.joining(", "))
         + ")";
   }

   public String signatureDeclaration() {
      return this.returnType + " " + this.name + "(" + this.args.stream().<CharSequence>map(Pair::getFirst).collect(Collectors.joining(", ")) + ")";
   }

   public static class Builder {
      @Nullable
      private String returnType;
      @Nullable
      private String name;
      private final com.google.common.collect.ImmutableList.Builder<Pair<String, String>> args = ImmutableList.builder();

      public FnSignature.Builder returnType(String returnType) {
         this.returnType = returnType;
         return this;
      }

      public FnSignature.Builder name(String name) {
         this.name = name;
         return this;
      }

      public FnSignature.Builder arg(String type, String name) {
         this.args.add(Pair.of(type, name));
         return this;
      }

      public FnSignature build() {
         if (this.returnType == null) {
            throw new IllegalStateException("returnType not set");
         } else if (this.name == null) {
            throw new IllegalStateException("name not set");
         } else {
            return new FnSignature(this.returnType, this.name, this.args.build());
         }
      }
   }
}
