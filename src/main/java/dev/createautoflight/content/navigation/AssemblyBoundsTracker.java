package dev.createautoflight.content.navigation;

import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.joml.Vector3i;

public final class AssemblyBoundsTracker {
    private int width;
    private int height;
    private int length;
    private double frontEdgeDistance;

    public void refresh(ServerSubLevel root) {
        BoundingBox3ic box = root.getPlot().getBoundingBox();
        width = box.maxX() - box.minX() + 1;
        height = box.maxY() - box.minY() + 1;
        length = box.maxZ() - box.minZ() + 1;

        Vector3d localCenter = assemblyCenterLocal(root);
        Vector3d localMaxZ = new Vector3d(box.minX() + 0.5, localCenter.y, box.maxZ() + 0.5);
        frontEdgeDistance = localCenter.distance(localMaxZ);
    }

    public int width() { return width; }
    public int height() { return height; }
    public int length() { return length; }
    public double frontEdgeDistance() { return frontEdgeDistance; }

    /** Assembly center in plot-local block coordinates. */
    public static Vector3d assemblyCenterLocal(SubLevel root) {
        BoundingBox3ic box = root.getPlot().getBoundingBox();
        Vector3i center = new Vector3i();
        box.center(center);
        return new Vector3d(center.x + 0.5, center.y + 0.5, center.z + 0.5);
    }

    public static Vector3d assemblyCenterWorld(SubLevel root) {
        return assemblyCenterWorld(root, root.logicalPose());
    }

    public static Vector3d assemblyCenterWorld(SubLevel root, Pose3dc pose) {
        return pose.transformPosition(assemblyCenterLocal(root), new Vector3d());
    }

    /**
     * Offset from a block's local center to assembly CoM, in block-local space (for dynamic colliders).
     */
    public static Vector3d comOffsetFromBlock(ServerSubLevel root, BlockPos blockPos) {
        Vector3d comLocal = assemblyCenterLocal(root);
        Vector3d blockLocal = blockPlotLocal(root, blockPos);
        return new Vector3d(comLocal).sub(blockLocal);
    }

    private static Vector3d blockPlotLocal(ServerSubLevel root, BlockPos worldPos) {
        var plot = root.getPlot();
        var pose = root.logicalPose();
        Vector3d worldCenter = new Vector3d(worldPos.getX() + 0.5, worldPos.getY() + 0.5, worldPos.getZ() + 0.5);
        return pose.transformPositionInverse(worldCenter, new Vector3d());
    }

    public static Vector3d worldBoundsMin(SubLevel root) {
        return worldBoundsMin(root, root.logicalPose());
    }

    public static Vector3d worldBoundsMin(SubLevel root, Pose3dc pose) {
        BoundingBox3ic box = root.getPlot().getBoundingBox();
        return pose.transformPosition(
                new Vector3d(box.minX(), box.minY(), box.minZ()),
                new Vector3d()
        );
    }

    public static Vector3d worldBoundsMax(SubLevel root) {
        return worldBoundsMax(root, root.logicalPose());
    }

    public static Vector3d worldBoundsMax(SubLevel root, Pose3dc pose) {
        BoundingBox3ic box = root.getPlot().getBoundingBox();
        return pose.transformPosition(
                new Vector3d(box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1),
                new Vector3d()
        );
    }
}
