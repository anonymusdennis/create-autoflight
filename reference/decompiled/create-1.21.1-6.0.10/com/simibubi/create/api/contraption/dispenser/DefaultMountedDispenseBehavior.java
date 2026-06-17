package com.simibubi.create.api.contraption.dispenser;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;

public class DefaultMountedDispenseBehavior implements MountedDispenseBehavior {
   public static final MountedDispenseBehavior INSTANCE = new DefaultMountedDispenseBehavior();

   @Override
   public ItemStack dispense(ItemStack stack, MovementContext context, BlockPos pos) {
      Vec3 normal = MountedDispenseBehavior.getDispenserNormal(context);
      Direction closestToFacing = MountedDispenseBehavior.getClosestFacingDirection(normal);
      Container inventory = HopperBlockEntity.getContainerAt(context.world, pos.relative(closestToFacing));
      if (inventory == null) {
         ItemStack remainder = this.execute(stack, context, pos, normal);
         this.playSound(context.world, pos);
         this.playAnimation(context.world, pos, closestToFacing);
         return remainder;
      } else {
         ItemStack toInsert = stack.copyWithCount(1);
         ItemStack remainder = HopperBlockEntity.addItem(null, inventory, toInsert, closestToFacing.getOpposite());
         if (remainder.isEmpty()) {
            stack.shrink(1);
         }

         return stack;
      }
   }

   protected ItemStack execute(ItemStack stack, MovementContext context, BlockPos pos, Vec3 facing) {
      ItemStack toDispense = stack.split(1);
      spawnItem(context.world, toDispense, 6, facing, pos, context);
      return stack;
   }

   protected void playSound(LevelAccessor level, BlockPos pos) {
      level.levelEvent(1000, pos, 0);
   }

   protected void playAnimation(LevelAccessor level, BlockPos pos, Vec3 facing) {
      this.playAnimation(level, pos, MountedDispenseBehavior.getClosestFacingDirection(facing));
   }

   protected void playAnimation(LevelAccessor level, BlockPos pos, Direction direction) {
      level.levelEvent(2000, pos, direction.get3DDataValue());
   }

   public static void spawnItem(Level level, ItemStack stack, int speed, Vec3 facing, BlockPos pos, MovementContext context) {
      double x = (double)pos.getX() + facing.x + 0.5;
      double y = (double)pos.getY() + facing.y + 0.5;
      double z = (double)pos.getZ() + facing.z + 0.5;
      if (MountedDispenseBehavior.getClosestFacingDirection(facing).getAxis() == Axis.Y) {
         y -= 0.125;
      } else {
         y -= 0.15625;
      }

      ItemEntity entity = new ItemEntity(level, x, y, z, stack);
      double d3 = level.random.nextDouble() * 0.1 + 0.2;
      entity.setDeltaMovement(
         level.random.nextGaussian() * 0.0075 * (double)speed + facing.x() * d3 + context.motion.x,
         level.random.nextGaussian() * 0.0075 * (double)speed + facing.y() * d3 + context.motion.y,
         level.random.nextGaussian() * 0.0075 * (double)speed + facing.z() * d3 + context.motion.z
      );
      level.addFreshEntity(entity);
   }
}
