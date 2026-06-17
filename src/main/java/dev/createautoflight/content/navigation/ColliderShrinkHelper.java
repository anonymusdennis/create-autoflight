package dev.createautoflight.content.navigation;

import dev.ryanhcode.sable.api.physics.collider.VoxelColliderData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;

import java.util.UUID;

/**
 * Approach-mode physics collider: one 1×1×1 box at assembly CoM on the primary nav block;
 * all other autoflight blocks contribute zero collision volume.
 */
public final class ColliderShrinkHelper {
    private static final Vector3d DEFAULT_MIN = new Vector3d(-0.5, -0.5, -0.5);
    private static final Vector3d DEFAULT_MAX = new Vector3d(0.5, 0.5, 0.5);
    private static final double DOCK_HALF = 0.5;

    private ColliderShrinkHelper() {}

    public static void buildBoxes(
            VoxelColliderData data,
            ServerSubLevel root,
            BlockPos blockPos,
            boolean primaryNavAnchor
    ) {
        if (root == null || blockPos == null) {
            return;
        }
        UUID assemblyId = root.getUniqueId();
        if (!FlightCommandBus.isShrinkActive(assemblyId)) {
            data.clearBoxes();
            data.addBox(DEFAULT_MIN, DEFAULT_MAX);
            return;
        }

        data.clearBoxes();
        if (!primaryNavAnchor) {
            return;
        }

        Vector3d offset = AssemblyBoundsTracker.comOffsetFromBlock(root, blockPos);
        Vector3d min = new Vector3d(offset).sub(DOCK_HALF, DOCK_HALF, DOCK_HALF);
        Vector3d max = new Vector3d(offset).add(DOCK_HALF, DOCK_HALF, DOCK_HALF);
        data.addBox(min, max);
    }
}
