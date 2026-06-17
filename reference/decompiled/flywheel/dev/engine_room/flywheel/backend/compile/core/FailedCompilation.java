package dev.engine_room.flywheel.backend.compile.core;

import dev.engine_room.flywheel.backend.glsl.SourceFile;
import dev.engine_room.flywheel.backend.glsl.SourceLines;
import dev.engine_room.flywheel.backend.glsl.error.ErrorBuilder;
import dev.engine_room.flywheel.backend.glsl.error.ErrorLevel;
import dev.engine_room.flywheel.backend.glsl.span.Span;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import dev.engine_room.flywheel.lib.util.StringUtil;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class FailedCompilation {
   public static final ResourceLocation GENERATED_SOURCE_NAME = ResourceUtil.rl("generated_source");
   private static final Pattern PATTERN_ONE = Pattern.compile("(\\d+)\\((\\d+)\\) : (.*)");
   private static final Pattern PATTERN_TWO = Pattern.compile("(\\w+): (\\d+):(\\d+):(?: '(.+?)' :)?(.*)");
   private final List<SourceFile> files;
   private final SourceLines generatedSource;
   private final String errorLog;
   private final String shaderName;
   private final String completeSource;

   public FailedCompilation(String shaderName, List<SourceFile> files, String generatedSource, String completeSource, String errorLog) {
      this.shaderName = shaderName;
      this.files = files;
      this.generatedSource = new SourceLines(GENERATED_SOURCE_NAME, generatedSource);
      this.completeSource = completeSource;
      this.errorLog = errorLog;
   }

   public String generateMessage() {
      return "\u001b[1;91mFailed to compile " + this.shaderName + ":\n" + this.errorString();
   }

   public String errorString() {
      return this.errorStream().map(ErrorBuilder::build).collect(Collectors.joining("\n"));
   }

   private Stream<ErrorBuilder> errorStream() {
      return this.errorLog.lines().mapMulti(this::interpretLine);
   }

   private void interpretLine(String s, Consumer<ErrorBuilder> out) {
      if (!s.isEmpty()) {
         try {
            Matcher matcher = PATTERN_ONE.matcher(s);
            if (matcher.find()) {
               out.accept(this.interpretPattern1(matcher));
               return;
            }

            matcher = PATTERN_TWO.matcher(s);
            if (matcher.find()) {
               out.accept(this.interpretPattern2(matcher));
               return;
            }
         } catch (Throwable var4) {
         }

         out.accept(ErrorBuilder.create().error(s));
      }
   }

   private ErrorBuilder interpretPattern1(Matcher matcher) {
      int fileId = Integer.parseInt(matcher.group(1));
      int lineNo = Integer.parseInt(matcher.group(2));
      String msg = StringUtil.trimPrefix(matcher.group(3), "error").stripLeading();
      return fileId == 0 ? this.interpretGeneratedError(ErrorLevel.ERROR, lineNo, msg) : this.interpretSourceError(fileId, lineNo, msg);
   }

   private ErrorBuilder interpretPattern2(Matcher matcher) {
      ErrorLevel errorLevel = parseErrorLevel(matcher.group(1));
      int fileId = Integer.parseInt(matcher.group(2));
      int lineNo = Integer.parseInt(matcher.group(3)) - 1;
      String span = matcher.group(4);
      String msg = matcher.group(5).trim();
      return fileId == 0 ? this.interpretGeneratedError(errorLevel, lineNo, msg) : this.interpretWithSpan(errorLevel, fileId, lineNo, span, msg);
   }

   private ErrorBuilder interpretSourceError(int fileId, int lineNo, String msg) {
      SourceFile sourceFile = this.files.get(fileId - 1);
      Span span = sourceFile.getLineSpanNoWhitespace(lineNo);
      return ErrorBuilder.create().error(msg).pointAtFile(sourceFile).pointAt(span, 1);
   }

   private ErrorBuilder interpretWithSpan(ErrorLevel errorLevel, int fileId, int lineNo, @Nullable String span, String msg) {
      SourceFile sourceFile = this.files.get(fileId - 1);
      Span errorSpan = sourceFile.getLineSpanMatching(lineNo, span);
      return ErrorBuilder.create().header(errorLevel, msg).pointAtFile(sourceFile).pointAt(errorSpan, 1);
   }

   private ErrorBuilder interpretGeneratedError(ErrorLevel errorLevel, int lineNo, String msg) {
      return ErrorBuilder.create()
         .header(errorLevel, msg)
         .pointAtFile("[in generated source]")
         .pointAtLine(this.generatedSource, lineNo, 1)
         .note("This generally indicates a bug in Flywheel, not your shader code.");
   }

   private static ErrorLevel parseErrorLevel(String level) {
      String var1 = level.toLowerCase(Locale.ROOT);

      return switch (var1) {
         case "error" -> ErrorLevel.ERROR;
         case "warning", "warn" -> ErrorLevel.WARN;
         default -> ErrorLevel.NOTE;
      };
   }
}
