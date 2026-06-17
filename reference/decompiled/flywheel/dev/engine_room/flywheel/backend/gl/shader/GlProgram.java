package dev.engine_room.flywheel.backend.gl.shader;

import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.logging.LogUtils;
import dev.engine_room.flywheel.backend.gl.GlObject;
import dev.engine_room.flywheel.backend.gl.GlTextureUnit;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.slf4j.Logger;

public class GlProgram extends GlObject {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Object2IntMap<String> uniformLocationCache = new Object2IntOpenHashMap();

   public GlProgram(int handle) {
      this.handle(handle);
   }

   public void bind() {
      ProgramManager.glUseProgram(this.handle());
   }

   public static void unbind() {
      ProgramManager.glUseProgram(0);
   }

   public void setFloat(String glslName, float value) {
      int uniform = this.getUniformLocation(glslName);
      if (uniform >= 0) {
         GL20.glUniform1f(uniform, value);
      }
   }

   public void setVec2(String glslName, float x, float y) {
      int uniform = this.getUniformLocation(glslName);
      if (uniform >= 0) {
         GL20.glUniform2f(uniform, x, y);
      }
   }

   public void setVec3(String glslName, float x, float y, float z) {
      int uniform = this.getUniformLocation(glslName);
      if (uniform >= 0) {
         GL20.glUniform3f(uniform, x, y, z);
      }
   }

   public void setVec4(String glslName, float x, float y, float z, float w) {
      int uniform = this.getUniformLocation(glslName);
      if (uniform >= 0) {
         GL20.glUniform4f(uniform, x, y, z, w);
      }
   }

   public void setMat4(String glslName, Matrix4fc matrix) {
      int uniform = this.getUniformLocation(glslName);
      if (uniform >= 0) {
         GL20.glUniformMatrix4fv(uniform, false, matrix.get(new float[16]));
      }
   }

   public void setMat3(String glslName, Matrix3fc matrix) {
      int uniform = this.getUniformLocation(glslName);
      if (uniform >= 0) {
         GL20.glUniformMatrix3fv(uniform, false, matrix.get(new float[9]));
      }
   }

   public void setBool(String glslName, boolean bool) {
      this.setInt(glslName, bool ? 1 : 0);
   }

   public void setUInt(String glslName, int value) {
      int uniform = this.getUniformLocation(glslName);
      if (uniform >= 0) {
         GL30.glUniform1ui(uniform, value);
      }
   }

   public void setUVec2(String name, int x, int y) {
      int uniform = this.getUniformLocation(name);
      if (uniform >= 0) {
         GL30.glUniform2ui(uniform, x, y);
      }
   }

   public void setInt(String glslName, int value) {
      int uniform = this.getUniformLocation(glslName);
      if (uniform >= 0) {
         GL20.glUniform1i(uniform, value);
      }
   }

   public int getUniformLocation(String uniform) {
      return this.uniformLocationCache.computeIfAbsent(uniform, s -> {
         int index = GL20.glGetUniformLocation(this.handle(), uniform);
         if (index < 0) {
            LOGGER.debug("No active uniform '{}' exists. Could be unused.", uniform);
         }

         return index;
      });
   }

   public void setSamplerBinding(String name, GlTextureUnit binding) {
      this.setSamplerBinding(name, binding.number);
   }

   public void setSamplerBinding(String name, int binding) {
      int samplerUniform = this.getUniformLocation(name);
      if (samplerUniform >= 0) {
         GL20.glUniform1i(samplerUniform, binding);
      }
   }

   public void setUniformBlockBinding(String name, int binding) {
      int index = GL31.glGetUniformBlockIndex(this.handle(), name);
      if (index == -1) {
         LOGGER.debug("No active uniform block '{}' exists. Could be unused.", name);
      } else {
         GL31.glUniformBlockBinding(this.handle(), index, binding);
      }
   }

   public void bindAttribLocation(String attribute, int binding) {
      GL20.glBindAttribLocation(this.handle(), binding, attribute);
   }

   @Override
   protected void deleteInternal(int handle) {
      GL20.glDeleteProgram(handle);
   }
}
