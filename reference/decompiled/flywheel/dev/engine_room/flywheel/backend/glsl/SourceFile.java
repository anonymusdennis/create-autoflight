package dev.engine_room.flywheel.backend.glsl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Pair;
import dev.engine_room.flywheel.backend.glsl.span.Span;
import dev.engine_room.flywheel.backend.glsl.span.StringSpan;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class SourceFile implements SourceComponent {
   public final ResourceLocation name;
   public final SourceLines source;
   public final ImmutableList<Import> imports;
   public final List<SourceFile> included;
   public final String finalSource;

   private SourceFile(ResourceLocation name, SourceLines source, ImmutableList<Import> imports, List<SourceFile> included, String finalSource) {
      this.name = name;
      this.source = source;
      this.imports = imports;
      this.included = included;
      this.finalSource = finalSource;
   }

   public static LoadResult empty(ResourceLocation name) {
      return new LoadResult.Success(new SourceFile(name, new SourceLines(name, ""), ImmutableList.of(), ImmutableList.of(), ""));
   }

   public static LoadResult parse(Function<ResourceLocation, LoadResult> sourceFinder, ResourceLocation name, String stringSource) {
      SourceLines source = new SourceLines(name, stringSource);
      ImmutableList<Import> imports = Import.parseImports(source);
      List<SourceFile> included = new ArrayList<>();
      List<Pair<Span, LoadError>> failures = new ArrayList<>();
      Set<String> seen = new HashSet<>();
      UnmodifiableIterator finalSource = imports.iterator();

      while (finalSource.hasNext()) {
         Import i = (Import)finalSource.next();
         Span fileSpan = i.file();
         String string = fileSpan.toString();
         if (seen.add(string)) {
            ResourceLocation location;
            try {
               location = ResourceUtil.parseFlywheelDefault(string);
            } catch (ResourceLocationException var16) {
               failures.add(Pair.of(fileSpan, new LoadError.MalformedInclude(var16)));
               continue;
            }

            LoadResult result = sourceFinder.apply(location);
            if (result instanceof LoadResult.Success s) {
               included.add(s.unwrap());
            } else if (result instanceof LoadResult.Failure e) {
               failures.add(Pair.of(fileSpan, e.error()));
            }
         }
      }

      if (!failures.isEmpty()) {
         return new LoadResult.Failure(new LoadError.IncludeError(name, failures));
      } else {
         String finalSourcex = generateFinalSource(imports, source);
         return new LoadResult.Success(new SourceFile(name, source, imports, included, finalSourcex));
      }
   }

   @Override
   public Collection<? extends SourceComponent> included() {
      return this.included;
   }

   @Override
   public String source() {
      return this.finalSource;
   }

   @Override
   public String name() {
      return this.name.toString();
   }

   public Span getLineSpanNoWhitespace(int line) {
      int begin = this.source.lineStartIndex(line);
      int end = begin + this.source.lineString(line).length();

      while (begin < end && Character.isWhitespace(this.source.charAt(begin))) {
         begin++;
      }

      return new StringSpan(this.source, begin, end);
   }

   public Span getLineSpanMatching(int line, @Nullable String match) {
      if (match == null) {
         return this.getLineSpanNoWhitespace(line);
      } else {
         int spanBegin = this.source.lineString(line).indexOf(match);
         if (spanBegin == -1) {
            return this.getLineSpanNoWhitespace(line);
         } else {
            int begin = this.source.lineStartIndex(line) + spanBegin;
            int end = begin + match.length();
            return new StringSpan(this.source, begin, end);
         }
      }
   }

   @Override
   public String toString() {
      return this.name.toString();
   }

   @Override
   public boolean equals(Object o) {
      return this == o;
   }

   @Override
   public int hashCode() {
      return System.identityHashCode(this);
   }

   private static String generateFinalSource(ImmutableList<Import> imports, SourceLines source) {
      StringBuilder out = new StringBuilder();
      int lastEnd = 0;
      UnmodifiableIterator var4 = imports.iterator();

      while (var4.hasNext()) {
         Import include = (Import)var4.next();
         Span loc = include.self();
         out.append(source, lastEnd, loc.startIndex());
         lastEnd = loc.endIndex();
      }

      out.append(source, lastEnd, source.length());
      return out.toString();
   }
}
