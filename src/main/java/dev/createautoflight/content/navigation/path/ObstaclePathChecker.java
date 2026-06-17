package dev.createautoflight.content.navigation.path;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.createautoflight.content.navigation.AssemblyShapeProfiler;
import dev.createautoflight.content.navigation.PlotBlockScanner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;

import java.util.UUID;

public final class ObstaclePathChecker {
    // Sub-block spacing: a coarser step skips thin walls that fall between two centerline
    // samples, letting the craft clip straight through them. Half-block steps guarantee the
    // block containing every point along the path centerline is tested.
    private static final double SAMPLE_SPACING = 0.5;

    private ObstaclePathChecker() {}

    public record PathCheckResult(boolean clear, Vector3d obstructionPoint, double closestObstacleDist) {
        public static PathCheckResult clear(double closest) {
            return new PathCheckResult(true, null, closest);
        }

        public static PathCheckResult blocked(Vector3d point, double closest) {
            return new PathCheckResult(false, point, closest);
        }
    }

    public static PathCheckResult testPath(
            ServerLevel level,
            ServerSubLevel self,
            FlightPathCapsule capsule,
            AssemblyShapeProfiler profiler,
            boolean ignoreTerrain,
            UUID selfId
    ) {
        double closest = Double.MAX_VALUE;

        if (!ignoreTerrain) {
            PathCheckResult terrain = testTerrain(level, capsule);
            if (!terrain.clear()) {
                return terrain;
            }
            closest = Math.min(closest, terrain.closestObstacleDist());
        }

        AABB capsuleAabb = capsuleAabb(capsule);
        BoundingBox3d query = new BoundingBox3d(
                capsuleAabb.minX,
                capsuleAabb.minY,
                capsuleAabb.minZ,
                capsuleAabb.maxX,
                capsuleAabb.maxY,
                capsuleAabb.maxZ
        );

        SubLevelContainer container = SubLevelContainer.getContainer(level);
        for (SubLevel other : container.queryIntersecting(query)) {
            if (!(other instanceof ServerSubLevel otherServer)) {
                continue;
            }
            if (otherServer.getUniqueId().equals(selfId)) {
                continue;
            }
            if (isAttached(otherServer, selfId)) {
                continue;
            }

            PathCheckResult assemblyHit = testForeignAssembly(otherServer, capsule, profiler, self, capsuleAabb);
            if (!assemblyHit.clear()) {
                return assemblyHit;
            }
            closest = Math.min(closest, assemblyHit.closestObstacleDist());
        }

        return PathCheckResult.clear(closest == Double.MAX_VALUE ? Double.MAX_VALUE : closest);
    }

    private static PathCheckResult testForeignAssembly(
            ServerSubLevel otherServer,
            FlightPathCapsule capsule,
            AssemblyShapeProfiler profiler,
            ServerSubLevel self,
            AABB capsuleAabb
    ) {
        LevelPlot plot = otherServer.getPlot();
        BoundingBox3ic plotBox = plot.getBoundingBox();
        Vector3d plotMin = otherServer.logicalPose().transformPosition(
                new Vector3d(plotBox.minX(), plotBox.minY(), plotBox.minZ()),
                new Vector3d()
        );
        Vector3d plotMax = otherServer.logicalPose().transformPosition(
                new Vector3d(plotBox.maxX() + 1, plotBox.maxY() + 1, plotBox.maxZ() + 1),
                new Vector3d()
        );
        AABB assemblyAabb = new AABB(
                Math.min(plotMin.x, plotMax.x),
                Math.min(plotMin.y, plotMax.y),
                Math.min(plotMin.z, plotMax.z),
                Math.max(plotMin.x, plotMax.x),
                Math.max(plotMin.y, plotMax.y),
                Math.max(plotMin.z, plotMax.z)
        );
        if (!assemblyAabb.intersects(capsuleAabb)) {
            return PathCheckResult.clear(Double.MAX_VALUE);
        }

        double closest = Double.MAX_VALUE;
        final double[] closestHolder = {closest};
        final Vector3d[] obstruction = {null};

        PlotBlockScanner.forEachSolidBlockInBox(
                plot,
                plotBox.minX(),
                plotBox.minY(),
                plotBox.minZ(),
                plotBox.maxX(),
                plotBox.maxY(),
                plotBox.maxZ(),
                (x, y, z, state) -> {
                    if (obstruction[0] != null) {
                        return;
                    }
                    Vector3d local = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                    Vector3d world = otherServer.logicalPose().transformPosition(local, new Vector3d());
                    if (!capsule.intersectsPoint(world.x, world.y, world.z)) {
                        return;
                    }
                    if (profiler.rejectionIntersects(self, world.x, world.y, world.z)) {
                        double dist = Math.sqrt(capsule.distanceSquaredToPoint(world.x, world.y, world.z));
                        closestHolder[0] = Math.min(closestHolder[0], dist);
                        obstruction[0] = world;
                    }
                }
        );

        if (obstruction[0] != null) {
            return PathCheckResult.blocked(obstruction[0], closestHolder[0]);
        }
        return PathCheckResult.clear(closestHolder[0]);
    }

    private static PathCheckResult testTerrain(ServerLevel level, FlightPathCapsule capsule) {
        Vector3d a = capsule.p0();
        Vector3d b = capsule.p1();
        Vector3d delta = new Vector3d(b).sub(a);
        double length = delta.length();
        if (length < 1e-4) {
            return PathCheckResult.clear(Double.MAX_VALUE);
        }

        int steps = Math.max(1, (int) Math.ceil(length / SAMPLE_SPACING));
        double closest = Double.MAX_VALUE;
        Vector3d step = delta.div(steps, new Vector3d());

        for (int i = 0; i <= steps; i++) {
            Vector3d sample = new Vector3d(a).fma(i, step);
            var pos = net.minecraft.core.BlockPos.containing(sample.x, sample.y, sample.z);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.getFluidState().is(Fluids.WATER) && !state.getFluidState().is(Fluids.LAVA)) {
                if (capsule.intersectsAABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)) {
                    double dist = Math.sqrt(capsule.distanceSquaredToPoint(sample.x, sample.y, sample.z));
                    closest = Math.min(closest, dist);
                    return PathCheckResult.blocked(new Vector3d(sample), closest);
                }
            }
        }
        return PathCheckResult.clear(closest);
    }

    private static boolean isAttached(ServerSubLevel other, UUID selfId) {
        ServerSubLevel current = other;
        while (current.getSplitFromSubLevel() != null) {
            if (current.getSplitFromSubLevel().equals(selfId)) {
                return true;
            }
            SubLevel parent = SubLevelContainer.getContainer(current.getLevel()).getSubLevel(current.getSplitFromSubLevel());
            if (!(parent instanceof ServerSubLevel parentServer)) {
                break;
            }
            current = parentServer;
        }
        return false;
    }

    private static AABB capsuleAabb(FlightPathCapsule capsule) {
        double r = capsule.radius();
        Vector3d a = capsule.p0();
        Vector3d b = capsule.p1();
        return new AABB(
                Math.min(a.x, b.x) - r, Math.min(a.y, b.y) - r, Math.min(a.z, b.z) - r,
                Math.max(a.x, b.x) + r, Math.max(a.y, b.y) + r, Math.max(a.z, b.z) + r
        );
    }
}
