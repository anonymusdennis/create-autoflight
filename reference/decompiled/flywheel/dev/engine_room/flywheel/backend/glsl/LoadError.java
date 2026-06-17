package dev.engine_room.flywheel.backend.glsl;

import com.mojang.datafixers.util.Pair;
import dev.engine_room.flywheel.backend.glsl.error.ErrorBuilder;
import dev.engine_room.flywheel.backend.glsl.span.Span;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

public sealed interface LoadError
   permits LoadError.CircularDependency,
   LoadError.IncludeError,
   LoadError.IOError,
   LoadError.ResourceError,
   LoadError.MalformedInclude {
   ErrorBuilder generateMessage();

   public static record CircularDependency(ResourceLocation offender, List<ResourceLocation> stack) implements LoadError {
      public String format() {
         return this.stack.stream().dropWhile(l -> !l.equals(this.offender)).<CharSequence>map(ResourceLocation::toString).collect(Collectors.joining(" -> "));
      }

      @Override
      public ErrorBuilder generateMessage() {
         return ErrorBuilder.create().error("files are circularly dependent").note(this.format());
      }
   }

   public static record IOError(ResourceLocation location, IOException exception) implements LoadError {
      @Override
      public ErrorBuilder generateMessage() {
         return this.exception instanceof FileNotFoundException
            ? ErrorBuilder.create().error("\"" + this.location + "\" was not found")
            : ErrorBuilder.create().error("could not load \"" + this.location + "\" due to an IO error").note(this.exception.toString());
      }
   }

   public static record IncludeError(ResourceLocation location, List<Pair<Span, LoadError>> innerErrors) implements LoadError {
      @Override
      public ErrorBuilder generateMessage() {
         ErrorBuilder out = ErrorBuilder.create().error("could not load \"" + this.location + "\"").pointAtFile(this.location);

         for (Pair<Span, LoadError> innerError : this.innerErrors) {
            ErrorBuilder err = ((LoadError)innerError.getSecond()).generateMessage();
            out.pointAt((Span)innerError.getFirst()).nested(err);
         }

         return out;
      }
   }

   public static record MalformedInclude(ResourceLocationException exception) implements LoadError {
      @Override
      public ErrorBuilder generateMessage() {
         return ErrorBuilder.create().error(this.exception.toString());
      }
   }

   public static record ResourceError(ResourceLocation location) implements LoadError {
      @Override
      public ErrorBuilder generateMessage() {
         return ErrorBuilder.create().error("\"" + this.location + "\" was not found");
      }
   }
}
