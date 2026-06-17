package dev.engine_room.flywheel.lib.model.part;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.transform.Affine;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3fc;

public final class InstanceTree {
   private final ModelTree source;
   @Nullable
   private final TransformedInstance instance;
   private final InstanceTree[] children;
   private final Matrix4f poseMatrix;
   private float x;
   private float y;
   private float z;
   private float xRot;
   private float yRot;
   private float zRot;
   private float xScale;
   private float yScale;
   private float zScale;
   private boolean visible = true;
   private boolean skipDraw = false;
   private boolean changed;

   private InstanceTree(ModelTree source, @Nullable TransformedInstance instance, InstanceTree[] children) {
      this.source = source;
      this.instance = instance;
      this.children = children;
      if (instance != null) {
         this.poseMatrix = instance.pose;
      } else {
         this.poseMatrix = new Matrix4f();
      }

      this.resetPose();
   }

   public static InstanceTree create(InstancerProvider provider, ModelTree meshTree) {
      InstanceTree[] children = new InstanceTree[meshTree.childCount()];

      for (int i = 0; i < meshTree.childCount(); i++) {
         children[i] = create(provider, meshTree.child(i));
      }

      Model model = meshTree.model();
      TransformedInstance instance;
      if (model != null) {
         instance = provider.instancer(InstanceTypes.TRANSFORMED, model).createInstance();
      } else {
         instance = null;
      }

      return new InstanceTree(meshTree, instance, children);
   }

   @Nullable
   public TransformedInstance instance() {
      return this.instance;
   }

   public PartPose initialPose() {
      return this.source.initialPose();
   }

   public int childCount() {
      return this.children.length;
   }

   public InstanceTree child(int index) {
      return this.children[index];
   }

   public String childName(int index) {
      return this.source.childName(index);
   }

   public int childIndex(String name) {
      return this.source.childIndex(name);
   }

   public boolean hasChild(String name) {
      return this.childIndex(name) >= 0;
   }

   @Nullable
   public InstanceTree child(String name) {
      int index = this.childIndex(name);
      return index < 0 ? null : this.child(index);
   }

   public InstanceTree childOrThrow(String name) {
      InstanceTree child = this.child(name);
      if (child == null) {
         throw new NoSuchElementException("Can't find part " + name);
      } else {
         return child;
      }
   }

   public void traverse(Consumer<? super TransformedInstance> consumer) {
      if (this.instance != null) {
         consumer.accept(this.instance);
      }

      for (InstanceTree child : this.children) {
         child.traverse(consumer);
      }
   }

   @Experimental
   public void traverse(int i, ObjIntConsumer<? super TransformedInstance> consumer) {
      if (this.instance != null) {
         consumer.accept(this.instance, i);
      }

      for (InstanceTree child : this.children) {
         child.traverse(i, consumer);
      }
   }

   @Experimental
   public void traverse(int i, int j, InstanceTree.ObjIntIntConsumer<? super TransformedInstance> consumer) {
      if (this.instance != null) {
         consumer.accept(this.instance, i, j);
      }

      for (InstanceTree child : this.children) {
         child.traverse(i, j, consumer);
      }
   }

   public void translateAndRotate(Affine<?> affine, Quaternionf tempQuaternion) {
      affine.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
      if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
         affine.rotate(tempQuaternion.rotationZYX(this.zRot, this.yRot, this.xRot));
      }

