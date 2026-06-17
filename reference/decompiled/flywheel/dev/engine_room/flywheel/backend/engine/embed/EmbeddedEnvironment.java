package dev.engine_room.flywheel.backend.engine.embed;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.backend.engine.EngineImpl;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import net.minecraft.core.Vec3i;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class EmbeddedEnvironment implements VisualEmbedding, Environment {
   private final EngineImpl engine;
   private final Vec3i renderOrigin;
   @Nullable
   private final EmbeddedEnvironment parent;
   private final InstancerProvider instancerProvider;
   private final Matrix4f pose = new Matrix4f();
   private final Matrix3f normal = new Matrix3f();
   private final Matrix4f poseComposed = new Matrix4f();
   private final Matrix3f normalComposed = new Matrix3f();
   public int matrixIndex = 0;
   private boolean deleted = false;

   public EmbeddedEnvironment(EngineImpl engine, Vec3i renderOrigin, @Nullable EmbeddedEnvironment parent) {
      this.engine = engine;
      this.renderOrigin = renderOrigin;
      this.parent = parent;
      this.instancerProvider = new InstancerProvider() {
         @Override
         public <I extends Instance> Instancer<I> instancer(InstanceType<I> type, Model model, int bias) {
            return engine.instancer(EmbeddedEnvironment.this, type, model, bias);
         }
      };
   }

   public EmbeddedEnvironment(EngineImpl engine, Vec3i renderOrigin) {
      this(engine, renderOrigin, null);
   }

   @Override
   public void transforms(Matrix4fc pose, Matrix3fc normal) {
      this.pose.set(pose);
      this.normal.set(normal);
   }

   @Override
   public InstancerProvider instancerProvider() {
      return this.instancerProvider;
   }

   @Override
   public Vec3i renderOrigin() {
      return this.renderOrigin;
   }

   @Override
   public VisualEmbedding createEmbedding(Vec3i renderOrigin) {
      EmbeddedEnvironment out = new EmbeddedEnvironment(this.engine, renderOrigin, this);
      this.engine.environmentStorage().track(out);
      return out;
   }

   @Override
   public ContextShader contextShader() {
      return ContextShader.EMBEDDED;
   }

   @Override
   public void setupDraw(GlProgram program) {
      program.setMat4("_flw_modelMatrixUniform", this.poseComposed);
      program.setMat3("_flw_normalMatrixUniform", this.normalComposed);
   }

   @Override
   public int matrixIndex() {
      return this.matrixIndex;
   }

   public void flush(long ptr) {
      this.poseComposed.identity();
      this.normalComposed.identity();
      this.composeMatrices(this.poseComposed, this.normalComposed);
      ExtraMemoryOps.putMatrix4f(ptr, this.poseComposed);
      ExtraMemoryOps.putMatrix3fPadded(ptr + 64L, this.normalComposed);
   }

   private void composeMatrices(Matrix4f pose, Matrix3f normal) {
      if (this.parent != null) {
         this.parent.composeMatrices(pose, normal);
         pose.mul(this.pose);
         normal.mul(this.normal);
      } else {
         pose.set(this.pose);
         normal.set(this.normal);
      }
   }

   public boolean isDeleted() {
      return this.deleted;
   }

   @Override
   public void delete() {
      this.deleted = true;
   }
}
