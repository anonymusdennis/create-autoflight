package dev.ryanhcode.sable.sublevel.render;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.io.Closeable;
import net.minecraft.client.Camera;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import org.joml.Matrix4f;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface SubLevelRenderData extends Closeable {
   @Override
   void close();

   void rebuild();

   boolean isSectionCompiled(int var1, int var2, int var3);

   void setDirty(int var1, int var2, int var3, boolean var4);

   void compileSections(PrioritizeChunkUpdates var1, RenderRegionCache var2, Camera var3);

   int getVisibleSectionCount();

   default Matrix4f getTransformation(double camX, double camY, double camZ) {
      return this.getTransformation(camX, camY, camZ, new Matrix4f());
   }

   default Matrix4f getTransformation(double camX, double camY, double camZ, Matrix4f store) {
      store.identity();
      Pose3dc pose = this.getSubLevel().renderPose();
      Vector3dc pos = pose.position();
      Vector3dc scale = pose.scale();
      Quaterniondc orientation = pose.orientation();
      store.translate((float)(pos.x() - camX), (float)(pos.y() - camY), (float)(pos.z() - camZ));
      store.rotate(new Quaternionf(orientation));
      store.scale((float)scale.x(), (float)scale.y(), (float)scale.z());
      return store;
   }

   ClientSubLevel getSubLevel();

   default Vector3d getChunkOffset() {
      return this.getChunkOffset(new Vector3d());
   }

   default Vector3d getChunkOffset(Vector3d dest) {
      return this.getSubLevel().renderPose().rotationPoint().negate(dest);
   }
}
