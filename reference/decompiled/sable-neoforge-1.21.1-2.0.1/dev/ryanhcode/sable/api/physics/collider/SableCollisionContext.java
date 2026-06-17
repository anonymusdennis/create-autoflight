package dev.ryanhcode.sable.api.physics.collider;

import dev.ryanhcode.sable.physics.impl.SableCollisionContextImpl;
import net.minecraft.world.phys.shapes.CollisionContext;

public interface SableCollisionContext extends CollisionContext {
   static SableCollisionContext get() {
      return SableCollisionContextImpl.INSTANCE;
   }
}
