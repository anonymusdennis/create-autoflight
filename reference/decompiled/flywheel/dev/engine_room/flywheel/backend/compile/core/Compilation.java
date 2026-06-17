package dev.engine_room.flywheel.backend.compile.core;

import dev.engine_room.flywheel.backend.compile.FlwPrograms;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.shader.GlShader;
import dev.engine_room.flywheel.backend.gl.shader.ShaderType;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import dev.engine_room.flywheel.backend.glsl.SourceFile;
import dev.engine_room.flywheel.lib.util.StringUtil;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20;

public class Compilation {
   public static final boolean DUMP_SHADER_SOURCE = System.getProperty("flw.dumpShaderSource") != null;
   private final List<SourceFile> files = new ArrayList<>();
   private final StringBuilder generatedSource = new StringBuilder();
   private final StringBuilder fullSource = new StringBuilder();
   private int generatedLines = 0;

   public ShaderResult compile(ShaderType shaderType, String name) {
      int handle = GL20.glCreateShader(shaderType.glEnum);
      String source = this.fullSource.toString();
      GlCompat.safeShaderSource(handle, source);
      GL20.glCompileShader(handle);
      String shaderName = name + "." + shaderType.extension;
      dumpSource(source, shaderName);
      String infoLog = GL20.glGetShaderInfoLog(handle);
      if (compiledSuccessfully(handle)) {
         return ShaderResult.success(new GlShader(handle, shaderType, shaderName), infoLog);
      } else {
         GL20.glDeleteShader(handle);
         return ShaderResult.failure(new FailedCompilation(shaderName, this.files, this.generatedSource.toString(), source, infoLog));
      }
   }

   public void version(GlslVersion version) {
      this.fullSource.append("#version ").append(version.version).append('\n');
   }

   public void enableExtension(String ext) {
      this.fullSource.append("#extension ").append(ext).append(" : enable\n");
   }

   public void requireExtension(String ext) {
      this.fullSource.append("#extension ").append(ext).append(" : require\n");
   }

   public void define(String key, String value) {
      this.fullSource.append("#define ").append(key).append(' ').append(value).append('\n');
   }

   public void define(String key) {
      this.fullSource.append("#define ").append(key).append('\n');
   }

   public void appendComponent(SourceComponent component) {
      String source = component.source();
      this.appendHeader(component, source);
      this.fullSource.append(source);
   }

   private void appendHeader(SourceComponent component, String source) {
      if (component instanceof SourceFile file) {
         int fileId = this.files.size() + 1;
         this.files.add(file);
         this.fullSource.append("\n#line 0 ").append(fileId).append(" // ").append(file.name()).append('\n');
      } else {
         this.generatedSource.append(source).append('\n');
         this.fullSource.append("\n#line ").append(this.generatedLines).append(" 0 // (generated) ").append(component.name()).append('\n');
         this.generatedLines = this.generatedLines + StringUtil.countLines(source);
      }
   }

   private static void dumpSource(String source, String fileName) {
      if (DUMP_SHADER_SOURCE) {
         File file = new File(new File(Minecraft.getInstance().gameDirectory, "flywheel_sources"), fileName);
         file.getParentFile().mkdirs();

         try (FileWriter writer = new FileWriter(file)) {
            writer.write(source);
         } catch (Exception var8) {
            FlwPrograms.LOGGER.error("Could not dump source.", var8);
         }
      }
   }

   public static boolean compiledSuccessfully(int handle) {
      return GL20.glGetShaderi(handle, 35713) == 1;
   }
}
