package dev.ryanhcode.sable.api.physics.collider;

import org.joml.Vector3dc;

public interface VoxelColliderData {
   void addBox(Vector3dc var1, Vector3dc var2);

   void clearBoxes();
}
