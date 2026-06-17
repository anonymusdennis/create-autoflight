package dev.ryanhcode.sable.neoforge.gametest;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CommonLevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class SableTestHelper {
   public static ServerSubLevel spawnSubLevel(SubLevelContainer plotContainer, Vector3dc pos, Consumer<CommonLevelAccessor> setter) {
      Pose3d pose = new Pose3d();
      pose.position().set(pos);
      SubLevel subLevel = plotContainer.allocateNewSubLevel(pose);
      LevelPlot plot = subLevel.getPlot();
      ChunkPos center = plot.getCenterChunk();
      plot.newEmptyChunk(center);
      setter.accept(plot.getEmbeddedLevelAccessor());
      subLevel.updateLastPose();
      return (ServerSubLevel)subLevel;
   }

   public static ServerSubLevel spawnSingleBlockSubLevel(SubLevelContainer plotContainer, Vector3dc pos, BlockState state) {
      return spawnSubLevel(plotContainer, pos, accessor -> accessor.setBlock(BlockPos.ZERO, state, 3));
   }

   public static Vector3d absoluteDirection(GameTestHelper helper, Vector3dc localDirection) {
      return new Vector3d(localDirection).rotateY(-getAngle(helper.getTestRotation()));
   }

   public static Vector3d localDirection(GameTestHelper helper, Vector3dc globalDirection) {
      return new Vector3d(globalDirection).rotateY(getAngle(helper.getTestRotation()));
   }

   public static Vector3d absolutePosition(GameTestHelper helper, Vector3dc localPosition) {
      BlockPos origin = helper.testInfo.getStructureBlockPos();
      Vector3d pos = localPosition.sub(0.5, 0.5, 0.5, new Vector3d()).rotateY(-getAngle(helper.getTestRotation()));
      return pos.add((double)origin.getX() + 0.5, (double)origin.getY() + 0.5, (double)origin.getZ() + 0.5);
   }

   public static Vector3d localPosition(GameTestHelper helper, Vector3dc globalPosition) {
      BlockPos origin = helper.testInfo.getStructureBlockPos();
      Vector3d pos = globalPosition.sub((double)origin.getX(), (double)origin.getY(), (double)origin.getZ(), new Vector3d());
      return pos.rotateY(getAngle(helper.getTestRotation()));
   }

   public static double getAngle(Rotation rotation) {
      return switch (rotation) {
         case NONE -> 0.0;
         case CLOCKWISE_90 -> Math.PI / 2;
         case CLOCKWISE_180 -> Math.PI;
         case COUNTERCLOCKWISE_90 -> -Math.PI / 2;
         default -> throw new MatchException(null, null);
      };
   }

   public static boolean isInBounds(GameTestHelper helper, Vector3dc globalPosition) {
      AABB box = helper.getBounds();
      return box.contains(globalPosition.x(), globalPosition.y(), globalPosition.z());
   }

   public static void removeSubLevel(SubLevelContainer container, ServerSubLevel subLevel) {
      LevelPlot plot = subLevel.getPlot();
      Vector2i origin = container.getOrigin();
      container.removeSubLevel(plot.plotPos.x - origin.x, plot.plotPos.z - origin.y, SubLevelRemovalReason.REMOVED);
   }
}
