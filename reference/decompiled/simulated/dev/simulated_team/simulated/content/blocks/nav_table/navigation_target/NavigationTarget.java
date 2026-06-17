package dev.simulated_team.simulated.content.blocks.nav_table.navigation_target;

import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlock;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;

public interface NavigationTarget {
   @Nullable
   Vec3 getTarget(NavTableBlockEntity var1, ItemStack var2);

   default float getDeadzone() {
      return 2.0F;
   }

   default float getMaxRange() {
      return 0.0F;
   }

   default float getModulatingRange() {
      return 200.0F;
   }

   default int getRedstoneStrength(NavTableBlockEntity navBE, Direction direction, ItemStack self) {
      return this.calculateSideStrength(navBE, direction, self);
   }

   default int calculateModulatingStrength(NavTableBlockEntity navBE, ItemStack self) {
      Vec3 currentTarget = navBE.getTargetPosition(false);
      if (currentTarget == null) {
         return 0;
      } else {
         Vec3 target = navBE.getTargetPosition(true);
         Vec3 navPos = navBE.getProjectedSelfPos();
         double distance = target.distanceTo(navPos);
         return (int)Math.round(((double)this.getModulatingRange() - distance) * (double)(15.0F / this.getModulatingRange()));
      }
   }

   default int calculateSideStrength(NavTableBlockEntity navBE, Direction direction, ItemStack self) {
      Vec3 currentTarget = navBE.getTargetPosition(false);
      if (currentTarget == null) {
         return 0;
      } else {
         Direction facing = (Direction)navBE.getBlockState().getValue(NavTableBlock.FACING);
         Vec3i normal = facing.getNormal();
         Vec3 projectedTarget = navBE.getTargetPosition(true);
         Vec3 navPos = navBE.getProjectedSelfPos();
         Vec3 differenceVec = projectedTarget.subtract(navPos);
         Quaterniond worldshellRot = navBE.getSublevelRot();
         differenceVec = SimMathUtils.rotateQuat(differenceVec, worldshellRot);
         Vec3 projectedPos = getPlaneProjectedPos(differenceVec, normal);
         double distance = projectedPos.length();
         if (this.getMaxRange() > 0.0F && distance > (double)this.getMaxRange() - 1.0E-4) {
            return 0;
         } else if (distance < (double)this.getDeadzone() - 1.0E-4) {
            return 0;
         } else {
            double dot = -projectedPos.dot(Vec3.atLowerCornerOf(direction.getNormal())) / distance;
            return (int)(Math.asin(dot) / Math.PI * 30.0 + 0.5);
         }
      }
   }

   default double distanceToTarget(NavTableBlockEntity blockEntity) {
      Vec3 targetPosition = blockEntity.getTargetPosition(true);
      return targetPosition != null ? blockEntity.getProjectedSelfPos().distanceTo(targetPosition) : -1.0;
   }

   default void onInsert(ItemStack itemStack, NavTableBlockEntity be, @Nullable Player player) {
   }

   default void onExtract(ItemStack itemStack, NavTableBlockEntity be, @Nullable Player player) {
   }

   static Vec3 getPlaneProjectedPos(Vec3 targetPos, Vec3i normal) {
      double dot = targetPos.dot(Vec3.atLowerCornerOf(normal));
      return targetPos.subtract(Vec3.atLowerCornerOf(normal).scale(dot));
   }

   @Nullable
   static NavigationTarget ofStack(ItemStack itemStack) {
      return (NavigationTarget)itemStack.get(SimDataComponents.TARGET);
   }
}
