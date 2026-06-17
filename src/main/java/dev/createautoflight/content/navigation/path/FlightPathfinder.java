package dev.createautoflight.content.navigation.path;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.createautoflight.content.navigation.AssemblyBoundsTracker;
import dev.createautoflight.content.navigation.AssemblyShapeProfiler;
import dev.createautoflight.content.navigation.FlightCommand;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Capsule pathfinder with perpendicular dodge waypoints (borrel Pathfinder port).
 */
public final class FlightPathfinder {
    private static final int[] DODGE_DISTANCES = {8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192};

    private final AssemblyShapeProfiler profiler = new AssemblyShapeProfiler();
    private final AssemblyBoundsTracker bounds = new AssemblyBoundsTracker();
    private PathfinderState state = PathfinderState.PATH_CLEAR;
    private Vector3d activeWaypoint;
    private Vector3d lastDestination;
    private FlightPathCapsule lastCapsule;
    private double closestObstacle = Double.MAX_VALUE;
    private int replanCooldown;
    private final List<Vector3d> pathWaypoints = new ArrayList<>();

    public PathfinderState state() { return state; }
    public Vector3d activeWaypoint() { return activeWaypoint; }
    public FlightPathCapsule lastCapsule() { return lastCapsule; }
    public double closestObstacle() { return closestObstacle; }
    public List<Vector3d> pathWaypoints() { return List.copyOf(pathWaypoints); }

    public void clearCaches() {
        pathWaypoints.clear();
        activeWaypoint = null;
        lastDestination = null;
        lastCapsule = null;
        closestObstacle = Double.MAX_VALUE;
        replanCooldown = 0;
        state = PathfinderState.PATH_CLEAR;
        profiler.clear();
    }

    public boolean allowsMovement() {
        return state != PathfinderState.NO_WAY_FORWARD;
    }

    public void tick(
            ServerLevel level,
            ServerSubLevel root,
            Vector3d destination,
            boolean approachMode,
            boolean ignoreTerrain,
            float capsuleRadiusOverride
    ) {
        if (destination == null) {
            state = PathfinderState.PATH_CLEAR;
            activeWaypoint = null;
            pathWaypoints.clear();
            lastCapsule = null;
            return;
        }

        if (lastDestination == null || lastDestination.distance(destination) > 0.5) {
            activeWaypoint = null;
        }
        lastDestination = new Vector3d(destination);

        if (replanCooldown > 0) {
            replanCooldown--;
            return;
        }
        replanCooldown = approachMode ? 20 : 40;

        bounds.refresh(root);
        Vector3d origin = AssemblyBoundsTracker.assemblyCenterWorld(root);
        Vector3d target = activeWaypoint != null ? activeWaypoint : destination;

        double extension = bounds.frontEdgeDistance();
        float radius = approachMode ? capsuleRadiusOverride : FlightCommand.CAPSULE_RADIUS_FULL;
        float override = radius >= 0 ? radius : -1;
        lastCapsule = profiler.createCapsule(root, target, extension, override);

        if (approachMode) {
            state = PathfinderState.PATH_CLEAR;
            activeWaypoint = null;
            rebuildPath(origin, destination);
            closestObstacle = Double.MAX_VALUE;
            return;
        }

        state = PathfinderState.SEARCHING_ALT;
        ObstaclePathChecker.PathCheckResult direct = ObstaclePathChecker.testPath(
                level, root, lastCapsule, profiler, ignoreTerrain, root.getUniqueId()
        );
        closestObstacle = direct.closestObstacleDist();

        if (direct.clear()) {
            state = PathfinderState.PATH_CLEAR;
            activeWaypoint = null;
            rebuildPath(origin, destination);
            return;
        }

        Vector3d obstruction = direct.obstructionPoint();
        if (obstruction == null) {
            state = PathfinderState.NO_WAY_FORWARD;
            return;
        }

        Vector3d flightDir = new Vector3d(target).sub(origin);
        if (flightDir.lengthSquared() < 1e-6) {
            state = PathfinderState.NO_WAY_FORWARD;
            return;
        }
        flightDir.normalize();

        List<Vector3d> perps = perpendiculars(flightDir);
        perps.sort((a, b) -> Double.compare(
                a.dot(destination) - origin.dot(destination),
                b.dot(destination) - origin.dot(destination)
        ));

        for (int dist : DODGE_DISTANCES) {
            for (Vector3d perp : perps) {
                Vector3d alt = new Vector3d(obstruction).fma(dist, perp);
                FlightPathCapsule altCapsule = profiler.createCapsule(root, alt, extension, -1);
                ObstaclePathChecker.PathCheckResult altCheck = ObstaclePathChecker.testPath(
                        level, root, altCapsule, profiler, ignoreTerrain, root.getUniqueId()
                );
                if (!altCheck.clear()) {
                    continue;
                }
                if (!isUseful(origin, alt, destination)) {
                    continue;
                }
                activeWaypoint = alt;
                state = PathfinderState.ALTERNATE_PATH;
                rebuildPath(origin, alt, destination);
                return;
            }
        }

        state = PathfinderState.NO_WAY_FORWARD;
        pathWaypoints.clear();
        pathWaypoints.add(new Vector3d(origin));
    }

    private void rebuildPath(Vector3d origin, Vector3d destination) {
        pathWaypoints.clear();
        pathWaypoints.add(new Vector3d(origin));
        pathWaypoints.add(new Vector3d(destination));
    }

    private void rebuildPath(Vector3d origin, Vector3d waypoint, Vector3d destination) {
        pathWaypoints.clear();
        pathWaypoints.add(new Vector3d(origin));
        pathWaypoints.add(new Vector3d(waypoint));
        pathWaypoints.add(new Vector3d(destination));
    }

    private static boolean isUseful(Vector3d origin, Vector3d alt, Vector3d destination) {
        double direct = origin.distance(destination);
        double viaAlt = origin.distance(alt) + alt.distance(destination);
        return viaAlt < direct * 1.5 + 16;
    }

    private static List<Vector3d> perpendiculars(Vector3d flightDir) {
        Vector3d up = Math.abs(flightDir.y) < 0.9 ? new Vector3d(0, 1, 0) : new Vector3d(1, 0, 0);
        Vector3d perpA = flightDir.cross(up, new Vector3d()).normalize();
        Vector3d perpB = flightDir.cross(perpA, new Vector3d()).normalize();
        List<Vector3d> result = new ArrayList<>(4);
        result.add(new Vector3d(perpA));
        result.add(new Vector3d(perpA).negate());
        result.add(new Vector3d(perpB));
        result.add(new Vector3d(perpB).negate());
        return result;
    }
}
