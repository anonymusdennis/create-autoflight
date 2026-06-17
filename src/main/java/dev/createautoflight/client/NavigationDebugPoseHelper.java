package dev.createautoflight.client;

import dev.createautoflight.content.navigation.NavigationBlockEntity;
import dev.createautoflight.content.navigation.NavigationDebugSnapshot;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Client debug geometry from synced snapshots only — never touches Sable from the render thread.
 */
public final class NavigationDebugPoseHelper {
    private NavigationDebugPoseHelper() {}

    public record LiveGeometry(
            Vec3 destinationWorld,
            Vec3 activeWaypointWorld,
            List<Vec3> pathWaypoints,
            Vec3 capsuleP0,
            Vec3 capsuleP1,
            float capsuleRadius,
            Vec3 assemblyCenterWorld,
            Vec3 desiredVelocityWorld,
            Vec3 boundsMinWorld,
            Vec3 boundsMaxWorld
    ) {}

    public static LiveGeometry resolve(NavigationBlockEntity nav, float partialTick) {
        NavigationDebugSnapshot snap = nav.getDebugSnapshot();
        if (snap == null || snap.mode().equals("Idle")) {
            return null;
        }
        return fromSnapshot(snap);
    }

    private static LiveGeometry fromSnapshot(NavigationDebugSnapshot snap) {
        return new LiveGeometry(
                snap.destinationWorld(),
                snap.activeWaypointWorld(),
                snap.pathWaypoints(),
                snap.capsuleP0(),
                snap.capsuleP1(),
                snap.capsuleRadius(),
                snap.assemblyCenterWorld(),
                snap.desiredVelocityWorld(),
                snap.boundsMinWorld(),
                snap.boundsMaxWorld()
        );
    }
}
