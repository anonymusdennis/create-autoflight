package com.simibubi.create.api.contraption;

import com.simibubi.create.impl.contraption.BlockMovementChecksImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockMovementChecks {
   public static void registerMovementNecessaryCheck(BlockMovementChecks.MovementNecessaryCheck check) {
      BlockMovementChecksImpl.registerMovementNecessaryCheck(check);
   }

   public static void registerMovementAllowedCheck(BlockMovementChecks.MovementAllowedCheck check) {
      BlockMovementChecksImpl.registerMovementAllowedCheck(check);
   }

   public static void registerBrittleCheck(BlockMovementChecks.BrittleCheck check) {
      BlockMovementChecksImpl.registerBrittleCheck(check);
   }

   public static void registerAttachedCheck(BlockMovementChecks.AttachedCheck check) {
      BlockMovementChecksImpl.registerAttachedCheck(check);
   }

   public static void registerNotSupportiveCheck(BlockMovementChecks.NotSupportiveCheck check) {
      BlockMovementChecksImpl.registerNotSupportiveCheck(check);
   }

   public static boolean isMovementNecessary(BlockState state, Level world, BlockPos pos) {
      return BlockMovementChecksImpl.isMovementNecessary(state, world, pos);
   }

   public static boolean isMovementAllowed(BlockState state, Level world, BlockPos pos) {
      return BlockMovementChecksImpl.isMovementAllowed(state, world, pos);
   }

   public static boolean isBrittle(BlockState state) {
      return BlockMovementChecksImpl.isBrittle(state);
   }

   public static boolean isBlockAttachedTowards(BlockState state, Level world, BlockPos pos, Direction direction) {
      return BlockMovementChecksImpl.isBlockAttachedTowards(state, world, pos, direction);
   }

   public static boolean isNotSupportive(BlockState state, Direction facing) {
      return BlockMovementChecksImpl.isNotSupportive(state, facing);
   }

   private BlockMovementChecks() {
      throw new AssertionError("This class should not be instantiated");
   }

   @FunctionalInterface
   public interface AttachedCheck {
      BlockMovementChecks.CheckResult isBlockAttachedTowards(BlockState var1, Level var2, BlockPos var3, Direction var4);
   }

   @FunctionalInterface
   public interface BrittleCheck {
      BlockMovementChecks.CheckResult isBrittle(BlockState var1);
   }

   public static enum CheckResult {
      SUCCESS,
      FAIL,
      PASS;

      public boolean toBoolean() {
         if (this == PASS) {
            throw new IllegalStateException("PASS does not have a boolean value");
         } else {
            return this == SUCCESS;
         }
      }

      public static BlockMovementChecks.CheckResult of(boolean b) {
         return b ? SUCCESS : FAIL;
      }

      public static BlockMovementChecks.CheckResult of(Boolean b) {
         return b == null ? PASS : (b ? SUCCESS : FAIL);
      }
   }

   @FunctionalInterface
   public interface MovementAllowedCheck {
      BlockMovementChecks.CheckResult isMovementAllowed(BlockState var1, Level var2, BlockPos var3);
   }

   @FunctionalInterface
   public interface MovementNecessaryCheck {
      BlockMovementChecks.CheckResult isMovementNecessary(BlockState var1, Level var2, BlockPos var3);
   }

   @FunctionalInterface
   public interface NotSupportiveCheck {
      BlockMovementChecks.CheckResult isNotSupportive(BlockState var1, Direction var2);
   }
}
