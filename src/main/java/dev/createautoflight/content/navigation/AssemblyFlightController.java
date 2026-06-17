package dev.createautoflight.content.navigation;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.createautoflight.content.navigation.path.FlightPathCapsule;
import dev.createautoflight.content.navigation.path.FlightPathfinder;
import dev.createautoflight.content.navigation.path.PathfinderState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class AssemblyFlightController {
    private static final double DOCKED_SPEED_THRESHOLD = 0.08;
    private static final double FINAL_DOCK_HEADING_LIMIT_RAD = Math.toRadians(30);

    private final FlightPathfinder pathfinder = new FlightPathfinder();
    private final AssemblyBoundsTracker bounds = new AssemblyBoundsTracker();

    private HoldPositionSnapshot holdSnapshot;
    /** One-shot latch for redstone + nav-table station keeping; cleared when redstone turns off. */
    private HoldPositionSnapshot redstoneHoldLatch;
    private Vector3d currentDestination;
    private NavigationMode mode = NavigationMode.IDLE;
    private NavStatus status = NavStatus.IDLE;
    private ApproachPhase approachPhase = ApproachPhase.NONE;
    private boolean lastShrinkActive;
    private int dockedTicks;

    public NavStatus status() { return status; }
    public NavigationMode mode() { return mode; }
    public ApproachPhase approachPhase() { return approachPhase; }

    public void onActivated(ServerSubLevel root, boolean hasNavTable) {
        dockedTicks = 0;
        if (redstoneHoldLatch != null) {
            holdSnapshot = copySnapshot(redstoneHoldLatch);
            return;
        }
        if (!hasNavTable) {
            holdSnapshot = snapshotFrom(root);
        } else {
            holdSnapshot = null;
        }
    }

    /** Captures the navigation goal once when redstone first turns on. */
    public void onRedstoneHoldEngaged(ServerLevel level, BlockPos navPos, ServerSubLevel root) {
        if (redstoneHoldLatch == null) {
            redstoneHoldLatch = latchGoalSnapshot(level, navPos, root);
        }
        holdSnapshot = copySnapshot(redstoneHoldLatch);
        mode = NavigationMode.HOLD;
        dockedTicks = 0;
    }

    public void onRedstoneHoldReleased() {
        redstoneHoldLatch = null;
        onDeactivated();
    }

    public boolean isRedstoneHoldActive() {
        return redstoneHoldLatch != null;
    }

    public void onDeactivated() {
        if (redstoneHoldLatch != null) {
            return;
        }
        holdSnapshot = null;
        currentDestination = null;
        mode = NavigationMode.IDLE;
        status = NavStatus.IDLE;
        approachPhase = ApproachPhase.NONE;
        dockedTicks = 0;
        pathfinder.clearCaches();
    }

    public FlightCommand tick(ServerLevel level, ServerSubLevel root, BlockPos navPos, NavigationSettings settings, boolean active) {
        if (!active) {
            mode = NavigationMode.IDLE;
            status = NavStatus.IDLE;
            approachPhase = ApproachPhase.NONE;
            return FlightCommand.idle();
        }

        bounds.refresh(root);
        Vector3d velocity = new Vector3d(root.latestLinearVelocity);
        double speed = velocity.length();
        Vector3d destination = resolveDestination(level, navPos, root);
        currentDestination = destination;

        if (destination == null) {
            mode = NavigationMode.IDLE;
            status = NavStatus.IDLE;
            approachPhase = ApproachPhase.NONE;
            return FlightCommand.idle();
        }

        Vector3d center = AssemblyBoundsTracker.assemblyCenterWorld(root);
        double distToDest = center.distance(destination);

        ThrusterFleetProfiler.FleetProfile navFleet = ThrusterFleetProfiler.navProfile(root);
        ThrusterFleetProfiler.FleetProfile brakeFleet = ThrusterFleetProfiler.brakeProfile(root);
        double maxDecel = ThrusterFleetProfiler.effectiveDeceleration(navFleet, brakeFleet);
        double stopping = ThrusterFleetProfiler.combinedStoppingDistance(speed, navFleet, brakeFleet);

        boolean stationKeep = redstoneHoldLatch != null || mode == NavigationMode.HOLD;
        double dockTolerance = NavigationSettings.HOLD_DOCK_TOLERANCE;

        approachPhase = computeApproachPhase(distToDest, settings, stopping, speed, dockTolerance);
        boolean approachMode = approachPhase != ApproachPhase.NONE;
        if (approachPhase == ApproachPhase.APPROACH || approachPhase == ApproachPhase.FINAL_DOCK) {
            if (mode != NavigationMode.HOLD) {
                mode = NavigationMode.APPROACHING;
            }
        }

        float capsuleOverride = approachMode ? FlightCommand.CAPSULE_RADIUS_APPROACH : FlightCommand.CAPSULE_RADIUS_FULL;
        boolean dockedHold = stationKeep && distToDest <= dockTolerance && speed < DOCKED_SPEED_THRESHOLD;
        boolean holdNearGoal = mode == NavigationMode.HOLD
                && approachPhase == ApproachPhase.NONE
                && distToDest < settings.getAvoidanceOffDistance();
        if (dockedHold || holdNearGoal) {
            pathfinder.clearCaches();
        } else {
            pathfinder.tick(level, root, destination, approachMode, settings.isIgnoreTerrain(), capsuleOverride);
        }

        Vector3d flightTarget = pathfinder.activeWaypoint() != null && approachPhase == ApproachPhase.NONE
                ? pathfinder.activeWaypoint()
                : destination;

        if (!pathfinder.allowsMovement() && approachPhase == ApproachPhase.NONE) {
            status = pathfinder.state() == PathfinderState.NO_WAY_FORWARD ? NavStatus.NO_PATH : NavStatus.PATHFINDING;
            updateShrinkCollider(root, approachMode);
            return buildCommand(root, flightTarget, new Vector3d(), approachPhase, approachMode, capsuleOverride, true, false, settings, distToDest);
        }

        NavigationKinematics.KinematicsResult kin = NavigationKinematics.measure(root, flightTarget, velocity);
        NavSpeedController.SpeedPlan speedPlan = NavSpeedController.compute(
                settings, approachPhase, distToDest, stopping, maxDecel, pathfinder.closestObstacle()
        );
        double targetSpeed = NavSpeedController.selectSpeed(
                speedPlan, approachPhase, distToDest, settings.getArrivalRadius(), speed
        );
        targetSpeed = NavSpeedController.clampToStoppingProfile(targetSpeed, distToDest, maxDecel);

        boolean blockTranslation = kin.needsStopAndRotate();
        if (stationKeep) {
            blockTranslation = false;
        } else if (approachPhase == ApproachPhase.FINAL_DOCK || approachPhase == ApproachPhase.APPROACH) {
            blockTranslation = kin.needsStopAndRotate() && headingErrorExceeds(root, flightTarget, FINAL_DOCK_HEADING_LIMIT_RAD);
        }

        boolean requestBrakeAssist = approachPhase == ApproachPhase.FINAL_DOCK
                || approachPhase == ApproachPhase.DOCKED
                || (approachPhase == ApproachPhase.APPROACH && speed > targetSpeed + 0.05);

        status = resolveStatus(approachPhase, pathfinder.state(), distToDest, settings, speed);

        Vector3d desiredVel = new Vector3d();
        if (distToDest > dockTolerance && !blockTranslation) {
            Vector3d toTarget = NavigationKinematics.toTargetVector(center, flightTarget, settings.isInvertThrust());
            if (toTarget.lengthSquared() > 1e-6) {
                Vector3d moveDir = settings.isHelicopterMode()
                        ? NavigationKinematics.helicopterMovementDirection(
                                center, flightTarget, settings.getHelicopterMaxPitchDeg(),
                                settings.isInvertThrust(), distToDest, settings.getArrivalRadius())
                        : new Vector3d(toTarget).normalize();
                if (approachPhase != ApproachPhase.DOCKED && targetSpeed > 1e-4) {
                    desiredVel.set(moveDir.mul(Math.max(targetSpeed, 0.35)));
                } else if (stationKeep || distToDest > NavigationSettings.HOLD_DOCK_TOLERANCE) {
                    double correction = Math.min(
                            NavSpeedController.clampToStoppingProfile(
                                    NavSpeedController.BASE_CRUISE_BLOCKS_PER_SEC * (settings.getSlowSpeedPercent() / 100.0),
                                    distToDest,
                                    maxDecel
                            ),
                            Math.max(0.5, distToDest * 0.35)
                    );
                    desiredVel.set(moveDir.mul(Math.max(correction, 0.35)));
                }
            }
        }

        if (approachPhase == ApproachPhase.DOCKED) {
            dockedTicks++;
        } else {
            dockedTicks = 0;
        }

        if (settings.isHelicopterMode()) {
            blockTranslation = false;
        }

        updateShrinkCollider(root, approachMode);
        return buildCommand(
                root,
                flightTarget,
                desiredVel,
                approachPhase,
                approachMode,
                capsuleOverride,
                blockTranslation,
                requestBrakeAssist,
                settings,
                distToDest
        );
    }

    private NavStatus resolveStatus(
            ApproachPhase phase,
            PathfinderState pfState,
            double distToDest,
            NavigationSettings settings,
            double speed
    ) {
        if (phase == ApproachPhase.DOCKED) {
            return mode == NavigationMode.HOLD ? NavStatus.HOLDING : NavStatus.ARRIVED;
        }
        if (phase == ApproachPhase.FINAL_DOCK) {
            return NavStatus.DOCKING;
        }
        if (phase == ApproachPhase.APPROACH) {
            return NavStatus.APPROACHING;
        }
        if (distToDest <= NavigationSettings.HOLD_DOCK_TOLERANCE && speed < DOCKED_SPEED_THRESHOLD) {
            return mode == NavigationMode.HOLD ? NavStatus.HOLDING : NavStatus.ARRIVED;
        }
        return pfState == PathfinderState.ALTERNATE_PATH ? NavStatus.PATHFINDING : NavStatus.PATH_OK;
    }

    private ApproachPhase computeApproachPhase(
            double distToDest,
            NavigationSettings settings,
            double stoppingDistance,
            double speed,
            double dockTolerance
    ) {
        if (distToDest <= dockTolerance) {
            return speed < DOCKED_SPEED_THRESHOLD * 2 || dockedTicks > 0
                    ? ApproachPhase.DOCKED
                    : ApproachPhase.FINAL_DOCK;
        }
        double finalDockThreshold = Math.max(
                Math.max(settings.getArrivalRadius(), stoppingDistance * 2.0),
                4.0
        );
        if (distToDest <= finalDockThreshold) {
            return ApproachPhase.FINAL_DOCK;
        }
        if (distToDest <= settings.getAvoidanceOffDistance()) {
            return ApproachPhase.APPROACH;
        }
        return ApproachPhase.NONE;
    }

    private static HoldPositionSnapshot latchGoalSnapshot(ServerLevel level, BlockPos navPos, ServerSubLevel root) {
        var navGoal = NavigationTargetResolver.snapshotNavGoal(level, navPos);
        if (navGoal.isPresent()) {
            Vec3 pos = navGoal.get().worldPosition();
            return new HoldPositionSnapshot(
                    new Vector3d(pos.x, pos.y, pos.z),
                    new Quaterniond(root.logicalPose().orientation())
            );
        }
        return snapshotFrom(root);
    }

    private static HoldPositionSnapshot snapshotFrom(ServerSubLevel root) {
        return new HoldPositionSnapshot(
                AssemblyBoundsTracker.assemblyCenterWorld(root),
                new Quaterniond(root.logicalPose().orientation())
        );
    }

    private static HoldPositionSnapshot copySnapshot(HoldPositionSnapshot source) {
        return new HoldPositionSnapshot(
                new Vector3d(source.worldPosition()),
                new Quaterniond(source.orientation())
        );
    }

    private static boolean headingErrorExceeds(ServerSubLevel root, Vector3d flightTarget, double limitRad) {
        Vector3d center = AssemblyBoundsTracker.assemblyCenterWorld(root);
        Vector3d toTarget = new Vector3d(flightTarget).sub(center);
        if (toTarget.lengthSquared() < 1e-6) {
            return false;
        }
        toTarget.normalize();
        Vector3d forward = root.logicalPose().orientation().transform(new Vector3d(0, 0, 1), new Vector3d()).normalize();
        double cos = Math.clamp(forward.dot(toTarget), -1, 1);
        double angle = Math.acos(cos);
        return angle > limitRad;
    }

    public NavigationDebugSnapshot buildDebugSnapshot(ServerSubLevel root, NavigationSettings settings, FlightCommand command) {
        if (!settings.isDebugOverlayEnabled()) {
            return NavigationDebugSnapshot.empty();
        }

        Vector3d dest = currentDestination;
        FlightPathCapsule cap = pathfinder.lastCapsule();
        Vec3 cap0 = cap != null ? new Vec3(cap.p0().x, cap.p0().y, cap.p0().z) : null;
        Vec3 cap1 = cap != null ? new Vec3(cap.p1().x, cap.p1().y, cap.p1().z) : null;

        Vector3d center = AssemblyBoundsTracker.assemblyCenterWorld(root);
        Vector3d bMin = AssemblyBoundsTracker.worldBoundsMin(root);
        Vector3d bMax = AssemblyBoundsTracker.worldBoundsMax(root);

        java.util.List<Vec3> path = new java.util.ArrayList<>();
        for (Vector3d p : pathfinder.pathWaypoints()) {
            path.add(new Vec3(p.x, p.y, p.z));
        }

        Vec3 wp = command.activeWaypoint() != null
                ? new Vec3(command.activeWaypoint().x, command.activeWaypoint().y, command.activeWaypoint().z)
                : null;

        GyroTargetAngles currentAngles = GyroTargetAngles.measureAttitude(root.logicalPose().orientation());
        GyroTargetAngles targetAngles = command.helicopterAssist()
                ? command.gyroTargetAngles()
                : GyroTargetAngles.ZERO;

        return new NavigationDebugSnapshot(
                dest != null ? new Vec3(dest.x, dest.y, dest.z) : null,
                wp,
                path,
                cap0, cap1,
                cap != null ? (float) cap.radius() : 0,
                new Vec3(center.x, center.y, center.z),
                new Vec3(command.desiredWorldVelocity().x, command.desiredWorldVelocity().y, command.desiredWorldVelocity().z),
                mode.name(),
                pathfinder.state().name(),
                approachPhase.name(),
                dest != null ? center.distance(dest) : 0,
                pathfinder.closestObstacle(),
                command.collisionShrinkActive(),
                command.requestBrakeAssist(),
                new Vec3(bMin.x, bMin.y, bMin.z),
                new Vec3(bMax.x, bMax.y, bMax.z),
                targetAngles.pitchDeg(),
                targetAngles.yawDeg(),
                targetAngles.rollDeg(),
                currentAngles.pitchDeg(),
                currentAngles.yawDeg(),
                currentAngles.rollDeg()
        );
    }

    private Vector3d resolveDestination(ServerLevel level, BlockPos navPos, ServerSubLevel root) {
        if (redstoneHoldLatch != null) {
            mode = NavigationMode.HOLD;
            if (holdSnapshot == null) {
                holdSnapshot = copySnapshot(redstoneHoldLatch);
            }
            return new Vector3d(redstoneHoldLatch.worldPosition());
        }

        var navTable = NavigationTargetResolver.resolveNavTable(level, navPos);
        if (navTable.isPresent()) {
            mode = NavigationMode.NAV_TABLE;
            Vec3 pos = navTable.get().worldPosition();
            return new Vector3d(pos.x, pos.y, pos.z);
        }

        if (holdSnapshot == null) {
            holdSnapshot = snapshotFrom(root);
        }
        mode = NavigationMode.HOLD;
        return new Vector3d(holdSnapshot.worldPosition());
    }

    private FlightCommand buildCommand(
            ServerSubLevel root,
            Vector3d flightTarget,
            Vector3d desiredVel,
            ApproachPhase phase,
            boolean shrink,
            float capsuleOverride,
            boolean blockTranslation,
            boolean requestBrakeAssist,
            NavigationSettings settings,
            double distToDest
    ) {
        Quaterniond orientation = desiredOrientation(root, flightTarget, phase, settings, distToDest);
        Vector3d center = AssemblyBoundsTracker.assemblyCenterWorld(root);
        GyroTargetAngles currentAttitude = GyroTargetAngles.fromOrientation(root.logicalPose().orientation());
        GyroTargetAngles gyroTargets = settings.isHelicopterMode() && phase != ApproachPhase.DOCKED
                ? NavigationKinematics.helicopterTargetAngles(
                        center, flightTarget, settings.getHelicopterMaxPitchDeg(), settings.isInvertAngle(),
                        distToDest, settings.getArrivalRadius(),
                        currentAttitude.rollDeg(), currentAttitude.yawDeg())
                : GyroTargetAngles.ZERO;
        orientation = settings.isHelicopterMode() && phase != ApproachPhase.DOCKED
                ? NavigationKinematics.orientationFromLevelAttitude(gyroTargets)
                : orientation;
        return new FlightCommand(
                desiredVel,
                orientation,
                true,
                shrink,
                capsuleOverride,
                pathfinder.activeWaypoint(),
                blockTranslation,
                phase,
                requestBrakeAssist,
                settings.isHelicopterMode(),
                gyroTargets,
                settings.getNavMaxThrust()
        );
    }

    private Quaterniond desiredOrientation(
            ServerSubLevel root,
            Vector3d flightTarget,
            ApproachPhase phase,
            NavigationSettings settings,
            double distToDest
    ) {
        if (phase == ApproachPhase.DOCKED && holdSnapshot != null) {
            return new Quaterniond(holdSnapshot.orientation());
        }
        if (settings.isHelicopterMode() && phase != ApproachPhase.DOCKED) {
            return NavigationKinematics.desiredHelicopterOrientation(
                    root, flightTarget, settings.getHelicopterMaxPitchDeg(), settings.isInvertAngle(),
                    distToDest, settings.getArrivalRadius());
        }
        if ((mode == NavigationMode.HOLD || redstoneHoldLatch != null) && holdSnapshot != null
                && phase != ApproachPhase.FINAL_DOCK) {
            return new Quaterniond(holdSnapshot.orientation());
        }
        return NavigationKinematics.desiredOrientationToward(root, flightTarget, settings.isInvertAngle());
    }

    private void updateShrinkCollider(ServerSubLevel root, boolean shrink) {
        if (shrink == lastShrinkActive) {
            return;
        }
        lastShrinkActive = shrink;
        root.getPlot().updateBoundingBox();
    }

    public void writePersistence(CompoundTag tag) {
        if (redstoneHoldLatch != null) {
            tag.putBoolean("LatchActive", true);
            tag.putDouble("LatchX", redstoneHoldLatch.worldPosition().x);
            tag.putDouble("LatchY", redstoneHoldLatch.worldPosition().y);
            tag.putDouble("LatchZ", redstoneHoldLatch.worldPosition().z);
            Quaterniond o = redstoneHoldLatch.orientation();
            tag.putDouble("LatchOx", o.x);
            tag.putDouble("LatchOy", o.y);
            tag.putDouble("LatchOz", o.z);
            tag.putDouble("LatchOw", o.w);
        } else {
            tag.putBoolean("LatchActive", false);
        }
    }

    public void readPersistence(CompoundTag tag) {
        if (!tag.getBoolean("LatchActive")) {
            redstoneHoldLatch = null;
            return;
        }
        Vector3d pos = new Vector3d(
                tag.getDouble("LatchX"),
                tag.getDouble("LatchY"),
                tag.getDouble("LatchZ")
        );
        Quaterniond orient = new Quaterniond(
                tag.getDouble("LatchOx"),
                tag.getDouble("LatchOy"),
                tag.getDouble("LatchOz"),
                tag.getDouble("LatchOw")
        );
        redstoneHoldLatch = new HoldPositionSnapshot(pos, orient);
        holdSnapshot = copySnapshot(redstoneHoldLatch);
        mode = NavigationMode.HOLD;
    }
}
