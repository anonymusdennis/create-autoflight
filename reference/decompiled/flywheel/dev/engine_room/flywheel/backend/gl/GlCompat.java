package dev.engine_room.flywheel.backend.gl;

import dev.engine_room.flywheel.backend.FlwBackend;
import dev.engine_room.flywheel.backend.compile.core.Compilation;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.glsl.GlslVersion;
import dev.engine_room.flywheel.lib.math.MoreMath;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.UnknownNullability;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class GlCompat {
   @UnknownNullability
   public static final GLCapabilities CAPABILITIES;
   public static final String GL_VENDOR_STRING;
   public static final String GL_RENDERER_STRING;
   public static final String GL_VERSION_STRING;
   public static final String GL_SHADING_LANGUAGE_VERSION_STRING;
   public static final Driver DRIVER;
   public static final int SUBGROUP_SIZE;
   public static final boolean ALLOW_DSA = true;
   public static final GlslVersion MAX_GLSL_VERSION;
   public static final boolean SUPPORTS_DSA;
   public static final boolean SUPPORTS_INSTANCING;
   public static final boolean SUPPORTS_INDIRECT;

   private GlCompat() {
   }

   public static void init() {
   }

   public static int getComputeGroupCount(int invocations) {
      return MoreMath.ceilingDiv(invocations, SUBGROUP_SIZE);
   }

   public static void safeShaderSource(int glId, CharSequence source) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source, true);
         PointerBuffer pointers = stack.mallocPointer(1);
         pointers.put(sourceBuffer);
         GL20C.nglShaderSource(glId, 1, pointers.address0(), 0L);
         MemoryUtil.memFree(sourceBuffer);
      } catch (Throwable var6) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (stack != null) {
         stack.close();
      }
   }

   public static void safeMultiDrawElementsIndirect(GlProgram drawProgram, int mode, int type, int start, int end, long stride) {
      int count = end - start;
      long indirect = (long)start * stride;
      if (DRIVER == Driver.INTEL) {
         for (int i = 0; i < count; i++) {
            drawProgram.setUInt("_flw_baseDraw", start + i);
            GL40.glDrawElementsIndirect(mode, type, indirect);
            indirect += stride;
         }
      } else {
         drawProgram.setUInt("_flw_baseDraw", start);
         GL43.glMultiDrawElementsIndirect(mode, type, indirect, count, (int)stride);
      }
   }

   private static Driver readVendorString() {
      if (CAPABILITIES == null) {
         return Driver.UNKNOWN;
      } else if (GL_VENDOR_STRING.contains("ATI") || GL_VENDOR_STRING.contains("AMD")) {
         return Driver.AMD;
      } else if (GL_VENDOR_STRING.contains("NVIDIA")) {
         return Driver.NVIDIA;
      } else if (GL_VENDOR_STRING.contains("Intel")) {
         return Driver.INTEL;
      } else {
         return GL_VENDOR_STRING.contains("Mesa") ? Driver.MESA : Driver.UNKNOWN;
      }
   }

   private static int subgroupSize() {
      if (CAPABILITIES == null) {
         return 32;
      } else if (CAPABILITIES.GL_KHR_shader_subgroup) {
         return GL31C.glGetInteger(38194);
      } else {
         return DRIVER != Driver.AMD && DRIVER != Driver.MESA ? 32 : 64;
      }
   }

   private static boolean isInstancingSupported() {
      if (CAPABILITIES == null) {
         return false;
      } else {
         return CAPABILITIES.OpenGL33 ? true : CAPABILITIES.GL_ARB_shader_bit_encoding;
      }
   }

   private static boolean isIndirectSupported() {
      if (CAPABILITIES == null) {
         return false;
      } else {
         return CAPABILITIES.OpenGL46
            ? true
            : CAPABILITIES.GL_ARB_compute_shader
               && CAPABILITIES.GL_ARB_direct_state_access
               && CAPABILITIES.GL_ARB_gpu_shader5
               && CAPABILITIES.GL_ARB_multi_bind
               && CAPABILITIES.GL_ARB_multi_draw_indirect
               && CAPABILITIES.GL_ARB_shader_draw_parameters
               && CAPABILITIES.GL_ARB_shader_storage_buffer_object
               && CAPABILITIES.GL_ARB_shading_language_420pack
               && CAPABILITIES.GL_ARB_vertex_attrib_binding
               && CAPABILITIES.GL_ARB_shader_image_load_store
               && CAPABILITIES.GL_ARB_shader_image_size;
      }
   }

   private static boolean isDsaSupported() {
      return CAPABILITIES == null ? false : CAPABILITIES.GL_ARB_direct_state_access;
   }

   private static GlslVersion maxGlslVersion() {
      if (CAPABILITIES == null) {
         return GlslVersion.V150;
      } else {
         GlslVersion[] glslVersions = GlslVersion.values();

         for (int i = glslVersions.length - 1; i > 0; i--) {
            GlslVersion version = glslVersions[i];
            if (canCompileVersion(version)) {
               return version;
            }
         }

         return GlslVersion.V150;
      }
   }

   private static boolean canCompileVersion(GlslVersion version) {
      int handle = GL20.glCreateShader(35633);
      String source = "#version %d\nvoid main() {}\n".formatted(version.version);
      safeShaderSource(handle, source);
      GL20.glCompileShader(handle);
      boolean success = Compilation.compiledSuccessfully(handle);
      GL20.glDeleteShader(handle);
      return success;
   }

   private static String safeGetString(int name) {
      if (CAPABILITIES == null) {
         return "invalid";
      } else {
         String str = GL20C.glGetString(name);
         return str == null ? "null" : str;
      }
   }

   static {
      GLCapabilities caps;
      try {
         caps = GL.getCapabilities();
      } catch (IllegalStateException var2) {
         FlwBackend.LOGGER.warn("Failed to get GL capabilities; default Flywheel backends will be disabled.");
         caps = null;
      }

      CAPABILITIES = caps;
      GL_VENDOR_STRING = safeGetString(7936);
      GL_RENDERER_STRING = safeGetString(7937);
      GL_VERSION_STRING = safeGetString(7938);
      GL_SHADING_LANGUAGE_VERSION_STRING = safeGetString(35724);
      DRIVER = readVendorString();
      SUBGROUP_SIZE = subgroupSize();
      MAX_GLSL_VERSION = maxGlslVersion();
      SUPPORTS_DSA = isDsaSupported();
      SUPPORTS_INSTANCING = isInstancingSupported();
      SUPPORTS_INDIRECT = isIndirectSupported();
   }
}
