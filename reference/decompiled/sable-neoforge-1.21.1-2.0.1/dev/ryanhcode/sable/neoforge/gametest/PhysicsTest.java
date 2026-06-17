package dev.ryanhcode.sable.neoforge.gametest;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import org.joml.Vector3d;
import org.joml.Vector3dc;

@GameTestHolder("sable")
public final class PhysicsTest {
   @GameTest(
      template = "continuouscollision"
   )
   public static void testContinuousCollision(GameTestHelper helper) {
      ServerLevel level = helper.getLevel();
      ServerSubLevelContainer plotContainer = SubLevelContainer.getContainer(level);
      if (plotContainer == null) {
         throw new IllegalStateException("Plot container not found in level");
      } else {
         SubLevelPhysicsSystem physicsSystem = plotContainer.physicsSystem();
         if (physicsSystem == null) {
            throw new IllegalStateException("Plot container does not have physics");
         } else {
            ServerSubLevel subLevel = SableTestHelper.spawnSingleBlockSubLevel(
               plotContainer, SableTestHelper.absolutePosition(helper, new Vector3d(2.5, 4.0, 1.5)), Blocks.GLASS.defaultBlockState()
            );
            RigidBodyHandle handle = physicsSystem.getPhysicsHandle(subLevel);
            Vector3d impulse = SableTestHelper.absoluteDirection(helper, new Vector3d(0.0, 10.0, 20.0));
            helper.startSequence().thenExecuteAfter(10, () -> handle.applyLinearImpulse(impulse)).thenExecuteFor(40, () -> {
               Vector3d globalPos = subLevel.logicalPose().position();
               Vector3d localPos = SableTestHelper.localPosition(helper, globalPos);
               if (localPos.z >= 9.0 || !SableTestHelper.isInBounds(helper, globalPos)) {
                  helper.fail("Sublevel passed through wall", BlockPos.containing(localPos.x, localPos.y, localPos.z));
               }
            }).thenSucceed();
         }
      }
   }

   @GameTest(
      template = "gravity",
      required = false
   )
   public static void testGravity(GameTestHelper helper) {
      ServerLevel level = helper.getLevel();
      ServerSubLevelContainer plotContainer = SubLevelContainer.getContainer(level);
      if (plotContainer == null) {
         throw new IllegalStateException("Plot container not found in level");
      } else {
         SubLevelPhysicsSystem physicsSystem = plotContainer.physicsSystem();
         if (physicsSystem == null) {
            throw new IllegalStateException("Plot container does not have physics");
         } else {
            Vector3dc spawnPos = SableTestHelper.absolutePosition(helper, new Vector3d(2.5, 12.0, 2.5));
            ServerSubLevel subLevel = SableTestHelper.spawnSingleBlockSubLevel(plotContainer, spawnPos, Blocks.DIAMOND_BLOCK.defaultBlockState());
            helper.runAfterDelay(
               20L,
               () -> {
                  if (subLevel.isRemoved()) {
                     helper.fail("Sublevel was removed");
                  } else {
                     Vector3dc gravity = DimensionPhysicsData.getGravity(helper.getLevel(), spawnPos);
                     RigidBodyHandle handle = physicsSystem.getPhysicsHandle(subLevel);
                     Vector3dc linearVelocity = handle.getLinearVelocity(new Vector3d());
                     if (!gravity.equals(linearVelocity, 0.01)) {
                        Vector3d localPos = SableTestHelper.localPosition(helper, spawnPos);
                        helper.fail(
                           "Sublevel velocity didn't follow gravity: Delta: " + gravity.distance(linearVelocity),
                           BlockPos.containing(localPos.x, localPos.y, localPos.z)
                        );
                     } else {
                        Vector3d expectedDelta = gravity.mul(0.5, new Vector3d());
                        Vector3d delta = subLevel.logicalPose().position().sub(spawnPos, new Vector3d());
                        if (!expectedDelta.equals(delta, 0.01)) {
                           Vector3d localPos = SableTestHelper.localPosition(helper, spawnPos);
                           helper.fail(
                              "Sublevel position didn't follow gravity. Delta: " + expectedDelta.distance(delta),
                              BlockPos.containing(localPos.x, localPos.y, localPos.z)
                           );
                        }

                        helper.succeed();
                     }
                  }
               }
            );
         }
      }
   }

   @GameTest(
      template = "snag",
      attempts = 10,
      requiredSuccesses = 10,
      required = false
   )
   public static void testSnag(GameTestHelper helper) {
      ServerLevel level = helper.getLevel();
      ServerSubLevelContainer plotContainer = SubLevelContainer.getContainer(level);
      if (plotContainer == null) {
         throw new IllegalStateException("Plot container not found in level");
      } else {
         SubLevelPhysicsSystem physicsSystem = plotContainer.physicsSystem();
         if (physicsSystem == null) {
            throw new IllegalStateException("Plot container does not have physics");
         } else {
            Vector3dc spawnPos = SableTestHelper.absolutePosition(helper, new Vector3d(13.0, 3.5, 3.5));
            ServerSubLevel subLevel = SableTestHelper.spawnSingleBlockSubLevel(plotContainer, spawnPos, Blocks.DIAMOND_BLOCK.defaultBlockState());
            RigidBodyHandle handle = physicsSystem.getPhysicsHandle(subLevel);
            Vector3d impulse = SableTestHelper.absoluteDirection(helper, new Vector3d(-60.0, 0.0, 0.0));
            helper.startSequence().thenExecuteAfter(10, () -> handle.applyLinearImpulse(impulse)).thenExecuteFor(40, () -> {
               Vector3d globalPos = subLevel.logicalPose().position();
               Vector3d localPos = SableTestHelper.localPosition(helper, globalPos);
               if (localPos.x <= 9.0 && SableTestHelper.isInBounds(helper, globalPos)) {
                  helper.succeed();
               }
            }).thenFail(() -> {
               Vector3dc position = subLevel.logicalPose().position();
               BlockPos globalPos = BlockPos.containing(position.x(), position.y(), position.z());
               return new GameTestAssertPosException("Sub-level got stuck", globalPos, helper.relativePos(globalPos), helper.getTick());
            });
         }
      }
   }
}
