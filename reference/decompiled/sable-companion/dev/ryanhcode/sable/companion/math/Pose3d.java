package dev.ryanhcode.sable.companion.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.companion.impl.SableCompanionUtil;
import java.text.NumberFormat;
import org.jetbrains.annotations.Contract;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class Pose3d implements Pose3dc {
   public static Codec<Pose3d> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               SableCompanionUtil.VECTOR_3D_CODEC.fieldOf("position").forGetter(Pose3d::position),
               SableCompanionUtil.QUATERNIOND_CODEC.fieldOf("orientation").forGetter(Pose3d::orientation),
               SableCompanionUtil.VECTOR_3D_CODEC.fieldOf("rotation_point").forGetter(Pose3d::rotationPoint),
               SableCompanionUtil.VECTOR_3D_CODEC.fieldOf("scale").forGetter(Pose3d::scale)
            )
            .apply(instance, Pose3d::new)
   );
   private final Vector3d position;
   private final Quaterniond orientation;
   private final Vector3d rotationPoint;
   private final Vector3d scale;

   public Pose3d(Vector3d position, Quaterniond orientation, Vector3d rotationPoint, Vector3d scale) {
      this.position = position;
      this.orientation = orientation;
      this.rotationPoint = rotationPoint;
      this.scale = scale;
   }

   public Pose3d() {
      this.position = new Vector3d();
      this.orientation = new Quaterniond();
      this.rotationPoint = new Vector3d();
      this.scale = new Vector3d(1.0);
   }

   public Pose3d(Pose3dc pose) {
      this.position = new Vector3d(pose.position());
      this.orientation = new Quaterniond(pose.orientation());
      this.rotationPoint = new Vector3d(pose.rotationPoint());
      this.scale = new Vector3d(pose.scale());
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public Pose3d set(Pose3dc pose) {
      this.position.set(pose.position());
      this.orientation.set(pose.orientation());
      this.rotationPoint.set(pose.rotationPoint());
      this.scale.set(pose.scale());
      return this;
   }

   @Contract(
      value = "_,_->this",
      mutates = "this"
   )
   public Pose3d lerp(Pose3dc pose, double frac) {
      return this.lerp(pose, frac, this);
   }

   public Vector3d position() {
      return this.position;
   }

   public Quaterniond orientation() {
      return this.orientation;
   }

   public Vector3d rotationPoint() {
      return this.rotationPoint;
   }

   public Vector3d scale() {
      return this.scale;
   }

   @Override
   public String toString() {
      NumberFormat numberFormat = NumberFormat.getInstance();
      return "Pose3d{position=%s, orientation=%s, rotationPoint=%s, scale=%s}"
         .formatted(
            this.position.toString(numberFormat),
            this.orientation.toString(numberFormat),
            this.rotationPoint.toString(numberFormat),
            this.scale.toString(numberFormat)
         );
   }
}
