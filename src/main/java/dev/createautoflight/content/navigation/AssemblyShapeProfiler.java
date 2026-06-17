package dev.createautoflight.content.navigation;

import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.createautoflight.content.navigation.path.FlightPathCapsule;
import org.joml.Vector3d;

import java.util.Arrays;

/**
 * Dynamic ship cross-section for capsule pathfinding (borrel GridShapeProfiler port).
 */
public final class AssemblyShapeProfiler {
    private static final double MARGIN = 3.0;
    /** Packed plot coords (x,y,z per block). Avoids one heap {@link Vector3d} per solid block. */
    private static final int MAX_CACHED_BLOCKS = 65_536;

    private int[] occupiedCoords = new int[0];
    private int occupiedBlockCount;
    private double maxRejectionDistSq;
    private int cachedMinX;
    private int cachedMinY;
    private int cachedMinZ;
    private int cachedMaxX;
    private int cachedMaxY;
    private int cachedMaxZ;
    private int cachedChunkCount = -1;

    public void refreshIfNeeded(ServerSubLevel root) {
        LevelPlot plot = root.getPlot();
        BoundingBox3ic box = plot.getBoundingBox();
        int chunkCount = plot.getLoadedChunks().size();
        if (occupiedBlockCount > 0
                && chunkCount == cachedChunkCount
                && cachedMinX == box.minX()
                && cachedMinY == box.minY()
                && cachedMinZ == box.minZ()
                && cachedMaxX == box.maxX()
                && cachedMaxY == box.maxY()
                && cachedMaxZ == box.maxZ()) {
            return;
        }

        cachedMinX = box.minX();
        cachedMinY = box.minY();
        cachedMinZ = box.minZ();
        cachedMaxX = box.maxX();
        cachedMaxY = box.maxY();
        cachedMaxZ = box.maxZ();
        cachedChunkCount = chunkCount;

        CoordBuffer buffer = new CoordBuffer();
        PlotBlockScanner.forEachSolidBlock(plot, (x, y, z, state) -> {
            if (buffer.blocks < MAX_CACHED_BLOCKS) {
                buffer.add(x, y, z);
            }
        });
        occupiedCoords = buffer.data;
        occupiedBlockCount = buffer.blocks;
        maxRejectionDistSq = 0;
    }

    public void clear() {
        occupiedCoords = new int[0];
        occupiedBlockCount = 0;
        maxRejectionDistSq = 0;
        cachedChunkCount = -1;
    }

    public FlightPathCapsule createCapsule(
            ServerSubLevel root,
            Vector3d destinationWorld,
            double extensionPastDest,
            float radiusOverride
    ) {
        refreshIfNeeded(root);
        Vector3d centerWorld = AssemblyBoundsTracker.assemblyCenterWorld(root);
        Vector3d flightDir = new Vector3d(destinationWorld).sub(centerWorld);
        if (flightDir.lengthSquared() < 1e-6) {
            flightDir.set(0, 0, 1);
        } else {
            flightDir.normalize();
        }

        Vector3d localFlight = root.logicalPose().transformPositionInverse(flightDir, new Vector3d()).normalize();
        Vector3d localCenter = root.logicalPose().transformPositionInverse(centerWorld, new Vector3d());

        maxRejectionDistSq = 0;
        Vector3d cell = new Vector3d();
        Vector3d rel = new Vector3d();
        for (int i = 0; i < occupiedBlockCount; i++) {
            int base = i * 3;
            cell.set(occupiedCoords[base] + 0.5, occupiedCoords[base + 1] + 0.5, occupiedCoords[base + 2] + 0.5);
            rel.set(cell).sub(localCenter);
            Vector3d rejection = rejectFromAxis(rel, localFlight);
            double distSq = rejection.lengthSquared();
            if (distSq > maxRejectionDistSq) {
                maxRejectionDistSq = distSq;
            }
        }

        double radius = radiusOverride >= 0
                ? radiusOverride
                : Math.sqrt(maxRejectionDistSq) + MARGIN;

        Vector3d p1 = new Vector3d(destinationWorld).fma(extensionPastDest, flightDir);
        return new FlightPathCapsule(centerWorld, p1, radius);
    }

    public boolean rejectionIntersects(ServerSubLevel root, double wx, double wy, double wz) {
        Vector3d local = root.logicalPose().transformPositionInverse(new Vector3d(wx, wy, wz), new Vector3d());
        Vector3d localCenter = root.logicalPose().transformPositionInverse(
                AssemblyBoundsTracker.assemblyCenterWorld(root),
                new Vector3d()
        );
        Vector3d rel = new Vector3d(local).sub(localCenter);
        double distSq = rel.lengthSquared();
        return distSq <= maxRejectionDistSq + 0.25;
    }

    private static Vector3d rejectFromAxis(Vector3d rel, Vector3d axis) {
        double along = rel.dot(axis);
        return new Vector3d(rel).fma(-along, axis);
    }

    private static final class CoordBuffer {
        int[] data = new int[768];
        int blocks;

        void add(int x, int y, int z) {
            int need = (blocks + 1) * 3;
            if (need > data.length) {
                data = Arrays.copyOf(data, Math.max(need, data.length * 2));
            }
            int offset = blocks * 3;
            data[offset] = x;
            data[offset + 1] = y;
            data[offset + 2] = z;
            blocks++;
        }
    }
}