      if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
         affine.scale(this.xScale, this.yScale, this.zScale);
      }
   }

   public void translateAndRotate(PoseStack poseStack, Quaternionf tempQuaternion) {
      this.translateAndRotate(TransformStack.of(poseStack), tempQuaternion);
   }

   public void translateAndRotate(Matrix4f pose) {
      pose.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
      if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
         pose.rotateZYX(this.zRot, this.yRot, this.xRot);
      }

      if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
         pose.scale(this.xScale, this.yScale, this.zScale);
      }
   }

   public void updateInstances(Matrix4fc initialPose) {
      this.propagateAnimation(initialPose, true);
   }

   public void updateInstancesStatic(Matrix4fc initialPose) {
      this.propagateAnimation(initialPose, false);
   }

   public void propagateAnimation(Matrix4fc initialPose, boolean force) {
      if (this.visible) {
         if (this.changed || force) {
            this.poseMatrix.set(initialPose);
            this.translateAndRotate(this.poseMatrix);
            force = true;
            if (this.instance != null && !this.skipDraw) {
               this.instance.setChanged();
            }
         }

         for (InstanceTree child : this.children) {
            child.propagateAnimation(this.poseMatrix, force);
         }

         this.changed = false;
      }
   }

   public void visible(boolean visible) {
      this.visible = visible;
      this.updateVisible();

      for (InstanceTree child : this.children) {
         child.visible(visible);
      }
   }

   public void skipDraw(boolean skipDraw) {
      this.skipDraw = skipDraw;
      this.updateVisible();
   }

   private void updateVisible() {
      if (this.instance != null) {
         this.instance.setVisible(this.visible && !this.skipDraw);
      }
   }

   public boolean visible() {
      return this.visible;
   }

   public boolean skipDraw() {
      return this.skipDraw;
   }

   public float xPos() {
      return this.x;
   }

   public float yPos() {
      return this.y;
   }

   public float zPos() {
      return this.z;
   }

   public float xRot() {
      return this.xRot;
   }

   public float yRot() {
      return this.yRot;
   }

   public float zRot() {
      return this.zRot;
   }

   public float xScale() {
      return this.xScale;
   }

   public float yScale() {
      return this.yScale;
   }

   public float zScale() {
      return this.zScale;
   }

   public void xPos(float x) {
      this.x = x;
      this.setChanged();
   }

   public void yPos(float y) {
      this.y = y;
      this.setChanged();
   }

   public void zPos(float z) {
      this.z = z;
      this.setChanged();
   }

   public void pos(float x, float y, float z) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.setChanged();
   }

   public void xRot(float xRot) {
      this.xRot = xRot;
      this.setChanged();
   }

   public void yRot(float yRot) {
      this.yRot = yRot;
      this.setChanged();
   }

   public void zRot(float zRot) {
      this.zRot = zRot;
      this.setChanged();
   }

   public void rotation(float xRot, float yRot, float zRot) {
      this.xRot = xRot;
      this.yRot = yRot;
      this.zRot = zRot;
      this.setChanged();
   }

   public void xScale(float xScale) {
      this.xScale = xScale;
      this.setChanged();
   }

   public void yScale(float yScale) {
      this.yScale = yScale;
      this.setChanged();
   }

   public void zScale(float zScale) {
      this.zScale = zScale;
      this.setChanged();
   }

   public void scale(float xScale, float yScale, float zScale) {
      this.xScale = xScale;
      this.yScale = yScale;
      this.zScale = zScale;
      this.setChanged();
   }

   public void offsetPos(float xOffset, float yOffset, float zOffset) {
      this.x += xOffset;
      this.y += yOffset;
      this.z += zOffset;
      this.setChanged();
   }

   public void offsetXPos(float xOffset) {
      this.x += xOffset;
      this.setChanged();
   }

   public void offsetYPos(float yOffset) {
      this.y += yOffset;
      this.setChanged();
   }

   public void offsetZPos(float zOffset) {
      this.z += zOffset;
      this.setChanged();
   }

   public void offsetPos(Vector3fc offset) {
      this.offsetPos(offset.x(), offset.y(), offset.z());
   }

   public void offsetRotation(float xOffset, float yOffset, float zOffset) {
      this.xRot += xOffset;
      this.yRot += yOffset;
      this.zRot += zOffset;
      this.setChanged();
   }

   public void offsetXRot(float xOffset) {
      this.xRot += xOffset;
      this.setChanged();
   }

   public void offsetYRot(float yOffset) {
      this.yRot += yOffset;
      this.setChanged();
   }

   public void offsetZRot(float zOffset) {
      this.zRot += zOffset;
      this.setChanged();
   }

   public void offsetRotation(Vector3fc offset) {
      this.offsetRotation(offset.x(), offset.y(), offset.z());
   }

   public void offsetScale(float xOffset, float yOffset, float zOffset) {
      this.xScale += xOffset;
      this.yScale += yOffset;
      this.zScale += zOffset;
      this.setChanged();
   }

   public void offsetXScale(float xOffset) {
      this.xScale += xOffset;
      this.setChanged();
   }

   public void offsetYScale(float yOffset) {
      this.yScale += yOffset;
      this.setChanged();
   }

   public void offsetZScale(float zOffset) {
      this.zScale += zOffset;
      this.setChanged();
   }

   public void offsetScale(Vector3fc offset) {
      this.offsetScale(offset.x(), offset.y(), offset.z());
   }

   public PartPose storePose() {
      return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
   }

   public void loadPose(PartPose pose) {
      this.x = pose.x;
      this.y = pose.y;
      this.z = pose.z;
      this.xRot = pose.xRot;
      this.yRot = pose.yRot;
      this.zRot = pose.zRot;
      this.xScale = 1.0F;
      this.yScale = 1.0F;
      this.zScale = 1.0F;
      this.setChanged();
   }

   public void resetPose() {
      this.loadPose(this.source.initialPose());
   }

   public void copyTransform(InstanceTree tree) {
      this.x = tree.x;
      this.y = tree.y;
      this.z = tree.z;
      this.xRot = tree.xRot;
      this.yRot = tree.yRot;
      this.zRot = tree.zRot;
      this.xScale = tree.xScale;
      this.yScale = tree.yScale;
      this.zScale = tree.zScale;
      this.setChanged();
   }

   public void copyTransform(ModelPart modelPart) {
      this.x = modelPart.x;
      this.y = modelPart.y;
      this.z = modelPart.z;
      this.xRot = modelPart.xRot;
      this.yRot = modelPart.yRot;
      this.zRot = modelPart.zRot;
      this.xScale = modelPart.xScale;
      this.yScale = modelPart.yScale;
      this.zScale = modelPart.zScale;
      this.setChanged();
   }

   private void setChanged() {
      this.changed = true;
   }

   public void delete() {
      if (this.instance != null) {
         this.instance.delete();
      }

      for (InstanceTree child : this.children) {
         child.delete();
      }
   }

   @FunctionalInterface
   @Experimental
   public interface ObjIntIntConsumer<T> {
      void accept(T var1, int var2, int var3);
   }
}
