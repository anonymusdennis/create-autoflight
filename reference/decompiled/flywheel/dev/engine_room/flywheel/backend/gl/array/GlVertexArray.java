package dev.engine_room.flywheel.backend.gl.array;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.engine_room.flywheel.backend.gl.GlObject;
import dev.engine_room.flywheel.backend.gl.GlStateTracker;
import java.util.List;
import org.lwjgl.opengl.GL32;

public abstract class GlVertexArray extends GlObject {
   protected static final int MAX_ATTRIBS = GL32.glGetInteger(34921);
   protected static final int MAX_ATTRIB_BINDINGS = 16;

   public static GlVertexArray create() {
      if (GlVertexArrayDSA.SUPPORTED) {
         return new GlVertexArrayDSA();
      } else if (GlVertexArraySeparateAttributes.SUPPORTED) {
         return new GlVertexArraySeparateAttributes();
      } else if (GlVertexArrayGL3.Core33.SUPPORTED) {
         return new GlVertexArrayGL3.Core33();
      } else {
         return (GlVertexArray)(GlVertexArrayGL3.ARB.SUPPORTED ? new GlVertexArrayGL3.ARB() : new GlVertexArrayGL3.Core());
      }
   }

   public void bindForDraw() {
      GlStateTracker.bindVao(this.handle());
   }

   public abstract void bindVertexBuffer(int var1, int var2, long var3, int var5);

   public abstract void setBindingDivisor(int var1, int var2);

   public abstract void bindAttributes(int var1, int var2, List<VertexAttribute> var3);

   public abstract void setElementBuffer(int var1);

   @Override
   protected void deleteInternal(int handle) {
      GlStateManager._glDeleteVertexArrays(handle);
   }
}
