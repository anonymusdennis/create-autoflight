package dev.createautoflight.content.navigation.path;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Hitbox-aware 3D A* flight planner.
 *
 * <p>Where {@link FlightPathfinder}'s perpendicular-dodge fallback only escapes a single convex
 * obstacle, this performs a full graph search over a coarse 3D lattice and therefore routes around
 * concave terrain, overhangs and maze-like structures. Obstacles are inflated by the ship hitbox
 * radius: every candidate edge is validated as a swept {@link FlightPathCapsule} against both world
 * terrain and foreign assemblies via {@link ObstaclePathChecker#isCapsuleClear}, so a node is only
 * reachable if the whole ship can sweep into it without clipping.</p>
 *
 * <p>The raw lattice path is then simplified with a line-of-sight "string pulling" pass, collapsing
 * collinear/visible waypoints so the craft flies smooth straight legs instead of a staircase.</p>
 */
public final class FlightAStar {
    /** Grid cell size is clamped into this range (blocks); the true hitbox radius drives collision. */
    private static final double MIN_STEP = 2.0;
    private static final double MAX_STEP = 24.0;
    /** Skip A* entirely beyond this start->goal distance; long hauls fall back to dodge. */
    private static final double MAX_RANGE = 384.0;
    /** Extra slack around the start/goal bounding box the search is allowed to wander into. */
    private static final double SEARCH_MARGIN = 64.0;
    /** Hard cap on expanded nodes to bound server-tick cost. */
    private static final int MAX_EXPANSIONS = 1500;

    private static final int[][] NEIGHBOR_OFFSETS = buildNeighborOffsets();

    /**
     * Plans a smoothed sequence of world-space waypoints from {@code start} to {@code goal}.
     *
     * @param shipRadius omni-directional ship hitbox radius used to inflate every obstacle.
     * @return ordered waypoints ending at {@code goal} (excluding {@code start}); {@code null} if no
     *         route is found or the request is out of A* range.
     */
    public List<Vector3d> plan(
            ServerLevel level,
            ServerSubLevel root,
            Vector3d start,
            Vector3d goal,
            double shipRadius,
            boolean ignoreTerrain,
            UUID selfId
    ) {
        double directDist = start.distance(goal);
        if (directDist < 1e-3 || directDist > MAX_RANGE) {
            return null;
        }

        double step = Math.clamp(shipRadius, MIN_STEP, MAX_STEP);
        double radius = Math.max(0.5, shipRadius);

        // Bounding box (in cell units) the search may not leave, derived from start+goal plus slack.
        double pad = SEARCH_MARGIN + step;
        int boundMinX = floorDiv(Math.min(start.x, goal.x) - pad - start.x, step);
        int boundMaxX = ceilDiv(Math.max(start.x, goal.x) + pad - start.x, step);
        int boundMinY = floorDiv(Math.min(start.y, goal.y) - pad - start.y, step);
        int boundMaxY = ceilDiv(Math.max(start.y, goal.y) + pad - start.y, step);
        int boundMinZ = floorDiv(Math.min(start.z, goal.z) - pad - start.z, step);
        int boundMaxZ = ceilDiv(Math.max(start.z, goal.z) + pad - start.z, step);

        int goalX = (int) Math.round((goal.x - start.x) / step);
        int goalY = (int) Math.round((goal.y - start.y) / step);
        int goalZ = (int) Math.round((goal.z - start.z) / step);

        Map<Long, Node> nodes = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();

        Node startNode = new Node(0, 0, 0, key(0, 0, 0));
        startNode.g = 0;
        startNode.f = heuristic(0, 0, 0, goalX, goalY, goalZ);
        nodes.put(startNode.key, startNode);
        open.add(startNode);

        int expansions = 0;
        Node goalReached = null;

        while (!open.isEmpty() && expansions < MAX_EXPANSIONS) {
            Node current = open.poll();
            if (current.closed) {
                continue;
            }
            current.closed = true;
            expansions++;

            Vector3d currentWorld = world(start, current, step);

            // Try to connect directly to the goal once we are within a couple of cells of it.
            if (cellDistanceSq(current.x, current.y, current.z, goalX, goalY, goalZ) <= 4
                    && segmentClear(level, root, currentWorld, goal, radius, ignoreTerrain, selfId)) {
                goalReached = current;
                break;
            }

            for (int[] off : NEIGHBOR_OFFSETS) {
                int nx = current.x + off[0];
                int ny = current.y + off[1];
                int nz = current.z + off[2];
                if (nx < boundMinX || nx > boundMaxX
                        || ny < boundMinY || ny > boundMaxY
                        || nz < boundMinZ || nz > boundMaxZ) {
                    continue;
                }

                long nKey = key(nx, ny, nz);
                Node neighbor = nodes.get(nKey);
                if (neighbor != null && neighbor.closed) {
                    continue;
                }

                double stepCost = Math.sqrt(off[0] * off[0] + off[1] * off[1] + off[2] * off[2]) * step;
                double tentativeG = current.g + stepCost;
                if (neighbor != null && tentativeG >= neighbor.g) {
                    continue;
                }

                Vector3d neighborWorld = worldOf(start, nx, ny, nz, step);
                if (!segmentClear(level, root, currentWorld, neighborWorld, radius, ignoreTerrain, selfId)) {
                    continue;
                }

                if (neighbor == null) {
                    neighbor = new Node(nx, ny, nz, nKey);
                    nodes.put(nKey, neighbor);
                }
                neighbor.parent = current;
                neighbor.g = tentativeG;
                neighbor.f = tentativeG + heuristic(nx, ny, nz, goalX, goalY, goalZ);
                open.add(neighbor);
            }
        }

        if (goalReached == null) {
            return null;
        }

        List<Vector3d> raw = reconstruct(goalReached, start, goal, step);
        return smooth(level, root, start, raw, radius, ignoreTerrain, selfId);
    }

    private List<Vector3d> reconstruct(Node goalNode, Vector3d start, Vector3d goal, double step) {
        ArrayDeque<Vector3d> stack = new ArrayDeque<>();
        stack.push(new Vector3d(goal));
        Node node = goalNode;
        while (node != null) {
            stack.push(world(start, node, step));
            node = node.parent;
        }
        // First element corresponds to the start cell; drop it so callers receive only forward goals.
        List<Vector3d> path = new ArrayList<>(stack);
        if (!path.isEmpty()) {
            path.remove(0);
        }
        return path;
    }

    /** Line-of-sight string pulling: keep a waypoint only when the leg past it would clip. */
    private List<Vector3d> smooth(
            ServerLevel level,
            ServerSubLevel root,
            Vector3d start,
            List<Vector3d> path,
            double radius,
            boolean ignoreTerrain,
            UUID selfId
    ) {
        if (path.size() <= 1) {
            return path;
        }
        List<Vector3d> result = new ArrayList<>();
        Vector3d anchor = new Vector3d(start);
        int i = 0;
        while (i < path.size()) {
            int farthest = i;
            for (int j = path.size() - 1; j > i; j--) {
                if (segmentClear(level, root, anchor, path.get(j), radius, ignoreTerrain, selfId)) {
                    farthest = j;
                    break;
                }
            }
            Vector3d keep = path.get(farthest);
            result.add(new Vector3d(keep));
            anchor = keep;
            i = farthest + 1;
        }
        return result;
    }

    private static boolean segmentClear(
            ServerLevel level,
            ServerSubLevel root,
            Vector3d a,
            Vector3d b,
            double radius,
            boolean ignoreTerrain,
            UUID selfId
    ) {
        FlightPathCapsule capsule = new FlightPathCapsule(a, b, radius);
        return ObstaclePathChecker.isCapsuleClear(level, root, capsule, ignoreTerrain, selfId);
    }

    private static double heuristic(int x, int y, int z, int gx, int gy, int gz) {
        double dx = (double) x - gx;
        double dy = (double) y - gy;
        double dz = (double) z - gz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static int cellDistanceSq(int x, int y, int z, int gx, int gy, int gz) {
        int dx = x - gx;
        int dy = y - gy;
        int dz = z - gz;
        return dx * dx + dy * dy + dz * dz;
    }

    private static Vector3d world(Vector3d start, Node node, double step) {
        return worldOf(start, node.x, node.y, node.z, step);
    }

    private static Vector3d worldOf(Vector3d start, int x, int y, int z, double step) {
        return new Vector3d(start.x + x * step, start.y + y * step, start.z + z * step);
    }

    private static int floorDiv(double value, double step) {
        return (int) Math.floor(value / step);
    }

    private static int ceilDiv(double value, double step) {
        return (int) Math.ceil(value / step);
    }

    private static long key(int x, int y, int z) {
        return (x & 0x1FFFFFL) | ((y & 0x1FFFFFL) << 21) | ((z & 0x1FFFFFL) << 42);
    }

    private static int[][] buildNeighborOffsets() {
        List<int[]> offsets = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    offsets.add(new int[]{dx, dy, dz});
                }
            }
        }
        return offsets.toArray(new int[0][]);
    }

    private static final class Node implements Comparable<Node> {
        final int x;
        final int y;
        final int z;
        final long key;
        Node parent;
        double g = Double.MAX_VALUE;
        double f = Double.MAX_VALUE;
        boolean closed;

        Node(int x, int y, int z, long key) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.key = key;
        }

        @Override
        public int compareTo(Node o) {
            return Double.compare(f, o.f);
        }
    }
}
