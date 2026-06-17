package dev.simulated_team.simulated.mixin.aabb;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({AABB.class})
public interface AABBMixin {
   @Invoker
   static Direction invokeGetDirection(AABB aABB, Vec3 vec3, double[] ds, @Nullable Direction direction, double d, double e, double f) {
      throw new AssertionError();
   }
}
