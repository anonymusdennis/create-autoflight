package com.simibubi.create.content.contraptions.actors;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import net.minecraft.core.BlockPos;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class ActorInstance extends AbstractInstance {
   public float x;
   public float y;
   public float z;
   public byte blockLight;
   public byte skyLight;
   public float rotationOffset;
   public byte rotationAxisX;
   public byte rotationAxisY;
   public byte rotationAxisZ;
   public Quaternionf rotation = new Quaternionf();
   public byte rotationCenterX = 64;
   public byte rotationCenterY = 64;
   public byte rotationCenterZ = 64;
   public float speed;

   public ActorInstance(InstanceType<?> type, InstanceHandle handle) {
      super(type, handle);
   }

   public ActorInstance setPosition(BlockPos pos) {
      this.x = (float)pos.getX();
      this.y = (float)pos.getY();
      this.z = (float)pos.getZ();
      return this;
   }

   public ActorInstance setBlockLight(int blockLight) {
      this.blockLight = (byte)blockLight;
      return this;
   }

   public ActorInstance setSkyLight(int skyLight) {
      this.skyLight = (byte)skyLight;
      return this;
   }

   public ActorInstance setRotationOffset(float rotationOffset) {
      this.rotationOffset = rotationOffset;
      return this;
   }

   public ActorInstance setSpeed(float speed) {
      this.speed = speed;
      return this;
   }

   public ActorInstance setRotationAxis(Vector3f axis) {
      this.setRotationAxis(axis.x(), axis.y(), axis.z());
      return this;
   }

   public ActorInstance setRotationAxis(float rotationAxisX, float rotationAxisY, float rotationAxisZ) {
      this.rotationAxisX = (byte)((int)(rotationAxisX * 127.0F));
      this.rotationAxisY = (byte)((int)(rotationAxisY * 127.0F));
      this.rotationAxisZ = (byte)((int)(rotationAxisZ * 127.0F));
      return this;
   }

   public ActorInstance setRotationCenter(Vector3f axis) {
      this.setRotationCenter(axis.x(), axis.y(), axis.z());
      return this;
   }

   public ActorInstance setRotationCenter(float rotationCenterX, float rotationCenterY, float rotationCenterZ) {
      this.rotationCenterX = (byte)((int)(rotationCenterX * 127.0F));
      this.rotationCenterY = (byte)((int)(rotationCenterY * 127.0F));
      this.rotationCenterZ = (byte)((int)(rotationCenterZ * 127.0F));
      return this;
   }

   public ActorInstance setLocalRotation(Quaternionfc q) {
      this.rotation.set(q);
      return this;
   }
}
