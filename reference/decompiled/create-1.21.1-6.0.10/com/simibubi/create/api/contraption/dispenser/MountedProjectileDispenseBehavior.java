package com.simibubi.create.api.contraption.dispenser;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.mixin.accessor.ProjectileDispenseBehaviorAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public abstract class MountedProjectileDispenseBehavior extends DefaultMountedDispenseBehavior {
   @Override
   protected ItemStack execute(ItemStack stack, MovementContext context, BlockPos pos, Vec3 facing) {
      double x = (double)pos.getX() + facing.x * 0.7 + 0.5;
      double y = (double)pos.getY() + facing.y * 0.7 + 0.5;
      double z = (double)pos.getZ() + facing.z * 0.7 + 0.5;
      Projectile projectile = this.getProjectile(context.world, x, y, z, stack.copy(), MountedDispenseBehavior.getClosestFacingDirection(facing));
      if (projectile == null) {
         return stack;
      } else {
         Vec3 motion = facing.scale((double)this.getPower()).add(context.motion);
         projectile.shoot(motion.x, motion.y, motion.z, (float)motion.length(), this.getUncertainty());
         context.world.addFreshEntity(projectile);
         stack.shrink(1);
         return stack;
      }
   }

   @Override
   protected void playSound(LevelAccessor level, BlockPos pos) {
      level.levelEvent(1002, pos, 0);
   }

   @Nullable
   protected abstract Projectile getProjectile(Level var1, double var2, double var4, double var6, ItemStack var8, Direction var9);

   protected float getUncertainty() {
      return 6.0F;
   }

   protected float getPower() {
      return 1.1F;
   }

   public static MountedDispenseBehavior of(ProjectileDispenseBehavior vanillaBehaviour) {
      final ProjectileDispenseBehaviorAccessor accessor = (ProjectileDispenseBehaviorAccessor)vanillaBehaviour;
      return new MountedProjectileDispenseBehavior() {
         @Override
         protected Projectile getProjectile(Level level, double x, double y, double z, ItemStack stack, Direction facing) {
            return accessor.create$getProjectileItem().asProjectile(level, new Vec3(x, y, z), stack, facing);
         }

         @Override
         protected float getUncertainty() {
            return accessor.create$getDispenseConfig().uncertainty();
         }

         @Override
         protected float getPower() {
            return accessor.create$getDispenseConfig().power();
         }
      };
   }
}
